package com.charactor.avatar.maker.pfp.activity_app.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.utils.DataLocal
import com.charactor.avatar.maker.pfp.databinding.ActivityIntroBinding
import com.charactor.avatar.maker.pfp.activity_app.main.MainActivity
import com.charactor.avatar.maker.pfp.activity_app.permission.PermissionActivity
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import kotlin.system.exitProcess

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val introAdapter by lazy { IntroAdapter(this) }

    override fun setViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initVpg()
    }

    override fun viewListener() {
        binding.btnNext.setOnSingleClick { handleNext() }
    }

    override fun initText() {}

    override fun initActionBar() {}

    private fun initVpg() {
        binding.apply {
            binding.vpgTutorial.adapter = introAdapter
            binding.dotsIndicator.attachTo(binding.vpgTutorial)
            introAdapter.submitList(DataLocal.itemIntroList)
        }
    }

    private fun handleNext() {
        binding.apply {
            val nextItem = binding.vpgTutorial.currentItem + 1
            if (nextItem < DataLocal.itemIntroList.size) {
                vpgTutorial.setCurrentItem(nextItem, true)
            } else {
                val intent =
                    if (sharePreference.getIsFirstPermission()) {
                        Intent(this@IntroActivity, PermissionActivity::class.java)
                    } else {
                        Intent(this@IntroActivity, MainActivity::class.java)
                    }
                startActivity(intent)
                finishAffinity()
            }
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() { exitProcess(0) }
}