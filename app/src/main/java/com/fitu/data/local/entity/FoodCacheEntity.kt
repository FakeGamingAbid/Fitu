package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_cache")
data class FoodCacheEntity(
    @PrimaryKey val query: String, // Text query or image hash
    val resultJson: String,
    val timestamp: Long
)
