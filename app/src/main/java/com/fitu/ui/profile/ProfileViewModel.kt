package com.fitu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // BMI Calculation
    val bmi: StateFlow<Float> = combine(userHeightCm, userWeightKg) { height, weight ->
        if (height > 0) {
            val heightM = height / 100f
            weight / heightM.pow(2)
        } else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val bmiCategory: StateFlow<String> = bmi.combine(kotlinx.coroutines.flow.flowOf(Unit)) { bmiValue, _ ->
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

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            secureStorage.saveApiKey(apiKey.trim())
            _showApiKeyDialog.value = false
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingComplete(false)
        }
    }
}
