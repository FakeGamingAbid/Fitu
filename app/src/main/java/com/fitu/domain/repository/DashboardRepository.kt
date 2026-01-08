package com.fitu.domain.repository

import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getWorkoutsForDay(startTime: Long, endTime: Long): Flow<List<WorkoutEntity>>
    fun getCaloriesBurnedForDay(startTime: Long, endTime: Long): Flow<Int?>
    fun getMealsForDay(startTime: Long, endTime: Long): Flow<List<MealEntity>>
    fun getCaloriesConsumedForDay(startTime: Long, endTime: Long): Flow<Int?>
    suspend fun insertWorkout(workout: WorkoutEntity)
    suspend fun insertMeal(meal: MealEntity)
}
