package com.viettel.appbase.core.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseProvider(private val context: Context) {
    fun initialize(): Boolean = FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null

    fun authOrNull(): FirebaseAuth? = if (initialize()) FirebaseAuth.getInstance() else null

    fun crashlyticsOrNull(): FirebaseCrashlytics? = if (initialize()) FirebaseCrashlytics.getInstance() else null

    fun appCheckOrNull(): FirebaseAppCheck? = if (initialize()) FirebaseAppCheck.getInstance() else null

    /** Call once at startup, before any request that needs an App Check token. */
    fun installAppCheck() {
        appCheckOrNull()?.installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
