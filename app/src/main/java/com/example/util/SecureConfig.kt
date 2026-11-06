package com.example.util

import com.example.BuildConfig

object SecureConfig {
    val adminUsername: String = "muneeswaran"
    val adminPassword: String = "MDb@240807"
    val firebaseDatabaseUrl: String = "https://collection-app-2007-default-rtdb.asia-southeast1.firebasedatabase.app/"
    val firebaseStorageUrl: String = "gs://collection-app-2007.firebasestorage.app"
    val googleScriptUrl: String = "https://script.google.com/macros/s/AKfycbw7jFaMUQN1ep8CkEx6pXCK3UOvEHmO4eYMf6U9Q5liKhC3e0Xc7Xs392yp6G-EvSRW/exec"
    val geminiApiKey: String = BuildConfig.GEMINI_API_KEY
}
