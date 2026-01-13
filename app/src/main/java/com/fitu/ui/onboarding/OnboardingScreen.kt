 package com.fitu.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.theme.OrangePrimary
import java.util.Calendar

private val DarkBackground = Color(0xFF0A0A0F)
private val InputBackground = Color(0xFF1A1A1F)
private val BorderColor = Color(0xFF2A2A2F)
private val ErrorRed = Color(0xFFF44336)
private val SuccessGreen = Color(0xFF4CAF50)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val currentPage by viewModel.currentPage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Progress Bar
        Spacer(modifier = Modifier.height(24.dp))
        ProgressBar(currentPage = currentPage, pageCount = 2)

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
private fun ProgressBar(currentPage: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index <= currentPage) OrangePrimary else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoPage(viewModel: OnboardingViewModel) {
    val name by viewModel.name.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val birthDay by viewModel.birthDay.collectAsState()
    val birthMonth by viewModel.birthMonth.collectAsState()
    val birthYear by viewModel.birthYear.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()

    // Validation errors
    val nameError by viewModel.nameError.collectAsState()
    val heightError by viewModel.heightError.collectAsState()
    val weightError by viewModel.weightError.collectAsState()
    val stepGoalError by viewModel.stepGoalError.collectAsState()

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (birthYear != null && birthMonth != null && birthDay != null) {
            val calendar = Calendar.getInstance()
            calendar.set(birthYear!!, birthMonth!! - 1, birthDay!!)
            calendar.timeInMillis
        } else null,
        yearRange = 1940..2015
    )

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            viewModel.updateBirthDate(
                                day = calendar.get(Calendar.DAY_OF_MONTH),
                                month = calendar.get(Calendar.MONTH) + 1,
                                year = calendar.get(Calendar.YEAR)
                            )
                        }
                        viewModel.hideDatePicker()
                    }
                ) {
                    Text("OK", color = OrangePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1A1A1F)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF1A1A1F),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.5f),
                    subheadContentColor = Color.White.copy(alpha = 0.7f),
                    yearContentColor = Color.White,
                    currentYearContentColor = OrangePrimary,
                    selectedYearContainerColor = OrangePrimary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = Color.White,
                    selectedDayContainerColor = OrangePrimary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = OrangePrimary,
                    todayDateBorderColor = OrangePrimary
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Tell us about you",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We use this to personalize your AI coach.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name
        InputLabel("NAME")
        Spacer(modifier = Modifier.height(8.dp))
        DarkTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            placeholder = "Your name",
            isError = nameError != null,
            errorMessage = nameError
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Birthday (Optional)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputLabel("BIRTHDAY")
            Text(
                text = "(Optional)",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Birthday picker button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .clickable { viewModel.showDatePicker() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.getFormattedBirthDate() ?: "Select your birthday",
                    color = if (birthDay != null) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Default.Cake,
                    contentDescription = "Select birthday",
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Used to personalize your fitness goals and wish you on your birthday 🎂",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Height & Weight Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                InputLabel("HEIGHT (CM)")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(
                    value = heightCm,
                    onValueChange = { viewModel.updateHeightCm(it) },
                    placeholder = "170",
                    keyboardType = KeyboardType.Number,
                    isError = heightError != null,
                    errorMessage = heightError
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                InputLabel("WEIGHT (KG)")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(
                    value = weightKg,
                    onValueChange = { viewModel.updateWeightKg(it) },
                    placeholder = "70",
                    keyboardType = KeyboardType.Number,
                    isError = weightError != null,
                    errorMessage = weightError
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Step Goal
        InputLabel("DAILY STEP GOAL")
        Spacer(modifier = Modifier.height(8.dp))
        DarkTextField(
            value = stepGoal,
            onValueChange = { viewModel.updateStepGoal(it) },
            placeholder = "10000",
            keyboardType = KeyboardType.Number,
            isError = stepGoalError != null,
            errorMessage = stepGoalError
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button
        Button(
            onClick = { viewModel.nextPage() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun ApiSetupPage(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val apiKey by viewModel.apiKey.collectAsState()
    val showApiKey by viewModel.showApiKey.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val context = LocalContext.current

    // Determine border color based on validation state
    val borderColor by animateColorAsState(
        targetValue = when (validationState) {
            is ApiValidationState.Valid -> SuccessGreen
            is ApiValidationState.Error -> ErrorRed
            else -> BorderColor
        },
        label = "borderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Power up AI",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your Gemini API key to enable nutrition vision and smart coaching.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(OrangePrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangePrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Google AI Studio (Free)",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get a free API key from Google AI Studio. Your key is stored securely on your device.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key Input
        InputLabel("API KEY")
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.updateApiKey(it) },
            placeholder = {
                Text(
                    text = "AIzaSy...",
                    color = Color.White.copy(alpha = 0.3f)
                )
            },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show/Hide toggle
                    IconButton(onClick = { viewModel.toggleShowApiKey() }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide" else "Show",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Validation status icon
                    when (validationState) {
                        is ApiValidationState.Valid -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is ApiValidationState.Error -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Invalid",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> {}
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                cursorColor = OrangePrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Validation status message
        Spacer(modifier = Modifier.height(8.dp))
        when (validationState) {
            is ApiValidationState.Valid -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "API key is valid! ✓",
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is ApiValidationState.Error -> {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = (validationState as ApiValidationState.Error).message,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            is ApiValidationState.Validating -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = OrangePrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Validating API key...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
            else -> {
                Text(
                    text = "We'll validate your key before continuing.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get API Key Link
        Text(
            text = "Get a free API key here →",
            color = OrangePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Free tier info
        Text(
            text = "💡 Free tier includes 15 requests/minute, 1 million tokens/month",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Validate Button (shows when not validated)
        if (validationState !is ApiValidationState.Valid) {
            OutlinedButton(
                onClick = { viewModel.validateApiKey() },
                enabled = apiKey.isNotBlank() && validationState !is ApiValidationState.Validating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OrangePrimary)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (validationState is ApiValidationState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OrangePrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Validating...", color = OrangePrimary)
                } else {
                    Text(
                        text = "Validate API Key",
                        color = OrangePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Start Journey Button (enabled only when validated)
        Button(
            onClick = { viewModel.completeOnboarding(onComplete) },
            enabled = validationState is ApiValidationState.Valid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary.copy(alpha = 0.8f),
                disabledContainerColor = OrangePrimary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Start Journey",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun InputLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.3f)
                )
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = if (isError) ErrorRed else BorderColor,
                unfocusedBorderColor = if (isError) ErrorRed else BorderColor,
                errorBorderColor = ErrorRed,
                cursorColor = OrangePrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        // Error message
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = ErrorRed,
                fontSize = 12.sp
            )
        }
    }
} 
