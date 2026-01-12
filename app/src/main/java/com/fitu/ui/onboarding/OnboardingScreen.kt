package com.fitu.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.theme.OrangePrimary
import java.time.LocalDate
import java.util.Calendar

private val DarkBackground = Color(0xFF0A0A0F)
private val InputBackground = Color(0xFF1A1A1F)
private val BorderColor = Color(0xFF2A2A2F)

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
            placeholder = "Your name"
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
                    keyboardType = KeyboardType.Number
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                InputLabel("WEIGHT (KG)")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(
                    value = weightKg,
                    onValueChange = { viewModel.updateWeightKg(it) },
                    placeholder = "70",
                    keyboardType = KeyboardType.Number
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
            keyboardType = KeyboardType.Number
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
    val validationState by viewModel.validationState.collectAsState()
    val context = LocalContext.current

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
                        text = "Gemini API Required",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fitu runs entirely in your browser. Your key is stored securely on your device.",
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
        DarkTextField(
            value = apiKey,
            onValueChange = { viewModel.updateApiKey(it) },
            placeholder = "AIzaSy...",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Get API Key Link
        Text(
            text = "Get a free API key here",
            color = OrangePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Start Journey Button
        Button(
            onClick = { viewModel.completeOnboarding(onComplete) },
            enabled = apiKey.isNotBlank(),
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
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.3f)
            )
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            focusedBorderColor = BorderColor,
            unfocusedBorderColor = BorderColor,
            cursorColor = OrangePrimary
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
