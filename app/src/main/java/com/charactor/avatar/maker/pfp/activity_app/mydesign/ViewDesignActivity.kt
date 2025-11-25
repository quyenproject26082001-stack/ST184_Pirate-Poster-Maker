package com.charactor.avatar.maker.pfp.activity_app.mydesign

import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.*
import com.charactor.avatar.maker.pfp.core.helper.MediaHelper
import com.charactor.avatar.maker.pfp.core.utils.state.HandleState
import com.charactor.avatar.maker.pfp.databinding.ActivityViewBinding
import com.charactor.avatar.maker.pfp.dialog.YesNoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ViewDesignActivity : BaseActivity<ActivityViewBinding>() {

    private var imagePath: String? = null

    override fun setViewBinding(): ActivityViewBinding {
        return ActivityViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        imagePath = intent.getStringExtra("imagePath")

        imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .into(binding.imgPoster)
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Right button - Delete
            btnActionBarRight.setImageResource(R.drawable.ic_delete)
            btnActionBarRight.visible()

            // Hide other elements
            tvCenter.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
            cvLogo.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Back button
            actionBar.btnActionBarLeft.setOnSingleClick {
                finishAfterTransition()
            }

            // Delete button
            actionBar.btnActionBarRight.setOnSingleClick {
                showDeleteConfirmation()
            }

            // Share button
            btnShare.setOnSingleClick(2000) {
                shareImage()
            }

            // Download button
            btnDownload.setOnSingleClick {
                downloadImage()
            }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    private fun showDeleteConfirmation() {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.delete,
            description = R.string.delete_design_confirmation
        )
        dialog.onYesClick = {
            deleteImage()
            dialog.dismiss()
        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteImage() {
        imagePath?.let { path ->
            val file = File(path)
            if (file.exists() && file.delete()) {
                Toast.makeText(this, R.string.design_deleted, Toast.LENGTH_SHORT).show()
                finishAfterTransition()
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage() {
        imagePath?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    private fun downloadImage() {
        imagePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@ViewDesignActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@ViewDesignActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@ViewDesignActivity,
                                    strings(R.string.download_failed_please_try_again_later),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {}
                        }
                    }
            }
        }
    }
}
