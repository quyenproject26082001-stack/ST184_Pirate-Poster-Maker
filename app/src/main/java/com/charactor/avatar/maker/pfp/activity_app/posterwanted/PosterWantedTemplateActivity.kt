package com.charactor.avatar.maker.pfp.activity_app.posterwanted

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.makescreen.MakeScreenActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivityPosterWantedTemplateBinding
import com.charactor.avatar.maker.pfp.dialog.WaitingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Poster Wanted Template Activity
 * Shows 100 random poster templates with random data
 * When clicked, navigates to MakeScreenActivity with selected data
 */
class PosterWantedTemplateActivity : BaseActivity<ActivityPosterWantedTemplateBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()
    private lateinit var adapter: PosterWantedTemplateAdapter

    private var waitingDialog: WaitingDialog? =null
    override fun setViewBinding(): ActivityPosterWantedTemplateBinding {
        return ActivityPosterWantedTemplateBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {

        binding.rvTemplates.apply{
            layoutManager = GridLayoutManager(this@PosterWantedTemplateActivity,2)
            setHasFixedSize(true)
            setItemViewCacheSize(100)
        }
        // Generate 100 random items
        waitingDialog = WaitingDialog(this)
        waitingDialog?.show()
        lifecycleScope.launch {

            val items = withContext(Dispatchers.Default){

                PosterWantedItem.generateRandomItems(100)
            }

            adapter = PosterWantedTemplateAdapter(items) { item ->
                onItemClicked(item)
            }

            binding.rvTemplates.adapter = adapter
            binding.rvTemplates.postDelayed({
                waitingDialog?.dismiss()
            }, 1500)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.cleanup()
        }
    }

    /**
     * Handle item click - Navigate to MakeScreen with selected data
     */
    private fun onItemClicked(item: PosterWantedItem) {
        // Set data to ViewModel
        viewModel.setSelectedTemplate(item.templateId)
        viewModel.setNameText(item.name)
        // Use full bounty text with prefix and suffix from template config
        viewModel.setBountyText(item.getFullBountyText())

        // Explicitly set bountySize from template config
        // This ensures thumbnail and actual poster have the same text size
        // (fixes issue where ViewModel persists old bountySize from previous session)
        viewModel.setBountySize(viewModel.getConfig().bountySize)

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
