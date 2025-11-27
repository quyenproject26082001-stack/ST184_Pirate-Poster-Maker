package com.piratemaker.postermaker.poster.core.extensions

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import com.piratemaker.postermaker.poster.core.helper.RateHelper
import com.piratemaker.postermaker.poster.core.helper.SharePreferenceHelper
import com.piratemaker.postermaker.poster.core.utils.state.RateState

fun Activity.shareApp() {
    ShareCompat.IntentBuilder.from(this).setType("text/plain").setChooserTitle("Chooser title")
        .setText("http://play.google.com/store/apps/details?id=" + (this).packageName)
        .startChooser()
}

fun Activity.policy() {
    val url = "https://sites.google.com/view/pirate-poster-maker/home"
    val i = Intent(Intent.ACTION_VIEW)
    i.data = url.toUri()
    startActivity(i)
}
fun Activity.rateApp(
    sharePreference: SharePreferenceHelper,
    onRateResult: (RateState) -> Unit = {}
) {
    RateHelper.showRateDialog(this, sharePreference, onRateResult)
}
