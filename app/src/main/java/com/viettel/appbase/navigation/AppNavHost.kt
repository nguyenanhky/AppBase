package com.viettel.appbase.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.viettel.appbase.feature.auth.login.LoginScreen
import com.viettel.appbase.feature.auth.register.RegisterScreen
import com.viettel.appbase.feature.home.HomeScreen
import com.viettel.appbase.feature.splash.SplashDestination
import com.viettel.appbase.feature.splash.SplashScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        composable<SplashRoute> {
            SplashScreen(onDestination = { destination ->
                val route = if (destination == SplashDestination.HOME) HomeRoute else LoginRoute
                navController.navigate(route) { popUpTo<SplashRoute> { inclusive = true } }
            })
        }
        composable<LoginRoute> {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(HomeRoute) { popUpTo<LoginRoute> { inclusive = true } }
                },
                onRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onAuthenticated = {
                    navController.navigate(HomeRoute) { popUpTo<LoginRoute> { inclusive = true } }
                },
                onBack = navController::popBackStack,
            )
        }
        composable<HomeRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinkHandler.HOME_URI }),
        ) {
            HomeScreen(
                onLoggedOut = {
                    navController.navigate(LoginRoute) { popUpTo<HomeRoute> { inclusive = true } }
                },
            )
        }
    }
}
