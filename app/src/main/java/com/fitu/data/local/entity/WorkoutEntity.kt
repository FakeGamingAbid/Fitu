package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["date"])]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseType: String,
    val reps: Int = 0,
    val sets: Int = 0,
    val durationSeconds: Int = 0,
    val caloriesBurned: Int = 0,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
