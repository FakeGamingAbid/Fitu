package com.fitu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.GlassCard
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val stepCount by viewModel.stepCount.collectAsState()
    val motionMagnitude by viewModel.motionMagnitude.collectAsState()
    val dailyGoal by viewModel.dailyStepGoal.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()

    var hasPermission by remember {
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.startService()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Animation state for step ring
    var isStepAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(stepCount) {
        if (stepCount > 0) {
            isStepAnimating = true
            delay(250)
            isStepAnimating = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isStepAnimating) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)) // Dark background
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
                    text = "Steps",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt, // Using Bolt as Activity icon proxy
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "PRECISION ENGINE V2",
                        color = OrangePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            // Active Indicator
            if (hasPermission) {
                Box(
                    modifier = Modifier
                        .background(OrangePrimary.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, OrangePrimary.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OrangePrimary, CircleShape)
                        )
                        Text(
                            text = "ACTIVE",
                            color = OrangePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // --- Main Counter Ring ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .scale(scale)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(10.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { /* No-op, just for feel */ },
                contentAlignment = Alignment.Center
            ) {
                // Animated Ring (Ping effect simulated by border color change for now)
                if (isStepAnimating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, OrangePrimary.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = SneakerIcon,
                        contentDescription = null,
                        tint = if (isStepAnimating) OrangePrimary else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (isStepAnimating) 1.1f else 1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stepCount.toString(),
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp,
                        lineHeight = 72.sp
                    )
                    Text(
                        text = "STEPS TAKEN",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // --- Motion Stability ---
        GlassCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Bolt, null, tint = OrangePrimary, modifier = Modifier.size(12.dp))
                        Text("MOTION STABILITY", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = String.format("%.2f m/s²", motionMagnitude),
                        color = OrangePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .clip(CircleShape)
                ) {
                    val progress = (motionMagnitude / 6f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(if (motionMagnitude > 2.4f) Color.Red else OrangePrimary)
                    )
                }
            }
        }

        // --- Controls ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { /* Toggle tracking logic if needed, currently service runs always */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(elevation = 8.dp, spotColor = OrangePrimary),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White) // Or Pause
                    Text("Tracking Active", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // --- Metrics Row ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Rounded.Straighten, null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp)) // Purple-ish
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(String.format("%.2f", distanceKm), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("KM", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp)) // Red
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(caloriesBurned.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("KCAL", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // --- Today's Goal ---
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                        Text("TODAY'S GOAL", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stepCount.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text(" / $dailyGoal", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp, start = 8.dp))
                    }
                }
                
                // Circular Progress
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        drawCircle(color = Color.White.copy(alpha = 0.05f), style = Stroke(width = 6.dp.toPx()))
                        drawArc(
                            color = OrangePrimary,
                            startAngle = -90f,
                            sweepAngle = (stepCount.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "${((stepCount.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// Custom Sneaker Icon Vector
val SneakerIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sneaker",
        defaultWidth = 256.dp,
        defaultHeight = 256.dp,
        viewportWidth = 256f,
        viewportHeight = 256f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // Converted path from the React component
            moveTo(231.16f, 166.63f)
            lineTo(202.53f, 152.32f)
            arcTo(47.74f, 47.74f, 0f, 0f, 1f, 176f, 109.39f)
            verticalLineTo(80f)
            arcTo(8f, 8f, 0f, 0f, 0f, 168f, 72f)
            arcTo(48.05f, 48.05f, 0f, 0f, 1f, 120f, 24f)
            arcTo(8f, 8f, 0f, 0f, 0f, 107.17f, 17.63f)
            lineTo(30.13f, 76f)
            lineTo(29.93f, 76.16f)
            arcTo(16f, 16f, 0f, 0f, 0f, 28.69f, 99.91f)
            lineTo(142.4f, 213.66f)
            arcTo(8f, 8f, 0f, 0f, 0f, 148.06f, 216f)
            horizontalLineTo(224f)
            arcTo(16f, 16f, 0f, 0f, 0f, 240f, 200f)
            verticalLineTo(180.94f)
            arcTo(15.92f, 15.92f, 0f, 0f, 0f, 231.16f, 166.63f)
            close()
            moveTo(224f, 200f)
            horizontalLineTo(151.37f)
            lineTo(40f, 88.63f)
            lineTo(52.87f, 78.87f)
            lineTo(91.66f, 117.66f)
            arcTo(8f, 8f, 0f, 0f, 0f, 103f, 106.34f)
            lineTo(65.74f, 69.11f)
            lineTo(105.74f, 38.8f)
            arcTo(64.15f, 64.15f, 0f, 0f, 0f, 160f, 87.5f)
            verticalLineTo(109.39f)
            arcTo(63.65f, 63.65f, 0f, false, false, 195.38f, 166.63f)
            lineTo(224f, 180.94f)
            close()
            moveTo(70.8f, 184f)
            horizontalLineTo(32f)
            arcTo(8f, 8f, 0f, false, true, 32f, 168f)
            horizontalLineTo(70.8f)
            arcTo(8f, 8f, 0f, true, true, 70.8f, 184f)
            close()
            moveTo(110.8f, 208f)
            arcTo(8f, 8f, 0f, false, true, 102.8f, 216f)
            horizontalLineTo(48f)
            arcTo(8f, 8f, 0f, false, true, 48f, 200f)
            horizontalLineTo(102.8f)
            arcTo(8f, 8f, 0f, false, true, 110.8f, 208f)
            close()
        }
    }.build()
