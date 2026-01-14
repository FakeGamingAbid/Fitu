package com.fitu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.OrangePrimary
import com.fitu.util.AutoStartManager
import com.fitu.util.BatteryOptimizationHelper

private val WarningOrange = Color(0xFFFF9800)
private val SuccessGreen = Color(0xFF4CAF50)

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

    // Permission states
    var isBatteryOptimized by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var showAutoStartWarning by remember {
        mutableStateOf(AutoStartManager.shouldShowAutoStartWarning(context))
    }
    val manufacturerName = AutoStartManager.getManufacturerName()

    // Refresh permission state when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                showAutoStartWarning = AutoStartManager.shouldShowAutoStartWarning(context)
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
        animationSpec = tween(1000),
        label = "progress"
    )
    val goalProgress = if (dailyGoal > 0) (currentSteps.toFloat() / dailyGoal * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // --- Header ---
        Text(
            text = "Steps",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Track your daily activity",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Permission Warning Card ---
        if (isBatteryOptimized || showAutoStartWarning) {
            PermissionWarningCard(
                isBatteryOptimized = isBatteryOptimized,
                needsAutoStart = showAutoStartWarning,
                manufacturerName = manufacturerName,
                onBatteryClick = {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                },
                onAutoStartClick = {
                    AutoStartManager.openAutoStartSettings(context)
                    showAutoStartWarning = false
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Main Step Ring ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = OrangePrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .size(48.dp)
                    .background(Color(0xFF1A1A1F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    SneakerIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%,d", currentSteps),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "STEPS TAKEN",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Tracking Status ---
        TrackingStatusSection(
            isServiceRunning = isServiceRunning,
            usesHardwareCounter = usesHardwareCounter,
            onStartTracking = { viewModel.startService() },
            onStopTracking = { viewModel.stopService() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Stats Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(FootprintsIcon, null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formattedDistance, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(distanceUnit, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$caloriesBurned", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("KCAL", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Today's Goal ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TODAY'S GOAL", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%,d", currentSteps), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(" / ${String.format("%,d", dailyGoal)}", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                }
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = OrangePrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("${goalProgress}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Weekly Activity ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Weekly Activity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("STEPS", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (isWeeklyDataLoading) {
                WeeklyChartLoading()
            } else {
                WeeklyChartContent(weeklySteps = weeklySteps, currentSteps = currentSteps)
            }
        }
    }
}

// --- Permission Warning Card with Device-Specific Text ---
@Composable
private fun PermissionWarningCard(
    isBatteryOptimized: Boolean,
    needsAutoStart: Boolean,
    manufacturerName: String,
    onBatteryClick: () -> Unit,
    onAutoStartClick: () -> Unit
) {
    // Device-specific helper texts for battery
    val batteryHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> "📋 Find Fitu → Battery saver → Select 'No restrictions'"
        AutoStartManager.Manufacturer.SAMSUNG -> "📋 Find Fitu → Battery → Select 'Unrestricted'"
        AutoStartManager.Manufacturer.HUAWEI -> "📋 Find Fitu → Disable 'Power-intensive prompt'"
        AutoStartManager.Manufacturer.OPPO -> "📋 Find Fitu → Enable 'Allow background activity'"
        AutoStartManager.Manufacturer.VIVO -> "📋 Find Fitu → Enable 'High background power consumption'"
        AutoStartManager.Manufacturer.ONEPLUS -> "📋 Find Fitu → Battery → Select 'Don't optimize'"
        else -> "📋 Set Fitu to 'Unrestricted' in battery settings"
    }

    // Device-specific helper texts for auto-start
    val autoStartHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> "📋 Find 'Fitu' and turn ON the autostart toggle"
        AutoStartManager.Manufacturer.HUAWEI -> "📋 Find 'Fitu' → Manage manually → Turn on all toggles"
        AutoStartManager.Manufacturer.OPPO -> "📋 Find 'Fitu' → Enable 'Allow Auto-startup'"
        AutoStartManager.Manufacturer.VIVO -> "📋 Find 'Fitu' → Enable autostart permission"
        AutoStartManager.Manufacturer.SAMSUNG -> "📋 Find 'Fitu' → Allow background activity"
        AutoStartManager.Manufacturer.ONEPLUS -> "📋 Find 'Fitu' → Enable 'Allow auto-launch'"
        else -> "📋 Find 'Fitu' in the list and enable autostart"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Background permissions needed",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Step counting may stop when screen is off",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Battery Optimization Section
            if (isBatteryOptimized) {
                Text(
                    text = batteryHelperText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBatteryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(WarningOrange)
                    )
                ) {
                    Icon(Icons.Default.BatteryAlert, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Battery Restrictions")
                }

                if (needsAutoStart) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Auto-Start Section
            if (needsAutoStart) {
                Text(
                    text = autoStartHelperText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onAutoStartClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(WarningOrange)
                    )
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Auto-Start ($manufacturerName)")
                }
            }
        }
    }
}

// --- Tracking Status Section ---
@Composable
private fun TrackingStatusSection(
    isServiceRunning: Boolean,
    usesHardwareCounter: Boolean,
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(SuccessGreen, CircleShape)
                    )
                    Column {
                        Text(
                            "Tracking Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (usesHardwareCounter) "Using hardware sensor" else "Using accelerometer",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(
                    onClick = onStopTracking,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Tracking",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        Button(
            onClick = onStartTracking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Start Tracking",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- Weekly Chart Loading ---
@Composable
private fun WeeklyChartLoading() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((20 + (it * 10)).dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(it, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = OrangePrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Loading weekly data...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

// --- Weekly Chart Content ---
@Composable
private fun WeeklyChartContent(
    weeklySteps: List<com.fitu.ui.steps.DaySteps>,
    currentSteps: Int
) {
    val maxSteps = remember(weeklySteps, currentSteps) {
        weeklySteps.maxOfOrNull {
            if (it.isToday) currentSteps else it.steps
        }?.coerceAtLeast(1) ?: 1
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklySteps.forEach { dayData ->
                val steps = if (dayData.isToday) currentSteps else dayData.steps
                val barHeight = if (maxSteps > 0) (steps.toFloat() / maxSteps * 80).dp else 4.dp
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight.coerceAtLeast(4.dp))
                        .background(
                            if (steps > 0) {
                                if (dayData.isToday) OrangePrimary else OrangePrimary.copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklySteps.forEach { dayData ->
                Text(
                    text = dayData.day,
                    color = if (dayData.isToday) OrangePrimary else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// --- Sneaker Icon ---
private val SneakerIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sneaker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 256f,
        viewportHeight = 256f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(231.16f, 166.63f)
            lineTo(202.53f, 152.32f)
            arcTo(47.74f, 47.74f, 0f, false, true, 176f, 109.39f)
            verticalLineTo(80f)
            arcTo(8f, 8f, 0f, false, false, 168f, 72f)
            arcTo(48.05f, 48.05f, 0f, false, true, 120f, 24f)
            arcTo(8f, 8f, 0f, false, false, 107.17f, 17.63f)
            lineTo(30.13f, 76f)
            lineTo(29.93f, 76.16f)
            arcTo(16f, 16f, 0f, false, false, 28.69f, 99.91f)
            lineTo(142.4f, 213.66f)
            arcTo(8f, 8f, 0f, false, false, 148.06f, 216f)
            horizontalLineTo(224f)
            arcTo(16f, 16f, 0f, false, false, 240f, 200f)
            verticalLineTo(180.94f)
            arcTo(15.92f, 15.92f, 0f, false, false, 231.16f, 166.63f)
            close()
        }
    }.build()
