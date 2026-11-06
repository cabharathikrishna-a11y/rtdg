package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupHelper {
    private const val TAG = "DatabaseBackupHelper"

    suspend fun autoBackup(context: Context, db: AppDatabase): Boolean {
        return try {
            val customersList = db.collectionDao().getAllCustomersOnce()
            val loanCyclesList = db.collectionDao().getAllLoanCyclesOnce()
            val paymentsList = db.collectionDao().getAllPaymentsOnce()
            val cashBalanceLogsList = db.collectionDao().getAllCashBalanceLogsOnce()

            val csvString = CsvBackupHelper.generateCsvString(
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
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val backupFile = File(backupDir, "finance_ALL_backup_before_update_$timestamp.csv")
            backupFile.writeText(csvString, Charsets.UTF_8)
            Log.i(TAG, "Backup successfully saved locally to: ${backupFile.absolutePath}")

            val latestBackupFile = File(context.filesDir, "finance_ALL_latest_backup.csv")
            latestBackupFile.writeText(csvString, Charsets.UTF_8)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "autoBackup failed", e)
            false
        }
    }
}
