package com.example.network

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.SecureConfig
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==========================================
// 1. UpdateStatus Enum
// ==========================================
enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

// ==========================================
// 2. FirebaseUpdateManager
// ==========================================
object FirebaseUpdateManager {
    private const val TAG = "FirebaseUpdate"
    private var enqueuedDownloadId: Long = -1L

    // Live observable state flow for real-time UI tracking
    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _latestVersionCode = MutableStateFlow(1L)
    val latestVersionCode: StateFlow<Long> = _latestVersionCode.asStateFlow()

    private val _latestVersionName = MutableStateFlow("")
    val latestVersionName: StateFlow<String> = _latestVersionName.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    /**
     * Checks if a download ID is currently running or pending in system DownloadManager
     */
    private fun isDownloadInProgress(context: Context, downloadId: Long): Boolean {
        if (downloadId == -1L) return false
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIdx != -1) {
                        val status = cursor.getInt(statusIdx)
                        return status == DownloadManager.STATUS_RUNNING || 
                               status == DownloadManager.STATUS_PENDING || 
                               status == DownloadManager.STATUS_PAUSED
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query download status for ID $downloadId", e)
        }
        return false
    }

    /**
     * Deletes obsolete downloaded APK update files from both cache and external downloads
     */
    fun cleanupObsoleteApks(context: Context) {
        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
        } catch (e: Exception) {
            1L
        }

        val prefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)

        // Clear obsolete preference entries
        val downloadedCode = prefs.getLong("downloaded_version_code", -1L)
        if (downloadedCode != -1L && downloadedCode <= currentVersionCode) {
            prefs.edit().remove("downloaded_version_code").apply()
        }
        val downloadingCode = prefs.getLong("downloading_version_code", -1L)
        if (downloadingCode != -1L && downloadingCode <= currentVersionCode) {
            prefs.edit().remove("downloading_version_code").apply()
        }

        // Delete any leftover apk files from both cache and external downloads
        val dirs = listOf(
            context.cacheDir,
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        )

        for (dir in dirs) {
            if (dir != null && dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("mdfinance-update-") && file.name.endsWith(".apk")) {
                        try {
                            val versionStr = file.name
                                .substringAfter("mdfinance-update-")
                                .substringBefore(".apk")
                            val version = versionStr.toLongOrNull()
                            if (version != null && version <= currentVersionCode) {
                                if (file.delete()) {
                                    Log.i(TAG, "Successfully deleted obsolete/installed APK: ${file.absolutePath}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete old APK: ${file.absolutePath}", e)
                        }
                    }
                }
            }
        }
    }

    fun checkForCloudUpdates(context: Context, manualCheck: Boolean = false) {
        // Run cleanup on every check
        try {
            cleanupObsoleteApks(context)
        } catch (e: Exception) {
            Log.e(TAG, "Periodic obsolete APK cleanup failed", e)
        }

        val prefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
        
        // Critical check: updates must ONLY happen when user is logged in
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) {
            Log.i(TAG, "User is not logged in. Skipping update check.")
            _updateStatus.value = UpdateStatus.IDLE
            return
        }

        // Avoid triggering duplicate download if one is already in progress
        val downloadingCode = prefs.getLong("downloading_version_code", -1L)
        val savedDownloadId = prefs.getLong("enqueued_download_id", -1L)
        if (downloadingCode != -1L && isDownloadInProgress(context, savedDownloadId)) {
            Log.i(TAG, "Update download is already in progress for build v$downloadingCode. Skipping check.")
            _updateStatus.value = UpdateStatus.DOWNLOADING
            _latestVersionCode.value = downloadingCode
            return
        }

        val pauseUpdates = prefs.getBoolean("pause_updates_enabled", false)
        if (pauseUpdates && !manualCheck) {
            Log.i(TAG, "Background update check paused by preference.")
            return
        }

        val autoUpdate = prefs.getBoolean("auto_update_enabled", true)
        if (!autoUpdate && !manualCheck) {
            Log.i(TAG, "Automatic updates are disabled. Skipping update check.")
            return
        }

        // Set state to checking
        _updateStatus.value = UpdateStatus.CHECKING
        _updateError.value = null

        val database = try {
            FirebaseDatabase.getInstance(SecureConfig.firebaseDatabaseUrl)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
        val configRef = database.getReference("update_config")

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
        } catch (e: Exception) {
            1L
        }

        configRef.get().addOnSuccessListener { snapshot ->
            val latestCode = snapshot.child("versionId").getValue(Long::class.java)
                ?: snapshot.child("versionCode").getValue(Long::class.java)
                ?: snapshot.child("version_code").getValue(Long::class.java)
                ?: currentVersionCode

            val latestName = snapshot.child("versionName").getValue(String::class.java)
                ?: snapshot.child("version_name").getValue(String::class.java)
                ?: "1.0.$latestCode"

            val apkDownloadUrl = snapshot.child("apkFileId").getValue(String::class.java)
                ?: snapshot.child("apkUrl").getValue(String::class.java)
                ?: snapshot.child("apkFileUrl").getValue(String::class.java)
                ?: snapshot.child("downloadUrl").getValue(String::class.java)
                ?: snapshot.child("url").getValue(String::class.java)
                ?: ""

            _latestVersionCode.value = latestCode
            _latestVersionName.value = latestName
            
            val isUpdateAvailable = latestCode > currentVersionCode && apkDownloadUrl.isNotEmpty()
            if (isUpdateAvailable) {
                Log.i(TAG, "New Update Detected! Server: v$latestCode, Local: v$currentVersionCode")
                
                // Check if we have already downloaded this specific update file
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mdfinance-update-$latestCode.apk")
                val downloadedCode = prefs.getLong("downloaded_version_code", -1L)

                if (file.exists() && downloadedCode == latestCode) {
                    _updateStatus.value = UpdateStatus.DOWNLOADED
                    Log.i(TAG, "Update file already downloaded and ready: v$latestCode")
                } else {
                    _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
                    Toast.makeText(context, "New Update Detected! Creating emergency backups...", Toast.LENGTH_LONG).show()

                    // Perform robust backup before starting download
                    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    coroutineScope.launch {
                        try {
                            val db = com.example.data.DatabaseProvider.getDatabase(context)
                            val customersList = db.collectionDao().getAllCustomersOnce()
                            val loanCyclesList = db.collectionDao().getAllLoanCyclesOnce()
                            val paymentsList = db.collectionDao().getAllPaymentsOnce()
                            val cashBalanceLogsList = db.collectionDao().getAllCashBalanceLogsOnce()

                            val csvString = com.example.util.CsvBackupHelper.generateCsvString(
                                customers = customersList,
                                loanCycles = loanCyclesList,
                                payments = paymentsList,
                                dayFilter = "ALL",
                                cashBalanceLogs = cashBalanceLogsList
                            )

                            val backupDir = File(context.filesDir, "update_backups")
                            if (!backupDir.exists()) {
                                backupDir.mkdirs()
                            }
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            val timestamp = sdf.format(java.util.Date())
                            val backupFile = File(backupDir, "finance_ALL_backup_before_update_$timestamp.csv")
                            backupFile.writeText(csvString, Charsets.UTF_8)
                            Log.i(TAG, "Backup successfully saved locally to: ${backupFile.absolutePath}")

                            val latestBackupFile = File(context.filesDir, "finance_ALL_latest_backup.csv")
                            latestBackupFile.writeText(csvString, Charsets.UTF_8)

                            // Upload backup securely to RTDB
                            val rtdb = FirebaseDatabase.getInstance(SecureConfig.firebaseDatabaseUrl)
                            val ref = rtdb.getReference("ledger_csv")
                            val task = ref.setValue(csvString)
                            com.google.android.gms.tasks.Tasks.await(task)
                            Log.i(TAG, "Cloud ledger backup successfully uploaded and synchronized.")

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "Backups finalized! Launching automatic background downloader...", Toast.LENGTH_SHORT).show()
                                executeApkDownload(context.applicationContext, apkDownloadUrl, latestCode)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Backup procedure failed: ${e.message}", e)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(context, "Backup failed but proceeding with update...", Toast.LENGTH_SHORT).show()
                                executeApkDownload(context.applicationContext, apkDownloadUrl, latestCode)
                            }
                        }
                    }
                }
            } else {
                Log.i(TAG, "Application is fully up to date.")
                _updateStatus.value = UpdateStatus.UP_TO_DATE
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to read update config: ${e.message}")
            _updateStatus.value = UpdateStatus.FAILED
            _updateError.value = e.message
        }
    }

    fun setDownloadedState(latestCode: Long) {
        _updateStatus.value = UpdateStatus.DOWNLOADED
    }

    fun setFailedState(errorMsg: String?) {
        _updateStatus.value = UpdateStatus.FAILED
        _updateError.value = errorMsg
    }

    private fun executeApkDownload(context: Context, url: String, latestCode: Long) {
        _updateStatus.value = UpdateStatus.DOWNLOADING
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mdfinance-update-$latestCode.apk")
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("MD Finance Update")
                .setDescription("Downloading update version v$latestCode...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            enqueuedDownloadId = downloadId
            
            val sharedPrefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putLong("enqueued_download_id", downloadId)
                .putLong("downloading_version_code", latestCode)
                .apply()

            Toast.makeText(context, "Download started! Progress is in the notification bar.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule download", e)
            _updateStatus.value = UpdateStatus.FAILED
            _updateError.value = e.message
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Called by UpdateDownloadReceiver when DownloadManager finishes the update download.
     * Centralized download complete logic.
     */
    fun onDownloadComplete(context: Context, downloadId: Long, latestCode: Long) {
        val sharedPrefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
        
        // 1. Save successfully downloaded code to SharedPreferences
        sharedPrefs.edit().putLong("downloaded_version_code", latestCode).apply()

        // 2. Set StateFlow state if app is running
        _updateStatus.value = UpdateStatus.DOWNLOADED

        // 3. Copy downloaded APK to internal cache for robust installation on Android 10+ (Scoped Storage)
        copyDownloadToCache(context, downloadId, latestCode)

        Toast.makeText(context, "Download Completed! Opening update installer...", Toast.LENGTH_LONG).show()

        // 4. Trigger actual installation
        triggerInstall(context, latestCode)
    }

    fun copyDownloadToCache(context: Context, downloadId: Long, latestCode: Long): Boolean {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUri = downloadManager.getUriForDownloadedFile(downloadId)
            if (downloadUri != null) {
                context.contentResolver.openInputStream(downloadUri)?.use { inputStream ->
                    val cacheFile = File(context.cacheDir, "mdfinance-update-$latestCode.apk")
                    cacheFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Log.i(TAG, "Successfully copied download to internal cache: ${cacheFile.absolutePath}")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy downloaded file via URI, trying fallback to file copy", e)
        }
        
        // Fallback: Copy directly from the expected external files download path
        try {
            val externalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mdfinance-update-$latestCode.apk")
            if (externalFile.exists()) {
                val cacheFile = File(context.cacheDir, "mdfinance-update-$latestCode.apk")
                externalFile.inputStream().use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Successfully copied download to internal cache via file fallback.")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "File copy fallback failed", e)
        }
        return false
    }

    fun triggerInstall(context: Context, latestCode: Long) {
        val appContext = context.applicationContext
        
        // 1. Try to find the file in cacheDir (which is the most robust location for Android 10+)
        var file = File(appContext.cacheDir, "mdfinance-update-$latestCode.apk")
        
        if (!file.exists()) {
            // Fallback: look in external downloads dir
            val externalFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mdfinance-update-$latestCode.apk")
            if (externalFile.exists()) {
                try {
                    // Copy to cacheDir for robust package installation
                    externalFile.inputStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i(TAG, "Copied APK from external files to cache dir for installation.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy APK to cache directory, trying directly from external files", e)
                    file = externalFile
                }
            }
        }

        if (file.exists()) {
            try {
                // Save the running firebase version so we don't get stuck in an install loop
                val sharedPrefs = appContext.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putLong("running_firebase_version", latestCode).apply()

                // Check and request "Install Unknown Apps" permission on Android 8.0+ (Oreo+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!appContext.packageManager.canRequestPackageInstalls()) {
                        Log.w(TAG, "Install Unknown Apps permission not granted. Launching system settings...")
                        Toast.makeText(appContext, "Please enable 'Install Unknown Apps' permission to apply the update.", Toast.LENGTH_LONG).show()
                        val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${appContext.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        appContext.startActivity(settingsIntent)
                        return
                    }
                }

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    val apkUri = androidx.core.content.FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                appContext.startActivity(installIntent)
                Log.i(TAG, "Installation activity started successfully for: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start installation activity", e)
                Toast.makeText(appContext, "Package installer error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(appContext, "Update APK file not found on device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownloadedUpdate(context: Context) {
        val prefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
        
        // Cancel any active download in DownloadManager
        val savedDownloadId = prefs.getLong("enqueued_download_id", -1L)
        if (savedDownloadId != -1L) {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.remove(savedDownloadId)
                Log.i(TAG, "Cancelled active download ID: $savedDownloadId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove download ID $savedDownloadId", e)
            }
        }

        // Remove all preferences
        prefs.edit()
            .remove("downloaded_version_code")
            .remove("downloading_version_code")
            .remove("enqueued_download_id")
            .apply()

        // Delete cache apk files
        context.cacheDir?.listFiles()?.forEach { 
            if (it.name.startsWith("mdfinance-update-") && it.name.endsWith(".apk")) {
                it.delete()
            }
        }
        
        // Delete external downloads apk files
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        dir?.listFiles()?.forEach { 
            if (it.name.startsWith("mdfinance-update-") && it.name.endsWith(".apk")) {
                it.delete()
            }
        }
        
        _updateStatus.value = UpdateStatus.IDLE
        Toast.makeText(context, "Downloaded update deleted.", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 3. UpdateDownloadReceiver
// ==========================================
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val sharedPrefs = context.getSharedPreferences("weekly_finance_prefs", Context.MODE_PRIVATE)
            
            // Critical check: Only process update installation if the user is signed in
            val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
            if (!isLoggedIn) {
                Log.i("UpdateDownloadReceiver", "User is not logged in. Discarding update install.")
                return
            }

            val savedDownloadId = sharedPrefs.getLong("enqueued_download_id", -1L)
            val latestCode = sharedPrefs.getLong("downloading_version_code", -1L)

            if (id == savedDownloadId && id != -1L && latestCode != -1L) {
                Log.i("UpdateDownloadReceiver", "Persisted Update download completed! Delegating to FirebaseUpdateManager for v$latestCode...")
                FirebaseUpdateManager.onDownloadComplete(context, id, latestCode)
            }
        }
    }
}

// ==========================================
// 4. MyFirebaseMessagingService
// ==========================================
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New Firebase Messaging token generated: $token")
        saveTokenToPreferences(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Log the message to Firebase Analytics
        FirebaseAnalyticsManager.logEvent("fcm_message_received")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val title = it.title ?: "MD Finance Notification"
            val body = it.body ?: ""
            showLocalNotification(title, body)
        } ?: run {
            // Check if message contains data payload
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "MD Finance Alert"
                val body = remoteMessage.data["body"] ?: ""
                showLocalNotification(title, body)
            }
        }
    }

    private fun showLocalNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create Notification Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cloud Messages & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows live push messages and reminders synced from Firebase Cloud"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Find the default application launcher icon dynamically
        val iconResId = applicationInfo.icon

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    companion object {
        private const val TAG = "MyFirebaseMessaging"
        private const val CHANNEL_ID = "fcm_cloud_alerts_channel"
        private const val PREFS_NAME = "weekly_finance_prefs"
        private const val KEY_FCM_TOKEN = "firebase_fcm_token"

        fun getSavedFcmToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
        }
    }
}
