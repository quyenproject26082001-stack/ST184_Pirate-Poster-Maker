package com.piratemaker.postermaker.poster.activity_app.sticker_customize

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.editsticker.StickerCategory
import com.piratemaker.postermaker.poster.activity_app.editsticker.StickerCategoryAdapter
import com.piratemaker.postermaker.poster.activity_app.editsticker.StickerItemAdapter
import com.piratemaker.postermaker.poster.activity_app.maker_success.SuccessActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.draw.Draw
import com.piratemaker.postermaker.poster.core.draw.DrawableDraw
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.setTextActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.AssetHelper
import com.piratemaker.postermaker.poster.core.helper.BitmapHelper
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.listener.listenerdraw.OnDrawListener
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.SaveState
import com.piratemaker.postermaker.poster.databinding.ActivityStickerCustomizeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class StickerCustomizeActivity : BaseActivity<ActivityStickerCustomizeBinding>() {

    private lateinit var categoryAdapter: StickerCategoryAdapter
    private lateinit var stickerAdapter: StickerItemAdapter
    private var currentDraw: Draw? = null
    private var touchedAnyDraw = false

    override fun setViewBinding(): ActivityStickerCustomizeBinding {
        return ActivityStickerCustomizeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initDrawView()
        setupCategoryNavigation()
        setupStickerGrid()
        loadStickersForCategory(DEFAULT_CATEGORY_ID)
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.sticker))
            setImageActionBar(btnActionBarRight, R.drawable.ic_done)
            btnActionBarRight.visible()
            btnActionBarReset.gone()
            btnActionBarRightText.gone()
            tvRightText.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.setOnSingleClick {
            showInterAll { handleBackLeftToRight() }
        }
        binding.actionBar.btnActionBarRight.setOnSingleClick(1000) {
            handleSave()
        }
    }

    private fun handleSave() {
        lifecycleScope.launch {
            showLoading()
            delay(200)
            binding.drawView.hideSelect()
            val bitmap = BitmapHelper.createBimapFromView(binding.flSave)

            MediaHelper.saveBitmapToInternalStorage(this@StickerCustomizeActivity, "bounty_designs", bitmap)
                .collect { result ->
                    when (result) {
                        is SaveState.Loading -> showLoading()
                        is SaveState.Error -> {
                            dismissLoading(true)
                            showToast(R.string.save_failed_please_try_again)
                        }
                        is SaveState.Success -> {
                            writeDesignSourceMetadata(result.path, "sticker_customize")
                            val intent = Intent(this@StickerCustomizeActivity, SuccessActivity::class.java).apply {
                                putExtra(IntentKey.INTENT_KEY, result.path)
                                putExtra(IntentKey.STATUS_KEY, ValueKey.MY_DESIGN_TYPE)
                            }
                            val options = ActivityOptions.makeCustomAnimation(
                                this@StickerCustomizeActivity,
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                            dismissLoading(true)
                            showInterAll { startActivity(intent, options.toBundle()) }
                        }
                    }
                }
        }
    }

    private fun writeDesignSourceMetadata(imagePath: String, source: String) {
        runCatching {
            val imageFile = File(imagePath)
            val metadataFile = File(imageFile.parent, "${imageFile.nameWithoutExtension}.json")
            metadataFile.writeText(
                buildString {
                    append("{\n")
                    append("  \"source\": \"$source\"\n")
                    append("}")
                }
            )
        }
    }

    private fun setupCategoryNavigation() {
        val categories = AssetHelper.getAllStickerCategories().map { StickerCategory(it) }
        categoryAdapter = StickerCategoryAdapter(categories, DEFAULT_CATEGORY_ID) { categoryId ->
            loadStickersForCategory(categoryId)
        }

        binding.rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
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
            val stickers = AssetHelper.getStickersByCategory(this@StickerCustomizeActivity, categoryId)
            withContext(Dispatchers.Main) {
                stickerAdapter.updateStickers(stickers)
            }
        }
    }

    private fun addDrawable(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmapDefault = Glide.with(this@StickerCustomizeActivity)
                .load(path)
                .override(400, 400)
                .submit()
                .get()
                .toBitmap()

            withContext(Dispatchers.Main) {
                binding.drawView.addDraw(loadDrawableEmoji(this@StickerCustomizeActivity, bitmapDefault))
            }

            val current = binding.drawView.getCurrentDraw()
            val values = FloatArray(9)
            current?.getMatrix()?.getValues(values)
            Log.d("StickerCustomize", "Initial scale: ${values[Matrix.MSCALE_X]}")
        }
    }

    private fun initDrawView() {
        binding.drawView.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                touchedAnyDraw = false
                binding.drawView.post {
                    if (!touchedAnyDraw) {
                        binding.drawView.hideSelect()
                        currentDraw = null
                    }
                }
            }
            false
        }

        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    currentDraw = draw
                }

                override fun onClickedDraw(draw: Draw) {
                    currentDraw = draw
                }

                override fun onDeletedDraw(draw: Draw) {
                    if (currentDraw == draw) currentDraw = null
                }

                override fun onDragFinishedDraw(draw: Draw) {}
                override fun onTouchedDownDraw(draw: Draw) {
                    touchedAnyDraw = true
                    currentDraw = draw
                }

                override fun onZoomFinishedDraw(draw: Draw) {}
                override fun onFlippedDraw(draw: Draw) {}
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

    private fun loadDrawableEmoji(context: Context, bitmap: Bitmap): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        return DrawableDraw(drawable, "${SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())}.png")
    }

    companion object {
        private const val DEFAULT_CATEGORY_ID = 2
    }
    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(
            this,
            getString(R.string.native_collap_stickerMaker),
            binding.flNativeCollab
        )
    }

    override fun initAds() {
        initNativeCollab()
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }
}
