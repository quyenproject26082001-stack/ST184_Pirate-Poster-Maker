package com.piratemaker.postermaker.poster.activity_app.editsticker

import android.content.Intent
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.template.CenterZoomLayoutManager
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

    override fun initView() {
        // Get image path from intent
        currentImagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        // Load background image
        if (currentImagePath.isNotEmpty()) {
            Glide.with(this)
                .load(File(currentImagePath))
                .centerInside()
                .into(binding.imgBackground)
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
            tvCenter.visible()

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

        val layoutManager = CenterZoomLayoutManager(this)
        binding.rvCategories.layoutManager = layoutManager
        binding.rvCategories.adapter = categoryAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvCategories)

        binding.rvCategories.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateSelectedCategoryFromCenter(recyclerView, snapHelper)
                }
            }
        })
    }

    private fun setupStickerGrid() {
        stickerAdapter = StickerItemAdapter { stickerPath ->
            addStickerToCanvas(stickerPath)
        }

        binding.rvStickers.layoutManager = GridLayoutManager(this, 4)
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

    private fun updateSelectedCategoryFromCenter(recyclerView: RecyclerView, snapHelper: LinearSnapHelper) {
        val layoutManager = recyclerView.layoutManager ?: return
        val snappedView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snappedView)

        val newCategoryId = position + 1

        if (currentCategoryId != newCategoryId) {
            currentCategoryId = newCategoryId
            categoryAdapter.setSelectedPosition(position)
            loadStickersForCategory(newCategoryId)
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
                    // Create ImageView for sticker
                    val stickerView = ImageView(this@EditStickerActivity).apply {
                        setImageBitmap(bitmap)
                        scaleType = ImageView.ScaleType.FIT_CENTER

                        // Set initial size (25% of canvas)
                        val size = (binding.stickerCanvas.width * 0.25f).toInt()
                        layoutParams = FrameLayout.LayoutParams(size, size).apply {
                            gravity = Gravity.CENTER
                        }

                        // Add touch handling for move/scale/rotate
                        setOnTouchListener(StickerTouchListener(this@EditStickerActivity))
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
