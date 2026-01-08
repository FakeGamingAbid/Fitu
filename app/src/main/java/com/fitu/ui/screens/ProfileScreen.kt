package com.fitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.GlassCard
import com.fitu.ui.profile.ProfileViewModel
import com.fitu.ui.theme.OrangePrimary
import java.text.DecimalFormat

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
    val isEditing by viewModel.isEditing.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()

    // Edit state
    var editName by remember(userName) { mutableStateOf(userName) }
    var editAge by remember(userAge) { mutableStateOf(userAge.toString()) }
    var editHeight by remember(userHeightCm) { mutableStateOf(userHeightCm.toString()) }
    var editWeight by remember(userWeightKg) { mutableStateOf(userWeightKg.toString()) }
    var editStepGoal by remember(dailyStepGoal) { mutableStateOf(dailyStepGoal.toString()) }
    var editCalorieGoal by remember(dailyCalorieGoal) { mutableStateOf(dailyCalorieGoal.toString()) }
    var editApiKey by remember(apiKey) { mutableStateOf(apiKey) }

    // Dialogs
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = editApiKey,
            onKeyChange = { editApiKey = it },
            onSave = { viewModel.saveApiKey(editApiKey) },
            onDismiss = { viewModel.hideApiKeyDialog() }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.hideAboutDialog() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Header ---
        Text(
            text = "Profile",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        // --- User Info ---
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(OrangePrimary, Color(0xFFD94F00))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(userName),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = userName.ifBlank { "User" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pro Member",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        // --- My Goals ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MY GOALS", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Daily Steps", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        Text(String.format("%,d", dailyStepGoal), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Daily Calories", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        Text(String.format("%,d", dailyCalorieGoal), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Body Stats ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("BODY STATS", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            GlassCard {
                if (isEditing) {
                    // Edit Mode
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it.filter { c -> c.isDigit() } },
                                label = { Text("Age", color = Color.White.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it.filter { c -> c.isDigit() } },
                                label = { Text("Height (cm)", color = Color.White.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it.filter { c -> c.isDigit() } },
                                label = { Text("Weight (kg)", color = Color.White.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editStepGoal,
                                onValueChange = { editStepGoal = it.filter { c -> c.isDigit() } },
                                label = { Text("Step Goal", color = Color.White.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveProfile(
                                    name = editName,
                                    age = editAge.toIntOrNull() ?: userAge,
                                    heightCm = editHeight.toIntOrNull() ?: userHeightCm,
                                    weightKg = editWeight.toIntOrNull() ?: userWeightKg,
                                    stepGoal = editStepGoal.toIntOrNull() ?: dailyStepGoal,
                                    calorieGoal = editCalorieGoal.toIntOrNull() ?: dailyCalorieGoal
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Display Mode
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("BMI", color = Color.White.copy(alpha = 0.8f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(DecimalFormat("#.#").format(bmi), color = getBmiColor(bmiCategory), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Box(
                                    modifier = Modifier
                                        .background(getBmiColor(bmiCategory).copy(alpha = 0.2f), RoundedCornerShape(100))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(bmiCategory.uppercase(), color = getBmiColor(bmiCategory), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Height", color = Color.White.copy(alpha = 0.8f))
                            Text("$userHeightCm cm", color = Color.White.copy(alpha = 0.8f))
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Weight", color = Color.White.copy(alpha = 0.8f))
                            Text("$userWeightKg kg", color = Color.White.copy(alpha = 0.8f))
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("API Key", color = Color.White.copy(alpha = 0.8f))
                            Text("••••••••", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // --- App Settings ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("APP SETTINGS", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            GlassCard(modifier = Modifier.padding(0.dp)) {
                Column {
                    SettingsItem(
                        icon = Icons.Filled.Edit,
                        title = "Edit Profile",
                        onClick = { viewModel.toggleEdit() }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    SettingsItem(
                        icon = Icons.Filled.Key,
                        title = "Update API Key",
                        subtitle = if (apiKey.isNotBlank()) "Configured" else "Not set",
                        onClick = { viewModel.showApiKeyDialog() }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    SettingsItem(
                        icon = Icons.Filled.Refresh,
                        title = "Reset Onboarding",
                        subtitle = "Start fresh",
                        onClick = { viewModel.resetOnboarding() }
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = "About",
                        subtitle = "Version 1.0",
                        onClick = { viewModel.showAboutDialog() }
                    )
                }
            }
        }
        
        Text("Version 2.0.0 • Fitu Android", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(OrangePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
fun ApiKeyDialog(
    currentKey: String,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update API Key") },
        text = {
            Column {
                Text("Enter your Gemini API key:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = onKeyChange,
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Fitu") },
        text = {
            Column {
                Text(
                    text = "Fitu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your AI-powered fitness companion")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version 2.0.0", style = MaterialTheme.typography.bodySmall)
                Text("© 2026 Fitu", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun getInitials(name: String): String {
    if (name.isBlank()) return "U"
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "U"
    }
}

private fun getBmiColor(category: String): Color {
    return when (category) {
        "Underweight" -> Color(0xFF2196F3)
        "Normal" -> Color(0xFF4CAF50)
        "Overweight" -> Color(0xFFFF9800)
        "Obese" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}
