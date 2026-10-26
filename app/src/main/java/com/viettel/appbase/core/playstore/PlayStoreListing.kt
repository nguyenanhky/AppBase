package com.viettel.appbase.core.playstore

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.viettel.appbase.BuildConfig

/** Opens the app's Play Store page (for "Rate app" buttons); falls back to the browser without Play Store. */
fun Context.openPlayStoreListing(packageName: String = BuildConfig.PLAY_STORE_PACKAGE) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
    }
}
