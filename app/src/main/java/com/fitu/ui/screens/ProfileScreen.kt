package com.fitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val userHeightCm by viewModel.userHeightCm.collectAsState()
    val userWeightKg by viewModel.userWeightKg.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val bmi by viewModel.bmi.collectAsState()
    val bmiCategory by viewModel.bmiCategory.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()

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
            .padding(top = 32.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Header ---
        Text(
            text = "Profile",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        // --- User Info ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(listOf(OrangePrimary, Color(0xFFD94F00))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(userName),
                    color = Color.White,
                    fontSize = 28.sp,
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

        // --- Body Stats ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // BMI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BMI", color = Color.White, fontSize = 16.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DecimalFormat("#.#").format(bmi),
                            color = getBmiColor(bmiCategory),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(getBmiColor(bmiCategory).copy(alpha = 0.2f), RoundedCornerShape(100))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = bmiCategory.uppercase(),
                                color = getBmiColor(bmiCategory),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.1f)))
                // Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Height", color = Color.White, fontSize = 16.sp)
                    Text("$userHeightCm cm", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.1f)))
                // Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weight", color = Color.White, fontSize = 16.sp)
                    Text("$userWeightKg kg", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.1f)))
                // API Key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("API Key", color = Color.White, fontSize = 16.sp)
                    Text(
                        text = if (apiKey.isNotBlank()) "••••••••" else "Not set",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // --- App Settings ---
        Text(
            text = "APP SETTINGS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Download,
                    iconBgColor = Color(0xFF2196F3),
                    title = "Export Data",
                    subtitle = "Download JSON backup",
                    onClick = { /* TODO */ }
                )
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.1f)))
                SettingsItem(
                    icon = Icons.Default.Delete,
                    iconBgColor = Color(0xFFF44336),
                    title = "Reset App",
                    subtitle = "Delete all local data",
                    titleColor = Color(0xFFF44336),
                    onClick = { viewModel.resetOnboarding() }
                )
            }
        }

        // --- Version ---
        Text(
            text = "Version 2.0.0 • Fitu PWA",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = Color.White,
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
                .background(iconBgColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconBgColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        title = { Text("Update API Key", color = Color.White) },
        text = {
            Column {
                Text("Enter your Gemini API key:", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = onKeyChange,
                    label = { Text("API Key", color = Color.White.copy(alpha = 0.5f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        title = { Text("About Fitu", color = Color.White) },
        text = {
            Column {
                Text(
                    text = "Fitu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your AI-powered fitness companion", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version 2.0.0", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text("© 2026 Fitu", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OrangePrimary)
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
