package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // e.g., "Squat", "Pushup"
    val reps: Int,
    val timestamp: Long,
    val caloriesBurned: Int
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val timestamp: Long,
    val mealType: String = "snacks", // "breakfast", "lunch", "dinner", "snacks"
    val portion: Float = 1f,
    // ✅ NEW: Store photo URI or file path (null if added via text search)
    val photoUri: String? = null
)
