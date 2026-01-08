package com.fitu.ui.nutrition

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val geminiModelProvider: GeminiModelProvider,
    private val repository: DashboardRepository,
    private val mealDao: MealDao,
    private val foodCacheDao: com.fitu.data.local.dao.FoodCacheDao,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NutritionUiState>(NutritionUiState.Idle)
    val uiState: StateFlow<NutritionUiState> = _uiState

    private val _selectedMealType = MutableStateFlow("breakfast")
    val selectedMealType: StateFlow<String> = _selectedMealType

    private val _showAddFoodSheet = MutableStateFlow(false)
    val showAddFoodSheet: StateFlow<Boolean> = _showAddFoodSheet

    private val _portion = MutableStateFlow(1f)
    val portion: StateFlow<Float> = _portion

    private val _analyzedFood = MutableStateFlow<AnalyzedFood?>(null)
    val analyzedFood: StateFlow<AnalyzedFood?> = _analyzedFood

    private val _textSearch = MutableStateFlow("")
    val textSearch: StateFlow<String> = _textSearch

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

    fun selectMealType(type: String) {
        _selectedMealType.value = type
    }

    fun showAddFood() {
        _showAddFoodSheet.value = true
    }

    fun hideAddFood() {
        _showAddFoodSheet.value = false
        _analyzedFood.value = null
        _portion.value = 1f
        _textSearch.value = ""
        _uiState.value = NutritionUiState.Idle
    }

    fun updatePortion(value: Float) {
        _portion.value = value
    }

    fun updateTextSearch(value: String) {
        _textSearch.value = value
    }

    fun analyzeFood(bitmap: Bitmap) {
        _uiState.value = NutritionUiState.Analyzing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputContent = content {
                    image(bitmap)
                    text("""
                        Analyze this food image. Return ONLY a JSON object with these exact fields:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                        All numbers should be integers representing grams or kcal.
                        Do not include any markdown, code blocks, or explanation.
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    modelName = "gemini-3-flash-preview"
                )
                
                if (response != null && response.text != null) {
                    val text = response.text!!.trim()
                    try {
                        val json = JSONObject(text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                        val food = AnalyzedFood(
                            name = json.getString("name"),
                            calories = json.getInt("calories"),
                            protein = json.getInt("protein"),
                            carbs = json.getInt("carbs"),
                            fats = json.getInt("fats")
                        )
                        _analyzedFood.value = food
                        _uiState.value = NutritionUiState.Success(text)
                    } catch (e: Exception) {
                        _uiState.value = NutritionUiState.Error("Failed to parse: $text")
                    }
                } else {
                    _uiState.value = NutritionUiState.Error("Failed to analyze image. Please check your API key or internet connection.")
                }
            } catch (e: Exception) {
                _uiState.value = NutritionUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun searchFood(query: String) {
        if (query.isBlank()) return
        _uiState.value = NutritionUiState.Analyzing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = foodCacheDao.getCache(query)
                if (cached != null && System.currentTimeMillis() - cached.timestamp < 24 * 60 * 60 * 1000) { // 24 hours cache
                    try {
                        val json = JSONObject(cached.resultJson)
                        val food = AnalyzedFood(
                            name = json.getString("name"),
                            calories = json.getInt("calories"),
                            protein = json.getInt("protein"),
                            carbs = json.getInt("carbs"),
                            fats = json.getInt("fats")
                        )
                        _analyzedFood.value = food
                        _uiState.value = NutritionUiState.Success(cached.resultJson)
                        return@launch
                    } catch (e: Exception) {
                        // Cache invalid, proceed to API
                    }
                }

                val inputContent = content {
                    text("""
                        Estimate the nutritional info for: "$query" (standard serving).
                        Return ONLY a JSON object:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                        All numbers should be integers. No markdown or explanation.
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    modelName = "gemini-3-flash-preview"
                )
                
                if (response != null && response.text != null) {
                    val text = response.text!!.trim()
                    try {
                        val cleanText = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val json = JSONObject(cleanText)
                        val food = AnalyzedFood(
                            name = json.getString("name"),
                            calories = json.getInt("calories"),
                            protein = json.getInt("protein"),
                            carbs = json.getInt("carbs"),
                            fats = json.getInt("fats")
                        )
                        _analyzedFood.value = food
                        _uiState.value = NutritionUiState.Success(text)
                        
                        // Cache the result
                        foodCacheDao.insertCache(
                            com.fitu.data.local.entity.FoodCacheEntity(
                                query = query,
                                resultJson = cleanText,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } catch (e: Exception) {
                        _uiState.value = NutritionUiState.Error("Failed to parse response")
                    }
                } else {
                    _uiState.value = NutritionUiState.Error("Failed to search food. Please check your API key or internet connection.")
                }
            } catch (e: Exception) {
                _uiState.value = NutritionUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addFoodToMeal() {
        val food = _analyzedFood.value ?: return
        val portionMultiplier = _portion.value

        viewModelScope.launch {
            val meal = MealEntity(
                name = food.name,
                calories = (food.calories * portionMultiplier).toInt(),
                protein = (food.protein * portionMultiplier).toInt(),
                carbs = (food.carbs * portionMultiplier).toInt(),
                fats = (food.fats * portionMultiplier).toInt(),
                timestamp = System.currentTimeMillis(),
                mealType = _selectedMealType.value,
                portion = portionMultiplier
            )
            mealDao.insertMeal(meal)
            hideAddFood()
        }
    }

    fun deleteMeal(mealId: Int) {
        viewModelScope.launch {
            mealDao.deleteMeal(mealId)
        }
    }

    fun reset() {
        _uiState.value = NutritionUiState.Idle
        _analyzedFood.value = null
    }
}

data class AnalyzedFood(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

sealed class NutritionUiState {
    object Idle : NutritionUiState()
    object Analyzing : NutritionUiState()
    data class Success(val result: String) : NutritionUiState()
    data class Error(val message: String) : NutritionUiState()
}
