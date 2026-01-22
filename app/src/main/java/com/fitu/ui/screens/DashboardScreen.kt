package com.fitu.ui.screens

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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.AccentGlassCard
import com.fitu.ui.components.AnimatedDecimalCounter
import com.fitu.ui.components.AnimatedFormattedCounter
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.components.GoalCelebrationDialog
import com.fitu.ui.components.GoalType
import com.fitu.ui.components.PullToRefreshContainer
import com.fitu.ui.components.StreakCounterCard
import com.fitu.ui.components.skeletons.DashboardSkeleton
import com.fitu.ui.dashboard.DashboardViewModel
import com.fitu.ui.theme.AppColors
import com.fitu.ui.theme.OrangePrimary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val formattedDistance by viewModel.formattedDistance.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val isStepsInitialized by viewModel.isStepsInitialized.collectAsState()
    val streakData by viewModel.streakData.collectAsState()
    val showStepGoalCelebration by viewModel.showStepGoalCelebration.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val progress = if (dailyStepGoal > 0) currentSteps.toFloat() / dailyStepGoal.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1200),
        label = "progress"
    )

    // Animation states for staggered entry
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(isStepsInitialized) {
        if (isStepsInitialized) {
            showContent = true
        }
    }

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    val formattedDate = today.format(dateFormatter)

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val (motivationalMessage, motivationalEmoji) = when {
        currentSteps >= dailyStepGoal -> "Goal achieved! Amazing work!" to "🎉"
        currentSteps >= dailyStepGoal * 0.75 -> "Almost there! Keep pushing!" to "🔥"
        currentSteps >= dailyStepGoal * 0.5 -> "Halfway there! You got this!" to "💪"
        currentSteps >= dailyStepGoal * 0.25 -> "Great start! Keep moving!" to "🚶"
        else -> "Let's get moving today!" to "👟"
    }

    // Goal celebration dialog
    GoalCelebrationDialog(
        show = showStepGoalCelebration,
        goalType = GoalType.STEPS,
        achievedValue = String.format("%,d", currentSteps),
        goalValue = String.format("%,d", dailyStepGoal),
        onDismiss = { viewModel.dismissStepGoalCelebration() }
    )

    if (!isStepsInitialized) {
        DashboardSkeleton()
        return
    }

    PullToRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "$greeting,",
                                color = AppColors.TextTertiary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userName.ifEmpty { "Athlete" },
                                color = AppColors.TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedDate,
                                color = AppColors.TextHint,
                                fontSize = 13.sp
                            )
                        }
                        
                        // Profile Avatar with gradient
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AppColors.OrangePrimary,
                                            AppColors.OrangeSecondary
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase().ifEmpty { "U" },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Motivational message with accent card
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.tint(AppColors.OrangePrimary))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = motivationalEmoji, fontSize = 16.sp)
                        Text(
                            text = motivationalMessage,
                            color = AppColors.OrangePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== STREAK CARD ====================
            AnimatedVisibility(
                visible = showContent && (streakData.currentStreak > 0 || streakData.longestStreak > 0),
                enter = fadeIn(tween(300, 100)) + slideInVertically(tween(400, 100)) { 20 }
            ) {
                Column {
                    StreakCounterCard(
                        currentStreak = streakData.currentStreak,
                        longestStreak = streakData.longestStreak
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ==================== MAIN STEPS CARD ====================
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(300, 150)) + slideInVertically(tween(400, 150)) { 20 }
            ) {
                AccentGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = AppColors.OrangePrimary
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            AppColors.iconBackground(AppColors.OrangePrimary),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        FootprintsIcon,
                                        contentDescription = null,
                                        tint = AppColors.OrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Today's Steps",
                                    color = AppColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Goal badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        AppColors.SurfaceLight,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Goal: ${String.format("%,d", dailyStepGoal)}",
                                    color = AppColors.OrangePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Circular Progress with Glow
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
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    color = AppColors.OrangePrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== STATS ROW ====================
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(300, 200)) + slideInVertically(tween(400, 200)) { 20 }
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
                                    .size(40.dp)
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
                                    modifier = Modifier.size(20.dp)
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
                                text = distanceUnit,
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Calories Burned Card
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    modifier = Modifier.size(22.dp)
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
                                text = "burned",
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== NUTRITION CARD ====================
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(300, 250)) + slideInVertically(tween(400, 250)) { 20 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            AppColors.iconBackground(AppColors.Success),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = AppColors.Success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Nutrition",
                                    color = AppColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        AppColors.SurfaceLight,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Goal: ${String.format("%,d", dailyCalorieGoal)} kcal",
                                    color = AppColors.Success,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Consumed
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedFormattedCounter(
                                    count = caloriesConsumed,
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "consumed",
                                    color = AppColors.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(50.dp)
                                    .background(AppColors.DividerColor)
                            )

                            // Remaining
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val remaining = (dailyCalorieGoal - caloriesConsumed).coerceAtLeast(0)
                                AnimatedFormattedCounter(
                                    count = remaining,
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remaining > 0) Color.White else AppColors.Success
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "remaining",
                                    color = AppColors.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Progress bar with gradient
                        val calorieProgress = if (dailyCalorieGoal > 0) {
                            (caloriesConsumed.toFloat() / dailyCalorieGoal).coerceIn(0f, 1f)
                        } else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(calorieProgress)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                AppColors.Success,
                                                AppColors.SuccessLight
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
