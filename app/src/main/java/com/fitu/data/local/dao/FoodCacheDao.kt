package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitu.data.local.entity.FoodCacheEntity

@Dao
interface FoodCacheDao {
    @Query("SELECT * FROM food_cache WHERE query = :query")
    suspend fun getCache(query: String): FoodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: FoodCacheEntity)

    @Query("DELETE FROM food_cache WHERE timestamp < :timestamp")
    suspend fun clearOldCache(timestamp: Long)
}
