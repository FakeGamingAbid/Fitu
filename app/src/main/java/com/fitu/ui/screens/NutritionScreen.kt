package com.fitu.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.data.local.entity.MealEntity
import com.fitu.ui.components.GlassCard
import com.fitu.ui.nutrition.AnalyzedFood
import com.fitu.ui.nutrition.NutritionUiState
import com.fitu.ui.nutrition.NutritionViewModel
import com.fitu.ui.theme.OrangePrimary
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val todayMeals by viewModel.todayMeals.collectAsState()
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsState()
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val portion by viewModel.portion.collectAsState()
    val textSearch by viewModel.textSearch.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()

    val isAnalyzing = uiState is NutritionUiState.Analyzing

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showAddFoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddFood() },
            sheetState = sheetState,
            containerColor = Color(0xFF1C1C24)
        ) {
            AddFoodSheet(
                viewModel = viewModel,
                uiState = uiState,
                analyzedFood = analyzedFood,
                portion = portion,
                textSearch = textSearch,
                selectedMealType = selectedMealType,
                onDismiss = { viewModel.hideAddFood() }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "Nutrition",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "AI Food Analysis",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        // --- Summary Card ---
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S CALORIES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${caloriesConsumed.toInt()}",
                            color = OrangePrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / $dailyCalorieGoal",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }
                }
                
                // Add Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrangePrimary, CircleShape)
                        .clickable { viewModel.showAddFood() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }

        // --- Meals List ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's Meals",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Analyzing State
            AnimatedVisibility(
                visible = isAnalyzing,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(OrangePrimary.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .border(1.dp, OrangePrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary, strokeWidth = 2.dp)
                        Column {
                            Text("Analyzing Food...", color = OrangePrimary, fontWeight = FontWeight.Bold)
                            Text("Identifying macros & calories", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (todayMeals.isEmpty() && !isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No meals tracked yet.", color = Color.White.copy(alpha = 0.3f))
                        Text("Tap + to snap a photo", color = Color.White.copy(alpha = 0.2f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(todayMeals) { meal ->
                        MealItemCard(meal = meal, onDelete = { viewModel.deleteMeal(meal.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun MealItemCard(meal: MealEntity, onDelete: () -> Unit) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Placeholder (or image if available)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = Color.White.copy(alpha = 0.5f))
                }
                
                Column {
                    Text(meal.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "${meal.mealType.replaceFirstChar { it.uppercase() }} • ${java.text.SimpleDateFormat("HH:mm").format(meal.timestamp)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MacroPill("P: ${meal.protein}g", OrangePrimary)
                        MacroPill("C: ${meal.carbs}g", Color(0xFF2196F3))
                        MacroPill("F: ${meal.fats}g", Color(0xFF4CAF50))
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${meal.calories}", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("kcal", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddFoodSheet(
    viewModel: NutritionViewModel,
    uiState: NutritionUiState,
    analyzedFood: AnalyzedFood?,
    portion: Float,
    textSearch: String,
    selectedMealType: String,
    onDismiss: () -> Unit
) {
    var showCamera by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (granted) showCamera = true
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add to ${selectedMealType.replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showCamera && hasCameraPermission) {
            CameraCapture(
                onImageCaptured = { bitmap ->
                    viewModel.analyzeFood(bitmap)
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        } else if (analyzedFood != null) {
            AnalyzedFoodResult(
                food = analyzedFood,
                portion = portion,
                onPortionChange = { viewModel.updatePortion(it) },
                onAddToMeal = { viewModel.addFoodToMeal() },
                onScanAnother = { viewModel.reset() }
            )
        } else {
            // Input Options
            Button(
                onClick = {
                    if (hasCameraPermission) {
                        showCamera = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Scan with Camera", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Text("  OR  ", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = textSearch,
                onValueChange = { viewModel.updateTextSearch(it) },
                placeholder = { Text("Search food (e.g. apple)", color = Color.White.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchFood(textSearch) }) {
                        Icon(Icons.Default.Search, null, tint = OrangePrimary)
                    }
                }
            )
            
            if (uiState is NutritionUiState.Analyzing) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            }
        }
    }
}

@Composable
fun AnalyzedFoodResult(
    food: AnalyzedFood,
    portion: Float,
    onPortionChange: (Float) -> Unit,
    onAddToMeal: () -> Unit,
    onScanAnother: () -> Unit
) {
    Column {
        Text(food.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientChip("${(food.calories * portion).toInt()} kcal", OrangePrimary)
            NutrientChip("P: ${(food.protein * portion).toInt()}g", Color(0xFF4CAF50))
            NutrientChip("C: ${(food.carbs * portion).toInt()}g", Color(0xFF2196F3))
            NutrientChip("F: ${(food.fats * portion).toInt()}g", Color(0xFFFF9800))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Portion: ${DecimalFormat("#.#").format(portion)}x", color = Color.White.copy(alpha = 0.7f))
        Slider(
            value = portion,
            onValueChange = onPortionChange,
            valueRange = 0.25f..3f,
            steps = 10,
            colors = SliderDefaults.colors(thumbColor = OrangePrimary, activeTrackColor = OrangePrimary)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onScanAnother,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Retake", color = Color.White)
            }
            Button(
                onClick = onAddToMeal,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Food", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NutrientChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(100))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CameraCapture(
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(24.dp))) {
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

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraCapture", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Capture button
        IconButton(
            onClick = {
                takePhoto(
                    imageCapture = imageCapture,
                    executor = ContextCompat.getMainExecutor(context),
                    onImageCaptured = onImageCaptured,
                    onError = { Log.e("CameraCapture", "Error", it) }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(64.dp)
                .background(OrangePrimary, CircleShape)
                .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }

            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                onImageCaptured(bitmap)
                image.close()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
