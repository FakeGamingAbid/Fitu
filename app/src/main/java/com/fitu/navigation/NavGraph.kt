package com.fitu.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
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

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSteps = {
                    navController.navigate(Screen.Steps.route)
                },
                onNavigateToNutrition = {
                    navController.navigate(Screen.Nutrition.route)
                }
            )
        }

        composable(Screen.Steps.route) {
            StepsScreen()
        }

        composable(Screen.Nutrition.route) {
            NutritionScreen()
        }

        composable(Screen.Coach.route) {
            CoachScreen()
        }

        composable(Screen.Generator.route) {
            GeneratorScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen()
        }
    }
} 
