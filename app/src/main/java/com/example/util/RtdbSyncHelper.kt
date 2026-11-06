package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RtdbSyncHelper {

    private const val TAG = "RtdbSyncHelper"

    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)

    private fun formatDate(millis: Long): String {
        return if (millis > 0) dateFormat.format(Date(millis)) else ""
    }

    private fun formatTime(millis: Long): String {
        return if (millis > 0) timeFormat.format(Date(millis)) else ""
    }

    private fun formatDateTime(millis: Long): String {
        return if (millis > 0) fullDateTimeFormat.format(Date(millis)) else ""
    }

    /**
     * Sanitizes strings to make them safe for Firebase RTDB keys (no . # $ [ ] /)
     */
    fun sanitizeKey(key: String): String {
        return key.replace(Regex("[.#$\\[\\]/]"), "_").trim()
    }

    private fun roundMoney(value: Double): Double {
        return (value * 100.0).let { Math.round(it) / 100.0 }
    }

    /**
     * Converts local database tables into Day Branch -> Customer Sub-Branch map with complete
     * totals, customer details, loan details and transaction logs.
     * Excludes "Friday" as per AGENTS.md rules.
     */
    fun buildDayBranchMap(
        customers: List<Customer>,
        loanCycles: List<LoanCycle>,
        payments: List<WeeklyPayment>
    ): Map<String, Any> {
        val activeCustomers = customers.filter { 
            it.status.uppercase() != "DELETED" && !it.collectionDay.trim().equals("Friday", ignoreCase = true) 
        }
        val validLoanCycles = loanCycles.filter { it.status.uppercase() != "DELETED" }
        val validPayments = payments.filter { it.status.uppercase() != "DELETED" }

        // Standard active day branches (Friday excluded per rule)
        val defaultDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Saturday", "Sunday mrg", "Sunday eve")
        
        // Group customers by collectionDay
        val groupedByDay = activeCustomers.groupBy { cust ->
            val day = cust.collectionDay.trim()
            if (day.equals("Friday", ignoreCase = true)) "Monday" else day
        }

        val resultDaysMap = mutableMapOf<String, Any>()

        // Combine default days and any custom day present in DB (excluding Friday)
        val allDaysToProcess = (defaultDays + groupedByDay.keys)
            .distinct()
            .filter { !it.equals("Friday", ignoreCase = true) }

        val nowMs = System.currentTimeMillis()

        for (dayName in allDaysToProcess) {
            val safeDayKey = sanitizeKey(dayName)
            val dayCustomers = groupedByDay[dayName] ?: emptyList()
            val customersMap = mutableMapOf<String, Any>()
            val dayTransactionsMap = mutableMapOf<String, Any>()

            var dayTotalDisbursed = 0.0
            var dayTotalInterest = 0.0
            var dayTotalExpectedCollection = 0.0
            var dayTotalCollected = 0.0
            var dayTotalDeductions = 0.0
            var dayActiveLoansCount = 0

            for (cust in dayCustomers.sortedBy { it.customOrder }) {
                val custKey = sanitizeKey("cust_${cust.name}_${cust.id}_${cust.uuid.take(8)}")
                val custCycles = validLoanCycles.filter { it.customerId == cust.id }.sortedByDescending { it.id }

                val loansMap = mutableMapOf<String, Any>()
                val customerTransactionsMap = mutableMapOf<String, Any>()

                var custTotalDisbursed = 0.0
                var custTotalInterest = 0.0
                var custTotalExpected = 0.0
                var custTotalPaid = 0.0
                var custActiveLoansCount = 0

                for (cycle in custCycles) {
                    val loanKey = sanitizeKey("loan_${cycle.id}_${cycle.uuid.take(8)}")
                    val cyclePayments = validPayments.filter { it.loanCycleId == cycle.id }.sortedBy { it.weekNumber }

                    val paymentsMap = mutableMapOf<String, Any>()
                    var cyclePaidSum = 0.0

                    for (payment in cyclePayments) {
                        val paymentKey = sanitizeKey("pay_w${payment.weekNumber}_${payment.id}")
                        val pTime = payment.paymentDate
                        val txnDateStr = formatDate(pTime)
                        val txnTimeStr = formatTime(pTime)
                        val txnDateTimeStr = formatDateTime(pTime)

                        cyclePaidSum += payment.amountPaid

                        val paymentDetailMap = mapOf(
                            "paymentId" to payment.id,
                            "uuid" to payment.uuid,
                            "loanCycleId" to cycle.id,
                            "customerId" to cust.id,
                            "customerName" to cust.name,
                            "customerCode" to cust.customerCode,
                            "collectionDay" to cust.collectionDay,
                            "weekNumber" to payment.weekNumber,
                            "amountPaid" to payment.amountPaid,
                            "paymentDateTimestamp" to pTime,
                            "transactionDate" to txnDateStr,
                            "transactionTime" to txnTimeStr,
                            "transactionDateTime" to txnDateTimeStr,
                            "upiTxnId" to (payment.upiTxnId ?: ""),
                            "notes" to payment.notes,
                            "status" to payment.status,
                            "timeVerificationStatus" to payment.timeVerificationStatus,
                            "lastModified" to payment.lastModified,
                            "lastModifiedDateTime" to formatDateTime(payment.lastModified)
                        )

                        paymentsMap[paymentKey] = paymentDetailMap
                        customerTransactionsMap[paymentKey] = paymentDetailMap
                        dayTransactionsMap[paymentKey] = paymentDetailMap
                    }

                    val disbursalTime = cycle.startDate
                    val cycleExpected = roundMoney(cycle.loanAmount + cycle.interestAmount)
                    val roundPaid = roundMoney(cyclePaidSum)
                    val cycleOutstanding = roundMoney((cycleExpected - roundPaid).coerceAtLeast(0.0))

                    custTotalDisbursed += cycle.loanAmount
                    custTotalInterest += cycle.interestAmount
                    custTotalExpected += cycleExpected
                    custTotalPaid += roundPaid
                    if (cycle.status.uppercase() == "ACTIVE") {
                        custActiveLoansCount++
                        dayActiveLoansCount++
                    }

                    dayTotalDisbursed += cycle.loanAmount
                    dayTotalInterest += cycle.interestAmount
                    dayTotalExpectedCollection += cycleExpected
                    dayTotalCollected += roundPaid
                    dayTotalDeductions += cycle.deduction

                    loansMap[loanKey] = mapOf(
                        "loanId" to cycle.id,
                        "uuid" to cycle.uuid,
                        "customerId" to cust.id,
                        "loanAmount" to roundMoney(cycle.loanAmount),
                        "amountDisbursed" to roundMoney((cycle.loanAmount - cycle.deduction).coerceAtLeast(0.0)),
                        "principal" to roundMoney(cycle.loanAmount),
                        "interest" to roundMoney(cycle.interestAmount),
                        "weeklyAmount" to roundMoney(cycle.weeklyAmount),
                        "totalWeeks" to cycle.totalWeeks,
                        "startDateTimestamp" to disbursalTime,
                        "disbursalDate" to formatDate(disbursalTime),
                        "disbursalTime" to formatTime(disbursalTime),
                        "disbursalDateTime" to formatDateTime(disbursalTime),
                        "paidAmount" to roundPaid,
                        "outstandingAmount" to cycleOutstanding,
                        "deduction" to roundMoney(cycle.deduction),
                        "status" to cycle.status,
                        "notes" to cycle.notes,
                        "lastModified" to cycle.lastModified,
                        "lastModifiedDateTime" to formatDateTime(cycle.lastModified),
                        "payments" to paymentsMap
                    )
                }

                val custCreatedTime = cust.createdAt
                val custModTime = cust.lastModified
                val custOutstanding = roundMoney((custTotalExpected - custTotalPaid).coerceAtLeast(0.0))

                customersMap[custKey] = mapOf(
                    "customerId" to cust.id,
                    "uuid" to cust.uuid,
                    "customerCode" to cust.customerCode,
                    "name" to cust.name,
                    "phone" to cust.phone,
                    "phone2" to cust.phone2,
                    "city" to cust.city,
                    "routeNo" to cust.customOrder,
                    "customOrder" to cust.customOrder,
                    "collectionDay" to cust.collectionDay,
                    "preferredLanguage" to cust.preferredLanguage,
                    "upiNameAlias" to cust.upiNameAlias,
                    "smsSettings" to mapOf(
                        "weeklyReminder" to cust.smsWeeklyReminder,
                        "entryConfirmation" to cust.smsConfirmationOfEntry,
                        "autoWeeklySms" to cust.autoWeeklySms,
                        "autoWeeklyWhatsapp" to cust.autoWeeklyWhatsapp
                    ),
                    "createdAtTimestamp" to custCreatedTime,
                    "createdAtDateTime" to formatDateTime(custCreatedTime),
                    "lastModifiedTimestamp" to custModTime,
                    "lastModifiedDateTime" to formatDateTime(custModTime),
                    "status" to cust.status,
                    "customerTotals" to mapOf(
                        "totalDisbursed" to roundMoney(custTotalDisbursed),
                        "totalInterest" to roundMoney(custTotalInterest),
                        "totalExpected" to roundMoney(custTotalExpected),
                        "totalPaid" to roundMoney(custTotalPaid),
                        "totalOutstanding" to custOutstanding,
                        "activeLoansCount" to custActiveLoansCount
                    ),
                    "loans" to loansMap,
                    "transactions" to customerTransactionsMap
                )
            }

            val dayOutstanding = roundMoney((dayTotalExpectedCollection - dayTotalCollected).coerceAtLeast(0.0))

            resultDaysMap[safeDayKey] = mapOf(
                "dayName" to dayName,
                "dayTotals" to mapOf(
                    "totalCustomers" to dayCustomers.size,
                    "activeLoansCount" to dayActiveLoansCount,
                    "totalDisbursed" to roundMoney(dayTotalDisbursed),
                    "totalInterest" to roundMoney(dayTotalInterest),
                    "totalExpectedCollection" to roundMoney(dayTotalExpectedCollection),
                    "totalCollected" to roundMoney(dayTotalCollected),
                    "totalOutstanding" to dayOutstanding,
                    "totalDeductions" to roundMoney(dayTotalDeductions),
                    "lastUpdatedTimestamp" to nowMs,
                    "lastUpdatedDateTime" to formatDateTime(nowMs)
                ),
                "customers" to customersMap,
                "transactions" to dayTransactionsMap
            )
        }

        return resultDaysMap
    }

    /**
     * Syncs local data to Firebase RTDB under structured Day Branches & Customer Sub-Branches,
     * and deletes legacy/unwanted branches from RTDB.
     */
    fun syncToFirebaseRtdb(
        rtdb: FirebaseDatabase,
        customers: List<Customer>,
        loanCycles: List<LoanCycle>,
        payments: List<WeeklyPayment>
    ) {
        try {
            val treeMap = buildDayBranchMap(customers, loanCycles, payments)
            
            // 1. Store clean Day Branches structure under "day_branches"
            val dayBranchesRef = rtdb.getReference("day_branches")
            Tasks.await(dayBranchesRef.setValue(treeMap), 5, java.util.concurrent.TimeUnit.SECONDS)

            // 2. Remove legacy/unneeded RTDB nodes to keep database clean as requested
            try {
                Tasks.await(rtdb.getReference("days").removeValue(), 5, java.util.concurrent.TimeUnit.SECONDS)
                Tasks.await(rtdb.getReference("all_transactions").removeValue(), 5, java.util.concurrent.TimeUnit.SECONDS)
                Tasks.await(rtdb.getReference("rtdb_pings").removeValue(), 5, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup of legacy RTDB nodes skipped: ${e.message}")
            }

            Log.i(TAG, "Successfully synchronized Day Branches & Customer Sub-Branches to Firebase RTDB.")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Day Branches to RTDB: ${e.message}", e)
        }
    }
}

