package com.fitu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.GlassCard
import com.fitu.ui.profile.BackupState
import com.fitu.ui.profile.ProfileViewModel
import com.fitu.ui.profile.RestoreState
import com.fitu.ui.theme.AppColors
import com.fitu.ui.theme.OrangePrimary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val userAge by viewModel.userAge.collectAsState()
    val userHeightCm by viewModel.userHeightCm.collectAsState()
    val userWeightKg by viewModel.userWeightKg.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val bmi by viewModel.bmi.collectAsState()
    val bmiCategory by viewModel.bmiCategory.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val apiKeyError by viewModel.apiKeyError.collectAsState()
    val profileValidationErrors by viewModel.profileValidationErrors.collectAsState()

    // Birth date
    val birthDay by viewModel.birthDay.collectAsState()
    val birthMonth by viewModel.birthMonth.collectAsState()
    val birthYear by viewModel.birthYear.collectAsState()
    val formattedBirthDate by viewModel.formattedBirthDate.collectAsState()
    val calculatedAge by viewModel.calculatedAge.collectAsState()
    val showBirthDatePicker by viewModel.showBirthDatePicker.collectAsState()

    // Unit preference
    val useImperialUnits by viewModel.useImperialUnits.collectAsState()
    val formattedHeight by viewModel.formattedHeight.collectAsState()
    val formattedWeight by viewModel.formattedWeight.collectAsState()

    // Backup & Restore
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // Animation state
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    // File picker for restore
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedRestoreUri = it
            showRestoreConfirmDialog = true
        }
    }

    // Share intent for backup
    LaunchedEffect(backupState) {
        if (backupState is BackupState.Success) {
            val uri = (backupState as BackupState.Success).uri
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
        }
    }

    // Date picker
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            if (birthYear != null && birthMonth != null && birthDay != null) {
                val calendar = Calendar.getInstance()
                calendar.set(birthYear!!, birthMonth!! - 1, birthDay!!)
                calendar.timeInMillis
            } else null
        } catch (e: Exception) { null },
        yearRange = 1920..currentYear
    )

    // Dialogs
    if (showBirthDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideBirthDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            viewModel.saveBirthDate(
                                day = calendar.get(Calendar.DAY_OF_MONTH),
                                month = calendar.get(Calendar.MONTH) + 1,
                                year = calendar.get(Calendar.YEAR)
                            )
                        }
                    }
                ) {
                    Text("Save", color = AppColors.OrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (birthDay != null) {
                        TextButton(onClick = { viewModel.clearBirthDate() }) {
                            Text("Clear", color = AppColors.Error)
                        }
                    }
                    TextButton(onClick = { viewModel.hideBirthDatePicker() }) {
                        Text("Cancel", color = AppColors.TextSecondary)
                    }
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppColors.BackgroundSheet)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = AppColors.BackgroundSheet,
                    titleContentColor = AppColors.TextPrimary,
                    headlineContentColor = AppColors.TextPrimary,
                    weekdayContentColor = AppColors.TextTertiary,
                    subheadContentColor = AppColors.TextSecondary,
                    yearContentColor = AppColors.TextPrimary,
                    currentYearContentColor = AppColors.OrangePrimary,
                    selectedYearContainerColor = AppColors.OrangePrimary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = AppColors.TextPrimary,
                    selectedDayContainerColor = AppColors.OrangePrimary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = AppColors.OrangePrimary,
                    todayDateBorderColor = AppColors.OrangePrimary
                )
            )
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentAge = calculatedAge ?: userAge,
            currentHeight = userHeightCm,
            currentWeight = userWeightKg,
            currentStepGoal = dailyStepGoal,
            currentCalorieGoal = dailyCalorieGoal,
            validationErrors = profileValidationErrors,
            onSave = { name, age, height, weight, stepGoal, calorieGoal ->
                viewModel.saveProfile(name, age, height, weight, stepGoal, calorieGoal)
                if (profileValidationErrors.nameError == null &&
                    profileValidationErrors.ageError == null &&
                    profileValidationErrors.heightError == null &&
                    profileValidationErrors.weightError == null &&
                    profileValidationErrors.stepGoalError == null &&
                    profileValidationErrors.calorieGoalError == null
                ) {
                    showEditProfileDialog = false
                }
            },
            onDismiss = {
                showEditProfileDialog = false
                viewModel.clearValidationErrors()
            }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            apiKeyError = apiKeyError,
            onSave = { viewModel.validateAndSaveApiKey(it) },
            onDismiss = { viewModel.hideApiKeyDialog() }
        )
    }

    if (showResetConfirmDialog) {
        ResetConfirmDialog(
            onConfirm = {
                showResetConfirmDialog = false
                viewModel.resetOnboarding()
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupOptionsDialog(
            onBackup = { includeApiKey ->
                viewModel.exportData(includeApiKey)
                showBackupDialog = false
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showRestoreConfirmDialog && selectedRestoreUri != null) {
        RestoreConfirmDialog(
            uri = selectedRestoreUri!!,
            viewModel = viewModel,
            onConfirm = {
                viewModel.importData(selectedRestoreUri!!)
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.hideAboutDialog() })
    }

    // Main Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.BackgroundDark,
                        AppColors.BackgroundElevated
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ==================== HEADER ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { -20 }
        ) {
            Text(
                text = "Profile",
                color = AppColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ==================== USER CARD ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 50)) + slideInVertically(tween(400, 50)) { 20 }
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                enableGlow = true,
                glowColor = AppColors.OrangePrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar with gradient
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AppColors.OrangePrimary,
                                            AppColors.OrangeSecondary
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(userName),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = userName.ifBlank { "User" },
                                color = AppColors.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        AppColors.tint(AppColors.OrangePrimary),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Pro Member",
                                    color = AppColors.OrangePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Edit button
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                AppColors.iconBackground(AppColors.OrangePrimary),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = AppColors.OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ==================== GOALS SECTION ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 100)) + slideInVertically(tween(400, 100)) { 20 }
        ) {
            Column {
                SectionHeader(icon = "🎯", title = "MY GOALS")
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Steps Goal
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Daily Steps",
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format("%,d", dailyStepGoal),
                                color = AppColors.TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Calories Goal
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Daily Calories",
                                color = AppColors.TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format("%,d", dailyCalorieGoal),
                                color = AppColors.TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ==================== BODY STATS ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 150)) + slideInVertically(tween(400, 150)) { 20 }
        ) {
            Column {
                SectionHeader(icon = "📊", title = "BODY STATS")
                Spacer(modifier = Modifier.height(12.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // BMI Row with badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BMI", color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (bmi.isNaN() || bmi == 0f) "--" else DecimalFormat("#.#").format(bmi),
                                    color = getBmiColor(bmiCategory),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (bmiCategory.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                getBmiColor(bmiCategory).copy(alpha = 0.15f),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = bmiCategory.uppercase(),
                                            color = getBmiColor(bmiCategory),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = AppColors.DividerColor)

                        // Birthday
                        ProfileInfoRow(
                            icon = Icons.Default.Cake,
                            iconColor = AppColors.Warning,
                            label = "Birthday",
                            value = formattedBirthDate ?: "Not set",
                            onClick = { viewModel.showBirthDatePicker() }
                        )

                        Divider(color = AppColors.DividerColor)

                        // Age
                        ProfileStatRow(label = "Age", value = "${calculatedAge ?: userAge} years")

                        Divider(color = AppColors.DividerColor)

                        // Height
                        ProfileStatRow(label = "Height", value = formattedHeight)

                        Divider(color = AppColors.DividerColor)

                        // Weight
                        ProfileStatRow(label = "Weight", value = formattedWeight)

                        Divider(color = AppColors.DividerColor)

                        // API Key
                        ProfileInfoRow(
                            icon = Icons.Default.Key,
                            iconColor = AppColors.OrangePrimary,
                            label = "API Key",
                            value = if (apiKey.isNotBlank()) "••••••••" else "Not set",
                            onClick = { viewModel.showApiKeyDialog() }
                        )
                    }
                }
            }
        }

        // ==================== APP SETTINGS ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 200)) + slideInVertically(tween(400, 200)) { 20 }
        ) {
            Column {
                SectionHeader(icon = "⚙️", title = "APP SETTINGS")
                Spacer(modifier = Modifier.height(12.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // Unit Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            AppColors.iconBackground(AppColors.Info),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Straighten,
                                        contentDescription = null,
                                        tint = AppColors.Info,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Imperial Units",
                                        color = AppColors.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (useImperialUnits) "ft/in, lbs, miles" else "cm, kg, km",
                                        color = AppColors.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = useImperialUnits,
                                onCheckedChange = { viewModel.setUnitPreference(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppColors.OrangePrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = AppColors.SurfaceLight
                                )
                            )
                        }

                        Divider(color = AppColors.DividerColor)

                        SettingsItem(
                            icon = Icons.Default.Cake,
                            iconBgColor = AppColors.Warning,
                            title = "Update Birthday",
                            subtitle = "Change your birth date",
                            onClick = { viewModel.showBirthDatePicker() }
                        )

                        Divider(color = AppColors.DividerColor)

                        SettingsItem(
                            icon = Icons.Default.Key,
                            iconBgColor = AppColors.OrangePrimary,
                            title = "Update API Key",
                            subtitle = "Change your Gemini API key",
                            onClick = { viewModel.showApiKeyDialog() }
                        )

                        Divider(color = AppColors.DividerColor)

                        SettingsItem(
                            icon = Icons.Default.Delete,
                            iconBgColor = AppColors.Error,
                            title = "Reset App",
                            subtitle = "Delete all local data",
                            titleColor = AppColors.Error,
                            onClick = { showResetConfirmDialog = true }
                        )
                    }
                }
            }
        }

        // ==================== BACKUP & RESTORE ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 250)) + slideInVertically(tween(400, 250)) { 20 }
        ) {
            Column {
                SectionHeader(icon = "☁️", title = "BACKUP & RESTORE")
                Spacer(modifier = Modifier.height(12.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.CloudUpload,
                            iconBgColor = AppColors.Success,
                            title = "Backup Data",
                            subtitle = "Export all your data to a file",
                            onClick = { showBackupDialog = true }
                        )

                        Divider(color = AppColors.DividerColor)

                        SettingsItem(
                            icon = Icons.Default.CloudDownload,
                            iconBgColor = AppColors.Info,
                            title = "Restore Data",
                            subtitle = "Import data from a backup file",
                            onClick = { restoreFilePicker.launch(arrayOf("application/json")) }
                        )
                    }
                }

                // Status feedback
                Spacer(modifier = Modifier.height(12.dp))
                
                when (backupState) {
                    is BackupState.Loading -> {
                        StatusCard(
                            icon = null,
                            message = "Creating backup...",
                            color = AppColors.Info,
                            isLoading = true
                        )
                    }
                    is BackupState.Success -> {
                        StatusCard(
                            icon = "✓",
                            message = "Backup created successfully!",
                            color = AppColors.Success
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.resetBackupState()
                        }
                    }
                    is BackupState.Error -> {
                        StatusCard(
                            icon = "✗",
                            message = (backupState as BackupState.Error).message,
                            color = AppColors.Error
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(5000)
                            viewModel.resetBackupState()
                        }
                    }
                    else -> {}
                }

                when (restoreState) {
                    is RestoreState.Loading -> {
                        StatusCard(
                            icon = null,
                            message = "Restoring data...",
                            color = AppColors.Info,
                            isLoading = true
                        )
                    }
                    is RestoreState.Success -> {
                        StatusCard(
                            icon = "✓",
                            message = "Data restored successfully!",
                            color = AppColors.Success
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.resetRestoreState()
                        }
                    }
                    is RestoreState.Error -> {
                        StatusCard(
                            icon = "✗",
                            message = (restoreState as RestoreState.Error).message,
                            color = AppColors.Error
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(5000)
                            viewModel.resetRestoreState()
                        }
                    }
                    else -> {}
                }
            }
        }

        // ==================== VERSION ====================
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(300, 300))
        ) {
            Text(
                text = "Fitu v2.0.0",
                color = AppColors.TextHint,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== HELPER COMPOSABLES ====================

@Composable
private fun SectionHeader(icon: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = title,
            color = AppColors.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(value, color = AppColors.TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            Text(label, color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                color = if (value == "Not set") AppColors.TextHint else AppColors.TextSecondary,
                fontSize = 14.sp
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                null,
                tint = AppColors.TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusCard(
    icon: String?,
    message: String,
    color: Color,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = color,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Text(text = icon, fontSize = 14.sp, color = color)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = message,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = AppColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconBgColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = AppColors.TextTertiary, fontSize = 12.sp)
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ==================== DIALOGS (Keep your existing dialogs but update colors) ====================
// I'll keep this short - use the same dialog code you have but replace hardcoded colors with AppColors

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    apiKeyError: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editKey by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    val isFormatValid = editKey.trim().let { it.length >= 20 && it.startsWith("AIza") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        title = {
            Text("Update API Key", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Enter your Google AI Studio API key:",
                    color = AppColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = editKey,
                    onValueChange = { editKey = it },
                    label = { Text("API Key", color = AppColors.TextHint) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = apiKeyError != null,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = AppColors.TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isFormatValid && apiKeyError == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Valid format",
                                    tint = AppColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedBorderColor = when {
                            apiKeyError != null -> AppColors.Error
                            isFormatValid -> AppColors.Success
                            else -> AppColors.OrangePrimary
                        },
                        unfocusedBorderColor = when {
                            apiKeyError != null -> AppColors.Error
                            isFormatValid -> AppColors.Success
                            else -> AppColors.BorderLight
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    apiKeyError != null -> Text(apiKeyError, color = AppColors.Error, fontSize = 12.sp)
                    isFormatValid -> Text("✓ Format looks good!", color = AppColors.Success, fontSize = 12.sp)
                    else -> Text(
                        "Key should start with 'AIza' and be 20+ characters",
                        color = AppColors.TextHint,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editKey) },
                enabled = editKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}

// Add these remaining dialog functions with similar styling updates...
// EditProfileDialog, ResetConfirmDialog, BackupOptionsDialog, RestoreConfirmDialog, AboutDialog
// (Keep your existing logic, just update colors to use AppColors)

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentAge: Int,
    currentHeight: Int,
    currentWeight: Int,
    currentStepGoal: Int,
    currentCalorieGoal: Int,
    validationErrors: com.fitu.ui.profile.ProfileValidationErrors,
    onSave: (String, Int, Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var age by remember { mutableStateOf(currentAge.toString()) }
    var height by remember { mutableStateOf(currentHeight.toString()) }
    var weight by remember { mutableStateOf(currentWeight.toString()) }
    var stepGoal by remember { mutableStateOf(currentStepGoal.toString()) }
    var calorieGoal by remember { mutableStateOf(currentCalorieGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        title = { Text("Edit Profile", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField(value = name, onValueChange = { name = it }, label = "Name", error = validationErrors.nameError)
                ProfileTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() } }, label = "Age", isNumber = true, error = validationErrors.ageError)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() } }, label = "Height (cm)", isNumber = true, modifier = Modifier.weight(1f), error = validationErrors.heightError)
                    ProfileTextField(value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() } }, label = "Weight (kg)", isNumber = true, modifier = Modifier.weight(1f), error = validationErrors.weightError)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(value = stepGoal, onValueChange = { stepGoal = it.filter { c -> c.isDigit() } }, label = "Step Goal", isNumber = true, modifier = Modifier.weight(1f), error = validationErrors.stepGoalError)
                    ProfileTextField(value = calorieGoal, onValueChange = { calorieGoal = it.filter { c -> c.isDigit() } }, label = "Cal Goal", isNumber = true, modifier = Modifier.weight(1f), error = validationErrors.calorieGoalError)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, age.toIntOrNull() ?: currentAge, height.toIntOrNull() ?: currentHeight, weight.toIntOrNull() ?: currentWeight, stepGoal.toIntOrNull() ?: currentStepGoal, calorieGoal.toIntOrNull() ?: currentCalorieGoal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.OrangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } }
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isNumber: Boolean = false,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = AppColors.TextHint) },
            keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedBorderColor = if (error != null) AppColors.Error else AppColors.OrangePrimary,
                unfocusedBorderColor = if (error != null) AppColors.Error else AppColors.BorderLight
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (error != null) {
            Text(text = error, color = AppColors.Error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Warning, null, tint = AppColors.Error, modifier = Modifier.size(28.dp)) }
        },
        title = { Text("Reset App?", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("This will permanently delete all your data.", color = AppColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("This action cannot be undone.", color = AppColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Yes, Reset Everything", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = AppColors.TextSecondary) } }
    )
}

@Composable
private fun BackupOptionsDialog(onBackup: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var includeApiKey by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        title = { Text("Backup Data", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Create a backup file with all your fitness data.", color = AppColors.TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { includeApiKey = !includeApiKey }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Include API Key", color = AppColors.TextPrimary, fontSize = 14.sp)
                    Checkbox(checked = includeApiKey, onCheckedChange = { includeApiKey = it }, colors = CheckboxDefaults.colors(checkedColor = AppColors.OrangePrimary))
                }
                if (includeApiKey) {
                    Text("⚠️ API key will be stored in plain text", color = AppColors.Warning, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onBackup(includeApiKey) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success), shape = RoundedCornerShape(12.dp)) {
                Text("Create Backup", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } }
    )
}

@Composable
private fun RestoreConfirmDialog(uri: Uri, viewModel: ProfileViewModel, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        title = { Text("Restore Data?", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text("This will add data from the backup file to your existing data.", color = AppColors.TextSecondary, fontSize = 14.sp) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Info), shape = RoundedCornerShape(12.dp)) {
                Text("Restore", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextSecondary) } }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BackgroundSheet,
        title = { Text("About Fitu", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Fitu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.OrangePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your AI-powered fitness companion", color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version 2.0.0", color = AppColors.TextHint, fontSize = 12.sp)
                Text("© 2026 Fitu", color = AppColors.TextHint, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = AppColors.OrangePrimary) } }
    )
}

// ==================== UTILITY FUNCTIONS ====================

private fun getInitials(name: String): String {
    if (name.isBlank()) return "U"
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty() -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "U"
    }
}

private fun getBmiColor(category: String): Color {
    return when (category) {
        "Underweight" -> AppColors.Info
        "Normal" -> AppColors.Success
        "Overweight" -> AppColors.Warning
        "Obese" -> AppColors.Error
        else -> AppColors.TextTertiary
    }
}
