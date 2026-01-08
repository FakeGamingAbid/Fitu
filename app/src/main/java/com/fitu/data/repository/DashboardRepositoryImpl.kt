package com.fitu.data.repository

import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import com.fitu.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val mealDao: MealDao
) : DashboardRepository {

    override fun getWorkoutsForDay(startTime: Long, endTime: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsForDay(startTime, endTime)
    }

    override fun getCaloriesBurnedForDay(startTime: Long, endTime: Long): Flow<Int?> {
        return workoutDao.getCaloriesBurnedForDay(startTime, endTime)
    }

    override fun getMealsForDay(startTime: Long, endTime: Long): Flow<List<MealEntity>> {
        return mealDao.getMealsForDay(startTime, endTime)
    }

    override fun getCaloriesConsumedForDay(startTime: Long, endTime: Long): Flow<Int?> {
        return mealDao.getCaloriesConsumedForDay(startTime, endTime)
    }

    override suspend fun insertWorkout(workout: WorkoutEntity) {
        workoutDao.insertWorkout(workout)
    }

    override suspend fun insertMeal(meal: MealEntity) {
        mealDao.insertMeal(meal)
    }
}
