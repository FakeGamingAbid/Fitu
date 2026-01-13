 package com.fitu.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fitu_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_HEIGHT_CM = intPreferencesKey("user_height_cm")
        val USER_WEIGHT_KG = intPreferencesKey("user_weight_kg")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
        val DAILY_CALORIE_GOAL = intPreferencesKey("daily_calorie_goal")
        
        // ✅ FIX #2: REMOVED - geminiApiKey is now ONLY in SecureStorage
        // val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        
        // Birth date fields (stored separately for flexibility)
        val BIRTH_DAY = intPreferencesKey("birth_day")
        val BIRTH_MONTH = intPreferencesKey("birth_month")
        val BIRTH_YEAR = intPreferencesKey("birth_year")
        
        // Birthday wish tracking (only show once per year)
        val LAST_BIRTHDAY_WISH_YEAR = intPreferencesKey("last_birthday_wish_year")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETE] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME] ?: ""
    }

    val userAge: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_AGE] ?: 25
    }

    val userHeightCm: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_HEIGHT_CM] ?: 170
    }

    val userWeightKg: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_WEIGHT_KG] ?: 70
    }

    val dailyStepGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_STEP_GOAL] ?: 10000
    }

    val dailyCalorieGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_CALORIE_GOAL] ?: 2000
    }

    // ✅ FIX #2: REMOVED - geminiApiKey getter
    // API key is now ONLY accessed via SecureStorage.getApiKey() and SecureStorage.apiKeyFlow

    // Birth date fields
    val birthDay: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIRTH_DAY]
    }

    val birthMonth: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIRTH_MONTH]
    }

    val birthYear: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIRTH_YEAR]
    }

    val lastBirthdayWishYear: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_BIRTHDAY_WISH_YEAR]
    }

    suspend fun saveUserProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.USER_AGE] = age
            preferences[PreferencesKeys.USER_HEIGHT_CM] = heightCm
            preferences[PreferencesKeys.USER_WEIGHT_KG] = weightKg
            preferences[PreferencesKeys.DAILY_STEP_GOAL] = stepGoal
            preferences[PreferencesKeys.DAILY_CALORIE_GOAL] = calorieGoal
        }
    }

    /**
     * Save birth date (day, month, year).
     * Pass null values to clear the birth date.
     */
    suspend fun saveBirthDate(day: Int?, month: Int?, year: Int?) {
        context.dataStore.edit { preferences ->
            if (day != null && month != null && year != null) {
                preferences[PreferencesKeys.BIRTH_DAY] = day
                preferences[PreferencesKeys.BIRTH_MONTH] = month
                preferences[PreferencesKeys.BIRTH_YEAR] = year
            } else {
                preferences.remove(PreferencesKeys.BIRTH_DAY)
                preferences.remove(PreferencesKeys.BIRTH_MONTH)
                preferences.remove(PreferencesKeys.BIRTH_YEAR)
            }
        }
    }

    /**
     * Mark that the user has been wished for the given year.
     */
    suspend fun setLastBirthdayWishYear(year: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BIRTHDAY_WISH_YEAR] = year
        }
    }

    // ✅ FIX #2: REMOVED - saveApiKey function
    // API key is now ONLY saved via SecureStorage.saveApiKey()

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETE] = complete
        }
    }

    /**
     * ✅ NEW: Clear all user data (for app reset)
     * Note: This does NOT clear the API key (that's in SecureStorage)
     */
    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 
