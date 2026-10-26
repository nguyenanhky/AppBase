package com.viettel.appbase.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute

@Serializable data object LoginRoute

@Serializable data object RegisterRoute

@Serializable data object HomeRoute

/**
 * Screens that need a real account, not an anonymous session (e.g. profile, cross-device purchases).
 * Anonymous users opening them are sent to Login. Add such routes to this list.
 */
private val accountRoutes = listOf<(NavDestination) -> Boolean>(
    // { it.hasRoute<ProfileRoute>() },
)

fun NavDestination.requiresAccount(): Boolean = accountRoutes.any { it(this) } && !hasRoute<LoginRoute>()
