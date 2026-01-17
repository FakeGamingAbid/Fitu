package com.fitu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.AppleIcon
import com.fitu.ui.components.DumbbellIcon
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.components.GoalCelebrationDialog
import com.fitu.ui.components.GoalType
import com.fitu.ui.components.StreakBadge
import com.fitu.ui.components.StreakCounterCard
import com.fitu.ui.dashboard.DashboardViewModel
import com.fitu.ui.theme.OrangePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun getTimeBasedGreeting(): Pair<String, String> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning" to "Rise and shine! Ready to crush your goals today?"
        in 12..16 -> "Good afternoon" to "Keep the momentum going! You're doing great."
        in 17..20 -> "Good evening" to "Great job today! Time to wind down."
        else -> "Good night" to "Rest well! Tomorrow is a new opportunity."
    }
}

private fun getGreetingEmoji(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "☀️"
        in 12..16 -> "🌤️"
        in 17..20 -> "🌅"
        else -> "🌙"
    }
}

private fun getMotivationalMessage(stepProgress: Float, workoutsCompleted: Int): String {
    return when {
        stepProgress >= 1f -> "Amazing! You crushed your step goal! 🎉"
        stepProgress >= 0.75f -> "Almost there! Just a little more to hit your goal! 💪"
        stepProgress >= 0.5f -> "Halfway there! Keep moving! 🚀"
        stepProgress >= 0.25f -> "Great start! Every step counts! 👍"
        workoutsCompleted > 0 -> "Nice workout! Keep the energy up! 🔥"
        else -> "Let's get moving! Your goals are waiting! 🏃"
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToSteps: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {}
) {
    val userName by viewModel.userName.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val workoutsCompleted by viewModel.workoutsCompleted.collectAsState()

    val isStepsInitialized by viewModel.isStepsInitialized.collectAsState()
    val isWeeklyDataLoading by viewModel.isWeeklyDataLoading.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()

    val formattedDistance by viewModel.formattedDistance.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()

    val isBirthday by viewModel.isBirthday.collectAsState()
    val showStepGoalCelebration by viewModel.showStepGoalCelebration.collectAsState()

    val streakData by viewModel.streakData.collectAsState()
    val stepsNeededForStreak by viewModel.stepsNeededForStreak.collectAsState()

    val stepProgress = if (dailyStepGoal > 0) currentSteps.toFloat() / dailyStepGoal else 0f
    val animatedStepProgress by animateFloatAsState(
        targetValue = stepProgress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "stepProgress"
    )
    val stepPercentage = (stepProgress * 100).toInt().coerceIn(0, 100)

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val todayDate = dateFormat.format(Date())

    val (greeting, _) = remember { getTimeBasedGreeting() }
    val greetingEmoji = remember { getGreetingEmoji() }
    val motivationalMessage = remember(stepProgress, workoutsCompleted) {
        getMotivationalMessage(stepProgress, workoutsCompleted)
    }

    // Step Goal Celebration Dialog
    GoalCelebrationDialog(
        show = showStepGoalCelebration,
        goalType = GoalType.STEPS,
        achievedValue = String.format("%,d", currentSteps),
        goalValue = String.format("%,d", dailyStepGoal),
        onDismiss = { viewModel.dismissStepGoalCelebration() }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isBirthday) {
                    Text(
                        text = "Happy Birthday, ${userName.ifBlank { "User" }}! 🎉",
                        color = OrangePrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Have an amazing day! 🎂",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$greeting, ${userName.ifBlank { "User" }}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = greetingEmoji,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todayDate,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (streakData.currentStreak > 0) {
                    StreakBadge(streak = streakData.currentStreak)
                }

                Box(
                    modifier = Modifier
                        .size(if (isBirthday) 52.dp else 48.dp)
                        .background(
                            if (isBirthday) Brush.linearGradient(
                                listOf(OrangePrimary, Color(0xFFFFD700))
                            ) else Brush.linearGradient(
                                listOf(OrangePrimary, OrangePrimary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBirthday) {
                        Text(text = "🎂", fontSize = 24.sp)
                    } else {
                        Text(
                            text = userName.take(1).uppercase().ifEmpty { "U" },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Motivational Message
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            OrangePrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = OrangePrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = motivationalMessage,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streak Counter Card
        StreakCounterCard(
            currentStreak = streakData.currentStreak,
            longestStreak = streakData.longestStreak,
            modifier = Modifier.fillMaxWidth()
        )

        // Steps needed for streak
        if (stepsNeededForStreak > 0 && streakData.currentStreak > 0) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF1A1A1F),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        OrangePrimary.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format("%,d", stepsNeededForStreak)} steps to keep your streak!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Steps Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToSteps
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            FootprintsIcon,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Steps",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (stepProgress >= 1f) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "🏆", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isStepsInitialized) {
                        Text(
                            text = "Loading...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = String.format("%,d", currentSteps),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Goal: ${String.format("%,d", dailyStepGoal)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }

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
                            color = if (stepProgress >= 1f) Color(0xFF4CAF50) else OrangePrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedStepProgress,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = if (stepProgress >= 1f) "✓" else "${stepPercentage}%",
                        color = if (stepProgress >= 1f) Color(0xFF4CAF50) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Distance & Burned Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            FootprintsIcon,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Distance",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedDistance,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distanceUnit,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.weight(1f)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Burned",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$caloriesBurned",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "KCAL",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nutrition Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToNutrition
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AppleIcon,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nutrition",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$caloriesConsumed / $dailyCalorieGoal kcal consumed",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Track",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly Activity Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Activity",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "STEPS",
                        color = OrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isWeeklyDataLoading) {
                    DashboardWeeklyChartLoading()
                } else {
                    DashboardWeeklyChartContent(weeklySteps = weeklySteps)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Workout Coach Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            OrangePrimary.copy(alpha = 0.8f),
                            OrangePrimary.copy(alpha = 0.4f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .border(1.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    DumbbellIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "AI Workout Coach",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time form correction",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardWeeklyChartLoading() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((15 + (it * 6)).dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = OrangePrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Loading...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DashboardWeeklyChartContent(weeklySteps: List<Int>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxSteps = weeklySteps.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklySteps.forEachIndexed { index, steps ->
                val barHeight = if (maxSteps > 0) (steps.toFloat() / maxSteps * 50).dp else 4.dp
                val isToday = index == 6
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight.coerceAtLeast(4.dp))
                        .background(
                            if (steps > 0) {
                                if (isToday) OrangePrimary else OrangePrimary.copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, day ->
                val isToday = index == 6
                Text(
                    text = day,
                    color = if (isToday) OrangePrimary else Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
