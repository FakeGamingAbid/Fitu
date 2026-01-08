package com.fitu.ui.screens

import android.Manifest
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.coach.CoachViewModel
import com.fitu.ui.coach.PoseAnalyzer
import com.fitu.ui.coach.WorkoutState
import com.fitu.ui.theme.OrangePrimary
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun CoachScreen(
    viewModel: CoachViewModel = hiltViewModel()
) {
    val repCount by viewModel.repCount.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()
    val formFeedback by viewModel.formFeedback.collectAsState()
    val incorrectLandmarks by viewModel.incorrectLandmarks.collectAsState()
    val workoutState by viewModel.workoutState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CoachCameraPreview(
            viewModel = viewModel,
            onPoseDetected = { viewModel.updatePose(it) },
            onExerciseUpdate = { viewModel.updateExerciseResult(it) }
        )

        // Skeleton Overlay
        currentPose?.let { pose ->
            PoseOverlay(
                pose = pose,
                incorrectLandmarks = incorrectLandmarks
            )
        }

        // UI Overlay
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                TopControlBar(
                    onFullscreenToggle = { viewModel.toggleFullscreen() },
                    isFullscreen = isFullscreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise Selector
                if (workoutState == WorkoutState.IDLE) {
                    ExerciseSelector(
                        exercises = viewModel.availableExercises,
                        selectedExercise = currentExercise,
                        onSelect = { viewModel.selectExercise(it) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Form Feedback
                AnimatedVisibility(
                    visible = workoutState == WorkoutState.ACTIVE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FormFeedbackCard(
                        isCorrect = formFeedback.isCorrect,
                        message = formFeedback.message
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats and Controls
                StatsAndControls(
                    repCount = repCount,
                    elapsedSeconds = elapsedSeconds,
                    formattedTime = viewModel.formatTime(elapsedSeconds),
                    currentExercise = currentExercise,
                    workoutState = workoutState,
                    onStart = { viewModel.startWorkout() },
                    onPause = { viewModel.pauseWorkout() },
                    onResume = { viewModel.resumeWorkout() },
                    onStop = { viewModel.stopWorkout() },
                    onReset = { viewModel.resetWorkout() }
                )
            }
        } else {
            // Fullscreen Mode - Minimal UI
            FullscreenOverlay(
                repCount = repCount,
                formattedTime = viewModel.formatTime(elapsedSeconds),
                currentExercise = currentExercise,
                formFeedback = formFeedback.message,
                isFormCorrect = formFeedback.isCorrect,
                workoutState = workoutState,
                onExitFullscreen = { viewModel.toggleFullscreen() },
                onPause = { viewModel.pauseWorkout() },
                onResume = { viewModel.resumeWorkout() },
                onStop = { viewModel.stopWorkout() }
            )
        }
    }
}

@Composable
fun TopControlBar(
    onFullscreenToggle: () -> Unit,
    isFullscreen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AI Coach",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        IconButton(
            onClick = onFullscreenToggle,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Toggle Fullscreen",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ExerciseSelector(
    exercises: List<String>,
    selectedExercise: String,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Exercise",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises) { exercise ->
                    ExerciseChip(
                        exercise = exercise,
                        isSelected = exercise == selectedExercise,
                        onClick = { onSelect(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseChip(
    exercise: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) OrangePrimary else Color.White.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = exercise,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun FormFeedbackCard(isCorrect: Boolean, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.8f) 
                            else Color(0xFFF44336).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCorrect) "✓" else "⚠",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun StatsAndControls(
    repCount: Int,
    elapsedSeconds: Int,
    formattedTime: String,
    currentExercise: String,
    workoutState: WorkoutState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exercise Name
            Text(
                text = currentExercise,
                style = MaterialTheme.typography.titleLarge,
                color = OrangePrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatDisplay(value = "$repCount", label = "Reps")
                StatDisplay(value = formattedTime, label = "Time")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (workoutState) {
                    WorkoutState.IDLE -> {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Workout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Workout", fontWeight = FontWeight.Bold)
                        }
                    }
                    WorkoutState.ACTIVE -> {
                        FilledTonalButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    WorkoutState.PAUSED -> {
                        Button(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    WorkoutState.COMPLETED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Workout Complete! 🎉",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onReset) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Workout")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatDisplay(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FullscreenOverlay(
    repCount: Int,
    formattedTime: String,
    currentExercise: String,
    formFeedback: String,
    isFormCorrect: Boolean,
    workoutState: WorkoutState,
    onExitFullscreen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Exit fullscreen button
        IconButton(
            onClick = onExitFullscreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit Fullscreen", tint = Color.White)
        }

        // Rep counter - large and centered
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$repCount",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "REPS",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Form feedback at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isFormCorrect) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = formFeedback, color = Color.White, fontWeight = FontWeight.Medium)
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = formattedTime, color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Pause/Resume button
            if (workoutState == WorkoutState.ACTIVE) {
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                }
            } else if (workoutState == WorkoutState.PAUSED) {
                IconButton(
                    onClick = onResume,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White)
                }
            }

            // Stop button
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
            }
        }
    }
}

@Composable
fun CoachCameraPreview(
    viewModel: CoachViewModel,
    onPoseDetected: (Pose) -> Unit,
    onExerciseUpdate: (com.fitu.ui.coach.ExerciseResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentExercise by viewModel.currentExercise.collectAsState()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val poseAnalyzer = PoseAnalyzer(
                    onPoseDetected = onPoseDetected,
                    onExerciseUpdate = onExerciseUpdate,
                    selectedExercise = currentExercise
                )
                viewModel.setAnalyzer(poseAnalyzer)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx), poseAnalyzer)
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CoachScreen", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
fun PoseOverlay(
    pose: Pose,
    incorrectLandmarks: Set<Int>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return@Canvas

        val scaleX = size.width / 480f
        val scaleY = size.height / 640f

        // Draw skeleton connections
        val connections = listOf(
            // Torso
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
            // Left arm
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
            Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            // Right arm
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
            Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
            // Left leg
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
            Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            // Right leg
            Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
            Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        )

        connections.forEach { (startType, endType) ->
            val startLandmark = pose.getPoseLandmark(startType)
            val endLandmark = pose.getPoseLandmark(endType)

            if (startLandmark != null && endLandmark != null &&
                startLandmark.inFrameLikelihood > 0.5f && endLandmark.inFrameLikelihood > 0.5f) {

                val startX = startLandmark.position.x * scaleX
                val startY = startLandmark.position.y * scaleY
                val endX = endLandmark.position.x * scaleX
                val endY = endLandmark.position.y * scaleY

                // Check if this connection involves incorrect landmarks
                val isIncorrect = incorrectLandmarks.contains(startType) || incorrectLandmarks.contains(endType)
                val lineColor = if (isIncorrect) Color.Red else Color(0xFF00FF00)

                drawLine(
                    color = lineColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isIncorrect) 12f else 8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw joint points
        landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                val x = landmark.position.x * scaleX
                val y = landmark.position.y * scaleY
                val isIncorrect = incorrectLandmarks.contains(landmark.landmarkType)
                val pointColor = if (isIncorrect) Color.Red else OrangePrimary

                drawCircle(
                    color = pointColor,
                    radius = if (isIncorrect) 14f else 10f,
                    center = Offset(x, y)
                )
                // White inner circle
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
