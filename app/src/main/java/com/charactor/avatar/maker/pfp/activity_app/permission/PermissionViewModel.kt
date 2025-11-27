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

    // ✅ Session counter - Reset mỗi lần vào màn (không lưu SharedPreference)
    private var storageSessionCounter = 0
    private var notificationSessionCounter = 0

    fun updateStorageGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _storageGranted.value = granted

        if (granted) {
            storageSessionCounter = 0  // Reset nếu granted
        } else {
            storageSessionCounter++  // Tăng counter trong session
        }
    }

    fun updateNotificationGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _notificationGranted.value = granted

        if (granted) {
            notificationSessionCounter = 0
        } else {
            notificationSessionCounter++
        }
    }

    fun needGoToSettings(sharePrefer: SharePreferenceHelper, storage: Boolean): Boolean {
        return if (storage) {
            // Check flag "dontAskAgain" HOẶC counter session > 2
            sharePrefer.isDontAskAgainStorage() ||
            (storageSessionCounter > 2 && !_storageGranted.value)
        } else {
            sharePrefer.isDontAskAgainNotification() ||
            (notificationSessionCounter > 2 && !_notificationGranted.value)
        }
    }

    // ✅ Lưu flag "Don't ask again" vào SharedPreference
    fun markDontAskAgain(sharePrefer: SharePreferenceHelper, storage: Boolean) {
        if (storage) {
            sharePrefer.setDontAskAgainStorage(true)
        } else {
            sharePrefer.setDontAskAgainNotification(true)
        }
    }

    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}