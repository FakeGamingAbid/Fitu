package com.fitu.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

// Animation durations
private const val ANIMATION_DURATION = 400
private const val FADE_DURATION = 300

// Animation specs
private val tweenSpec = tween<Float>(ANIMATION_DURATION, easing = FastOutSlowInEasing)
private val fadeSpec = tween<Float>(FADE_DURATION, easing = FastOutSlowInEasing)
private val intTweenSpec = tween<Int>(ANIMATION_DURATION, easing = FastOutSlowInEasing)

/**
 * Slide in from right
 */
private fun slideInFromRight(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = intTweenSpec
    ) + fadeIn(animationSpec = fadeSpec)
}

/**
 * Slide out to left
 */
private fun slideOutToLeft(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = intTweenSpec
    ) + fadeOut(animationSpec = fadeSpec)
}

/**
 * Slide in from left
 */
private fun slideInFromLeft(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = intTweenSpec
    ) + fadeIn(animationSpec = fadeSpec)
}

/**
 * Slide out to right
 */
private fun slideOutToRight(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = intTweenSpec
    ) + fadeOut(animationSpec = fadeSpec)
}

/**
 * Slide in from bottom (for modals/sheets)
 */
private fun slideInFromBottom(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = intTweenSpec
    ) + fadeIn(animationSpec = fadeSpec)
}

/**
 * Slide out to bottom
 */
private fun slideOutToBottom(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = intTweenSpec
    ) + fadeOut(animationSpec = fadeSpec)
}

/**
 * Scale + fade in (for feature screens)
 */
private fun scaleInWithFade(): EnterTransition {
    return scaleIn(
        initialScale = 0.92f,
        animationSpec = tweenSpec
    ) + fadeIn(animationSpec = fadeSpec)
}

/**
 * Scale + fade out
 */
private fun scaleOutWithFade(): ExitTransition {
    return scaleOut(
        targetScale = 0.92f,
        animationSpec = tweenSpec
    ) + fadeOut(animationSpec = fadeSpec)
}

/**
 * Simple fade transition
 */
private fun fadeInOnly(): EnterTransition {
    return fadeIn(animationSpec = fadeSpec)
}

private fun fadeOutOnly(): ExitTransition {
    return fadeOut(animationSpec = fadeSpec)
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Default transitions
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromLeft() },
        popExitTransition = { slideOutToRight() }
    ) {
        // Splash Screen - Fade only
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

        // Onboarding - Fade transition
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

        // Dashboard - Scale + fade (main hub)
        composable(
            route = Screen.Dashboard.route,
            enterTransition = { scaleInWithFade() },
            exitTransition = { scaleOutWithFade() },
            popEnterTransition = { scaleInWithFade() },
            popExitTransition = { scaleOutWithFade() }
        ) {
            DashboardScreen(
                onNavigateToSteps = {
                    navController.navigate(Screen.Steps.route)
                },
                onNavigateToNutrition = {
                    navController.navigate(Screen.Nutrition.route)
                }
            )
        }

        // Steps Screen - Slide from right
        composable(
            route = Screen.Steps.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            StepsScreen()
        }

        // Nutrition Screen - Slide from right
        composable(
            route = Screen.Nutrition.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            NutritionScreen()
        }

        // Coach Screen - Slide from bottom (feature screen)
        composable(
            route = Screen.Coach.route,
            enterTransition = { slideInFromBottom() },
            exitTransition = { slideOutToBottom() },
            popEnterTransition = { slideInFromBottom() },
            popExitTransition = { slideOutToBottom() }
        ) {
            CoachScreen()
        }

        // Generator Screen - Scale + fade
        composable(
            route = Screen.Generator.route,
            enterTransition = { scaleInWithFade() },
            exitTransition = { scaleOutWithFade() },
            popEnterTransition = { scaleInWithFade() },
            popExitTransition = { scaleOutWithFade() }
        ) {
            GeneratorScreen()
        }

        // Profile Screen - Slide from right
        composable(
            route = Screen.Profile.route,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { slideOutToRight() }
        ) {
            ProfileScreen()
        }
    }
}
