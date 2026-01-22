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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.data.local.entity.MealEntity
import com.fitu.ui.components.GlassCard
import com.fitu.ui.components.PullToRefreshContainer
import com.fitu.ui.nutrition.AnalyzedFood
import com.fitu.ui.nutrition.NutritionErrorType
import com.fitu.ui.nutrition.NutritionUiState
import com.fitu.ui.nutrition.NutritionViewModel
import com.fitu.ui.nutrition.PendingFoodAnalysis
import com.fitu.ui.theme.AppColors
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val mealToDelete by viewModel.mealToDelete.collectAsState()
    val showDuplicateWarning by viewModel.showDuplicateWarning.collectAsState()
    val duplicateWarningMessage by viewModel.duplicateWarningMessage.collectAsState()

    // Background analysis state
    val pendingAnalyzedFood by viewModel.pendingAnalyzedFood.collectAsState()
    val showReviewSheet by viewModel.showReviewSheet.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && mealToDelete != null) {
        DeleteMealConfirmDialog(
            meal = mealToDelete!!,
            onConfirm = { viewModel.confirmDeleteMeal() },
            onDismiss = { viewModel.cancelDeleteMeal() }
        )
    }

    // Duplicate food warning dialog
    if (showDuplicateWarning) {
        DuplicateFoodWarningDialog(
            message = duplicateWarningMessage,
            onConfirm = { viewModel.forceAddFoodToMeal() },
            onDismiss = { viewModel.dismissDuplicateWarning() }
        )
    }

    // Add food bottom sheet
    if (showAddFoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddFood() },
            sheetState = sheetState,
            containerColor = AppColors.BackgroundSheet
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

    // Review sheet for background analysis results
    if (showReviewSheet && pendingAnalyzedFood != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeReviewSheet() },
            sheetState = reviewSheetState,
            containerColor = AppColors.BackgroundSheet
        ) {
            ReviewFoodSheetContent(
                pendingFood = pendingAnalyzedFood!!,
                selectedMealType = selectedMealType,
                portion = portion,
                onMealTypeSelected = { viewModel.selectMealType(it) },
                onPortionChange = { viewModel.updatePortion(it) },
                onAddClick = { viewModel.addPendingFoodToMeal() },
                onDismiss = { viewModel.closeReviewSheet() },
                isSuggestedMealType = { viewModel.isSuggestedMealType(it) }
            )
        }
    }

    // Animation state
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshContainer(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.BackgroundDark)
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 120.dp)
            ) {
                // Header
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { -20 }
                ) {
                    Column {
                        Text(
                            text = "Nutrition",
                            color = AppColors.TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Food Analysis",
                            color = AppColors.TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Calorie summary card
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(300, 100)) + slideInVertically(tween(400, 100)) { 20 }
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TODAY'S CALORIES",
                                    color = AppColors.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$caloriesConsumed",
                                        color = AppColors.OrangePrimary,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ $dailyCalorieGoal",
                                        color = AppColors.TextTertiary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }

                            FloatingActionButton(
                                onClick = { viewModel.showAddFood() },
                                containerColor = AppColors.OrangePrimary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Food",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(300, 150)) + slideInVertically(tween(400, 150)) { 20 }
                ) {
                    Text(
                        text = "Today's Meals",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Meals list or empty state
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(300, 200)) + slideInVertically(tween(400, 200)) { 20 }
                ) {
                    if (todayMeals.isEmpty()) {
                        EmptyMealsState(
                            onAddClick = { viewModel.showAddFood() }
                        )
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
        }
    }
}

// ==================== ENHANCED EMPTY STATE ====================

@Composable
private fun EmptyMealsState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated food illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = AppColors.OrangePrimary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Single camera icon for clean look
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = AppColors.OrangePrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No meals tracked yet",
            color = AppColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start tracking your nutrition by snapping\na photo of your food or searching manually",
            color = AppColors.TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera button
            OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.OrangePrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(AppColors.OrangePrimary.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Snap Photo")
            }

            // Search button
            OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.TextSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(AppColors.BorderLight)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tips section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.SurfaceDark,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💡 Tips for accurate tracking",
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            TipRow(emoji = "📸", text = "Take clear photos of your food")
            TipRow(emoji = "🍽️", text = "Include the whole plate in the frame")
            TipRow(emoji = "✨", text = "AI will estimate calories & macros")
        }
    }
}

@Composable
private fun TipRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 14.sp
        )
        Text(
            text = text,
            color = AppColors.TextTertiary,
            fontSize = 12.sp
        )
    }
}

// ==================== REVIEW FOOD SHEET ====================

@Composable
private fun ReviewFoodSheetContent(
    pendingFood: PendingFoodAnalysis,
    selectedMealType: String,
    portion: Float,
    onMealTypeSelected: (String) -> Unit,
    onPortionChange: (Float) -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit,
    isSuggestedMealType: (String) -> Boolean
) {
    val food = pendingFood.food

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add to Meal",
                color = AppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = AppColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Meal type selector
        MealTypeSelector(
            selectedMealType = selectedMealType,
            onMealTypeSelected = onMealTypeSelected,
            isSuggestedMealType = isSuggestedMealType,
            getMealTypeTimeRange = { "" }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Food preview card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Photo or icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.SurfaceLight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pendingFood.bitmap != null) {
                            Image(
                                bitmap = pendingFood.bitmap.asImageBitmap(),
                                contentDescription = "Food photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            MealTypeIcon(mealType = selectedMealType)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = food.name,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analyzed by AI ✨",
                            color = AppColors.OrangePrimary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${(food.calories * portion).toInt()} kcal",
                        color = AppColors.OrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "P: ${(food.protein * portion).toInt()}g",
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "C: ${(food.carbs * portion).toInt()}g",
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "F: ${(food.fats * portion).toInt()}g",
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Portion: ${String.format("%.1f", portion)}x",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )

                Slider(
                    value = portion,
                    onValueChange = onPortionChange,
                    valueRange = 0.5f..3f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.OrangePrimary,
                        activeTrackColor = AppColors.OrangePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Add button
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to ${selectedMealType.replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==================== DIALOGS ====================

@Composable
private fun DuplicateFoodWarningDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.Warning.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Duplicate Food?",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary),
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
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun DeleteMealConfirmDialog(
    meal: MealEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = AppColors.Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Meal?",
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceDark, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodThumbnail(
                            photoUri = meal.photoUri,
                            mealType = meal.mealType,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = meal.name,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${meal.calories} kcal • P: ${meal.protein}g • C: ${meal.carbs}g • F: ${meal.fat}g",
                                color = AppColors.TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This action cannot be undone.",
                    color = AppColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}

// ==================== MEAL CARD ====================

@Composable
private fun MealCard(meal: MealEntity, onDeleteClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumbnail(
                photoUri = meal.photoUri,
                mealType = meal.mealType,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${meal.calories} kcal • ${meal.mealType.replaceFirstChar { it.uppercase() }}",
                    color = AppColors.TextTertiary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MacroPill("P: ${meal.protein}g", AppColors.ProteinColor)
                MacroPill("C: ${meal.carbs}g", AppColors.CarbsColor)
                MacroPill("F: ${meal.fat}g", AppColors.FatsColor)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete meal",
                    tint = AppColors.Error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FoodThumbnail(
    photoUri: String?,
    mealType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceLight),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            val bitmap = remember(photoUri) {
                try {
                    val file = File(photoUri)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Food photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                MealTypeIcon(mealType = mealType)
            }
        } else {
            MealTypeIcon(mealType = mealType)
        }
    }
}

@Composable
private fun MealTypeIcon(mealType: String) {
    val (icon, tint) = when (mealType.lowercase()) {
        "breakfast" -> Icons.Default.FreeBreakfast to AppColors.BreakfastColor
        "lunch" -> Icons.Default.LunchDining to AppColors.LunchColor
        "dinner" -> Icons.Default.DinnerDining to AppColors.DinnerColor
        "snacks" -> Icons.Default.Cookie to AppColors.SnacksColor
        else -> Icons.Default.Restaurant to AppColors.TextTertiary
    }

    Icon(
        imageVector = icon,
        contentDescription = mealType,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(AppColors.chipBackground(color), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// ==================== ADD FOOD SHEET ====================

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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) showCamera = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Add Food",
                color = AppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = AppColors.TextPrimary)
            }
        }

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
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            tint = AppColors.OrangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snap a Photo", color = AppColors.TextPrimary, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            null,
                            tint = AppColors.OrangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gallery", color = AppColors.TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text search input
            OutlinedTextField(
                value = textSearch,
                onValueChange = { viewModel.updateTextSearch(it) },
                label = { Text("Or describe your food...", color = AppColors.TextHint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.OrangePrimary,
                    unfocusedBorderColor = AppColors.BorderLight
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (textSearch.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.searchFood(textSearch) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary)
                ) {
                    Text("Analyze", color = Color.White)
                }
            }

            // Show loading, error, or analyzed food
            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is NutritionUiState.Analyzing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppColors.OrangePrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing food...",
                                color = AppColors.TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is NutritionUiState.Error -> {
                    ErrorCard(
                        error = uiState,
                        onRetry = { viewModel.retry() }
                    )
                }

                else -> {
                    if (analyzedFood != null) {
                        AnalyzedFoodCard(
                            food = analyzedFood,
                            portion = portion,
                            photoBitmap = null,
                            selectedMealType = selectedMealType,
                            onPortionChange = { viewModel.updatePortion(it) },
                            onAddClick = { viewModel.addFoodToMeal() }
                        )
                    }
                }
            }
        } else {
            CameraPreviewSection(
                onImageCaptured = { bitmap ->
                    viewModel.analyzeFood(bitmap)
                },
                onError = { Log.e("NutritionScreen", "Camera error", it) },
                onCancel = { showCamera = false }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==================== ANALYZED FOOD CARD ====================

@Composable
private fun AnalyzedFoodCard(
    food: AnalyzedFood,
    portion: Float,
    photoBitmap: Bitmap?,
    selectedMealType: String,
    onPortionChange: (Float) -> Unit,
    onAddClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Food photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        MealTypeIcon(mealType = selectedMealType)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detected by AI",
                        color = AppColors.OrangePrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${(food.calories * portion).toInt()} kcal",
                    color = AppColors.OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "P: ${(food.protein * portion).toInt()}g",
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "C: ${(food.carbs * portion).toInt()}g",
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "F: ${(food.fats * portion).toInt()}g",
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Portion: ${String.format("%.1f", portion)}x",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )

            Slider(
                value = portion,
                onValueChange = onPortionChange,
                valueRange = 0.5f..3f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.OrangePrimary,
                    activeTrackColor = AppColors.OrangePrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary)
            ) {
                Text(
                    text = "Add to ${selectedMealType.replaceFirstChar { it.uppercase() }}",
                    color = Color.White
                )
            }
        }
    }
}

// ==================== MEAL TYPE SELECTOR ====================

@Composable
private fun MealTypeSelector(
    selectedMealType: String,
    onMealTypeSelected: (String) -> Unit,
    isSuggestedMealType: (String) -> Boolean,
    getMealTypeTimeRange: (String) -> String
) {
    val mealTypes = listOf("breakfast", "lunch", "dinner", "snacks")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meal Type",
                color = AppColors.TextSecondary,
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
                    tint = AppColors.Success,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Auto-detected",
                    color = AppColors.Success,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mealTypes.forEach { type ->
                val isSelected = selectedMealType == type
                val isSuggested = isSuggestedMealType(type)

                MealTypeButton(
                    mealType = type,
                    isSelected = isSelected,
                    isSuggested = isSuggested,
                    onClick = { onMealTypeSelected(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MealTypeButton(
    mealType: String,
    isSelected: Boolean,
    isSuggested: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> AppColors.OrangePrimary
        else -> AppColors.SurfaceLight
    }

    val borderColor = when {
        isSelected -> AppColors.OrangePrimary
        isSuggested && !isSelected -> AppColors.Success.copy(alpha = 0.5f)
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

            Text(
                text = mealType.replaceFirstChar { it.uppercase() },
                color = if (isSelected) Color.White else AppColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            if (isSuggested && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Now",
                        color = AppColors.Success,
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

// ==================== ERROR CARD ====================

@Composable
private fun ErrorCard(
    error: NutritionUiState.Error,
    onRetry: () -> Unit
) {
    val (icon, iconColor) = when (error.errorType) {
        NutritionErrorType.NETWORK -> Icons.Default.CloudOff to AppColors.Warning
        NutritionErrorType.API_KEY -> Icons.Default.Key to AppColors.Error
        NutritionErrorType.RATE_LIMIT -> Icons.Default.Refresh to AppColors.Warning
        NutritionErrorType.CONTENT -> Icons.Default.CameraAlt to AppColors.Warning
        NutritionErrorType.SERVICE -> Icons.Default.Error to AppColors.Warning
        NutritionErrorType.UNKNOWN -> Icons.Default.Error to AppColors.Error
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
                    imageVector = icon,
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
                        color = AppColors.TextSecondary,
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

// ==================== CAMERA PREVIEW ====================

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
                containerColor = AppColors.OrangePrimary
            ) {
                Icon(Icons.Default.CameraAlt, "Capture", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = AppColors.TextSecondary)
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }

        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        Log.e("NutritionScreen", "Error converting image proxy to bitmap", e)
        null
    }
}
