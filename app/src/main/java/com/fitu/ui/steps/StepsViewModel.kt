 package com.fitu.ui.steps

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.service.StepCounterService
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

    // Progress (0.0 to 1.0)
    val progress: StateFlow<Float> = combine(stepCount, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * ✅ FIX #7: Personalized stride length based on user's height
     * 
     * Formula: Stride length ≈ Height × 0.415 (for walking)
     * Source: Scientific studies on gait analysis
     * 
     * Examples:
     * - Height 150cm → Stride 62.25cm → 0.0006225 km
     * - Height 170cm → Stride 70.55cm → 0.0007055 km
     * - Height 190cm → Stride 78.85cm → 0.0007885 km
     */
    val distanceKm: StateFlow<Float> = combine(stepCount, userHeightCm) { steps, heightCm ->
        val strideLengthKm = calculateStrideLengthKm(heightCm)
        steps * strideLengthKm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * ✅ FIX #6: Personalized calorie calculation based on user's weight
     * 
     * Formula: Calories per step ≈ 0.04 × (weight / 70)
     * This scales the base rate (0.04 kcal for 70kg person) proportionally
     * 
     * More accurate formula considers MET (Metabolic Equivalent):
     * Calories = MET × weight(kg) × time(hours)
     * For walking: MET ≈ 3.5
     * 
     * Simplified per-step formula:
     * Calories per step = (weight × 0.0005)
     * 
     * Examples:
     * - Weight 50kg → 0.025 kcal/step
     * - Weight 70kg → 0.035 kcal/step
     * - Weight 90kg → 0.045 kcal/step
     * - Weight 100kg → 0.050 kcal/step
     */
    val caloriesBurned: StateFlow<Int> = combine(stepCount, userWeightKg) { steps, weightKg ->
        val caloriesPerStep = calculateCaloriesPerStep(weightKg)
        (steps * caloriesPerStep).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
     * 
     * @param heightCm User's height in centimeters
     * @return Stride length in kilometers
     */
    private fun calculateStrideLengthKm(heightCm: Int): Float {
        // Stride length = height × 0.415 (walking average)
        // Convert cm to km: divide by 100,000
        val strideMultiplier = 0.415f
        val strideLengthCm = heightCm * strideMultiplier
        return strideLengthCm / 100_000f
    }

    /**
     * Calculate calories burned per step based on weight
     * 
     * @param weightKg User's weight in kilograms
     * @return Calories burned per step
     */
    private fun calculateCaloriesPerStep(weightKg: Int): Float {
        // Base: 0.04 kcal per step for 70kg person
        // Scale proportionally: weight × 0.00057
        // This gives ~0.04 for 70kg
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)
            
            stepDao.getStepsBetweenDates(startDate, endDate).collect { stepEntities ->
                _weeklyStepsFromDb.value = stepEntities
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
