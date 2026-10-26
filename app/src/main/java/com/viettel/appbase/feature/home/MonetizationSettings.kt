package com.viettel.appbase.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.viettel.appbase.R
import com.viettel.appbase.ui.components.AppButton
import com.viettel.appbase.ui.components.AppButtonStyle
import com.viettel.appbase.ui.theme.LocalAppSpacing

/** "Remove ads" purchase, restore, and the privacy options entry UMP requires in some regions. */
@Composable
internal fun MonetizationSettings(
    state: MonetizationUiState,
    onRemoveAds: () -> Unit,
    onRestorePurchases: () -> Unit,
    onPrivacyOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.adsEnabled) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.small)) {
        Text(stringResource(R.string.ads_title), style = MaterialTheme.typography.titleLarge)
        when {
            state.adFree -> Text(stringResource(R.string.ad_free_active))

            state.purchasePending -> Text(stringResource(R.string.purchase_pending))

            else -> AppButton(
                text = state.removeAdsPrice?.let { stringResource(R.string.remove_ads_price, it) }
                    ?: stringResource(R.string.remove_ads),
                onClick = onRemoveAds,
            )
        }
        AppButton(stringResource(R.string.restore_purchases), onClick = onRestorePurchases, style = AppButtonStyle.TEXT)
        if (state.privacyOptionsRequired) {
            AppButton(stringResource(R.string.privacy_options), onClick = onPrivacyOptions, style = AppButtonStyle.TEXT)
        }
    }
}
