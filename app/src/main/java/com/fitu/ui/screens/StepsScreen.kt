package com.fitu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fitu.ui.components.AnimatedDecimalCounter
import com.fitu.ui.components.AnimatedFormattedCounter
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.components.AccentGlassCard
import com.fitu.ui.steps.DaySteps
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.AppColors
import com.fitu.util.AutoStartManager
import com.fitu.util.BatteryOptimizationHelper

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentSteps by viewModel.stepCount.collectAsState()
    val dailyGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val usesHardwareCounter by viewModel.usesHardwareCounter.collectAsState()
    val isWeeklyDataLoading by viewModel.isWeeklyDataLoading.collectAsState()
    val formattedDistance by viewModel.formattedDistance.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()

    var isBatteryOptimized by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var showAutoStartWarning by remember {
        mutableStateOf(AutoStartManager.shouldShowAutoStartWarning(context))
    }
    val manufacturerName = AutoStartManager.getManufacturerName()

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted) {
            viewModel.startService()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                showAutoStartWarning = AutoStartManager.shouldShowAutoStartWarning(context)
                hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val progress = if (dailyGoal > 0) currentSteps.toFloat() / dailyGoal.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "progress"
    )
    val goalProgress = if (dailyGoal > 0) (currentSteps.toFloat() / dailyGoal * 100).toInt() else 0

    // Animation state
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.BackgroundDark,
                        AppColors.BackgroundElevated
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // ==================== HEADER ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { -20 }
        ) {
            Column {
                Text(
                    text = "Steps",
                    color = AppColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track your daily activity",
                    color = AppColors.TextTertiary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==================== PERMISSION WARNINGS ====================
        AnimatedVisibility(
            visible = showContent && (!hasActivityPermission || isBatteryOptimized || showAutoStartWarning),
            enter = fadeIn(tween(300, 100)) + slideInVertically(tween(400, 100)) { 20 }
        ) {
            Column {
                PermissionWarningCard(
                    hasActivityPermission = hasActivityPermission,
                    isBatteryOptimized = isBatteryOptimized,
                    needsAutoStart = showAutoStartWarning,
                    manufacturerName = manufacturerName,
                    onActivityPermissionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    },
                    onBatteryClick = {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                    },
                    onAutoStartClick = {
                        AutoStartManager.openAutoStartSettings(context)
                        showAutoStartWarning = false
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ==================== MAIN PROGRESS RING (FIXED - Same as Dashboard) ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 150)) + slideInVertically(tween(400, 150)) { 20 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.OrangePrimary.copy(alpha = 0.2f * animatedProgress),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2
                            )
                        )
                    }

                    // Background track
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Progress arc with gradient
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    AppColors.OrangePrimary,
                                    AppColors.OrangeSecondary,
                                    AppColors.OrangeLight,
                                    AppColors.OrangePrimary
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Center content
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedFormattedCounter(
                            count = currentSteps,
                            style = TextStyle(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "steps",
                            color = AppColors.TextTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress percentage
                        Text(
                            text = "${goalProgress}%",
                            color = AppColors.OrangePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== TRACKING STATUS ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 200)) + slideInVertically(tween(400, 200)) { 20 }
        ) {
            TrackingStatusSection(
                isServiceRunning = isServiceRunning,
                usesHardwareCounter = usesHardwareCounter,
                hasActivityPermission = hasActivityPermission,
                onStartTracking = {
                    if (hasActivityPermission) {
                        viewModel.startService()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        viewModel.startService()
                    }
                },
                onStopTracking = { viewModel.stopService() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==================== STATS CARDS ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 250)) + slideInVertically(tween(400, 250)) { 20 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance Card
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    AppColors.iconBackground(AppColors.Info),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                FootprintsIcon,
                                contentDescription = null,
                                tint = AppColors.Info,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedDecimalCounter(
                            value = formattedDistance,
                            style = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = distanceUnit.uppercase(),
                            color = AppColors.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Calories Card
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    AppColors.iconBackground(AppColors.CaloriesColor),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AppColors.CaloriesColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedFormattedCounter(
                            count = caloriesBurned,
                            style = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "KCAL",
                            color = AppColors.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==================== TODAY'S GOAL CARD ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 300)) + slideInVertically(tween(400, 300)) { 20 }
        ) {
            AccentGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = if (goalProgress >= 100) AppColors.Success else AppColors.OrangePrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎯", fontSize = 16.sp)
                            Text(
                                text = "TODAY'S GOAL",
                                color = AppColors.TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedFormattedCounter(
                                count = currentSteps,
                                style = TextStyle(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = " / ${String.format("%,d", dailyGoal)}",
                                color = AppColors.TextHint,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Mini progress ring
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = if (goalProgress >= 100) {
                                        listOf(AppColors.Success, AppColors.SuccessLight, AppColors.Success)
                                    } else {
                                        listOf(AppColors.OrangePrimary, AppColors.OrangeLight, AppColors.OrangePrimary)
                                    }
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        if (goalProgress >= 100) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = AppColors.Success,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "${goalProgress}%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==================== WEEKLY ACTIVITY ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 350)) + slideInVertically(tween(400, 350)) { 20 }
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊", fontSize = 16.sp)
                        Text(
                            text = "Weekly Activity",
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                AppColors.tint(AppColors.OrangePrimary),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STEPS",
                            color = AppColors.OrangePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    if (isWeeklyDataLoading) {
                        WeeklyChartLoading()
                    } else {
                        WeeklyChartContent(weeklySteps, currentSteps, dailyGoal)
                    }
                }
            }
        }
    }
}

// ==================== PERMISSION WARNING CARD ====================
@Composable
private fun PermissionWarningCard(
    hasActivityPermission: Boolean,
    isBatteryOptimized: Boolean,
    needsAutoStart: Boolean,
    manufacturerName: String,
    onActivityPermissionClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onAutoStartClick: () -> Unit
) {
    val batteryHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> "Find Fitu → Battery saver → Select 'No restrictions'"
        AutoStartManager.Manufacturer.SAMSUNG -> "Find Fitu → Battery → Select 'Unrestricted'"
        AutoStartManager.Manufacturer.HUAWEI -> "Find Fitu → Disable 'Power-intensive prompt'"
        AutoStartManager.Manufacturer.OPPO -> "Find Fitu → Enable 'Allow background activity'"
        AutoStartManager.Manufacturer.VIVO -> "Find Fitu → Enable 'High background power consumption'"
        AutoStartManager.Manufacturer.ONEPLUS -> "Find Fitu → Battery → Select 'Don't optimize'"
        else -> "Set Fitu to 'Unrestricted' in battery settings"
    }

    val autoStartHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> "Find 'Fitu' and turn ON the autostart toggle"
        AutoStartManager.Manufacturer.HUAWEI -> "Find 'Fitu' → Manage manually → Turn on all toggles"
        AutoStartManager.Manufacturer.OPPO -> "Find 'Fitu' → Enable 'Allow Auto-startup'"
        AutoStartManager.Manufacturer.VIVO -> "Find 'Fitu' → Enable autostart permission"
        AutoStartManager.Manufacturer.SAMSUNG -> "Find 'Fitu' → Allow background activity"
        AutoStartManager.Manufacturer.ONEPLUS -> "Find 'Fitu' → Enable 'Allow auto-launch'"
        else -> "Find 'Fitu' in the list and enable autostart"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Warning.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            AppColors.iconBackground(AppColors.Warning),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Permissions Needed",
                        color = AppColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable for accurate step tracking",
                        color = AppColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PermissionButton(
                    icon = Icons.Default.DirectionsWalk,
                    title = "Activity Tracking",
                    description = "Allow access to physical activity data",
                    buttonText = "Enable",
                    color = AppColors.Info,
                    onClick = onActivityPermissionClick
                )

                if (isBatteryOptimized || needsAutoStart) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = AppColors.DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (isBatteryOptimized) {
                PermissionButton(
                    icon = Icons.Default.BatteryAlert,
                    title = "Battery Optimization",
                    description = batteryHelperText,
                    buttonText = "Disable Restrictions",
                    color = AppColors.Warning,
                    onClick = onBatteryClick
                )

                if (needsAutoStart) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = AppColors.DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (needsAutoStart) {
                PermissionButton(
                    icon = Icons.Default.Settings,
                    title = "Auto-Start ($manufacturerName)",
                    description = autoStartHelperText,
                    buttonText = "Enable",
                    color = AppColors.Warning,
                    onClick = onAutoStartClick
                )
            }
        }
    }
}

@Composable
private fun PermissionButton(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    color: Color,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = description,
            color = AppColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(color.copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==================== TRACKING STATUS ====================
@Composable
private fun TrackingStatusSection(
    isServiceRunning: Boolean,
    usesHardwareCounter: Boolean,
    hasActivityPermission: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    if (isServiceRunning) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Animated pulse indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(AppColors.Success, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Tracking Active",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (usesHardwareCounter) Icons.Rounded.Check else Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = AppColors.TextHint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (usesHardwareCounter) "Hardware sensor" else "Accelerometer",
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onStopTracking,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            AppColors.iconBackground(AppColors.Error),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Tracking",
                        tint = AppColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    } else {
        Button(
            onClick = onStartTracking,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.OrangePrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (hasActivityPermission) "Start Tracking" else "Enable & Start",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== WEEKLY CHART ====================
@Composable
private fun WeeklyChartLoading() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) { index ->
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((30 + (index * 12)).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = AppColors.OrangePrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Loading weekly data...",
                color = AppColors.TextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WeeklyChartContent(
    weeklySteps: List<DaySteps>,
    currentSteps: Int,
    dailyGoal: Int
) {
    val maxSteps = remember(weeklySteps, currentSteps) {
        weeklySteps.maxOfOrNull { daySteps: DaySteps ->
            if (daySteps.isToday) currentSteps else daySteps.steps
        }?.coerceAtLeast(1) ?: 1
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklySteps.forEach { dayData: DaySteps ->
                val steps = if (dayData.isToday) currentSteps else dayData.steps
                val barHeight = if (maxSteps > 0) (steps.toFloat() / maxSteps * 100).dp else 6.dp
                val reachedGoal = steps >= dailyGoal

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step count on top of bar
                    if (steps > 0) {
                        Text(
                            text = if (steps >= 1000) "${steps / 1000}k" else "$steps",
                            color = if (dayData.isToday) AppColors.OrangePrimary else AppColors.TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barHeight.coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                brush = when {
                                    steps == 0 -> SolidColor(Color.White.copy(alpha = 0.08f))
                                    reachedGoal -> Brush.verticalGradient(
                                        colors = listOf(AppColors.Success, AppColors.SuccessLight)
                                    )
                                    dayData.isToday -> Brush.verticalGradient(
                                        colors = listOf(AppColors.OrangePrimary, AppColors.OrangeLight)
                                    )
                                    else -> Brush.verticalGradient(
                                        colors = listOf(
                                            AppColors.OrangePrimary.copy(alpha = 0.7f),
                                            AppColors.OrangeLight.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklySteps.forEach { dayData: DaySteps ->
                Text(
                    text = dayData.day,
                    color = if (dayData.isToday) AppColors.OrangePrimary else AppColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}
