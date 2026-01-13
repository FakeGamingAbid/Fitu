 package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class StepEntity(
    @PrimaryKey
    val date: String, // Format: "yyyy-MM-dd"
    val steps: Int,
    val lastUpdated: Long // Timestamp
) 
