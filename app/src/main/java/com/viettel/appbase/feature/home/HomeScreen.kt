package com.viettel.appbase.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viettel.appbase.BuildConfig
import com.viettel.appbase.R
import com.viettel.appbase.core.storage.ThemeMode
import com.viettel.appbase.ui.components.AppButton
import com.viettel.appbase.ui.components.LoadingDialog
import com.viettel.appbase.ui.theme.LocalAppSpacing
import org.koin.androidx.compose.koinViewModel

private enum class HomeTab(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    EXPLORE(Icons.Default.Search),
    SETTINGS(Icons.Default.Settings),
}

@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    val stateHolder = rememberSaveableStateHolder()
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    val label = when (tab) {
                        HomeTab.HOME -> stringResource(R.string.tab_home)
                        HomeTab.EXPLORE -> stringResource(R.string.tab_explore)
                        HomeTab.SETTINGS -> stringResource(R.string.tab_settings)
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        stateHolder.SaveableStateProvider(currentTab) {
            when (currentTab) {
                HomeTab.HOME -> DashboardTab(padding)
                HomeTab.EXPLORE -> ExploreTab(padding)
                HomeTab.SETTINGS -> SettingsTab(padding, viewModel)
            }
        }
    }
    LoadingDialog(state.isLoggingOut)
}

@Composable
private fun DashboardTab(padding: PaddingValues) {
    TabContainer(padding) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.home_description))
        Text(
            stringResource(
                if (BuildConfig.FIREBASE_CONFIGURED) R.string.firebase_connected else R.string.firebase_not_connected,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ExploreTab(padding: PaddingValues) {
    TabContainer(padding) {
        Text(stringResource(R.string.explore_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.explore_description))
    }
}

@Composable
private fun SettingsTab(padding: PaddingValues, viewModel: HomeViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    TabContainer(padding) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleLarge)
        ThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
            }
            AppButton(
                text = if (mode == themeMode) stringResource(R.string.selected_format, label) else label,
                onClick = { viewModel.setThemeMode(mode) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppButton(text = stringResource(R.string.enable_notifications), onClick = {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        AppButton(stringResource(R.string.logout_action), onClick = viewModel::logout)
    }
}

@Composable
private fun TabContainer(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(LocalAppSpacing.current.large),
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.medium),
        horizontalAlignment = Alignment.Start,
    ) { content() }
}
