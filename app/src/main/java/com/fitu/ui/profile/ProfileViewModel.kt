 package com.fitu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.util.BirthdayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage  // ✅ FIX #2: Use SecureStorage for API key
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

    // ✅ FIX #2: Get API key from SecureStorage (encrypted)
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

    fun toggleEdit() {
        _isEditing.value = !_isEditing.value
    }

    fun showApiKeyDialog() {
        _showApiKeyDialog.value = true
    }

    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
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

    fun saveProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ) {
        viewModelScope.launch {
            userPreferencesRepository.saveUserProfile(
                name = name,
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                stepGoal = stepGoal,
                calorieGoal = calorieGoal
            )
            _isEditing.value = false
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

    // ✅ FIX #2: Save API key ONLY to SecureStorage (encrypted)
    fun saveApiKey(apiKey: String) {
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
            // Clear preferences
            userPreferencesRepository.clearAllData()
            
            // Clear API key from secure storage
            secureStorage.clearApiKey()
            
            // Mark onboarding as incomplete
            userPreferencesRepository.setOnboardingComplete(false)
        }
    }
} 
