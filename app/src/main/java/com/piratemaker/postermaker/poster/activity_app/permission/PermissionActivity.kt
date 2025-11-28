    package com.piratemaker.postermaker.poster.activity_app.permission
    
    import android.content.pm.PackageManager
    import android.os.Build
    import android.text.TextUtils
    import android.view.LayoutInflater
    import androidx.activity.viewModels
    import androidx.core.graphics.toColorInt
    import androidx.lifecycle.Lifecycle
    import androidx.lifecycle.lifecycleScope
    import androidx.lifecycle.repeatOnLifecycle
    import com.piratemaker.postermaker.poster.R
    import com.piratemaker.postermaker.poster.core.base.BaseActivity
    import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
    import com.piratemaker.postermaker.poster.core.extensions.goToSettings
    import com.piratemaker.postermaker.poster.core.extensions.gone
    import com.piratemaker.postermaker.poster.core.extensions.requestPermission
    import com.piratemaker.postermaker.poster.core.extensions.select
    import com.piratemaker.postermaker.poster.core.extensions.showToast
    import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
    import com.piratemaker.postermaker.poster.core.extensions.visible
    import com.piratemaker.postermaker.poster.core.helper.StringHelper
    import com.piratemaker.postermaker.poster.core.utils.key.RequestKey
    import com.piratemaker.postermaker.poster.databinding.ActivityPermissionBinding
    import com.piratemaker.postermaker.poster.activity_app.main.MainActivity
    import com.piratemaker.postermaker.poster.core.extensions.setGradientTextHeightColor
    import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
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
            setGradientTextHeightColor(binding.tvContinue, "#C4561B".toColorInt(), "#C4561B".toColorInt())
            val textRes =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.to_access_13 else R.string.to_access
    
            binding.txtPer.text = TextUtils.concat(
                createColoredText(R.string.allow, R.color.white),
                " ",
                createColoredText(R.string.app_name, R.color.white),
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

            // ✅ CHECK: Nếu user dismiss dialog → grantResults sẽ EMPTY
            if (grantResults.isEmpty()) {
                // User dismissed the dialog, không làm gì cả
                return
            }

           val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            when (requestCode) {
                RequestKey.STORAGE_PERMISSION_CODE -> {
                    viewModel.updateStorageGranted(sharePreference, granted)

                    // ✅ Detect "Don't ask again" - Chỉ lưu flag, KHÔNG set counter = 999
                    if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val canAskAgain = shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        if (!canAskAgain) {
                            // User clicked "Don't ask again" → Lưu flag
                            viewModel.markDontAskAgain(sharePreference, storage = true)
                        }
                    }
                }

                RequestKey.NOTIFICATION_PERMISSION_CODE -> {
                    viewModel.updateNotificationGranted(sharePreference, granted)

                    if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val canAskAgain = shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)
                        if (!canAskAgain) {
                            viewModel.markDontAskAgain(sharePreference, storage = false)
                        }
                    }
                }
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
                gone()
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