package com.fitu.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data like API keys.
 * 
 * Uses Android's EncryptedSharedPreferences with AES-256 encryption.
 * Falls back to regular SharedPreferences if encryption fails (rare device issues).
 */
@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SecureStorage"
        private const val SECURE_PREFS_NAME = "fitu_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "fitu_fallback_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_USING_FALLBACK = "using_fallback_storage"
    }

    /**
     * Flag to track if we're using fallback (unencrypted) storage.
     * This is stored in regular prefs so we know on next launch.
     */
    private val metaPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("fitu_meta_prefs", Context.MODE_PRIVATE)
    }

    private var isUsingFallback: Boolean
        get() = metaPrefs.getBoolean(KEY_USING_FALLBACK, false)
        set(value) = metaPrefs.edit().putBoolean(KEY_USING_FALLBACK, value).apply()

    /**
     * Lazily initialized SharedPreferences - either encrypted or fallback.
     */
    private val prefs: SharedPreferences by lazy {
        getOrCreatePreferences()
    }

    /**
     * Attempts to create EncryptedSharedPreferences.
     * Falls back to regular SharedPreferences if encryption fails.
     */
    private fun getOrCreatePreferences(): SharedPreferences {
        // If we previously failed and are using fallback, continue using it
        if (isUsingFallback) {
            Log.w(TAG, "Using fallback storage (previous encryption failure)")
            return getFallbackPreferences()
        }

        return try {
            createEncryptedPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted storage, attempting recovery", e)
            attemptRecoveryAndRetry()
        }
    }

    /**
     * Creates encrypted SharedPreferences with MasterKey.
     */
    private fun createEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            Log.d(TAG, "Successfully created encrypted storage")
        }
    }

    /**
     * Attempts to recover from encryption failure by clearing corrupted data.
     * Returns encrypted prefs if recovery succeeds, fallback prefs otherwise.
     */
    private fun attemptRecoveryAndRetry(): SharedPreferences {
        return try {
            // Step 1: Try deleting the corrupted encrypted prefs file
            Log.d(TAG, "Attempting recovery: deleting corrupted prefs")
            deleteEncryptedPrefsFile()

            // Step 2: Try creating encrypted prefs again
            createEncryptedPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed, using fallback storage", e)
            
            // Mark that we're using fallback so we don't retry every launch
            isUsingFallback = true
            
            // Migrate any existing data from corrupted prefs if possible
            migrateToFallbackIfNeeded()
            
            getFallbackPreferences()
        }
    }

    /**
     * Deletes the encrypted SharedPreferences file.
     */
    private fun deleteEncryptedPrefsFile() {
        try {
            val prefsFile = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
            prefsFile.edit().clear().commit()
            
            // Also try to delete the actual file
            val prefsDir = context.filesDir.parentFile?.resolve("shared_prefs")
            val securePrefsFile = prefsDir?.resolve("$SECURE_PREFS_NAME.xml")
            securePrefsFile?.delete()
            
            Log.d(TAG, "Deleted corrupted prefs file")
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete prefs file", e)
        }
    }

    /**
     * Gets the fallback (unencrypted) SharedPreferences.
     * 
     * Note: This is less secure but better than crashing.
     * The API key will be stored in plain text.
     */
    private fun getFallbackPreferences(): SharedPreferences {
        Log.w(TAG, "⚠️ Using unencrypted fallback storage - API key not encrypted")
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Attempts to migrate data from encrypted to fallback prefs.
     * This handles the case where encrypted prefs were working before but got corrupted.
     */
    private fun migrateToFallbackIfNeeded() {
        try {
            // Try to read from encrypted prefs (might work for reading even if creation fails)
            val encryptedPrefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
            val existingKey = encryptedPrefs.getString(KEY_GEMINI_API_KEY, null)
            
            if (!existingKey.isNullOrBlank()) {
                Log.d(TAG, "Migrating existing API key to fallback storage")
                getFallbackPreferences().edit()
                    .putString(KEY_GEMINI_API_KEY, existingKey)
                    .apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not migrate existing data", e)
        }
    }

    /**
     * Flow that emits API key changes.
     * Use this to observe API key state reactively.
     */
    val apiKeyFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GEMINI_API_KEY) {
                trySend(getApiKey())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        emit(getApiKey())
    }

    /**
     * Save API key securely (or in fallback storage).
     */
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
        Log.d(TAG, "API key saved (encrypted: ${!isUsingFallback})")
    }

    /**
     * Get API key synchronously.
     */
    fun getApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    /**
     * Clear API key (for logout/reset).
     */
    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        Log.d(TAG, "API key cleared")
    }

    /**
     * Check if API key is set.
     */
    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    /**
     * Check if we're using fallback (unencrypted) storage.
     * UI can use this to warn the user.
     */
    fun isUsingFallbackStorage(): Boolean {
        return isUsingFallback
    }

    /**
     * Attempt to upgrade from fallback to encrypted storage.
     * Call this if user wants to try re-enabling encryption.
     * 
     * @return true if upgrade succeeded, false if still using fallback
     */
    fun attemptUpgradeToEncrypted(): Boolean {
        if (!isUsingFallback) {
            return true // Already using encrypted
        }

        return try {
            // Save current API key
            val currentKey = getApiKey()
            
            // Try to create encrypted prefs
            val encryptedPrefs = createEncryptedPreferences()
            
            // If successful, migrate the key
            encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, currentKey).apply()
            
            // Clear fallback
            getFallbackPreferences().edit().clear().apply()
            
            // Mark that we're no longer using fallback
            isUsingFallback = false
            
            Log.d(TAG, "Successfully upgraded to encrypted storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upgrade to encrypted storage failed", e)
            false
        }
    }

    /**
     * Force reset all storage (for debugging/troubleshooting).
     * Clears both encrypted and fallback storage.
     */
    fun forceReset() {
        try {
            // Clear fallback
            getFallbackPreferences().edit().clear().apply()
            
            // Try to clear encrypted
            try {
                context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().clear().apply()
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear encrypted prefs", e)
            }
            
            // Delete encrypted file
            deleteEncryptedPrefsFile()
            
            // Reset fallback flag
            isUsingFallback = false
            
            Log.d(TAG, "Force reset completed")
        } catch (e: Exception) {
            Log.e(TAG, "Force reset failed", e)
        }
    }
}
