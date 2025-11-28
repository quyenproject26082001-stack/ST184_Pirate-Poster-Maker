package com.piratemaker.postermaker.poster.activity_app.permission

import androidx.lifecycle.ViewModel
import com.piratemaker.postermaker.poster.core.helper.PermissionHelper
import com.piratemaker.postermaker.poster.core.helper.SharePreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PermissionViewModel : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted

    // ✅ BỎ SESSION COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"

    fun updateStorageGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _storageGranted.value = granted
        // Không còn counter nữa
    }

    fun updateNotificationGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _notificationGranted.value = granted
        // Không còn counter nữa
    }

    fun needGoToSettings(sharePrefer: SharePreferenceHelper, storage: Boolean): Boolean {
        // ✅ CHỈ check flag "Don't ask again", KHÔNG check counter
        return if (storage) {
            sharePrefer.isDontAskAgainStorage()
        } else {
            sharePrefer.isDontAskAgainNotification()
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