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
import kotlinx.coroutines.flow.flowOf
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
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val stepCount: StateFlow<Int> = StepCounterService.stepCount
    val motionMagnitude: StateFlow<Float> = StepCounterService.motionMagnitude

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    // Progress (0.0 to 1.0)
    val progress: StateFlow<Float> = combine(stepCount, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Distance in km (average stride length ~0.762m = 0.000762km)
    val distanceKm: StateFlow<Float> = stepCount.combine(flowOf(0.000762f)) { steps, strideKm ->
        steps * strideKm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Calories burned (approx 0.04 kcal per step for average person)
    val caloriesBurned: StateFlow<Int> = stepCount.combine(flowOf(0.04f)) { steps, calPerStep ->
        (steps * calPerStep).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Weekly steps data - Raw data from database
    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())
    
    // Weekly steps - FIXED: Combines DB data with live step count for reactive updates
    val weeklySteps: StateFlow<List<DaySteps>> = combine(
        _weeklyStepsFromDb,
        stepCount
    ) { stepEntities, todaySteps ->
        buildWeeklyDaySteps(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startService()
        loadWeeklySteps()
    }

    fun startService() {
        val intent = Intent(context, StepCounterService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun loadWeeklySteps() {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)
            
            // Collect database data only - processing happens in combine
            stepDao.getStepsBetweenDates(startDate, endDate).collect { stepEntities ->
                _weeklyStepsFromDb.value = stepEntities
            }
        }
    }

    /**
     * Build weekly DaySteps list combining DB data with live today's steps.
     * This ensures the chart always shows the current step count for today.
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
                isToday -> todaySteps  // Always use live value for today
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
