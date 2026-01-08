package com.fitu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.OrangePrimary
import java.text.DecimalFormat

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    val stepCount by viewModel.stepCount.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Steps",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Large Animated Progress Ring
        AnimatedProgressRing(
            progress = progress,
            stepCount = stepCount,
            stepGoal = dailyStepGoal,
            modifier = Modifier.size(280.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StepStatCard(
                icon = Icons.Filled.Route,
                value = DecimalFormat("#.##").format(distanceKm),
                unit = "km",
                label = "Distance",
                color = Color(0xFF4CAF50)
            )
            StepStatCard(
                icon = Icons.Filled.LocalFireDepartment,
                value = "$caloriesBurned",
                unit = "kcal",
                label = "Burned",
                color = Color(0xFFFF5722)
            )
            StepStatCard(
                icon = Icons.Filled.DirectionsWalk,
                value = "$dailyStepGoal",
                unit = "",
                label = "Goal",
                color = OrangePrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Motivational text
        Text(
            text = when {
                progress >= 1f -> "🎉 Goal reached! Amazing!"
                progress >= 0.75f -> "Almost there! Keep going! 💪"
                progress >= 0.5f -> "Halfway there! You got this!"
                progress >= 0.25f -> "Great start! Keep moving!"
                else -> "Let's get moving! 👟"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    stepCount: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    // Animate the progress value
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(progress) {
        animatedProgress = progress
    }

    val animatedValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    val ringColors = listOf(
        OrangePrimary,
        Color(0xFFFF6B35),
        Color(0xFFFF8F00)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Background ring
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated progress arc with gradient
            drawArc(
                brush = Brush.sweepGradient(ringColors),
                startAngle = -90f,
                sweepAngle = 360f * animatedValue,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$stepCount",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "steps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(animatedValue * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = OrangePrimary
            )
        }
    }
}

@Composable
fun StepStatCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}
