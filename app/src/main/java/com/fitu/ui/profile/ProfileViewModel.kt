 package com.fitu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.util.BirthdayUtils
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userAge: StateFlow<Int> = userPreferencesRepository.userAge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val apiKey: StateFlow<String> = secureStorage.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Birth date fields
    val birthDay: StateFlow<Int?> = userPreferencesRepository.birthDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val birthMonth: StateFlow<Int?> = userPreferencesRepository.birthMonth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val birthYear: StateFlow<Int?> = userPreferencesRepository.birthYear
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Formatted birth date for display
    val formattedBirthDate: StateFlow<String?> = combine(birthDay, birthMonth, birthYear) { day, month, year ->
        BirthdayUtils.formatBirthDate(day, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Calculated age from birth date
    val calculatedAge: StateFlow<Int?> = combine(birthDay, birthMonth, birthYear) { day, month, year ->
        BirthdayUtils.calculateAge(day, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // BMI Calculation
    val bmi: StateFlow<Float> = combine(userHeightCm, userWeightKg) { height, weight ->
        if (height > 0) {
            val heightM = height / 100f
            weight / heightM.pow(2)
        } else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val bmiCategory: StateFlow<String> = bmi.combine(flowOf(Unit)) { bmiValue, _ ->
        when {
            bmiValue < 18.5f -> "Underweight"
            bmiValue < 25f -> "Normal"
            bmiValue < 30f -> "Overweight"
            else -> "Obese"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog

    private val _showBirthDatePicker = MutableStateFlow(false)
    val showBirthDatePicker: StateFlow<Boolean> = _showBirthDatePicker

    // ✅ API Key validation state
    private val _apiKeyValidationState = MutableStateFlow<ApiKeyValidationState>(ApiKeyValidationState.Idle)
    val apiKeyValidationState: StateFlow<ApiKeyValidationState> = _apiKeyValidationState

    // ✅ Profile validation errors
    private val _profileValidationErrors = MutableStateFlow<ProfileValidationErrors>(ProfileValidationErrors())
    val profileValidationErrors: StateFlow<ProfileValidationErrors> = _profileValidationErrors

    fun toggleEdit() {
        _isEditing.value = !_isEditing.value
    }

    fun showApiKeyDialog() {
        _apiKeyValidationState.value = ApiKeyValidationState.Idle
        _showApiKeyDialog.value = true
    }

    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
        _apiKeyValidationState.value = ApiKeyValidationState.Idle
    }

    fun showAboutDialog() {
        _showAboutDialog.value = true
    }

    fun hideAboutDialog() {
        _showAboutDialog.value = false
    }

    fun showBirthDatePicker() {
        _showBirthDatePicker.value = true
    }

    fun hideBirthDatePicker() {
        _showBirthDatePicker.value = false
    }

    /**
     * ✅ Validate profile inputs with realistic ranges
     */
    fun validateProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ): Boolean {
        val errors = ProfileValidationErrors()
        var isValid = true

        // Name validation
        val trimmedName = name.trim()
        when {
            trimmedName.isEmpty() -> {
                errors.nameError = "Name is required"
                isValid = false
            }
            trimmedName.length < 2 -> {
                errors.nameError = "Name must be at least 2 characters"
                isValid = false
            }
            trimmedName.length > 50 -> {
                errors.nameError = "Name must be less than 50 characters"
                isValid = false
            }
        }

        // Age validation (1-150)
        when {
            age < 1 -> {
                errors.ageError = "Age must be at least 1"
                isValid = false
            }
            age > 150 -> {
                errors.ageError = "Age must be less than 150"
                isValid = false
            }
        }

        // Height validation (50-300 cm)
        when {
            heightCm < 50 -> {
                errors.heightError = "Height must be at least 50 cm"
                isValid = false
            }
            heightCm > 300 -> {
                errors.heightError = "Height must be less than 300 cm"
                isValid = false
            }
        }

        // Weight validation (10-500 kg)
        when {
            weightKg < 10 -> {
                errors.weightError = "Weight must be at least 10 kg"
                isValid = false
            }
            weightKg > 500 -> {
                errors.weightError = "Weight must be less than 500 kg"
                isValid = false
            }
        }

        // Step goal validation (1000-100000)
        when {
            stepGoal < 1000 -> {
                errors.stepGoalError = "Step goal must be at least 1,000"
                isValid = false
            }
            stepGoal > 100000 -> {
                errors.stepGoalError = "Step goal must be less than 100,000"
                isValid = false
            }
        }

        // Calorie goal validation (500-10000)
        when {
            calorieGoal < 500 -> {
                errors.calorieGoalError = "Calorie goal must be at least 500"
                isValid = false
            }
            calorieGoal > 10000 -> {
                errors.calorieGoalError = "Calorie goal must be less than 10,000"
                isValid = false
            }
        }

        _profileValidationErrors.value = errors
        return isValid
    }

    fun clearValidationErrors() {
        _profileValidationErrors.value = ProfileValidationErrors()
    }

    fun saveProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ) {
        // Validate first
        if (!validateProfile(name, age, heightCm, weightKg, stepGoal, calorieGoal)) {
            return
        }

        viewModelScope.launch {
            userPreferencesRepository.saveUserProfile(
                name = name.trim(),
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                stepGoal = stepGoal,
                calorieGoal = calorieGoal
            )
            _isEditing.value = false
            clearValidationErrors()
        }
    }

    fun saveBirthDate(day: Int, month: Int, year: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveBirthDate(day, month, year)
            // Also update the calculated age
            val calculatedAge = BirthdayUtils.calculateAge(day, month, year) ?: 25
            userPreferencesRepository.saveUserProfile(
                name = userName.value,
                age = calculatedAge,
                heightCm = userHeightCm.value,
                weightKg = userWeightKg.value,
                stepGoal = dailyStepGoal.value,
                calorieGoal = dailyCalorieGoal.value
            )
            _showBirthDatePicker.value = false
        }
    }

    fun clearBirthDate() {
        viewModelScope.launch {
            userPreferencesRepository.saveBirthDate(null, null, null)
            _showBirthDatePicker.value = false
        }
    }

    /**
     * ✅ Validate API key before saving (for Google AI Studio free plan)
     */
    fun validateAndSaveApiKey(newApiKey: String) {
        val key = newApiKey.trim()

        // Basic format validation
        if (key.isEmpty()) {
            _apiKeyValidationState.value = ApiKeyValidationState.Error("API key is required")
            return
        }

        if (key.length < 20) {
            _apiKeyValidationState.value = ApiKeyValidationState.Error("API key seems too short")
            return
        }

        if (!key.startsWith("AIza")) {
            _apiKeyValidationState.value = ApiKeyValidationState.Error("Invalid format. Google AI keys start with 'AIza'")
            return
        }

        _apiKeyValidationState.value = ApiKeyValidationState.Validating

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = key
                )

                // Simple test - uses minimal tokens
                val response = withTimeoutOrNull(15000L) {
                    model.generateContent(content { text("Hi") })
                }

                withContext(Dispatchers.Main) {
                    when {
                        response == null -> {
                            _apiKeyValidationState.value = ApiKeyValidationState.Error(
                                "Connection timeout. Check your internet."
                            )
                        }
                        response.text != null -> {
                            // Valid! Save and close
                            secureStorage.saveApiKey(key)
                            _apiKeyValidationState.value = ApiKeyValidationState.Valid
                            _showApiKeyDialog.value = false
                        }
                        else -> {
                            _apiKeyValidationState.value = ApiKeyValidationState.Error(
                                "Validation failed. Please try again."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when {
                        e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                        e.message?.contains("invalid", ignoreCase = true) == true ->
                            "Invalid API key. Please check and try again."

                        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                            "API key doesn't have permission. Enable Generative Language API."

                        e.message?.contains("QUOTA_EXCEEDED", ignoreCase = true) == true ||
                        e.message?.contains("quota", ignoreCase = true) == true ->
                            "API quota exceeded. Wait a minute and try again."

                        e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                            "Rate limit reached. Wait a moment and try again."

                        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("network", ignoreCase = true) == true ->
                            "No internet connection."

                        else -> "Validation failed: ${e.message?.take(80) ?: "Unknown error"}"
                    }
                    _apiKeyValidationState.value = ApiKeyValidationState.Error(errorMessage)
                }
            }
        }
    }

    /**
     * Save API key without validation (for skip/later scenarios)
     */
    fun saveApiKeyWithoutValidation(apiKey: String) {
        viewModelScope.launch {
            secureStorage.saveApiKey(apiKey.trim())
            _showApiKeyDialog.value = false
        }
    }

    /**
     * Reset app - clears all user data
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.clearAllData()
            secureStorage.clearApiKey()
            userPreferencesRepository.setOnboardingComplete(false)
        }
    }
}

/**
 * API Key validation states
 */
sealed class ApiKeyValidationState {
    object Idle : ApiKeyValidationState()
    object Validating : ApiKeyValidationState()
    object Valid : ApiKeyValidationState()
    data class Error(val message: String) : ApiKeyValidationState()
}

/**
 * Profile validation errors
 */
data class ProfileValidationErrors(
    var nameError: String? = null,
    var ageError: String? = null,
    var heightError: String? = null,
    var weightError: String? = null,
    var stepGoalError: String? = null,
    var calorieGoalError: String? = null
) 
