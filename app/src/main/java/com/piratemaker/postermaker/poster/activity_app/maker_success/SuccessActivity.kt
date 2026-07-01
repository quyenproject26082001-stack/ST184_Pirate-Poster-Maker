package com.piratemaker.postermaker.poster.activity_app.maker_success

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
import com.piratemaker.postermaker.poster.core.extensions.goToSettings
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.invisible
import com.piratemaker.postermaker.poster.core.extensions.loadImage
import com.piratemaker.postermaker.poster.core.extensions.loadNativeCollabAds
import com.piratemaker.postermaker.poster.core.extensions.requestPermission
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.setTextActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
import com.piratemaker.postermaker.poster.core.extensions.startIntentWithClearTop
import com.piratemaker.postermaker.poster.core.extensions.strings
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.UnitHelper
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.RequestKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.databinding.ActivityMakerSuccessBinding
import com.piratemaker.postermaker.poster.activity_app.main.MainActivity
import com.piratemaker.postermaker.poster.activity_app.mycreation.MyCreationActivity
import com.piratemaker.postermaker.poster.activity_app.permission.PermissionViewModel
import kotlinx.coroutines.launch

class SuccessActivity : BaseActivity<ActivityMakerSuccessBinding>() {
    private val viewModel: SuccessViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()
    private var savedType = ValueKey.AVATAR_TYPE

    override fun setViewBinding(): ActivityMakerSuccessBinding {
        return ActivityMakerSuccessBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY) ?: "")
        savedType = intent.getIntExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE)
        setButtonBackgrounds()
    }

    private fun setButtonBackgrounds() {

    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pathInternal.collect { path ->
                        if (path.isNotEmpty()) {
                            loadImage(this@SuccessActivity, path, binding.imvImage)
                        }
                    }
                }
            }
        }
    }

    private fun handleBack() {
        handleBackLeftToRight()
    }
    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarRight.tap {

                       showInterAll {   startIntentWithClearTop(MainActivity::class.java)}

                }
                btnActionBarLeft.tap {  handleBack()  }

            }

            // My Album button
            actionBar.btnActionBarNextToRight.tap(2590) {
                showInterAll {
                    startIntentRightToLeft(MyCreationActivity::class.java, IntentKey.TAB_KEY, savedType)
                }
            }

            // Download button
            btnDownload.tap(2000) {
                checkStoragePermission()
            }
            btnShare.tap(2000){
                    viewModel.shareFiles(this@SuccessActivity)
            }


        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            imgCenter.gone()
                setImageActionBar(btnActionBarNextToRight, R.drawable.ic_mycreation)
            setImageActionBar(btnActionBarRight,R.drawable.ic_home)
            btnActionBarNextToRight.visible()
            tvCenter.select()

        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload()
        } else {
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload()
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleDownload() {
        lifecycleScope.launch {
            viewModel.downloadFiles(this@SuccessActivity).collect { state ->
                when (state) {
                    HandleState.LOADING -> showLoading()
                    HandleState.SUCCESS -> {
                        dismissLoading()
                        showToast(R.string.download_success)
                    }
                    else -> {
                        dismissLoading()
                        showToast(R.string.download_failed_please_try_again_later)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                handleDownload()
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
            }
        }
    }

    override fun initAds() {
        initNativeCollab()
    }

    fun initNativeCollab() {

        Admob.getInstance().loadNativeAd(this, getString(R.string.native_success), binding.nativeAds, R.layout.ads_native_avg2_btn_top)

        //loadNativeCollabAds(R.string.native_cl_success, binding.flNativeCollab)


    }

    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackLeftToRight()
    }
}
