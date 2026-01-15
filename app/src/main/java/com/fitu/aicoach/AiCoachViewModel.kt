package com.fitu.aicoach

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the AI Coach screen.
 * Manages exercise selection, rep count, and timer state.
 */
@HiltViewModel
class AiCoachViewModel @Inject constructor() : ViewModel() {

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
        resetStats()
    }

    /**
     * Stop workout session
     */
    fun stopWorkout() {
        _isWorkoutActive.value = false
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
