package com.viettel.appbase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.navigation.AppNavHost
import com.viettel.appbase.ui.theme.AppTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot(appDataStore: AppDataStore = koinInject()) {
    val themeMode by appDataStore.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    AppTheme(darkTheme = darkTheme) {
        AppNavHost(rememberNavController())
    }
}
