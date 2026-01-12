package com.fitu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.service.StepCounterService
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.fitu.util.BirthdayUtils
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

    // Steps - Read directly from StepCounterService (real data)
    val currentSteps: StateFlow<Int> = StepCounterService.stepCount

    // Weekly progress (for display purposes - today's steps are real, past days would need persistence)
    private val _weeklySteps = MutableStateFlow<List<Int>>(listOf(0, 0, 0, 0, 0, 0, 0))
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps

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

    val caloriesBurned: StateFlow<Int> = repository.getCaloriesBurnedForDay(getTodayRange().first, getTodayRange().second)
        .combine(MutableStateFlow(0)) { burned, _ -> burned ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val caloriesConsumed: StateFlow<Int> = repository.getCaloriesConsumedForDay(getTodayRange().first, getTodayRange().second)
        .combine(MutableStateFlow(0)) { consumed, _ -> consumed ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val netCalories: StateFlow<Int> = combine(caloriesConsumed, caloriesBurned) { consumed, burned ->
        consumed - burned
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Step progress percentage
    val stepProgress: StateFlow<Float> = combine(currentSteps, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat() * 100).coerceIn(0f, 100f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        // Update weekly steps with today's real steps when they change
        viewModelScope.launch {
            currentSteps.collect { todaySteps ->
                // Put today's steps at the end of the week (index 6 = today)
                val updatedWeekly = _weeklySteps.value.toMutableList()
                updatedWeekly[6] = todaySteps
                _weeklySteps.value = updatedWeekly
            }
        }

        // Check for birthday on init
        checkBirthday()
    }

    /**
     * Check if today is the user's birthday and show the wish dialog if:
     * 1. It's the user's birthday (or within grace period)
     * 2. User hasn't been wished this year
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

            // Show dialog if it's birthday and user hasn't been wished this year
            if (isBirthdayToday && lastWishYear != currentYear) {
                _showBirthdayDialog.value = true
            }
        }
    }

    /**
     * Dismiss the birthday dialog and mark as wished for this year.
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
