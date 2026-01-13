 package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitu.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getStepsForDate(date: String): StepEntity?
    
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun getStepsForDateFlow(date: String): Flow<StepEntity?>
    
    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :days")
    fun getRecentSteps(days: Int): Flow<List<StepEntity>>
    
    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getStepsBetweenDates(startDate: String, endDate: String): Flow<List<StepEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stepEntity: StepEntity)
    
    @Query("UPDATE daily_steps SET steps = :steps, lastUpdated = :timestamp WHERE date = :date")
    suspend fun updateSteps(date: String, steps: Int, timestamp: Long)
    
    @Query("DELETE FROM daily_steps")
    suspend fun deleteAll()
} 
