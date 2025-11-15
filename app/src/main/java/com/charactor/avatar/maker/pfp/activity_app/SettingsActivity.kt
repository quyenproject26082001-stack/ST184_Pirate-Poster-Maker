package com.charactor.avatar.maker.pfp.activity_app

import android.view.LayoutInflater
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.gone
import com.charactor.avatar.maker.pfp.core.extensions.handleBackLeftToRight
import com.charactor.avatar.maker.pfp.core.extensions.policy
import com.charactor.avatar.maker.pfp.core.extensions.rateApp
import com.charactor.avatar.maker.pfp.core.extensions.select
import com.charactor.avatar.maker.pfp.core.extensions.shareApp
import com.charactor.avatar.maker.pfp.core.extensions.startIntentRightToLeft
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.core.utils.key.IntentKey
import com.charactor.avatar.maker.pfp.core.utils.state.RateState
import com.charactor.avatar.maker.pfp.databinding.ActivitySettingsBinding
import com.charactor.avatar.maker.pfp.activity_app.language.LanguageActivity
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.strings
import kotlin.jvm.java

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRate()
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.setOnSingleClick { handleBackLeftToRight() }
            btnLang.setOnSingleClick { startIntentRightToLeft(LanguageActivity::class.java, IntentKey.INTENT_KEY) }
            btnShare.setOnSingleClick(1500) { shareApp() }
            btnRate.setOnSingleClick {
                rateApp(sharePreference) { state ->
                    if (state != RateState.CANCEL) {
                        binding.btnRate.gone()
                    }
                }
            }
            btnPolicy.setOnSingleClick(1500) { policy() }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = strings(R.string.settings)
            tvCenter.visible()

            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
        }
    }

    private fun initRate() {
        if (sharePreference.getIsRate(this)) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
    }
}