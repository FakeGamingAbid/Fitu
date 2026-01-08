package com.fitu.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val currentPage by viewModel.currentPage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Page Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PageIndicator(currentPage = currentPage, pageCount = 2)
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(targetState = currentPage, label = "page") { page ->
            when (page) {
                0 -> PersonalInfoPage(viewModel)
                1 -> ApiSetupPage(viewModel, onComplete)
            }
        }
    }
}

@Composable
fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 12.dp else 8.dp)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun PersonalInfoPage(viewModel: OnboardingViewModel) {
    val name by viewModel.name.collectAsState()
    val age by viewModel.age.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val useImperial by viewModel.useImperial.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val ageError by viewModel.ageError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Welcome to Fitu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Let's set up your profile",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Name") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Age
        OutlinedTextField(
            value = age,
            onValueChange = { viewModel.updateAge(it) },
            label = { Text("Age") },
            isError = ageError != null,
            supportingText = ageError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Height with toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = heightCm,
                onValueChange = { viewModel.updateHeightCm(it) },
                label = { Text(if (useImperial) "Height (in)" else "Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Imperial", style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = useImperial,
                    onCheckedChange = { viewModel.toggleImperial() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight
        OutlinedTextField(
            value = weightKg,
            onValueChange = { viewModel.updateWeightKg(it) },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step Goal
        OutlinedTextField(
            value = stepGoal,
            onValueChange = { viewModel.updateStepGoal(it) },
            label = { Text("Daily Step Goal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calorie Goal
        OutlinedTextField(
            value = calorieGoal,
            onValueChange = { viewModel.updateCalorieGoal(it) },
            label = { Text("Daily Calorie Goal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // Next Button
        Button(
            onClick = { viewModel.nextPage() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
fun ApiSetupPage(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val apiKey by viewModel.apiKey.collectAsState()
    val showApiKey by viewModel.showApiKey.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "API Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Connect your Gemini AI",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Key") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleShowApiKey() }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = if (showApiKey) "Hide" else "Show"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Validation Button
                Button(
                    onClick = { viewModel.validateApiKey() },
                    enabled = validationState !is ApiValidationState.Validating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (validationState) {
                        is ApiValidationState.Validating -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating...")
                        }
                        is ApiValidationState.Valid -> {
                            Icon(Icons.Default.Check, contentDescription = "Valid API Key")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Valid!")
                        }
                        else -> Text("Validate API Key")
                    }
                }

                // Error/Success message
                when (validationState) {
                    is ApiValidationState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (validationState as ApiValidationState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ApiValidationState.Valid -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "API key is valid!",
                            color = Color.Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help Link
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
            }
        ) {
            Text("How to get an API key?")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.previousPage() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = { viewModel.completeOnboarding(onComplete) },
                enabled = validationState is ApiValidationState.Valid || apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Started")
            }
        }
    }
}
