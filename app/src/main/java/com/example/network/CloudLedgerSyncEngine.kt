package com.example.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.CashBalanceLog
import com.example.data.Customer
import com.example.data.LoanCycle
import com.example.data.WeeklyPayment
import com.example.util.SecureConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PendingOfflineEdit(
    val entityType: String, // "CUSTOMER", "LOAN_CYCLE", "WEEKLY_PAYMENT", "CASH_LOG"
    val entityUuid: String,
    val customerUuid: String,
    val collectionDay: String,
    val action: String,     // "UPSERT", "DELETE"
    val timestamp: Long,
    val summary: String = ""
)

data class ArchivedPastLoanItem(
    val loanUuid: String,
    val customerUuid: String,
    val customerCode: String,
    val customerName: String,
    val fromDate: Long,
    val toDate: Long,
    val loanAmount: Double,
    val interestAmount: Double,
    val deduction: Double,
    val paidAmount: Double,
    val storagePath: String,
    val markdownContent: String
)

object CloudLedgerSyncEngine {
    private const val TAG = "CloudLedgerSyncEngine"
    private const val PREFS_NAME = "weekly_finance_prefs"
    private const val KEY_LAST_SYNCED_AT = "firestore_last_synced_at_ms"
    private const val KEY_INITIAL_MIGRATION_DONE = "firestore_initial_migration_v2_done"
    private const val KEY_PENDING_OFFLINE_EDITS = "firestore_pending_offline_edits_json"
    private const val KEY_LAST_ARCHIVE_CHECK_AT = "firestore_last_archive_check_ms"
    private const val KEY_LEGACY_RTDB_PURGED = "rtdb_legacy_giant_nodes_purged_v2"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushMutex = Mutex()
    private val reconcileMutex = Mutex()
    private var debounceFlushJob: Job? = null

    private val _onlinePeerCount = MutableStateFlow(0)
    val onlinePeerCount = _onlinePeerCount.asStateFlow()

    private val _pendingOfflineCount = MutableStateFlow(0)
    val pendingOfflineCount = _pendingOfflineCount.asStateFlow()

    private val _isFetchingPastArchive = MutableStateFlow(false)
    val isFetchingPastArchive = _isFetchingPastArchive.asStateFlow()

    private var presenceListener: ValueEventListener? = null
    private var syncTriggerListener: ValueEventListener? = null
    private var lastAppliedTriggerTime: Long = 0L
    private var isPresenceInitialized = false
    private var lastPresenceStatusTime = 0L

    private fun getRtdb(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(SecureConfig.firebaseDatabaseUrl)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    private fun getFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    private fun getStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    /**
     * Purges any unacknowledged oversized RTDB writes (`day_branches` / `ledger_csv` from v49)
     * from memory and local disk persistence so `PersistentConnectionImpl.restoreState()`
     * never triggers `JsonMapper.serializeJsonValue` `OutOfMemoryError` (37.7 MB allocation).
     */
    fun purgeLegacyRtdbQueuesAndNodes(context: Context, removeRemoteLegacyBranches: Boolean = false) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean("rtdb_local_disk_cache_purged_v2", false)) {
                try {
                    context.databaseList()?.forEach { dbName ->
                        if (dbName.contains("firebasedatabase", ignoreCase = true) ||
                            dbName.contains("default-rtdb", ignoreCase = true)
                        ) {
                            context.deleteDatabase(dbName)
                            Log.i(TAG, "Deleted legacy RTDB persistence database: $dbName")
                        }
                    }
                } catch (_: Exception) {}
                prefs.edit().putBoolean("rtdb_local_disk_cache_purged_v2", true).apply()
            }

            val rtdb = getRtdb()
            try {
                rtdb.purgeOutstandingWrites()
            } catch (_: Exception) {}

            if (removeRemoteLegacyBranches && !prefs.getBoolean(KEY_LEGACY_RTDB_PURGED, false)) {
                rtdb.getReference("day_branches").removeValue()
                rtdb.getReference("ledger_csv").removeValue()
                rtdb.getReference("days").removeValue()
                rtdb.getReference("all_transactions").removeValue()
                rtdb.getReference("rtdb_pings").removeValue()
                prefs.edit().putBoolean(KEY_LEGACY_RTDB_PURGED, true).apply()
                Log.i(TAG, "Removed bloated legacy RTDB server nodes (/day_branches, /ledger_csv, /days, /all_transactions)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy RTDB purge notice: ${e.message}")
        }
    }

    private fun formatTimestamp(ms: Long): String {
        if (ms <= 0L) return "Never"
        return SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date(ms))
    }

    fun getDeviceId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "dev"
        } catch (_: Exception) {
            "dev"
        }
        val model = "${Build.MANUFACTURER}_${Build.MODEL}".replace(Regex("[^a-zA-Z0-9_]"), "_")
        return "${model}_${androidId.take(8)}"
    }

    fun sanitizeGmailKey(email: String): String {
        val clean = email.trim().lowercase(Locale.US).ifBlank { "muneeswaran45@gmail.com" }
        return clean.replace(".", "_dot_").replace("@", "_at_").replace(Regex("[#$\\[\\]/]"), "_")
    }

    fun getCurrentUserEmail(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("current_email", "")?.trim().orEmpty()
        return saved.ifBlank { "muneeswaran45@gmail.com" }
    }

    fun getCurrentUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("current_user", "")?.trim().orEmpty()
        return saved.ifBlank { "Muneeswaran" }
    }

    /**
     * Registers this device under RTDB `devices/{gmailKey}/{deviceId}` with automatic
     * `.info/connected` + `onDisconnect()` online/offline status switching, and listens ONLY
     * to the tiny `sync_triggers/latest` node (~150 bytes) for live delta updates.
     * Does NOT attach a continuous listener to `/devices` (which previously caused an echo loop
     * re-downloading the entire devices tree on every edit).
     */
    fun initializeDevicePresenceAndTriggers(
        context: Context,
        db: AppDatabase,
        onStatusUpdate: (String) -> Unit = {}
    ) {
        try {
            refreshPendingCount(context)
            purgeLegacyRtdbQueuesAndNodes(context, removeRemoteLegacyBranches = false)
            if (isPresenceInitialized && presenceListener != null && syncTriggerListener != null) {
                return
            }

            val rtdb = getRtdb()
            val email = getCurrentUserEmail(context)
            val username = getCurrentUsername(context)
            val gmailKey = sanitizeGmailKey(email)
            val myDeviceId = getDeviceId(context)
            val deviceRef = rtdb.getReference("devices").child(gmailKey).child(myDeviceId)

            // 1. Bind .info/connected for automatic online/offline presence
            val connectedRef = rtdb.getReference(".info/connected")
            presenceListener?.let { connectedRef.removeEventListener(it) }
            val pListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    val now = System.currentTimeMillis()
                    if (connected) {
                        val disconnectPayload = mapOf(
                            "status" to "offline",
                            "lastActiveSessionTime" to now,
                            "lastActiveFormatted" to formatTimestamp(now)
                        )
                        deviceRef.onDisconnect().updateChildren(disconnectPayload)

                        val onlinePayload = mutableMapOf<String, Any>(
                            "username" to username,
                            "userLoginId" to email,
                            "deviceId" to myDeviceId,
                            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                            "status" to "online",
                            "lastActiveSessionTime" to now,
                            "lastActiveFormatted" to formatTimestamp(now)
                        )
                        deviceRef.updateChildren(onlinePayload)

                        // Attach FCM token once if not already saved
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        if (!prefs.getBoolean("fcm_token_synced_to_rtdb", false)) {
                            try {
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                    if (!token.isNullOrBlank()) {
                                        deviceRef.child("fcmToken").setValue(token)
                                        prefs.edit().putBoolean("fcm_token_synced_to_rtdb", true).apply()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            presenceListener = pListener
            connectedRef.addValueEventListener(pListener)

            // 2. Watch `sync_triggers/latest` (~150 bytes) for instant single-document delta pulls when online peers edit data
            val triggerRef = rtdb.getReference("sync_triggers").child("latest")
            syncTriggerListener?.let { triggerRef.removeEventListener(it) }
            val tListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val sourceDevice = snapshot.child("sourceDeviceId").getValue(String::class.java).orEmpty()
                    val triggerTime = snapshot.child("lastModified").getValue(Long::class.java) ?: 0L
                    if (sourceDevice == myDeviceId || triggerTime <= lastAppliedTriggerTime) return
                    lastAppliedTriggerTime = triggerTime
                    if (_onlinePeerCount.value == 0) {
                        _onlinePeerCount.value = 1
                    }

                    val entityType = snapshot.child("entityType").getValue(String::class.java).orEmpty()
                    val entityUuid = snapshot.child("entityUuid").getValue(String::class.java).orEmpty()
                    val action = snapshot.child("action").getValue(String::class.java) ?: "UPSERT"

                    if (entityType.isNotBlank() && entityUuid.isNotBlank()) {
                        scope.launch {
                            try {
                                pullSingleEntityFromFirestore(context, db, entityType, entityUuid, action)
                                onStatusUpdate("Synced live change ($entityType)")
                            } catch (e: Exception) {
                                Log.w(TAG, "Delta trigger pull failed: ${e.message}")
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            syncTriggerListener = tListener
            triggerRef.addValueEventListener(tListener)
            isPresenceInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device presence: ${e.message}")
        }
    }

    /**
     * Marks this device as `"online"` or `"offline"` on lifecycle transitions (throttled to avoid spam).
     */
    fun updateDeviceOnlineStatus(context: Context, isOnline: Boolean) {
        try {
            val now = System.currentTimeMillis()
            if (isOnline && (now - lastPresenceStatusTime) < 30_000L) return
            lastPresenceStatusTime = now
            val rtdb = getRtdb()
            val email = getCurrentUserEmail(context)
            val gmailKey = sanitizeGmailKey(email)
            val myDeviceId = getDeviceId(context)
            val updates = mapOf(
                "status" to if (isOnline) "online" else "offline",
                "lastActiveSessionTime" to now,
                "lastActiveFormatted" to formatTimestamp(now)
            )
            rtdb.getReference("devices").child(gmailKey).child(myDeviceId).updateChildren(updates)
        } catch (_: Exception) {}
    }

    // =========================================================================
    // OFFLINE EDIT QUEUE MANAGEMENT (COALESCED & DEBOUNCED)
    // =========================================================================

    private fun getPendingOfflineEdits(context: Context): MutableList<PendingOfflineEdit> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PENDING_OFFLINE_EDITS, "[]") ?: "[]"
        val list = mutableListOf<PendingOfflineEdit>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PendingOfflineEdit(
                        entityType = obj.optString("entityType"),
                        entityUuid = obj.optString("entityUuid"),
                        customerUuid = obj.optString("customerUuid"),
                        collectionDay = obj.optString("collectionDay"),
                        action = obj.optString("action", "UPSERT"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        summary = obj.optString("summary", "")
                    )
                )
            }
        } catch (_: Exception) {}
        _pendingOfflineCount.value = list.size
        return list
    }

    private fun savePendingOfflineEdits(context: Context, edits: List<PendingOfflineEdit>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        edits.forEach { edit ->
            val obj = JSONObject().apply {
                put("entityType", edit.entityType)
                put("entityUuid", edit.entityUuid)
                put("customerUuid", edit.customerUuid)
                put("collectionDay", edit.collectionDay)
                put("action", edit.action)
                put("timestamp", edit.timestamp)
                put("summary", edit.summary)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PENDING_OFFLINE_EDITS, arr.toString()).apply()
        _pendingOfflineCount.value = edits.size
    }

    fun refreshPendingCount(context: Context) {
        getPendingOfflineEdits(context)
    }

    @Synchronized
    fun enqueueOfflineOrLiveEdit(
        context: Context,
        db: AppDatabase,
        entityType: String,
        entityUuid: String,
        customerUuid: String,
        collectionDay: String,
        action: String = "UPSERT",
        timestamp: Long = System.currentTimeMillis(),
        summary: String = ""
    ) {
        if (entityUuid.isBlank()) return
        val current = getPendingOfflineEdits(context).filterNot {
            it.entityType == entityType && it.entityUuid == entityUuid
        }.toMutableList()
        current.add(
            PendingOfflineEdit(
                entityType = entityType,
                entityUuid = entityUuid,
                customerUuid = customerUuid,
                collectionDay = collectionDay,
                action = action,
                timestamp = timestamp,
                summary = summary
            )
        )
        savePendingOfflineEdits(context, current)

        // Debounce rapid consecutive edits (e.g. creating customer + loan or route order swaps) into a single batched flush
        debounceFlushJob?.cancel()
        debounceFlushJob = scope.launch {
            delay(800L)
            try {
                flushPendingEditsWithConflictCheck(context, db)
            } catch (e: Exception) {
                Log.d(TAG, "Queued edit for offline sync ($entityType / $entityUuid): ${e.message}")
            }
        }
    }

    // =========================================================================
    // 5-SCENARIO QUOTA-MINIMIZING RECONCILIATION ENGINE
    // =========================================================================

    /**
     * Executes the 5-scenario permutation check on app launch, login, or reconnection:
     * 1. Checks RTDB `devices` for any other device with `lastModified > myLastSyncedAt`.
     * 2. Short-circuits with 0 Firestore reads if no other device modified data!
     * 3. Otherwise queries `audit_logs` > `myLastSyncedAt`, resolves timestamp conflicts
     *    against local offline edits, and reads/writes only the exact changed UUIDs.
     */
    suspend fun reconcileWithCloud(
        context: Context,
        db: AppDatabase,
        forceFullMigrationCheck: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val myDeviceId = getDeviceId(context)
            val email = getCurrentUserEmail(context)
            val myLastSyncedAt = prefs.getLong(KEY_LAST_SYNCED_AT, 0L)
            val isInitialMigrationDone = prefs.getBoolean(KEY_INITIAL_MIGRATION_DONE, false)

            val rtdb = getRtdb()
            val firestore = getFirestore()

            // Step 0: Only run full UUID reconciliation if initial migration was never done OR user explicitly tapped Force Full Resync
            if (!isInitialMigrationDone || forceFullMigrationCheck) {
                onProgress("Synchronizing local ledger with Cloud Firestore...")
                val summary = performFullBidirectionalUuidReconciliation(context, db, onProgress)
                prefs.edit()
                    .putBoolean(KEY_INITIAL_MIGRATION_DONE, true)
                    .putLong(KEY_LAST_SYNCED_AT, System.currentTimeMillis())
                    .apply()
                purgeLegacyRtdbQueuesAndNodes(context, removeRemoteLegacyBranches = true)
                return@withLock summary
            }

            purgeLegacyRtdbQueuesAndNodes(context, removeRemoteLegacyBranches = true)

            // Step 1: Check lightweight RTDB `devices` branch once to see if ANY other device modified data after myLastSyncedAt
            onProgress("Checking connected device timestamps...")
            val devicesSnap = Tasks.await(rtdb.getReference("devices").get(), 6, TimeUnit.SECONDS)
            var maxOtherDeviceModified = 0L
            var peersOnline = 0

            for (gSnap in devicesSnap.children) {
                for (dSnap in gSnap.children) {
                    val devId = dSnap.child("deviceId").getValue(String::class.java) ?: dSnap.key.orEmpty()
                    val devStatus = dSnap.child("status").getValue(String::class.java) ?: "offline"
                    val devLastMod = dSnap.child("lastModified").getValue(Long::class.java) ?: 0L
                    if (devId != myDeviceId) {
                        if (devStatus.equals("online", ignoreCase = true)) {
                            peersOnline++
                        }
                        if (devLastMod > maxOtherDeviceModified) {
                            maxOtherDeviceModified = devLastMod
                        }
                    }
                }
            }
            _onlinePeerCount.value = peersOnline

            val pendingEdits = getPendingOfflineEdits(context)
            val otherDeviceModifiedSinceLastSync = maxOtherDeviceModified > myLastSyncedAt

            // PERMUTATION 1: No other device modified data AND no pending local offline edits -> 0 Firestore reads/writes!
            if (!otherDeviceModifiedSinceLastSync && pendingEdits.isEmpty()) {
                val totalCust = db.collectionDao().getAllCustomersOnce().size
                return@withLock "Synced ($totalCust borrowers • 0 cloud reads needed)"
            }

            // PERMUTATION 2: No other device modified data, BUT this device has pending offline edits -> 0 audit reads, direct batch push!
            if (!otherDeviceModifiedSinceLastSync && pendingEdits.isNotEmpty()) {
                onProgress("Uploading ${pendingEdits.size} local changes to Cloud Firestore...")
                flushPendingEditsWithConflictCheck(context, db)
                val now = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_SYNCED_AT, now).apply()
                val totalCust = db.collectionDao().getAllCustomersOnce().size
                return@withLock "Synced (${pendingEdits.size} local changes uploaded • $totalCust borrowers)"
            }

            // PERMUTATION 3 & 4: Another device HAS modified data since myLastSyncedAt!
            onProgress("Reading Firestore Audit Log for delta updates...")
            val auditQuerySnap = Tasks.await(
                firestore.collection("audit_logs")
                    .whereGreaterThan("timestamp", myLastSyncedAt)
                    .get(),
                10,
                TimeUnit.SECONDS
            )

            data class CloudAuditPointer(
                val entityType: String,
                val entityUuid: String,
                val action: String,
                val timestamp: Long,
                val deviceId: String
            )

            val latestCloudChanges = mutableMapOf<String, CloudAuditPointer>()
            var highestAuditTime = maxOf(myLastSyncedAt, maxOtherDeviceModified)

            for (doc in auditQuerySnap.documents) {
                val eType = doc.getString("entityType").orEmpty()
                val eUuid = doc.getString("entityUuid").orEmpty()
                val act = doc.getString("action") ?: "UPSERT"
                val ts = doc.getLong("timestamp") ?: 0L
                val devId = doc.getString("deviceId").orEmpty()
                if (ts > highestAuditTime) highestAuditTime = ts
                if (devId == myDeviceId || eType.isBlank() || eUuid.isBlank()) continue

                val key = "$eType|$eUuid"
                val existing = latestCloudChanges[key]
                if (existing == null || ts > existing.timestamp) {
                    latestCloudChanges[key] = CloudAuditPointer(eType, eUuid, act, ts, devId)
                }
            }

            // PERMUTATION 4: Reconcile any pending local offline edits against incoming cloud audit entries
            val remainingLocalToPush = mutableListOf<PendingOfflineEdit>()
            for (localEdit in pendingEdits) {
                val key = "${localEdit.entityType}|${localEdit.entityUuid}"
                val cloudEntry = latestCloudChanges[key]
                if (cloudEntry == null || localEdit.timestamp >= cloudEntry.timestamp) {
                    remainingLocalToPush.add(localEdit)
                    latestCloudChanges.remove(key)
                } else {
                    Log.i(TAG, "Cloud audit entry for $key (${cloudEntry.timestamp}) is newer than offline edit (${localEdit.timestamp}); adopting cloud version.")
                }
            }

            if (remainingLocalToPush.isNotEmpty()) {
                onProgress("Committing ${remainingLocalToPush.size} newer local edits to Firestore...")
                savePendingOfflineEdits(context, remainingLocalToPush)
                flushPendingEditsWithConflictCheck(context, db)
            } else if (pendingEdits.isNotEmpty()) {
                savePendingOfflineEdits(context, emptyList())
            }

            val orderedCloudChanges = latestCloudChanges.values.sortedBy {
                when (it.entityType) {
                    "CUSTOMER" -> 1
                    "LOAN_CYCLE" -> 2
                    "WEEKLY_PAYMENT" -> 3
                    else -> 4
                }
            }

            if (orderedCloudChanges.isNotEmpty()) {
                onProgress("Applying ${orderedCloudChanges.size} delta updates from Cloud...")
                for (change in orderedCloudChanges) {
                    pullSingleEntityFromFirestore(context, db, change.entityType, change.entityUuid, change.action)
                }
            }

            val finalSyncStamp = maxOf(highestAuditTime, System.currentTimeMillis())
            prefs.edit().putLong(KEY_LAST_SYNCED_AT, finalSyncStamp).apply()
            val totalCust = db.collectionDao().getAllCustomersOnce().size
            return@withLock "Synced (${orderedCloudChanges.size} cloud updates applied • $totalCust borrowers)"
        }
    }

    /**
     * Flushes queued local edits to Firestore in a single atomic WriteBatch (0 pre-read GET overhead!),
     * records each change in `audit_logs`, updates RTDB `devices/.../lastModified`,
     * and emits a lightweight `sync_triggers/latest` event (~150 bytes).
     */
    suspend fun flushPendingEditsWithConflictCheck(
        context: Context,
        db: AppDatabase
    ) = withContext(Dispatchers.IO) {
        flushMutex.withLock {
            val pending = getPendingOfflineEdits(context)
            if (pending.isEmpty()) return@withLock

            val firestore = getFirestore()
            val rtdb = getRtdb()
            val myDeviceId = getDeviceId(context)
            val email = getCurrentUserEmail(context)
            val username = getCurrentUsername(context)
            val gmailKey = sanitizeGmailKey(email)

            val dao = db.collectionDao()
            val needCustomers = pending.any { it.entityType == "CUSTOMER" || it.entityType == "LOAN_CYCLE" || it.entityType == "WEEKLY_PAYMENT" }
            val needLoans = pending.any { it.entityType == "LOAN_CYCLE" || it.entityType == "WEEKLY_PAYMENT" }
            val needPayments = pending.any { it.entityType == "WEEKLY_PAYMENT" }
            val needCashLogs = pending.any { it.entityType == "CASH_LOG" }

            val customersByUuid = if (needCustomers) dao.getAllCustomersOnce().associateBy { it.uuid } else emptyMap()
            val customersById = if (needCustomers) customersByUuid.values.associateBy { it.id } else emptyMap()
            val loansByUuid = if (needLoans) dao.getAllLoanCyclesOnce().associateBy { it.uuid } else emptyMap()
            val loansById = if (needLoans) loansByUuid.values.associateBy { it.id } else emptyMap()
            val paymentsByUuid = if (needPayments) dao.getAllPaymentsOnce().associateBy { it.uuid } else emptyMap()
            val cashLogs = if (needCashLogs) dao.getAllCashBalanceLogsOnce() else emptyList()

            val remainingIfFailed = mutableListOf<PendingOfflineEdit>()
            var latestModificationTime = 0L
            var lastPushedEdit: PendingOfflineEdit? = null

            // Process in chunks of 150 edits (each edit writes 1 entity doc + 1 audit_log doc = 300 batch ops, well below Firestore's 500 limit)
            for (chunk in pending.chunked(150)) {
                try {
                    val batch = firestore.batch()
                    var opsInBatch = 0

                    for (edit in chunk) {
                        val collectionName = when (edit.entityType) {
                            "CUSTOMER" -> "customers"
                            "LOAN_CYCLE" -> "loan_cycles"
                            "WEEKLY_PAYMENT" -> "weekly_payments"
                            "CASH_LOG" -> "cash_balance_logs"
                            else -> "customers"
                        }
                        val docRef = firestore.collection(collectionName).document(edit.entityUuid)

                        when (edit.entityType) {
                            "CUSTOMER" -> {
                                val c = customersByUuid[edit.entityUuid]
                                if (edit.action == "DELETE" || c == null) {
                                    batch.set(
                                        docRef,
                                        mapOf(
                                            "uuid" to edit.entityUuid,
                                            "status" to "DELETED",
                                            "lastModified" to edit.timestamp,
                                            "lastModifiedFormatted" to formatTimestamp(edit.timestamp),
                                            "lastModifiedByDevice" to myDeviceId,
                                            "lastModifiedByUser" to email
                                        ),
                                        SetOptions.merge()
                                    )
                                    opsInBatch++
                                } else {
                                    batch.set(docRef, customerToFirestoreMap(c, myDeviceId, email))
                                    opsInBatch++
                                }
                            }
                            "LOAN_CYCLE" -> {
                                val l = loansByUuid[edit.entityUuid]
                                if (edit.action == "DELETE" || l == null) {
                                    batch.set(
                                        docRef,
                                        mapOf(
                                            "uuid" to edit.entityUuid,
                                            "status" to "DELETED",
                                            "lastModified" to edit.timestamp,
                                            "lastModifiedFormatted" to formatTimestamp(edit.timestamp),
                                            "lastModifiedByDevice" to myDeviceId,
                                            "lastModifiedByUser" to email
                                        ),
                                        SetOptions.merge()
                                    )
                                    opsInBatch++
                                } else {
                                    val parentCustomer = customersById[l.customerId]
                                    if (parentCustomer != null) {
                                        batch.set(docRef, loanCycleToFirestoreMap(l, parentCustomer, myDeviceId, email))
                                        opsInBatch++
                                    }
                                }
                            }
                            "WEEKLY_PAYMENT" -> {
                                val p = paymentsByUuid[edit.entityUuid]
                                if (edit.action == "DELETE" || p == null || p.status.equals("DELETED", ignoreCase = true)) {
                                    batch.set(
                                        docRef,
                                        mapOf(
                                            "uuid" to edit.entityUuid,
                                            "status" to "DELETED",
                                            "lastModified" to edit.timestamp,
                                            "lastModifiedFormatted" to formatTimestamp(edit.timestamp),
                                            "lastModifiedByDevice" to myDeviceId,
                                            "lastModifiedByUser" to email
                                        ),
                                        SetOptions.merge()
                                    )
                                    opsInBatch++
                                } else {
                                    val parentLoan = loansById[p.loanCycleId]
                                    val parentCustomer = parentLoan?.let { customersById[it.customerId] }
                                    if (parentLoan != null && parentCustomer != null) {
                                        batch.set(docRef, paymentToFirestoreMap(p, parentLoan, parentCustomer, myDeviceId, email))
                                        opsInBatch++
                                    }
                                }
                            }
                            "CASH_LOG" -> {
                                val logItem = cashLogs.find { "cash_${it.date}" == edit.entityUuid || it.id.toString() == edit.entityUuid }
                                    ?: cashLogs.maxByOrNull { it.date }
                                if (logItem != null) {
                                    batch.set(docRef, cashLogToFirestoreMap(logItem, edit.entityUuid, myDeviceId, email))
                                    opsInBatch++
                                }
                            }
                        }

                        // Write Firestore Audit Log entry in the same atomic batch
                        val logUuid = UUID.randomUUID().toString()
                        val auditMap = mapOf(
                            "logUuid" to logUuid,
                            "timestamp" to edit.timestamp,
                            "dateTimeFormatted" to formatTimestamp(edit.timestamp),
                            "deviceId" to myDeviceId,
                            "username" to username,
                            "userLoginId" to email,
                            "entityType" to edit.entityType,
                            "entityUuid" to edit.entityUuid,
                            "customerUuid" to edit.customerUuid,
                            "collectionDay" to edit.collectionDay,
                            "action" to edit.action,
                            "summary" to edit.summary
                        )
                        batch.set(firestore.collection("audit_logs").document(logUuid), auditMap)
                        opsInBatch++

                        if (edit.timestamp > latestModificationTime) {
                            latestModificationTime = edit.timestamp
                        }
                        lastPushedEdit = edit
                    }

                    if (opsInBatch > 0) {
                        Tasks.await(batch.commit(), 10, TimeUnit.SECONDS)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to commit edit batch (${chunk.size} items): ${e.message}")
                    remainingIfFailed.addAll(chunk)
                }
            }

            // Preserve any newly enqueued edits that arrived while batch was committing
            val currentInPrefs = getPendingOfflineEdits(context)
            val processedKeys = pending.filterNot { it in remainingIfFailed }.map { "${it.entityType}|${it.entityUuid}|${it.timestamp}" }.toSet()
            val finalRemaining = currentInPrefs.filterNot { "${it.entityType}|${it.entityUuid}|${it.timestamp}" in processedKeys }
            savePendingOfflineEdits(context, finalRemaining)

            if (latestModificationTime > 0L) {
                val now = System.currentTimeMillis()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_SYNCED_AT, maxOf(now, latestModificationTime)).apply()

                val devUpdates = mapOf(
                    "lastModified" to latestModificationTime,
                    "lastModifiedFormatted" to formatTimestamp(latestModificationTime),
                    "lastActiveSessionTime" to now,
                    "lastActiveFormatted" to formatTimestamp(now),
                    "status" to "online"
                )
                rtdb.getReference("devices").child(gmailKey).child(myDeviceId).updateChildren(devUpdates)

                if (lastPushedEdit != null) {
                    val triggerMap = mapOf(
                        "entityType" to lastPushedEdit.entityType,
                        "entityUuid" to lastPushedEdit.entityUuid,
                        "customerUuid" to lastPushedEdit.customerUuid,
                        "collectionDay" to lastPushedEdit.collectionDay,
                        "action" to lastPushedEdit.action,
                        "lastModified" to latestModificationTime,
                        "sourceDeviceId" to myDeviceId
                    )
                    rtdb.getReference("sync_triggers").child("latest").setValue(triggerMap)
                }
            }
        }
    }

    /**
     * Pulls a single specific document from Firestore by UUID and merges it into local Room SQLite
     * if `cloud.lastModified >= local.lastModified`.
     */
    suspend fun pullSingleEntityFromFirestore(
        context: Context,
        db: AppDatabase,
        entityType: String,
        entityUuid: String,
        action: String
    ): Unit = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val dao = db.collectionDao()

        when (entityType) {
            "CUSTOMER" -> {
                val doc = Tasks.await(firestore.collection("customers").document(entityUuid).get(), 6, TimeUnit.SECONDS)
                val existingLocal = dao.getAllCustomersOnce().find { it.uuid == entityUuid }
                if (!doc.exists() || doc.getString("status") == "DELETED" || action == "DELETE") {
                    if (existingLocal != null) {
                        dao.deleteCustomer(existingLocal)
                    }
                    return@withContext
                }
                val cloudMod = doc.getLong("lastModified") ?: 0L
                if (existingLocal != null && existingLocal.lastModified > cloudMod) {
                    return@withContext
                }
                val updatedCustomer = Customer(
                    id = existingLocal?.id ?: 0,
                    name = doc.getString("name") ?: existingLocal?.name.orEmpty(),
                    phone = doc.getString("phone") ?: existingLocal?.phone.orEmpty(),
                    phone2 = doc.getString("phone2") ?: existingLocal?.phone2.orEmpty(),
                    customOrder = (doc.getLong("customOrder") ?: existingLocal?.customOrder?.toLong() ?: 0L).toInt(),
                    collectionDay = doc.getString("collectionDay") ?: existingLocal?.collectionDay ?: "Monday",
                    createdAt = doc.getLong("createdAt") ?: existingLocal?.createdAt ?: System.currentTimeMillis(),
                    city = doc.getString("city") ?: existingLocal?.city.orEmpty(),
                    smsWeeklyReminder = doc.getBoolean("smsWeeklyReminder") ?: existingLocal?.smsWeeklyReminder ?: true,
                    smsConfirmationOfEntry = doc.getBoolean("smsConfirmationOfEntry") ?: existingLocal?.smsConfirmationOfEntry ?: true,
                    autoWeeklySms = doc.getBoolean("autoWeeklySms") ?: false,
                    autoWeeklyWhatsapp = doc.getBoolean("autoWeeklyWhatsapp") ?: false,
                    upiNameAlias = doc.getString("upiNameAlias").orEmpty(),
                    preferredLanguage = doc.getString("preferredLanguage") ?: "English",
                    uuid = entityUuid,
                    lastModified = cloudMod,
                    syncedLastSavedAt = System.currentTimeMillis(),
                    status = doc.getString("status") ?: "ACTIVE"
                )
                dao.insertCustomer(updatedCustomer)
            }

            "LOAN_CYCLE" -> {
                val doc = Tasks.await(firestore.collection("loan_cycles").document(entityUuid).get(), 6, TimeUnit.SECONDS)
                val existingLocal = dao.getAllLoanCyclesOnce().find { it.uuid == entityUuid }
                if (!doc.exists() || doc.getString("status") == "DELETED" || action == "DELETE") {
                    if (existingLocal != null) {
                        dao.deleteLoanCycle(existingLocal)
                    }
                    return@withContext
                }
                val cloudMod = doc.getLong("lastModified") ?: 0L
                if (existingLocal != null && existingLocal.lastModified > cloudMod) {
                    return@withContext
                }
                val customerUuid = doc.getString("customerUuid").orEmpty()
                var parentCustomer = dao.getAllCustomersOnce().find { it.uuid == customerUuid }
                if (parentCustomer == null && customerUuid.isNotBlank()) {
                    pullSingleEntityFromFirestore(context, db, "CUSTOMER", customerUuid, "UPSERT")
                    parentCustomer = dao.getAllCustomersOnce().find { it.uuid == customerUuid }
                }
                if (parentCustomer == null) return@withContext

                val updatedLoan = LoanCycle(
                    id = existingLocal?.id ?: 0,
                    customerId = parentCustomer.id,
                    loanAmount = doc.getDouble("loanAmount") ?: 0.0,
                    interestAmount = doc.getDouble("interestAmount") ?: 0.0,
                    weeklyAmount = doc.getDouble("weeklyAmount") ?: 0.0,
                    totalWeeks = (doc.getLong("totalWeeks") ?: 10L).toInt(),
                    startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                    status = doc.getString("status") ?: "ACTIVE",
                    notes = doc.getString("notes").orEmpty(),
                    paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                    uuid = entityUuid,
                    lastModified = cloudMod,
                    deduction = doc.getDouble("deduction") ?: 0.0
                )
                dao.insertLoanCycle(updatedLoan)
            }

            "WEEKLY_PAYMENT" -> {
                val doc = Tasks.await(firestore.collection("weekly_payments").document(entityUuid).get(), 6, TimeUnit.SECONDS)
                val existingLocal = dao.getAllPaymentsOnce().find { it.uuid == entityUuid }
                if (!doc.exists() || doc.getString("status") == "DELETED" || action == "DELETE") {
                    if (existingLocal != null) {
                        dao.deletePaymentById(existingLocal.id)
                    }
                    return@withContext
                }
                val cloudMod = doc.getLong("lastModified") ?: 0L
                if (existingLocal != null && existingLocal.lastModified > cloudMod) {
                    return@withContext
                }
                val loanUuid = doc.getString("loanUuid").orEmpty()
                var parentLoan = dao.getAllLoanCyclesOnce().find { it.uuid == loanUuid }
                if (parentLoan == null && loanUuid.isNotBlank()) {
                    pullSingleEntityFromFirestore(context, db, "LOAN_CYCLE", loanUuid, "UPSERT")
                    parentLoan = dao.getAllLoanCyclesOnce().find { it.uuid == loanUuid }
                }
                if (parentLoan == null) return@withContext

                val amt = doc.getDouble("amountPaid") ?: 0.0
                val rawWeek = (doc.getLong("weekNumber") ?: 0L).toInt()
                val updatedPayment = WeeklyPayment(
                    id = existingLocal?.id ?: 0,
                    loanCycleId = parentLoan.id,
                    amountPaid = amt,
                    paymentDate = doc.getLong("paymentDate") ?: System.currentTimeMillis(),
                    weekNumber = if (amt <= 0.0) 0 else rawWeek,
                    notes = doc.getString("notes").orEmpty(),
                    upiTxnId = doc.getString("upiTxnId"),
                    uuid = entityUuid,
                    lastModified = cloudMod,
                    status = doc.getString("status") ?: "ACTIVE",
                    timeVerificationStatus = doc.getString("timeVerificationStatus") ?: "VERIFIED"
                )
                dao.insertPayment(updatedPayment)
            }
        }
    }

    // =========================================================================
    // FULL BIDIRECTIONAL UUID RECONCILIATION & AUTO-MIGRATION ON FIRST LOGIN
    // =========================================================================

    suspend fun performFullBidirectionalUuidReconciliation(
        context: Context,
        db: AppDatabase,
        onProgress: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val rtdb = getRtdb()
        val dao = db.collectionDao()
        val myDeviceId = getDeviceId(context)
        val email = getCurrentUserEmail(context)
        val gmailKey = sanitizeGmailKey(email)

        // 1. Customers
        onProgress("Reconciling customers by UUID with Cloud Firestore...")
        val localCustomers = dao.getAllCustomersOnce()
        val localCustByUuid = localCustomers.associateBy { it.uuid }.toMutableMap()
        val cloudCustSnap = Tasks.await(firestore.collection("customers").get(), 12, TimeUnit.SECONDS)

        var uploadedCount = 0
        var downloadedCount = 0

        for (doc in cloudCustSnap.documents) {
            val uuid = doc.getString("uuid") ?: doc.id
            val status = doc.getString("status") ?: "ACTIVE"
            val cloudMod = doc.getLong("lastModified") ?: 0L
            val local = localCustByUuid[uuid]

            if (status == "DELETED") {
                if (local != null && cloudMod >= local.lastModified) {
                    dao.deleteCustomer(local)
                    localCustByUuid.remove(uuid)
                }
                continue
            }

            if (local == null || cloudMod > local.lastModified) {
                val merged = Customer(
                    id = local?.id ?: 0,
                    name = doc.getString("name") ?: local?.name.orEmpty(),
                    phone = doc.getString("phone") ?: local?.phone.orEmpty(),
                    phone2 = doc.getString("phone2") ?: local?.phone2.orEmpty(),
                    customOrder = (doc.getLong("customOrder") ?: local?.customOrder?.toLong() ?: 0L).toInt(),
                    collectionDay = doc.getString("collectionDay") ?: local?.collectionDay ?: "Monday",
                    createdAt = doc.getLong("createdAt") ?: local?.createdAt ?: System.currentTimeMillis(),
                    city = doc.getString("city") ?: local?.city.orEmpty(),
                    smsWeeklyReminder = doc.getBoolean("smsWeeklyReminder") ?: true,
                    smsConfirmationOfEntry = doc.getBoolean("smsConfirmationOfEntry") ?: true,
                    autoWeeklySms = doc.getBoolean("autoWeeklySms") ?: false,
                    autoWeeklyWhatsapp = doc.getBoolean("autoWeeklyWhatsapp") ?: false,
                    upiNameAlias = doc.getString("upiNameAlias").orEmpty(),
                    preferredLanguage = doc.getString("preferredLanguage") ?: "English",
                    uuid = uuid,
                    lastModified = cloudMod,
                    syncedLastSavedAt = System.currentTimeMillis(),
                    status = "ACTIVE"
                )
                dao.insertCustomer(merged)
                downloadedCount++
            }
        }

        // Batch upload local customers missing in cloud or genuinely newer than cloud
        val cloudCustModMap = cloudCustSnap.documents.associate { (it.getString("uuid") ?: it.id) to (it.getLong("lastModified") ?: 0L) }
        val customersToUpload = dao.getAllCustomersOnce().filter { c ->
            val cMod = cloudCustModMap[c.uuid]
            cMod == null || c.lastModified > cMod + 2000L
        }
        for (chunk in customersToUpload.chunked(400)) {
            val batch = firestore.batch()
            for (c in chunk) {
                batch.set(firestore.collection("customers").document(c.uuid), customerToFirestoreMap(c, myDeviceId, email))
                uploadedCount++
            }
            Tasks.await(batch.commit(), 12, TimeUnit.SECONDS)
        }

        // 2. Loan Cycles
        onProgress("Reconciling loan contracts with Cloud Firestore...")
        val customersByUuid = dao.getAllCustomersOnce().associateBy { it.uuid }
        val customersById = customersByUuid.values.associateBy { it.id }
        val localLoans = dao.getAllLoanCyclesOnce()
        val localLoansByUuid = localLoans.associateBy { it.uuid }
        val cloudLoansSnap = Tasks.await(firestore.collection("loan_cycles").get(), 12, TimeUnit.SECONDS)

        for (doc in cloudLoansSnap.documents) {
            val uuid = doc.getString("uuid") ?: doc.id
            val status = doc.getString("status") ?: "ACTIVE"
            val cloudMod = doc.getLong("lastModified") ?: 0L
            val local = localLoansByUuid[uuid]

            if (status == "DELETED") {
                if (local != null && cloudMod >= local.lastModified) {
                    dao.deleteLoanCycle(local)
                }
                continue
            }

            val custUuid = doc.getString("customerUuid").orEmpty()
            val parentCustomer = customersByUuid[custUuid] ?: continue

            if (local == null || cloudMod > local.lastModified) {
                val mergedLoan = LoanCycle(
                    id = local?.id ?: 0,
                    customerId = parentCustomer.id,
                    loanAmount = doc.getDouble("loanAmount") ?: 0.0,
                    interestAmount = doc.getDouble("interestAmount") ?: 0.0,
                    weeklyAmount = doc.getDouble("weeklyAmount") ?: 0.0,
                    totalWeeks = (doc.getLong("totalWeeks") ?: 10L).toInt(),
                    startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                    status = status,
                    notes = doc.getString("notes").orEmpty(),
                    paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                    uuid = uuid,
                    lastModified = cloudMod,
                    deduction = doc.getDouble("deduction") ?: 0.0
                )
                dao.insertLoanCycle(mergedLoan)
                downloadedCount++
            }
        }

        val cloudLoanModMap = cloudLoansSnap.documents.associate { (it.getString("uuid") ?: it.id) to (it.getLong("lastModified") ?: 0L) }
        val loansToUpload = dao.getAllLoanCyclesOnce().filter { l ->
            val cMod = cloudLoanModMap[l.uuid]
            customersById.containsKey(l.customerId) && (cMod == null || l.lastModified > cMod + 2000L)
        }
        for (chunk in loansToUpload.chunked(400)) {
            val batch = firestore.batch()
            for (l in chunk) {
                val parentCust = customersById[l.customerId] ?: continue
                batch.set(firestore.collection("loan_cycles").document(l.uuid), loanCycleToFirestoreMap(l, parentCust, myDeviceId, email))
                uploadedCount++
            }
            Tasks.await(batch.commit(), 12, TimeUnit.SECONDS)
        }

        // 3. Weekly Payments
        onProgress("Reconciling installment receipts with Cloud Firestore...")
        val loansByUuidAfter = dao.getAllLoanCyclesOnce().associateBy { it.uuid }
        val loansByIdAfter = loansByUuidAfter.values.associateBy { it.id }
        val localPayments = dao.getAllPaymentsOnce()
        val localPaymentsByUuid = localPayments.associateBy { it.uuid }
        val cloudPaymentsSnap = Tasks.await(firestore.collection("weekly_payments").get(), 15, TimeUnit.SECONDS)

        for (doc in cloudPaymentsSnap.documents) {
            val uuid = doc.getString("uuid") ?: doc.id
            val status = doc.getString("status") ?: "ACTIVE"
            val cloudMod = doc.getLong("lastModified") ?: 0L
            val local = localPaymentsByUuid[uuid]

            if (status == "DELETED") {
                if (local != null && cloudMod >= local.lastModified) {
                    dao.deletePaymentById(local.id)
                }
                continue
            }

            val loanUuid = doc.getString("loanUuid").orEmpty()
            val parentLoan = loansByUuidAfter[loanUuid] ?: continue

            if (local == null || cloudMod > local.lastModified) {
                val amt = doc.getDouble("amountPaid") ?: 0.0
                val rawWk = (doc.getLong("weekNumber") ?: 0L).toInt()
                val mergedPay = WeeklyPayment(
                    id = local?.id ?: 0,
                    loanCycleId = parentLoan.id,
                    amountPaid = amt,
                    paymentDate = doc.getLong("paymentDate") ?: System.currentTimeMillis(),
                    weekNumber = if (amt <= 0.0) 0 else rawWk,
                    notes = doc.getString("notes").orEmpty(),
                    upiTxnId = doc.getString("upiTxnId"),
                    uuid = uuid,
                    lastModified = cloudMod,
                    status = status,
                    timeVerificationStatus = doc.getString("timeVerificationStatus") ?: "VERIFIED"
                )
                dao.insertPayment(mergedPay)
                downloadedCount++
            }
        }

        val cloudPayModMap = cloudPaymentsSnap.documents.associate { (it.getString("uuid") ?: it.id) to (it.getLong("lastModified") ?: 0L) }
        val paymentsToUpload = dao.getAllPaymentsOnce().filter { p ->
            val parentLoan = loansByIdAfter[p.loanCycleId]
            val parentCust = parentLoan?.let { customersById[it.customerId] }
            val cMod = cloudPayModMap[p.uuid]
            parentLoan != null && parentCust != null && (cMod == null || p.lastModified > cMod + 2000L)
        }
        for (chunk in paymentsToUpload.chunked(400)) {
            val batch = firestore.batch()
            for (p in chunk) {
                val parentLoan = loansByIdAfter[p.loanCycleId] ?: continue
                val parentCust = customersById[parentLoan.customerId] ?: continue
                batch.set(firestore.collection("weekly_payments").document(p.uuid), paymentToFirestoreMap(p, parentLoan, parentCust, myDeviceId, email))
                uploadedCount++
            }
            Tasks.await(batch.commit(), 15, TimeUnit.SECONDS)
        }

        if (uploadedCount > 0) {
            val now = System.currentTimeMillis()
            rtdb.getReference("devices").child(gmailKey).child(myDeviceId).updateChildren(
                mapOf(
                    "lastModified" to now,
                    "lastModifiedFormatted" to formatTimestamp(now),
                    "status" to "online"
                )
            )
        }

        // Run 6-Month Past History Archiver at most once every 7 days in background
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastArchiveCheck = prefs.getLong(KEY_LAST_ARCHIVE_CHECK_AT, 0L)
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - lastArchiveCheck >= sevenDaysMs) {
            prefs.edit().putLong(KEY_LAST_ARCHIVE_CHECK_AT, System.currentTimeMillis()).apply()
            scope.launch {
                try {
                    archiveSettledLoansOlderThan6Months(context, db)
                } catch (_: Exception) {}
            }
        }

        val finalCustCount = dao.getAllCustomersOnce().size
        return@withContext "Synced ($finalCustCount borrowers • ↑$uploadedCount ↓$downloadedCount)"
    }

    // =========================================================================
    // 6-MONTH SETTLED LOAN ARCHIVER (FIRESTORE -> FIREBASE STORAGE .MD)
    // =========================================================================

    suspend fun archiveSettledLoansOlderThan6Months(
        context: Context,
        db: AppDatabase
    ) = withContext(Dispatchers.IO) {
        val dao = db.collectionDao()
        val firestore = getFirestore()
        val storage = getStorage()
        val now = System.currentTimeMillis()
        val sixMonthsMs = 180L * 24L * 60L * 60L * 1000L

        val customersById = dao.getAllCustomersOnce().associateBy { it.id }
        val allLoans = dao.getAllLoanCyclesOnce()
        val allPayments = dao.getAllPaymentsOnce()
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)

        for (loan in allLoans) {
            if (loan.status != "PAID" && loan.status != "CLOSED") continue
            val customer = customersById[loan.customerId] ?: continue
            val cyclePayments = allPayments
                .filter { it.loanCycleId == loan.id && it.status.uppercase() != "DELETED" }
                .sortedBy { it.paymentDate }

            val lastPayDate = cyclePayments.maxOfOrNull { it.paymentDate } ?: loan.lastModified
            val completionTime = if (lastPayDate > 0L) lastPayDate else loan.lastModified
            if (completionTime <= 0L || (now - completionTime) < sixMonthsMs) continue

            // Check if already indexed in `past_history_index`
            val indexDocRef = firestore.collection("past_history_index").document(loan.uuid)
            val existingIdx = try {
                Tasks.await(indexDocRef.get(), 5, TimeUnit.SECONDS)
            } catch (_: Exception) {
                null
            }
            if (existingIdx != null && existingIdx.exists()) continue

            // Build structured Markdown report
            val sb = StringBuilder()
            sb.appendLine("# MD Finance — Archived Settled Loan Report")
            sb.appendLine("- **Customer ID**: ${customer.customerCode}")
            sb.appendLine("- **Customer UUID**: ${customer.uuid}")
            sb.appendLine("- **Customer Name**: ${customer.name}")
            sb.appendLine("- **Collection Day**: ${customer.collectionDay}")
            sb.appendLine("- **Phone**: ${customer.phone}")
            sb.appendLine("- **Loan UUID**: ${loan.uuid}")
            sb.appendLine("- **Principal Amount**: ₹${loan.loanAmount.toLong()}")
            sb.appendLine("- **Interest Amount**: ₹${loan.interestAmount.toLong()}")
            sb.appendLine("- **Upfront Deduction**: ₹${loan.deduction.toLong()}")
            sb.appendLine("- **Total Paid**: ₹${loan.paidAmount.toLong()}")
            sb.appendLine("- **From Date**: ${sdf.format(Date(loan.startDate))}")
            sb.appendLine("- **Settled Date**: ${sdf.format(Date(completionTime))}")
            sb.appendLine()
            sb.appendLine("## Paid Installment Receipts")
            sb.appendLine("| Week | Date & Time | Amount Paid | Mode |")
            sb.appendLine("| :--- | :--- | :--- | :--- |")
            cyclePayments.filter { it.amountPaid > 0.0 }.forEachIndexed { idx, p ->
                val wk = if (p.weekNumber > 0) p.weekNumber else (idx + 1)
                sb.appendLine("| Week $wk | ${sdf.format(Date(p.paymentDate))} | ₹${p.amountPaid.toLong()} | ${p.notes.ifBlank { "Cash" }} |")
            }

            val markdownText = sb.toString()
            val storagePath = "past_history/${customer.customerCode}_${customer.uuid}/${loan.uuid}.md"

            try {
                val storageRef = storage.reference.child(storagePath)
                Tasks.await(storageRef.putBytes(markdownText.toByteArray(StandardCharsets.UTF_8)), 10, TimeUnit.SECONDS)

                val indexData = mapOf(
                    "loanUuid" to loan.uuid,
                    "customerUuid" to customer.uuid,
                    "customerCode" to customer.customerCode,
                    "customerName" to customer.name,
                    "collectionDay" to customer.collectionDay,
                    "fromDate" to loan.startDate,
                    "toDate" to completionTime,
                    "loanAmount" to loan.loanAmount,
                    "interestAmount" to loan.interestAmount,
                    "deduction" to loan.deduction,
                    "paidAmount" to loan.paidAmount,
                    "storagePath" to storagePath,
                    "markdownPreview" to markdownText,
                    "archivedAt" to now
                )
                Tasks.await(indexDocRef.set(indexData), 6, TimeUnit.SECONDS)

                // Prune raw payment documents for this >6-month archived loan from Firestore to save quota
                for (p in cyclePayments) {
                    try {
                        firestore.collection("weekly_payments").document(p.uuid).delete()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed archiving >6m loan ${loan.uuid}: ${e.message}")
            }
        }
    }

    /**
     * Fetches archived >6-month Past History Markdown files for a specific customer UUID from
     * Firestore `past_history_index` + Firebase Storage.
     */
    suspend fun fetchArchivedPastHistoryForCustomer(
        customerUuid: String
    ): List<ArchivedPastLoanItem> = withContext(Dispatchers.IO) {
        if (customerUuid.isBlank()) return@withContext emptyList()
        _isFetchingPastArchive.value = true
        try {
            val firestore = getFirestore()
            val storage = getStorage()
            val snap = Tasks.await(
                firestore.collection("past_history_index")
                    .whereEqualTo("customerUuid", customerUuid)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            val result = mutableListOf<ArchivedPastLoanItem>()
            for (doc in snap.documents) {
                val loanUuid = doc.getString("loanUuid") ?: doc.id
                val storagePath = doc.getString("storagePath").orEmpty()
                var mdText = doc.getString("markdownPreview").orEmpty()

                if (mdText.isBlank() && storagePath.isNotBlank()) {
                    try {
                        val bytes = Tasks.await(
                            storage.reference.child(storagePath).getBytes(512 * 1024),
                            8,
                            TimeUnit.SECONDS
                        )
                        mdText = String(bytes, StandardCharsets.UTF_8)
                    } catch (_: Exception) {}
                }

                result.add(
                    ArchivedPastLoanItem(
                        loanUuid = loanUuid,
                        customerUuid = customerUuid,
                        customerCode = doc.getString("customerCode").orEmpty(),
                        customerName = doc.getString("customerName").orEmpty(),
                        fromDate = doc.getLong("fromDate") ?: 0L,
                        toDate = doc.getLong("toDate") ?: 0L,
                        loanAmount = doc.getDouble("loanAmount") ?: 0.0,
                        interestAmount = doc.getDouble("interestAmount") ?: 0.0,
                        deduction = doc.getDouble("deduction") ?: 0.0,
                        paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                        storagePath = storagePath,
                        markdownContent = mdText
                    )
                )
            }
            result.sortedByDescending { it.fromDate }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching archived past history: ${e.message}")
            emptyList()
        } finally {
            _isFetchingPastArchive.value = false
        }
    }

    // =========================================================================
    // ENTITY -> FIRESTORE MAP SERIALIZERS (WITH PER-COLUMN lastModified SECTION)
    // =========================================================================

    private fun customerToFirestoreMap(
        c: Customer,
        deviceId: String,
        userLoginId: String
    ): Map<String, Any?> {
        return mapOf(
            "uuid" to c.uuid,
            "customerCode" to c.customerCode,
            "name" to c.name,
            "phone" to c.phone,
            "phone2" to c.phone2,
            "customOrder" to c.customOrder,
            "collectionDay" to c.collectionDay,
            "createdAt" to c.createdAt,
            "city" to c.city,
            "smsWeeklyReminder" to c.smsWeeklyReminder,
            "smsConfirmationOfEntry" to c.smsConfirmationOfEntry,
            "autoWeeklySms" to c.autoWeeklySms,
            "autoWeeklyWhatsapp" to c.autoWeeklyWhatsapp,
            "upiNameAlias" to c.upiNameAlias,
            "preferredLanguage" to c.preferredLanguage,
            "status" to c.status,
            "lastModified" to c.lastModified,
            "lastModifiedFormatted" to formatTimestamp(c.lastModified),
            "lastModifiedByDevice" to deviceId,
            "lastModifiedByUser" to userLoginId
        )
    }

    private fun loanCycleToFirestoreMap(
        l: LoanCycle,
        parentCustomer: Customer,
        deviceId: String,
        userLoginId: String
    ): Map<String, Any?> {
        return mapOf(
            "uuid" to l.uuid,
            "customerUuid" to parentCustomer.uuid,
            "customerCode" to parentCustomer.customerCode,
            "customerName" to parentCustomer.name,
            "collectionDay" to parentCustomer.collectionDay,
            "loanAmount" to l.loanAmount,
            "interestAmount" to l.interestAmount,
            "weeklyAmount" to l.weeklyAmount,
            "totalWeeks" to l.totalWeeks,
            "startDate" to l.startDate,
            "status" to l.status,
            "notes" to l.notes,
            "paidAmount" to l.paidAmount,
            "deduction" to l.deduction,
            "lastModified" to l.lastModified,
            "lastModifiedFormatted" to formatTimestamp(l.lastModified),
            "lastModifiedByDevice" to deviceId,
            "lastModifiedByUser" to userLoginId
        )
    }

    private fun paymentToFirestoreMap(
        p: WeeklyPayment,
        parentLoan: LoanCycle,
        parentCustomer: Customer,
        deviceId: String,
        userLoginId: String
    ): Map<String, Any?> {
        return mapOf(
            "uuid" to p.uuid,
            "loanUuid" to parentLoan.uuid,
            "customerUuid" to parentCustomer.uuid,
            "customerCode" to parentCustomer.customerCode,
            "collectionDay" to parentCustomer.collectionDay,
            "amountPaid" to p.amountPaid,
            "paymentDate" to p.paymentDate,
            "weekNumber" to if (p.amountPaid <= 0.0) 0 else p.weekNumber,
            "notes" to p.notes,
            "upiTxnId" to p.upiTxnId,
            "status" to p.status,
            "timeVerificationStatus" to p.timeVerificationStatus,
            "lastModified" to p.lastModified,
            "lastModifiedFormatted" to formatTimestamp(p.lastModified),
            "lastModifiedByDevice" to deviceId,
            "lastModifiedByUser" to userLoginId
        )
    }

    private fun cashLogToFirestoreMap(
        log: CashBalanceLog,
        docKey: String,
        deviceId: String,
        userLoginId: String
    ): Map<String, Any?> {
        return mapOf(
            "uuid" to docKey,
            "date" to log.date,
            "actualCash" to log.actualCash,
            "systemCash" to log.systemCash,
            "collectionAmount" to log.collectionAmount,
            "disbursalAmount" to log.disbursalAmount,
            "expenses" to log.expenses,
            "notes" to log.notes,
            "lastModified" to log.date,
            "lastModifiedFormatted" to formatTimestamp(log.date),
            "lastModifiedByDevice" to deviceId,
            "lastModifiedByUser" to userLoginId
        )
    }
}
