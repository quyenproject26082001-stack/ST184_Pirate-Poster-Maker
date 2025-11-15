package com.charactor.avatar.maker.pfp.activity_app.permission

import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.checkPermissions
import com.charactor.avatar.maker.pfp.core.extensions.goToSettings
import com.charactor.avatar.maker.pfp.core.extensions.gone
import com.charactor.avatar.maker.pfp.core.extensions.requestPermission
import com.charactor.avatar.maker.pfp.core.extensions.select
import com.charactor.avatar.maker.pfp.core.extensions.showToast
import com.charactor.avatar.maker.pfp.core.extensions.startIntentRightToLeft
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.core.helper.StringHelper
import com.charactor.avatar.maker.pfp.core.utils.key.RequestKey
import com.charactor.avatar.maker.pfp.databinding.ActivityPermissionBinding
import com.charactor.avatar.maker.pfp.activity_app.main.MainActivity
import com.charactor.avatar.maker.pfp.core.extensions.setGradientTextHeightColor
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import kotlinx.coroutines.launch

class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {

    private val viewModel: PermissionViewModel by viewModels()

    override fun setViewBinding() = ActivityPermissionBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.btnStorage.visible()
            binding.btnNotification.gone()
        } else {
            binding.btnNotification.visible()
            binding.btnStorage.gone()
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
        setGradientTextHeightColor(binding.tvContinue, "#FFA1F7".toColorInt(), "#821FED".toColorInt())
        val textRes =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.to_access_13 else R.string.to_access

        binding.txtPer.text = TextUtils.concat(
            createColoredText(R.string.allow, R.color.white),
            " ",
            createColoredText(R.string.app_name, R.color.purple_F0),
            " ",
            createColoredText(textRes, R.color.white)
        )
    }

    override fun viewListener() {
        binding.swPermission.setOnSingleClick { handlePermissionRequest(isStorage = true) }
        binding.swNotification.setOnSingleClick { handlePermissionRequest(isStorage = false) }
        binding.tvContinue.setOnSingleClick(1500) { handleContinue() }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageGranted.collect { granted ->
                    updatePermissionUI(granted, true)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notificationGranted.collect { granted ->
                    updatePermissionUI(granted, false)
                }
            }
        }
    }

    private fun handlePermissionRequest(isStorage: Boolean) {
        val perms = if (isStorage) viewModel.getStoragePermissions() else viewModel.getNotificationPermissions()
        if (checkPermissions(perms)) {
            showToast(if (isStorage) R.string.granted_storage else R.string.granted_notification)
        } else if (viewModel.needGoToSettings(sharePreference, isStorage)) {
            goToSettings()
        } else {
            val requestCode = if (isStorage) RequestKey.STORAGE_PERMISSION_CODE else RequestKey.NOTIFICATION_PERMISSION_CODE
            requestPermission(perms, requestCode)
        }
    }

    private fun updatePermissionUI(granted: Boolean, isStorage: Boolean) {
        val imageView = if (isStorage) binding.swPermission else binding.swNotification
        imageView.setImageResource(if (granted) R.drawable.ic_sw_on else R.drawable.ic_sw_off)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when (requestCode) {
            RequestKey.STORAGE_PERMISSION_CODE -> viewModel.updateStorageGranted(sharePreference, granted)

            RequestKey.NOTIFICATION_PERMISSION_CODE -> viewModel.updateNotificationGranted(sharePreference, granted)
        }
        if (granted) {
            showToast(if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) R.string.granted_storage else R.string.granted_notification)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.updateStorageGranted(
            sharePreference, checkPermissions(viewModel.getStoragePermissions())
        )
        viewModel.updateNotificationGranted(
            sharePreference, checkPermissions(viewModel.getNotificationPermissions())
        )
    }


    override fun initActionBar() {
        binding.actionBar.tvCenter.apply {
            text = getString(R.string.permission)
            visible()
        }
    }

    private fun createColoredText(
        @androidx.annotation.StringRes textRes: Int,
        @androidx.annotation.ColorRes colorRes: Int,
        font: Int = R.font.roboto_regular
    ) = StringHelper.changeColor(this, getString(textRes), colorRes, font)

    private fun handleContinue() {
        sharePreference.setIsFirstPermission(false)
        startIntentRightToLeft(MainActivity::class.java)
        finishAffinity()
    }
}