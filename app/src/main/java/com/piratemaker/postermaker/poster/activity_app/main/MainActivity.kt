package com.piratemaker.postermaker.poster.activity_app.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.rateApp
import com.piratemaker.postermaker.poster.core.extensions.select
import com.piratemaker.postermaker.poster.core.extensions.startIntentRightToLeft
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.core.helper.LanguageHelper
import com.piratemaker.postermaker.poster.core.helper.MediaHelper
import com.piratemaker.postermaker.poster.core.utils.key.ValueKey
import com.piratemaker.postermaker.poster.core.utils.state.RateState
import com.piratemaker.postermaker.poster.databinding.ActivityHomeBinding
import com.piratemaker.postermaker.poster.activity_app.SettingsActivity
import com.piratemaker.postermaker.poster.activity_app.bountyfilter.BountyFilterActivity
import com.piratemaker.postermaker.poster.activity_app.mydesign.MyDesignActivity
import com.piratemaker.postermaker.poster.activity_app.posterwanted.PosterWantedTemplateActivity
import com.piratemaker.postermaker.poster.core.extensions.gone

import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.strings
//quyen
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
//quyen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseActivity<ActivityHomeBinding>() {

    private var currentLanguage: String = ""
    //quyen
    //quyen

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        deleteTempFolder()
        currentLanguage = sharePreference.getPreLanguage()

        // Disable window transitions để tránh flicker
        with(window) {
            enterTransition = null
            exitTransition = null
            reenterTransition = null
            returnTransition = null
        }

        // Enable hardware layer cho TextViews để giảm redraw
        binding.apply {
            tv0.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            tv1.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            tv2.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            tv3.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }

    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.setOnSingleClick { startIntentRightToLeft(SettingsActivity::class.java) }
            //quyen
            btnBountyFilter.setOnSingleClick {
               showInterAll {
                   startIntentRightToLeft(BountyFilterActivity::class.java)
               }
            }
            btnCreate.setOnSingleClick {
                showInterAll{
                        startIntentRightToLeft(com.piratemaker.postermaker.poster.activity_app.makescreen.MakeScreenActivity::class.java)
                    }

            }
            //quyen
            BtnPosterWantedTemplate.setOnSingleClick {
                //quyen

                        startIntentRightToLeft(PosterWantedTemplateActivity::class.java)

                //quyen
            }
            //quyen
            btnMydesgin.setOnSingleClick {
                //quyen
                showInterAll {
                        startIntentRightToLeft(MyDesignActivity::class.java)
                    }

                //quyen
            }
            //quyen
        }
    }

    override fun initText() {
        super.initText()
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            cvLogo.gone()
            tvCenter.text = strings(R.string.character_maker)
            tvCenter.gone()
            tvCenter.gone()
            btnActionBarRight.setImageResource(R.drawable.ic_settings)
            btnActionBarRight.visible()
        }
    }

    //quyen
    override fun initAds() {
        // Load interstitial ad
        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
        Admob.getInstance().loadNativeAll(this, getString(R.string.native_all))

        // Load native collapsible ad
        initNativeCollab()
    }
    //quyen

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!sharePreference.getIsRate(this) && sharePreference.getCountBack() % 2 == 0) {
            rateApp(sharePreference) { state ->
                when (state) {
                    RateState.LESS3 -> {
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(1000)
                            finishAffinity()
                        }
                    }
                    RateState.GREATER3 -> {
                        finishAffinity()
                    }
                    RateState.CANCEL -> {
                        lifecycleScope.launch {
                            sharePreference.setCountBack(sharePreference.getCountBack() + 1)
                            withContext(Dispatchers.Main) {
                                delay(1000)
                                finishAffinity()
                            }
                        }
                    }
                }
            }
        } else {
            sharePreference.setCountBack(sharePreference.getCountBack() + 1)
            finishAffinity()
        }
    }

    private fun deleteTempFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataTemp = MediaHelper.getImageInternal(this@MainActivity, ValueKey.DOWNLOAD_ALBUM_BACKGROUND)
            if (dataTemp.isNotEmpty()) {
                dataTemp.forEach {
                    val file = File(it)
                    file.delete()
                }
            }
        }
    }

    private fun updateText() {
        // Sử dụng View.post để update sau khi view đã stable → tránh flicker
        binding.root.post {
            binding.apply {
                // Fade out nhanh → update text → fade in
                // Điều này tạo smooth transition thay vì sudden change
                listOf(tv0, tv1, tv2, tv3).forEach { textView ->
                    textView.animate()
                        .alpha(0f)
                        .setDuration(50)
                        .withEndAction {
                            // Update text khi đã invisible
                            when (textView) {
                                tv0 -> textView.text = strings(R.string.bountyFilter)
                                tv1 -> textView.text = strings(R.string.posterwantedmaker)
                                tv2 -> textView.text = strings(R.string.posterwantedtemplates)
                                tv3 -> textView.text = strings(R.string.my_design)
                            }
                            // Fade in lại
                            textView.animate()
                                .alpha(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
            }
        }
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,
            getString(R.string.native_cl_home),
            binding.nativeClHome)
    }
    override fun onRestart() {
        super.onRestart()
        // Chỉ update text khi language thực sự thay đổi
        val newLanguage = sharePreference.getPreLanguage()
        if (currentLanguage != newLanguage) {
            LanguageHelper.setLocale(this)
            currentLanguage = newLanguage
            // Delay update để view đã được render xong
            updateText()
        }
        //quyen
        // Reload native collapsible ad
       initNativeCollab()
        //quyen
    }
}