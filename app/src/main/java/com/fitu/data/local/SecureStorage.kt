 package com.fitu.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ FIX #2: Secure storage for sensitive data like API keys.
 * 
 * This is the ONLY place where API keys should be stored.
 * Uses Android's EncryptedSharedPreferences with AES-256 encryption.
 */
@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "fitu_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    /**
     * Flow that emits API key changes.
     * Use this to observe API key state reactively.
     */
    val apiKeyFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_GEMINI_API_KEY) {
                trySend(prefs.getString(KEY_GEMINI_API_KEY, "") ?: "")
            }
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        emit(getApiKey())
    }

    /**
     * Save API key securely (encrypted).
     */
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    /**
     * Get API key synchronously.
     */
    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    /**
     * Clear API key (for logout/reset).
     */
    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    /**
     * Check if API key is set.
     */
    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }
} 
