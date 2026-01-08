package com.fitu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.dashboard.DashboardViewModel
import com.fitu.ui.theme.OrangePrimary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToSteps: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {}
) {
    val userName by viewModel.userName.collectAsState()
    val todayDate = viewModel.todayDate
    val currentSteps by viewModel.currentSteps.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val workoutsCompleted by viewModel.workoutsCompleted.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()
    val dailyRecap by viewModel.dailyRecap.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateDailyRecap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Greeting Header
        GreetingSection(userName = userName, date = todayDate)

        Spacer(modifier = Modifier.height(24.dp))

        // Steps Summary Card (Clickable)
        SummaryCard(
            title = "Steps",
            value = "$currentSteps",
            subtitle = "of $dailyStepGoal goal",
            progress = currentSteps.toFloat() / dailyStepGoal.toFloat(),
            icon = Icons.Filled.DirectionsRun,
            gradientColors = listOf(OrangePrimary, Color(0xFFFF6B35)),
            onClick = onNavigateToSteps
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Workout and Nutrition Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workout Summary Card
            SmallSummaryCard(
                modifier = Modifier.weight(1f),
                title = "Workouts",
                value = "$workoutsCompleted",
                subtitle = "completed",
                icon = Icons.Filled.FitnessCenter,
                color = Color(0xFF7C4DFF),
                onClick = {}
            )

            // Nutrition Summary Card
            SmallSummaryCard(
                modifier = Modifier.weight(1f),
                title = "Nutrition",
                value = "$caloriesConsumed",
                subtitle = "kcal consumed",
                icon = Icons.Filled.Restaurant,
                color = Color(0xFF4CAF50),
                onClick = onNavigateToNutrition
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Progress Chart
        WeeklyProgressSection(weeklySteps = weeklySteps, stepGoal = dailyStepGoal)

        Spacer(modifier = Modifier.height(24.dp))

        // AI Daily Recap
        DailyRecapCard(recap = dailyRecap)
    }
}

@Composable
fun GreetingSection(userName: String, date: String) {
    Column {
        Text(
            text = "Hello, ${userName.ifBlank { "there" }}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(gradientColors),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Steps Icon",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Circular Progress
                CircularProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    size = 70.dp,
                    strokeWidth = 8.dp,
                    backgroundColor = Color.White.copy(alpha = 0.3f),
                    progressColor = Color.White
                )
            }
        }
    }
}

@Composable
fun SmallSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Nutrition Icon",
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun WeeklyProgressSection(weeklySteps: List<Int>, stepGoal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            WeeklyBarChart(
                data = weeklySteps,
                maxValue = stepGoal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    data: List<Int>,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    val barColor = OrangePrimary
    val backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val spacing = barWidth
        val maxHeight = size.height

        data.forEachIndexed { index, value ->
            val x = spacing / 2 + (barWidth + spacing) * index
            val barHeight = (value.toFloat() / maxValue.toFloat() * maxHeight).coerceAtMost(maxHeight)

            // Background bar
            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, maxHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // Progress bar
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, maxHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
        }
    }
}

@Composable
fun DailyRecapCard(recap: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✨ AI Daily Recap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OrangePrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recap,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CircularProgressIndicator(
    progress: Float,
    size: Dp,
    strokeWidth: Dp,
    backgroundColor: Color,
    progressColor: Color
) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            // Background circle
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = progressColor
        )
    }
}
