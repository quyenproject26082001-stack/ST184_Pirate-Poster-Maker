package com.piratemaker.postermaker.poster.activity_app.editsticker

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.AssetHelper
import com.piratemaker.postermaker.poster.core.helper.BitmapHelper
import com.piratemaker.postermaker.poster.databinding.ActivityEditStickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditStickerActivity : BaseActivity<ActivityEditStickerBinding>() {

    private lateinit var categoryAdapter: StickerCategoryAdapter
    private lateinit var stickerAdapter: StickerItemAdapter
    private var currentImagePath: String = ""
    private var currentCategoryId: Int = 1

    override fun setViewBinding(): ActivityEditStickerBinding {
        return ActivityEditStickerBinding.inflate(LayoutInflater.from(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        // Get image path from intent
        currentImagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        // Load background image
        if (currentImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(currentImagePath))
                .into(binding.imgBackground)
        }

        // Setup canvas touch to deselect stickers
        binding.stickerCanvas.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Deselect all stickers when canvas background is tapped
                for (i in 0 until binding.stickerCanvas.childCount) {
                    val child = binding.stickerCanvas.getChildAt(i)
                    if (child is StickerView) {
                        child.setStickerSelected(false)
                    }
                }
            }
            false // Allow touch events to propagate to children
        }

        setupCategoryNavigation()
        setupStickerGrid()
        loadStickersForCategory(1)
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            tvCenter.text = getString(R.string.add_sticker)
            tvCenter.gone()

            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()

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
                saveAndReturn()
            }
        }
    }

    private fun setupCategoryNavigation() {
        val categories = AssetHelper.getAllStickerCategories().map { StickerCategory(it) }

        categoryAdapter = StickerCategoryAdapter(categories, 1) { categoryId ->
            currentCategoryId = categoryId
            loadStickersForCategory(categoryId)
        }

        // Use normal horizontal LinearLayoutManager
        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvCategories.layoutManager = layoutManager
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupStickerGrid() {
        stickerAdapter = StickerItemAdapter { stickerPath ->
            addStickerToCanvas(stickerPath)
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

    private fun addStickerToCanvas(stickerPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Load sticker bitmap with Glide
                val bitmap = Glide.with(this@EditStickerActivity)
                    .asBitmap()
                    .load(stickerPath)
                    .submit(512, 512)
                    .get()

                withContext(Dispatchers.Main) {
                    // Deselect all other stickers first
                    for (i in 0 until binding.stickerCanvas.childCount) {
                        val child = binding.stickerCanvas.getChildAt(i)
                        if (child is StickerView) {
                            child.setStickerSelected(false)
                        }
                    }

                    // Create StickerView with handle box
                    val stickerView = StickerView(
                        this@EditStickerActivity,
                        bitmap
                    ).apply {
                        // Set initial size (25% of canvas)
                        val size = (binding.stickerCanvas.width * 0.25f).toInt()
                        layoutParams = FrameLayout.LayoutParams(size, size).apply {
                            gravity = Gravity.CENTER
                        }
                        tag = stickerPath
                        setStickerSelected(true)

                        // Set delete callback after view is created
                        setOnDeleteListener {
                            // Post removal to happen after touch event completes
                            binding.stickerCanvas.post {
                                binding.stickerCanvas.removeView(this)
                            }
                        }
                    }

                    // Add to canvas
                    binding.stickerCanvas.addView(stickerView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveAndReturn() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Deselect all stickers before capturing to avoid showing handle boxes
                withContext(Dispatchers.Main) {
                    for (i in 0 until binding.stickerCanvas.childCount) {
                        val child = binding.stickerCanvas.getChildAt(i)
                        if (child is StickerView) {
                            child.setStickerSelected(false)
                        }
                    }
                }

                // Small delay to ensure UI updates
                kotlinx.coroutines.delay(100)

                // Render entire canvas to bitmap
                val bitmap = BitmapHelper.createBimapFromView(binding.flCanvas)

                // Save to temp file
                val tempFile = File(cacheDir, "temp_edited_${System.currentTimeMillis()}.png")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Check if any stickers were added
                val hasStickers = binding.stickerCanvas.childCount > 0

                withContext(Dispatchers.Main) {
                    val resultIntent = Intent().apply {
                        putExtra("EDITED_IMAGE_PATH", tempFile.absolutePath)
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

    override fun initAds() {
        // Load native ad if needed
    }
}
