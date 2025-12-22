package com.piratemaker.postermaker.poster.activity_app.bountyfilter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import java.io.FileOutputStream
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
import com.piratemaker.postermaker.poster.databinding.SuccessfullBountyBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class SuccessfulBountyActivity : BaseActivity<SuccessfullBountyBinding>() {

    private var photoPath: String? = null
    private var bountyValue: String? = null
    private var downloadPermissionDeniedCount = 0
    private var compositeImagePath: String? = null

    private val viewModel = PosterEditorSharedViewModel.getInstance()




    // Permission launcher for Android 8-9
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            downloadPermissionDeniedCount = 0
            proceedDownload()
        } else {
            downloadPermissionDeniedCount++

            val canAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                true
            }

            if (!canAskAgain) {
                goToSettings()
            } else if (downloadPermissionDeniedCount >= 2) {
                goToSettings()
            } else {
                Toast.makeText(
                    this,
                    strings(R.string.download_failed_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun setViewBinding(): SuccessfullBountyBinding {
        return SuccessfullBountyBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Get data from intent
        photoPath = intent.getStringExtra("PHOTO_PATH")
        bountyValue = intent.getStringExtra("BOUNTY_VALUE")

        binding.apply {
            // Load captured photo into imgCamera
            photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Glide.with(this@SuccessfulBountyActivity)
                        .load(file)
                        .into(imgCamera)
                }
            }

            // Show bounty value
            bountyValue?.let {
                tvBountyFilter.text = it
                tvBountyFilter.visible()
            }
        }

        // Create composite image after view is laid out
        binding.containerBounty.post {
            createCompositeImage()
        }
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
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.setOnSingleClick {
                goToHome()
            }

            btnDownload.setOnSingleClick {
                downloadImage()
            }

            btnShare.setOnSingleClick(2000) {
                shareImage()
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = getString(R.string.bountyFilter)
            btnActionBarLeft.setImageResource(R.drawable.ic_home)
            btnActionBarLeft.visible()
            btnActionBarRight.gone()
        }
    }

    private fun createCompositeImage() {
        try {
            // Capture the entire containerBounty (background + camera photo + text)
            val view = binding.containerBounty
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            // Save composite bitmap to cache
            val fileName = "bounty_composite_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            compositeImagePath = file.absolutePath
            android.util.Log.d("SuccessfulBounty", "Composite image created: $compositeImagePath")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("SuccessfulBounty", "Failed to create composite image", e)
        }
    }

    private fun shareImage() {
        val pathToShare = compositeImagePath ?: photoPath
        pathToShare?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    private fun downloadImage() {
        // Android 10+: No permission needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            proceedDownload()
            return
        }

        // Android 8-9: Need permission
        val storagePermissions = PermissionHelper.storagePermission

        if (checkPermissions(storagePermissions)) {
            proceedDownload()
        } else if (downloadPermissionDeniedCount >= 2) {
            goToSettings()
        } else {
            permissionLauncher.launch(storagePermissions)
        }
    }

    private fun proceedDownload() {
        val pathToDownload = compositeImagePath ?: photoPath
        pathToDownload?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@SuccessfulBountyActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@SuccessfulBountyActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@SuccessfulBountyActivity,
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
