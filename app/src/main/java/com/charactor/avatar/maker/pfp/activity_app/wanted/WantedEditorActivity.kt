package com.charactor.avatar.maker.pfp.activity_app.wanted

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.adapter.FontItem
import com.charactor.avatar.maker.pfp.adapter.FontSelectorAdapter
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.AssetHelper
import com.charactor.avatar.maker.pfp.core.helper.BackgroundRemovalHelper
import com.charactor.avatar.maker.pfp.core.helper.BitmapHelper
import com.charactor.avatar.maker.pfp.core.helper.ShadowTransformation
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivityWantedEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WantedEditorActivity : BaseActivity<ActivityWantedEditorBinding>() {

    // Use shared ViewModel for data binding with MakeScreenActivity
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
            // Load image into both avatar and shadow ImageViews
            loadImageToAvatars(it)
        }
    }

    private val fontList = listOf(
        FontItem("Roboto Bold", R.font.roboto_bold),
        FontItem("Roboto Medium", R.font.roboto_medium),
        FontItem("Roboto Regular", R.font.roboto_regular),
        FontItem("Londrina Solid", R.font.londrina_solid_regular),
        FontItem("Montserrat Bold", R.font.montserrat_bold),
        FontItem("Montserrat Medium", R.font.montserrat_medium)
    )

    override fun setViewBinding(): ActivityWantedEditorBinding {
        return ActivityWantedEditorBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Inflate template layout dynamically
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // IMPORTANT: Setup listeners FIRST before restoring values
        // This ensures that when we restore values, listeners are already attached
        // and will trigger to update the preview (tvName, tvBounty, filters, etc.)
        setupFontSelector()
        setupSeekBars()
        setupEditTexts()

        // Apply text colors from template config
        applyTemplateColors()

        // Check if this is first time entering Editor (no edits yet)
        val isFirstTime = !viewModel.isEditingStarted.value

        if (isFirstTime) {
            // First time: Hide all editable elements, show only item.png template
            tvName?.visibility = View.GONE
            tvBounty?.visibility = View.GONE
            imgAvatar?.visibility = View.GONE
            imgAvatarShadow?.visibility = View.GONE
            imgTemplateShadow?.visibility = View.GONE
        } else {
            // Already editing: Show elements with current values
            val config = viewModel.getConfig()

            // Show name only if template has name field
            tvName?.visibility = if (config.hasName) View.VISIBLE else View.GONE
            tvBounty?.visibility = View.VISIBLE
            imgAvatar?.visibility = View.VISIBLE

            // Load current image if exists
            viewModel.selectedImageUri.value?.let { uri ->
                loadImageToAvatars(uri)
            }

            // RESTORE UI VALUES FROM VIEWMODEL
            // Since listeners are already setup, they will trigger and update the preview
            restoreUIFromViewModel()
        }
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

        // Enable auto-resize for tvName to handle long text + high spacing
        // Higher minTextSize (18sp) makes it PREFER wrapping over shrinking
        tvName?.setAutoSizeTextTypeUniformWithConfiguration(
            18,     // minTextSize: 18sp (prefer wrapping to multiple lines)
            28,     // maxTextSize: 28sp
            1,      // granularity: 1sp step
            android.util.TypedValue.COMPLEX_UNIT_SP
        )

        // Load template background
        loadTemplateBackground()
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { handleBackLeftToRight() }
                btnActionBarRight.setOnSingleClick { handleSave() }
                btnActionBarReset.setOnSingleClick { handleReset() }
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
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            viewModel.selectedImageUri.collect { uri ->
                uri?.let {
                    // Show imgAvatar when user imports an image
                    imgAvatar?.visibility = View.VISIBLE
                    loadImageToAvatars(it)
                }
            }
        }

        // Observe template config changes to show/hide sections
        lifecycleScope.launch {
            viewModel.currentConfig.collect { config ->
                // Show/hide Name section based on template config
                binding.layoutNameHeader.visibility = if (config.hasName) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutNameContent.visibility = android.view.View.GONE

                // Show/hide Bounty section based on template config
                binding.layoutBountyHeader.visibility = if (config.hasBounty) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutBountyContent.visibility = android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isNameSectionExpanded.collect { isExpanded ->
                // Only show content if section is expanded AND template has name
                val config = viewModel.getConfig()
                binding.layoutNameContent.visibility = if (isExpanded && config.hasName) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgNameArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isBountySectionExpanded.collect { isExpanded ->
                // Only show content if section is expanded AND template has bounty
                val config = viewModel.getConfig()
                binding.layoutBountyContent.visibility = if (isExpanded && config.hasBounty) android.view.View.VISIBLE else android.view.View.GONE
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
            tvCenter.gone()
            btnActionBarReset.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()
        }
    }

    private fun handleSave() {
        // Mark editing as started if user made any changes
        // This will switch MakeScreen from avatar.png to item.png display
        if (viewModel.hasChanges.value) {
            viewModel.markEditingStarted()
        }

        // Data is already in shared ViewModel - MakeScreenActivity will automatically have access
        setResult(RESULT_OK)
        finish()
    }

    /**
     * Apply text colors from template config
     * Each template has specific colors for name and bounty text
     */
    private fun applyTemplateColors() {
        val config = viewModel.getConfig()

        try {
            // Apply name color
            if (config.hasName) {
                tvName?.setTextColor(Color.parseColor(config.nameColor))
            }

            // Apply bounty color
            tvBounty?.setTextColor(Color.parseColor(config.bountyColor))

            // Apply text sizes
            tvName?.textSize = config.nameSize
            tvBounty?.textSize = config.bountySize
        } catch (e: Exception) {
            // Fallback to default colors if parsing fails
            tvName?.setTextColor(Color.BLACK)
            tvBounty?.setTextColor(Color.BLACK)
        }
    }

    /**
     * Restore all UI values from ViewModel
     * Called when returning to Editor after saving
     *
     * IMPORTANT: This must be called AFTER setupSeekBars() and setupEditTexts()
     * so that listeners are already attached and will trigger when values are set
     */
    private fun restoreUIFromViewModel() {
        // Restore EditText values
        // TextWatcher will trigger and update tvName/tvBounty automatically
        binding.edtName.setText(viewModel.nameText.value)
        binding.edtBounty.setText(viewModel.bountyText.value)

        // Restore Name section
        // Listener will trigger and apply letterSpacing to tvName
        binding.seekBarNameSpacing.progress = (viewModel.nameSpacing.value * 500f).toInt()  // spacing 0-0.2 → progress 0-100

        // Restore Bounty section
        // Listeners will trigger and apply size/spacing/position to tvBounty
        val bountySizeProgress = ((viewModel.bountySize.value - 12f) / 48f * 100f).toInt()
        binding.seekBarBountySize.progress = bountySizeProgress

        binding.seekBarBountyWeight.progress = viewModel.bountyWeight.value.toInt()

        binding.seekBarBountySpacing.progress = (viewModel.bountySpacing.value * 500f).toInt()  // spacing 0-0.2 → progress 0-100

        binding.seekBarBountyPositionX.progress = ((viewModel.bountyPositionX.value / 2f) + 50).toInt()

        binding.seekBarBountyPositionY.progress = ((viewModel.bountyPositionY.value / 2f) + 50).toInt()

        // Restore Photo Filter section
        // Listeners will trigger and apply shadow/blur/filters to imgAvatar
        binding.seekBarFilterShadow.progress = viewModel.filterShadow.value.toInt()

        binding.seekBarFilterBlur.progress = viewModel.filterBlur.value.toInt()

        binding.seekBarFilterBrightness.progress = (viewModel.filterBrightness.value * 100f).toInt()

        binding.seekBarFilterContrast.progress = (viewModel.filterContrast.value * 100f).toInt()

        binding.seekBarFilterGrayscale.progress = (viewModel.filterGrayscale.value * 100f).toInt()

        binding.seekBarFilterHueRotate.progress = viewModel.filterHueRotate.value.toInt()

        binding.seekBarFilterSaturate.progress = (viewModel.filterSaturate.value * 100f).toInt()

        binding.seekBarFilterSepia.progress = (viewModel.filterSepia.value * 100f).toInt()

        // Restore Poster Shadow section
        // Listener will trigger and apply template shadow
        binding.seekBarPosterShadow.progress = viewModel.posterShadow.value.toInt()

        // Font restoration is handled by setupFontSelector()
    }

    /**
     * Handle reset button - Reset all values to default
     */
    private fun handleReset() {
        // Reset ViewModel data to template config defaults
        viewModel.resetAll()

        // Reset UI components to match ViewModel defaults (from template config)
        binding.apply {
            // Reset EditTexts to template config defaults (NOT hardcoded values!)
            edtName.setText(viewModel.nameText.value)
            edtBounty.setText(viewModel.bountyText.value)

            // Reset Name section
            tvCurrentNameFont.text = fontList[0].name
            val initialTypeface = ResourcesCompat.getFont(this@WantedEditorActivity, fontList[0].fontResId)
            tvName?.typeface = initialTypeface
            tvCurrentNameFont.typeface = initialTypeface
            seekBarNameSpacing.progress = 0

            // Reset Bounty section
            // Calculate progress from config bounty size (12-60sp range)
            val bountySizeProgress = ((viewModel.bountySize.value - 12f) / 48f * 100f).toInt()
            seekBarBountySize.progress = bountySizeProgress
            seekBarBountyWeight.progress = 0
            seekBarBountySpacing.progress = 0
            seekBarBountyPositionX.progress = 50 // Center (0f offset)
            seekBarBountyPositionY.progress = 50 // Center (0f offset)

            // Reset Photo Filter section
            seekBarFilterShadow.progress = 0
            seekBarFilterBlur.progress = 0
            seekBarFilterBrightness.progress = 100 // 1f = 100%
            seekBarFilterContrast.progress = 100 // 1f = 100%
            seekBarFilterGrayscale.progress = 0
            seekBarFilterHueRotate.progress = 0
            seekBarFilterSaturate.progress = 100 // 1f = 100%
            seekBarFilterSepia.progress = 0

            // Reset Poster Shadow section
            seekBarPosterShadow.progress = 0
        }

        // Reset TextViews to template config defaults
        // Note: EditText listeners will also update these, but we set them here for immediate feedback
        tvName?.text = viewModel.nameText.value
        tvBounty?.text = viewModel.bountyText.value
        tvName?.letterSpacing = 0f
        tvBounty?.textSize = viewModel.bountySize.value  // Use config default size
        tvBounty?.letterSpacing = 0f
        tvBounty?.translationX = 0f
        tvBounty?.translationY = 0f

        // Apply filter reset
        imgAvatar?.colorFilter = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            imgAvatar?.setRenderEffect(null)
        }

        // Reset shadow effects
        applyShadowEffect(0f)
        applyTemplateShadow(0f)

        showToast(R.string.reset_to_default_values)
    }

    /**
     * Load template background from assets
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        imgTemplate?.let { imageView ->
            Glide.with(this)
                .load(templatePath)
                .into(imageView)
        }
    }

    private fun setupFontSelector() {
        // Find font index from ViewModel (for restoring state)
        val savedFontName = viewModel.nameFont.value
        val selectedIndex = fontList.indexOfFirst { it.name == savedFontName }.takeIf { it >= 0 } ?: 0
        val initialFont = fontList[selectedIndex]

        // Set initial/restored font name and apply font
        binding.tvCurrentNameFont.text = initialFont.name
        val initialTypeface = ResourcesCompat.getFont(this, initialFont.fontResId)
        tvName?.typeface = initialTypeface
        binding.tvCurrentNameFont.typeface = initialTypeface

        // Only update ViewModel if we're using default (not restoring)
        if (savedFontName != initialFont.name) {
            viewModel.setNameFont(initialFont.name)
        }

        // Setup RecyclerView with adapter
        val adapter = FontSelectorAdapter(fontList, selectedIndex) { fontItem, _ ->
            // Update current font display
            binding.tvCurrentNameFont.text = fontItem.name
            val typeface = ResourcesCompat.getFont(this, fontItem.fontResId)
            tvName?.typeface = typeface
            binding.tvCurrentNameFont.typeface = typeface

            // Update ViewModel
            viewModel.setNameFont(fontItem.name)

            // Collapse the font list after selection
            binding.rvFontList.visibility = View.GONE
            binding.imgFontArrow.rotation = 0f
        }

        binding.rvFontList.apply {
            layoutManager = LinearLayoutManager(this@WantedEditorActivity)
            this.adapter = adapter
        }

        // Toggle expand/collapse on click
        binding.layoutFontSelector.setOnClickListener {
            if (binding.rvFontList.visibility == View.GONE) {
                // Expand
                binding.rvFontList.visibility = View.VISIBLE
                binding.imgFontArrow.rotation = 180f
            } else {
                // Collapse
                binding.rvFontList.visibility = View.GONE
                binding.imgFontArrow.rotation = 0f
            }
        }
    }

    private fun setupEditTexts() {
        binding.edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                // Show tvName when user starts typing (only if template has name field)
                val config = viewModel.getConfig()
                if (text.isNotEmpty() && config.hasName) {
                    tvName?.visibility = View.VISIBLE
                }
                tvName?.text = text
                viewModel.setNameText(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.edtBounty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                // Show tvBounty when user starts typing
                if (text.isNotEmpty()) {
                    tvBounty?.visibility = View.VISIBLE
                }
                tvBounty?.text = text
                viewModel.setBountyText(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSeekBars() {
        // Name Spacing (0-0.2 for text spacing)
        binding.seekBarNameSpacing.onProgressChanged { progress ->
            val spacing = progress / 500f  // 0-100 → 0-0.2
            tvName?.letterSpacing = spacing
            // Auto-size handles long TEXT, multi-line handles wide SPACING
            viewModel.setNameSpacing(spacing)
        }

        // Bounty Size
        binding.seekBarBountySize.onProgressChanged { progress ->
            val size = 12f + (progress / 100f) * 48f // 12sp to 60sp
            tvBounty?.textSize = size
            viewModel.setBountySize(size)
        }

        // Bounty Weight
        binding.seekBarBountyWeight.onProgressChanged { progress ->
            // Weight doesn't directly map to Android, but we can use different font styles
            // For simplicity, we'll just store the value
            viewModel.setBountyWeight(progress.toFloat())
        }

        // Bounty Spacing (0-0.2 for text spacing)
        binding.seekBarBountySpacing.onProgressChanged { progress ->
            val spacing = progress / 500f  // 0-100 → 0-0.2
            tvBounty?.letterSpacing = spacing
            viewModel.setBountySpacing(spacing)
        }

        // Bounty Position X
        binding.seekBarBountyPositionX.onProgressChanged { progress ->
            val offsetX = (progress - 50) * 2f // -100 to 100
            tvBounty?.translationX = offsetX
            viewModel.setBountyPositionX(offsetX)
        }

        // Bounty Position Y
        binding.seekBarBountyPositionY.onProgressChanged { progress ->
            val offsetY = (progress - 50) * 2f // -100 to 100
            tvBounty?.translationY = offsetY
            viewModel.setBountyPositionY(offsetY)
        }

        setupPhotoFilterSeekBars()
        setupPosterShadowSeekBar()
    }

    private fun setupPhotoFilterSeekBars() {
        // Shadow - Use dedicated shadow effect function
        binding.seekBarFilterShadow.onProgressChanged { progress ->
            viewModel.setFilterShadow(progress.toFloat())
            applyShadowEffect(progress.toFloat())  // Use shadow layer approach instead of elevation
        }

        // Blur
        binding.seekBarFilterBlur.onProgressChanged { progress ->
            android.util.Log.d("PhotoBlur", "=== Blur seekbar changed ===")
            android.util.Log.d("PhotoBlur", "Progress: $progress")
            android.util.Log.d("PhotoBlur", "API Level: ${Build.VERSION.SDK_INT}")

            viewModel.setFilterBlur(progress.toFloat())

            // Use Glide BlurTransformation for ALL Android versions
            // RenderEffect on Android 12+ causes zoom artifacts, so we use Glide instead
            android.util.Log.d("PhotoBlur", "Using Glide BlurTransformation (all API levels)")
            val currentUri = viewModel.selectedImageUri.value
            if (currentUri != null) {
                reloadImageWithBlur(currentUri, progress.toFloat())
            } else {
                android.util.Log.e("PhotoBlur", "currentUri is NULL!")
            }

            applyFilters()
        }

        // Brightness
        binding.seekBarFilterBrightness.onProgressChanged { progress ->
            val brightness = progress / 100f // 0 to 2
            viewModel.setFilterBrightness(brightness)
            applyFilters()
        }

        // Contrast
        binding.seekBarFilterContrast.onProgressChanged { progress ->
            val contrast = progress / 100f // 0 to 2
            viewModel.setFilterContrast(contrast)
            applyFilters()
        }

        // Grayscale
        binding.seekBarFilterGrayscale.onProgressChanged { progress ->
            val grayscale = progress / 100f // 0 to 1
            viewModel.setFilterGrayscale(grayscale)
            applyFilters()
        }

        // Hue Rotate
        binding.seekBarFilterHueRotate.onProgressChanged { progress ->
            val hueRotate = progress.toFloat() // 0 to 360
            viewModel.setFilterHueRotate(hueRotate)
            applyFilters()
        }

        // Saturate
        binding.seekBarFilterSaturate.onProgressChanged { progress ->
            val saturate = progress / 100f // 0 to 2
            viewModel.setFilterSaturate(saturate)
            applyFilters()
        }

        // Sepia
        binding.seekBarFilterSepia.onProgressChanged { progress ->
            val sepia = progress / 100f // 0 to 1
            viewModel.setFilterSepia(sepia)
            applyFilters()
        }
    }

    private fun setupPosterShadowSeekBar() {
        binding.seekBarPosterShadow.onProgressChanged { progress ->
            // Control template shadow instead of CardView elevation
            applyTemplateShadow(progress.toFloat())
            viewModel.setPosterShadow(progress.toFloat())
        }
    }

    /**
     * Reload image with blur transformation (for Android 8-11)
     * Android 12+ uses RenderEffect which is faster
     */
    private fun reloadImageWithBlur(uri: Uri, blurValue: Float) {
        android.util.Log.d("PhotoBlur", "=== reloadImageWithBlur ===")
        android.util.Log.d("PhotoBlur", "blurValue: $blurValue")

        imgAvatar?.let { imageView ->
            android.util.Log.d("PhotoBlur", "imgAvatar size BEFORE: ${imageView.width}x${imageView.height}")
            android.util.Log.d("PhotoBlur", "imgAvatar scaleType: ${imageView.scaleType}")

            if (blurValue > 0) {
                // Calculate blur radius (0-25)
                val blurRadius = (blurValue / 100f * 25f).toInt().coerceAtLeast(1)
                android.util.Log.d("PhotoBlur", "Applying BlurTransformation: radius=$blurRadius, sampling=3")

                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3)) // radius, sampling
                    .into(imageView)
            } else {
                android.util.Log.d("PhotoBlur", "No blur - loading with centerCrop only")
                // No blur - reload normal
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }

            imageView.post {
                android.util.Log.d("PhotoBlur", "imgAvatar size AFTER: ${imageView.width}x${imageView.height}")
            }
        } ?: run {
            android.util.Log.e("PhotoBlur", "imgAvatar is NULL!")
        }
    }

    /**
     * Load image into both avatar and shadow ImageViews
     * Shadow uses ShadowTransformation to follow alpha channel (contour shadow like icon)
     */
    private fun loadImageToAvatars(uri: Uri) {
        // Load into main avatar with centerCrop
        imgAvatar?.let { imageView ->
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(imageView)
        }

        // Load into shadow layer with ShadowTransformation
        // This creates shadow that follows the alpha channel/contour of the image like icon shadow
        val shadowRadius = viewModel.filterShadow.value / 100f * 15f
        val shadowAlpha = 0.8f

        imgAvatarShadow?.let { imageView ->
            Glide.with(this)
                .load(uri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(imageView)
        }

        // Re-enable shadow seekbar when loading new image
        binding.seekBarFilterShadow.isEnabled = true
    }

    /**
     * Apply shadow effect to imgAvatarShadow layer
     * Reloads shadow with transformation that follows alpha channel (contour shadow like icon)
     */
    private fun applyShadowEffect(shadowValue: Float) {
        android.util.Log.d("PhotoShadow", "=== applyShadowEffect called ===")
        android.util.Log.d("PhotoShadow", "seekbar value: $shadowValue")

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)
        android.util.Log.d("PhotoShadow", "effectiveShadowValue (35-100): $effectiveShadowValue")

        val shadowView = imgAvatarShadow
        android.util.Log.d("PhotoShadow", "shadowView is null: ${shadowView == null}")
        if (shadowView == null) return

        if (shadowValue <= 0) {
            android.util.Log.d("PhotoShadow", "shadowValue <= 0, hiding shadow")
            shadowView.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        android.util.Log.d("PhotoShadow", "Setting visibility to VISIBLE")
        shadowView.visibility = View.VISIBLE

        // Reload shadow with new transformation parameters
        val currentUri = viewModel.selectedImageUri.value
        android.util.Log.d("PhotoShadow", "currentUri: $currentUri")
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f // 35-100 → 5.25-15px
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9
            android.util.Log.d("PhotoShadow", "Loading shadow: radius=$shadowRadius, alpha=$shadowAlpha")

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        } else {
            android.util.Log.e("PhotoShadow", "currentUri is NULL - cannot load shadow!")
        }

        // 1. Alpha - overall shadow visibility
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha
        android.util.Log.d("PhotoShadow", "viewAlpha: $viewAlpha")

        // 2. Offset - shadow displacement
        val offsetX = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY
        android.util.Log.d("PhotoShadow", "offset: X=$offsetX, Y=$offsetY")

        // 3. Scale - noticeable shadow width increase
        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale
        android.util.Log.d("PhotoShadow", "scale: $scale")

        // 4. Additional blur via RenderEffect (API 31+) - DISABLED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shadowView.setRenderEffect(null)
            android.util.Log.d("PhotoShadow", "RenderEffect disabled (using Glide blur only)")
        }

        android.util.Log.d("PhotoShadow", "=== applyShadowEffect done ===")
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
            imgAvatar?.colorFilter = ColorMatrixColorFilter(colorMatrix)

            // Note: Shadow is now handled by applyShadowEffect() using shadow layer approach
            // Old elevation-based shadow code removed as it didn't work properly with ImageView

            // Apply Blur
            // NOTE: Blur is now handled by Glide BlurTransformation for ALL Android versions
            // RenderEffect on Android 12+ caused zoom artifacts, so we disabled it
            android.util.Log.d("PhotoBlur", "=== applyFilters() - Blur section ===")
            android.util.Log.d("PhotoBlur", "Blur is handled by Glide BlurTransformation in reloadImageWithBlur()")

            // Make sure no RenderEffect is applied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                imgAvatar?.setRenderEffect(null)
                android.util.Log.d("PhotoBlur", "Cleared any existing RenderEffect")
            }
        }
    }

    /**
     * Handle remove background from current image
     */
    private fun handleRemoveBackground() {
        val currentUri = viewModel.selectedImageUri.value

        if (currentUri == null) {
            showToast(R.string.please_import_a_photo_first)
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
                    showToast(R.string.failed_to_load_image)
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
                    imgAvatar?.let { imageView ->
                        Glide.with(this@WantedEditorActivity)
                            .load(resultBitmap)
                            .centerCrop()
                            .into(imageView)
                    }

                    // Load into shadow layer with ShadowTransformation
                    // Shadow will follow the contour of the person (no background)
                    val shadowRadius = 15f
                    val shadowAlpha = 0.8f
                    imgAvatarShadow?.let { imageView ->
                        Glide.with(this@WantedEditorActivity)
                            .load(resultBitmap)
                            .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                            .into(imageView)
                    }

                    // Keep shadow enabled - it will follow the contour of the person!
                    binding.seekBarFilterShadow.isEnabled = true
                    imgAvatarShadow?.visibility = View.VISIBLE

                    showToast(R.string.background_removed_success)
                } else {
                    showToast(R.string.failed_to_remove_background)
                }

            } catch (e: Exception) {
                dismissLoading()
                setRemoveBackgroundButtonEnabled(true)
                e.printStackTrace()
                showToast(getString(R.string.error_message, e.message ?: "Unknown"))
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
     * WORKS EXACTLY LIKE Photo Filter Shadow:
     * - Photo Filter: Load user image → ShadowTransformation → imgAvatarShadow
     * - Poster Shadow: Load template drawable → ShadowTransformation → imgTemplateShadow
     * Both create contour shadow following the object shape!
     */
    private fun applyTemplateShadow(shadowValue: Float) {
        android.util.Log.d("PosterShadow", "=== applyTemplateShadow called ===")
        android.util.Log.d("PosterShadow", "seekbar value: $shadowValue")

        val shadowView = imgTemplateShadow
        android.util.Log.d("PosterShadow", "shadowView is null: ${shadowView == null}")
        if (shadowView == null) return

        if (shadowValue <= 0) {
            android.util.Log.d("PosterShadow", "shadowValue <= 0, hiding shadow")
            shadowView.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        android.util.Log.d("PosterShadow", "Setting visibility to VISIBLE")
        shadowView.visibility = View.VISIBLE

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)
        android.util.Log.d("PosterShadow", "effectiveShadowValue (35-100): $effectiveShadowValue")

        // EXACTLY LIKE Photo Filter: Reload with new transformation parameters
        val shadowRadius = effectiveShadowValue / 100f * 15f  // 35-100 → 5.25-15px blur
        val shadowAlpha = effectiveShadowValue / 100f * 0.9f   // 35-100 → 0.315-0.9
        android.util.Log.d("PosterShadow", "shadowRadius: $shadowRadius, shadowAlpha: $shadowAlpha")

        // Load template from assets with ShadowTransformation
        // This creates shadow following the template's alpha channel/contour
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)
        android.util.Log.d("PosterShadow", "templateId: $templateId, templatePath: $templatePath")

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // 1. Alpha - overall shadow visibility (MATCHED with Photo Filter)
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha
        android.util.Log.d("PosterShadow", "viewAlpha: $viewAlpha")

        // 2. Offset - shadow displacement (MATCHED with Photo Filter)
        val offsetX = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY
        android.util.Log.d("PosterShadow", "offset: X=$offsetX, Y=$offsetY")

        // 3. Scale - noticeable shadow width increase
        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale
        android.util.Log.d("PosterShadow", "scale: $scale")

        // 4. Additional blur via RenderEffect (API 31+) for extra softness (MATCHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f  // 35-100 → 3.5-10px additional blur
            android.util.Log.d("PosterShadow", "Android 12+: additionalBlur=$additionalBlur")
            if (additionalBlur > 0) {
                val blurEffect = RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        } else {
            android.util.Log.d("PosterShadow", "Android < 12: No RenderEffect")
        }
    }
}

