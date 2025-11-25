package com.charactor.avatar.maker.pfp.activity_app.success

import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.activity_app.main.MainActivity
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.gone
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.shareImagesPaths
import com.charactor.avatar.maker.pfp.core.extensions.strings
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.core.helper.MediaHelper
import com.charactor.avatar.maker.pfp.core.utils.state.HandleState
import com.charactor.avatar.maker.pfp.core.viewmodel.PosterEditorSharedViewModel
import com.charactor.avatar.maker.pfp.databinding.ActivitySuccessBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()
    private var savedImagePath: String? = null

    override fun setViewBinding(): ActivitySuccessBinding {
        return ActivitySuccessBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Load saved image from ViewModel
        savedImagePath = viewModel.savedImagePath.value

        android.util.Log.d("SuccessActivity", "savedImagePath: $savedImagePath")

        savedImagePath?.let { path ->
            val file = File(path)
            android.util.Log.d("SuccessActivity", "File exists: ${file.exists()}, size: ${file.length()}")
            Glide.with(this)
                .load(file)
                .into(binding.imgPoster)
        } ?: run {
            android.util.Log.e("SuccessActivity", "savedImagePath is null!")
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Home
            btnActionBarLeft.setImageResource(R.drawable.ic_home)
            btnActionBarLeft.visible()

            // Hide other elements
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
            cvLogo.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Home button
            actionBar.btnActionBarLeft.setOnSingleClick {
                goToHome()
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

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun shareImage() {
        savedImagePath?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    private fun downloadImage() {
        savedImagePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@SuccessActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@SuccessActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@SuccessActivity,
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

    override fun onBackPressed() {
        goToHome()
    }
}
