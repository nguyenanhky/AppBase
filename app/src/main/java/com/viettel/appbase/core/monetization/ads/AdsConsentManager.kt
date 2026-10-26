package com.viettel.appbase.core.monetization.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/**
 * Google UMP consent (GDPR/EEA, US states). The form only appears where the law requires it;
 * configure the messages in AdMob > Privacy & messaging.
 */
class AdsConsentManager(context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    /** Settings must then offer a "Privacy options" entry that calls [showPrivacyOptionsForm]. */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Refreshes consent status and shows the form if required. Failures fall back to the stored consent. */
    suspend fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        val updated = suspendCancellableCoroutine { continuation ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                { continuation.resume(true) },
                { error ->
                    Timber.d("Consent info update failed: %s", error.message)
                    continuation.resume(false)
                },
            )
        }
        if (!updated) return
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                error?.let { Timber.d("Consent form failed: %s", it.message) }
                continuation.resume(Unit)
            }
        }
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Timber.d("Privacy options form failed: %s", it.message) }
        }
    }
}
