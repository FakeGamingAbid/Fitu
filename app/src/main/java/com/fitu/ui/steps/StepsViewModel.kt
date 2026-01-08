package com.fitu.ui.steps

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.service.StepCounterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StepsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    val distanceKm: StateFlow<Float> = stepCount.combine(kotlinx.coroutines.flow.flowOf(0.000762f)) { steps, strideKm ->
        steps * strideKm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Calories burned (approx 0.04 kcal per step for average person)
    val caloriesBurned: StateFlow<Int> = stepCount.combine(kotlinx.coroutines.flow.flowOf(0.04f)) { steps, calPerStep ->
        (steps * calPerStep).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        startService()
    }

    private fun startService() {
        val intent = Intent(context, StepCounterService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
