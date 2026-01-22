 package com.fitu.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fitu.ui.nutrition.NutritionViewModel
import com.fitu.ui.onboarding.OnboardingScreen
import com.fitu.ui.screens.CoachScreen
import com.fitu.ui.screens.DashboardScreen
import com.fitu.ui.screens.GeneratorScreen
import com.fitu.ui.screens.NutritionScreen
import com.fitu.ui.screens.ProfileScreen
import com.fitu.ui.screens.StepsScreen
import com.fitu.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Steps : Screen("steps")
    object Nutrition : Screen("nutrition")
    object Coach : Screen("coach")
    object Generator : Screen("generator")
    object Profile : Screen("profile")
}

private const val POPUP_DURATION = 200
private const val FADE_DURATION = 150

private fun popupEnter(): EnterTransition {
    return scaleIn(
        initialScale = 0.85f,
        animationSpec = tween(POPUP_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(FADE_DURATION))
}

private fun popupExit(): ExitTransition {
    return scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(POPUP_DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(FADE_DURATION))
}

private fun fadeInOnly(): EnterTransition {
    return fadeIn(animationSpec = tween(FADE_DURATION))
}

private fun fadeOutOnly(): ExitTransition {
    return fadeOut(animationSpec = tween(FADE_DURATION))
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    // NEW: Accept shared NutritionViewModel from MainScreen
    nutritionViewModel: NutritionViewModel? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { popupEnter() },
        exitTransition = { popupExit() },
        popEnterTransition = { popupEnter() },
        popExitTransition = { popupExit() }
    ) {
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeInOnly() },
            exitTransition = { fadeOutOnly() }
        ) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeInOnly() },
            exitTransition = { fadeOutOnly() }
        ) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            DashboardScreen()
        }

        composable(
            route = Screen.Steps.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            StepsScreen()
        }

        // UPDATED: Pass the shared NutritionViewModel to NutritionScreen
        composable(
            route = Screen.Nutrition.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            // Use the shared viewModel if provided, otherwise let Hilt create one
            if (nutritionViewModel != null) {
                NutritionScreen(viewModel = nutritionViewModel)
            } else {
                NutritionScreen()
            }
        }

        composable(
            route = Screen.Coach.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            CoachScreen()
        }

        composable(
            route = Screen.Generator.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            GeneratorScreen()
        }

        composable(
            route = Screen.Profile.route,
            enterTransition = { popupEnter() },
            exitTransition = { popupExit() },
            popEnterTransition = { popupEnter() },
            popExitTransition = { popupExit() }
        ) {
            ProfileScreen()
        }
    }
} 
