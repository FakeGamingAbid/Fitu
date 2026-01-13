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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.OrangePrimary

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel()
) {
    val currentSteps by viewModel.stepCount.collectAsState()
    val dailyGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()
    
    // ✅ FIX #11: Observe loading state
    val isWeeklyDataLoading by viewModel.isWeeklyDataLoading.collectAsState()

    // ✅ FIX #24: Unit conversion
    val formattedDistance by viewModel.formattedDistance.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()

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

        Spacer(modifier = Modifier.height(32.dp))

        // --- Main Step Ring ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ring background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Ring progress
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = OrangePrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Sneaker icon at top
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
            // Center content
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

        // --- Start Tracking Button ---
        Button(
            onClick = { viewModel.startService() },
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
                text = "Start Tracking",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Stats Row (Distance and KCAL) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✅ FIX #24: Distance Card with unit conversion
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        FootprintsIcon,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedDistance,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = distanceUnit,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
            // KCAL Card
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$caloriesBurned",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KCAL",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
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
                        Text(
                            text = "TODAY'S GOAL",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%,d", currentSteps),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${String.format("%,d", dailyGoal)}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 16.sp
                        )
                    }
                }
                // Goal Progress Ring
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
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
                    Text(
                        text = "${goalProgress}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                Text(
                    text = "Weekly Activity",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "STEPS",
                color = OrangePrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ FIX #11: Weekly Bar Chart with Loading State
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (isWeeklyDataLoading) {
                // Show loading shimmer/skeleton
                WeeklyChartLoading()
            } else {
                // Show actual chart
                WeeklyChartContent(
                    weeklySteps = weeklySteps,
                    currentSteps = currentSteps
                )
            }
        }
    }
}

/**
 * ✅ FIX #11: Loading skeleton for weekly chart
 */
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
                        .background(
                            Color.White.copy(alpha = 0.1f),
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
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Loading indicator
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
                text = "Loading weekly data...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * ✅ FIX #11: Actual weekly chart content
 */
@Composable
private fun WeeklyChartContent(
    weeklySteps: List<com.fitu.ui.steps.DaySteps>,
    currentSteps: Int
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val maxSteps = weeklySteps.maxOfOrNull { 
                if (it.isToday) currentSteps else it.steps 
            }?.coerceAtLeast(1) ?: 1
            
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

// Sneaker Icon
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
