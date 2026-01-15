package com.fitu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.aicoach.AiCoachViewModel
import com.fitu.aicoach.ExerciseType
import com.fitu.aicoach.PoseAnalyzer
import com.fitu.aicoach.PoseOverlayView
import com.fitu.ui.components.GlassCard
import com.fitu.ui.theme.OrangePrimary
import java.util.concurrent.Executors

private val BackgroundDark = Color(0xFF0A0A0F)

@Composable
fun CoachScreen(
    viewModel: AiCoachViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val holdTimeMs by viewModel.holdTimeMs.collectAsState()
    val bestHoldTimeMs by viewModel.bestHoldTimeMs.collectAsState()
    val formScore by viewModel.formScore.collectAsState()
    val currentAngle by viewModel.currentAngle.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()

    // Check initial permission state
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermission(hasPermission)
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setCameraPermission(granted)
    }

    // Pose analyzer reference (to update exercise and reset)
    var poseAnalyzer by remember { mutableStateOf<PoseAnalyzer?>(null) }

    // Update analyzer when exercise changes
    LaunchedEffect(selectedExercise) {
        poseAnalyzer?.setExercise(selectedExercise)
        poseAnalyzer?.reset()
        viewModel.resetStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Coach",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time form correction",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            
            // Reset button
            if (isWorkoutActive) {
                IconButton(
                    onClick = {
                        poseAnalyzer?.reset()
                        viewModel.resetStats()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = OrangePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exercise selector
        ExerciseSelector(
            selectedExercise = selectedExercise,
            onExerciseSelected = { viewModel.selectExercise(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (!hasCameraPermission) {
                // Permission request UI
                PermissionRequestCard(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                // Camera preview with pose overlay
                CameraPreviewWithOverlay(
                    selectedExercise = selectedExercise,
                    onAnalyzerCreated = { analyzer ->
                        poseAnalyzer = analyzer
                    },
                    onStatsUpdate = { reps, holdMs, bestMs, score, angle ->
                        viewModel.updateRepCount(reps)
                        viewModel.updateHoldTime(holdMs, bestMs)
                        viewModel.updateFormScore(score)
                        viewModel.updateAngle(angle)
                    }
                )

                // Stats overlay at bottom
                StatsOverlay(
                    exerciseType = selectedExercise,
                    repCount = repCount,
                    holdTimeMs = holdTimeMs,
                    bestHoldTimeMs = bestHoldTimeMs,
                    formScore = formScore,
                    currentAngle = currentAngle,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Exercise selector chips
 */
@Composable
private fun ExerciseSelector(
    selectedExercise: ExerciseType,
    onExerciseSelected: (ExerciseType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ExerciseType.entries) { exercise ->
            val isSelected = exercise == selectedExercise
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) OrangePrimary else Color.White.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) OrangePrimary else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onExerciseSelected(exercise) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exercise.emoji,
                        fontSize = 16.sp
                    )
                    Text(
                        text = exercise.displayName,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Permission request card
 */
@Composable
private fun PermissionRequestCard(
    onRequestPermission: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Allow camera access to use real-time\npose detection for exercise tracking",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Grant Permission",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Camera preview with pose overlay
 */
@Composable
private fun CameraPreviewWithOverlay(
    selectedExercise: ExerciseType,
    onAnalyzerCreated: (PoseAnalyzer) -> Unit,
    onStatsUpdate: (Int, Long, Long, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var overlayView by remember { mutableStateOf<PoseOverlayView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview use case
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Create pose analyzer
                    val overlay = PoseOverlayView(ctx)
                    overlayView = overlay

                    val analyzer = PoseAnalyzer(
                        overlay = overlay,
                        onPoseDetected = { pose, angle, config ->
                            // Get stats from analyzer
                            // Stats are updated via the overlay callbacks
                        }
                    ).also {
                        it.setExercise(selectedExercise)
                        it.setFrontCamera(true)
                        onAnalyzerCreated(it)
                    }

                    // Image analysis use case
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                        }

                    // Select front camera
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CoachScreen", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Pose overlay on top of camera
        overlayView?.let { overlay ->
            AndroidView(
                factory = { overlay },
                modifier = Modifier.fillMaxSize(),
                update = { /* Overlay updates itself via updatePose() */ }
            )
        }
    }
}

/**
 * Stats overlay showing reps/time and form score
 */
@Composable
private fun StatsOverlay(
    exerciseType: ExerciseType,
    repCount: Int,
    holdTimeMs: Long,
    bestHoldTimeMs: Long,
    formScore: Float,
    currentAngle: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (exerciseType.isTimeBased) {
                // Time-based stats (Plank)
                StatItem(
                    label = "HOLD TIME",
                    value = formatTime(holdTimeMs),
                    highlight = true
                )
                StatItem(
                    label = "BEST",
                    value = formatTime(bestHoldTimeMs),
                    highlight = false
                )
                StatItem(
                    label = "FORM",
                    value = String.format("%.1f", formScore),
                    highlight = formScore >= 7f
                )
            } else {
                // Rep-based stats
                StatItem(
                    label = "REPS",
                    value = repCount.toString(),
                    highlight = true
                )
                StatItem(
                    label = "ANGLE",
                    value = if (currentAngle > 0) "${currentAngle.toInt()}°" else "--",
                    highlight = false
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    highlight: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = if (highlight) OrangePrimary else Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Format milliseconds to MM:SS
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
