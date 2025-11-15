package com.charactor.avatar.maker.pfp.activity_app.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.charactor.avatar.maker.pfp.core.base.BaseActivity
import com.charactor.avatar.maker.pfp.core.extensions.initNetworkMonitor
import com.charactor.avatar.maker.pfp.core.utils.state.HandleState
import com.charactor.avatar.maker.pfp.databinding.ActivitySplashBinding
import com.charactor.avatar.maker.pfp.activity_app.intro.IntroActivity
import com.charactor.avatar.maker.pfp.activity_app.language.LanguageActivity
import com.charactor.avatar.maker.pfp.activity_app.main.DataViewModel
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    var intentActivity: Intent? = null
    private val dataViewModel: DataViewModel by viewModels()
    override fun setViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        if (!isTaskRoot &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.action != null &&
            intent.action.equals(Intent.ACTION_MAIN)) {
            finish(); return
        }

        intentActivity = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }
        initNetworkMonitor()
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { dataList ->
                if (dataList.isNotEmpty()){
                    dataViewModel.getAllParts(this@SplashActivity).collect { dataAPI ->
                        when(dataAPI){
                            HandleState.LOADING -> {}
                            else -> {
                                startActivity(intentActivity)
                                finishAffinity()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
    }

    override fun initText() {}

    override fun initActionBar() {}

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {}
}