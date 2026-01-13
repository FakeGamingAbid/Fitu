 package com.fitu.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.util.BirthdayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage  // ✅ FIX #2: Only use SecureStorage for API key
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

    fun updateName(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun updateHeightCm(value: String) {
        _heightCm.value = value.filter { it.isDigit() }
    }

    fun updateWeightKg(value: String) {
        _weightKg.value = value.filter { it.isDigit() }
    }

    fun updateStepGoal(value: String) {
        _stepGoal.value = value.filter { it.isDigit() }
    }

    fun updateCalorieGoal(value: String) {
        _calorieGoal.value = value.filter { it.isDigit() }
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
        _validationState.value = ApiValidationState.Idle
    }

    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    fun validatePage1(): Boolean {
        var isValid = true

        if (_name.value.length < 2) {
            _nameError.value = "Name must be at least 2 characters"
            isValid = false
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

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Calculate age from birth date if available, otherwise use default
            val calculatedAge = getCalculatedAge()

            // Save user profile (NO API key here - that's separate)
            userPreferencesRepository.saveUserProfile(
                name = _name.value,
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

            // ✅ FIX #2: Save API key ONLY to SecureStorage (encrypted)
            secureStorage.saveApiKey(_apiKey.value)

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
