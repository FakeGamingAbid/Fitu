package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    indices = [Index(value = ["date"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val calories: Int,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fats: Float = 0f,  // Alternative field name for compatibility
    val fiber: Float = 0f,
    val mealType: String = "snack",
    val imageUri: String? = null,
    val photoUri: String? = null,  // Alternative field name for compatibility
    val portion: String? = null,
    val date: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
