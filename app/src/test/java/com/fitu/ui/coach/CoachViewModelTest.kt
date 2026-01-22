package com.fitu.ui.coach

import com.fitu.aicoach.AiCoachViewModel
import com.fitu.aicoach.ExerciseType
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    @Mock
    private lateinit var workoutDao: WorkoutDao
    
    @Mock
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var viewModel: AiCoachViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock the user preferences repository to return a default weight
        Mockito.`when`(userPreferencesRepository.userWeightKg).thenReturn(flowOf(70))
        
        viewModel = AiCoachViewModel(userPreferencesRepository, workoutDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startWorkout sets workout to active and resets stats`() = runTest {
        viewModel.startWorkout()
        assertTrue(viewModel.isWorkoutActive.value)
        assertFalse(viewModel.workoutSaved.value)
        assertEquals(0, viewModel.repCount.value)
    }

    @Test
    fun `stopWorkout saves workout if there are reps or time`() = runTest {
        viewModel.selectExercise(ExerciseType.PUSH_UP)
        viewModel.startWorkout()
        
        // Simulate some reps
        viewModel.updateRepCount(10)
        
        viewModel.stopWorkout()
        
        assertFalse(viewModel.isWorkoutActive.value)
        
        // Advance dispatcher to execute the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        Mockito.verify(workoutDao).insertWorkout(Mockito.any(WorkoutEntity::class.java))
    }
    
    @Test
    fun `selectExercise updates selected exercise and resets stats`() = runTest {
        viewModel.updateRepCount(5)
        viewModel.selectExercise(ExerciseType.SQUAT)
        
        assertEquals(ExerciseType.SQUAT, viewModel.selectedExercise.value)
        assertEquals(0, viewModel.repCount.value)
    }
    
    @Test
    fun `updateRepCount updates the rep count`() = runTest {
        viewModel.updateRepCount(10)
        assertEquals(10, viewModel.repCount.value)
    }
    
    @Test
    fun `updateHoldTime updates current and best hold times`() = runTest {
        viewModel.updateHoldTime(5000L, 5000L)
        assertEquals(5000L, viewModel.holdTimeMs.value)
        assertEquals(5000L, viewModel.bestHoldTimeMs.value)
        
        // Update with higher best time
        viewModel.updateHoldTime(3000L, 7000L)
        assertEquals(3000L, viewModel.holdTimeMs.value)
        assertEquals(7000L, viewModel.bestHoldTimeMs.value)
    }
    
    @Test
    fun `resetStats clears all stats`() = runTest {
        viewModel.updateRepCount(10)
        viewModel.updateHoldTime(5000L, 5000L)
        viewModel.updateFormScore(8.5f)
        
        viewModel.resetStats()
        
        assertEquals(0, viewModel.repCount.value)
        assertEquals(0L, viewModel.holdTimeMs.value)
        assertEquals(0f, viewModel.formScore.value, 0.01f)
        assertFalse(viewModel.workoutSaved.value)
    }
    
    @Test
    fun `saveCurrentWorkout saves when there are reps`() = runTest {
        viewModel.updateRepCount(5)
        viewModel.saveCurrentWorkout()
        
        // Advance dispatcher to execute the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        Mockito.verify(workoutDao).insertWorkout(Mockito.any(WorkoutEntity::class.java))
    }
    
    @Test
    fun `saveCurrentWorkout does not save when no progress`() = runTest {
        viewModel.saveCurrentWorkout()
        
        // Advance dispatcher to execute the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        Mockito.verify(workoutDao, Mockito.never()).insertWorkout(Mockito.any(WorkoutEntity::class.java))
    }
}
