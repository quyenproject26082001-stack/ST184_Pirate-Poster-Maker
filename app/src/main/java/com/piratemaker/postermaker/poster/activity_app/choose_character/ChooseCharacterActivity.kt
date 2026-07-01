package com.piratemaker.postermaker.poster.activity_app.choose_character

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.customize.CustomizeCharacterActivity
import com.piratemaker.postermaker.poster.activity_app.main.DataViewModel
import com.piratemaker.postermaker.poster.activity_app.mycreation.MyCreationActivity
import com.piratemaker.postermaker.poster.activity_app.sticker_customize.StickerCustomizeActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.hideNavigation
import com.piratemaker.postermaker.poster.core.extensions.loadNativeCollabAds
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.setImageActionBar
import com.piratemaker.postermaker.poster.core.extensions.setTextActionBar
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.tap
import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.InternetHelper
import com.piratemaker.postermaker.poster.core.utils.key.IntentKey
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.HandleState
import com.piratemaker.postermaker.poster.databinding.ActivityChooseCharacterBinding
import kotlinx.coroutines.launch

class ChooseCharacterActivity : BaseActivity<ActivityChooseCharacterBinding>() {
    private val viewModel: ChooseCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val chooseCharacterAdapter by lazy { ChooseCharacterAdapter() }
    private var hasCheckedInternet = false  // Flag to check internet only once
    override fun setViewBinding(): ActivityChooseCharacterBinding {
        return ActivityChooseCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        if (dataViewModel.allData.value.isEmpty()) {
            lifecycleScope.launch { showLoading() }
        }
        initRcv()
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { data ->
                if (data.isNotEmpty()) {
                    chooseCharacterAdapter.submitList(data)

                    // Dismiss loading when data is loaded
                    dismissLoading()

                    // Check if there are API characters and user has no internet
                    checkInternetForAPICharacters(data)
                }
            }
        }
    }

    private fun checkInternetForAPICharacters(data: ArrayList<com.piratemaker.postermaker.poster.data.model.custom.CustomizeModel>) {
        // Only check once per activity lifecycle
        if (hasCheckedInternet) return
        hasCheckedInternet = true

        android.util.Log.d("ChooseCharacter", "========================================")
        android.util.Log.d("ChooseCharacter", "checkInternetForAPICharacters called")
        android.util.Log.d("ChooseCharacter", "Total characters in data: ${data.size}")

        // Check if API characters are already loaded
        val hasAPICharacters = data.any { it.isFromAPI }
        val apiCount = data.count { it.isFromAPI }
        val localCount = data.count { !it.isFromAPI }

        android.util.Log.d("ChooseCharacter", "API characters: $apiCount")
        android.util.Log.d("ChooseCharacter", "Local characters: $localCount")
        android.util.Log.d("ChooseCharacter", "hasAPICharacters: $hasAPICharacters")

        // Only show notification if API characters are NOT loaded yet
        if (!hasAPICharacters) {
            android.util.Log.d("ChooseCharacter", "No API characters - checking internet...")
            InternetHelper.checkInternet(this) { state ->
                android.util.Log.d("ChooseCharacter", "Internet check result: $state")
                if (state != HandleState.SUCCESS) {
                    android.util.Log.d("ChooseCharacter", "❌ No internet - SHOWING DIALOG")
                    // No internet and no API characters loaded - notify user
                    val dialog = com.piratemaker.postermaker.poster.dialog.YesNoDialog(
                        this@ChooseCharacterActivity,
                        R.string.notification,
                        R.string.internet_required_for_more_characters,
                        isError = true,  // Shows only OK button
                        dialogType = com.piratemaker.postermaker.poster.dialog.DialogType.INTERNET
                    )
                    dialog.show()
                    dialog.onYesClick = {
                        dialog.dismiss()
                        hideNavigation()
                    }
                } else {
                    android.util.Log.d("ChooseCharacter", "✓ Has internet - no dialog")
                }
            }
        } else {
            android.util.Log.d("ChooseCharacter", "✓ API characters already loaded - no dialog")
        }
        android.util.Log.d("ChooseCharacter", "========================================")
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
            actionBar.btnActionBarRight.tap {
                showInterAll {
                    startIntentRightToLeft(MyCreationActivity::class.java, 0)
                }
            }
            btnMaker.tap(1000) {
                startIntentRightToLeft(CustomizeCharacterActivity::class.java, 0)
            }
            btnSticker.tap(1000) {
                startIntentRightToLeft(StickerCustomizeActivity::class.java)
            }
        }
        chooseCharacterAdapter.onItemClick = { position ->

            android.util.Log.d("ChooseCharacter", "========================================")
            android.util.Log.d("ChooseCharacter", "Item clicked: position $position")

            // ✅ FIX: Use isFromAPI flag from character data instead of position
            val selectedCharacter = dataViewModel.allData.value.getOrNull(position)
            val needsInternet = selectedCharacter?.isFromAPI ?: false


            // Log AdMob event with detailed information
            val bundle = Bundle()
            bundle.putString("character_name", selectedCharacter?.dataName ?: "unknown")
            bundle.putString("avatar_path", selectedCharacter?.avatar ?: "unknown")
            bundle.putInt("position", position)
            bundle.putBoolean("is_from_api", needsInternet)
            AdmobEvent.logEvent(this@ChooseCharacterActivity, "click_character_item", bundle)



            android.util.Log.d("ChooseCharacter", "Character isFromAPI: $needsInternet")
            android.util.Log.d("ChooseCharacter", "Character name: ${selectedCharacter?.dataName}")

            if (needsInternet) {
                android.util.Log.d("ChooseCharacter", "API character - checking internet...")
                InternetHelper.checkInternet(this) { state ->
                    if (state == HandleState.SUCCESS) {
                        showInterAll {
                            startIntentRightToLeft(
                                CustomizeCharacterActivity::class.java,
                                position
                            )
                        }
                    } else {
                        // Show No Internet dialog
                        val dialog = com.piratemaker.postermaker.poster.dialog.YesNoDialog(
                            this@ChooseCharacterActivity,
                            R.string.no_internet,
                            R.string.please_check_your_internet,
                            isError = true,
                            dialogType = com.piratemaker.postermaker.poster.dialog.DialogType.INTERNET
                        )
                        dialog.show()
                        dialog.onYesClick = {
                            dialog.dismiss()
                            hideNavigation()
                        }
                    }
                }
            } else {
                android.util.Log.d("ChooseCharacter", "Local character - navigating directly")
                android.util.Log.d("ChooseCharacter", "========================================")
                showInterAll {
                    startIntentRightToLeft(
                        CustomizeCharacterActivity::class.java,
                        position
                    )
                }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setImageActionBar(btnActionBarRight, R.drawable.ic_mycreation)
        }
    }

    private fun initRcv() {
        binding.rcvCharacter.apply {
            adapter = chooseCharacterAdapter
            itemAnimator = null
        }
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeAd(
            this,
            getString(R.string.native_collap_OCMaker),
            binding.nativeOC,
            R.layout.ads_native_big_btn_top
        )
    }

    override fun initAds() {
        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_language),
//            binding.nativeLanguage,
//            R.layout.ads_native_big
//        )
    }

    override fun onRestart() {
        super.onRestart()
        initNativeCollab()
    }

}
