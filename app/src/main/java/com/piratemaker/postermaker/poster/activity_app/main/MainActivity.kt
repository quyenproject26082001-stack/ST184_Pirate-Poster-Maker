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
import com.piratemaker.postermaker.poster.activity_app.mydesign.MyDesignActivity
import com.piratemaker.postermaker.poster.activity_app.posterwanted.PosterWantedTemplateActivity
import com.piratemaker.postermaker.poster.core.extensions.gone

import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseActivity<ActivityHomeBinding>() {

    private var currentLanguage: String = ""

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
            tv1.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            tv2.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            tv3.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.setOnSingleClick { startIntentRightToLeft(SettingsActivity::class.java) }
            btnCreate.setOnSingleClick {
                startIntentRightToLeft(com.piratemaker.postermaker.poster.activity_app.makescreen.MakeScreenActivity::class.java)
            }
            BtnPosterWantedTemplate.setOnSingleClick {
                startIntentRightToLeft(PosterWantedTemplateActivity::class.java)
            }
            btnMydesgin.setOnSingleClick {
                startIntentRightToLeft(MyDesignActivity::class.java)
            }
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

    // updateText() đã bị remove - không cần update text mỗi lần restart
    // Text được load từ XML layout, tự động update khi language thay đổi

    override fun onRestart() {
        super.onRestart()
        // Không làm gì cả - tránh redraw gây flicker
        // Language đã được set trong onCreate/initView
    }
}