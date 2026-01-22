package com.fitu.data.local

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.fitu.data.local.dao.*
import com.fitu.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val preferences: PreferencesData? = null,
    val apiKey: String? = null,
    val steps: List<StepEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val workoutPlans: List<WorkoutPlanEntity> = emptyList(),
    val foodCache: List<FoodCacheEntity> = emptyList()
)

data class PreferencesData(
    val userName: String?,
    val userAge: Int?,
    val userHeightCm: Int?,
    val userWeightKg: Int?,
    val dailyStepGoal: Int?,
    val dailyCalorieGoal: Int?,
    val birthDay: Int?,
    val birthMonth: Int?,
    val birthYear: Int?,
    val lastBirthdayWishYear: Int?,
    val useImperialUnits: Boolean?,
    val isOnboardingComplete: Boolean?
)

@Singleton
class BackupRestoreRepository @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage,
    private val stepDao: StepDao,
    private val mealDao: MealDao,
    private val workoutDao: WorkoutDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val foodCacheDao: FoodCacheDao
) {
    companion object {
        private const val TAG = "BackupRestoreRepo"
        private const val BACKUP_FOLDER = "FituBackups"
        private const val BACKUP_FILE_PREFIX = "fitu_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }

    /**
     * Export all app data to a JSON file
     */
    suspend fun exportData(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Collect all data
            val preferencesData = PreferencesData(
                userName = userPreferencesRepository.userName.first(),
                userAge = userPreferencesRepository.userAge.first(),
                userHeightCm = userPreferencesRepository.userHeightCm.first(),
                userWeightKg = userPreferencesRepository.userWeightKg.first(),
                dailyStepGoal = userPreferencesRepository.dailyStepGoal.first(),
                dailyCalorieGoal = userPreferencesRepository.dailyCalorieGoal.first(),
                birthDay = userPreferencesRepository.birthDay.first(),
                birthMonth = userPreferencesRepository.birthMonth.first(),
                birthYear = userPreferencesRepository.birthYear.first(),
                lastBirthdayWishYear = userPreferencesRepository.lastBirthdayWishYear.first(),
                useImperialUnits = userPreferencesRepository.useImperialUnits.first(),
                isOnboardingComplete = userPreferencesRepository.isOnboardingComplete.first()
            )

            val apiKey = secureStorage.getApiKey()
            val steps = stepDao.getAllSteps()
            val meals = mealDao.getAllMeals()
            val workouts = workoutDao.getAllWorkouts()
            val workoutPlans = workoutPlanDao.getAllPlansSync()
            val foodCache = foodCacheDao.getAllCache()

            val backupData = BackupData(
                preferences = preferencesData,
                apiKey = apiKey,
                steps = steps,
                meals = meals,
                workouts = workouts,
                workoutPlans = workoutPlans,
                foodCache = foodCache
            )

            // Convert to JSON
            val json = backupDataToJson(backupData)

            // Save to file
            val file = createBackupFile()
            FileOutputStream(file).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
            }

            // Get URI using FileProvider for sharing
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentUri = saveToDownloadsMediaStore(file, json)
                contentUri ?: FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                // Use FileProvider for older versions
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating FileProvider URI", e)
                    Uri.fromFile(file)
                }
            }

            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
            Result.failure(e)
        }
    }

    /**
     * Import data from a JSON file
     */
    suspend fun importData(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Read file content
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Failed to read backup file"))

            // Parse JSON
            val backupData = jsonToBackupData(jsonString)

            // Restore preferences
            backupData.preferences?.let { prefs ->
                prefs.userName?.let { 
                    userPreferencesRepository.saveUserProfile(
                        name = it,
                        age = prefs.userAge ?: 25,
                        heightCm = prefs.userHeightCm ?: 170,
                        weightKg = prefs.userWeightKg ?: 70,
                        stepGoal = prefs.dailyStepGoal ?: 10000,
                        calorieGoal = prefs.dailyCalorieGoal ?: 2000
                    ) 
                }

                if (prefs.birthDay != null && prefs.birthMonth != null && prefs.birthYear != null) {
                    userPreferencesRepository.saveBirthDate(
                        prefs.birthDay,
                        prefs.birthMonth,
                        prefs.birthYear
                    )
                }

                prefs.useImperialUnits?.let {
                    userPreferencesRepository.setUseImperialUnits(it)
                }

                prefs.isOnboardingComplete?.let {
                    userPreferencesRepository.setOnboardingComplete(it)
                }

                prefs.lastBirthdayWishYear?.let {
                    userPreferencesRepository.setLastBirthdayWishYear(it)
                }
            }

            // Restore API key
            backupData.apiKey?.let {
                secureStorage.saveApiKey(it)
            }

            // Restore database data
            // Note: We're not clearing existing data - new data will be added
            
            // Restore steps
            backupData.steps.forEach { step ->
                stepDao.insertOrUpdate(step)
            }

            // Restore meals
            backupData.meals.forEach { meal ->
                mealDao.insert(meal)
            }

            // Restore workouts
            backupData.workouts.forEach { workout ->
                workoutDao.insert(workout)
            }

            // Restore workout plans
            backupData.workoutPlans.forEach { plan ->
                workoutPlanDao.insertPlan(plan)
            }

            // Restore food cache
            backupData.foodCache.forEach { cache ->
                foodCacheDao.insertCache(cache)
            }

            val summary = buildString {
                appendLine("Backup restored successfully!")
                appendLine("Profile: ${if (backupData.preferences != null) "✓" else "✗"}")
                appendLine("API Key: ${if (backupData.apiKey != null) "✓" else "✗"}")
                appendLine("Steps: ${backupData.steps.size} records")
                appendLine("Meals: ${backupData.meals.size} records")
                appendLine("Workouts: ${backupData.workouts.size} records")
                appendLine("Workout Plans: ${backupData.workoutPlans.size} records")
                appendLine("Food Cache: ${backupData.foodCache.size} records")
            }

            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing data", e)
            Result.failure(e)
        }
    }

    private fun createBackupFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${BACKUP_FILE_PREFIX}$timestamp$BACKUP_FILE_EXTENSION"
        
        val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER)
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FOLDER)
        }
        
        folder.mkdirs()
        return File(folder, fileName)
    }

    @Suppress("DEPRECATION")
    private fun saveToDownloadsMediaStore(file: File, content: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$BACKUP_FOLDER")
                }
                
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to MediaStore", e)
                null
            }
        } else {
            null
        }
    }

    private fun backupDataToJson(backupData: BackupData): String {
        val json = JSONObject()
        json.put("version", backupData.version)
        json.put("timestamp", backupData.timestamp)

        // Preferences
        backupData.preferences?.let { prefs ->
            val prefsJson = JSONObject()
            prefs.userName?.let { prefsJson.put("userName", it) }
            prefs.userAge?.let { prefsJson.put("userAge", it) }
            prefs.userHeightCm?.let { prefsJson.put("userHeightCm", it) }
            prefs.userWeightKg?.let { prefsJson.put("userWeightKg", it) }
            prefs.dailyStepGoal?.let { prefsJson.put("dailyStepGoal", it) }
            prefs.dailyCalorieGoal?.let { prefsJson.put("dailyCalorieGoal", it) }
            prefs.birthDay?.let { prefsJson.put("birthDay", it) }
            prefs.birthMonth?.let { prefsJson.put("birthMonth", it) }
            prefs.birthYear?.let { prefsJson.put("birthYear", it) }
            prefs.lastBirthdayWishYear?.let { prefsJson.put("lastBirthdayWishYear", it) }
            prefs.useImperialUnits?.let { prefsJson.put("useImperialUnits", it) }
            prefs.isOnboardingComplete?.let { prefsJson.put("isOnboardingComplete", it) }
            json.put("preferences", prefsJson)
        }

        // API Key
        backupData.apiKey?.let { json.put("apiKey", it) }

        // Steps
        val stepsArray = org.json.JSONArray()
        backupData.steps.forEach { step ->
            val stepJson = JSONObject()
            stepJson.put("id", step.id)
            stepJson.put("date", step.date)
            stepJson.put("steps", step.steps)
            stepJson.put("goal", step.goal)
            stepJson.put("caloriesBurned", step.caloriesBurned)
            stepJson.put("distanceMeters", step.distanceMeters.toDouble())
            stepJson.put("lastUpdated", step.lastUpdated)
            stepsArray.put(stepJson)
        }
        json.put("steps", stepsArray)

        // Meals
        val mealsArray = org.json.JSONArray()
        backupData.meals.forEach { meal ->
            val mealJson = JSONObject()
            mealJson.put("id", meal.id)
            mealJson.put("name", meal.name)
            mealJson.put("description", meal.description)
            mealJson.put("calories", meal.calories)
            mealJson.put("protein", meal.protein)
            mealJson.put("carbs", meal.carbs)
            mealJson.put("fat", meal.fat)
            mealJson.put("fiber", meal.fiber)
            mealJson.put("mealType", meal.mealType)
            mealJson.put("portion", meal.portion.toDouble())
            meal.photoUri?.let { mealJson.put("photoUri", it) }
            mealJson.put("date", meal.date)
            mealJson.put("timestamp", meal.timestamp)
            mealJson.put("createdAt", meal.createdAt)
            mealsArray.put(mealJson)
        }
        json.put("meals", mealsArray)

        // Workouts
        val workoutsArray = org.json.JSONArray()
        backupData.workouts.forEach { workout ->
            val workoutJson = JSONObject()
            workoutJson.put("id", workout.id)
            workoutJson.put("exerciseType", workout.exerciseType)
            workoutJson.put("type", workout.type)
            workoutJson.put("reps", workout.reps)
            workoutJson.put("sets", workout.sets)
            workoutJson.put("durationSeconds", workout.durationSeconds)
            workoutJson.put("durationMs", workout.durationMs)
            workoutJson.put("caloriesBurned", workout.caloriesBurned)
            workoutJson.put("date", workout.date)
            workoutJson.put("timestamp", workout.timestamp)
            workoutJson.put("createdAt", workout.createdAt)
            workoutsArray.put(workoutJson)
        }
        json.put("workouts", workoutsArray)

        // Workout Plans
        val workoutPlansArray = org.json.JSONArray()
        backupData.workoutPlans.forEach { plan ->
            val planJson = JSONObject()
            planJson.put("id", plan.id)
            planJson.put("name", plan.name)
            planJson.put("description", plan.description)
            planJson.put("exercises", plan.exercises)
            planJson.put("difficulty", plan.difficulty)
            planJson.put("durationMinutes", plan.durationMinutes)
            planJson.put("duration", plan.duration)
            planJson.put("targetMuscles", plan.targetMuscles)
            planJson.put("muscleGroups", plan.muscleGroups)
            planJson.put("equipment", plan.equipment)
            planJson.put("createdAt", plan.createdAt)
            workoutPlansArray.put(planJson)
        }
        json.put("workoutPlans", workoutPlansArray)

        // Food Cache
        val foodCacheArray = org.json.JSONArray()
        backupData.foodCache.forEach { cache ->
            val cacheJson = JSONObject()
            cacheJson.put("query", cache.query)
            cacheJson.put("resultJson", cache.resultJson)
            cacheJson.put("timestamp", cache.timestamp)
            foodCacheArray.put(cacheJson)
        }
        json.put("foodCache", foodCacheArray)

        return json.toString(2) // Pretty print with 2 spaces indentation
    }

    private fun jsonToBackupData(jsonString: String): BackupData {
        val json = JSONObject(jsonString)
        
        val preferencesJson = json.optJSONObject("preferences")
        val preferences = preferencesJson?.let {
            PreferencesData(
                userName = it.optString("userName", null).takeIf { s -> s.isNotEmpty() },
                userAge = it.optInt("userAge").takeIf { i -> i > 0 },
                userHeightCm = it.optInt("userHeightCm").takeIf { i -> i > 0 },
                userWeightKg = it.optInt("userWeightKg").takeIf { i -> i > 0 },
                dailyStepGoal = it.optInt("dailyStepGoal").takeIf { i -> i > 0 },
                dailyCalorieGoal = it.optInt("dailyCalorieGoal").takeIf { i -> i > 0 },
                birthDay = it.optInt("birthDay").takeIf { i -> i > 0 },
                birthMonth = it.optInt("birthMonth").takeIf { i -> i > 0 },
                birthYear = it.optInt("birthYear").takeIf { i -> i > 0 },
                lastBirthdayWishYear = it.optInt("lastBirthdayWishYear").takeIf { i -> i > 0 },
                useImperialUnits = if (it.has("useImperialUnits")) it.optBoolean("useImperialUnits") else null,
                isOnboardingComplete = if (it.has("isOnboardingComplete")) it.optBoolean("isOnboardingComplete") else null
            )
        }

        val apiKey = json.optString("apiKey", null).takeIf { it.isNotEmpty() }

        val steps = mutableListOf<StepEntity>()
        json.optJSONArray("steps")?.let { array ->
            for (i in 0 until array.length()) {
                val stepJson = array.getJSONObject(i)
                steps.add(
                    StepEntity(
                        id = stepJson.optLong("id", 0),
                        date = stepJson.getString("date"),
                        steps = stepJson.getInt("steps"),
                        goal = stepJson.optInt("goal", 10000),
                        caloriesBurned = stepJson.optInt("caloriesBurned", 0),
                        distanceMeters = stepJson.optDouble("distanceMeters", 0.0).toFloat(),
                        lastUpdated = stepJson.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }
        }

        val meals = mutableListOf<MealEntity>()
        json.optJSONArray("meals")?.let { array ->
            for (i in 0 until array.length()) {
                val mealJson = array.getJSONObject(i)
                meals.add(
                    MealEntity(
                        id = mealJson.optLong("id", 0),
                        name = mealJson.getString("name"),
                        description = mealJson.optString("description", ""),
                        calories = mealJson.getInt("calories"),
                        protein = mealJson.optInt("protein", 0),
                        carbs = mealJson.optInt("carbs", 0),
                        fat = mealJson.optInt("fat", 0),
                        fiber = mealJson.optInt("fiber", 0),
                        mealType = mealJson.optString("mealType", "snack"),
                        portion = mealJson.optDouble("portion", 1.0).toFloat(),
                        photoUri = mealJson.optString("photoUri", null).takeIf { it.isNotEmpty() },
                        date = mealJson.optLong("date", System.currentTimeMillis()),
                        timestamp = mealJson.optLong("timestamp", System.currentTimeMillis()),
                        createdAt = mealJson.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val workouts = mutableListOf<WorkoutEntity>()
        json.optJSONArray("workouts")?.let { array ->
            for (i in 0 until array.length()) {
                val workoutJson = array.getJSONObject(i)
                workouts.add(
                    WorkoutEntity(
                        id = workoutJson.optLong("id", 0),
                        exerciseType = workoutJson.optString("exerciseType", ""),
                        type = workoutJson.optString("type", ""),
                        reps = workoutJson.optInt("reps", 0),
                        sets = workoutJson.optInt("sets", 0),
                        durationSeconds = workoutJson.optInt("durationSeconds", 0),
                        durationMs = workoutJson.optLong("durationMs", 0),
                        caloriesBurned = workoutJson.optInt("caloriesBurned", 0),
                        date = workoutJson.optLong("date", System.currentTimeMillis()),
                        timestamp = workoutJson.optLong("timestamp", System.currentTimeMillis()),
                        createdAt = workoutJson.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val workoutPlans = mutableListOf<WorkoutPlanEntity>()
        json.optJSONArray("workoutPlans")?.let { array ->
            for (i in 0 until array.length()) {
                val planJson = array.getJSONObject(i)
                workoutPlans.add(
                    WorkoutPlanEntity(
                        id = planJson.optLong("id", 0),
                        name = planJson.getString("name"),
                        description = planJson.optString("description", ""),
                        exercises = planJson.optString("exercises", ""),
                        difficulty = planJson.optString("difficulty", "intermediate"),
                        durationMinutes = planJson.optInt("durationMinutes", 30),
                        duration = planJson.optInt("duration", 30),
                        targetMuscles = planJson.optString("targetMuscles", ""),
                        muscleGroups = planJson.optString("muscleGroups", ""),
                        equipment = planJson.optString("equipment", ""),
                        createdAt = planJson.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val foodCache = mutableListOf<FoodCacheEntity>()
        json.optJSONArray("foodCache")?.let { array ->
            for (i in 0 until array.length()) {
                val cacheJson = array.getJSONObject(i)
                foodCache.add(
                    FoodCacheEntity(
                        query = cacheJson.getString("query"),
                        resultJson = cacheJson.getString("resultJson"),
                        timestamp = cacheJson.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        return BackupData(
            version = json.optInt("version", 1),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            preferences = preferences,
            apiKey = apiKey,
            steps = steps,
            meals = meals,
            workouts = workouts,
            workoutPlans = workoutPlans,
            foodCache = foodCache
        )
    }
}
