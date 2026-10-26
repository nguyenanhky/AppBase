package com.viettel.appbase.core.monetization.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.viettel.appbase.core.monetization.MonetizationConfig
import org.koin.compose.koinInject

/** Place once at the app root: restores purchases, asks for consent where required, starts the ads SDK. */
@Composable
fun MonetizationInitializer(controller: AdsController = koinInject()) {
    val activity = LocalActivity.current
    LaunchedEffect(activity, controller) { activity?.let { controller.initialize(it) } }
}

/** Anchored adaptive banner; renders nothing when ads are off, not consented, or removed by purchase. */
@Composable
fun AppBannerAd(
    modifier: Modifier = Modifier,
    controller: AdsController = koinInject(),
    config: MonetizationConfig = koinInject(),
) {
    val visible by controller.adsVisible.collectAsStateWithLifecycle()
    if (!visible) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = config.bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
        LifecycleResumeEffect(adView) {
            adView.resume()
            onPauseOrDispose { adView.pause() }
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            onRelease = { it.destroy() },
        )
    }
}

/**
 * Returns a callback that shows an interstitial if one is loaded and the frequency cap allows.
 * Call it at natural breaks (level finished, back from a detail screen), never on app open or mid-task.
 */
@Composable
fun rememberInterstitialAd(controller: AdsController = koinInject()): () -> Unit {
    val activity = LocalActivity.current
    return remember(activity, controller) { { activity?.let { controller.showInterstitial(it) } } }
}
