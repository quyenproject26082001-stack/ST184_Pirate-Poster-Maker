package com.charactor.avatar.maker.pfp.activity_app.makescreen

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.template.TemplateListActivity
import com.charactor.avatar.maker.pfp.activity_app.wanted.WantedEditorActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.AssetHelper
import com.charactor.avatar.maker.pfp.core.helper.ShadowTransformation
import com.charactor.avatar.maker.pfp.databinding.ActivityMakeScreenBinding
import kotlinx.coroutines.launch

/**
 * MakeScreen Activity
 * Main screen after clicking "Create" button from Home
 * Shows poster preview with 3 action buttons: Templates, Import, Edit
 */
class MakeScreenActivity : BaseActivity<ActivityMakeScreenBinding>() {

    private val viewModel: MakeScreenViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedImageUri(it)
            loadImageToPreview(it)
        }
    }

    // Request code for Edit button (to receive edited data back)
    private val editActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                // Extract data and update ViewModel
                handleEditedData(data)
            }
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
        // Load template background from assets
        loadTemplateBackground()

        // Load default avatar image from assets
        loadDefaultAvatar()

        // Initialize with default template and preview
        updatePreviewWithCurrentState()
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { handleBack() }
                btnActionBarRight.setOnSingleClick { handleSave() }
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
                binding.tvName.text = text
            }
        }

        // Observe bounty text
        lifecycleScope.launch {
            viewModel.bountyText.collect { text ->
                binding.tvBounty.text = text
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
        // TODO: Capture poster view as bitmap and save to gallery
        showToast("Saving poster...")
        // After save success, navigate to SuccessActivity
    }

    /**
     * Navigate to WantedEditorActivity with current data
     */
    private fun navigateToEditor() {
        val intent = Intent(this, WantedEditorActivity::class.java).apply {
            // Pass current data to editor
            viewModel.selectedImageUri.value?.let { uri ->
                putExtra("imageUri", uri.toString())
            }
            putExtra("nameText", viewModel.nameText.value)
            putExtra("bountyText", viewModel.bountyText.value)
            putExtra("selectedTemplate", viewModel.selectedTemplate.value)

            // Pass filter values
            putExtra("filterBrightness", viewModel.filterBrightness.value)
            putExtra("filterContrast", viewModel.filterContrast.value)
            putExtra("filterSaturate", viewModel.filterSaturate.value)
            putExtra("filterGrayscale", viewModel.filterGrayscale.value)
            putExtra("filterHueRotate", viewModel.filterHueRotate.value)
            putExtra("filterSepia", viewModel.filterSepia.value)
            putExtra("filterBlur", viewModel.filterBlur.value)
            putExtra("filterShadow", viewModel.filterShadow.value)
            putExtra("posterShadow", viewModel.posterShadow.value)
        }
        editActivityLauncher.launch(intent)
    }

    /**
     * Handle edited data returned from WantedEditorActivity
     */
    private fun handleEditedData(data: Intent) {
        // Extract edited data and update ViewModel
        data.getStringExtra("imageUri")?.let { uriString ->
            viewModel.setSelectedImageUri(uriString.toUri())
        }

        data.getStringExtra("nameText")?.let { text ->
            viewModel.setNameText(text)
        }

        data.getStringExtra("bountyText")?.let { text ->
            viewModel.setBountyText(text)
        }

        // Update template
        val template = data.getIntExtra("selectedTemplate", 1)
        viewModel.setSelectedTemplate(template)

        // Update filter values
        viewModel.setFilterBrightness(data.getFloatExtra("filterBrightness", 1f))
        viewModel.setFilterContrast(data.getFloatExtra("filterContrast", 1f))
        viewModel.setFilterSaturate(data.getFloatExtra("filterSaturate", 1f))
        viewModel.setFilterGrayscale(data.getFloatExtra("filterGrayscale", 0f))
        viewModel.setFilterHueRotate(data.getFloatExtra("filterHueRotate", 0f))
        viewModel.setFilterSepia(data.getFloatExtra("filterSepia", 0f))
        viewModel.setFilterBlur(data.getFloatExtra("filterBlur", 0f))
        viewModel.setFilterShadow(data.getFloatExtra("filterShadow", 0f))
        viewModel.setPosterShadow(data.getFloatExtra("posterShadow", 0f))

        // Update name properties
        data.getStringExtra("nameFont")?.let { font ->
            viewModel.setNameFont(font)
        }
        viewModel.setNameSpacing(data.getFloatExtra("nameSpacing", 0.1f))

        // Update bounty properties
        viewModel.setBountySize(data.getFloatExtra("bountySize", 28f))
        viewModel.setBountyWeight(data.getFloatExtra("bountyWeight", 0f))
        viewModel.setBountySpacing(data.getFloatExtra("bountySpacing", 0f))
        viewModel.setBountyPositionX(data.getFloatExtra("bountyPositionX", 0f))
        viewModel.setBountyPositionY(data.getFloatExtra("bountyPositionY", 0f))

        // Update preview
        updatePreviewWithCurrentState()
    }

    /**
     * Load template background from assets
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .into(binding.imgTemplate)
    }

    /**
     * Load default avatar image from assets
     */
    private fun loadDefaultAvatar() {
        val templateId = viewModel.selectedTemplate.value
        val avatarPath = AssetHelper.getTemplateAvatarPath(templateId)

        Glide.with(this)
            .load(avatarPath)
            .centerCrop()
            .into(binding.imgAvatar)
    }

    /**
     * Load image into preview
     */
    private fun loadImageToPreview(uri: Uri) {
        // Load into main avatar
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.imgAvatar)

        // Load into shadow layer with ShadowTransformation (contour shadow)
        val shadowRadius = viewModel.filterShadow.value / 100f * 15f
        val shadowAlpha = 0.8f

        Glide.with(this)
            .load(uri)
            .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
            .into(binding.imgAvatarShadow)
    }

    /**
     * Update preview with current ViewModel state
     * Called after receiving edited data from WantedEditorActivity
     */
    private fun updatePreviewWithCurrentState() {
        // Update template background
        loadTemplateBackground()

        // Update name
        binding.tvName.text = viewModel.nameText.value

        // Update bounty
        binding.tvBounty.text = viewModel.bountyText.value

        // Update image if exists, otherwise show default avatar
        viewModel.selectedImageUri.value?.let { uri ->
            loadImageToPreview(uri)
        } ?: loadDefaultAvatar()

        // Update shadows
        applyPosterShadow(viewModel.posterShadow.value)
        applyPhotoShadow(viewModel.filterShadow.value)
    }

    /**
     * Apply poster shadow effect (template shadow)
     * EXACTLY LIKE WantedEditorActivity.applyTemplateShadow()
     */
    private fun applyPosterShadow(shadowValue: Float) {
        if (shadowValue <= 0) {
            binding.imgTemplateShadow.visibility = android.view.View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.imgTemplateShadow.setRenderEffect(null)
            }
            return
        }

        binding.imgTemplateShadow.visibility = android.view.View.VISIBLE

        // Reload with new transformation parameters
        val shadowRadius = shadowValue / 100f * 15f
        val shadowAlpha = shadowValue / 100f * 0.9f

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(binding.imgTemplateShadow)

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        binding.imgTemplateShadow.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 7f
        binding.imgTemplateShadow.translationX = offsetX
        binding.imgTemplateShadow.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        binding.imgTemplateShadow.scaleX = scale
        binding.imgTemplateShadow.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                binding.imgTemplateShadow.setRenderEffect(blurEffect)
            } else {
                binding.imgTemplateShadow.setRenderEffect(null)
            }
        }
    }

    /**
     * Apply photo shadow effect (avatar shadow)
     * EXACTLY LIKE WantedEditorActivity.applyShadowEffect()
     */
    private fun applyPhotoShadow(shadowValue: Float) {
        if (shadowValue <= 0) {
            binding.imgAvatarShadow.visibility = android.view.View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.imgAvatarShadow.setRenderEffect(null)
            }
            return
        }

        binding.imgAvatarShadow.visibility = android.view.View.VISIBLE

        // Reload shadow with new transformation
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = shadowValue / 100f * 15f
            val shadowAlpha = shadowValue / 100f * 0.9f

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(binding.imgAvatarShadow)
        }

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        binding.imgAvatarShadow.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 7f
        binding.imgAvatarShadow.translationX = offsetX
        binding.imgAvatarShadow.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        binding.imgAvatarShadow.scaleX = scale
        binding.imgAvatarShadow.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                binding.imgAvatarShadow.setRenderEffect(blurEffect)
            } else {
                binding.imgAvatarShadow.setRenderEffect(null)
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

