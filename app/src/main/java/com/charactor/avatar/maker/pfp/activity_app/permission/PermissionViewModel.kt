package com.charactor.avatar.maker.pfp.activity_app.permission

import androidx.lifecycle.ViewModel
import com.charactor.avatar.maker.pfp.core.helper.PermissionHelper
import com.charactor.avatar.maker.pfp.core.helper.SharePreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PermissionViewModel : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted

    fun updateStorageGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _storageGranted.value = granted
        sharePrefer.setStoragePermission(if (granted) 0 else sharePrefer.getStoragePermission() + 1)
    }

    fun updateNotificationGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _notificationGranted.value = granted
        sharePrefer.setNotificationPermission(if (granted) 0 else sharePrefer.getNotificationPermission() + 1)
    }

    fun needGoToSettings(sharePrefer: SharePreferenceHelper, storage: Boolean): Boolean {
        return if (storage) {
            sharePrefer.getStoragePermission() > 2 && !_storageGranted.value
        } else {
            sharePrefer.getNotificationPermission() > 2 && !_notificationGranted.value
        }
    }

    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}