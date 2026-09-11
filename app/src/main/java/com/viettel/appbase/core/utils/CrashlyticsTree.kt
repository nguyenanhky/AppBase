package com.viettel.appbase.core.utils

import android.util.Log
import com.viettel.appbase.data.repository.FirebaseProvider
import timber.log.Timber

class CrashlyticsTree(private val firebaseProvider: FirebaseProvider) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        firebaseProvider.crashlyticsOrNull()?.apply {
            setCustomKey("log_tag", tag.orEmpty())
            log(message)
            t?.let(::recordException)
        }
    }
}
