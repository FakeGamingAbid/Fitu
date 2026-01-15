package com.fitu.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.WorkoutEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the AI Coach screen.
 * Manages exercise selection, rep count, timer state, and calorie tracking.
 * Saves completed workouts to the database.
 */
@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    // User's weight for calorie calculation
    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // Selected exercise type
    private val _selectedExercise = MutableStateFlow(ExerciseType.PUSH_UP)
    val selectedExercise: StateFlow<ExerciseType> = _selectedExercise.asStateFlow()

    // Rep count for rep-based exercises
    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    // Hold time for time-based exercises (milliseconds)
    private val _holdTimeMs = MutableStateFlow(0L)
    val holdTimeMs: StateFlow<Long> = _holdTimeMs.asStateFlow()

    // Best hold time (milliseconds)
    private val _bestHoldTimeMs = MutableStateFlow(0L)
    val bestHoldTimeMs: StateFlow<Long> = _bestHoldTimeMs.asStateFlow()

    // Form score (0-10) for time-based exercises
    private val _formScore = MutableStateFlow(0f)
    val formScore: StateFlow<Float> = _formScore.asStateFlow()

    // Current angle being measured
    private val _currentAngle = MutableStateFlow(0f)
    val currentAngle: StateFlow<Float> = _currentAngle.asStateFlow()

    // Feedback message
    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    // Camera permission state
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // Is workout active
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    // Track if workout was saved (to show feedback)
    private val _workoutSaved = MutableStateFlow(false)
    val workoutSaved: StateFlow<Boolean> = _workoutSaved.asStateFlow()

    /**
     * Calories burned - calculated based on exercise type, reps/time, and user weight
     */
    val caloriesBurned: StateFlow<Float> = combine(
        _selectedExercise,
        _repCount,
        _holdTimeMs,
        userWeightKg
    ) { exercise, reps, holdTime, weight ->
        if (exercise.isTimeBased) {
            ExerciseType.calculateCaloriesFromTime(exercise, holdTime, weight)
        } else {
            ExerciseType.calculateCaloriesFromReps(exercise, reps, weight)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /**
     * Select an exercise to track
     */
    fun selectExercise(type: ExerciseType) {
        _selectedExercise.value = type
        resetStats()
    }

    /**
     * Update rep count (called from PoseAnalyzer)
     */
    fun updateRepCount(count: Int) {
        _repCount.value = count
    }

    /**
     * Update hold time (called from PoseAnalyzer for time-based exercises)
     */
    fun updateHoldTime(currentMs: Long, bestMs: Long) {
        _holdTimeMs.value = currentMs
        if (bestMs > _bestHoldTimeMs.value) {
            _bestHoldTimeMs.value = bestMs
        }
    }

    /**
     * Update form score
     */
    fun updateFormScore(score: Float) {
        _formScore.value = score
    }

    /**
     * Update current angle
     */
    fun updateAngle(angle: Float) {
        _currentAngle.value = angle
    }

    /**
     * Update feedback message
     */
    fun updateFeedback(message: String) {
        _feedback.value = message
    }

    /**
     * Set camera permission state
     */
    fun setCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    /**
     * Start workout session
     */
    fun startWorkout() {
        _isWorkoutActive.value = true
        _workoutSaved.value = false
        resetStats()
    }

    /**
     * Stop workout session and save to database
     */
    fun stopWorkout() {
        _isWorkoutActive.value = false
        saveWorkoutToDatabase()
    }

    /**
     * Save the current workout to the database
     */
    private fun saveWorkoutToDatabase() {
        val exercise = _selectedExercise.value
        val reps = _repCount.value
        val holdTime = _holdTimeMs.value
        val calories = caloriesBurned.value.toInt()

        // Only save if there's actual activity
        if (reps > 0 || holdTime > 0) {
            viewModelScope.launch {
                try {
                    val workout = WorkoutEntity(
                        type = exercise.displayName,
                        reps = reps,
                        durationMs = holdTime,
                        timestamp = System.currentTimeMillis(),
                        caloriesBurned = calories
                    )
                    workoutDao.insertWorkout(workout)
                    _workoutSaved.value = true
                } catch (e: Exception) {
                    android.util.Log.e("AiCoachViewModel", "Failed to save workout", e)
                    _workoutSaved.value = false
                }
            }
        }
    }

    /**
     * Manually save current workout (e.g., when user leaves screen)
     */
    fun saveCurrentWorkout() {
        val reps = _repCount.value
        val holdTime = _holdTimeMs.value
        
        if (reps > 0 || holdTime > 0) {
            saveWorkoutToDatabase()
        }
    }

    /**
     * Reset all stats
     */
    fun resetStats() {
        _repCount.value = 0
        _holdTimeMs.value = 0L
        _formScore.value = 0f
        _currentAngle.value = 0f
        _feedback.value = ""
        _workoutSaved.value = false
    }

    /**
     * Format hold time as MM:SS
     */
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
