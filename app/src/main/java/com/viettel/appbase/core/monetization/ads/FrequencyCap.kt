package com.viettel.appbase.core.monetization.ads

import kotlin.time.Duration

/**
 * At most one full-screen ad per [minInterval]. The first one also waits [minInterval] from session
 * start, so users are never hit by an ad right after opening the app.
 */
class FrequencyCap(private val minInterval: Duration, sessionStartMillis: Long) {
    private var lastShownMillis = sessionStartMillis

    fun canShow(nowMillis: Long): Boolean = nowMillis - lastShownMillis >= minInterval.inWholeMilliseconds

    fun recordShown(nowMillis: Long) {
        lastShownMillis = nowMillis
    }
}
