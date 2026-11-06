package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class FinanceRepository(private val dao: CollectionDao) {

    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val activeLoanCycles: Flow<List<LoanCycle>> = dao.getActiveLoanCycles()
    val allLoanCycles: Flow<List<LoanCycle>> = dao.getAllLoanCycles()
    val allPayments: Flow<List<WeeklyPayment>> = dao.getAllPayments()
    val allEditLogs: Flow<List<EditLog>> = dao.getAllEditLogs()
    val allCashBalanceLogs: Flow<List<CashBalanceLog>> = dao.getAllCashBalanceLogsFlow()

    suspend fun addCashBalanceLog(log: CashBalanceLog): Long {
        return dao.insertCashBalanceLog(log)
    }

    suspend fun updateCashBalanceLog(log: CashBalanceLog) {
        dao.updateCashBalanceLog(log)
    }

    suspend fun deleteCashBalanceLog(log: CashBalanceLog) {
        dao.deleteCashBalanceLog(log)
    }

    suspend fun getLastCashBalanceLog(): CashBalanceLog? {
        return dao.getLastCashBalanceLog()
    }

    var deviceUsername: String = "Device User"

    suspend fun addEditLog(log: EditLog): Long {
        val decoratedDesc = if (log.actionDescription.contains(" (by ")) {
            log.actionDescription
        } else {
            "${log.actionDescription} (by $deviceUsername)"
        }
        return dao.insertEditLog(log.copy(actionDescription = decoratedDesc))
    }

    suspend fun deleteEditLog(log: EditLog) {
        dao.deleteEditLog(log)
    }

    suspend fun clearEditLogs() {
        dao.deleteAllEditLogs()
    }

    suspend fun getCustomerById(id: Int): Customer? = dao.getCustomerById(id)

    suspend fun addCustomer(customer: Customer): Long {
        val withTime = customer.copy(lastModified = System.currentTimeMillis())
        return dao.insertCustomer(withTime)
    }

    suspend fun updateCustomer(customer: Customer) {
        val withTime = customer.copy(lastModified = System.currentTimeMillis())
        dao.updateCustomer(withTime)
    }

    suspend fun deleteCustomer(customer: Customer) {
        dao.deletePaymentsForCustomer(customer.id)
        dao.deleteLoanCyclesForCustomer(customer.id)
        dao.deleteCustomer(customer)
    }

    suspend fun updateCustomerOrder(id: Int, order: Int) {
        dao.updateCustomerOrder(id, order)
        updateCustomerLastModified(id)
    }

    fun getLoanCyclesForCustomer(customerId: Int): Flow<List<LoanCycle>> {
        return dao.getLoanCyclesForCustomer(customerId)
    }

    suspend fun getActiveLoanCycleForCustomer(customerId: Int): LoanCycle? {
        return dao.getActiveLoanCycleForCustomer(customerId)
    }

    suspend fun getLoanCycleById(id: Int): LoanCycle? = dao.getLoanCycleById(id)

    suspend fun addLoanCycle(cycle: LoanCycle): Long {
        val withTime = cycle.copy(lastModified = System.currentTimeMillis())
        val insertedId = dao.insertLoanCycle(withTime)
        return insertedId
    }

    suspend fun updateLoanCycle(cycle: LoanCycle) {
        val withTime = cycle.copy(lastModified = System.currentTimeMillis())
        dao.updateLoanCycle(withTime)
    }

    suspend fun deleteLoanCycle(cycle: LoanCycle) {
        dao.deleteLoanCycle(cycle)
    }

    fun getPaymentsForCycle(loanCycleId: Int): Flow<List<WeeklyPayment>> {
        return dao.getPaymentsForCycle(loanCycleId)
    }

    suspend fun addWeeklyPayment(payment: WeeklyPayment): Long {
        return dao.addWeeklyPaymentTx(payment)
    }

    suspend fun removeWeeklyPayment(paymentId: Int, loanCycleId: Int) {
        dao.removeWeeklyPaymentTx(paymentId, loanCycleId)
    }

    suspend fun getPaymentCountByUpiTxnId(txnId: String): Int {
        return dao.getPaymentCountByUpiTxnId(txnId)
    }

    suspend fun getPaymentByUpiTxnId(txnId: String): WeeklyPayment? {
        return dao.getPaymentByUpiTxnId(txnId)
    }

    suspend fun updateWeeklyPayment(
        paymentId: Int,
        loanCycleId: Int,
        newAmount: Double,
        newWeekNumber: Int,
        newDate: Long,
        newNotes: String,
        upiTxnId: String? = null
    ) {
        dao.updateWeeklyPaymentTx(
            paymentId = paymentId,
            loanCycleId = loanCycleId,
            newAmount = newAmount,
            newWeekNumber = newWeekNumber,
            newDate = newDate,
            newNotes = newNotes,
            upiTxnId = upiTxnId
        )
    }

    suspend fun insertWeeklyPayment(payment: WeeklyPayment) {
        dao.insertPayment(payment)
    }

    private suspend fun updateCustomerLastModified(customerId: Int) {
        val customer = dao.getCustomerById(customerId)
        if (customer != null) {
            dao.updateCustomer(customer.copy(lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun restoreBackup(
        customers: List<Customer>,
        loanCycles: List<LoanCycle>,
        payments: List<WeeklyPayment>
    ) {
        dao.restoreBackupTx(customers, loanCycles, payments)
    }

    suspend fun populateMissingUuids() {
        // 1. Customers
        val customersList = dao.getAllCustomersOnce()
        for (c in customersList) {
            if (c.uuid.isBlank() || c.uuid == "") {
                val newUuid = java.util.UUID.randomUUID().toString()
                dao.updateCustomer(c.copy(uuid = newUuid))
            }
        }

        val updatedCustomers = dao.getAllCustomersOnce()
        val customerGroups = updatedCustomers.groupBy { it.uuid }
        for ((uuid, group) in customerGroups) {
            if (uuid.isNotBlank() && group.size > 1) {
                val sorted = group.sortedByDescending { it.lastModified }
                val primary = sorted[0]
                val duplicates = sorted.subList(1, sorted.size)
                for (dup in duplicates) {
                    val newUuid = java.util.UUID.randomUUID().toString()
                    dao.updateCustomer(dup.copy(uuid = newUuid, lastModified = System.currentTimeMillis()))
                }
            }
        }

        // 2. Loans
        val loansList = dao.getAllLoanCyclesOnce()
        for (l in loansList) {
            if (l.uuid.isBlank() || l.uuid == "") {
                val newUuid = java.util.UUID.randomUUID().toString()
                dao.updateLoanCycle(l.copy(uuid = newUuid))
            }
        }

        val updatedLoans = dao.getAllLoanCyclesOnce()
        val loanGroups = updatedLoans.groupBy { it.uuid }
        for ((uuid, group) in loanGroups) {
            if (uuid.isNotBlank() && group.size > 1) {
                val sorted = group.sortedByDescending { it.lastModified }
                val primary = sorted[0]
                val duplicates = sorted.subList(1, sorted.size)
                for (dup in duplicates) {
                    val newUuid = java.util.UUID.randomUUID().toString()
                    dao.updateLoanCycle(dup.copy(uuid = newUuid, lastModified = System.currentTimeMillis()))
                }
            }
        }

        // 3. Payments
        val paymentsList = dao.getAllPaymentsOnce()
        for (p in paymentsList) {
            if (p.uuid.isBlank() || p.uuid == "") {
                val newUuid = java.util.UUID.randomUUID().toString()
                dao.insertPayment(p.copy(uuid = newUuid))
            }
        }

        val updatedPayments = dao.getAllPaymentsOnce()
        val paymentGroupsByUuid = updatedPayments.groupBy { it.uuid }
        for ((uuid, group) in paymentGroupsByUuid) {
            if (uuid.isNotBlank() && group.size > 1) {
                val sorted = group.sortedByDescending { it.lastModified }
                val primary = sorted[0]
                val duplicates = sorted.subList(1, sorted.size)
                for (dup in duplicates) {
                    val newUuid = java.util.UUID.randomUUID().toString()
                    dao.insertPayment(dup.copy(uuid = newUuid, lastModified = System.currentTimeMillis()))
                }
            }
        }

        // 4. Edit Logs
        val auditList = dao.getAllEditLogsOnce()
        for (a in auditList) {
            if (a.uuid.isBlank() || a.uuid == "") {
                val newUuid = java.util.UUID.randomUUID().toString()
                dao.insertEditLog(a.copy(uuid = newUuid))
            }
        }

        val updatedAudits = dao.getAllEditLogsOnce()
        val auditGroupsByUuid = updatedAudits.groupBy { it.uuid }
        for ((uuid, group) in auditGroupsByUuid) {
            if (uuid.isNotBlank() && group.size > 1) {
                val sorted = group.sortedByDescending { it.timestamp }
                val primary = sorted[0]
                val duplicates = sorted.subList(1, sorted.size)
                for (dup in duplicates) {
                    val newUuid = java.util.UUID.randomUUID().toString()
                    dao.insertEditLog(dup.copy(uuid = newUuid))
                }
            }
        }
    }
}

