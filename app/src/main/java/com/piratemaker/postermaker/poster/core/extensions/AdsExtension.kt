package com.piratemaker.postermaker.poster.core.extensions

import android.app.Activity
import android.widget.FrameLayout
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob

fun Activity.showInterAll(onFinishInter: () -> Unit) {
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
            onFinishInter.invoke()
        }
    })
}

fun Activity.loadNativeCollabAds(id: String, layout: FrameLayout) {
    Admob.getInstance().loadNativeCollap(this, id, layout)
}

fun Activity.loadNativeCollabAds(id: Int, layout: FrameLayout) {
    Admob.getInstance().loadNativeCollap(this, getString(id), layout)
}
