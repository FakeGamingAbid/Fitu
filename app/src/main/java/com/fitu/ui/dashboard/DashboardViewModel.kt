package com.fitu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.repository.StreakRepository
import com.fitu.data.repository.StreakData
import com.fitu.data.service.StepCounterService
import com.fitu.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val stepDao: StepDao,
    private val streakRepository: StreakRepository
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

    // Steps - Read directly from StepCounterService (real-time data)
    val currentSteps: StateFlow<Int> = StepCounterService.stepCount

    // Track if steps are initialized (to prevent showing 0)
    val isStepsInitialized: StateFlow<Boolean> = StepCounterService.isInitialized

    // Loading state for weekly steps chart
    private val _isWeeklyDataLoading = MutableStateFlow(true)
    val isWeeklyDataLoading: StateFlow<Boolean> = _isWeeklyDataLoading.asStateFlow()

    // Weekly progress - Raw data from database
    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())

    // Weekly steps - Combines DB data with live step count for reactive updates
    val weeklySteps: StateFlow<List<Int>> = combine(
        _weeklyStepsFromDb,
        currentSteps
    ) { stepEntities, todaySteps ->
        buildWeeklyData(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(0, 0, 0, 0, 0, 0, 0))

    // Calories
    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _caloriesConsumed = MutableStateFlow(0)
    val caloriesConsumed: StateFlow<Int> = _caloriesConsumed.asStateFlow()

    // Workouts completed today
    private val _workoutsCompleted = MutableStateFlow(0)
    val workoutsCompleted: StateFlow<Int> = _workoutsCompleted.asStateFlow()

    // Distance calculation with unit conversion
    val formattedDistance: StateFlow<String> = combine(
        currentSteps,
        userHeightCm,
        useImperialUnits
    ) { steps, heightCm, useImperial ->
        val strideLength = heightCm * 0.415 / 100.0 // Convert to meters
        val distanceKm = (steps * strideLength) / 1000.0
        
        if (useImperial) {
            val miles = distanceKm * 0.621371
            String.format("%.2f", miles)
        } else {
            String.format("%.2f", distanceKm)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.00")

    val distanceUnit: StateFlow<String> = useImperialUnits.map { imperial ->
        if (imperial) "mi" else "km"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    // Birthday feature
    private val _showBirthdayDialog = MutableStateFlow(false)
    val showBirthdayDialog: StateFlow<Boolean> = _showBirthdayDialog.asStateFlow()

    private val _isBirthday = MutableStateFlow(false)
    val isBirthday: StateFlow<Boolean> = _isBirthday.asStateFlow()

    // Goal Celebration
    private val _showStepGoalCelebration = MutableStateFlow(false)
    val showStepGoalCelebration: StateFlow<Boolean> = _showStepGoalCelebration.asStateFlow()

    private val _hasShownStepCelebrationToday = MutableStateFlow(false)

    // Streak data
    private val _streakData = MutableStateFlow(StreakData())
    val streakData: StateFlow<StreakData> = _streakData.asStateFlow()

    private val _stepsNeededForStreak = MutableStateFlow(0)
    val stepsNeededForStreak: StateFlow<Int> = _stepsNeededForStreak.asStateFlow()

    init {
        loadDashboardData()
        loadWeeklySteps()
        checkBirthday()
        loadStreakData()
        observeStepGoalCompletion()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load calories burned from steps
            combine(currentSteps, userWeightKg) { steps, weight ->
                // Approximate: 0.04 kcal per step per kg of body weight / 70kg baseline
                (steps * 0.04 * weight / 70).toInt()
            }.collect { burned ->
                _caloriesBurned.value = burned
            }
        }

        viewModelScope.launch {
            // Load calories consumed from nutrition repository
            try {
                val todayRange = getTodayRange()
                repository.getMealsForDay(todayRange.first, todayRange.second).collect { meals ->
                    _caloriesConsumed.value = meals.sumOf { it.calories }
                }
            } catch (e: Exception) {
                _caloriesConsumed.value = 0
            }
        }

        viewModelScope.launch {
            // Load workouts completed today
            try {
                val todayRange = getTodayRange()
                repository.getWorkoutsForDay(todayRange.first, todayRange.second).collect { workouts ->
                    _workoutsCompleted.value = workouts.size
                }
            } catch (e: Exception) {
                _workoutsCompleted.value = 0
            }
        }
    }

    private fun loadWeeklySteps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isWeeklyDataLoading.value = true
            try {
                val calendar = Calendar.getInstance()
                
                // Get end of today
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis
                
                // Get start of 7 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                val steps = stepDao.getStepsInRange(startDate, endDate)
                _weeklyStepsFromDb.value = steps
            } catch (e: Exception) {
                _weeklyStepsFromDb.value = emptyList()
            } finally {
                _isWeeklyDataLoading.value = false
            }
        }
    }

    private fun buildWeeklyData(stepEntities: List<StepEntity>, todaySteps: Int): List<Int> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Create a map of day of year to steps
        val stepsMap = mutableMapOf<Int, Int>()
        
        stepEntities.forEach { entity ->
            val entityCalendar = Calendar.getInstance().apply {
                timeInMillis = entity.date
            }
            val dayOfYear = entityCalendar.get(Calendar.DAY_OF_YEAR)
            val year = entityCalendar.get(Calendar.YEAR)
            
            // Only include if same year or handle year boundary
            if (year == currentYear) {
                stepsMap[dayOfYear] = entity.steps
            }
        }
        
        // Override today with live steps
        stepsMap[today] = todaySteps
        
        // Build list for last 7 days (Monday to Sunday or based on current day)
        val weeklyList = mutableListOf<Int>()
        for (i in 6 downTo 0) {
            val targetCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val targetDay = targetCalendar.get(Calendar.DAY_OF_YEAR)
            weeklyList.add(stepsMap[targetDay] ?: 0)
        }
        
        return weeklyList
    }

    private fun checkBirthday() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.userBirthday.collect { birthdayMillis ->
                    if (birthdayMillis > 0) {
                        val birthdayCalendar = Calendar.getInstance().apply {
                            timeInMillis = birthdayMillis
                        }
                        val todayCalendar = Calendar.getInstance()
                        
                        val isBirthdayToday = birthdayCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                                birthdayCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
                        
                        _isBirthday.value = isBirthdayToday
                        
                        // Show dialog only once per session
                        if (isBirthdayToday && !_showBirthdayDialog.value) {
                            val hasShownToday = userPreferencesRepository.hasShownBirthdayDialogToday.first()
                            if (!hasShownToday) {
                                _showBirthdayDialog.value = true
                                userPreferencesRepository.setHasShownBirthdayDialogToday(true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _isBirthday.value = false
            }
        }
    }

    private fun loadStreakData() {
        viewModelScope.launch {
            streakRepository.getStreakData().collect { data ->
                _streakData.value = data
            }
        }

        viewModelScope.launch {
            _stepsNeededForStreak.value = streakRepository.getStepsNeededForStreak()
        }
    }

    private fun observeStepGoalCompletion() {
        viewModelScope.launch {
            combine(currentSteps, dailyStepGoal) { steps, goal ->
                steps >= goal && goal > 0
            }
            .distinctUntilChanged()
            .collect { goalReached ->
                if (goalReached && !_hasShownStepCelebrationToday.value) {
                    _showStepGoalCelebration.value = true
                    _hasShownStepCelebrationToday.value = true
                    // Refresh streak data when goal is reached
                    loadStreakData()
                }
            }
        }
    }

    fun dismissBirthdayDialog() {
        _showBirthdayDialog.value = false
    }

    fun dismissStepGoalCelebration() {
        _showStepGoalCelebration.value = false
    }

    fun refreshStreakData() {
        loadStreakData()
    }

    fun refreshData() {
        loadDashboardData()
        loadWeeklySteps()
        loadStreakData()
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
}
