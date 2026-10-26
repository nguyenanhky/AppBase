package com.viettel.appbase.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.viettel.appbase.feature.auth.login.LoginScreen
import com.viettel.appbase.feature.auth.register.RegisterScreen
import com.viettel.appbase.feature.home.HomeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    isAuthenticated: Boolean?,
    modifier: Modifier = Modifier,
) {
    // Auth gate: whenever the session ends (logout, refresh failure), leave protected screens.
    LaunchedEffect(isAuthenticated) {
        val destination = navController.currentDestination ?: return@LaunchedEffect
        if (isAuthenticated == false && destination.requiresAuth()) {
            navController.navigate(LoginRoute) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        // Invisible routing step (the system splash covers it). Deep links enter here
        // so the session is checked before any protected screen opens.
        composable<SplashRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinkHandler.HOME_URI }),
        ) {
            LaunchedEffect(isAuthenticated) {
                val route: Any = when (isAuthenticated) {
                    true -> HomeRoute
                    false -> LoginRoute
                    null -> return@LaunchedEffect
                }
                navController.navigate(route) {
                    popUpTo<SplashRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(HomeRoute) { popUpTo<LoginRoute> { inclusive = true } }
                },
                onRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(HomeRoute) { popUpTo<LoginRoute> { inclusive = true } }
                },
                onBack = navController::popBackStack,
            )
        }
        composable<HomeRoute> {
            HomeScreen()
        }
    }
}
