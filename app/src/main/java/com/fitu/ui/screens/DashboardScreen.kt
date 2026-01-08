package com.fitu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.GlassCard
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
    val dailyRecap by viewModel.dailyRecap.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateDailyRecap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 100.dp), // Padding for bottom nav
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${userName.split(" ").firstOrNull() ?: "User"}",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayDate,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(OrangePrimary, Color(0xFFD94F00))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.toString() ?: "U",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Main Steps Card ---
        GlassCard(onClick = onNavigateToSteps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DirectionsRun, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Text("Steps", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%,d", currentSteps),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Goal: ${String.format("%,d", dailyStepGoal)}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
                
                // Stat Ring
                Box(contentAlignment = Alignment.Center) {
                    val progress = (currentSteps.toFloat() / dailyStepGoal.toFloat()).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawCircle(color = Color.White.copy(alpha = 0.1f), style = Stroke(width = 8.dp.toPx()))
                        drawArc(
                            color = OrangePrimary,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- Burned Card ---
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                    Text("Burned", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${caloriesBurned.toInt()}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((caloriesBurned.toFloat() / dailyCalorieGoal.toFloat()).coerceIn(0f, 1f))
                            .background(Color(0xFFFF5252), CircleShape)
                    )
                }
            }
        }

        // --- Nutrition Card ---
        GlassCard(onClick = onNavigateToNutrition) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Restaurant, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Text("Nutrition", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(100))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Track", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (caloriesConsumed > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Eaten", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text("${caloriesConsumed.toInt()}", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.1f)))
                        Column {
                            val net = caloriesConsumed - caloriesBurned
                            Text("Net", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text(
                                text = "${if (net > 0) "+" else ""}${net.toInt()}",
                                color = if (net > 0) Color.White else OrangePrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Track your meals with AI vision.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- AI Coach Card ---
        GlassCard(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(OrangePrimary.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    RoundedCornerShape(24.dp)
                )
                .border(1.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI Workout Coach", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Real-time form correction & rep counting", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Navigate to Coach */ },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(100)
                ) {
                    Text("Start Workout", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // --- Daily Recap (if available) ---
        if (dailyRecap.isNotEmpty()) {
             GlassCard {
                 Column {
                     Text("✨ AI Daily Recap", color = OrangePrimary, fontWeight = FontWeight.Bold)
                     Spacer(modifier = Modifier.height(8.dp))
                     Text(dailyRecap, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
                 }
             }
        }
    }
}
