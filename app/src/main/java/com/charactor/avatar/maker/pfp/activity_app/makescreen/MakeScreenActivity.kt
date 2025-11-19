package com.charactor.avatar.maker.pfp.activity_app.makescreen

import android.content.Intent
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
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.template.TemplateListActivity
import com.charactor.avatar.maker.pfp.activity_app.wanted.WantedEditorActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.AssetHelper
import com.charactor.avatar.maker.pfp.core.helper.ShadowTransformation
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivityMakeScreenBinding
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
        // TODO: Capture poster view as bitmap and save to gallery
        showToast("Saving poster...")
        // After save success, navigate to SuccessActivity
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
     * Load image into preview
     */
    private fun loadImageToPreview(uri: Uri) {
        // Load into main avatar
        imgAvatar?.let { imageView ->
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(imageView)
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

            // Update shadows
            applyPosterShadow(viewModel.posterShadow.value)
            applyPhotoShadow(viewModel.filterShadow.value)
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

        // Reload with new transformation parameters
        val shadowRadius = shadowValue / 100f * 15f
        val shadowAlpha = shadowValue / 100f * 0.9f

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 7f
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f
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

        // Reload shadow with new transformation
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = shadowValue / 100f * 15f
            val shadowAlpha = shadowValue / 100f * 0.9f

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        }

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 7f
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f
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

