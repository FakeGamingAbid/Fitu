package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fitu.data.local.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Insert
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getPlanById(id: Int): WorkoutPlanEntity?

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlan(id: Int)
}
