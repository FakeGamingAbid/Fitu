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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.data.local.entity.MealEntity
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
    val calorieProgress by viewModel.calorieProgress.collectAsState()
    val proteinConsumed by viewModel.proteinConsumed.collectAsState()
    val carbsConsumed by viewModel.carbsConsumed.collectAsState()
    val fatsConsumed by viewModel.fatsConsumed.collectAsState()
    val todayMeals by viewModel.todayMeals.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsState()
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val portion by viewModel.portion.collectAsState()
    val textSearch by viewModel.textSearch.collectAsState()

    val mealTypes = listOf("breakfast", "lunch", "dinner", "snacks")
    val selectedTabIndex = mealTypes.indexOf(selectedMealType).coerceAtLeast(0)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showAddFoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddFood() },
            sheetState = sheetState
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            // Header
            Text(
                text = "Nutrition",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Calorie Progress Ring
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CalorieProgressRing(
                    progress = calorieProgress,
                    caloriesConsumed = caloriesConsumed,
                    calorieGoal = dailyCalorieGoal,
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Macro Breakdown
            MacroBreakdownRow(
                protein = proteinConsumed,
                carbs = carbsConsumed,
                fats = fatsConsumed
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Meal Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                mealTypes.forEachIndexed { index, mealType ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.selectMealType(mealType) },
                        text = {
                            Text(
                                text = mealType.replaceFirstChar { it.uppercase() },
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meals List
            val filteredMeals = todayMeals.filter { it.mealType == selectedMealType }

            if (filteredMeals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No ${selectedMealType} logged",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add food",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMeals) { meal ->
                        MealItemCard(meal = meal, onDelete = { viewModel.deleteMeal(meal.id) })
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAddFood() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = OrangePrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Food",
                tint = Color.White
            )
        }
    }
}

@Composable
fun CalorieProgressRing(
    progress: Float,
    caloriesConsumed: Int,
    calorieGoal: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "calorie_progress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Background
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress
            drawArc(
                color = OrangePrimary,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$caloriesConsumed",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "of $calorieGoal kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun MacroBreakdownRow(protein: Int, carbs: Int, fats: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroItem(value = protein, label = "Protein", color = Color(0xFF4CAF50), unit = "g")
        MacroItem(value = carbs, label = "Carbs", color = Color(0xFF2196F3), unit = "g")
        MacroItem(value = fats, label = "Fats", color = Color(0xFFFF9800), unit = "g")
    }
}

@Composable
fun MacroItem(value: Int, label: String, color: Color, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$label ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun MealItemCard(meal: MealEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${meal.calories} kcal • P:${meal.protein}g C:${meal.carbs}g F:${meal.fats}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
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
    val context = LocalContext.current
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
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add Food to ${selectedMealType.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showCamera && hasCameraPermission) {
            // Camera View
            CameraCapture(
                onImageCaptured = { bitmap ->
                    viewModel.analyzeFood(bitmap)
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        } else if (analyzedFood != null) {
            // Show analyzed food result
            AnalyzedFoodResult(
                food = analyzedFood,
                portion = portion,
                onPortionChange = { viewModel.updatePortion(it) },
                onAddToMeal = { viewModel.addFoodToMeal() },
                onScanAnother = { viewModel.reset() }
            )
        } else {
            // Input options
            Column {
                // Camera button
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            showCamera = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Food Photo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan with Camera")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Or divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Text("  or  ", color = Color.Gray)
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Text search
                OutlinedTextField(
                    value = textSearch,
                    onValueChange = { viewModel.updateTextSearch(it) },
                    label = { Text("Search food") },
                    placeholder = { Text("e.g., grilled chicken breast") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.searchFood(textSearch) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )

                if (uiState is NutritionUiState.Analyzing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (uiState is NutritionUiState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (uiState as NutritionUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientChip("${(food.calories * portion).toInt()} kcal", OrangePrimary)
                NutrientChip("P: ${(food.protein * portion).toInt()}g", Color(0xFF4CAF50))
                NutrientChip("C: ${(food.carbs * portion).toInt()}g", Color(0xFF2196F3))
                NutrientChip("F: ${(food.fats * portion).toInt()}g", Color(0xFFFF9800))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portion slider
            Text("Portion: ${DecimalFormat("#.#").format(portion)}x")
            Slider(
                value = portion,
                onValueChange = onPortionChange,
                valueRange = 0.25f..3f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddToMeal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add to Meal")
                }
                Button(
                    onClick = onScanAnother,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan Another")
                }
            }
        }
    }
}

@Composable
fun NutrientChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
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

    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
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
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
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
