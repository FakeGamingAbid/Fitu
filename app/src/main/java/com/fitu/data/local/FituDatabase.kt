package com.fitu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.dao.WorkoutPlanDao
import com.fitu.data.local.dao.FoodCacheDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.local.entity.WorkoutEntity
import com.fitu.data.local.entity.WorkoutPlanEntity
import com.fitu.data.local.entity.FoodCacheEntity

@Database(
    entities = [
        WorkoutEntity::class,
        MealEntity::class,
        WorkoutPlanEntity::class,
        FoodCacheEntity::class,
        StepEntity::class
    ],
    version = 8,  // ✅ Incremented for durationMs column in workouts
    exportSchema = false
)
abstract class FituDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun mealDao(): MealDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun foodCacheDao(): FoodCacheDao
    abstract fun stepDao(): StepDao
}
