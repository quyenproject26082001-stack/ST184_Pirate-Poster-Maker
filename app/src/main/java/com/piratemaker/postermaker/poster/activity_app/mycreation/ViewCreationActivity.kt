package com.piratemaker.postermaker.poster.activity_app.mycreation

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.editsticker.EditStickerActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.*
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.helper.PermissionHelper
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.databinding.ActivityViewBinding
import com.piratemaker.postermaker.poster.dialog.YesNoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ViewCreationActivity : BaseActivity<ActivityViewBinding>() {

    private var imagePath: String? = null
    private var originalPhotoPath: String? = null
    private var bountyValue: String? = null

    private var isMyDesign: Boolean = false
    private var downloadPermissionDeniedCount = 0

    // ActivityResult launcher for EditStickerActivity
    private val editStickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val editedPath = data.getStringExtra("EDITED_IMAGE_PATH")
                val updatedBountyValue = data.getStringExtra("BOUNTY_VALUE")

                if (editedPath != null && imagePath != null) {
                    // Copy edited file to original location (in bounty_designs folder)
                    val editedFile = File(editedPath)
                    val originalFile = File(imagePath!!)

                    if (editedFile.exists() && originalFile.exists()) {
                        try {
                            editedFile.copyTo(originalFile, overwrite = true)

                            // Update bountyValue if it was changed
                            updatedBountyValue?.let { newValue ->
                                bountyValue = newValue
                            }

                            // Update metadata file with new bountyValue
                            updateMetadata()

                            // Reload image with cache bypass
                            Glide.with(this)
                                .load(originalFile)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .signature(com.bumptech.glide.signature.ObjectKey(originalFile.lastModified()))
                                .into(binding.imgPoster)

                            // Delete temp file
                            editedFile.delete()

                            // Notify MyCreationActivity to refresh
                            setResult(RESULT_OK)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    // Permission launcher for Android 8-9
    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
    // Permission launcher for Android 8-9
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            // ✅ Granted: Reset counter và proceed download
            downloadPermissionDeniedCount = 0
            proceedDownload()
        } else {
            // ❌ Denied: Tăng counter
            downloadPermissionDeniedCount++

            // Check if "Don't ask again" was clicked
            val canAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                true
            }

            if (!canAskAgain) {
                // 🚫 User clicked "Don't ask again" → Hiện dialog Settings
                goToSettings()
            } else if (downloadPermissionDeniedCount >= 2) {
                // ✅ Từ chối 2 lần → Hiện dialog Settings
            } else {
                // Show error toast
                Toast.makeText(
                    this,
                    strings(R.string.download_failed_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun setViewBinding(): ActivityViewBinding {
        return ActivityViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        imagePath = intent.getStringExtra("imagePath")

        isMyDesign = intent.getBooleanExtra("isMyDesign", false)

        // Load metadata if available
        imagePath?.let { path ->
            loadMetadata(path)

            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))  // ✅ Dùng timestamp
                    .into(binding.imgPoster)
            }
        }

        if (isMyDesign) {
            binding.btnEdit.visible()
        } else {
            binding.btnEdit.gone()
        }
    }

    override fun onResume() {
        super.onResume()

        imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))  // ✅ Dùng timestamp
                    .into(binding.imgPoster)
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Right button - Delete
            btnActionBarRight.setImageResource(R.drawable.ic_delete)
            btnActionBarRight.visible()

            // Hide other elements
            tvCenter.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
            cvLogo.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Back button
            actionBar.btnActionBarLeft.setOnSingleClick {
                showInterAll { finishAfterTransition() }
            }

            // Delete button
            actionBar.btnActionBarRight.setOnSingleClick {
                showDeleteConfirmation()
            }

            // Share button
            btnShare.setOnSingleClick(2000) {
                shareImage()
            }

            // Download button
            btnDownload.setOnSingleClick {
                if (!isMyDesign) {
                    showInterAll { downloadImage() }
                }
                else{
                    downloadImage()
                }
            }
            btnEdit.setOnSingleClick {
                openEditSticker()
            }
        }

    }

    private fun loadMetadata(imagePath: String) {
        try {
            // Get metadata file path (same name as image, but .json extension)
            val imageFile = File(imagePath)
            val metadataFileName = imageFile.nameWithoutExtension + ".json"
            val metadataFile = File(imageFile.parent, metadataFileName)

            if (metadataFile.exists()) {
                val metadataJson = metadataFile.readText()
                // Simple JSON parsing (format: {"originalPhotoPath": "...", "bountyValue": "..."})
                originalPhotoPath = metadataJson.substringAfter("\"originalPhotoPath\": \"").substringBefore("\"")
                bountyValue = metadataJson.substringAfter("\"bountyValue\": \"").substringBefore("\"")

                // Handle empty values
                if (originalPhotoPath?.isEmpty() == true) originalPhotoPath = null
                if (bountyValue?.isEmpty() == true) bountyValue = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If metadata loading fails, continue without it
            originalPhotoPath = null
            bountyValue = null
        }
    }

    private fun updateMetadata() {
        imagePath?.let { path ->
            try {
                val imageFile = File(path)
                val metadataFileName = imageFile.nameWithoutExtension + ".json"
                val metadataFile = File(imageFile.parent, metadataFileName)

                // Save updated metadata
                val metadataJson = buildString {
                    append("{\n")
                    append("  \"originalPhotoPath\": \"${originalPhotoPath?.replace("\\", "\\\\") ?: ""}\",\n")
                    append("  \"bountyValue\": \"${bountyValue ?: ""}\"\n")
                    append("}")
                }
                metadataFile.writeText(metadataJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openEditSticker() {
        imagePath?.let { path ->
            val intent = Intent(this, EditStickerActivity::class.java).apply {
                putExtra("IMAGE_PATH", path)
                putExtra("IS_EDITING_EXISTING", true)
                putExtra("ORIGINAL_PHOTO_PATH", originalPhotoPath)
                putExtra("BOUNTY_VALUE", bountyValue)
            }
            showInterAll { editStickerLauncher.launch(intent) }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    private fun showDeleteConfirmation() {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.delete,
            description = R.string.delete_design_confirmation
        )
        dialog.onYesClick = {
            deleteImage()
            dialog.dismiss()
        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteImage() {
        imagePath?.let { path ->
            val file = File(path)
            if (file.exists() && file.delete()) {
                Toast.makeText(this, R.string.design_deleted, Toast.LENGTH_SHORT).show()
                finishAfterTransition()
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage() {
        imagePath?.let { path ->
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
        } else if (downloadPermissionDeniedCount >= 2) {
            // Đã từ chối 2 lần → Hiện dialog Settings
            goToSettings()
        } else {
            // Hỏi quyền lần đầu hoặc lần 2
            permissionLauncher.launch(storagePermissions)
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (isMyDesign) {
            Admob.getInstance().loadNativeCollapNotBanner(
                this,
                getString(R.string.native_cl_Old_West_detail),
                binding.nativeCollapDetailDesgin
            )
        }
    }

    override fun initAds() {

        if (isMyDesign) {
           binding.nativeDetail.gone()
            Admob.getInstance().loadNativeCollapNotBanner(
                this,
                getString(R.string.native_cl_Old_West_detail),
                binding.nativeCollapDetailDesgin
            )
        } else {
            binding.nativeDetail.visible()

            Admob.getInstance().loadNativeAd(
                this,
                getString(R.string.native_detail),
                binding.nativeDetail,
                R.layout.ads_native_big_btn_top
            )
        }
    }


    /**
     * Proceed with download after permission check
     */
    private fun proceedDownload() {
        imagePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@ViewCreationActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@ViewCreationActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@ViewCreationActivity,
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
