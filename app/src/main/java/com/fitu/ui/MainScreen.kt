package com.fitu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitu.navigation.NavGraph
import com.fitu.navigation.Screen
import com.fitu.ui.theme.OrangePrimary

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Filled.Home)
    object Steps : BottomNavItem(Screen.Steps.route, "Steps", Icons.Filled.DirectionsRun)
    object Coach : BottomNavItem(Screen.Coach.route, "AI Coach", Icons.Filled.Person)
    object Nutrition : BottomNavItem(Screen.Nutrition.route, "Nutrition", Icons.Filled.Restaurant)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.AccountCircle)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState(initial = null)
    val navController = rememberNavController()
    
    // Updated navigation order: Dashboard | Steps | AI Coach | Nutrition | Profile
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Steps,
        BottomNavItem.Coach,
        BottomNavItem.Nutrition,
        BottomNavItem.Profile
    )

    // Wait for onboarding state to load
    if (isOnboardingComplete == null) return

    // Request Camera Permission on start
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val permission = android.Manifest.permission.CAMERA
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }

    val startDestination = if (isOnboardingComplete == true) Screen.Dashboard.route else Screen.Onboarding.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = screen.icon, 
                                    contentDescription = screen.label
                                ) 
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangePrimary,
                                selectedTextColor = OrangePrimary,
                                indicatorColor = OrangePrimary.copy(alpha = 0.1f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController, startDestination = startDestination)
        }
    }
}
