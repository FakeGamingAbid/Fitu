package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val exercises: String = "", // JSON string of exercises
    val difficulty: String = "intermediate",
    val durationMinutes: Int = 30,
    val targetMuscles: String = "", // Comma-separated list
    val createdAt: Long = System.currentTimeMillis()
)
