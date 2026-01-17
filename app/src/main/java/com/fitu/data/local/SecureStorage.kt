package com.fitu.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "fitu_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        securePrefs.edit().putString(KEY_GEMINI_API, key).apply()
    }

    fun getApiKey(): String? {
        return securePrefs.getString(KEY_GEMINI_API, null)
    }

    fun clearApiKey() {
        securePrefs.edit().remove(KEY_GEMINI_API).apply()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}
