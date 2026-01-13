 package com.fitu.ui.steps

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.service.StepCounterService
import com.fitu.util.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StepsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepDao: StepDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val stepCount: StateFlow<Int> = StepCounterService.stepCount
    val motionMagnitude: StateFlow<Float> = StepCounterService.motionMagnitude

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    // User's physical stats for personalized calculations
    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // ✅ FIX #24: Unit preference
    val useImperialUnits: StateFlow<Boolean> = userPreferencesRepository.useImperialUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Progress (0.0 to 1.0)
    val progress: StateFlow<Float> = combine(stepCount, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * ✅ FIX #7: Personalized stride length based on user's height
     */
    val distanceKm: StateFlow<Float> = combine(stepCount, userHeightCm) { steps, heightCm ->
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
    val distanceUnit: StateFlow<String> = useImperialUnits.combine(stepCount) { useImperial, _ ->
        UnitConverter.getDistanceUnit(useImperial)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KM")

    /**
     * ✅ FIX #6: Personalized calorie calculation based on user's weight
     */
    val caloriesBurned: StateFlow<Int> = combine(stepCount, userWeightKg) { steps, weightKg ->
        val caloriesPerStep = calculateCaloriesPerStep(weightKg)
        (steps * caloriesPerStep).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ✅ FIX #11: Loading state for weekly steps chart
    private val _isWeeklyDataLoading = MutableStateFlow(true)
    val isWeeklyDataLoading: StateFlow<Boolean> = _isWeeklyDataLoading

    // Weekly steps data - Raw data from database
    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())
    
    // Weekly steps - Combines DB data with live step count for reactive updates
    val weeklySteps: StateFlow<List<DaySteps>> = combine(
        _weeklyStepsFromDb,
        stepCount
    ) { stepEntities, todaySteps ->
        buildWeeklyDaySteps(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startService()
        loadWeeklySteps()
        
        // Pass step goal to service when it changes
        viewModelScope.launch {
            userPreferencesRepository.dailyStepGoal.collect { goal ->
                updateServiceStepGoal(goal)
            }
        }
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

    fun startService() {
        viewModelScope.launch {
            val goal = userPreferencesRepository.dailyStepGoal.first()
            
            val intent = Intent(context, StepCounterService::class.java).apply {
                putExtra("step_goal", goal)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    /**
     * Update step goal in the service
     */
    private fun updateServiceStepGoal(goal: Int) {
        val prefs = context.getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("daily_step_goal", goal).apply()
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
            
            stepDao.getStepsBetweenDates(startDate, endDate).collect { stepEntities ->
                _weeklyStepsFromDb.value = stepEntities
                // ✅ FIX #11: Set loading to false after data is loaded
                _isWeeklyDataLoading.value = false
            }
        }
    }

    /**
     * Build weekly DaySteps list combining DB data with live today's steps.
     */
    private fun buildWeeklyDaySteps(stepEntities: List<StepEntity>, todaySteps: Int): List<DaySteps> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        val stepMap = stepEntities.associateBy { it.date }
        val todayDate = StepCounterService.getTodayDate()
        
        val weekData = mutableListOf<DaySteps>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val date = dateFormat.format(cal.time)
            val dayName = dayNameFormat.format(cal.time)
            val isToday = date == todayDate
            
            val steps = when {
                isToday -> todaySteps
                else -> stepMap[date]?.steps ?: 0
            }
            
            weekData.add(DaySteps(
                day = dayName,
                date = date,
                steps = steps,
                isToday = isToday
            ))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return weekData
    }
}

data class DaySteps(
    val day: String,
    val date: String,
    val steps: Int,
    val isToday: Boolean
) 
