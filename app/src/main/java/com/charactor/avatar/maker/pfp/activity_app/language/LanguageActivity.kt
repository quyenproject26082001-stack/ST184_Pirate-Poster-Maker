package com.charactor.avatar.maker.pfp.activity_app.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.charactor.avatar.maker.pfp.R
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.handleBackLeftToRight
import com.charactor.avatar.maker.pfp.core.extensions.select
import com.charactor.avatar.maker.pfp.core.extensions.showToast
import com.charactor.avatar.maker.pfp.core.extensions.startIntentRightToLeft
import com.charactor.avatar.maker.pfp.core.extensions.startIntentWithClearTop
import com.charactor.avatar.maker.pfp.core.extensions.visible
import com.charactor.avatar.maker.pfp.core.utils.key.IntentKey
import com.charactor.avatar.maker.pfp.databinding.ActivityLanguageBinding
import com.charactor.avatar.maker.pfp.activity_app.main.MainActivity
import com.charactor.avatar.maker.pfp.activity_app.intro.IntroActivity
import com.charactor.avatar.maker.pfp.core.extensions.setOnSingleClick
import com.charactor.avatar.maker.pfp.core.extensions.strings
import com.charactor.avatar.maker.pfp.ui.language.LanguageViewModel
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private val viewModel: LanguageViewModel by viewModels()

    private val languageAdapter by lazy { LanguageAdapter(this) }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRcv()
        val intentValue = intent.getStringExtra(IntentKey.INTENT_KEY)
        val currentLang = sharePreference.getPreLanguage()
        viewModel.setFirstLanguage(intentValue == null)
        viewModel.loadLanguages(currentLang)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            viewModel.isFirstLanguage.collect { isFirst ->
                if (isFirst) {
                    binding.actionBar.tvStart.visible()
                } else {
                    binding.actionBar.btnActionBarLeft.visible()
                    binding.actionBar.tvCenter.visible()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.languageList.collect { list ->
                languageAdapter.submitList(list)
            }
        }
        lifecycleScope.launch {
            viewModel.codeLang.collect { code ->
                if (code.isNotEmpty()) {
                    binding.actionBar.btnActionBarRight.visible()
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.setOnSingleClick { handleBackLeftToRight() }
            actionBar.btnActionBarRight.setOnSingleClick { handleDone() }
        }
        handleRcv()
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
        binding.actionBar.tvStart.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarRight.setImageResource(R.drawable.ic_done)
            val text = R.string.language
            tvCenter.text = strings(text)
            tvStart.text = strings(text)
        }
    }

    private fun initRcv() {
        binding.rcv.apply {
            adapter = languageAdapter
            itemAnimator = null
        }
    }

    private fun handleRcv() {
        binding.apply {
            languageAdapter.onItemClick = { code ->
                binding.actionBar.btnActionBarRight.visible()
                viewModel.selectLanguage(code)
            }
        }
    }

    private fun handleDone() {
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }
        sharePreference.setPreLanguage(code)

        if (viewModel.isFirstLanguage.value) {
            sharePreference.setIsFirstLang(false)
            startIntentRightToLeft(IntroActivity::class.java)
            finishAffinity()
        } else {
            startIntentWithClearTop(MainActivity::class.java)
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!viewModel.isFirstLanguage.value) {
            handleBackLeftToRight()
        } else {
            exitProcess(0)
        }
    }


}