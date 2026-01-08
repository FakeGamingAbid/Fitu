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
 * Secure storage for sensitive data like API keys using Android's EncryptedSharedPreferences.
 * This provides AES-256 encryption for both keys and values.
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

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }
}
