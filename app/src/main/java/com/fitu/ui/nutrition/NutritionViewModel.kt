package com.fitu.ui.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.di.GeminiErrorType
import com.fitu.di.GeminiException
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiModelProvider: GeminiModelProvider,
    private val repository: DashboardRepository,
    private val mealDao: MealDao,
    private val foodCacheDao: com.fitu.data.local.dao.FoodCacheDao,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NutritionViewModel"
        
        // Image compression settings
        private const val MAX_IMAGE_WIDTH = 1024
        private const val MAX_IMAGE_HEIGHT = 1024
        private const val JPEG_QUALITY = 85
        
        // ✅ NEW: Thumbnail settings
        private const val THUMBNAIL_SIZE = 200
        private const val THUMBNAIL_QUALITY = 80
        
        // Rate limiting
        private const val MIN_REQUEST_INTERVAL_MS = 2000L
        
        // Duplicate detection settings
        private const val DUPLICATE_DETECTION_WINDOW_MS = 5000L
    }

    private val _uiState = MutableStateFlow<NutritionUiState>(NutritionUiState.Idle)
    val uiState: StateFlow<NutritionUiState> = _uiState

    private val _selectedMealType = MutableStateFlow(getMealTypeByTime())
    val selectedMealType: StateFlow<String> = _selectedMealType

    private val _showAddFoodSheet = MutableStateFlow(false)
    val showAddFoodSheet: StateFlow<Boolean> = _showAddFoodSheet

    private val _portion = MutableStateFlow(1f)
    val portion: StateFlow<Float> = _portion

    private val _analyzedFood = MutableStateFlow<AnalyzedFood?>(null)
    val analyzedFood: StateFlow<AnalyzedFood?> = _analyzedFood

    private val _textSearch = MutableStateFlow("")
    val textSearch: StateFlow<String> = _textSearch

    private val _mealToDelete = MutableStateFlow<MealEntity?>(null)
    val mealToDelete: StateFlow<MealEntity?> = _mealToDelete

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog

    // ✅ NEW: Store the current photo bitmap for saving
    private val _currentPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPhotoBitmap: StateFlow<Bitmap?> = _currentPhotoBitmap

    // Rate limiting
    private var lastRequestTime = 0L
    private var currentAnalysisJob: Job? = null

    // Duplicate detection
    private var lastAddedFoodName: String? = null
    private var lastAddedFoodTime: Long = 0L
    private var lastAddedMealType: String? = null

    private val _showDuplicateWarning = MutableStateFlow(false)
    val showDuplicateWarning: StateFlow<Boolean> = _showDuplicateWarning

    private val _duplicateWarningMessage = MutableStateFlow("")
    val duplicateWarningMessage: StateFlow<String> = _duplicateWarningMessage

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    val todayMeals: StateFlow<List<MealEntity>> = mealDao.getMealsForDay(
        getTodayRange().first, getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val caloriesConsumed: StateFlow<Int> = mealDao.getCaloriesConsumedForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { cal, _ -> cal ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val proteinConsumed: StateFlow<Int> = mealDao.getProteinForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { p, _ -> p ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val carbsConsumed: StateFlow<Int> = mealDao.getCarbsForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { c, _ -> c ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val fatsConsumed: StateFlow<Int> = mealDao.getFatsForDay(
        getTodayRange().first, getTodayRange().second
    ).combine(MutableStateFlow(0)) { f, _ -> f ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val calorieProgress: StateFlow<Float> = combine(caloriesConsumed, dailyCalorieGoal) { consumed, goal ->
        if (goal > 0) (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        cleanOldFoodCache()
        cleanOldFoodPhotos()
    }

    private fun getMealTypeByTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute
        
        val breakfastStart = 5 * 60
        val breakfastEnd = 11 * 60 + 59
        val lunchStart = 12 * 60
        val lunchEnd = 16 * 60
        val dinnerStart = 19 * 60
        val dinnerEnd = 22 * 60
        
        return when {
            currentMinutes in breakfastStart..breakfastEnd -> "breakfast"
            currentMinutes in lunchStart..lunchEnd -> "lunch"
            currentMinutes in dinnerStart..dinnerEnd -> "dinner"
            else -> "snacks"
        }
    }

    fun getMealTypeTimeRange(mealType: String): String {
        return when (mealType) {
            "breakfast" -> "5:00 AM - 12:00 PM"
            "lunch" -> "12:00 PM - 4:00 PM"
            "dinner" -> "7:00 PM - 10:00 PM"
            "snacks" -> "Anytime"
            else -> ""
        }
    }

    fun isSuggestedMealType(mealType: String): Boolean {
        return mealType == getMealTypeByTime()
    }

    private fun cleanOldFoodCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                foodCacheDao.clearOldCache(sevenDaysAgo)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean food cache", e)
            }
        }
    }

    /**
     * ✅ NEW: Clean old food photos (older than 30 days)
     */
    private fun cleanOldFoodPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val photosDir = getFoodPhotosDir()
                if (photosDir.exists()) {
                    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                    photosDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < thirtyDaysAgo) {
                            file.delete()
                            Log.d(TAG, "Deleted old food photo: ${file.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean old food photos", e)
            }
        }
    }

    /**
     * ✅ NEW: Get the directory for storing food photos
     */
    private fun getFoodPhotosDir(): File {
        val dir = File(context.filesDir, "food_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * ✅ NEW: Save bitmap as thumbnail and return the file URI
     */
    private suspend fun saveFoodPhoto(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Create thumbnail
                val thumbnail = createThumbnail(bitmap)
                
                // Generate unique filename
                val filename = "food_${UUID.randomUUID()}.jpg"
                val file = File(getFoodPhotosDir(), filename)
                
                // Save to file
                FileOutputStream(file).use { out ->
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
                }
                
                // Recycle thumbnail if different from original
                if (thumbnail != bitmap) {
                    thumbnail.recycle()
                }
                
                Log.d(TAG, "Saved food photo: ${file.absolutePath}")
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save food photo", e)
                null
            }
        }
    }

    /**
     * ✅ NEW: Create a thumbnail from bitmap
     */
    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleFactor = minOf(
            THUMBNAIL_SIZE.toFloat() / width,
            THUMBNAIL_SIZE.toFloat() / height,
            1f
        )
        
        if (scaleFactor >= 1f) {
            return bitmap
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * ✅ NEW: Delete food photo file
     */
    private fun deleteFoodPhoto(photoUri: String?) {
        if (photoUri.isNullOrBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(photoUri)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted food photo: $photoUri")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete food photo", e)
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
            return true
        }
        lastRequestTime = now
        return false
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleFactor = minOf(
            MAX_IMAGE_WIDTH.toFloat() / width,
            MAX_IMAGE_HEIGHT.toFloat() / height,
            1f
        )
        
        if (scaleFactor >= 1f) {
            return bitmap
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun getBitmapSizeKB(bitmap: Bitmap): Int {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.size() / 1024
    }

    fun selectMealType(type: String) {
        _selectedMealType.value = type
    }

    fun showAddFood() {
        _selectedMealType.value = getMealTypeByTime()
        _showAddFoodSheet.value = true
    }

    fun hideAddFood() {
        _showAddFoodSheet.value = false
        _analyzedFood.value = null
        _currentPhotoBitmap.value = null  // ✅ Clear photo
        _portion.value = 1f
        _textSearch.value = ""
        _uiState.value = NutritionUiState.Idle
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
        currentAnalysisJob?.cancel()
    }

    fun updatePortion(value: Float) {
        _portion.value = value
    }

    fun updateTextSearch(value: String) {
        _textSearch.value = value
    }

    private fun isDuplicateFood(foodName: String, mealType: String): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastAdd = now - lastAddedFoodTime
        
        if (lastAddedFoodName != null && 
            timeSinceLastAdd < DUPLICATE_DETECTION_WINDOW_MS &&
            lastAddedFoodName.equals(foodName, ignoreCase = true) &&
            lastAddedMealType == mealType) {
            return true
        }
        
        return false
    }

    private fun findSimilarRecentMeal(foodName: String, mealType: String): MealEntity? {
        val recentMeals = todayMeals.value
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L)
        
        return recentMeals.find { meal ->
            meal.name.equals(foodName, ignoreCase = true) &&
            meal.mealType == mealType &&
            meal.timestamp > fiveMinutesAgo
        }
    }

    fun dismissDuplicateWarning() {
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
    }

    fun forceAddFoodToMeal() {
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
        addFoodToMealInternal(ignoreDuplicate = true)
    }

    /**
     * Analyze food from image - ✅ UPDATED to store bitmap
     */
    fun analyzeFood(bitmap: Bitmap) {
        if (isRateLimited()) {
            Log.d(TAG, "Rate limited - ignoring request")
            return
        }

        if (_uiState.value is NutritionUiState.Analyzing) {
            Log.d(TAG, "Already analyzing - ignoring request")
            return
        }

        if (!isOnline()) {
            _uiState.value = NutritionUiState.Error(
                message = "No internet connection",
                errorType = NutritionErrorType.NETWORK,
                canRetry = true
            )
            return
        }

        // ✅ Store the bitmap for later saving
        _currentPhotoBitmap.value = bitmap

        _uiState.value = NutritionUiState.Analyzing

        currentAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val compressedBitmap = compressBitmap(bitmap)
                val sizeKB = getBitmapSizeKB(compressedBitmap)
                Log.d(TAG, "Sending image: ${compressedBitmap.width}x${compressedBitmap.height}, ~${sizeKB}KB")

                val inputContent = content {
                    image(compressedBitmap)
                    text("""
                        Analyze this food image and estimate its nutritional content.
                        
                        Return ONLY a valid JSON object with these exact fields:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                        
                        Rules:
                        - "name" should be a short, descriptive name (e.g., "Grilled Chicken Salad")
                        - "calories" is in kcal (integer)
                        - "protein", "carbs", "fats" are in grams (integers)
                        - Estimate for a standard serving size
                        - Do NOT include markdown, code blocks, or any explanation
                        - Return ONLY the JSON object
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    useFallback = true
                )

                val text = response.text?.trim() ?: throw GeminiException(
                    GeminiErrorType.EMPTY_RESPONSE,
                    "No response from AI"
                )

                Log.d(TAG, "Raw response: $text")

                val food = parseJsonResponse(text)
                
                _analyzedFood.value = food
                _uiState.value = NutritionUiState.Success(food)
                Log.d(TAG, "Successfully analyzed: ${food.name}")

            } catch (e: GeminiException) {
                Log.e(TAG, "Gemini error: ${e.errorType} - ${e.message}")
                _currentPhotoBitmap.value = null  // Clear on error
                _uiState.value = mapGeminiExceptionToUiState(e)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _currentPhotoBitmap.value = null  // Clear on error
                _uiState.value = NutritionUiState.Error(
                    message = "Something went wrong. Please try again.",
                    errorType = NutritionErrorType.UNKNOWN,
                    canRetry = true
                )
            }
        }
    }

    /**
     * Search food by text description - no photo
     */
    fun searchFood(query: String) {
        if (query.isBlank()) return

        if (isRateLimited()) {
            Log.d(TAG, "Rate limited - ignoring request")
            return
        }

        if (_uiState.value is NutritionUiState.Analyzing) {
            Log.d(TAG, "Already analyzing - ignoring request")
            return
        }

        if (!isOnline()) {
            _uiState.value = NutritionUiState.Error(
                message = "No internet connection",
                errorType = NutritionErrorType.NETWORK,
                canRetry = true
            )
            return
        }

        // ✅ Clear any existing photo since this is text search
        _currentPhotoBitmap.value = null

        _uiState.value = NutritionUiState.Analyzing

        currentAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val cached = foodCacheDao.getCache(query.lowercase().trim())
                if (cached != null && System.currentTimeMillis() - cached.timestamp < 7 * 24 * 60 * 60 * 1000) {
                    try {
                        val food = parseJsonResponse(cached.resultJson)
                        _analyzedFood.value = food
                        _uiState.value = NutritionUiState.Success(food)
                        Log.d(TAG, "Loaded from cache: ${food.name}")
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "Cache invalid, fetching fresh")
                    }
                }

                val inputContent = content {
                    text("""
                        Estimate the nutritional information for: "$query"
                        
                        Assume a standard serving size.
                        
                        Return ONLY a valid JSON object with these exact fields:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                        
                        Rules:
                        - "name" should be a clean, descriptive name
                        - "calories" is in kcal (integer)
                        - "protein", "carbs", "fats" are in grams (integers)
                        - Do NOT include markdown, code blocks, or any explanation
                        - Return ONLY the JSON object
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    useFallback = true
                )

                val text = response.text?.trim() ?: throw GeminiException(
                    GeminiErrorType.EMPTY_RESPONSE,
                    "No response from AI"
                )

                Log.d(TAG, "Raw response: $text")

                val food = parseJsonResponse(text)

                val cleanJson = """{"name":"${food.name}","calories":${food.calories},"protein":${food.protein},"carbs":${food.carbs},"fats":${food.fats}}"""
                foodCacheDao.insertCache(
                    com.fitu.data.local.entity.FoodCacheEntity(
                        query = query.lowercase().trim(),
                        resultJson = cleanJson,
                        timestamp = System.currentTimeMillis()
                    )
                )

                _analyzedFood.value = food
                _uiState.value = NutritionUiState.Success(food)
                Log.d(TAG, "Successfully searched: ${food.name}")

            } catch (e: GeminiException) {
                Log.e(TAG, "Gemini error: ${e.errorType} - ${e.message}")
                _uiState.value = mapGeminiExceptionToUiState(e)
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _uiState.value = NutritionUiState.Error(
                    message = "Something went wrong. Please try again.",
                    errorType = NutritionErrorType.UNKNOWN,
                    canRetry = true
                )
            }
        }
    }

    private fun parseJsonResponse(text: String): AnalyzedFood {
        val cleanText = text
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        Log.d(TAG, "Parsing JSON: $cleanText")

        try {
            val json = JSONObject(cleanText)
            
            val name = json.optString("name", "").ifBlank { 
                throw IllegalArgumentException("Missing food name") 
            }
            
            val calories = json.optInt("calories", -1).let {
                if (it < 0) throw IllegalArgumentException("Invalid calories")
                it
            }
            
            val protein = json.optInt("protein", 0).coerceAtLeast(0)
            val carbs = json.optInt("carbs", 0).coerceAtLeast(0)
            val fats = json.optInt("fats", 0).coerceAtLeast(0)

            return AnalyzedFood(
                name = name.take(100),
                calories = calories.coerceIn(0, 10000),
                protein = protein.coerceIn(0, 1000),
                carbs = carbs.coerceIn(0, 1000),
                fats = fats.coerceIn(0, 1000)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            throw GeminiException(
                GeminiErrorType.INVALID_REQUEST,
                "Could not understand the AI response. Please try again."
            )
        }
    }

    private fun mapGeminiExceptionToUiState(e: GeminiException): NutritionUiState.Error {
        return when (e.errorType) {
            GeminiErrorType.API_KEY_MISSING -> NutritionUiState.Error(
                message = "API key not set. Please add your Gemini API key in Profile settings.",
                errorType = NutritionErrorType.API_KEY,
                canRetry = false
            )
            GeminiErrorType.API_KEY_INVALID -> NutritionUiState.Error(
                message = "Invalid API key. Please check your Gemini API key in Profile settings.",
                errorType = NutritionErrorType.API_KEY,
                canRetry = false
            )
            GeminiErrorType.PERMISSION_DENIED -> NutritionUiState.Error(
                message = "API access denied. Please enable Generative Language API in Google Cloud Console.",
                errorType = NutritionErrorType.API_KEY,
                canRetry = false
            )
            GeminiErrorType.RATE_LIMITED -> NutritionUiState.Error(
                message = "Too many requests. Please wait a moment and try again.",
                errorType = NutritionErrorType.RATE_LIMIT,
                canRetry = true
            )
            GeminiErrorType.NETWORK_ERROR -> NutritionUiState.Error(
                message = "No internet connection. Please check your network.",
                errorType = NutritionErrorType.NETWORK,
                canRetry = true
            )
            GeminiErrorType.TIMEOUT -> NutritionUiState.Error(
                message = "Request timed out. Please try again.",
                errorType = NutritionErrorType.NETWORK,
                canRetry = true
            )
            GeminiErrorType.CONTENT_BLOCKED -> NutritionUiState.Error(
                message = "Image could not be processed. Please try a different photo.",
                errorType = NutritionErrorType.CONTENT,
                canRetry = false
            )
            GeminiErrorType.MODEL_NOT_FOUND -> NutritionUiState.Error(
                message = "AI model unavailable. Please try again later.",
                errorType = NutritionErrorType.SERVICE,
                canRetry = true
            )
            GeminiErrorType.EMPTY_RESPONSE -> NutritionUiState.Error(
                message = "Could not identify the food. Please try a clearer photo.",
                errorType = NutritionErrorType.CONTENT,
                canRetry = true
            )
            else -> NutritionUiState.Error(
                message = e.message,
                errorType = NutritionErrorType.UNKNOWN,
                canRetry = true
            )
        }
    }

    fun addFoodToMeal() {
        addFoodToMealInternal(ignoreDuplicate = false)
    }

    /**
     * ✅ UPDATED: Save photo when adding meal
     */
    private fun addFoodToMealInternal(ignoreDuplicate: Boolean) {
        val food = _analyzedFood.value ?: return
        val mealType = _selectedMealType.value
        val portionMultiplier = _portion.value

        if (!ignoreDuplicate) {
            if (isDuplicateFood(food.name, mealType)) {
                Log.d(TAG, "Duplicate detected (rapid click): ${food.name}")
                _duplicateWarningMessage.value = "You just added \"${food.name}\" to ${mealType}. Add again?"
                _showDuplicateWarning.value = true
                return
            }

            val similarMeal = findSimilarRecentMeal(food.name, mealType)
            if (similarMeal != null) {
                Log.d(TAG, "Similar meal found recently: ${food.name}")
                _duplicateWarningMessage.value = "\"${food.name}\" was already added to ${mealType} recently. Add another serving?"
                _showDuplicateWarning.value = true
                return
            }
        }

        viewModelScope.launch {
            // ✅ Save photo if available
            val photoUri = _currentPhotoBitmap.value?.let { bitmap ->
                saveFoodPhoto(bitmap)
            }

            val meal = MealEntity(
                name = food.name,
                calories = (food.calories * portionMultiplier).toInt(),
                protein = (food.protein * portionMultiplier).toInt(),
                carbs = (food.carbs * portionMultiplier).toInt(),
                fats = (food.fats * portionMultiplier).toInt(),
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                portion = portionMultiplier,
                photoUri = photoUri  // ✅ Save photo URI
            )
            mealDao.insertMeal(meal)
            
            lastAddedFoodName = food.name
            lastAddedFoodTime = System.currentTimeMillis()
            lastAddedMealType = mealType
            
            Log.d(TAG, "Added meal: ${food.name} to $mealType with photo: $photoUri")
            hideAddFood()
        }
    }

    fun requestDeleteMeal(meal: MealEntity) {
        _mealToDelete.value = meal
        _showDeleteConfirmDialog.value = true
    }

    fun cancelDeleteMeal() {
        _mealToDelete.value = null
        _showDeleteConfirmDialog.value = false
    }

    /**
     * ✅ UPDATED: Delete photo when deleting meal
     */
    fun confirmDeleteMeal() {
        val meal = _mealToDelete.value ?: return
        viewModelScope.launch {
            // Delete photo file if exists
            deleteFoodPhoto(meal.photoUri)
            
            mealDao.deleteMeal(meal.id)
            _mealToDelete.value = null
            _showDeleteConfirmDialog.value = false
        }
    }

    fun reset() {
        _uiState.value = NutritionUiState.Idle
        _analyzedFood.value = null
        _currentPhotoBitmap.value = null
    }

    fun retry() {
        _uiState.value = NutritionUiState.Idle
    }
}

data class AnalyzedFood(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

enum class NutritionErrorType {
    NETWORK,
    API_KEY,
    RATE_LIMIT,
    CONTENT,
    SERVICE,
    UNKNOWN
}

sealed class NutritionUiState {
    object Idle : NutritionUiState()
    object Analyzing : NutritionUiState()
    data class Success(val food: AnalyzedFood) : NutritionUiState()
    data class Error(
        val message: String,
        val errorType: NutritionErrorType = NutritionErrorType.UNKNOWN,
        val canRetry: Boolean = true
    ) : NutritionUiState()
}
