package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LoanCycle
import com.example.data.WeeklyPayment
import java.text.SimpleDateFormat
import java.util.*

data class PaymentDetailItem(
    val paymentId: Int,
    val loanCycleId: Int,
    val paymentDate: Long,
    val customerId: Int,
    val customerName: String,
    val customerCode: String,
    val amount: Double,
    val weekNumber: Int,
    val notes: String,
    val upiTxnId: String?,
    val customOrder: Int
)

data class DisbursalDetailItem(
    val loanCycleId: Int,
    val customerId: Int,
    val customerName: String,
    val customerCode: String,
    val loanAmount: Double,
    val deduction: Double,
    val actualDisbursed: Double,
    val interestAmount: Double,
    val weeklyAmount: Double,
    val tenureWeeks: Int,
    val customOrder: Int,
    val notes: String = ""
)

data class ProfitDetailItem(
    val loanCycleId: Int?,
    val paymentId: Int?,
    val customerId: Int,
    val customerName: String,
    val customerCode: String,
    val source: String,
    val amount: Double,
    val details: String,
    val customOrder: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationDetailScreen(
    type: String, // "COLLECTION", "DISBURSAL", "PROFIT"
    day: String,  // "Home", "Monday", "Tuesday", etc.
    viewModel: FinanceViewModel,
    initialMode: String? = null
) {
    val appColors = LocalAppThemeColors.current
    val language by viewModel.language.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allLoanCycles by viewModel.allLoanCycles.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()

    // Parse data based on start of today
    val startOfToday = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val isHome = remember(day) { day.equals("Home", ignoreCase = true) }

    // Computations
    val paymentsToday = remember(allPayments, startOfToday) {
        allPayments.filter { it.paymentDate >= startOfToday && it.status.uppercase() != "DELETED" }
    }

    val loansToday = remember(allLoanCycles, startOfToday) {
        allLoanCycles.filter { l -> l.startDate >= startOfToday && l.status.uppercase() != "DELETED" }
    }

    // Title and stats setup
    val screenTitle = when (type) {
        "COLLECTION" -> translate("Today's Collections", language)
        "DISBURSAL" -> translate("Today's Disbursals", language)
        "DEDUCTIONS" -> translate("Today's Deductions", language)
        else -> translate("Today's Profit", language)
    }

    val dashboardSubtitle = if (isHome) {
        translate("Global / All Groups", language)
    } else {
        translate("$day Collection Group", language)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = dashboardSubtitle,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("calc_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorSlateDark)
            )
        },
        containerColor = appColors.mainBg
    ) { innerPadding ->
        when (type) {
            "COLLECTION" -> {
                val filteredPayments = remember(paymentsToday, allCustomers, allLoanCycles, day, isHome) {
                    paymentsToday.filter { p ->
                        val l = allLoanCycles.find { it.id == p.loanCycleId } ?: return@filter false
                        val c = allCustomers.find { it.id == l.customerId } ?: return@filter false
                        (isHome || c.collectionDay.trim().equals(day.trim(), ignoreCase = true)) && p.amountPaid > 0.0
                    }.map { p ->
                        val l = allLoanCycles.find { it.id == p.loanCycleId }!!
                        val c = allCustomers.find { it.id == l.customerId }!!
                        PaymentDetailItem(
                            paymentId = p.id,
                            loanCycleId = l.id,
                            paymentDate = p.paymentDate,
                            customerId = c.id,
                            customerName = c.name,
                            customerCode = c.customerCode,
                            amount = p.amountPaid,
                            weekNumber = p.weekNumber,
                            notes = p.notes,
                            upiTxnId = p.upiTxnId,
                            customOrder = c.customOrder
                        )
                    }.sortedBy { it.customOrder }
                }

                val totalSum = filteredPayments.sumOf { it.amount }

                CollectionDetailLayout(
                    items = filteredPayments,
                    totalSum = totalSum,
                    padding = innerPadding,
                    appColors = appColors,
                    language = language,
                    viewModel = viewModel,
                    initialMode = initialMode
                )
            }
            "DISBURSAL" -> {
                val filteredLoans = remember(loansToday, allCustomers, day, isHome) {
                    loansToday.filter { l ->
                        val c = allCustomers.find { it.id == l.customerId } ?: return@filter false
                        isHome || c.collectionDay.trim().equals(day.trim(), ignoreCase = true)
                    }.map { l ->
                        val c = allCustomers.find { it.id == l.customerId }!!
                        DisbursalDetailItem(
                            loanCycleId = l.id,
                            customerId = c.id,
                            customerName = c.name,
                            customerCode = c.customerCode,
                            loanAmount = l.loanAmount,
                            deduction = l.deduction,
                            actualDisbursed = l.loanAmount - l.deduction,
                            interestAmount = l.interestAmount,
                            weeklyAmount = l.weeklyAmount,
                            tenureWeeks = l.totalWeeks,
                            customOrder = c.customOrder,
                            notes = l.notes
                        )
                    }.sortedBy { it.customOrder }
                }

                val totalSum = filteredLoans.sumOf { it.actualDisbursed }

                DisbursalDetailLayout(
                    items = filteredLoans,
                    totalSum = totalSum,
                    padding = innerPadding,
                    appColors = appColors,
                    language = language,
                    viewModel = viewModel,
                    initialMode = initialMode
                )
            }
            "DEDUCTIONS" -> {
                val filteredItems = remember(loansToday, allCustomers, day, isHome) {
                    loansToday.filter { l ->
                        val c = allCustomers.find { it.id == l.customerId } ?: return@filter false
                        (isHome || c.collectionDay.trim().equals(day.trim(), ignoreCase = true)) && l.deduction > 0.0
                    }.map { l ->
                        val c = allCustomers.find { it.id == l.customerId }!!
                        ProfitDetailItem(
                            loanCycleId = l.id,
                            paymentId = null,
                            customerId = c.id,
                            customerName = c.name,
                            customerCode = c.customerCode,
                            source = "Upfront Deduction",
                            amount = l.deduction,
                            details = "₹${l.loanAmount.toLong()} loan cycle",
                            customOrder = c.customOrder
                        )
                    }.sortedBy { it.customOrder }
                }

                val totalSum = filteredItems.sumOf { it.amount }
                ProfitDetailLayout(
                    items = filteredItems,
                    totalSum = totalSum,
                    padding = innerPadding,
                    appColors = appColors,
                    language = language,
                    viewModel = viewModel,
                    title = translate("TOTAL DEDUCTIONS", language)
                )
            }
            else -> { // PROFIT
                val filteredProfitItems = remember(paymentsToday, allCustomers, allLoanCycles, day, isHome) {
                    paymentsToday.filter { p ->
                        val l = allLoanCycles.find { it.id == p.loanCycleId } ?: return@filter false
                        val c = allCustomers.find { it.id == l.customerId } ?: return@filter false
                        isHome || c.collectionDay.trim().equals(day.trim(), ignoreCase = true)
                    }.map { p ->
                        val l = allLoanCycles.find { it.id == p.loanCycleId }!!
                        val c = allCustomers.find { it.id == l.customerId }!!
                        val total = l.loanAmount + l.interestAmount
                        val ratio = if (total > 0.0) l.interestAmount / total else 0.0
                        val interestPortion = p.amountPaid * ratio
                        ProfitDetailItem(
                            loanCycleId = l.id,
                            paymentId = p.id,
                            customerId = c.id,
                            customerName = c.name,
                            customerCode = c.customerCode,
                            source = "Collected Interest Share",
                            amount = interestPortion,
                            details = "Interest component of ₹${p.amountPaid.toLong()} instalment payment",
                            customOrder = c.customOrder
                        )
                    }.filter { it.amount > 0.0 }.sortedBy { it.customOrder }
                }

                val totalSum = filteredProfitItems.sumOf { it.amount }

                ProfitDetailLayout(
                    items = filteredProfitItems,
                    totalSum = totalSum,
                    padding = innerPadding,
                    appColors = appColors,
                    language = language,
                    viewModel = viewModel,
                    title = translate("TOTAL REALIZED PROFIT", language)
                )
            }
        }
    }
}

@Composable
fun CollectionDetailLayout(
    items: List<PaymentDetailItem>,
    totalSum: Double,
    padding: PaddingValues,
    appColors: AppThemeColors,
    language: String,
    viewModel: FinanceViewModel,
    initialMode: String? = null
) {
    var showEditDialog by remember { mutableStateOf<PaymentDetailItem?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedMode by remember { mutableStateOf<String?>(initialMode) }
    var subSearchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedMode != null) {
        selectedMode = null
        subSearchQuery = ""
    }

    fun getCollectionSplit(item: PaymentDetailItem): Pair<Double, Double> {
        val notes = item.notes.trim()
        if (notes.startsWith("Multiple - ", ignoreCase = true)) {
            try {
                val cashMarker = "Cash: ₹"
                val onlineMarker = "Online: ₹"
                val upiMarker = "UPI: ₹"
                val cashStart = notes.indexOf(cashMarker)
                val onlineStart = notes.indexOf(onlineMarker)
                val upiStart = notes.indexOf(upiMarker)
                
                val oStart = if (onlineStart != -1) onlineStart else upiStart
                val oMarker = if (onlineStart != -1) onlineMarker else upiMarker
                
                if (cashStart != -1) {
                    val cashVal = parseAmountFromString(notes.substring(cashStart + cashMarker.length))
                    val onlineVal = if (oStart != -1) {
                        parseAmountFromString(notes.substring(oStart + oMarker.length))
                    } else {
                        0.0
                    }
                    return Pair(cashVal, onlineVal)
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        
        val isOnline = !item.upiTxnId.isNullOrBlank() || 
                       notes.contains("Online", ignoreCase = true) || 
                       notes.contains("UPI", ignoreCase = true) || 
                       notes.contains("GPay", ignoreCase = true) || 
                       notes.contains("PhonePe", ignoreCase = true) || 
                       notes.contains("Paytm", ignoreCase = true) || 
                       notes.contains("Bank", ignoreCase = true) ||
                       notes.contains("Google Pay", ignoreCase = true) ||
                       notes.contains("Phone Pe", ignoreCase = true) ||
                       notes.contains("IMPS", ignoreCase = true) ||
                       notes.contains("NEFT", ignoreCase = true) ||
                       notes.contains("RTGS", ignoreCase = true) ||
                       notes.contains("Net", ignoreCase = true) ||
                       notes.contains("Transfer", ignoreCase = true)
                       
        return if (isOnline) {
            Pair(0.0, item.amount)
        } else {
            Pair(item.amount, 0.0)
        }
    }

    val collectionSplit = remember(items) {
        var cashSum = 0.0
        var onlineSum = 0.0
        for (item in items) {
            val (cash, online) = getCollectionSplit(item)
            cashSum += cash
            onlineSum += online
        }
        Pair(cashSum, onlineSum)
    }
    val cashCollectionSum = collectionSplit.first
    val onlineCollectionSum = collectionSplit.second

    fun isItemOnline(item: PaymentDetailItem): Boolean {
        val cleanNotes = item.notes.trim()
        return !item.upiTxnId.isNullOrBlank() || 
               cleanNotes.contains("Online", ignoreCase = true) || 
               cleanNotes.contains("UPI", ignoreCase = true) || 
               cleanNotes.contains("GPay", ignoreCase = true) || 
               cleanNotes.contains("PhonePe", ignoreCase = true) || 
               cleanNotes.contains("Paytm", ignoreCase = true) || 
               cleanNotes.contains("Bank", ignoreCase = true) ||
               cleanNotes.contains("Google Pay", ignoreCase = true) ||
               cleanNotes.contains("Phone Pe", ignoreCase = true) ||
               cleanNotes.contains("IMPS", ignoreCase = true) ||
               cleanNotes.contains("NEFT", ignoreCase = true) ||
               cleanNotes.contains("RTGS", ignoreCase = true) ||
               cleanNotes.contains("Net", ignoreCase = true) ||
               cleanNotes.contains("Transfer", ignoreCase = true)
    }

    if (showEditDialog != null) {
        val item = showEditDialog!!
        var editAmount by rememberSaveable { mutableStateOf(if (item.amount % 1.0 == 0.0) item.amount.toInt().toString() else item.amount.toString()) }
        var editNotes by rememberSaveable { mutableStateOf(item.notes) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(translate("Edit Payment", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text(translate("Amount", language)) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        label = { Text(translate("Notes", language)) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = editAmount.toDoubleOrNull() ?: item.amount
                    viewModel.editWeeklyPayment(
                        paymentId = item.paymentId,
                        loanCycleId = item.loanCycleId,
                        amount = amount,
                        weekNum = item.weekNumber,
                        paymentDate = item.paymentDate,
                        notes = editNotes
                    )
                    showEditDialog = null
                }) {
                    Text(translate("Save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text(translate("Cancel", language))
                }
            }
        )
    }

    if (selectedMode != null) {
        val mode = selectedMode!!
        val subPageItems = remember(items, mode, subSearchQuery) {
            items.map { item ->
                val (cash, online) = getCollectionSplit(item)
                val targetAmt = if (mode == "ONLINE") online else cash
                Pair(item, targetAmt)
            }.filter { (item, amt) ->
                val matchesMode = amt > 0.0
                val matchesQuery = subSearchQuery.isBlank() || 
                                   item.customerName.contains(subSearchQuery, ignoreCase = true) || 
                                   item.customerCode.contains(subSearchQuery, ignoreCase = true)
                matchesMode && matchesQuery
            }
        }
        val subSum = if (mode == "ONLINE") onlineCollectionSum else cashCollectionSum
        val titleText = if (mode == "ONLINE") translate("UPI Collections", language) else translate("Cash Collections", language)
        val gradientColors = if (mode == "ONLINE") listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8)) else listOf(Color(0xFF065F46), Color(0xFF0F766E))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { 
                        selectedMode = null
                        subSearchQuery = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (appColors.isDark) Color.White else Color.Black
                    )
                }
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (appColors.isDark) Color.White else Color.Black
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = gradientColors))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = translate("TOTAL", language),
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹ ${String.format(Locale.US, "%,.2f", subSum)}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${subPageItems.size} " + translate("Instalments", language),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subSearchQuery,
                onValueChange = { subSearchQuery = it },
                placeholder = { Text(translate("Search customer...", language), color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (subSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { subSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White,
                    unfocusedContainerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White,
                    focusedBorderColor = appColors.primaryAccent,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = if (appColors.isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (appColors.isDark) Color.White else Color.Black
                )
            )

            if (subPageItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = translate("No matching records found.", language),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subPageItems) { (item, amt) ->
                        val cleanNotes = item.notes.trim()
                        val isOnline = isItemOnline(item)
                        val isMultiple = cleanNotes.startsWith("Multiple", ignoreCase = true)
                        val modeText = when {
                            isMultiple -> "MULTIPLE"
                            isOnline -> "UPI"
                            else -> "CASH"
                        }
                        val badgeBg = when (modeText) {
                            "MULTIPLE" -> Color(0xFFF3E8FF)
                            "UPI" -> Color(0xFFDBEAFE)
                            else -> Color(0xFFDCFCE7)
                        }
                        val badgeContentColor = when (modeText) {
                            "MULTIPLE" -> Color(0xFF5B21B6)
                            "UPI" -> Color(0xFF1E3A8A)
                            else -> Color(0xFF14532D)
                        }
                        val badgeIcon = when (modeText) {
                            "MULTIPLE" -> Icons.Filled.Payments
                            "UPI" -> Icons.Filled.AccountBalance
                            else -> Icons.Filled.MonetizationOn
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditDialog = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = RowBorderStroke(if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${item.customOrder}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.customerName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (appColors.isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A),
                                            modifier = Modifier.clickable {
                                                viewModel.navigateTo(Screen.CustomerDetail(item.customerId))
                                            }.padding(vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = (if (appColors.isDark) Color(0xFF475569) else Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.customerCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray
                                            )
                                        }
                                        Text(
                                            text = translate("Week", language) + " ${item.weekNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (appColors.isDark) Color.White else Color.Black
                                        )

                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, badgeContentColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = badgeIcon,
                                                    contentDescription = modeText,
                                                    tint = badgeContentColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = modeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = badgeContentColor,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                    if (item.notes.isNotBlank() || !item.upiTxnId.isNullOrBlank()) {
                                        val displayNotes = if (!item.upiTxnId.isNullOrBlank()) {
                                            "UPI ID: ${item.upiTxnId}"
                                        } else {
                                            val filterPrefix = item.notes.replace("Cash", "", ignoreCase = true)
                                                .replace("Online", "", ignoreCase = true)
                                                .replace("-", "")
                                                .trim()
                                            if (filterPrefix.isNotBlank()) "Notes: $filterPrefix" else ""
                                        }
                                        if (displayNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = displayNotes,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isDark) Color.White else Color(0xFF0F172A),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "₹ ${String.format(Locale.US, "%,.0f", amt)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = if (appColors.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = translate("TOTAL COLLECTIONS", language),
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹ ${String.format(Locale.US, "%,.2f", totalSum)}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.15f),
                            border = RowBorderStroke(Color(0xFF22C55E))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${items.size} " + translate("Instalments Received", language),
                                    color = Color(0xFF22C55E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMode = "CASH" }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = translate("Cash", language),
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹ ${String.format(Locale.US, "%,.2f", cashCollectionSum)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMode = "ONLINE" }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = translate("UPI", language),
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹ ${String.format(Locale.US, "%,.2f", onlineCollectionSum)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = translate("Instalment Log Breakdown", language),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (appColors.isDark) Color.White else Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = translate("No collections made today.", language),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        val cleanNotes = item.notes.trim()
                        val isOnline = isItemOnline(item)
                        val isMultiple = cleanNotes.startsWith("Multiple", ignoreCase = true)
                        val modeText = when {
                            isMultiple -> "MULTIPLE"
                            isOnline -> "UPI"
                            else -> "CASH"
                        }
                        val badgeBg = when (modeText) {
                            "MULTIPLE" -> Color(0xFFF3E8FF)
                            "UPI" -> Color(0xFFDBEAFE)
                            else -> Color(0xFFDCFCE7)
                        }
                        val badgeContentColor = when (modeText) {
                            "MULTIPLE" -> Color(0xFF5B21B6)
                            "UPI" -> Color(0xFF1E3A8A)
                            else -> Color(0xFF14532D)
                        }
                        val badgeIcon = when (modeText) {
                            "MULTIPLE" -> Icons.Filled.Payments
                            "UPI" -> Icons.Filled.AccountBalance
                            else -> Icons.Filled.MonetizationOn
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditDialog = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = RowBorderStroke(if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${item.customOrder}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.customerName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (appColors.isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A),
                                            modifier = Modifier.clickable {
                                                viewModel.navigateTo(Screen.CustomerDetail(item.customerId))
                                            }.padding(vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = (if (appColors.isDark) Color(0xFF475569) else Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.customerCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray
                                            )
                                        }
                                        Text(
                                            text = translate("Week", language) + " ${item.weekNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (appColors.isDark) Color.White else Color.Black
                                        )

                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, badgeContentColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = badgeIcon,
                                                    contentDescription = modeText,
                                                    tint = badgeContentColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = modeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = badgeContentColor,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                    if (item.notes.isNotBlank() || !item.upiTxnId.isNullOrBlank()) {
                                        val displayNotes = if (!item.upiTxnId.isNullOrBlank()) {
                                            "UPI ID: ${item.upiTxnId}"
                                        } else {
                                            val filterPrefix = item.notes.replace("Cash", "", ignoreCase = true)
                                                .replace("Online", "", ignoreCase = true)
                                                .replace("-", "")
                                                .trim()
                                            if (filterPrefix.isNotBlank()) "Notes: $filterPrefix" else ""
                                        }
                                        if (displayNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = displayNotes,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isDark) Color.White else Color(0xFF0F172A),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "₹ ${String.format(Locale.US, "%,.0f", item.amount)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = if (appColors.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisbursalDetailLayout(
    items: List<DisbursalDetailItem>,
    totalSum: Double,
    padding: PaddingValues,
    appColors: AppThemeColors,
    language: String,
    viewModel: FinanceViewModel,
    initialMode: String? = null
) {
    var selectedMode by remember { mutableStateOf<String?>(initialMode) }
    var subSearchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedMode != null) {
        selectedMode = null
        subSearchQuery = ""
    }

    fun getDisbursalSplit(item: DisbursalDetailItem): Pair<Double, Double> {
        val notes = item.notes.trim()
        if (notes.startsWith("Multiple - ", ignoreCase = true)) {
            try {
                val cashMarker = "Cash: ₹"
                val onlineMarker = "Online: ₹"
                val upiMarker = "UPI: ₹"
                val cashStart = notes.indexOf(cashMarker)
                val onlineStart = notes.indexOf(onlineMarker)
                val upiStart = notes.indexOf(upiMarker)
                
                val oStart = if (onlineStart != -1) onlineStart else upiStart
                val oMarker = if (onlineStart != -1) onlineMarker else upiMarker
                
                if (cashStart != -1) {
                    val cashVal = parseAmountFromString(notes.substring(cashStart + cashMarker.length))
                    val onlineVal = if (oStart != -1) {
                        parseAmountFromString(notes.substring(oStart + oMarker.length))
                    } else {
                        0.0
                    }
                    return Pair(cashVal, onlineVal)
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        
        val isOnline = notes.contains("Online", ignoreCase = true) || 
                       notes.contains("UPI", ignoreCase = true) || 
                       notes.contains("GPay", ignoreCase = true) || 
                       notes.contains("PhonePe", ignoreCase = true) || 
                       notes.contains("Paytm", ignoreCase = true) || 
                       notes.contains("Bank", ignoreCase = true) ||
                       notes.contains("Google Pay", ignoreCase = true) ||
                       notes.contains("Phone Pe", ignoreCase = true) ||
                       notes.contains("IMPS", ignoreCase = true) ||
                       notes.contains("NEFT", ignoreCase = true) ||
                       notes.contains("RTGS", ignoreCase = true) ||
                       notes.contains("Net", ignoreCase = true) ||
                       notes.contains("Transfer", ignoreCase = true)
        
        return if (isOnline) {
            Pair(0.0, item.actualDisbursed)
        } else {
            Pair(item.actualDisbursed, 0.0)
        }
    }

    val disbursalSplit = remember(items) {
        var cashSum = 0.0
        var onlineSum = 0.0
        for (item in items) {
            val notes = item.notes.trim()
            if (notes.startsWith("Multiple - ", ignoreCase = true)) {
                try {
                    val cashMarker = "Cash: ₹"
                    val onlineMarker = "Online: ₹"
                    val upiMarker = "UPI: ₹"
                    val cashStart = notes.indexOf(cashMarker)
                    val onlineStart = notes.indexOf(onlineMarker)
                    val upiStart = notes.indexOf(upiMarker)
                    
                    val oStart = if (onlineStart != -1) onlineStart else upiStart
                    val oMarker = if (onlineStart != -1) onlineMarker else upiMarker
                    
                    if (cashStart != -1) {
                        val cashVal = parseAmountFromString(notes.substring(cashStart + cashMarker.length))
                        val onlineVal = if (oStart != -1) {
                            parseAmountFromString(notes.substring(oStart + oMarker.length))
                        } else {
                            0.0
                        }
                        cashSum += cashVal
                        onlineSum += onlineVal
                    } else {
                        cashSum += item.actualDisbursed
                    }
                } catch (e: Exception) {
                    cashSum += item.actualDisbursed
                }
            } else {
                val isOnline = notes.contains("Online", ignoreCase = true) || 
                               notes.contains("UPI", ignoreCase = true) || 
                               notes.contains("GPay", ignoreCase = true) || 
                               notes.contains("PhonePe", ignoreCase = true) || 
                               notes.contains("Paytm", ignoreCase = true) || 
                               notes.contains("Bank", ignoreCase = true) ||
                               notes.contains("Google Pay", ignoreCase = true) ||
                               notes.contains("Phone Pe", ignoreCase = true) ||
                               notes.contains("IMPS", ignoreCase = true) ||
                               notes.contains("NEFT", ignoreCase = true) ||
                               notes.contains("RTGS", ignoreCase = true) ||
                               notes.contains("Net", ignoreCase = true) ||
                               notes.contains("Transfer", ignoreCase = true)
                if (isOnline) {
                    onlineSum += item.actualDisbursed
                } else {
                    cashSum += item.actualDisbursed
                }
            }
        }
        Pair(cashSum, onlineSum)
    }
    val cashDisbursalSum = disbursalSplit.first
    val onlineDisbursalSum = disbursalSplit.second

    if (selectedMode != null) {
        val mode = selectedMode!!
        val subPageItems = remember(items, mode, subSearchQuery) {
            items.map { item ->
                val (cash, online) = getDisbursalSplit(item)
                val targetAmt = if (mode == "ONLINE") online else cash
                Pair(item, targetAmt)
            }.filter { (item, amt) ->
                val matchesMode = amt > 0.0
                val matchesQuery = subSearchQuery.isBlank() || 
                                   item.customerName.contains(subSearchQuery, ignoreCase = true) || 
                                   item.customerCode.contains(subSearchQuery, ignoreCase = true)
                matchesMode && matchesQuery
            }
        }
        val subSum = if (mode == "ONLINE") onlineDisbursalSum else cashDisbursalSum
        val titleText = if (mode == "ONLINE") translate("UPI Disbursals", language) else translate("Cash Disbursals", language)
        val gradientColors = if (mode == "ONLINE") listOf(Color(0xFF312E81), Color(0xFF4338CA)) else listOf(Color(0xFF831843), Color(0xFF9D174D))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { 
                        selectedMode = null
                        subSearchQuery = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (appColors.isDark) Color.White else Color.Black
                    )
                }
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (appColors.isDark) Color.White else Color.Black
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = gradientColors))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = translate("TOTAL NET", language),
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹ ${String.format(Locale.US, "%,.2f", subSum)}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${subPageItems.size} " + translate("Contracts", language),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subSearchQuery,
                onValueChange = { subSearchQuery = it },
                placeholder = { Text(translate("Search customer...", language), color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (subSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { subSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White,
                    unfocusedContainerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White,
                    focusedBorderColor = appColors.primaryAccent,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = if (appColors.isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (appColors.isDark) Color.White else Color.Black
                )
            )

            if (subPageItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = translate("No matching records found.", language),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subPageItems) { (item, amt) ->
                        val cleanNotes = item.notes.trim()
                        val isOnline = cleanNotes.startsWith("Online", ignoreCase = true) || cleanNotes.startsWith("UPI", ignoreCase = true)
                        val isMultiple = cleanNotes.startsWith("Multiple", ignoreCase = true)
                        val modeText = when {
                            isMultiple -> "MULTIPLE"
                            isOnline -> "UPI"
                            else -> "CASH"
                        }
                        val badgeBg = when (modeText) {
                            "MULTIPLE" -> Color(0xFFF3E8FF)
                            "UPI" -> Color(0xFFDBEAFE)
                            else -> Color(0xFFDCFCE7)
                        }
                        val badgeContentColor = when (modeText) {
                            "MULTIPLE" -> Color(0xFF5B21B6)
                            "UPI" -> Color(0xFF1E3A8A)
                            else -> Color(0xFF14532D)
                        }
                        val badgeIcon = when (modeText) {
                            "MULTIPLE" -> Icons.Filled.Payments
                            "UPI" -> Icons.Filled.AccountBalance
                            else -> Icons.Filled.MonetizationOn
                        }
                        val labelColor = if (appColors.isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        val valueColor = if (appColors.isDark) Color.White else Color(0xFF0F172A)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = RowBorderStroke(if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = "${item.customOrder}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (appColors.isDark) Color.LightGray else Color.DarkGray,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = item.customerName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = if (appColors.isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A),
                                                modifier = Modifier.clickable {
                                                    viewModel.navigateTo(Screen.CustomerDetail(item.customerId))
                                                }.padding(vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = (if (appColors.isDark) Color(0xFF475569) else Color(0xFFF1F5F9)),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.wrapContentSize()
                                            ) {
                                                Text(
                                                    text = item.customerCode,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    color = if (appColors.isDark) Color.LightGray else Color.DarkGray
                                                )
                                            }

                                            Surface(
                                                color = badgeBg,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, badgeContentColor.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = badgeIcon,
                                                        contentDescription = modeText,
                                                        tint = badgeContentColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = modeText,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = badgeContentColor,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹ ${String.format(Locale.US, "%,.0f", amt)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.clickable {
                                                viewModel.navigateTo(Screen.EditLoan(item.loanCycleId))
                                            }.padding(4.dp)
                                        )
                                        Text(
                                            text = if (mode == "ONLINE") translate("UPI Portion", language) else translate("Cash Portion", language),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = labelColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(translate("Loan Principal", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                        Text("₹ ${String.format(Locale.US, "%,.0f", item.loanAmount)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                    }
                                    Column {
                                        Text(translate("Interest", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                        Text("₹ ${String.format(Locale.US, "%,.0f", item.interestAmount)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                    }
                                    Column {
                                        Text(translate("Deduction", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                        Text("₹ ${String.format(Locale.US, "%,.0f", item.deduction)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(translate("Weekly Due", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                        Text("₹ ${String.format(Locale.US, "%,.0f", item.weeklyAmount)} /wk", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
        // Hero Card showing Total
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = translate("TOTAL NET CASH DISBURSED", language),
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹ ${String.format(Locale.US, "%,.2f", totalSum)}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF43F5E).copy(alpha = 0.15f),
                        border = RowBorderStroke(Color(0xFFF43F5E))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${items.size} " + translate("Contracts Disbursed", language),
                                color = Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMode = "CASH" }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = translate("Cash", language),
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹ ${String.format(Locale.US, "%,.2f", cashDisbursalSum)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMode = "ONLINE" }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = translate("UPI", language),
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹ ${String.format(Locale.US, "%,.2f", onlineDisbursalSum)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = translate("Disbursed Loans Breakdown", language),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (appColors.isDark) Color.White else Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = translate("No disbursals made today.", language),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    val cleanNotes = item.notes.trim()
                    val isOnline = cleanNotes.startsWith("Online", ignoreCase = true) || cleanNotes.startsWith("UPI", ignoreCase = true)
                    val isMultiple = cleanNotes.startsWith("Multiple", ignoreCase = true)
                    val modeText = when {
                        isMultiple -> "MULTIPLE"
                        isOnline -> "UPI"
                        else -> "CASH"
                    }
                    val badgeBg = when (modeText) {
                        "MULTIPLE" -> Color(0xFFF3E8FF)
                        "UPI" -> Color(0xFFDBEAFE)
                        else -> Color(0xFFDCFCE7)
                    }
                    val badgeContentColor = when (modeText) {
                        "MULTIPLE" -> Color(0xFF5B21B6)
                        "UPI" -> Color(0xFF1E3A8A)
                        else -> Color(0xFF14532D)
                    }
                    val badgeIcon = when (modeText) {
                        "MULTIPLE" -> Icons.Filled.Payments
                        "UPI" -> Icons.Filled.AccountBalance
                        else -> Icons.Filled.MonetizationOn
                    }
                    val labelColor = if (appColors.isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    val valueColor = if (appColors.isDark) Color.White else Color(0xFF0F172A)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = RowBorderStroke(if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${item.customOrder}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.customerName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (appColors.isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A),
                                            modifier = Modifier.clickable {
                                                viewModel.navigateTo(Screen.CustomerDetail(item.customerId))
                                            }.padding(vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = (if (appColors.isDark) Color(0xFF475569) else Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = item.customerCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                color = if (appColors.isDark) Color.LightGray else Color.DarkGray
                                            )
                                        }

                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, badgeContentColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = badgeIcon,
                                                    contentDescription = modeText,
                                                    tint = badgeContentColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = modeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = badgeContentColor,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%,.0f", item.actualDisbursed)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.clickable {
                                            viewModel.navigateTo(Screen.EditLoan(item.loanCycleId))
                                        }.padding(4.dp)
                                    )
                                    Text(
                                        text = translate("Net Disbursed", language),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = labelColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(translate("Loan Principal", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("₹ ${String.format(Locale.US, "%,.0f", item.loanAmount)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                }
                                Column {
                                    Text(translate("Interest", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("₹ ${String.format(Locale.US, "%,.0f", item.interestAmount)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                }
                                Column {
                                    Text(translate("Deduction", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("₹ ${String.format(Locale.US, "%,.0f", item.deduction)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(translate("Weekly Due", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                    Text("₹ ${String.format(Locale.US, "%,.0f", item.weeklyAmount)} /wk", fontWeight = FontWeight.Black, fontSize = 12.sp, color = valueColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
fun ProfitDetailLayout(
    items: List<ProfitDetailItem>,
    totalSum: Double,
    padding: PaddingValues,
    appColors: AppThemeColors,
    language: String,
    viewModel: FinanceViewModel,
    title: String = translate("TOTAL REALIZED PROFIT", language)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        // Hero Card showing Total
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF581C87), Color(0xFF6B21A8))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹ ${String.format(Locale.US, "%,.2f", totalSum)}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFA855F7).copy(alpha = 0.15f),
                        border = RowBorderStroke(Color(0xFFA855F7))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Percent,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${items.size} " + translate("Elements Today", language),
                                color = Color(0xFFA855F7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = translate("Generation Log", language),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (appColors.isDark) Color.White else Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = translate("No items recorded today.", language),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = RowBorderStroke(if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (appColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${item.customOrder}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (appColors.isDark) Color.LightGray else Color.DarkGray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (appColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                        modifier = Modifier.clickable {
                                            viewModel.navigateTo(Screen.CustomerDetail(item.customerId))
                                        }.padding(vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = (if (appColors.isDark) Color(0xFF475569) else Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.customerCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = if (appColors.isDark) Color.LightGray else Color.DarkGray
                                        )
                                    }
                                    Text(
                                        text = item.details,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "₹ ${String.format(Locale.US, "%,.1f", item.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFA855F7),
                                modifier = Modifier.clickable {
                                    if (item.loanCycleId != null) {
                                        if (item.source.contains("Deduction")) {
                                            viewModel.navigateTo(Screen.EditLoan(item.loanCycleId))
                                        } else {
                                            viewModel.navigateTo(Screen.RecordPayment(item.loanCycleId))
                                        }
                                    }
                                }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowBorderStroke(color: Color) = BorderStroke(1.dp, color.copy(alpha = 0.3f))

fun parseAmountFromString(str: String): Double {
    val clean = StringBuilder()
    var dotCount = 0
    for (char in str) {
        if (char.isDigit()) {
            clean.append(char)
        } else if (char == '.' && dotCount == 0) {
            clean.append(char)
            dotCount++
        } else {
            if (clean.isNotEmpty()) break
        }
    }
    return clean.toString().toDoubleOrNull() ?: 0.0
}
