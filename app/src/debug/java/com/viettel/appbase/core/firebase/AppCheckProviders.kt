package com.viettel.appbase.core.firebase

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: logcat prints a debug token on first run ("DebugAppCheckProvider").
 * Register it in Firebase Console > App Check > Manage debug tokens, or requests are rejected.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
