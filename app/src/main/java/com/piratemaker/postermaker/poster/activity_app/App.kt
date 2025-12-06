package com.piratemaker.postermaker.poster.activity_app

import android.app.Application
import com.lvt.ads.util.AdsApplication
import com.lvt.ads.util.AppOpenManager
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.splash.SplashActivity

class App : AdsApplication() {
    override fun onCreate() {
        super.onCreate()
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
    }

    override fun enableAdsResume(): Boolean {
        return true
    }

    override fun getListTestDeviceId(): MutableList<String>? {
        return null
    }

    override fun getResumeAdId(): String {
        return getString(R.string.open_resume)
    }



    override fun buildDebug(): Boolean {
        return true
    }
}

