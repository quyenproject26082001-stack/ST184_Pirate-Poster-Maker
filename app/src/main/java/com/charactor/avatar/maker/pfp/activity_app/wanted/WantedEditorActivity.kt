package com.charactor.avatar.maker.pfp.activity_app.wanted

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.BackgroundRemovalHelper
import com.charactor.avatar.maker.pfp.core.helper.BitmapHelper
import com.charactor.avatar.maker.pfp.core.helper.ShadowTransformation
import com.charactor.avatar.maker.pfp.databinding.ActivityWantedEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WantedEditorActivity : BaseActivity<ActivityWantedEditorBinding>() {

    private val viewModel: WantedEditorViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedImageUri(it)
            // Load image into both avatar and shadow ImageViews
            loadImageToAvatars(it)
        }
    }

    private val fontList = listOf(
        "Roboto Bold" to R.font.roboto_bold,
        "Roboto Medium" to R.font.roboto_medium,
        "Roboto Regular" to R.font.roboto_regular,
        "Londrina Solid" to R.font.londrina_solid_regular,
        "Montserrat Bold" to R.font.montserrat_bold,
        "Montserrat Medium" to R.font.montserrat_medium
    )

    override fun setViewBinding(): ActivityWantedEditorBinding {
        return ActivityWantedEditorBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        setupFontSpinners()
        setupSeekBars()
        setupEditTexts()
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { handleBackLeftToRight() }
                btnActionBarRight.setOnSingleClick { handleSave() }
            }

            // Import photo button
            btnImportPhoto.setOnSingleClick {
                pickImageLauncher.launch("image/*")
            }

            // Remove background button
            btnRemoveBackground.setOnSingleClick {
                handleRemoveBackground()
            }

            // Name section toggle
            layoutNameHeader.setOnSingleClick {
                viewModel.toggleNameSection()
            }

            // Bounty section toggle
            layoutBountyHeader.setOnSingleClick {
                viewModel.toggleBountySection()
            }

            // Photo Filter section toggle
            layoutPhotoFilterHeader.setOnSingleClick {
                viewModel.togglePhotoFilterSection()
            }

            // Poster Shadow section toggle
            layoutPosterShadowHeader.setOnSingleClick {
                viewModel.togglePosterShadowSection()
            }

            // Reset button
            btnReset.setOnSingleClick {
                viewModel.resetAll()
            }
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            viewModel.selectedImageUri.collect { uri ->
                uri?.let {
                    loadImageToAvatars(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isNameSectionExpanded.collect { isExpanded ->
                binding.layoutNameContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgNameArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isBountySectionExpanded.collect { isExpanded ->
                binding.layoutBountyContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgBountyArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isPhotoFilterSectionExpanded.collect { isExpanded ->
                binding.layoutPhotoFilterContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgPhotoFilterArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isPosterShadowSectionExpanded.collect { isExpanded ->
                binding.layoutPosterShadowContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgPosterShadowArrow.rotation = if (isExpanded) 180f else 0f
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = strings(R.string.wanted_poster_maker)
            tvCenter.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()
        }
    }

    private fun handleSave() {
        // TODO: Implement save functionality
        showToast("Saving poster...")
    }

    private fun setupFontSpinners() {
        val fontNames = fontList.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerNameFont.adapter = adapter
        binding.spinnerNameFont.setSelection(0)
        binding.spinnerNameFont.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val fontRes = fontList[position].second
                val typeface = ResourcesCompat.getFont(this@WantedEditorActivity, fontRes)
                binding.tvName.typeface = typeface
                viewModel.setNameFont(fontList[position].first)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupEditTexts() {
        binding.edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: "NAME HERE"
                binding.tvName.text = text
                viewModel.setNameText(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.edtBounty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: "$2,000,000"
                binding.tvBounty.text = text
                viewModel.setBountyText(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSeekBars() {
        // Name Spacing
        binding.seekBarNameSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val spacing = progress / 10f
                binding.tvName.letterSpacing = spacing
                viewModel.setNameSpacing(spacing)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bounty Size
        binding.seekBarBountySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 12f + (progress / 100f) * 48f // 12sp to 60sp
                binding.tvBounty.textSize = size
                viewModel.setBountySize(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bounty Weight
        binding.seekBarBountyWeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Weight doesn't directly map to Android, but we can use different font styles
                // For simplicity, we'll just store the value
                viewModel.setBountyWeight(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bounty Spacing
        binding.seekBarBountySpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val spacing = progress / 10f
                binding.tvBounty.letterSpacing = spacing
                viewModel.setBountySpacing(spacing)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bounty Position X
        binding.seekBarBountyPositionX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val offsetX = (progress - 50) * 2f // -100 to 100
                binding.tvBounty.translationX = offsetX
                viewModel.setBountyPositionX(offsetX)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bounty Position Y
        binding.seekBarBountyPositionY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val offsetY = (progress - 50) * 2f // -100 to 100
                binding.tvBounty.translationY = offsetY
                viewModel.setBountyPositionY(offsetY)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        setupPhotoFilterSeekBars()
        setupPosterShadowSeekBar()
    }

    private fun setupPhotoFilterSeekBars() {
        // Shadow - Use dedicated shadow effect function
        binding.seekBarFilterShadow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setFilterShadow(progress.toFloat())
                applyShadowEffect(progress.toFloat())  // Use shadow layer approach instead of elevation
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Blur
        binding.seekBarFilterBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setFilterBlur(progress.toFloat())
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Brightness
        binding.seekBarFilterBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = progress / 100f // 0 to 2
                viewModel.setFilterBrightness(brightness)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Contrast
        binding.seekBarFilterContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrast = progress / 100f // 0 to 2
                viewModel.setFilterContrast(contrast)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Grayscale
        binding.seekBarFilterGrayscale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val grayscale = progress / 100f // 0 to 1
                viewModel.setFilterGrayscale(grayscale)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Hue Rotate
        binding.seekBarFilterHueRotate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val hueRotate = progress.toFloat() // 0 to 360
                viewModel.setFilterHueRotate(hueRotate)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Saturate
        binding.seekBarFilterSaturate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val saturate = progress / 100f // 0 to 2
                viewModel.setFilterSaturate(saturate)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Sepia
        binding.seekBarFilterSepia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sepia = progress / 100f // 0 to 1
                viewModel.setFilterSepia(sepia)
                applyFilters()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPosterShadowSeekBar() {
        binding.seekBarPosterShadow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Control template shadow instead of CardView elevation
                applyTemplateShadow(progress.toFloat())
                viewModel.setPosterShadow(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Load image into both avatar and shadow ImageViews
     * Shadow uses ShadowTransformation to follow alpha channel (contour shadow like icon)
     */
    private fun loadImageToAvatars(uri: Uri) {
        // Load into main avatar with centerCrop
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.imgAvatar)

        // Load into shadow layer with ShadowTransformation
        // This creates shadow that follows the alpha channel/contour of the image like icon shadow
        val shadowRadius = viewModel.filterShadow.value / 100f * 15f
        val shadowAlpha = 0.8f

        Glide.with(this)
            .load(uri)
            .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
            .into(binding.imgAvatarShadow)

        // Re-enable shadow seekbar when loading new image
        binding.seekBarFilterShadow.isEnabled = true
    }

    /**
     * Apply shadow effect to imgAvatarShadow layer
     * Reloads shadow with transformation that follows alpha channel (contour shadow like icon)
     */
    private fun applyShadowEffect(shadowValue: Float) {
        if (shadowValue <= 0) {
            binding.imgAvatarShadow.visibility = android.view.View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.imgAvatarShadow.setRenderEffect(null)
            }
            return
        }

        binding.imgAvatarShadow.visibility = android.view.View.VISIBLE

        // Reload shadow with new transformation parameters
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = shadowValue / 100f * 15f // 0-15px blur in transformation
            val shadowAlpha = shadowValue / 100f * 0.9f

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(binding.imgAvatarShadow)
        }

        // 1. Alpha - overall shadow visibility
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        binding.imgAvatarShadow.alpha = viewAlpha

        // 2. Offset - shadow displacement (small for natural look)
        val offsetX = shadowValue / 100f * 5f   // 0-5dp
        val offsetY = shadowValue / 100f * 7f   // 0-7dp
        binding.imgAvatarShadow.translationX = offsetX
        binding.imgAvatarShadow.translationY = offsetY

        // 3. Scale - very minimal to maintain shape accuracy
        val scale = 1f + (shadowValue / 100f * 0.03f)  // 1.0 to 1.03
        binding.imgAvatarShadow.scaleX = scale
        binding.imgAvatarShadow.scaleY = scale

        // 4. Additional blur via RenderEffect (API 31+) for extra softness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f // 0-10px additional blur
            if (additionalBlur > 0) {
                val blurEffect = RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, Shader.TileMode.CLAMP
                )
                binding.imgAvatarShadow.setRenderEffect(blurEffect)
            } else {
                binding.imgAvatarShadow.setRenderEffect(null)
            }
        }
    }

    private fun applyFilters() {
        lifecycleScope.launch {
            val brightness = viewModel.filterBrightness.value
            val contrast = viewModel.filterContrast.value
            val saturation = viewModel.filterSaturate.value
            val grayscale = viewModel.filterGrayscale.value
            val hueRotate = viewModel.filterHueRotate.value
            val sepia = viewModel.filterSepia.value
            val blur = viewModel.filterBlur.value
            // Note: shadow is now handled separately by applyShadowEffect()

            val colorMatrix = ColorMatrix()

            // Brightness
            val brightnessMatrix = ColorMatrix(floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(brightnessMatrix)

            // Contrast
            val scale = contrast
            val translate = (1f - contrast) / 2f * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)

            // Saturation
            val saturationMatrix = ColorMatrix()
            saturationMatrix.setSaturation(saturation)
            colorMatrix.postConcat(saturationMatrix)

            // Grayscale
            if (grayscale > 0) {
                val invGrayscale = 1 - grayscale
                val grayscaleMatrix = ColorMatrix(floatArrayOf(
                    invGrayscale + grayscale * 0.299f, grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, invGrayscale + grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, grayscale * 0.587f, invGrayscale + grayscale * 0.114f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(grayscaleMatrix)
            }

            // Hue Rotation
            if (hueRotate != 0f) {
                val angle = hueRotate * Math.PI.toFloat() / 180f
                val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                val sinA = kotlin.math.sin(angle.toDouble()).toFloat()

                // Hue rotation matrix
                val hueRotateMatrix = ColorMatrix(floatArrayOf(
                    0.213f + cosA * 0.787f - sinA * 0.213f, 0.715f - cosA * 0.715f - sinA * 0.715f, 0.072f - cosA * 0.072f + sinA * 0.928f, 0f, 0f,
                    0.213f - cosA * 0.213f + sinA * 0.143f, 0.715f + cosA * 0.285f + sinA * 0.140f, 0.072f - cosA * 0.072f - sinA * 0.283f, 0f, 0f,
                    0.213f - cosA * 0.213f - sinA * 0.787f, 0.715f - cosA * 0.715f + sinA * 0.715f, 0.072f + cosA * 0.928f + sinA * 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(hueRotateMatrix)
            }

            // Sepia
            if (sepia > 0) {
                val invSepia = 1 - sepia
                val sepiaMatrix = ColorMatrix(floatArrayOf(
                    invSepia + sepia * 0.393f, sepia * 0.769f, sepia * 0.189f, 0f, 0f,
                    sepia * 0.349f, invSepia + sepia * 0.686f, sepia * 0.168f, 0f, 0f,
                    sepia * 0.272f, sepia * 0.534f, invSepia + sepia * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(sepiaMatrix)
            }

            // Apply ColorMatrix filter
            binding.imgAvatar.colorFilter = ColorMatrixColorFilter(colorMatrix)

            // Note: Shadow is now handled by applyShadowEffect() using shadow layer approach
            // Old elevation-based shadow code removed as it didn't work properly with ImageView

            // Apply Blur (requires API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (blur > 0) {
                    val blurRadius = blur / 100f * 25f // Max blur radius 25
                    val blurEffect = RenderEffect.createBlurEffect(
                        blurRadius, blurRadius, Shader.TileMode.CLAMP
                    )
                    binding.imgAvatar.setRenderEffect(blurEffect)
                } else {
                    binding.imgAvatar.setRenderEffect(null)
                }
            }
        }
    }

    /**
     * Handle remove background from current image
     */
    private fun handleRemoveBackground() {
        val currentUri = viewModel.selectedImageUri.value

        if (currentUri == null) {
            showToast("Please import a photo first")
            return
        }

        lifecycleScope.launch {
            try {
                // Show loading
                super.showLoading()
                setRemoveBackgroundButtonEnabled(false)

                // Convert URI to Bitmap
                val originalBitmap = withContext(Dispatchers.IO) {
                    BitmapHelper.uriToBitmap(this@WantedEditorActivity, currentUri)
                }

                if (originalBitmap == null) {
                    dismissLoading()
                    setRemoveBackgroundButtonEnabled(true)
                    showToast("Failed to load image")
                    return@launch
                }

                // Remove background using ML Kit
                val resultBitmap = withContext(Dispatchers.IO) {
                    BackgroundRemovalHelper.removeBackground(
                        this@WantedEditorActivity,
                        originalBitmap,
                        confidence = 0.5f
                    )
                }

                dismissLoading()
                setRemoveBackgroundButtonEnabled(true)

                if (resultBitmap != null) {
                    // Display result in avatar
                    Glide.with(this@WantedEditorActivity)
                        .load(resultBitmap)
                        .centerCrop()
                        .into(binding.imgAvatar)

                    // Load into shadow layer with ShadowTransformation
                    // Shadow will follow the contour of the person (no background)
                    val shadowRadius = 15f
                    val shadowAlpha = 0.8f
                    Glide.with(this@WantedEditorActivity)
                        .load(resultBitmap)
                        .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                        .into(binding.imgAvatarShadow)

                    // Keep shadow enabled - it will follow the contour of the person!
                    binding.seekBarFilterShadow.isEnabled = true
                    binding.imgAvatarShadow.visibility = android.view.View.VISIBLE

                    showToast("Background removed! Shadow follows the person contour.")
                } else {
                    showToast("Failed to remove background. Please try with a person photo.")
                }

            } catch (e: Exception) {
                dismissLoading()
                setRemoveBackgroundButtonEnabled(true)
                e.printStackTrace()
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun setRemoveBackgroundButtonEnabled(enabled: Boolean) {
        binding.btnRemoveBackground.isEnabled = enabled
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Apply shadow effect to template background
     * Controlled by Poster Shadow seekbar
     * FIXED: Increased blur, added RenderEffect for smoother shadow
     */
    private fun applyTemplateShadow(shadowValue: Float) {
        if (shadowValue <= 0) {
            binding.imgTemplateShadow.visibility = android.view.View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.imgTemplateShadow.setRenderEffect(null)
            }
            return
        }

        binding.imgTemplateShadow.visibility = android.view.View.VISIBLE

        // Reload template shadow with new parameters
        // INCREASED: More blur for smoother shadow like PhotoFilter
        val shadowRadius = shadowValue / 100f * 35f  // 0-35px blur (was 25)
        val shadowAlpha = shadowValue / 100f * 0.9f  // 0-0.9 alpha (was 0.8)

        Glide.with(this)
            .load(R.drawable.template)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(binding.imgTemplateShadow)

        // View properties
        val viewAlpha = (shadowValue / 100f * 0.8f).coerceIn(0f, 1f)
        binding.imgTemplateShadow.alpha = viewAlpha

        // Offset for depth effect - slightly increased
        val offsetX = shadowValue / 100f * 12f  // 0-12dp (was 10)
        val offsetY = shadowValue / 100f * 15f  // 0-15dp (was 12)
        binding.imgTemplateShadow.translationX = offsetX
        binding.imgTemplateShadow.translationY = offsetY

        // Slight scale
        val scale = 1f + (shadowValue / 100f * 0.05f)  // 1.0-1.05 (was 1.03)
        binding.imgTemplateShadow.scaleX = scale
        binding.imgTemplateShadow.scaleY = scale

        // ADDED: Additional blur with RenderEffect (API 31+) for extra smoothness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 15f  // 0-15px additional blur
            if (additionalBlur > 0) {
                val blurEffect = RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, Shader.TileMode.CLAMP
                )
                binding.imgTemplateShadow.setRenderEffect(blurEffect)
            } else {
                binding.imgTemplateShadow.setRenderEffect(null)
            }
        }
    }
}

