package com.piratemaker.postermaker.poster.activity_app.editsticker

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.ocmaker.pixcel.maker.data.model.draw.Draw
import com.ocmaker.pixcel.maker.data.model.draw.DrawableDraw
import com.piratemaker.postermaker.listener.listenerdraw.OnDrawListener
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AlertDialog
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.AssetHelper
import com.piratemaker.postermaker.poster.core.helper.BitmapHelper
import com.piratemaker.postermaker.poster.databinding.ActivityEditStickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class EditStickerActivity : BaseActivity<ActivityEditStickerBinding>() {


    private var touchedAnyDraw = false

    private var isEditingExisting: Boolean = false
    var currentDraw: Draw? = null

    var drawViewList: ArrayList<Draw> = arrayListOf()
    private lateinit var categoryAdapter: StickerCategoryAdapter

    private lateinit var stickerAdapter: StickerItemAdapter
    private var currentImagePath: String = ""
    private var initialImagePath: String = ""
    private var initialBountyValue: String = ""
    private var currentCategoryId: Int = 2
    private var originalPhotoPath: String = ""
    private var bountyValue: String = ""
    private var tvBountyOverlay: TextView? = null
    private val animHandler = Handler(Looper.getMainLooper())
    private var animRunnable: Runnable? = null

    override fun setViewBinding(): ActivityEditStickerBinding {
        return ActivityEditStickerBinding.inflate(LayoutInflater.from(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        // Get image path from intent
        currentImagePath = intent.getStringExtra("IMAGE_PATH") ?: ""
        initialImagePath = currentImagePath
        originalPhotoPath = intent.getStringExtra("ORIGINAL_PHOTO_PATH") ?: ""
        bountyValue = intent.getStringExtra("BOUNTY_VALUE") ?: ""
        initialBountyValue = bountyValue

        isEditingExisting = intent.getBooleanExtra("IS_EDITING_EXISTING", false)

        // Only show btnImprove when bounty data is available (from SuccessfulBountyActivity)
        if (originalPhotoPath.isEmpty()) {
            binding.btnImprove.gone()
        }
        // Load background image
        if (currentImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(currentImagePath))
                .signature(com.bumptech.glide.signature.ObjectKey(File(currentImagePath).lastModified()))  // ✅ THÊM DÒNG NÀY
                .into(binding.imgBackground)
        }

        // Setup canvas touch to deselect stickers

        initDrawView()

        setupCategoryNavigation()
        setupStickerGrid()
        loadStickersForCategory(2)
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            tvCenter.text = getString(R.string.add_sticker)
            tvCenter.gone()

            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()
            btnActionBarReset.visible()
            btnActionBarRightText.gone()
            tvRightText.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
                finish()
            }

            btnActionBarRight.setOnSingleClick {
                showInterAll {  saveAndReturn() }
            }
            btnActionBarReset.setOnSingleClick {
                showResetConfirmation()            }
        }

        binding.btnImprove.setOnSingleClick {
            reRandomBounty()
        }
    }

    private fun setupCategoryNavigation() {
        val categories = AssetHelper.getAllStickerCategories().map { StickerCategory(it) }

        categoryAdapter = StickerCategoryAdapter(categories, 2) { categoryId ->
            currentCategoryId = categoryId
            loadStickersForCategory(categoryId)
        }

        // Use normal horizontal LinearLayoutManager
        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvCategories.layoutManager = layoutManager
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupStickerGrid() {
        stickerAdapter = StickerItemAdapter { stickerPath ->
            addDrawable(stickerPath)
        }

        binding.rvStickers.layoutManager = GridLayoutManager(this, 5)
        binding.rvStickers.adapter = stickerAdapter
    }

    private fun loadStickersForCategory(categoryId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val stickers = AssetHelper.getStickersByCategory(this@EditStickerActivity, categoryId)
            withContext(Dispatchers.Main) {
                stickerAdapter.updateStickers(stickers)
            }
        }
    }


    private fun saveAndReturn() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = withContext(Dispatchers.Main) {
                    // Hide bounty overlay if visible (safety check)
                    tvBountyOverlay?.visibility = View.GONE
                    // Deselect all stickers before capturing to avoid showing handle boxes
                    binding.drawView.hideSelect()

                    // Small delay to ensure UI updates (non-blocking)
                    // Render entire canvas to bitmap (must be on Main thread)
                    BitmapHelper.createBimapFromView(binding.flCanvas)
                }

                val fileToSave = if(isEditingExisting &&currentImagePath.isNotEmpty())
                {File(currentImagePath)}
                else{
                    File(cacheDir,"temp_edited_${System.currentTimeMillis()}.jpg")
                }
                // Save to temp file (heavy I/O on background thread)
                FileOutputStream(fileToSave).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                // Check if any stickers were added
                val hasStickers = binding.drawView.getStickerCount() > 0

                withContext(Dispatchers.Main) {
                    val resultIntent = Intent().apply {
                        putExtra("EDITED_IMAGE_PATH", fileToSave.absolutePath)
                        putExtra("HAS_STICKERS", hasStickers)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun addDrawable(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmapDefault =
                Glide.with(this@EditStickerActivity)
                    .load(path)
                    .override(400, 400)
                    .submit().get().toBitmap()
            Log.d("STICKER_TYPE", "==================")
            withContext(Dispatchers.Main) {
                binding.drawView.addDraw(loadDrawableEmoji(this@EditStickerActivity, bitmapDefault))
            }

            // ← CHECK TYPE
            val currentDraw = binding.drawView.getCurrentDraw()
            Log.d("STICKER_TYPE", "isText: ${currentDraw?.isText}")
            Log.d("STICKER_TYPE", "isCharacter: ${currentDraw?.isCharacter}")

            val values = FloatArray(9)
            currentDraw?.getMatrix()?.getValues(values)
            Log.d("STICKER_TYPE", "Initial scale: ${values[Matrix.MSCALE_X]}")

        }
    }


    private fun initDrawView() {
        binding.drawView.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                Log.d("DEBUG", "DrawView touched!")  // ← THÊM LOG
                touchedAnyDraw = false

                // delay cực nhỏ để OnDrawListener có cơ hội set touchedAnyDraw=true nếu hit draw
                binding.drawView.post {
                    Log.d("DEBUG", "Post run, touchedAnyDraw=$touchedAnyDraw")
                    if (!touchedAnyDraw) {
                        binding.drawView.hideSelect()
                        Log.d("DEBUG", "Deselecting, childCount=${binding.drawView.childCount}")  // ← THÊM
                        // ==> CLICK OUTSIDE (vùng trống trên canvas)
                        currentDraw = null

                        // Ẩn handle/option (tuỳ lib của bạn: gọi hàm hide option nếu có)
                        // binding.drawView.hideOptionIcon()  // nếu thư viện có

                        // Nếu bạn có custom StickerView con trong drawView:
                        for (i in 0 until binding.drawView.childCount) {
                            val child = binding.drawView.getChildAt(i)
                            Log.d("DEBUG", "Child $i: ${child::class.simpleName}")  // ← THÊM
                            val stickerView = child as? StickerView
                            if(stickerView !=null)
                            {
                                Log.d("DEBUG", "Setting selected =false for sticker $i")
                                stickerView.setStickerSelected(false)
                            }
                            else{
                                Log.d("DEBUG", "Child $i is not a StickerView")
                            }

                            (binding.drawView.getChildAt(i) as? StickerView)?.setStickerSelected(
                                false
                            )
                        }
                    }
                }
            }
            false // trả false để drawView v
        }

        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onAddedDraw")
                    updateCurrentCurrentDraw(draw)
                    addDrawView(draw)
                }

                override fun onClickedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onClickedDraw")

                }

                override fun onDeletedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDeletedDraw")
                    deleteDrawView(draw)
                }

                override fun onDragFinishedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onDragFinishedDraw")
                }

                override fun onTouchedDownDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onTouchedDownDraw")
                    touchedAnyDraw = true
                    touchedAnyDraw =true
                    updateCurrentCurrentDraw(draw)

                    val values = FloatArray(9)
                    draw.getMatrix().getValues(values)
                    val scaleX = values[Matrix.MSCALE_X]
                    val scaleY = values[Matrix.MSCALE_Y]
                    Log.d("DEBUG", "Touch down - scaleX: $scaleX, scaleY: $scaleY")  // ← CHECK

                    touchedAnyDraw = true
                    updateCurrentCurrentDraw(draw)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}

                override fun onFlippedDraw(draw: Draw) {
                    Log.d("EditTextFlow", "DrawView: onFlippedDraw")

                }

                override fun onDoubleTappedDraw(draw: Draw) {}

                override fun onHideOptionIconDraw() {}

                override fun onUndoDeleteDraw(draw: List<Draw?>) {}

                override fun onUndoUpdateDraw(draw: List<Draw?>) {}

                override fun onUndoDeleteAll() {}

                override fun onRedoAll() {}

                override fun onReplaceDraw(draw: Draw) {}

                override fun onEditText(draw: DrawableDraw) {}

                override fun onReplace(draw: Draw) {}
            })
        }

    }

    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    fun addDrawView(draw: Draw) {
        drawViewList.add(draw)
    }

    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    fun loadDrawableEmoji(context: Context, bitmap: Bitmap): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val drawableEmoji =
            DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
        return drawableEmoji
    }

    private fun generateRandomBountyText(): String {
        val random = Random.nextInt(100)
        return when {
            random < 10 -> "Infinity \u221E"
            random < 15 -> "0"
            random < 30 -> NumberFormat.getNumberInstance(Locale.US).format(999999999)
            random < 40 -> NumberFormat.getNumberInstance(Locale.US).format(666666)
            else -> {
                val randomValue = Random.nextInt(100000, 10000001)
                NumberFormat.getNumberInstance(Locale.US).format(randomValue)
            }
        }
    }

    private fun getOrCreateBountyOverlay(): TextView {
        if (tvBountyOverlay == null) {
            tvBountyOverlay = TextView(this).apply {
                setTextColor(Color.parseColor("#3B2104"))
                textSize = 40f
                typeface = ResourcesCompat.getFont(this@EditStickerActivity, R.font.caslon_antique_regular)
                gravity = Gravity.CENTER
                visibility = View.GONE
            }
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            binding.flCanvas.addView(tvBountyOverlay, params)
        }
        return tvBountyOverlay!!
    }

    private fun reRandomBounty() {
        // Prevent multiple simultaneous animations
        if (animRunnable != null) return

        lifecycleScope.launch {
            // 1. Patch text area WITHOUT text to erase old bounty (preserves baked stickers)
            val blankPoster = patchTextArea(withText = false) ?: return@launch
            binding.imgBackground.setImageBitmap(blankPoster)

            // 2. Show overlay and start cycling animation
            val overlay = getOrCreateBountyOverlay()
            overlay.text = bountyValue
            overlay.visibility = View.VISIBLE
            overlay.alpha = 1f

            // Position overlay at 82% vertical bias (matching poster layout)
            overlay.post {
                val parentH = binding.flCanvas.height
                val textH = overlay.height
                overlay.translationY = (parentH - textH) * 0.82f

                // Start cycling animation (2 seconds)
                val startTime = System.currentTimeMillis()
                val duration = 2000L

                animRunnable = object : Runnable {
                    override fun run() {
                        val elapsed = System.currentTimeMillis() - startTime

                        if (elapsed < duration) {
                            overlay.text = generateRandomBountyText()

                            // Pulse effect
                            overlay.animate()
                                .scaleX(1.05f).scaleY(1.05f)
                                .setDuration(50)
                                .withEndAction {
                                    overlay.animate()
                                        .scaleX(1f).scaleY(1f)
                                        .setDuration(50)
                                        .start()
                                }
                                .start()

                            animHandler.postDelayed(this, 50)
                        } else {
                            // Animation finished - set final value and render
                            bountyValue = overlay.text.toString()
                            overlay.visibility = View.GONE
                            animRunnable = null
                            reRenderPoster()
                        }
                    }
                }
                animHandler.post(animRunnable!!)
            }
        }
    }

    /**
     * Patch ONLY the bounty text area on the current background.
     * This preserves baked stickers outside the text region.
     * The text area is repainted with original photo + overlay, then optionally new text.
     */
    private suspend fun patchTextArea(withText: Boolean): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Capture current imgBackground as bitmap (preserves baked stickers)
            val currentBg = withContext(Dispatchers.Main) {
                val w = binding.imgBackground.width
                val h = binding.imgBackground.height
                if (w == 0 || h == 0) return@withContext null
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val tempCanvas = Canvas(bitmap)
                binding.imgBackground.draw(tempCanvas)
                bitmap
            } ?: return@withContext null

            val posterWidth = currentBg.width
            val posterHeight = currentBg.height
            val canvas = Canvas(currentBg)

            // Calculate text dimensions for clipping
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3B2104")
                textSize = 40f * resources.displayMetrics.scaledDensity
                typeface = ResourcesCompat.getFont(this@EditStickerActivity, R.font.caslon_antique_regular)
                textAlign = Paint.Align.CENTER
            }
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textTopY = (posterHeight - textHeight) * 0.82f

            // Text area rect with padding
            val padding = textHeight * 0.5f
            val clipTop = (textTopY - padding).toInt().coerceAtLeast(0)
            val clipBottom = (textTopY + textHeight + padding).toInt().coerceAtMost(posterHeight)

            // Re-render original photo + overlay ONLY in the text area to erase old text
            val photoBitmap = BitmapFactory.decodeFile(originalPhotoPath)
            if (photoBitmap != null) {
                val photoW = (posterWidth * 0.87f).toInt()
                val photoH = (posterHeight * 0.46f).toInt()
                val photoX = (posterWidth - photoW) / 2f
                val photoY = (posterHeight - photoH) * 0.354f
                val croppedPhoto = centerCropBitmap(photoBitmap, photoW, photoH)

                val overlayBmp = BitmapFactory.decodeResource(resources, R.drawable.img_bounty_playing)
                val scaledOverlay = Bitmap.createScaledBitmap(overlayBmp, posterWidth, posterHeight, true)

                // Clip to text area only — everything outside is untouched
                canvas.save()
                canvas.clipRect(0, clipTop, posterWidth, clipBottom)
                canvas.drawBitmap(croppedPhoto, photoX, photoY, null)
                canvas.drawBitmap(scaledOverlay, 0f, 0f, null)
                canvas.restore()

                croppedPhoto.recycle()
                scaledOverlay.recycle()
                overlayBmp.recycle()
                photoBitmap.recycle()
            }

            // Draw new bounty text if requested
            if (withText) {
                val textX = posterWidth / 2f
                val textY = textTopY - textPaint.ascent()
                canvas.drawText(bountyValue, textX, textY, textPaint)
            }

            currentBg
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun reRenderPoster() {
        lifecycleScope.launch {
            val poster = patchTextArea(withText = true) ?: return@launch

            val tempFile = withContext(Dispatchers.IO) {
                File(cacheDir, "temp_rerender_${System.currentTimeMillis()}.jpg").also { file ->
                    FileOutputStream(file).use { out ->
                        poster.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    poster.recycle()
                }
            }

            currentImagePath = tempFile.absolutePath
            Glide.with(this@EditStickerActivity)
                .load(tempFile)
                .signature(com.bumptech.glide.signature.ObjectKey(tempFile.lastModified()))
                .into(binding.imgBackground)
        }
    }

    private fun centerCropBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val scale = maxOf(targetW / source.width.toFloat(), targetH / source.height.toFloat())
        val scaledW = (source.width * scale).toInt()
        val scaledH = (source.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        val x = (scaledW - targetW) / 2
        val y = (scaledH - targetH) / 2
        return Bitmap.createBitmap(scaledBitmap, x, y, targetW, targetH)
    }

    fun resetDraw() {
        drawViewList.clear()

    }

    private fun showResetConfirmation()
    {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.reset,
            description = R.string.change_your_whole_design_are_you_sure
        )

        dialog.onYesClick = {
            showInterAll { resetToInitialState() }
            dialog.dismiss()
        }

        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun resetToInitialState(){
        binding.drawView.removeAllDraw()
        drawViewList.clear()
        currentDraw = null
        // Reset bounty value and image path back to initial
        bountyValue = initialBountyValue
        currentImagePath = initialImagePath
        if (initialImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(initialImagePath))
                .signature(com.bumptech.glide.signature.ObjectKey(File(initialImagePath).lastModified()))
                .into(binding.imgBackground)
        }
    }

    override fun onRestart() {
        super.onRestart()
        initAds()
    }
    override fun initAds() {
        // Load native regular ad above back button and list
        // Load native collapsible ad at bottom
        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_editFilter), binding.nativeCollapEditSticker)
    }

    override fun onDestroy() {
        animRunnable?.let { animHandler.removeCallbacks(it) }
        animRunnable = null
        super.onDestroy()
    }
}
