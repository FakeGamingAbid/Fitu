 package com.fitu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fitu.data.local.entity.MealEntity
import com.fitu.ui.components.GlassCard
import com.fitu.ui.nutrition.AnalyzedFood
import com.fitu.ui.nutrition.NutritionErrorType
import com.fitu.ui.nutrition.NutritionUiState
import com.fitu.ui.nutrition.NutritionViewModel
import com.fitu.ui.theme.OrangePrimary
import java.io.InputStream
import java.nio.ByteBuffer

private val ErrorRed = Color(0xFFF44336)
private val WarningOrange = Color(0xFFFF9800)
private val SuccessGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val todayMeals by viewModel.todayMeals.collectAsState()
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val analyzedFood by viewModel.analyzedFood.collectAsState()
    val portion by viewModel.portion.collectAsState()
    val textSearch by viewModel.textSearch.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()

    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val mealToDelete by viewModel.mealToDelete.collectAsState()

    // ✅ FIX #15: Duplicate warning state
    val showDuplicateWarning by viewModel.showDuplicateWarning.collectAsState()
    val duplicateWarningMessage by viewModel.duplicateWarningMessage.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && mealToDelete != null) {
        DeleteMealConfirmDialog(
            meal = mealToDelete!!,
            onConfirm = { viewModel.confirmDeleteMeal() },
            onDismiss = { viewModel.cancelDeleteMeal() }
        )
    }

    // ✅ FIX #15: Duplicate Warning Dialog
    if (showDuplicateWarning) {
        DuplicateFoodWarningDialog(
            message = duplicateWarningMessage,
            onConfirm = { viewModel.forceAddFoodToMeal() },
            onDismiss = { viewModel.dismissDuplicateWarning() }
        )
    }

    if (showAddFoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddFood() },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1F)
        ) {
            AddFoodSheetContent(
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
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        // Header
        Text(
            text = "Nutrition",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI Food Analysis",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Calories Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S CALORIES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$caloriesConsumed",
                            color = OrangePrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ $dailyCalorieGoal",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { viewModel.showAddFood() },
                    containerColor = OrangePrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Meals
        Text(
            text = "Today's Meals",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (todayMeals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No meals tracked yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
                Text(
                    text = "Tap + to add your first meal",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(todayMeals) { meal ->
                    MealCard(
                        meal = meal,
                        onDeleteClick = { viewModel.requestDeleteMeal(meal) }
                    )
                }
            }
        }
    }
}

/**
 * ✅ FIX #15: Duplicate Food Warning Dialog
 */
@Composable
private fun DuplicateFoodWarningDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(WarningOrange.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Duplicate Food?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Yes, Add Again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun MealCard(meal: MealEntity, onDeleteClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, null, tint = Color.White.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "${meal.calories} kcal • ${meal.mealType.replaceFirstChar { it.uppercase() }}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MacroPill("P: ${meal.protein}g", OrangePrimary)
                MacroPill("C: ${meal.carbs}g", Color(0xFF4CAF50))
                MacroPill("F: ${meal.fats}g", Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete meal",
                    tint = ErrorRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeleteMealConfirmDialog(meal: MealEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(ErrorRed.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text("Delete Meal?", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(16.dp)
                ) {
                    Column {
                        Text(meal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${meal.calories} kcal • P: ${meal.protein}g • C: ${meal.carbs}g • F: ${meal.fats}g",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("This action cannot be undone.", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), modifier = Modifier.fillMaxWidth()) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier.background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddFoodSheetContent(
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

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var cameraPermissionGranted by remember { mutableStateOf(hasCameraPermission) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted = granted
        if (granted) showCamera = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                inputStream?.let { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (bitmap != null) viewModel.analyzeFood(bitmap)
                }
            } catch (e: Exception) {
                Log.e("NutritionScreen", "Error loading image from gallery", e)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Food", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ FIX #14: Enhanced meal type selector with time-based suggestion
        MealTypeSelector(
            selectedMealType = selectedMealType,
            onMealTypeSelected = { viewModel.selectMealType(it) },
            isSuggestedMealType = { viewModel.isSuggestedMealType(it) },
            getMealTypeTimeRange = { viewModel.getMealTypeTimeRange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!showCamera) {
            // Camera and Gallery buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (cameraPermissionGranted) showCamera = true
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f).height(120.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snap a Photo", color = Color.White, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(120.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gallery", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text search
            OutlinedTextField(
                value = textSearch,
                onValueChange = { viewModel.updateTextSearch(it) },
                label = { Text("Or describe your food...", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (textSearch.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.searchFood(textSearch) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = uiState !is NutritionUiState.Analyzing
                ) {
                    Text("Analyze", color = Color.White)
                }
            }
        } else {
            // Camera preview
            CameraPreviewSection(
                onImageCaptured = { bitmap ->
                    viewModel.analyzeFood(bitmap)
                    showCamera = false
                },
                onError = { Log.e("NutritionScreen", "Camera error", it) },
                onCancel = { showCamera = false }
            )
        }

        // Show analyzed food
        analyzedFood?.let { food ->
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(food.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${(food.calories * portion).toInt()} kcal", color = OrangePrimary)
                        Text("P: ${(food.protein * portion).toInt()}g", color = Color.White.copy(alpha = 0.7f))
                        Text("C: ${(food.carbs * portion).toInt()}g", color = Color.White.copy(alpha = 0.7f))
                        Text("F: ${(food.fats * portion).toInt()}g", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Portion: ${String.format("%.1f", portion)}x", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Slider(
                        value = portion,
                        onValueChange = { viewModel.updatePortion(it) },
                        valueRange = 0.5f..3f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = OrangePrimary, activeTrackColor = OrangePrimary)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.addFoodToMeal() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Add to ${selectedMealType.replaceFirstChar { it.uppercase() }}", color = Color.White)
                    }
                }
            }
        }

        // Loading state
        if (uiState is NutritionUiState.Analyzing) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing your food...", color = Color.White)
            }
        }

        // Error state with detailed message and retry
        if (uiState is NutritionUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorCard(
                error = uiState as NutritionUiState.Error,
                onRetry = { viewModel.retry() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * ✅ FIX #14: Enhanced Meal Type Selector with time-based suggestions
 */
@Composable
private fun MealTypeSelector(
    selectedMealType: String,
    onMealTypeSelected: (String) -> Unit,
    isSuggestedMealType: (String) -> Boolean,
    getMealTypeTimeRange: (String) -> String
) {
    val mealTypes = listOf("breakfast", "lunch", "dinner", "snacks")
    
    Column {
        // Header with current suggestion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meal Type",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Auto-detected",
                    color = SuccessGreen,
                    fontSize = 11.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Meal type buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mealTypes.forEach { type ->
                val isSelected = selectedMealType == type
                val isSuggested = isSuggestedMealType(type)
                val timeRange = getMealTypeTimeRange(type)
                
                MealTypeButton(
                    mealType = type,
                    isSelected = isSelected,
                    isSuggested = isSuggested,
                    timeRange = timeRange,
                    onClick = { onMealTypeSelected(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * ✅ FIX #14: Individual Meal Type Button
 */
@Composable
private fun MealTypeButton(
    mealType: String,
    isSelected: Boolean,
    isSuggested: Boolean,
    timeRange: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> OrangePrimary
        else -> Color.White.copy(alpha = 0.1f)
    }
    
    val borderColor = when {
        isSelected -> OrangePrimary
        isSuggested && !isSelected -> SuccessGreen.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Meal type icon
            Text(
                text = when (mealType) {
                    "breakfast" -> "🌅"
                    "lunch" -> "☀️"
                    "dinner" -> "🌙"
                    "snacks" -> "🍿"
                    else -> "🍽️"
                },
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Meal type name
            Text(
                text = mealType.replaceFirstChar { it.uppercase() },
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            
            // Show suggested badge or time range
            if (isSuggested && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Now",
                        color = SuccessGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (isSelected && isSuggested) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "✓ Now",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Error card with icon, message, and retry button
 */
@Composable
private fun ErrorCard(
    error: NutritionUiState.Error,
    onRetry: () -> Unit
) {
    val (icon, iconColor) = when (error.errorType) {
        NutritionErrorType.NETWORK -> Icons.Default.CloudOff to WarningOrange
        NutritionErrorType.API_KEY -> Icons.Default.Key to ErrorRed
        NutritionErrorType.RATE_LIMIT -> Icons.Default.Refresh to WarningOrange
        NutritionErrorType.CONTENT -> Icons.Default.CameraAlt to WarningOrange
        NutritionErrorType.SERVICE -> Icons.Default.Error to WarningOrange
        NutritionErrorType.UNKNOWN -> Icons.Default.Error to ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getErrorTitle(error.errorType),
                        color = iconColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error.message,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            if (error.canRetry) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun getErrorTitle(errorType: NutritionErrorType): String {
    return when (errorType) {
        NutritionErrorType.NETWORK -> "Connection Problem"
        NutritionErrorType.API_KEY -> "API Key Issue"
        NutritionErrorType.RATE_LIMIT -> "Too Many Requests"
        NutritionErrorType.CONTENT -> "Image Problem"
        NutritionErrorType.SERVICE -> "Service Unavailable"
        NutritionErrorType.UNKNOWN -> "Something Went Wrong"
    }
}

@Composable
private fun CameraPreviewSection(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor = ContextCompat.getMainExecutor(context)

    // Clean up camera when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w("CameraPreview", "Error unbinding camera", e)
            }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Use case binding failed", e)
                        }
                    }, executor)
                }
            )

            // Capture button
            FloatingActionButton(
                onClick = {
                    imageCapture?.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = imageProxyToBitmap(image)
                                image.close()
                                bitmap?.let { onImageCaptured(it) }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                onError(exception)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .size(64.dp),
                containerColor = OrangePrimary
            ) {
                Icon(Icons.Default.CameraAlt, "Capture", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
} 
