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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // ✅ User's physical stats for personalized calculations
    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // ✅ FIX #24: Unit preference
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

    // ✅ FIX #11: Loading state for weekly steps chart
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
    private val _workoutsCompleted = MutableStateFlow(0)
    val workoutsCompleted: StateFlow<Int> = _workoutsCompleted

    // Birthday feature
    private val _showBirthdayDialog = MutableStateFlow(false)
    val showBirthdayDialog: StateFlow<Boolean> = _showBirthdayDialog

    private val _isBirthday = MutableStateFlow(false)
    val isBirthday: StateFlow<Boolean> = _isBirthday

    // Helper to get start and end of today
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
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    /**
     * ✅ FIX #6: Personalized calories burned from steps
     * Uses user's weight for accurate calculation
     */
    val caloriesBurnedFromSteps: StateFlow<Int> = combine(currentSteps, userWeightKg) { steps, weightKg ->
        val caloriesPerStep = calculateCaloriesPerStep(weightKg)
        (steps * caloriesPerStep).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Calories burned from workouts (from database)
    private val caloriesBurnedFromWorkouts: StateFlow<Int> = repository.getCaloriesBurnedForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { burned, _ -> burned ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total calories burned (steps + workouts)
    val caloriesBurned: StateFlow<Int> = combine(
        caloriesBurnedFromSteps,
        caloriesBurnedFromWorkouts
    ) { fromSteps, fromWorkouts ->
        fromSteps + fromWorkouts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val caloriesConsumed: StateFlow<Int> = repository.getCaloriesConsumedForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { consumed, _ -> consumed ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val netCalories: StateFlow<Int> = combine(caloriesConsumed, caloriesBurned) { consumed, burned ->
        consumed - burned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Step progress percentage
    val stepProgress: StateFlow<Float> = combine(currentSteps, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat() * 100).coerceIn(0f, 100f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * ✅ FIX #7: Personalized distance calculation
     * Uses user's height for accurate stride length
     */
    val distanceKm: StateFlow<Float> = combine(currentSteps, userHeightCm) { steps, heightCm ->
        val strideLengthKm = calculateStrideLengthKm(heightCm)
        steps * strideLengthKm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * ✅ FIX #24: Formatted distance based on unit preference
     */
    val formattedDistance: StateFlow<String> = combine(distanceKm, useImperialUnits) { km, useImperial ->
        if (useImperial) {
            val miles = UnitConverter.kmToMiles(km)
            String.format("%.2f", miles)
        } else {
            String.format("%.2f", km)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.00")

    /**
     * ✅ FIX #24: Distance unit label
     */
    val distanceUnit: StateFlow<String> = useImperialUnits.combine(currentSteps) { useImperial, _ ->
        UnitConverter.getDistanceUnit(useImperial)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KM")

    init {
        loadWeeklySteps()
        checkBirthday()
    }

    /**
     * Calculate stride length in kilometers based on height
     */
    private fun calculateStrideLengthKm(heightCm: Int): Float {
        val strideMultiplier = 0.415f
        val strideLengthCm = heightCm * strideMultiplier
        return strideLengthCm / 100_000f
    }

    /**
     * Calculate calories burned per step based on weight
     */
    private fun calculateCaloriesPerStep(weightKg: Int): Float {
        return weightKg * 0.00057f
    }

    private fun loadWeeklySteps() {
        viewModelScope.launch {
            // ✅ FIX #11: Set loading to true before fetching
            _isWeeklyDataLoading.value = true
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)
            
            // Collect database data only - processing happens in combine
            stepDao.getStepsBetweenDates(startDate, endDate).collect { stepEntities ->
                _weeklyStepsFromDb.value = stepEntities
                // ✅ FIX #11: Set loading to false after data is loaded
                _isWeeklyDataLoading.value = false
            }
        }
    }

    /**
     * Build weekly data array combining DB data with live today's steps.
     */
    private fun buildWeeklyData(stepEntities: List<StepEntity>, todaySteps: Int): List<Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val stepMap = stepEntities.associateBy { it.date }
        val todayDate = StepCounterService.getTodayDate()
        
        val weekData = mutableListOf<Int>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val date = dateFormat.format(cal.time)
            val steps = when {
                date == todayDate -> todaySteps
                else -> stepMap[date]?.steps ?: 0
            }
            weekData.add(steps)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return weekData
    }

    /**
     * Check if today is the user's birthday
     */
    private fun checkBirthday() {
        viewModelScope.launch {
            val birthDay = userPreferencesRepository.birthDay.first()
            val birthMonth = userPreferencesRepository.birthMonth.first()
            val birthYear = userPreferencesRepository.birthYear.first()
            val lastWishYear = userPreferencesRepository.lastBirthdayWishYear.first()

            val currentYear = LocalDate.now().year
            val isBirthdayToday = BirthdayUtils.isBirthdayWithinGracePeriod(
                birthDay, birthMonth, birthYear, graceDays = 3
            )

            _isBirthday.value = BirthdayUtils.isBirthday(birthDay, birthMonth)

            if (isBirthdayToday && lastWishYear != currentYear) {
                _showBirthdayDialog.value = true
            }
        }
    }

    /**
     * Dismiss the birthday dialog
     */
    fun dismissBirthdayDialog() {
        viewModelScope.launch {
            val currentYear = LocalDate.now().year
            userPreferencesRepository.setLastBirthdayWishYear(currentYear)
            _showBirthdayDialog.value = false
        }
    }

    fun generateDailyRecap() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val model = geminiModelProvider.getModel()
                if (model == null) {
                    _dailyRecap.value = "You're doing great! Keep it up."
                    return@launch
                }

                val prompt = """
                    Generate a short, encouraging daily fitness recap (2-3 sentences max).
                    User stats: ${currentSteps.value} steps today, goal: ${dailyStepGoal.value}.
                    Calories consumed: ${caloriesConsumed.value}, burned: ${caloriesBurned.value}.
                    Be motivating and concise.
                """.trimIndent()

                val response = model.generateContent(content { text(prompt) })
                _dailyRecap.value = response.text ?: "You're doing great! Keep it up."
            } catch (e: Exception) {
                _dailyRecap.value = "You're doing great! Keep it up."
            }
        }
    }
} 
