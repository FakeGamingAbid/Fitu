package com.fitu.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.WorkoutEntity
import com.google.mlkit.vision.pose.Pose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WorkoutState {
    IDLE,
    ACTIVE,
    PAUSED,
    COMPLETED
}

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount

    private val _currentExercise = MutableStateFlow("Squat")
    val currentExercise: StateFlow<String> = _currentExercise

    private val _currentPose = MutableStateFlow<Pose?>(null)
    val currentPose: StateFlow<Pose?> = _currentPose

    private val _formFeedback = MutableStateFlow(FormFeedback(true, "Get ready..."))
    val formFeedback: StateFlow<FormFeedback> = _formFeedback

    private val _incorrectLandmarks = MutableStateFlow<Set<Int>>(emptySet())
    val incorrectLandmarks: StateFlow<Set<Int>> = _incorrectLandmarks

    private val _workoutState = MutableStateFlow(WorkoutState.IDLE)
    val workoutState: StateFlow<WorkoutState> = _workoutState

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen

    private var timerJob: Job? = null

    val availableExercises = listOf("Squat", "Push-up", "Sit-up", "Bicep Curl")

    // Store analyzer reference for control
    private var poseAnalyzer: PoseAnalyzer? = null

    fun setAnalyzer(analyzer: PoseAnalyzer) {
        poseAnalyzer = analyzer
    }

    fun selectExercise(exercise: String) {
        _currentExercise.value = exercise
        poseAnalyzer?.setExercise(exercise)
        resetWorkout()
    }

    fun startWorkout() {
        _workoutState.value = WorkoutState.ACTIVE
        poseAnalyzer?.setWorkoutActive(true)
        startTimer()
    }

    fun pauseWorkout() {
        _workoutState.value = WorkoutState.PAUSED
        poseAnalyzer?.setWorkoutActive(false)
        timerJob?.cancel()
    }

    fun resumeWorkout() {
        _workoutState.value = WorkoutState.ACTIVE
        poseAnalyzer?.setWorkoutActive(true)
        startTimer()
    }

    fun stopWorkout() {
        if (_workoutState.value == WorkoutState.ACTIVE || _workoutState.value == WorkoutState.PAUSED) {
            saveWorkout()
        }
        _workoutState.value = WorkoutState.COMPLETED
        poseAnalyzer?.setWorkoutActive(false)
        timerJob?.cancel()
    }

    private fun saveWorkout() {
        val reps = _repCount.value
        if (reps > 0) {
            viewModelScope.launch {
                val workout = WorkoutEntity(
                    type = _currentExercise.value,
                    reps = reps,
                    timestamp = System.currentTimeMillis(),
                    caloriesBurned = calculateCalories(reps, _currentExercise.value)
                )
                workoutDao.insertWorkout(workout)
            }
        }
    }

    private fun calculateCalories(reps: Int, exercise: String): Int {
        // Rough estimation
        val caloriesPerRep = when (exercise) {
            "Squat" -> 0.5
            "Push-up" -> 0.6
            "Sit-up" -> 0.3
            "Bicep Curl" -> 0.2
            else -> 0.4
        }
        return (reps * caloriesPerRep).toInt()
    }

    fun resetWorkout() {
        _workoutState.value = WorkoutState.IDLE
        _repCount.value = 0
        _elapsedSeconds.value = 0
        _formFeedback.value = FormFeedback(true, "Get ready...")
        _incorrectLandmarks.value = emptySet()
        poseAnalyzer?.resetCounts()
        poseAnalyzer?.setWorkoutActive(false)
        timerJob?.cancel()
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun updatePose(pose: Pose) {
        _currentPose.value = pose
    }

    fun updateExerciseResult(result: ExerciseResult) {
        _currentExercise.value = result.exercise
        _repCount.value = result.repCount
        _formFeedback.value = result.formFeedback
        _incorrectLandmarks.value = result.formFeedback.incorrectLandmarks
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_workoutState.value == WorkoutState.ACTIVE) {
                    _elapsedSeconds.value++
                }
            }
        }
    }

    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
