package com.fitu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.service.StepCounterService
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.fitu.util.BirthdayUtils
import com.fitu.util.UnitConverter
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geminiModelProvider: GeminiModelProvider,
    private val stepDao: StepDao
) : ViewModel() {

    // User info
    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // User's physical stats for personalized calculations
    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // Unit preference
    val useImperialUnits: StateFlow<Boolean> = userPreferencesRepository.useImperialUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Today's date formatted
    val todayDate: String = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    // Daily recap
    private val _dailyRecap = MutableStateFlow<String>("Loading daily recap...")
    val dailyRecap: StateFlow<String> = _dailyRecap

    // Steps - Read directly from StepCounterService (real-time data)
    val currentSteps: StateFlow<Int> = StepCounterService.stepCount
    
    // Track if steps are initialized (to prevent showing 0)
    val isStepsInitialized: StateFlow<Boolean> = StepCounterService.isInitialized

    // Loading state for weekly steps chart
    private val _isWeeklyDataLoading = MutableStateFlow(true)
    val isWeeklyDataLoading: StateFlow<Boolean> = _isWeeklyDataLoading

    // Weekly progress - Raw data from database
    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())
    
    // Weekly steps - Combines DB data with live step count for reactive updates
    val weeklySteps: StateFlow<List<Int>> = combine(
        _weeklyStepsFromDb,
        currentSteps
    ) { stepEntities, todaySteps ->
        buildWeeklyData(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(0, 0, 0, 0, 0, 0, 0))

    // Workout summary
    val workoutsCompleted: StateFlow<Int> = repository.getWorkoutsForDay(
        getTodayRange().first, getTodayRange().second
    ).map { it.size }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Birthday feature
    private val _showBirthdayDialog = MutableStateFlow(false)
    val showBirthdayDialog: StateFlow<Boolean> = _showBirthdayDialog

    private val _isBirthday = MutableStateFlow(false)
    val isBirthday: StateFlow<Boolean> = _isBirthday

    // 🎉 Goal Celebration
    private val _showStepGoalCelebration = MutableStateFlow(false)
    val showStepGoalCelebration: StateFlow<Boolean> = _showStepGoalCelebration.asStateFlow()

    private val _hasShownStepCelebrationToday = MutableStateFlow(false)

    // Helper to get start and end of today
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar
