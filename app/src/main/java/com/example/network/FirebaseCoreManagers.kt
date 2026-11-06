package com.example.network

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.util.SecureConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==========================================
// 1. FirebaseAnalyticsManager
// ==========================================
object FirebaseAnalyticsManager {
    private const val TAG = "FirebaseAnalyticsMgr"
    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            analytics = FirebaseAnalytics.getInstance(context)
            Log.i(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Analytics", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Logged screen view event: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging screen view", e)
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            analytics?.logEvent(eventName, params)
            Log.d(TAG, "Logged custom event: $eventName with parameters: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging custom event: $eventName", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun setUserProperty(name: String, value: String) {
        try {
            analytics?.setUserProperty(name, value)
            Log.d(TAG, "Set user property: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user property: $name", e)
        }
    }
}

// ==========================================
// 2. FirebaseConnectionManager
// ==========================================
object FirebaseConnectionManager {
    private const val TAG = "FirebaseConn"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(SecureConfig.firebaseDatabaseUrl)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    // Global flag to check if the background vault is unlocked
    var isCloudPipelineReady = false
        private set

    /**
     * Executes the invisible cryptographic handshake with Firebase
     */
    fun initializeSilentCloudConnection(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            Log.d(TAG, "Existing silent session found. Pipeline Active. UID: ${currentUser.uid}")
            isCloudPipelineReady = true
            enableKeepSynced()
            onSuccess()
            return
        }

        Log.d(TAG, "Starting silent background authentication...")
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d(TAG, "Silent connection established successfully! Assigned App UID: ${user?.uid}")
                    isCloudPipelineReady = true
                    enableKeepSynced()
                    onSuccess()
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown security handshake error"
                    Log.w(TAG, "Cloud pipeline connection operating in local/offline mode: $errorMessage")
                    isCloudPipelineReady = false
                    onFailure(errorMessage)
                }
            }
    }

    /**
     * Offline Optimization: Tells Firebase to cache data locally on the tablet 
     * chip so it can survive rural route drops seamlessly.
     */
    private fun enableKeepSynced() {
        try {
            val uid = auth.currentUser?.uid ?: "anon_user"
            database.getReference("users").child(uid).keepSynced(true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set disk persistence properties: ${e.message}")
        }
    }

    /**
     * Tests full Realtime Database read/write latency and endpoint connectivity
     */
    fun testRtdbPing(onResult: (Boolean, String) -> Unit) {
        val startTime = System.currentTimeMillis()
        val pingRef = database.getReference("rtdb_pings").child(auth.currentUser?.uid ?: "anon_tester")
        val timestamp = System.currentTimeMillis()
        
        pingRef.setValue(mapOf("ping_time" to timestamp, "device" to android.os.Build.MODEL))
            .addOnSuccessListener {
                val latency = System.currentTimeMillis() - startTime
                pingRef.get().addOnSuccessListener { snapshot ->
                    onResult(true, "RTDB Live! Ping: ${latency}ms | Path: /rtdb_pings | Write/Read Verified")
                }.addOnFailureListener { e ->
                    onResult(true, "RTDB Write OK (${latency}ms), Read error: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "RTDB Write Failed: ${e.message ?: "Network timeout or permission denied"}")
            }
    }

    /**
     * Observes real-time connection status with RTDB using .info/connected
     */
    fun observeRtdbConnection(onConnectedChanged: (Boolean) -> Unit): com.google.firebase.database.ValueEventListener {
        val connectedRef = database.getReference(".info/connected")
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                onConnectedChanged(connected)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onConnectedChanged(false)
            }
        }
        connectedRef.addValueEventListener(listener)
        return listener
    }
}

// ==========================================
// 3. FirebaseRemoteConfigManager
// ==========================================
object FirebaseRemoteConfigManager {
    private const val TAG = "FirebaseRemoteConfig"

    private val _welcomeMessage = MutableStateFlow("Welcome to MD Finance!")
    val welcomeMessage = _welcomeMessage.asStateFlow()

    private val _defaultInterestRate = MutableStateFlow(10)
    val defaultInterestRate = _defaultInterestRate.asStateFlow()

    private val _enableUpiFeatures = MutableStateFlow(true)
    val enableUpiFeatures = _enableUpiFeatures.asStateFlow()

    private val _lastFetchTime = MutableStateFlow(0L)
    val lastFetchTime = _lastFetchTime.asStateFlow()

    fun initializeAndFetch() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // hourly cache
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Define default local values
            val defaultValues = mapOf(
                "welcome_message" to "Welcome to MD Finance!",
                "default_interest_rate" to 10L,
                "enable_upi_features" to true
            )
            remoteConfig.setDefaultsAsync(defaultValues)

            // Trigger fetch and activation
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Log.i(TAG, "Config fetch completed. Was updated: $updated")
                        applyConfigValues(remoteConfig)
                    } else {
                        Log.w(TAG, "Config fetch failed. Using default configuration.")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Remote Config", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun applyConfigValues(remoteConfig: FirebaseRemoteConfig) {
        try {
            _welcomeMessage.value = remoteConfig.getString("welcome_message")
            _defaultInterestRate.value = remoteConfig.getLong("default_interest_rate").toInt()
            _enableUpiFeatures.value = remoteConfig.getBoolean("enable_upi_features")
            _lastFetchTime.value = System.currentTimeMillis()
            Log.d(TAG, "Successfully updated configurations from Remote Config: ${_welcomeMessage.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing config values", e)
        }
    }
}
