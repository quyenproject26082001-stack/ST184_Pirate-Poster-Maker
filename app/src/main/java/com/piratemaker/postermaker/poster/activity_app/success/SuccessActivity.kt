package com.piratemaker.postermaker.poster.activity_app.success

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.main.MainActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.goToSettings
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.shareImagesPaths
import com.piratemaker.postermaker.poster.core.extensions.strings
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.helper.PermissionHelper
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.core.viewmodel.PosterEditorSharedViewModel
import com.piratemaker.postermaker.poster.databinding.ActivitySuccessBinding
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()
    private var savedImagePath: String? = null

    // Permission launcher for Android 8-9
    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            // ✅ Granted: Proceed download
            proceedDownload()
        } else {
            // ❌ Denied: Check if "Don't ask again" was clicked
            val canAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                true
            }

            if (!canAskAgain) {
                // 🚫 User clicked "Don't ask again" → Mark flag để lần sau vào Settings
                sharePreference.setDontAskAgainStorage(true)
            }

            // Show error toast
            Toast.makeText(
                this,
                strings(R.string.download_failed_please_try_again_later),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


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

        // ✅ Chỉ cần gọi 1 lần, ngay sau startActivity
        overridePendingTransition(0, 0)

        finish()

        // ✅ Clear ViewModel SAU KHI finish để tránh trigger UI update
        viewModel.clearAll()
    }

    private fun shareImage() {
        savedImagePath?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
    fun downloadImage() {
        // Android 10+: No permission needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            proceedDownload()
            return
        }

        // Android 8-9: Need permission
        val storagePermissions = PermissionHelper.storagePermission

        if (checkPermissions(storagePermissions)) {
            // Đã có quyền → Download
            proceedDownload()
        } else if (sharePreference.isDontAskAgainStorage()) {
            // User đã ấn "Don't ask again" → Mở Settings
            goToSettings()
        } else {
            // Hỏi quyền (hỏi mãi cho đến khi user ấn "Don't ask again")
            permissionLauncher.launch(storagePermissions)
        }
    }

    // ✅ THÊM MỚI


    /**
     * Proceed with download after permission check
     */
    private fun proceedDownload() {
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
