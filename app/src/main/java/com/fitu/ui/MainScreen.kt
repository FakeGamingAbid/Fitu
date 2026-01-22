 package com.fitu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import androidx.compose.ui.zIndex
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
import com.fitu.ui.nutrition.BackgroundAnalysisState
import com.fitu.ui.nutrition.NutritionViewModel
import com.fitu.ui.theme.OrangePrimary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val ErrorRed = Color(0xFFF44336)
private val SuccessGreen = Color(0xFF4CAF50)

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Dashboard.route, "Home", HouseIcon)
    object Steps : BottomNavItem(Screen.Steps.route, "Steps", FootprintsIcon)
    object Coach : BottomNavItem(Screen.Coach.route, "Coach", DumbbellIcon)
    object Food : BottomNavItem(Screen.Nutrition.route, "Food", AppleIcon)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", CircleUserIcon)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    // Get NutritionViewModel at the MainScreen level so it persists across navigation
    nutritionViewModel: NutritionViewModel = hiltViewModel()
) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState(initial = null)
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }

    // Observe background analysis state from NutritionViewModel
    val backgroundAnalysisState by nutritionViewModel.backgroundAnalysisState.collectAsState()
    val showReviewSheet by nutritionViewModel.showReviewSheet.collectAsState()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Steps,
        BottomNavItem.Coach,
        BottomNavItem.Food,
        BottomNavItem.Profile
    )

    if (isOnboardingComplete == null) return

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val startDestination = when {
        isOnboardingComplete == false -> Screen.Onboarding.route
        else -> Screen.Splash.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route != Screen.Onboarding.route &&
            currentDestination?.route != Screen.Splash.route

    Scaffold(
        containerColor = Color(0xFF0A0A0F)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
            ) {
                // Pass the nutritionViewModel to NavGraph so NutritionScreen uses the same instance
                NavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    nutritionViewModel = nutritionViewModel
                )
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
                                shape = RoundedCornerShape(24.dp),
                                tint = Color.Black.copy(alpha = 0.4f),
                                blurRadius = 20.dp,
                                noiseFactor = 0.05f
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
                                val selected =
                                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                val isCoach = screen == BottomNavItem.Coach

                                if (isCoach) {
                                    Spacer(modifier = Modifier.width(56.dp))
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (selected) OrangePrimary.copy(alpha = 0.2f)
                                                    else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                tint = if (selected) OrangePrimary else Color.White.copy(
                                                    alpha = 0.5f
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = screen.label,
                                            fontSize = 10.sp,
                                            color = if (selected) Color.White else Color.White.copy(
                                                alpha = 0.5f
                                            ),
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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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

            // ========== GLOBAL BACKGROUND ANALYSIS SNACKBAR ==========
            // This is now at the MainScreen level, so it persists across navigation!
            GlobalBackgroundAnalysisSnackbar(
                state = backgroundAnalysisState,
                onSuccessClick = {
                    // Navigate to Nutrition screen and open review sheet
                    navController.navigate(Screen.Nutrition.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    nutritionViewModel.openReviewSheet()
                },
                onRetryClick = { nutritionViewModel.retryBackgroundAnalysis() },
                onDismiss = { nutritionViewModel.dismissBackgroundAnalysis() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showBottomBar) 110.dp else 24.dp)
                    .zIndex(Float.MAX_VALUE)
            )
        }
    }
}

// ==================== GLOBAL SNACKBAR COMPONENTS ====================

@Composable
private fun GlobalBackgroundAnalysisSnackbar(
    state: BackgroundAnalysisState,
    onSuccessClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state !is BackgroundAnalysisState.Idle,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        when (state) {
            is BackgroundAnalysisState.Analyzing -> {
                AnalyzingSnackbar(message = state.message)
            }
            is BackgroundAnalysisState.Success -> {
                SuccessSnackbar(
                    message = state.message,
                    onClick = onSuccessClick,
                    onDismiss = onDismiss
                )
            }
            is BackgroundAnalysisState.Error -> {
                ErrorSnackbar(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetryClick = onRetryClick,
                    onDismiss = onDismiss
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun AnalyzingSnackbar(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = OrangePrimary,
                strokeWidth = 2.dp
            )
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SuccessSnackbar(
    message: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SuccessGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    canRetry: Boolean,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (canRetry) Modifier.clickable { onRetryClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(ErrorRed.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (canRetry) Icons.Default.Refresh else Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp
                )
                if (canRetry) {
                    Text(
                        text = "Tap to retry",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
} 
