package com.charactor.avatar.maker.pfp.activity_app.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.rateApp
import com.charactor.avatar.maker.pfp.core.extensions.select
import com.charactor.avatar.maker.pfp.core.extensions.startIntentRightToLeft
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.core.helper.LanguageHelper
import com.charactor.avatar.maker.pfp.core.helper.MediaHelper
import com.charactor.avatar.maker.pfp.core.utils.key.ValueKey
import com.charactor.avatar.maker.pfp.core.utils.state.RateState
import com.charactor.avatar.maker.pfp.databinding.ActivityHomeBinding
import com.charactor.avatar.maker.pfp.activity_app.SettingsActivity
import com.charactor.avatar.maker.pfp.activity_app.mydesign.MyDesignActivity
import com.charactor.avatar.maker.pfp.activity_app.posterwanted.PosterWantedTemplateActivity
import com.charactor.avatar.maker.pfp.core.extensions.gone

import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseActivity<ActivityHomeBinding>() {

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        deleteTempFolder()
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.setOnSingleClick { startIntentRightToLeft(SettingsActivity::class.java) }
            btnCreate.setOnSingleClick {
                startIntentRightToLeft(com.charactor.avatar.maker.pfp.activity_app.makescreen.MakeScreenActivity::class.java)
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

    private fun updateText() {
        binding.apply {
            tv1.text = strings(R.string.character_maker)
        }
    }

    override fun onRestart() {
        super.onRestart()
        LanguageHelper.setLocale(this)
        updateText()
    }
}