package com.fitu.domain.repository

import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val workoutDao: WorkoutDao,
    private val stepDao: StepDao
) : DashboardRepository {

    override fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>> {
        return mealDao.getMealsInRange(startOfDay, endOfDay)
    }

    override fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsInRange(startOfDay, endOfDay)
    }
}
