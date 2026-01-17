package com.fitu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitu.navigation.NavGraph
import com.fitu.navigation.Screen
import com.fitu.ui.components.AppleIcon
import com.fitu.ui.components.CircleUserIcon
import com.fitu.ui.components.DumbbellIcon
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.HouseIcon
import com.fitu.ui.theme.OrangePrimary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Dashboard.route, "Home", HouseIcon)
    object Steps : BottomNavItem(Screen.Steps.route, "Steps", FootprintsIcon)
    object Coach : BottomNavItem(Screen.Coach.route, "Coach", DumbbellIcon)
    object Food : BottomNavItem(Screen.Nutrition.route, "Food", AppleIcon)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", CircleUserIcon)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState(initial = null)
    val navController = rememberNavController()
    
    // Haze state for blur effect
    val hazeState = remember { HazeState() }
    
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Steps,
        BottomNavItem.Coach,
        BottomNavItem.Food,
        BottomNavItem.Profile
    )

    if (isOnboardingComplete == null) return

    val context = LocalContext.current
    
    // Permission launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // Request necessary permissions on app start
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        // Activity Recognition permission (required for step counting on Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        
        // Notification permission (required for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val startDestination = if (isOnboardingComplete == true) Screen.Dashboard.route else Screen.Onboarding.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Screen.Onboarding.route

    Scaffold(
        containerColor = Color(0xFF0A0A0F)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Content - Apply haze source here (content that will be blurred behind nav bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
            ) {
                NavGraph(navController = navController, startDestination = startDestination)
            }

            // Floating Bottom Navigation with Blur
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // Blurred glass background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .hazeChild(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = Color(0xFF0A0A0F),
                                    tint = Color.Black.copy(alpha = 0.2f),
                                    blurRadius = 20.dp,
                                    noiseFactor = 0.05f
                                )
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                val isCoach = screen == BottomNavItem.Coach

                                if (isCoach) {
                                    // Empty space for FAB
                                    Spacer(modifier = Modifier.width(56.dp))
                                } else {
                                    // Nav Item
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (selected) OrangePrimary.copy(alpha = 0.2f) else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                tint = if (selected) OrangePrimary else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = screen.label,
                                            fontSize = 10.sp,
                                            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Coach FAB (Center)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-20).dp)
                            .size(64.dp)
                            .background(OrangePrimary, CircleShape)
                            .border(4.dp, Color(0xFF0A0A0F), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                navController.navigate(Screen.Coach.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DumbbellIcon,
                            contentDescription = "AI Coach",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
