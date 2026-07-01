package com.piratemaker.postermaker.poster.activity_app.maker_view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.checkPermissions
import com.piratemaker.postermaker.poster.core.extensions.goToSettings
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.hideNavigation
import com.piratemaker.postermaker.poster.core.extensions.invisible
import com.piratemaker.postermaker.poster.core.extensions.loadImage
import com.piratemaker.postermaker.poster.core.extensions.loadImageFromFile
import com.piratemaker.postermaker.poster.core.extensions.loadNativeCollabAds
import com.piratemaker.postermaker.poster.core.extensions.requestPermission
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.setTextActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.strings
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.LanguageHelper
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.helper.UnitHelper
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.RequestKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.databinding.ActivityMakerViewBinding
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import com.piratemaker.postermaker.poster.activity_app.customize.CustomizeCharacterActivity
import com.piratemaker.postermaker.poster.activity_app.main.DataViewModel
import com.piratemaker.postermaker.poster.activity_app.mycreation.MyCreationActivity
import com.piratemaker.postermaker.poster.activity_app.permission.PermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewActivity : BaseActivity<ActivityMakerViewBinding>() {
    private val viewModel: ViewViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    override fun setViewBinding(): ActivityMakerViewBinding {
        return ActivityMakerViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        dataViewModel.ensureData(this)
        viewModel.setPath(intent.getStringExtra(IntentKey.INTENT_KEY)!!)
        viewModel.updateStatusFrom(intent.getIntExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE))

        setButtonBackgrounds()
        setupUI()
    }

    private fun setButtonBackgrounds() {

    }

    private fun setupUI() {
        binding.apply {
            actionBar.apply {
                if (viewModel.statusFrom == ValueKey.AVATAR_TYPE) {
                    setImageActionBar(btnActionBarNextToRight, R.drawable.ic_edit_view)
                    btnActionBarNextToRight.visible()
                } else {
                    btnActionBarNextToRight.gone()
                }
                setImageActionBar(btnActionBarRight, R.drawable.ic_delete_item)

            }

            // Set scaleType based on content type
            if (viewModel.statusFrom == ValueKey.AVATAR_TYPE) {
                // For avatars, use fitCenter to show full character without cropping
                imvImage.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            } else {
                // For designs, use center to maintain original size
                imvImage.scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }
    }

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newPath =
                    result.data?.getStringExtra("NEW_PATH") ?: return@registerForActivityResult
                viewModel.setPath(newPath)
                binding.imvImage.loadImageFromFile(newPath)
            }
        }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pathInternal.collect { path ->
                    loadImage(this@ViewActivity, path, binding.imvImage)
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap(1000) { showInterAll { handleBack() } }
                btnActionBarRight.tap(1000) { handleDelete() }
                btnActionBarNextToRight.tap(1000) { openEditCharacter() }
            }

            btnShare.tap(2590) {
                viewModel.shareFiles(this@ViewActivity)
            }
            btnDownload.tap(2000) {
                checkStoragePermission()
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // tvCenter.select()

            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            if (viewModel.statusFrom == ValueKey.AVATAR_TYPE) {
                setImageActionBar(btnActionBarNextToRight, R.drawable.ic_edit_view)
                btnActionBarNextToRight.visible()
            } else {
                btnActionBarNextToRight.gone()
            }
            setImageActionBar(btnActionBarRight, R.drawable.ic_delete_item)
        }
    }

    private fun openEditCharacter() {
        if (viewModel.statusFrom != ValueKey.AVATAR_TYPE) return

        val editList = MediaHelper.readListFromFile<com.piratemaker.postermaker.poster.data.model.custom.SuggestionModel>(
            this,
            ValueKey.EDIT_FILE_INTERNAL
        )
        val model = editList.firstOrNull { it.pathInternalEdit == viewModel.pathInternal.value } ?: return
        MediaHelper.writeModelToFile(this, ValueKey.SUGGESTION_FILE_INTERNAL, model)

        val intent = Intent(this, CustomizeCharacterActivity::class.java).apply {
            putExtra(IntentKey.INTENT_KEY, 0)
            putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.EDIT)
        }
        editLauncher.launch(intent)
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
            viewModel.downloadFiles(this@ViewActivity).collect { state ->
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

    private fun handleDelete() {
        val dialog =
            YesNoDialog(this, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(this)
        dialog.show()
        dialog.onNoClick = {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onYesClick = {
            dialog.dismiss()
            lifecycleScope.launch {
                viewModel.deleteFile(this@ViewActivity, viewModel.pathInternal.value)
                    .collect { state ->
                        when (state) {
                            HandleState.LOADING -> showLoading()
                            HandleState.SUCCESS -> {
                                dismissLoading()
//                                resetMyCreationSelectionMode()

                                setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra("DELETED_PATH", viewModel.pathInternal.value)
                                })
                                finish()
                            }

                            else -> {
                                dismissLoading()
                                showToast(R.string.delete_failed_please_try_again)
                            }
                        }
                    }
            }
        }
    }

    private fun handleBack() {
        setResult(Activity.RESULT_OK)
//        resetMyCreationSelectionMode()
        handleBackLeftToRight()
    }

//    private fun resetMyCreationSelectionMode() {
//        val myCreationActivity = MyCreationActivity.getInstance()
//        if (myCreationActivity != null && !
//            myCreationActivity.isFinishing && !
//            myCreationActivity.isDestroyed) {
//            android.util.Log.d("ViewActivity", "Resetting selection mode in MyCreationActivity")
//
//            val designFragment =
//                myCreationActivity.supportFragmentManager.findFragmentByTag("MyDesignFragment")
//            if (designFragment is com.piratemaker.postermaker.poster.ui.my_creation.fragment.MyDesignFragment) {
//                designFragment.resetSelectionMode()
//            }
//
//            val avatarFragment =
//                myCreationActivity.supportFragmentManager.findFragmentByTag("MyAvatarFragment")
//            if (avatarFragment is MyAvatarFragment) {
//                avatarFragment.resetSelectionMode()
//            }
//
//            myCreationActivity.exitSelectionMode()
//        } else {
//            android.util.Log.w(
//                "ViewActivity",
//                "MyCreationActivity instance not found - unable to reset selection mode"
//            )
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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

        loadNativeCollabAds(R.string.native_detail, binding.flNativeCollab)


    }

    @android.annotation.SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBack()
    }
}
