package com.viettel.appbase.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.viettel.appbase.ui.theme.LocalAppSpacing

@Composable
fun EmptyView(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(LocalAppSpacing.current.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.small),
    ) {
        Icon(Icons.Default.Info, contentDescription = null)
        Text(message)
    }
}
