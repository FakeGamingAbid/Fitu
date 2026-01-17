package com.fitu.data.repository

import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStreakDate: LocalDate? = null,
    val streakHistory: List<LocalDate> = emptyList()
)

@Singleton
class StreakRepository @Inject constructor(
    private val stepDao: StepDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    /**
     * Calculate streak data based on step history and daily goal
     */
    fun getStreakData(): Flow<StreakData> = flow {
        val dailyGoal = userPreferencesRepository.dailyStepGoal.first()
        
        // Get last 365 days of step data
        val today = LocalDate.now()
        val startDate = today.minusDays(365)
        
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val stepRecords = stepDao.getStepsInRange(startMillis, endMillis)
        
        // Convert to map of date -> steps
        val stepsMap = stepRecords.associate { record ->
            val date = java.time.Instant.ofEpochMilli(record.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date to record.steps
        }
        
        // Calculate current streak (consecutive days meeting goal, ending today or yesterday)
        var currentStreak = 0
        var checkDate = today
        
        // Check if today's goal is met, if not start from yesterday
        val todaySteps = stepsMap[today] ?: 0
        if (todaySteps < dailyGoal) {
            checkDate = today.minusDays(1)
        }
        
        while (true) {
            val steps = stepsMap[checkDate] ?: 0
            if (steps >= dailyGoal) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        
        // Calculate longest streak ever
        var longestStreak = 0
        var tempStreak = 0
        val streakDays = mutableListOf<LocalDate>()
        
        var scanDate = startDate
        while (!scanDate.isAfter(today)) {
            val steps = stepsMap[scanDate] ?: 0
            if (steps >= dailyGoal) {
                tempStreak++
                streakDays.add(scanDate)
            } else {
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
                tempStreak = 0
            }
            scanDate = scanDate.plusDays(1)
        }
        
        // Check final streak
        if (tempStreak > longestStreak) {
            longestStreak = tempStreak
        }
        
        // Determine last streak date
        val lastStreakDate = if (currentStreak > 0) {
            if (todaySteps >= dailyGoal) today else today.minusDays(1)
        } else {
            streakDays.lastOrNull()
        }
        
        emit(StreakData(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastStreakDate = lastStreakDate,
            streakHistory = streakDays
        ))
    }
    
    /**
     * Check if user can extend streak today
     */
    suspend fun canExtendStreakToday(): Boolean {
        val dailyGoal = userPreferencesRepository.dailyStepGoal.first()
        val today = LocalDate.now()
        
        val startMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val todayRecord = stepDao.getStepsInRange(startMillis, endMillis).firstOrNull()
        val todaySteps = todayRecord?.steps ?: 0
        
        return todaySteps < dailyGoal
    }
    
    /**
     * Get steps needed to maintain/extend streak
     */
    suspend fun getStepsNeededForStreak(): Int {
        val dailyGoal = userPreferencesRepository.dailyStepGoal.first()
        val today = LocalDate.now()
        
        val startMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val todayRecord = stepDao.getStepsInRange(startMillis, endMillis).firstOrNull()
        val todaySteps = todayRecord?.steps ?: 0
        
        return (dailyGoal - todaySteps).coerceAtLeast(0)
    }
}
