package com.fitu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = androidx.compose.ui.graphics.Color(0xFF0A0A0F)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Content
            Box(modifier = Modifier.padding(bottom = if (showBottomBar) 0.dp else 0.dp)) {
                NavGraph(navController = navController, startDestination = startDestination)
            }

            // Floating Bottom Navigation
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    com.fitu.ui.components.GlassCard(
                        modifier = Modifier.height(80.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                val isCoach = screen == BottomNavItem.Coach

                                if (isCoach) {
                                    // Center Coach Button
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .offset(y = (-24).dp) // Float up
                                            .size(56.dp)
                                            .background(
                                                if (selected) androidx.compose.ui.graphics.Color.White else OrangePrimary,
                                                androidx.compose.foundation.shape.CircleShape
                                            )
                                            .border(4.dp, androidx.compose.ui.graphics.Color(0xFF0A0A0F), androidx.compose.foundation.shape.CircleShape)
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            tint = if (selected) OrangePrimary else androidx.compose.ui.graphics.Color.White
                                        )
                                    }
                                } else {
                                    // Standard Icon
                                    androidx.compose.foundation.layout.Column(
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .background(
                                                    if (selected) OrangePrimary else androidx.compose.ui.graphics.Color.Transparent,
                                                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                tint = if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        if (selected) {
                                            Text(
                                                text = screen.label,
                                                fontSize = 10.sp,
                                                color = androidx.compose.ui.graphics.Color.White,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
