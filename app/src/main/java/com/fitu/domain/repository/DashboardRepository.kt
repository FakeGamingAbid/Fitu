package com.fitu.domain.repository

import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>>
    fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutEntity>>
}
