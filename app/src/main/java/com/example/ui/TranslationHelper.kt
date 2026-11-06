package com.example.ui

import android.content.res.Configuration
import java.util.Locale

object AppContextHolder {
    var context: android.content.Context? = null
}

/**
 * Dynamic translator helper.
 * Resolves translation keys dynamically from the compiled strings.xml resources 
 * of the currently selected configuration language.
 */
fun translate(key: String, language: String): String {
    val trimmedKey = key.trim()
    val appCtx = AppContextHolder.context
    if (appCtx != null) {
        try {
            val locale = when (language) {
                "Tamil" -> Locale("ta")
                "Hindi" -> Locale("hi")
                "Telugu" -> Locale("te")
                "Spanish" -> Locale("es")
                "French" -> Locale("fr")
                else -> Locale("en")
            }
            val config = Configuration(appCtx.resources.configuration)
            config.setLocale(locale)
            val localizedContext = appCtx.createConfigurationContext(config)
            
            val cleanKey = trimmedKey
                .replace("&", "and")
                .replace(" ", "_")
                .replace("-", "_")
                .replace("/", "_")
                .lowercase()
                .filter { it.isLetterOrDigit() || it == '_' }
                
            val prefixKey = if (cleanKey.isNotEmpty() && cleanKey[0].isDigit()) {
                "_$cleanKey"
            } else {
                cleanKey
            }

            val finalKey = if (prefixKey.isEmpty()) "empty_key" else prefixKey

            val resId = localizedContext.resources.getIdentifier(finalKey, "string", localizedContext.packageName)
            if (resId != 0) {
                return localizedContext.resources.getString(resId)
            }
        } catch (e: Exception) {
            // fallback to key
        }
    }
    return trimmedKey
}
