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
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
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

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
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

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETE] = complete
        }
    }
}
