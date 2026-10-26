package com.viettel.appbase.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.viettel.appbase.domain.model.AuthState
import com.viettel.appbase.feature.auth.login.LoginScreen
import com.viettel.appbase.feature.auth.register.RegisterScreen
import com.viettel.appbase.feature.home.HomeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    authState: AuthState,
    sessionReady: Boolean,
    modifier: Modifier = Modifier,
) {
    // Account gate: anonymous users on an account-only screen are sent to Login.
    LaunchedEffect(authState) {
        val destination = navController.currentDestination ?: return@LaunchedEffect
        if (authState != AuthState.SignedIn && destination.requiresAccount()) navController.navigate(LoginRoute)
    }

    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        // Invisible routing step (the system splash covers it while the session starts).
        composable<SplashRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinkHandler.HOME_URI }),
        ) {
            LaunchedEffect(sessionReady) {
                if (!sessionReady) return@LaunchedEffect
                navController.navigate(HomeRoute) {
                    popUpTo<SplashRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = { navController.backToHome() },
                onRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterSuccess = { navController.backToHome() },
                onBack = navController::popBackStack,
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                hasAccount = authState == AuthState.SignedIn,
                onLoginClick = { navController.navigate(LoginRoute) },
            )
        }
    }
}

/** Login/Register are opened on top of Home; return to it (or open it if it is not in the stack). */
private fun NavHostController.backToHome() {
    if (!popBackStack<HomeRoute>(inclusive = false)) navigate(HomeRoute) { popUpTo(graph.id) { inclusive = true } }
}
