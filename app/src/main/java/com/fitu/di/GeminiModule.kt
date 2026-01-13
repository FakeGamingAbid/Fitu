 package com.fitu.di

import com.fitu.data.local.SecureStorage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider that creates GenerativeModel instances with the current API key.
 * This allows the API key to be updated without restarting the app.
 * Also provides retry logic for API calls.
 */
@Singleton
class GeminiModelProvider @Inject constructor(
    private val secureStorage: SecureStorage
) {
    fun getModel(modelName: String = "gemini-3-flash-preview"): GenerativeModel? {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isBlank()) return null
        
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    suspend fun generateContentWithRetry(
        prompt: Content,
        modelName: String = "gemini-3-flash-preview",
        maxRetries: Int = 3,
        initialDelay: Long = 1000
    ): GenerateContentResponse? {
        val model = getModel(modelName) ?: return null
        var currentDelay = initialDelay
        
        repeat(maxRetries) { attempt ->
            try {
                return model.generateContent(prompt)
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                if (e is IOException) { // Network error, retry
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    throw e // Other errors (e.g. API key invalid), don't retry
                }
            }
        }
        return null
    }
}

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiModelProvider(
        secureStorage: SecureStorage
    ): GeminiModelProvider {
        return GeminiModelProvider(secureStorage)
    }
} 
