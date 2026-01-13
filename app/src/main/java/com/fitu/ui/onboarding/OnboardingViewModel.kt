 package com.fitu.ui.onboarding

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Page 1 - Personal Info
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _heightCm = MutableStateFlow("170")
    val heightCm: StateFlow<String> = _heightCm

    private val _weightKg = MutableStateFlow("70")
    val weightKg: StateFlow<String> = _weightKg

    private val _stepGoal = MutableStateFlow("10000")
    val stepGoal: StateFlow<String> = _stepGoal

    private val _calorieGoal = MutableStateFlow("2000")
    val calorieGoal: StateFlow<String> = _calorieGoal

    // Birth date (optional)
    private val _birthDay = MutableStateFlow<Int?>(null)
    val birthDay: StateFlow<Int?> = _birthDay

    private val _birthMonth = MutableStateFlow<Int?>(null)
    val birthMonth: StateFlow<Int?> = _birthMonth

    private val _birthYear = MutableStateFlow<Int?>(null)
    val birthYear: StateFlow<Int?> = _birthYear

    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker

    // Page 2 - API Setup
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _showApiKey = MutableStateFlow(false)
    val showApiKey: StateFlow<Boolean> = _showApiKey

    private val _validationState = MutableStateFlow<ApiValidationState>(ApiValidationState.Idle)
    val validationState: StateFlow<ApiValidationState> = _validationState

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    // Validation errors
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError

    private val _heightError = MutableStateFlow<String?>(null)
    val heightError: StateFlow<String?> = _heightError

    private val _weightError = MutableStateFlow<String?>(null)
    val weightError: StateFlow<String?> = _weightError

    private val _stepGoalError = MutableStateFlow<String?>(null)
    val stepGoalError: StateFlow<String?> = _stepGoalError

    private val _calorieGoalError = MutableStateFlow<String?>(null)
    val calorieGoalError: StateFlow<String?> = _calorieGoalError

    fun updateName(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun updateHeightCm(value: String) {
        _heightCm.value = value.filter { it.isDigit() }
        _heightError.value = null
    }

    fun updateWeightKg(value: String) {
        _weightKg.value = value.filter { it.isDigit() }
        _weightError.value = null
    }

    fun updateStepGoal(value: String) {
        _stepGoal.value = value.filter { it.isDigit() }
        _stepGoalError.value = null
    }

    fun updateCalorieGoal(value: String) {
        _calorieGoal.value = value.filter { it.isDigit() }
        _calorieGoalError.value = null
    }

    fun updateBirthDate(day: Int, month: Int, year: Int) {
        _birthDay.value = day
        _birthMonth.value = month
        _birthYear.value = year
    }

    fun clearBirthDate() {
        _birthDay.value = null
        _birthMonth.value = null
        _birthYear.value = null
    }

    fun showDatePicker() {
        _showDatePicker.value = true
    }

    fun hideDatePicker() {
        _showDatePicker.value = false
    }

    fun updateApiKey(value: String) {
        _apiKey.value = value
        // Reset validation state when key changes
        if (_validationState.value is ApiValidationState.Error || 
            _validationState.value is ApiValidationState.Valid) {
            _validationState.value = ApiValidationState.Idle
        }
    }

    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    /**
     * ✅ Validate Page 1 inputs with realistic ranges
     */
    fun validatePage1(): Boolean {
        var isValid = true

        // Name validation
        val trimmedName = _name.value.trim()
        when {
            trimmedName.isEmpty() -> {
                _nameError.value = "Name is required"
                isValid = false
            }
            trimmedName.length < 2 -> {
                _nameError.value = "Name must be at least 2 characters"
                isValid = false
            }
            trimmedName.length > 50 -> {
                _nameError.value = "Name must be less than 50 characters"
                isValid = false
            }
            !trimmedName.matches(Regex("^[a-zA-Z\\s]+$")) -> {
                _nameError.value = "Name can only contain letters and spaces"
                isValid = false
            }
        }

        // Height validation (realistic range: 50cm - 300cm)
        val height = _heightCm.value.toIntOrNull()
        when {
            height == null || _heightCm.value.isEmpty() -> {
                _heightError.value = "Height is required"
                isValid = false
            }
            height < 50 -> {
                _heightError.value = "Height must be at least 50 cm"
                isValid = false
            }
            height > 300 -> {
                _heightError.value = "Height must be less than 300 cm"
                isValid = false
            }
        }

        // Weight validation (realistic range: 10kg - 500kg)
        val weight = _weightKg.value.toIntOrNull()
        when {
            weight == null || _weightKg.value.isEmpty() -> {
                _weightError.value = "Weight is required"
                isValid = false
            }
            weight < 10 -> {
                _weightError.value = "Weight must be at least 10 kg"
                isValid = false
            }
            weight > 500 -> {
                _weightError.value = "Weight must be less than 500 kg"
                isValid = false
            }
        }

        // Step goal validation (realistic range: 1000 - 100000)
        val steps = _stepGoal.value.toIntOrNull()
        when {
            steps == null || _stepGoal.value.isEmpty() -> {
                _stepGoalError.value = "Step goal is required"
                isValid = false
            }
            steps < 1000 -> {
                _stepGoalError.value = "Step goal must be at least 1,000"
                isValid = false
            }
            steps > 100000 -> {
                _stepGoalError.value = "Step goal must be less than 100,000"
                isValid = false
            }
        }

        return isValid
    }

    fun nextPage() {
        if (_currentPage.value == 0 && validatePage1()) {
            _currentPage.value = 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }

    /**
     * Get formatted birth date for display.
     */
    fun getFormattedBirthDate(): String? {
        return BirthdayUtils.formatBirthDate(_birthDay.value, _birthMonth.value, _birthYear.value)
    }

    /**
     * Calculate user's age from birth date.
     */
    fun getCalculatedAge(): Int {
        return BirthdayUtils.calculateAge(_birthDay.value, _birthMonth.value, _birthYear.value) ?: 25
    }

    /**
     * ✅ Validate API key by making a simple test call to Gemini
     * Uses minimal tokens to avoid wasting free tier quota
     */
    fun validateApiKey() {
        val key = _apiKey.value.trim()
        
        // Basic format validation first
        if (key.isEmpty()) {
            _validationState.value = ApiValidationState.Error("API key is required")
            return
        }
        
        if (key.length < 20) {
            _validationState.value = ApiValidationState.Error("API key seems too short")
            return
        }
        
        if (!key.startsWith("AIza")) {
            _validationState.value = ApiValidationState.Error("Invalid API key format. Google AI keys start with 'AIza'")
            return
        }

        _validationState.value = ApiValidationState.Validating

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = key
                )

                // Simple test prompt - uses minimal tokens
                val response = withTimeoutOrNull(15000L) {
                    model.generateContent(content { text("Hi") })
                }

                withContext(Dispatchers.Main) {
                    when {
                        response == null -> {
                            _validationState.value = ApiValidationState.Error(
                                "Connection timeout. Please check your internet connection."
                            )
                        }
                        response.text != null -> {
                            _validationState.value = ApiValidationState.Valid
                        }
                        else -> {
                            _validationState.value = ApiValidationState.Error(
                                "API key validation failed. Please try again."
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
                            "API key doesn't have permission. Enable Generative Language API in Google Cloud Console."
                        
                        e.message?.contains("QUOTA_EXCEEDED", ignoreCase = true) == true ||
                        e.message?.contains("quota", ignoreCase = true) == true ->
                            "API quota exceeded. Wait a minute or check your Google AI Studio limits."
                        
                        e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                            "Rate limit reached. Please wait a moment and try again."
                        
                        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("network", ignoreCase = true) == true ->
                            "No internet connection. Please check your network."
                        
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Connection timeout. Please try again."
                        
                        else -> "Validation failed: ${e.message?.take(100) ?: "Unknown error"}"
                    }
                    _validationState.value = ApiValidationState.Error(errorMessage)
                }
            }
        }
    }

    /**
     * Complete onboarding - only proceeds if API key is validated
     */
    fun completeOnboarding(onComplete: () -> Unit) {
        // If not validated yet, validate first
        if (_validationState.value !is ApiValidationState.Valid) {
            validateApiKey()
            return
        }

        viewModelScope.launch {
            // Calculate age from birth date if available, otherwise use default
            val calculatedAge = getCalculatedAge()

            // Save user profile
            userPreferencesRepository.saveUserProfile(
                name = _name.value.trim(),
                age = calculatedAge,
                heightCm = _heightCm.value.toIntOrNull() ?: 170,
                weightKg = _weightKg.value.toIntOrNull() ?: 70,
                stepGoal = _stepGoal.value.toIntOrNull() ?: 10000,
                calorieGoal = _calorieGoal.value.toIntOrNull() ?: 2000
            )

            // Save birth date (optional)
            userPreferencesRepository.saveBirthDate(
                day = _birthDay.value,
                month = _birthMonth.value,
                year = _birthYear.value
            )

            // Save API key ONLY to SecureStorage (encrypted)
            secureStorage.saveApiKey(_apiKey.value.trim())

            // Mark onboarding complete
            userPreferencesRepository.setOnboardingComplete(true)

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}

sealed class ApiValidationState {
    object Idle : ApiValidationState()
    object Validating : ApiValidationState()
    object Valid : ApiValidationState()
    data class Error(val message: String) : ApiValidationState()
} 
