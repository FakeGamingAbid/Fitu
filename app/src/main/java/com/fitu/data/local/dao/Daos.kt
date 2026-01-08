package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getWorkoutsForDay(startTime: Long, endTime: Long): Flow<List<WorkoutEntity>>

    @Query("SELECT SUM(caloriesBurned) FROM workouts WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getCaloriesBurnedForDay(startTime: Long, endTime: Long): Flow<Int?>
}

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getMealsForDay(startTime: Long, endTime: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime AND mealType = :mealType")
    fun getMealsByType(startTime: Long, endTime: Long, mealType: String): Flow<List<MealEntity>>

    @Query("SELECT SUM(calories) FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getCaloriesConsumedForDay(startTime: Long, endTime: Long): Flow<Int?>

    @Query("SELECT SUM(protein) FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getProteinForDay(startTime: Long, endTime: Long): Flow<Int?>

    @Query("SELECT SUM(carbs) FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getCarbsForDay(startTime: Long, endTime: Long): Flow<Int?>

    @Query("SELECT SUM(fats) FROM meals WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getFatsForDay(startTime: Long, endTime: Long): Flow<Int?>

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMeal(mealId: Int)
}
