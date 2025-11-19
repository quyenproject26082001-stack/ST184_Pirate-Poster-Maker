package com.charactor.avatar.maker.pfp.activity_app.posterwanted

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.makescreen.MakeScreenActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivityPosterWantedTemplateBinding

/**
 * Poster Wanted Template Activity
 * Shows 100 random poster templates with random data
 * When clicked, navigates to MakeScreenActivity with selected data
 */
class PosterWantedTemplateActivity : BaseActivity<ActivityPosterWantedTemplateBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()
    private lateinit var adapter: PosterWantedTemplateAdapter

    override fun setViewBinding(): ActivityPosterWantedTemplateBinding {
        return ActivityPosterWantedTemplateBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Generate 100 random items
        val items = PosterWantedItem.generateRandomItems(100)

        // Setup adapter
        adapter = PosterWantedTemplateAdapter(items) { item ->
            onItemClicked(item)
        }

        binding.rvTemplates.adapter = adapter
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
                finishAfterTransition()
            }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = strings(R.string.poster_templates)
            tvCenter.visible()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    /**
     * Handle item click - Navigate to MakeScreen with selected data
     */
    private fun onItemClicked(item: PosterWantedItem) {
        // Set data to ViewModel
        viewModel.setSelectedTemplate(item.templateId)
        viewModel.setNameText(item.name)
        viewModel.setBountyText(item.bounty)

        // Set avatar URI from assets
        val avatarUri = Uri.parse(item.getAvatarPath())
        viewModel.setSelectedImageUri(avatarUri)

        // Mark editing started
        viewModel.markEditingStarted()

        // Navigate to MakeScreen
        val intent = Intent(this, MakeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }
}
