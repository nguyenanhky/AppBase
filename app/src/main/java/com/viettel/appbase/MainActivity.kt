package com.viettel.appbase

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.viettel.appbase.core.playstore.InAppUpdateHandler
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.navigation.AppNavHost
import com.viettel.appbase.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash stays until theme and session are loaded: no theme flash, no second splash screen.
        installSplashScreen().setKeepOnScreenCondition { viewModel.state.value.isAuthenticated == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(this, viewModel) }
    }
}

@Composable
private fun AppRoot(activity: ComponentActivity, viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    // Deep links arriving while the activity is alive (e.g. tapping a push notification).
    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    AppTheme(darkTheme = darkTheme) {
        AppNavHost(navController, isAuthenticated = state.isAuthenticated)
        InAppUpdateHandler()
    }
}
