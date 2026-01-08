package com.fitu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geminiModelProvider: GeminiModelProvider
) : ViewModel() {

    // User info
    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // Today's date formatted
    val todayDate: String = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    // Daily recap
    private val _dailyRecap = MutableStateFlow<String>("Loading daily recap...")
    val dailyRecap: StateFlow<String> = _dailyRecap

    // Steps
    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    // Weekly progress (last 7 days steps data)
    private val _weeklySteps = MutableStateFlow<List<Int>>(listOf(0, 0, 0, 0, 0, 0, 0))
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps

    // Workout summary
    private val _workoutsCompleted = MutableStateFlow(0)
    val workoutsCompleted: StateFlow<Int> = _workoutsCompleted

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

    val caloriesBurned: StateFlow<Int> = repository.getCaloriesBurnedForDay(getTodayRange().first, getTodayRange().second)
        .combine(MutableStateFlow(0)) { burned, _ -> burned ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val caloriesConsumed: StateFlow<Int> = repository.getCaloriesConsumedForDay(getTodayRange().first, getTodayRange().second)
        .combine(MutableStateFlow(0)) { consumed, _ -> consumed ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val netCalories: StateFlow<Int> = combine(caloriesConsumed, caloriesBurned) { consumed, burned ->
        consumed - burned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadWeeklyProgress()
    }

    private fun loadWeeklyProgress() {
        viewModelScope.launch {
            // For now, use mock data - in production this would come from step tracking service
            // Days: Mon, Tue, Wed, Thu, Fri, Sat, Sun (relative to today)
            _weeklySteps.value = listOf(8234, 6542, 10231, 7845, 9123, 5432, 0)
            _currentSteps.value = 6542 // Today's steps
            _workoutsCompleted.value = 3
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
                    User stats: ${_currentSteps.value} steps today, goal: ${dailyStepGoal.value}.
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
