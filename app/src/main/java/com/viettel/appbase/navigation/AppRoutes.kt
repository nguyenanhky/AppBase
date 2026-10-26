package com.viettel.appbase.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute

@Serializable data object LoginRoute

@Serializable data object RegisterRoute

@Serializable data object HomeRoute

/** Every route except these requires a signed-in user. Add new public routes here. */
fun NavDestination.requiresAuth(): Boolean =
    !(hasRoute<SplashRoute>() || hasRoute<LoginRoute>() || hasRoute<RegisterRoute>())
