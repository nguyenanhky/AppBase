package com.viettel.appbase.feature.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.viettel.appbase.R
import com.viettel.appbase.core.image.rememberImagePicker
import com.viettel.appbase.ui.components.AppButton
import com.viettel.appbase.ui.components.AppButtonStyle
import com.viettel.appbase.ui.error.toUserMessage
import com.viettel.appbase.ui.theme.LocalAppSpacing
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

/** Generic "photo → AI → result" demo; real apps copy this flow with their own task and result UI. */
@Composable
fun ScanScreen(modifier: Modifier = Modifier, viewModel: ScanViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberImagePicker(viewModel::onImagePicked)
    val spacing = LocalAppSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(stringResource(R.string.scan_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.scan_disclaimer), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            AppButton(
                text = stringResource(R.string.scan_take_photo),
                onClick = picker.takePhoto,
                modifier = Modifier.weight(1f),
                enabled = !state.isScanning,
            )
            AppButton(
                text = stringResource(R.string.scan_pick_photo),
                onClick = picker.pickFromGallery,
                modifier = Modifier.weight(1f),
                enabled = !state.isScanning,
                style = AppButtonStyle.OUTLINED,
            )
        }
        state.quota?.let {
            Text(
                pluralStringResource(R.plurals.ai_quota_remaining, it.remaining, it.remaining),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.selectedImage?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium),
            )
        }
        if (state.isScanning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.scan_analyzing))
            }
        }
        state.error?.let { Text(it.toUserMessage(), color = MaterialTheme.colorScheme.error) }
        state.result?.let { ScanResultCard(result = it, reported = state.reported, onReport = viewModel::reportResult) }
        if (state.history.isNotEmpty()) {
            Text(stringResource(R.string.scan_history_title), style = MaterialTheme.typography.titleLarge)
            state.history.forEach { ScanHistoryRow(it) }
        }
    }
}

@Composable
private fun ScanResultCard(
    result: DemoScanResult,
    reported: Boolean,
    onReport: () -> Unit,
) {
    val spacing = LocalAppSpacing.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (!result.recognized) {
                Text(stringResource(R.string.scan_not_recognized), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(result.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                result.scientificName?.let { Text(it, fontStyle = FontStyle.Italic) }
                result.confidence?.let {
                    Text(stringResource(R.string.scan_confidence, (it * PERCENT).roundToInt()))
                }
                result.summary?.let { Text(it) }
                if (result.advice.isNotEmpty()) {
                    Text(stringResource(R.string.scan_advice_title), style = MaterialTheme.typography.titleSmall)
                    result.advice.forEach { Text("• $it") }
                }
            }
            if (reported) {
                Text(stringResource(R.string.scan_reported), style = MaterialTheme.typography.bodySmall)
            } else {
                AppButton(stringResource(R.string.scan_report), onClick = onReport, style = AppButtonStyle.TEXT)
            }
        }
    }
}

@Composable
private fun ScanHistoryRow(item: ScanHistoryUi) {
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.small),
    ) {
        AsyncImage(
            model = item.imageFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
        )
        Column {
            Text(item.title ?: stringResource(R.string.scan_not_recognized))
            Text(formatter.format(Date(item.createdAtEpochMillis)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val PERCENT = 100
