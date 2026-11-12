package com.example.util

import android.util.Log
import com.example.data.Customer
import com.example.data.LoanCycle
import com.example.data.WeeklyPayment
import com.google.firebase.database.FirebaseDatabase

/**
 * Legacy RTDB full-tree sync helper retired in favor of Cloud Firestore (`CloudLedgerSyncEngine`).
 * Previously, serializing the 9-level nested `day_branches` map via `JsonMapper.serializeJsonValue`
 * allocated a ~37.7 MB contiguous JSONStringer buffer on the heap and caused `OutOfMemoryError`.
 *
 * RTDB is now strictly reserved for lightweight control metadata:
 * - `/devices`
 * - `/sync_triggers/latest`
 * - `/update_config` & `/versionId`
 * - `/app_password`
 */
object RtdbSyncHelper {

    private const val TAG = "RtdbSyncHelper"

    fun sanitizeKey(key: String): String {
        return key.replace(Regex("[.#$\\[\\]/]"), "_").trim()
    }

    @Suppress("UNUSED_PARAMETER")
    fun buildDayBranchMap(
        customers: List<Customer>,
        loanCycles: List<LoanCycle>,
        payments: List<WeeklyPayment>
    ): Map<String, Any> {
        return emptyMap()
    }

    @Suppress("UNUSED_PARAMETER")
    fun syncToFirebaseRtdb(
        rtdb: FirebaseDatabase,
        customers: List<Customer>,
        loanCycles: List<LoanCycle>,
        payments: List<WeeklyPayment>
    ) {
        try {
            rtdb.purgeOutstandingWrites()
            rtdb.getReference("day_branches").removeValue()
            rtdb.getReference("ledger_csv").removeValue()
            rtdb.getReference("days").removeValue()
            rtdb.getReference("all_transactions").removeValue()
            rtdb.getReference("rtdb_pings").removeValue()
            Log.i(TAG, "Purged legacy bloated RTDB branches; customer/loan data is managed in Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup of legacy RTDB nodes skipped: ${e.message}")
        }
    }
}
