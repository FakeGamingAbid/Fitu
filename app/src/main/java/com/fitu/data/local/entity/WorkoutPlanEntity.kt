package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroups: String, // Comma-separated: "Chest,Shoulders"
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val duration: Int, // in minutes
    val equipment: String, // "Bodyweight", "Dumbbells", "Full Gym"
    val exercises: String, // JSON string of exercises
    val createdAt: Long
)
