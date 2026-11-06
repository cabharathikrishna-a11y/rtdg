package com.example.data

import android.content.Context
import java.security.SecureRandom
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object DatabaseProvider {
    @Volatile
    private var cachedDbPassword: ByteArray? = null

    fun getDbPassword(context: Context): ByteArray {
        cachedDbPassword?.let { return it }
        val keyBytes = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val prefs = EncryptedSharedPreferences.create(
                context,
                "weekly_finance_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            var storedKey = prefs.getString("db_key", null)
            if (storedKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                storedKey = Base64.encodeToString(randomBytes, Base64.DEFAULT)
                prefs.edit().putString("db_key", storedKey).apply()
            }
            Base64.decode(storedKey, Base64.DEFAULT)
        } catch (e: Throwable) {
            android.util.Log.e("DatabaseProvider", "Failed to use EncryptedSharedPreferences, falling back", e)
            val prefs = context.getSharedPreferences("weekly_finance_fallback_secure_prefs", Context.MODE_PRIVATE)
            var storedKey = prefs.getString("db_key", null)
            if (storedKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                storedKey = Base64.encodeToString(randomBytes, Base64.DEFAULT)
                prefs.edit().putString("db_key", storedKey).apply()
            }
            Base64.decode(storedKey, Base64.DEFAULT)
        }
        cachedDbPassword = keyBytes
        return keyBytes
    }

    fun getDatabase(context: Context): AppDatabase {
        val prefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
        val isDemoMode = prefs.getBoolean("is_demo_mode", false)
        val currentRole = prefs.getString("current_role", "USER") ?: "USER"
        val isReadOnlyUser = currentRole == "USER"
        return AppDatabase.getDatabase(context, isDemoMode, isReadOnlyUser, getDbPassword(context))
    }
}
