package com.fitu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.local.entity.WorkoutEntity

@Database(
    entities = [
        StepEntity::class,
        MealEntity::class,
        WorkoutEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
}
