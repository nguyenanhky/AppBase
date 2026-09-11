package com.viettel.appbase.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class AppButtonStyle { PRIMARY, OUTLINED, TEXT }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: AppButtonStyle = AppButtonStyle.PRIMARY,
) {
    when (style) {
        AppButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
        ) { Text(text) }
        AppButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
        ) { Text(text) }
        AppButtonStyle.TEXT -> TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Text(text)
        }
    }
}
