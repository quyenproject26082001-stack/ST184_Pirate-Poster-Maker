package com.charactor.avatar.maker.pfp.activity_app.makescreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.success.SuccessActivity
import com.charactor.avatar.maker.pfp.activity_app.template.TemplateListActivity
import com.charactor.avatar.maker.pfp.activity_app.wanted.WantedEditorActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.AssetHelper
import com.charactor.avatar.maker.pfp.core.helper.MediaHelper
import com.charactor.avatar.maker.pfp.core.helper.ShadowTransformation
import com.charactor.avatar.maker.pfp.core.utils.state.SaveState
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivityMakeScreenBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MakeScreen Activity
 * Main screen after clicking "Create" button from Home
 * Shows poster preview with 3 action buttons: Templates, Import, Edit
 */
class MakeScreenActivity : BaseActivity<ActivityMakeScreenBinding>() {

    // Use shared ViewModel for data binding with WantedEditorActivity
    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Dynamic poster views (inflated from template layouts)
    private var imgTemplate: ImageView? = null
    private var imgTemplateShadow: ImageView? = null
    private var imgAvatar: ImageView? = null
    private var imgAvatarShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedImageUri(it)
            // Mark editing started to switch from avatar.png to item.png
            viewModel.markEditingStarted()
            // Refresh entire preview with item.png and all elements visible
            updatePreviewWithCurrentState()
        }
    }

    // Request code for Edit button (data is shared via ViewModel)
    private val editActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Data is already updated in shared ViewModel, just refresh UI
            updatePreviewWithCurrentState()
        }
    }

    // Request code for Template selection
    private val templateSelectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val selectedTemplateId = data.getIntExtra("selectedTemplateId", 1)
                viewModel.setSelectedTemplate(selectedTemplateId)
                updatePreviewWithCurrentState()
            }
        }
    }

    override fun setViewBinding(): ActivityMakeScreenBinding {
        return ActivityMakeScreenBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Load template layout and background from assets
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Initialize with default template and preview
        updatePreviewWithCurrentState()
    }

    /**
     * Get layout resource ID for template
     */
    private fun getTemplateLayoutResId(templateId: Int): Int {
        return when (templateId) {
            1 -> R.layout.layout_poster_template_1
            2 -> R.layout.layout_poster_template_2
            3 -> R.layout.layout_poster_template_3
            4 -> R.layout.layout_poster_template_4
            5 -> R.layout.layout_poster_template_5
            6 -> R.layout.layout_poster_template_6
            7 -> R.layout.layout_poster_template_7
            8 -> R.layout.layout_poster_template_8
            9 -> R.layout.layout_poster_template_9
            10 -> R.layout.layout_poster_template_10
            11 -> R.layout.layout_poster_template_11
            12 -> R.layout.layout_poster_template_12
            13 -> R.layout.layout_poster_template_13
            14 -> R.layout.layout_poster_template_14
            15 -> R.layout.layout_poster_template_15
            16 -> R.layout.layout_poster_template_16
            else -> R.layout.layout_poster_template_1
        }
    }

    /**
     * Inflate template layout and bind views
     */
    private fun inflateTemplateLayout(templateId: Int) {
        // Remove old layout
        binding.containerPoster.removeAllViews()

        // Inflate new layout
        val layoutResId = getTemplateLayoutResId(templateId)
        val posterView = layoutInflater.inflate(layoutResId, binding.containerPoster, true)

        // Bind views
        imgTemplate = posterView.findViewById(R.id.imgTemplate)
        imgTemplateShadow = posterView.findViewById(R.id.imgTemplateShadow)
        imgAvatar = posterView.findViewById(R.id.imgAvatar)
        imgAvatarShadow = posterView.findViewById(R.id.imgAvatarShadow)
        tvName = posterView.findViewById(R.id.tvName)
        tvBounty = posterView.findViewById(R.id.tvBounty)

        // Load template background
        loadTemplateBackground()
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { handleBack() }
                btnActionBarRightText.setOnSingleClick { handleSave() }
            }

            // Templates button - Navigate to Template Selection Screen
            cvTemplates.setOnSingleClick {
                val intent = Intent(this@MakeScreenActivity, TemplateListActivity::class.java).apply {
                    putExtra("currentTemplateId", viewModel.selectedTemplate.value)
                }
                templateSelectionLauncher.launch(intent)
            }

            // Import button - Pick image from gallery
            cvImport.setOnSingleClick {
                pickImageLauncher.launch("image/*")
            }

            // Edit button - Navigate to WantedEditorActivity
            cvEdit.setOnSingleClick {
                navigateToEditor()
            }
        }
    }

    override fun dataObservable() {
        // Observe selected image URI
        lifecycleScope.launch {
            viewModel.selectedImageUri.collect { uri ->
                uri?.let {
                    loadImageToPreview(it)
                }
            }
        }

        // Observe name text
        lifecycleScope.launch {
            viewModel.nameText.collect { text ->
                tvName?.text = text
            }
        }

        // Observe bounty text
        lifecycleScope.launch {
            viewModel.bountyText.collect { text ->
                tvBounty?.text = text
            }
        }

        // Observe poster shadow
        lifecycleScope.launch {
            viewModel.posterShadow.collect { shadowValue ->
                applyPosterShadow(shadowValue)
            }
        }

        // Observe photo shadow
        lifecycleScope.launch {
            viewModel.filterShadow.collect { shadowValue ->
                applyPhotoShadow(shadowValue)
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvCenter.text = strings(R.string.wanted_poster_maker)
            tvCenter.visible()
            btnActionBarRight.gone()
            btnActionBarReset.gone()
        }
    }

    /**
     * Handle back button with "Discard changes?" dialog if edited
     */
    private fun handleBack() {
        if (viewModel.hasChanges.value) {
            // TODO: Show dialog "Discard changes?"
            // For now, just finish
            finishAfterTransition()
        } else {
            finishAfterTransition()
        }
    }

    /**
     * Handle save button - Save poster and navigate to SuccessActivity
     */
    private fun handleSave() {
        // Capture poster view as bitmap
        val posterView = binding.containerPoster
        if (posterView.width == 0 || posterView.height == 0) {
            showToast(strings(R.string.download_failed_please_try_again_later))
            return
        }

        val bitmap = Bitmap.createBitmap(posterView.width, posterView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        posterView.draw(canvas)

        // Save bitmap to internal storage
        lifecycleScope.launch {
            MediaHelper.saveBitmapToInternalStorage(this@MakeScreenActivity, "posters", bitmap)
                .collectLatest { state ->
                    when (state) {
                        is SaveState.Success -> {
                            // Set path in ViewModel and navigate to SuccessActivity
                            viewModel.setSavedImagePath(state.path)
                            val intent = Intent(this@MakeScreenActivity, SuccessActivity::class.java)
                            startActivity(intent)
                        }
                        is SaveState.Error -> {
                            showToast(strings(R.string.download_failed_please_try_again_later))
                        }
                        SaveState.Loading -> {
                            // Show loading indicator if needed
                        }
                    }
                }
        }
    }

    /**
     * Navigate to WantedEditorActivity
     * Data is shared via PosterEditorSharedViewModel - no Intent extras needed
     */
    private fun navigateToEditor() {
        val intent = Intent(this, WantedEditorActivity::class.java)
        editActivityLauncher.launch(intent)
    }

    /**
     * Load template background from assets
     * Uses avatar.png when no edits, item.png after user edits
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val isEditing = viewModel.isEditingStarted.value

        // Use avatar.png for initial preview, item.png after editing
        val templatePath = if (isEditing) {
            AssetHelper.getTemplateItemPath(templateId)
        } else {
            AssetHelper.getTemplateAvatarPath(templateId)
        }

        android.util.Log.d("MakeScreen", "Loading template: $templateId, isEditing: $isEditing, path: $templatePath")

        imgTemplate?.let { imageView ->
            Glide.with(this)
                .load(templatePath)
                .error(R.drawable.template)
                .into(imageView)
        }
    }

    /**
     * Load default avatar image from assets
     */
    private fun loadDefaultAvatar() {
        val templateId = viewModel.selectedTemplate.value
        val avatarPath = AssetHelper.getTemplateAvatarPath(templateId)

        imgAvatar?.let { imageView ->
            Glide.with(this)
                .load(avatarPath)
                .centerCrop()
                .into(imageView)
        }
    }

    /**
     * Reload image with blur transformation (for Android 8-11)
     */
    private fun reloadImageWithBlur(uri: Uri, blurValue: Float) {
        imgAvatar?.let { imageView ->
            if (blurValue > 0) {
                val blurRadius = (blurValue / 100f * 25f).toInt().coerceAtLeast(1)
                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3))
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    /**
     * Load image into preview
     */
    private fun loadImageToPreview(uri: Uri) {
        // For Android 8-11: Apply blur via Glide transformation
        // For Android 12+: Apply blur via RenderEffect in applyPhotoFilters()
        val blur = viewModel.filterBlur.value
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && blur > 0) {
            reloadImageWithBlur(uri, blur)
        } else {
            // Load into main avatar
            imgAvatar?.let { imageView ->
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }
        }

        // Load into shadow layer with ShadowTransformation (contour shadow)
        val shadowRadius = viewModel.filterShadow.value / 100f * 15f
        val shadowAlpha = 0.8f

        imgAvatarShadow?.let { imageView ->
            Glide.with(this)
                .load(uri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(imageView)
        }
    }

    /**
     * Update preview with current ViewModel state
     * Called after receiving edited data from WantedEditorActivity
     */
    private fun updatePreviewWithCurrentState() {
        // Reload template layout if template changed
        val currentTemplateId = viewModel.selectedTemplate.value
        inflateTemplateLayout(currentTemplateId)

        val isEditing = viewModel.isEditingStarted.value
        val config = viewModel.getConfig()

        // Show/hide editable elements based on editing state
        if (isEditing) {
            // Show name only if template has name field (XML already sets visibility)
            // Just update text values
            if (config.hasName) {
                tvName?.visibility = View.VISIBLE
                tvName?.text = viewModel.nameText.value
            } else {
                tvName?.visibility = View.GONE
            }

            tvBounty?.visibility = View.VISIBLE
            tvBounty?.text = viewModel.bountyText.value

            imgAvatar?.visibility = View.VISIBLE
            imgAvatarShadow?.visibility = View.VISIBLE

            // Update image if exists, otherwise show default avatar
            viewModel.selectedImageUri.value?.let { uri ->
                loadImageToPreview(uri)
            } ?: loadDefaultAvatar()

            // Apply ALL effects from ViewModel to match Editor
            applyAllEffectsFromViewModel()
        } else {
            // Hide all editable elements - show only avatar.png preview
            tvName?.visibility = View.GONE
            tvBounty?.visibility = View.GONE
            imgAvatar?.visibility = View.GONE
            imgAvatarShadow?.visibility = View.GONE
            imgTemplateShadow?.visibility = View.GONE
        }
    }

    /**
     * Apply all effects from ViewModel to preview
     * This ensures MakeScreen preview matches Editor preview exactly
     */
    private fun applyAllEffectsFromViewModel() {
        val config = viewModel.getConfig()

        // Apply template colors and base sizes
        try {
            if (config.hasName) {
                tvName?.setTextColor(Color.parseColor(config.nameColor))
                tvName?.textSize = config.nameSize
            }
            tvBounty?.setTextColor(Color.parseColor(config.bountyColor))
            // Note: bounty size is overridden below by user's custom size
        } catch (e: Exception) {
            tvName?.setTextColor(Color.BLACK)
            tvBounty?.setTextColor(Color.BLACK)
        }

        // Name effects
        tvName?.letterSpacing = viewModel.nameSpacing.value
        // TODO: Apply font typeface (need to map font name to resource)

        // Bounty effects (override config values with user's customization)
        tvBounty?.textSize = viewModel.bountySize.value
        tvBounty?.letterSpacing = viewModel.bountySpacing.value
        tvBounty?.translationX = viewModel.bountyPositionX.value
        tvBounty?.translationY = viewModel.bountyPositionY.value

        // Photo filters
        applyPhotoFilters()

        // Shadows
        applyPosterShadow(viewModel.posterShadow.value)
        applyPhotoShadow(viewModel.filterShadow.value)
    }

    /**
     * Apply photo filters to imgAvatar
     * EXACTLY LIKE WantedEditorActivity.applyFilters()
     */
    private fun applyPhotoFilters() {
        val brightness = viewModel.filterBrightness.value
        val contrast = viewModel.filterContrast.value
        val saturation = viewModel.filterSaturate.value
        val grayscale = viewModel.filterGrayscale.value
        val hueRotate = viewModel.filterHueRotate.value
        val sepia = viewModel.filterSepia.value
        val blur = viewModel.filterBlur.value

        val colorMatrix = android.graphics.ColorMatrix()

        // Brightness
        val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(brightnessMatrix)

        // Contrast
        val scale = contrast
        val translate = (1f - contrast) / 2f * 255f
        val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(contrastMatrix)

        // Saturation
        val saturationMatrix = android.graphics.ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)

        // Grayscale
        if (grayscale > 0) {
            val invGrayscale = 1 - grayscale
            val grayscaleMatrix = android.graphics.ColorMatrix(floatArrayOf(
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

            val hueRotateMatrix = android.graphics.ColorMatrix(floatArrayOf(
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
            val sepiaMatrix = android.graphics.ColorMatrix(floatArrayOf(
                invSepia + sepia * 0.393f, sepia * 0.769f, sepia * 0.189f, 0f, 0f,
                sepia * 0.349f, invSepia + sepia * 0.686f, sepia * 0.168f, 0f, 0f,
                sepia * 0.272f, sepia * 0.534f, invSepia + sepia * 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(sepiaMatrix)
        }

        // Apply ColorMatrix filter
        imgAvatar?.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)

        // Apply Blur
        // Android 12+: Use RenderEffect (fast)
        // Android 8-11: Handled by reloadImageWithBlur() using Glide transformation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (blur > 0) {
                val blurRadius = blur / 100f * 25f // Max blur radius 25
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP
                )
                imgAvatar?.setRenderEffect(blurEffect)
            } else {
                imgAvatar?.setRenderEffect(null)
            }
        } else if (blur > 0) {
            // For Android 8-11: Reload image with blur transformation
            val currentUri = viewModel.selectedImageUri.value
            if (currentUri != null) {
                reloadImageWithBlur(currentUri, blur)
            }
        }
    }

    /**
     * Apply poster shadow effect (template shadow)
     * EXACTLY LIKE WantedEditorActivity.applyTemplateShadow()
     */
    private fun applyPosterShadow(shadowValue: Float) {
        val shadowView = imgTemplateShadow ?: return

        if (shadowValue <= 0) {
            shadowView.visibility = View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        shadowView.visibility = View.VISIBLE

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)

        // Reload with new transformation parameters
        val shadowRadius = effectiveShadowValue / 100f * 15f  // 35-100 → 5.25-15px
        val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // View properties
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha

        val offsetX = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f  // 35-100 → 3.5-10px
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        }
    }

    /**
     * Apply photo shadow effect (avatar shadow)
     * EXACTLY LIKE WantedEditorActivity.applyShadowEffect()
     */
    private fun applyPhotoShadow(shadowValue: Float) {
        val shadowView = imgAvatarShadow ?: return

        if (shadowValue <= 0) {
            shadowView.visibility = View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        shadowView.visibility = View.VISIBLE

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)

        // Reload shadow with new transformation
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f  // 35-100 → 5.25-15px
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        }

        // View properties
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha

        val offsetX = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f  // 35-100 → 3.5-10px
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

