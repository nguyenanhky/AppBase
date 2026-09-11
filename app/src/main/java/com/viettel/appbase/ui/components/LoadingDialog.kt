package com.viettel.appbase.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.viettel.appbase.ui.theme.LocalAppSpacing

@Composable
fun LoadingDialog(visible: Boolean) {
    if (!visible) return
    Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.large) {
            Box(Modifier.padding(LocalAppSpacing.current.large)) {
                CircularProgressIndicator()
            }
        }
    }
}
