package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Customer
import com.example.data.LoanCycle
import com.example.data.WeeklyPayment
import com.example.util.CurrencyFormatter
import com.example.util.CalendarWeek
import com.example.util.DateUtils
import com.example.util.StatementGenerator

import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val WhatsAppIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WhatsAppIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = androidx.compose.ui.graphics.SolidColor(Color(0xFF22C55E)),
        strokeLineWidth = 1.5f,
        fill = null
    ) {
        // Standard WhatsApp outer speech bubble shape as outline
        moveTo(12.003f, 4.078f)
        curveTo(7.628f, 4.078f, 4.078f, 7.628f, 4.078f, 12.003f)
        curveTo(4.078f, 13.403f, 4.444f, 14.719f, 5.084f, 15.878f)
        lineTo(4.078f, 19.922f)
        lineTo(8.216f, 18.844f)
        curveTo(9.344f, 19.459f, 10.634f, 19.928f, 12.003f, 19.928f)
        curveTo(16.378f, 19.928f, 19.922f, 16.378f, 19.922f, 12.003f)
        curveTo(19.922f, 7.628f, 16.378f, 4.078f, 12.003f, 4.078f)
        close()
    }.path(
        fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF22C55E)),
        stroke = null,
        strokeLineWidth = 0f
    ) {
        // Professional WhatsApp telephone receiver cutout shape
        moveTo(15.835f, 16.128f)
        curveTo(15.588f, 16.825f, 14.621f, 17.411f, 14.169f, 17.469f)
        curveTo(13.716f, 17.527f, 13.235f, 17.579f, 11.263f, 16.761f)
        curveTo(8.736f, 15.714f, 7.123f, 13.128f, 6.997f, 12.96f)
        curveTo(6.87f, 12.792f, 5.972f, 11.595f, 5.972f, 10.358f)
        curveTo(5.972f, 9.121f, 6.622f, 8.512f, 6.855f, 8.267f)
        curveTo(7.088f, 8.022f, 7.365f, 7.962f, 7.535f, 7.962f)
        curveTo(7.705f, 7.962f, 7.875f, 7.962f, 8.025f, 7.971f)
        curveTo(8.185f, 7.98f, 8.4f, 7.91f, 8.612f, 8.423f)
        curveTo(8.832f, 8.954f, 9.362f, 10.254f, 9.427f, 10.388f)
        curveTo(9.491f, 10.521f, 9.533f, 10.676f, 9.443f, 10.855f)
        curveTo(9.353f, 11.035f, 9.308f, 11.146f, 9.173f, 11.305f)
        curveTo(9.038f, 11.463f, 8.89f, 11.659f, 8.77f, 11.78f)
        curveTo(8.635f, 11.917f, 8.492f, 12.068f, 8.65f, 12.34f)
        curveTo(8.808f, 12.612f, 9.354f, 13.497f, 10.157f, 14.214f)
        curveTo(10.96f, 14.931f, 11.637f, 15.154f, 11.845f, 15.257f)
        curveTo(12.053f, 15.36f, 12.177f, 15.347f, 12.301f, 15.205f)
        curveTo(12.425f, 15.063f, 12.833f, 14.586f, 12.975f, 14.375f)
        curveTo(13.117f, 14.165f, 13.259f, 14.2f, 13.454f, 14.272f)
        curveTo(13.649f, 14.343f, 14.691f, 14.856f, 14.904f, 14.962f)
        curveTo(15.117f, 15.069f, 15.259f, 15.121f, 15.311f, 15.211f)
        curveTo(15.363f, 15.301f, 15.363f, 15.732f, 15.116f, 16.429f)
        close()
    }.build()
}

private fun shareLocalCustomerFile(context: android.content.Context, customer: Customer, fileExtension: String, viewModel: FinanceViewModel) {
    try {
        val groupsDir = context.getExternalFilesDir("Groups") ?: return
        val dayDir = java.io.File(groupsDir, customer.collectionDay)
        val safeName = customer.name.replace(Regex("[^a-zA-Z0-9_ -]"), "").trim().replace(" ", "_")
        val customerFolder = java.io.File(dayDir, "${customer.customerCode}_$safeName")
        val file = java.io.File(customerFolder, "details.$fileExtension")
        
        if (!file.exists()) {
            Toast.makeText(context, "Preparing dynamic files...", Toast.LENGTH_SHORT).show()
            viewModel.createOrUpdateCustomerFiles(customer.id)
        }
        
        if (file.exists()) {
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when (fileExtension) {
                "txt" -> "text/plain"
                "html" -> "text/html"
                "json" -> "application/json"
                else -> "*/*"
            }
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Code Page File (.$fileExtension)"))
        } else {
            Toast.makeText(context, "Preparing ledger details. Please try sharing again in a moment.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_LONG).show()
    }
}



@Composable
fun PaymentCalendarMatrix(payments: List<WeeklyPayment>, loanCycles: List<LoanCycle>) {
    val past12Months = remember {
        val list = mutableListOf<Pair<Int, Int>>() // Pair(monthIndex 0..11, year)
        val startCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -11)
        }
        for (i in 0 until 12) {
            list.add(Pair(startCal.get(Calendar.MONTH), startCal.get(Calendar.YEAR)))
            startCal.add(Calendar.MONTH, 1)
        }
        list
    }
    
    val allWeeksInPast12Months = remember(past12Months) {
        val yearsNeeded = past12Months.map { it.second }.distinct()
        val weeksList = mutableListOf<Triple<CalendarWeek, Int, Int>>() // Triple(week, monthIndex, year)
        for (yr in yearsNeeded) {
            val weeksOfYr = DateUtils.getCalendarWeeksForYear(yr)
            for (week in weeksOfYr) {
                weeksList.add(Triple(week, week.assignedMonth, yr))
            }
        }
        weeksList
    }
    
    val groupedWeeksByMonthYear = remember(allWeeksInPast12Months, past12Months) {
        past12Months.map { (m, y) ->
            allWeeksInPast12Months.filter { it.second == m && it.third == y }.map { it.first }
        }
    }

    // Helper to determine cover period 
    fun isWeekCoveredByLoan(startMs: Long, endMs: Long, loan: LoanCycle, payments: List<WeeklyPayment>): Boolean {
        val loanPayments = payments.filter { it.loanCycleId == loan.id && it.status.uppercase() != "DELETED" }
        val firstPaymentDate = loanPayments.minOfOrNull { it.paymentDate }
        
        // A dispersal date is considered wrong/incorrect if:
        // - It is after the first payment date (since dispersal must happen before or on the first collection).
        // - Or if it's very close to today/now (e.g. imported fallback) but we have older payments.
        val isDispersalDateWrong = firstPaymentDate != null && (
            loan.startDate > firstPaymentDate || 
            (Math.abs(loan.startDate - System.currentTimeMillis()) < 60_000 && firstPaymentDate < loan.startDate - 60_000)
        )
        
        val coverStart = if (isDispersalDateWrong && firstPaymentDate != null) {
            firstPaymentDate
        } else {
            loan.startDate
        }
        
        val coverEnd = if (loan.status.uppercase() == "ACTIVE") {
            System.currentTimeMillis()
        } else {
            // PAID
            loanPayments.maxOfOrNull { it.paymentDate } ?: loan.startDate
        }
        return endMs >= coverStart && startMs <= coverEnd
    }

    // Dynamic calculations representing the calendar map's visible space
    val counts = remember(groupedWeeksByMonthYear, payments, loanCycles) {
        var totalWeeksCount = 0
        var weeksPaidCount = 0
        groupedWeeksByMonthYear.forEach { weeksOfThisMonth ->
            weeksOfThisMonth.forEach { week ->
                val startMs = week.mondayTime
                val endMs = week.mondayTime + 7L * 24 * 3600 * 1000 - 1
                
                val isCovered = loanCycles.any { loan ->
                    isWeekCoveredByLoan(startMs, endMs, loan, payments)
                }
                val isDisbursedThisWeek = loanCycles.any { loan ->
                    loan.startDate in startMs..endMs
                }
                
                if (isCovered && !isDisbursedThisWeek) {
                    totalWeeksCount++
                    val isPaid = payments.any { p -> p.status == "ACTIVE" && p.amountPaid > 0.0 && p.paymentDate in startMs..endMs }
                    if (isPaid) {
                        weeksPaidCount++
                    }
                }
            }
        }
        Pair(weeksPaidCount, totalWeeksCount)
    }
    
    val weeksPaid = counts.first
    val totalWeeks = counts.second

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar Map",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSlateDark
                )
                if (totalWeeks > 0) {
                    Text(
                        text = "Weeks Paid: $weeksPaid / $totalWeeks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorSlateDark
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (colIndex in 0..11) {
                    val weeksOfThisMonth = groupedWeeksByMonthYear[colIndex]
                    val monthYearPair = past12Months[colIndex]
                    val monthNumberString = (monthYearPair.first + 1).toString()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = monthNumberString,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        for (weekIndex in 0..4) {
                            if (weekIndex < weeksOfThisMonth.size) {
                                val week = weeksOfThisMonth[weekIndex]
                                val startMs = week.mondayTime
                                val endMs = week.mondayTime + 7L * 24 * 3600 * 1000 - 1
                                
                                val isCovered = loanCycles.any { loan ->
                                    isWeekCoveredByLoan(startMs, endMs, loan, payments)
                                }
                                val isPaid = payments.any { p ->
                                    p.status == "ACTIVE" && p.amountPaid > 0.0 && p.paymentDate in startMs..endMs
                                }
                                val isDisbursedThisWeek = loanCycles.any { loan ->
                                    loan.startDate in startMs..endMs
                                }
                                
                                val dotColor = if (isDisbursedThisWeek) {
                                    Color(0xFFFFD700) // Yellow / Gold for disbursal week
                                } else if (isCovered) {
                                    if (isPaid) ColorGainGreen else ColorLossRed
                                } else {
                                    Color(0xFFCBD5E1) // Grey out point if no active cycle represents this duration
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = dotColor,
                                            shape = CircleShape
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = Color.Transparent,
                                            shape = CircleShape
                                        )
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
fun CustomerDetailScreen(
    customerId: Int,
    viewModel: FinanceViewModel
) {
    val appColors = LocalAppThemeColors.current
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Past History

    LaunchedEffect(customerId) {
        viewModel.recalibrateCalculationsSilent()
    }
    
    val customerList: List<Customer> by viewModel.allCustomers.collectAsStateWithLifecycle()
    val loanCycles: List<LoanCycle> by viewModel.getLoanCyclesForCustomer(customerId).collectAsStateWithLifecycle(initialValue = emptyList())
    val upiLinkVal by viewModel.upiLink.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    
    val customer = customerList.find { it.id == customerId }
    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Customer details not found.")
        }
        return
    }

    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var deletingCycleTarget by remember { mutableStateOf<LoanCycle?>(null) }
    var deletingPaymentTarget by remember { mutableStateOf<WeeklyPayment?>(null) }
    var editingPayment by remember { mutableStateOf<WeeklyPayment?>(null) }

    // Direct Interaction Heads-up state variables
    var showCallAlert by remember { mutableStateOf(false) }
    var showSmsAlert by remember { mutableStateOf(false) }
    var showUpiAlert by remember { mutableStateOf(false) }
    var showWhatsappAlert by remember { mutableStateOf(false) }

    val phoneParts = remember(customer.phone, customer.phone2) {
        val splitFromFirst = customer.phone.split(Regex("[/,;]")).map { it.trim() }.filter { it.isNotBlank() }
        val p1 = splitFromFirst.getOrNull(0) ?: customer.phone.trim()
        val p2 = if (customer.phone2.isNotBlank()) {
            customer.phone2.trim()
        } else {
            splitFromFirst.getOrNull(1) ?: ""
        }
        Pair(p1, p2)
    }
    val primaryPhone = phoneParts.first
    val secondaryPhone = phoneParts.second
    val hasTwoPhones = primaryPhone.isNotBlank() && secondaryPhone.isNotBlank()

    var selectedCallPhone by remember(customer.id, primaryPhone, secondaryPhone) { mutableStateOf(primaryPhone) }
    var selectedSmsPhone by remember(customer.id, primaryPhone, secondaryPhone) { mutableStateOf(primaryPhone) }
    var selectedWhatsappPhone by remember(customer.id, primaryPhone, secondaryPhone) { mutableStateOf(primaryPhone) }
    var selectedStatementPhone by remember(customer.id, primaryPhone, secondaryPhone) { mutableStateOf(primaryPhone) }
    var selectedUpiPhone by remember(customer.id, primaryPhone, secondaryPhone) { mutableStateOf(primaryPhone) }

    // Heads-up Dialogs
    if (showCallAlert) {
        AlertDialog(
            onDismissRequest = { showCallAlert = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Call Confirmation", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                if (hasTwoPhones) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select the number you want to call:", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCallPhone = primaryPhone }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedCallPhone == primaryPhone),
                                onClick = { selectedCallPhone = primaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = ColorGainGreen)
                            )
                            Text("Primary: $primaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCallPhone = secondaryPhone }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedCallPhone == secondaryPhone),
                                onClick = { selectedCallPhone = secondaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = ColorGainGreen)
                            )
                            Text("Secondary: $secondaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Text("Are you sure you want to call ${customer.name} now?", color = Color.Black)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCallAlert = false
                        try {
                            val targetNumber = if (hasTwoPhones) selectedCallPhone else primaryPhone
                            val cleanNumber = targetNumber.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$cleanNumber")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error making call: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGainGreen)
                ) {
                    Text("Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCallAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSmsAlert) {
        val firstActive = loanCycles.firstOrNull { it.status == "ACTIVE" }
        AlertDialog(
            onDismissRequest = { showSmsAlert = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("SMS Confirmation", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                if (hasTwoPhones) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select the number to send collection reminder SMS:", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSmsPhone = primaryPhone }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSmsPhone == primaryPhone),
                                onClick = { selectedSmsPhone = primaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = ColorAccentBlue)
                            )
                            Text("Primary: $primaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSmsPhone = secondaryPhone }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSmsPhone == secondaryPhone),
                                onClick = { selectedSmsPhone = secondaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = ColorAccentBlue)
                            )
                            Text("Secondary: $secondaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Text("Are you sure you want to send a collection reminder SMS to ${customer.name}?", color = Color.Black)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSmsAlert = false
                        val rawNumber = if (hasTwoPhones) selectedSmsPhone else primaryPhone
                        val targetNumber = rawNumber.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        if (firstActive != null) {
                            viewModel.triggerManualReminderSms(customer, firstActive, targetNumber)
                        } else {
                            try {
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$targetNumber")
                                    putExtra("sms_body", "Hi ${customer.name}, this is a gentle reminder regarding your outstanding weekly balance.")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(smsIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sending SMS: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue)
                ) {
                    Text("Send SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWhatsappAlert) {
        val activeLoan = loanCycles.firstOrNull { it.status == "ACTIVE" } ?: loanCycles.firstOrNull() ?: LoanCycle(id = 0, customerId = customer.id, loanAmount = 0.0, interestAmount = 0.0, weeklyAmount = 0.0, totalWeeks = 0, startDate = 0L)
        AlertDialog(
            onDismissRequest = { showWhatsappAlert = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("WhatsApp Confirmation", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                if (hasTwoPhones) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select the number to send collection reminder WhatsApp message:", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWhatsappPhone = primaryPhone }
                                .padding(vertical = 4.dp)
                                .testTag("whatsapp_phone_primary_option")
                        ) {
                            RadioButton(
                                selected = (selectedWhatsappPhone == primaryPhone),
                                onClick = { selectedWhatsappPhone = primaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF25D366))
                            )
                            Text("Primary: $primaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWhatsappPhone = secondaryPhone }
                                .padding(vertical = 4.dp)
                                .testTag("whatsapp_phone_secondary_option")
                        ) {
                            RadioButton(
                                selected = (selectedWhatsappPhone == secondaryPhone),
                                onClick = { selectedWhatsappPhone = secondaryPhone },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF25D366))
                            )
                            Text("Secondary: $secondaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Text("Are you sure you want to send a collection reminder WhatsApp message to ${customer.name}?", color = Color.Black)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWhatsappAlert = false
                        val rawNumber = if (hasTwoPhones) selectedWhatsappPhone else primaryPhone
                        val targetNumber = rawNumber.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        viewModel.triggerWhatsappReminder(customer, activeLoan, targetNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.testTag("confirm_send_whatsapp_button")
                ) {
                    Text("Send WhatsApp", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsappAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpiAlert) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val activeUpiPhone = if (hasTwoPhones) selectedUpiPhone else primaryPhone
        val rawPhone = activeUpiPhone.replace("[^0-9]".toRegex(), "")
        val cleanPhone = if (rawPhone.length >= 10) rawPhone.takeLast(10) else rawPhone
        val defaultUpi = if (customer.upiNameAlias.contains("@")) {
            customer.upiNameAlias.trim()
        } else if (cleanPhone.isNotEmpty()) {
            "${cleanPhone}@ybl"
        } else {
            ""
        }
        var upiIdToPay by remember(customer, cleanPhone) { mutableStateOf(defaultUpi) }

        val upiTitle = translate("UPI Payment", language)
        val upiText = when (language) {
            "Tamil" -> "${customer.name}-க்கு பணம் செலுத்த UPI செயலியைத் திறக்கலாமா? முன்னிருப்பாக வாடிக்கையாளரின் தொலைபேசி எண் UPI முகவரியாக (${cleanPhone}@ybl) பயன்படுத்தப்படும். திறந்ததும், நீங்கள் தொகையை உள்ளிட்டு பணம் செலுத்தலாம்."
            "Hindi" -> "क्या आप ${customer.name} को भुगतान करने के लिए UPI ऐप खोलना चाहते हैं? डिफ़ॉल्ट रूप से, ग्राहक का मोबाइल नंबर UPI पता (${cleanPhone}@ybl) के रूप में उपयोग किया जाएगा। एक बार UPI ऐप खुलने के बाद, आप भुगतान करने के लिए राशि दर्ज कर सकते हैं।"
            "Telugu" -> "${customer.name} కి చెల్లించడానికి UPI యాప్‌ని తెరవాలా? డిఫాల్ట్‌గా, కస్టమర్ మొబైల్ నంబర్ UPI చిరునామా (${cleanPhone}@ybl)గా ఉపయోగించబడుతుంది. UPI యాప్ తెరిచిన తర్వాత, మీరు చెల్లించాల్సిన మొత్తాన్ని టైప్ చేయవచ్చు."
            else -> "Open your UPI app to pay ${customer.name}? By default, it will use the customer's mobile number UPI address (${cleanPhone}@ybl). Once the UPI app opens, you can type the amount to pay."
        }
        val upiConfirm = translate("Open UPI App", language)
        val upiCancel = translate("Cancel", language)

        AlertDialog(
            onDismissRequest = { showUpiAlert = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(upiTitle, fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasTwoPhones) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select mobile number for UPI:", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUpiPhone = primaryPhone }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedUpiPhone == primaryPhone),
                                    onClick = { selectedUpiPhone = primaryPhone },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                                )
                                Text("Primary: $primaryPhone", color = Color.Black, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUpiPhone = secondaryPhone }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedUpiPhone == secondaryPhone),
                                    onClick = { selectedUpiPhone = secondaryPhone },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                                )
                                Text("Secondary: $secondaryPhone", color = Color.Black, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                    Text(upiText, color = Color.Black, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = upiIdToPay,
                        onValueChange = { upiIdToPay = it },
                        label = { Text(translate("Customer UPI ID (VPA)", language)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF0284C7),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    
                    if (cleanPhone.isNotEmpty()) {
                        Text(
                            text = translate("Quick Suffix:", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val suffixes = listOf("@ybl", "@upi", "@ptyes", "@okaxis")
                            suffixes.forEach { suffix ->
                                val targetVpa = "${cleanPhone}$suffix"
                                val isSelected = upiIdToPay == targetVpa
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { upiIdToPay = targetVpa }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = suffix,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpiAlert = false
                        val cleanUpi = upiIdToPay.trim()
                        if (cleanUpi.isBlank()) {
                            Toast.makeText(context, "UPI ID cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        try {
                            // Copy to Clipboard as automatic fallback so they can paste it if any app is restrictive
                            clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(cleanUpi) })
                            Toast.makeText(context, "UPI ID copied: $cleanUpi", Toast.LENGTH_LONG).show()

                            // Build simplified P2P UPI link to avoid "payment not allowed" merchant restrictions
                            val intentUri = Uri.Builder()
                                .scheme("upi")
                                .authority("pay")
                                .appendQueryParameter("pa", cleanUpi)
                                .build()
                            
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = intentUri
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Launch UPI Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text(upiConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpiAlert = false }) {
                    Text(upiCancel, color = Color.Black)
                }
            }
        )
    }

    if (showDeleteCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerDialog = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Confirm Customer Deletion", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to delete ${customer.name}? This will permanently delete this customer's profile, active contracts, and all payment records. This action cannot be undone!", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        val custDay = customer.collectionDay
                        viewModel.deleteCustomer(customer)
                        if (custDay.isNotBlank()) {
                            viewModel.selectDay(custDay)
                        }
                        showDeleteCustomerDialog = false
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteCustomerDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorLossRed)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingCycleTarget != null) {
        val target = deletingCycleTarget!!
        AlertDialog(
            onDismissRequest = { deletingCycleTarget = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Delete Loan Contract", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to delete this loan cycle worth ₹${target.loanAmount + target.interestAmount}? This will also delete all its payment history entries.", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLoanCycle(target)
                        deletingCycleTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed)
                ) {
                    Text("Delete Contract")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingCycleTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorLossRed)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingPaymentTarget != null) {
        val targetPayment = deletingPaymentTarget!!
        AlertDialog(
            onDismissRequest = { deletingPaymentTarget = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Delete Instalment Entry", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to delete this payment record? This will reduce the total paid amount and increase outstanding dues.", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(targetPayment.id, targetPayment.loanCycleId)
                        deletingPaymentTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed)
                ) {
                    Text("Delete Entry")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingPaymentTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorLossRed)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingPayment != null) {
        val entry = editingPayment!!
        val initialCalculatedWeek = remember(entry.id, entry.weekNumber, entry.loanCycleId) {
            if (entry.weekNumber > 0) {
                entry.weekNumber
            } else {
                val cyclePayments = allPayments.filter {
                    it.loanCycleId == entry.loanCycleId &&
                    it.status.uppercase() != "DELETED" &&
                    it.id != entry.id &&
                    it.amountPaid > 0.0 &&
                    it.weekNumber > 0
                }
                (cyclePayments.maxOfOrNull { it.weekNumber } ?: 0) + 1
            }
        }
        var amtText by remember(entry.id) { 
            mutableStateOf(if (entry.amountPaid <= 0.0) "" else entry.amountPaid.toLong().toString()) 
        }
        var wkNumText by remember(entry.id) { mutableStateOf(initialCalculatedWeek.toString()) }
        var noteText by remember(entry.id) { 
            val initialNote = if (entry.notes.equals("UNPAID", ignoreCase = true) || entry.notes.isBlank()) {
                "Cash"
            } else if (entry.notes.equals("Online", ignoreCase = true)) {
                "UPI"
            } else {
                entry.notes
            }
            mutableStateOf(initialNote)
        }
        
        val sdfEdit = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
        var dateText by remember(entry.id) { mutableStateOf(sdfEdit.format(Date(entry.paymentDate))) }

        // Date & Time Picker triggers
        val parsedTimestamp = try {
            sdfEdit.parse(dateText)?.time ?: entry.paymentDate
        } catch (e: Exception) {
            entry.paymentDate
        }

        var showDatePickerState by remember { mutableStateOf(false) }

        val showTimePicker = {
            val calendar = Calendar.getInstance().apply { timeInMillis = parsedTimestamp }
            android.app.TimePickerDialog(
            context.findActivity() ?: context,
                { _, hourOfDay, minute ->
                    val newCalendar = Calendar.getInstance().apply {
                        timeInMillis = parsedTimestamp
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    dateText = sdfEdit.format(Date(newCalendar.timeInMillis))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog(
            onDismissRequest = { editingPayment = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Edit Instalment Entry", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amtText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            amtText = filtered
                        },
                        label = { Text("Amount Collected (₹)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = wkNumText,
                        onValueChange = { input ->
                            wkNumText = input.filter { it.isDigit() }
                        },
                        label = { Text("Week Number", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Change/Edit Date & Time option row
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        label = { Text("Payment Date & Time", color = Color.Gray) },
                        readOnly = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.DarkGray,
                            unfocusedLabelColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerState = true },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showDatePickerState = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = Color.Black)
                                }
                                IconButton(onClick = { showTimePicker() }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = Color.Black)
                                }
                            }
                        }
                    )

                    if (showDatePickerState) {
                        com.example.ui.components.AdvancedDatePickerDialog(
                            initialTimeMs = parsedTimestamp,
                            onDismissRequest = { showDatePickerState = false },
                            onDateSelected = { selectedTimeMs ->
                                dateText = sdfEdit.format(Date(selectedTimeMs))
                                showDatePickerState = false
                            }
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Mode of Payment",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(vertical = 4.dp)
                                .background(
                                    color = if (noteText == "Cash") Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (noteText == "Cash") Color(0xFF16A34A) else Color(0xFF2563EB),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    noteText = if (noteText == "Cash") "UPI" else "Cash"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = noteText.uppercase(Locale.getDefault()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (noteText == "Cash") Color(0xFF16A34A) else Color(0xFF2563EB)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmt = amtText.toDoubleOrNull() ?: 0.0
                        val parsedWk = wkNumText.toIntOrNull()?.takeIf { it > 0 } ?: initialCalculatedWeek
                        val parsedDt = try {
                            sdfEdit.parse(dateText)?.time ?: entry.paymentDate
                        } catch (e: Exception) {
                            entry.paymentDate
                        }
                        if (parsedAmt <= 0.0) {
                            viewModel.editWeeklyPayment(
                                paymentId = entry.id,
                                loanCycleId = entry.loanCycleId,
                                amount = 0.0,
                                weekNum = parsedWk,
                                paymentDate = parsedDt,
                                notes = "UNPAID"
                            )
                        } else {
                            val finalNote = if (noteText.equals("UNPAID", ignoreCase = true) || noteText.isBlank()) "Cash" else noteText
                            viewModel.editWeeklyPayment(
                                paymentId = entry.id,
                                loanCycleId = entry.loanCycleId,
                                amount = parsedAmt,
                                weekNum = parsedWk,
                                paymentDate = parsedDt,
                                notes = finalNote
                            )
                        }
                        editingPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGainGreen)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPayment = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val liveLoanCycles = remember(loanCycles, allPayments) {
        loanCycles.map { lc ->
            val livePaid = allPayments.filter { it.loanCycleId == lc.id && it.status.uppercase() != "DELETED" }.sumOf { it.amountPaid }
            lc.copy(paidAmount = livePaid)
        }
    }
    val activeLoans = liveLoanCycles.filter { it.status == "ACTIVE" }
    val paidHistory = remember(liveLoanCycles) {
        liveLoanCycles.filter { lc ->
            (lc.status == "PAID" || lc.status == "CLOSED") && lc.status.uppercase() != "DELETED"
        }.sortedByDescending { it.startDate }
    }

    val closedLoans = paidHistory
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingStatement by remember { mutableStateOf(false) }
    var showStatementChooserDialog by remember { mutableStateOf(false) }
    var pendingStatementRequest by remember { mutableStateOf<Pair<LoanCycle, Boolean>?>(null) }

    val executeGenerateAndShareStatement: (LoanCycle, Boolean, String) -> Unit = { targetLoan, isClosed, targetPhone ->
        coroutineScope.launch {
            try {
                isGeneratingStatement = true
                val loanPayments = allPayments.filter {
                    it.loanCycleId == targetLoan.id &&
                    it.status.uppercase() != "DELETED" &&
                    it.amountPaid > 0.0
                }.sortedWith(compareBy<WeeklyPayment> { it.paymentDate }.thenBy { it.weekNumber })

                val statementBitmap = StatementGenerator.generateCustomerStatementBitmap(
                    context = context,
                    businessName = viewModel.businessName.value,
                    customerName = customer.name,
                    collectionDay = customer.collectionDay,
                    activeLoan = targetLoan,
                    payments = loanPayments,
                    themeName = viewModel.selectedTheme.value,
                    customizationCode = viewModel.statementCustomizationCode.value,
                    customerPhone = targetPhone,
                    isClosedLoan = isClosed
                )
                shareStatementImageToWhatsapp(
                    context = context,
                    bitmap = statementBitmap,
                    customerName = customer.name,
                    phoneNumber = targetPhone,
                    isClosedLoan = isClosed
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error generating statement: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingStatement = false
            }
        }
    }

    val generateAndShareStatement: (LoanCycle, Boolean) -> Unit = { targetLoan, isClosed ->
        if (hasTwoPhones) {
            pendingStatementRequest = Pair(targetLoan, isClosed)
        } else {
            executeGenerateAndShareStatement(targetLoan, isClosed, primaryPhone)
        }
    }

    if (pendingStatementRequest != null) {
        val (targetLoan, isClosed) = pendingStatementRequest!!
        AlertDialog(
            onDismissRequest = { pendingStatementRequest = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("WhatsApp Statement Confirmation", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the number to send WhatsApp statement to:", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatementPhone = primaryPhone }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedStatementPhone == primaryPhone),
                            onClick = { selectedStatementPhone = primaryPhone },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF25D366))
                        )
                        Text("Primary: $primaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatementPhone = secondaryPhone }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedStatementPhone == secondaryPhone),
                            onClick = { selectedStatementPhone = secondaryPhone },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF25D366))
                        )
                        Text("Secondary: $secondaryPhone", color = Color.Black, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingStatementRequest = null
                        val targetNumber = selectedStatementPhone.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        executeGenerateAndShareStatement(targetLoan, isClosed, targetNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("Send WhatsApp", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatementRequest = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatementChooserDialog) {
        val targetActiveLoan = activeLoans.firstOrNull()
        AlertDialog(
            onDismissRequest = { showStatementChooserDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ListAlt,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Select Loan Statement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorSlateDark
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose which loan statement to generate and share for ${customer.name}:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    // 1. Active Loan option
                    if (targetActiveLoan != null) {
                        val activeTotal = targetActiveLoan.loanAmount + targetActiveLoan.interestAmount
                        val activePaid = targetActiveLoan.paidAmount
                        val activeBal = maxOf(0.0, activeTotal - activePaid)
                        Surface(
                            onClick = {
                                showStatementChooserDialog = false
                                generateAndShareStatement(targetActiveLoan, false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = ColorGainGreenLight.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, ColorGainGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("statement_option_active")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Active Loan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ColorSlateDark
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(ColorGainGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = ColorGainGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Loan: ₹${CurrencyFormatter.format(targetActiveLoan.loanAmount)} • Balance: ₹${CurrencyFormatter.format(activeBal)}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    // 2. Closed Loan options
                    closedLoans.forEachIndexed { index, closedCycle ->
                        val closedLabel = if (closedLoans.size > 1) "Closed Loan ${index + 1}" else "Closed Loan"
                        val disbursalDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(closedCycle.startDate))
                        Surface(
                            onClick = {
                                showStatementChooserDialog = false
                                generateAndShareStatement(closedCycle, true)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.5.dp, Color(0xFF94A3B8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("statement_option_closed_${closedCycle.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = closedLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ColorSlateDark
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(ColorLossRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(1.dp, ColorLossRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "STATUS: CLOSED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ColorLossRed
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = ColorSlateDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Loan: ₹${CurrencyFormatter.format(closedCycle.loanAmount)} ($disbursalDateStr) • Settled",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showStatementChooserDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val customerLoanCycleIds = remember(loanCycles) { loanCycles.map { it.id }.toSet() }
    val customerPayments = allPayments.filter { it.loanCycleId in customerLoanCycleIds && it.status == "ACTIVE" }

    val overviewList by viewModel.customerOverviewList.collectAsStateWithLifecycle()
    val currentIndex = overviewList.indexOfFirst { it.customer.id == customerId }
    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex != -1 && currentIndex < overviewList.size - 1

    val previousCustomerId = if (hasPrevious) overviewList[currentIndex - 1].customer.id else null
    val nextCustomerId = if (hasNext) overviewList[currentIndex + 1].customer.id else null

    val prevCustomerName = if (hasPrevious) overviewList[currentIndex - 1].customer.name else null
    val nextCustomerName = if (hasNext) overviewList[currentIndex + 1].customer.name else null

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.matchParentSize().background(appColors.primaryAccent.copy(alpha = 0.03f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
        // Visual indicator showing previous/next customer for convenient switching
        if (prevCustomerName != null || nextCustomerName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prevCustomerName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(ColorSlateDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.replaceTopScreen(Screen.CustomerDetail(previousCustomerId!!)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Customer", tint = ColorSlateDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prevCustomerName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorSlateDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = "Navigator",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                if (nextCustomerName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(ColorSlateDark.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.replaceTopScreen(Screen.CustomerDetail(nextCustomerId!!)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = nextCustomerName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorSlateDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next Customer", tint = ColorSlateDark, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }

        // REDESIGNED: Home Dashboard Style Customer outstanding ledger card
        val allCustomerLoans = remember(loanCycles) { loanCycles.sortedBy { it.id } }
        val activeLoansToRender = if (activeLoans.isEmpty()) {
            listOf(
                LoanCycle(
                    id = -1,
                    customerId = customer.id,
                    loanAmount = 0.0,
                    interestAmount = 0.0,
                    weeklyAmount = 0.0,
                    totalWeeks = 10,
                    startDate = 0L,
                    status = "NONE",
                    notes = "",
                    paidAmount = 0.0,
                    uuid = "",
                    lastModified = 0L
                )
            )
        } else {
            activeLoans
        }

        activeLoansToRender.forEach { activeLoan ->
            val lastPayment = remember(customerPayments, activeLoan.id) {
                customerPayments
                    .filter { it.loanCycleId == activeLoan.id && it.status != "DELETED" }
                    .maxByOrNull { it.paymentDate }
            }

            val lastPaymentInfo = remember(lastPayment) {
                lastPayment?.let { lp ->
                    val sdfDateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val payDate = Date(lp.paymentDate)
                    val payDateStr = sdfDateOnly.format(payDate)
                    
                    // Format time
                    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val payTimeStr = sdfTime.format(payDate)

                    val nowCal = Calendar.getInstance()
                    val payCal = Calendar.getInstance().apply { timeInMillis = lp.paymentDate }
                    
                    val relativeDay = if (nowCal.get(Calendar.YEAR) == payCal.get(Calendar.YEAR) &&
                        nowCal.get(Calendar.DAY_OF_YEAR) == payCal.get(Calendar.DAY_OF_YEAR)) {
                        "Today"
                    } else {
                        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
                        if (yesterdayCal.get(Calendar.YEAR) == payCal.get(Calendar.YEAR) &&
                            yesterdayCal.get(Calendar.DAY_OF_YEAR) == payCal.get(Calendar.DAY_OF_YEAR)) {
                            "Yesterday"
                        } else {
                            payDateStr
                        }
                    }
                    
                    "₹${lp.amountPaid.toLong()} on $relativeDay at $payTimeStr (Week ${lp.weekNumber})"
                }
            }

            val totalToBePaid = activeLoan.loanAmount + activeLoan.interestAmount
            val remaining = maxOf(0.0, totalToBePaid - activeLoan.paidAmount)
            val progress = if (totalToBePaid > 0) (activeLoan.paidAmount / totalToBePaid).toFloat() else 0f

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = appColors.headerCardBg
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, appColors.headerCardBorder, RoundedCornerShape(16.dp))
            ) {
                // Gradient box contents with spacious 2x2 metric layout
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = customer.name.uppercase(Locale.ROOT),
                                fontSize = if (customer.name.length > 20) 16.sp else 20.sp,
                                lineHeight = if (customer.name.length > 20) 20.sp else 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = appColors.textOnHeader,
                                letterSpacing = 1.0.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (primaryPhone.isNotBlank() || secondaryPhone.isNotBlank()) {
                                Text(
                                    text = if (hasTwoPhones) "Ph: $primaryPhone, $secondaryPhone" else "Ph: ${primaryPhone.ifBlank { secondaryPhone }}",
                                    fontSize = 11.sp,
                                    color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (activeLoan.id != -1 && activeLoans.size > 1) {
                            val loanIndex = allCustomerLoans.indexOfFirst { it.id == activeLoan.id }
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Loan ${loanIndex + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = appColors.textOnHeader,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dividing Lime green animated progress bar
                    val limeColor = Color(0xFF84CC16)
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = limeColor,
                        trackColor = if (appColors.isDark) Color.White.copy(alpha = 0.2f) else appColors.textOnHeader.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Amt Receivable & Collected Amount in distinct spacious boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = translate("Amt Receivable", language),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedNumberText(
                                targetValue = remaining,
                                prefix = "₹",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = appColors.textOnHeader,
                                delayMillis = 0,
                                durationMillis = 1600
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = translate("Collected Amount", language),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedNumberText(
                                targetValue = activeLoan.paidAmount,
                                prefix = "₹",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80),
                                delayMillis = 150,
                                durationMillis = 1600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Borrowed Principal & Weekly Interest in distinct spacious boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = translate("Borrowed Principal", language),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedNumberText(
                                targetValue = activeLoan.loanAmount,
                                prefix = "₹",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textOnHeader,
                                delayMillis = 300,
                                durationMillis = 1600
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = translate("Weekly Interest", language),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedNumberText(
                                targetValue = activeLoan.interestAmount,
                                prefix = "₹",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textOnHeader,
                                delayMillis = 450,
                                durationMillis = 1600
                            )
                        }
                    }
                }
            }

            // Dispersal date indicator below the dashboard card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${translate("Date of Dispersal", language)}:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
                val sdfDispersal = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val dispersalDateStr = if (activeLoan.startDate > 0L) {
                    sdfDispersal.format(Date(activeLoan.startDate))
                } else {
                    "N/A"
                }
                Text(
                    text = dispersalDateStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }



            // MINI UTILITY BOX: Centering shortcuts same color, other shade
            val hasPhone = primaryPhone.filter { it.isDigit() }.length >= 10 || secondaryPhone.filter { it.isDigit() }.length >= 10
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = appColors.headerCardBg.map { it.copy(alpha = 0.85f) }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        appColors.headerCardBorder.copy(alpha = 0.75f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasPhone) {
                        // 1. Call Logo
                        IconButton(onClick = { showCallAlert = true }, modifier = Modifier.testTag("call_customer_button")) {
                            Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = Color(0xFF4ADE80), modifier = Modifier.size(22.dp))
                        }
                        
                        // 2. SMS Logo
                        IconButton(onClick = { showSmsAlert = true }, modifier = Modifier.testTag("sms_remind_button")) {
                            Icon(Icons.Default.Sms, contentDescription = "SMS Remind", tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                        }

                        // 3. WhatsApp Logo
                        IconButton(
                            onClick = { showWhatsappAlert = true },
                            modifier = Modifier.testTag("whatsapp_remind_button")
                        ) {
                            Icon(WhatsAppIcon, contentDescription = "WhatsApp Remind", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                        }

                        // 4. UPI Pay Logo
                        IconButton(onClick = { showUpiAlert = true }) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF0284C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "UPI",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // 4. File Statement Icon
                    val defaultActiveLoanForStatement = activeLoans.firstOrNull() ?: LoanCycle(
                        id = -1,
                        customerId = customer.id,
                        loanAmount = 0.0,
                        interestAmount = 0.0,
                        weeklyAmount = 0.0,
                        totalWeeks = 10,
                        startDate = 0L,
                        status = "NONE",
                        notes = "",
                        paidAmount = 0.0,
                        uuid = "",
                        lastModified = 0L
                    )
                    IconButton(onClick = {
                        if (closedLoans.isNotEmpty()) {
                            showStatementChooserDialog = true
                        } else {
                            val targetLoan = activeLoans.firstOrNull() ?: defaultActiveLoanForStatement
                            generateAndShareStatement(targetLoan, false)
                        }
                    }) {
                        Icon(Icons.Filled.ListAlt, contentDescription = "Share Customer Statement", tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                    }
                }
            }

            if ((customer.smsWeeklyReminder || customer.smsConfirmationOfEntry) && customer.phone.isNotBlank()) {
                val infiniteTransition = rememberInfiniteTransition(label = "slide_transition")
                val slideOffsetFloat = infiniteTransition.animateFloat(
                    initialValue = -8f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "slide"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = slideOffsetFloat.value.dp.toPx() }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val labelText = if (customer.smsWeeklyReminder && customer.smsConfirmationOfEntry) {
                        "🔔 Receives both Automated Reminders & Entry Notifications"
                    } else if (customer.smsWeeklyReminder) {
                        "✉️ Receives Weekly Auto SMS Reminders"
                    } else {
                        "💬 Receives Confirmation Entry SMS"
                    }
                    Text(
                        text = labelText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorAccentBlue
                    )
                }
            }
        if (activeLoans.isEmpty()) {
            // No active loan cycle state
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "No Active Cycle for ${customer.name}",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUserRole != "USER") Spacer(modifier = Modifier.width(8.dp))
                    if (currentUserRole != "USER") Button(
                        onClick = { viewModel.navigateTo(Screen.AddLoan(customer.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start First Loan Cycle", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Horizontal visual divider calendar grid
        PaymentCalendarMatrix(payments = customerPayments, loanCycles = loanCycles)

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        var selectedActiveLoanFilterId by rememberSaveable(customer.id) { mutableStateOf<Int?>(null) }
        var archivedCloudHistory by remember(customer.uuid) {
            mutableStateOf<List<com.example.network.ArchivedPastLoanItem>>(emptyList())
        }
        var isLoadingArchivedHistory by remember(customer.uuid) { mutableStateOf(false) }
        val customAppLogo by viewModel.customAppLogo.collectAsStateWithLifecycle()

        LaunchedEffect(activeTab, customer.uuid) {
            if (activeTab == 1 && customer.uuid.isNotBlank()) {
                isLoadingArchivedHistory = true
                archivedCloudHistory = com.example.network.CloudLedgerSyncEngine.fetchArchivedPastHistoryForCustomer(customer.uuid)
                isLoadingArchivedHistory = false
            }
        }

        if (activeTab == 0) {
            if (activeLoans.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeLoans.size > 1) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val allSelected = selectedActiveLoanFilterId == null
                                Surface(
                                    onClick = { selectedActiveLoanFilterId = null },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (allSelected) appColors.primaryAccent else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (allSelected) appColors.primaryAccent else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "All (${activeLoans.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allSelected) Color.White else ColorSlateDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                val allCustomerLoansSorted = remember(loanCycles) { loanCycles.sortedBy { it.id } }
                                activeLoans.forEach { loanItem ->
                                    val idx = allCustomerLoansSorted.indexOfFirst { it.id == loanItem.id }
                                    val isSel = selectedActiveLoanFilterId == loanItem.id
                                    Surface(
                                        onClick = { selectedActiveLoanFilterId = if (isSel) null else loanItem.id },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSel) appColors.primaryAccent else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isSel) appColors.primaryAccent else Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            text = "Loan ${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else ColorSlateDark,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (currentUserRole != "USER") {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.AddLoan(customer.id)) },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Additional Loan", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    val displayedActiveLoans = remember(activeLoans, selectedActiveLoanFilterId) {
                        if (selectedActiveLoanFilterId == null) activeLoans
                        else activeLoans.filter { it.id == selectedActiveLoanFilterId }.ifEmpty { activeLoans }
                    }

                    displayedActiveLoans.forEach { targetActiveLoan ->
                        val activePayments = customerPayments.filter { it.loanCycleId == targetActiveLoan.id }
                        val allCustomerLoansSorted = remember(loanCycles) { loanCycles.sortedBy { it.id } }
                        val loanIndex = allCustomerLoansSorted.indexOfFirst { it.id == targetActiveLoan.id }
                        val loanLabel = if (activeLoans.size <= 1) "Active Loan" else "Loan ${loanIndex + 1}"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = loanLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = appColors.primaryAccent,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                ActiveLoanSection(
                                    activeLoan = targetActiveLoan,
                                    payments = activePayments,
                                    onAddPaymentClicked = { viewModel.navigateTo(Screen.RecordPayment(targetActiveLoan.id)) },
                                    onMarkSettledClicked = { viewModel.markLoanCycleSettled(targetActiveLoan.id) },
                                    onEditPayment = { payment -> editingPayment = payment },
                                    onDeletePayment = { payment -> deletingPaymentTarget = payment },
                                    onDeleteCycle = { deletingCycleTarget = targetActiveLoan },
                                    onEditCycleClicked = { viewModel.navigateTo(Screen.EditLoan(targetActiveLoan.id)) },
                                    currentUserRole = currentUserRole,
                                    onShareClicked = {
                                        generateAndShareStatement(targetActiveLoan, false)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Active Loan Cycles", color = Color.Gray)
                }
            }
        } else {
            // Past Loans history page (including <6M Firestore settled loans and >6M Firebase Storage .md archives)
            if (isLoadingArchivedHistory && paidHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppLogoPulseLoader(
                        customAppLogo = customAppLogo,
                        title = "",
                        message = "Fetching archived Past History from Cloud Storage..."
                    )
                }
            } else if (paidHistory.isEmpty() && archivedCloudHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No past completed accounts.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    paidHistory.forEach { historicCycle ->
                        val historicPayments = customerPayments.filter { it.loanCycleId == historicCycle.id }
                        val allCustomerLoansSorted = remember(loanCycles) { loanCycles.sortedBy { it.id } }
                        val loanIndex = allCustomerLoansSorted.indexOfFirst { it.id == historicCycle.id }
                        val loanLabel = if (activeLoans.size <= 1 && (loanIndex + 1) == 1) "Past Loan" else "Loan ${loanIndex + 1}"
                        HistoricLoanCard(
                            historicCycle = historicCycle,
                            payments = historicPayments,
                            onEditLoan = { viewModel.navigateTo(Screen.EditLoan(historicCycle.id)) },
                            onDeleteLoan = { deletingCycleTarget = historicCycle },
                            onEditPayment = { payment -> editingPayment = payment },
                            onDeletePayment = { payment -> deletingPaymentTarget = payment },
                            onAddPayment = { viewModel.navigateTo(Screen.RecordPayment(historicCycle.id)) },
                            onShareClicked = { generateAndShareStatement(historicCycle, true) },
                            currentUserRole = currentUserRole,
                            loanLabel = loanLabel
                        )
                    }

                    // Render >6M Firebase Storage Markdown Archives that aren't already in local paidHistory
                    val localPaidUuids = remember(paidHistory) { paidHistory.map { it.uuid }.toSet() }
                    val extraCloudArchives = remember(archivedCloudHistory, localPaidUuids) {
                        archivedCloudHistory.filter { it.loanUuid !in localPaidUuids }
                    }
                    extraCloudArchives.forEach { archivedItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Archived Cloud Loan (>6 Months)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Cloud .MD Archive",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = archivedItem.markdownContent,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ColorSlateDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vertical stacked toggle rows for Active Loan and Past History counts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Line 1: Active Loan count
            Surface(
                onClick = { activeTab = 0 },
                shape = RoundedCornerShape(12.dp),
                color = if (activeTab == 0) appColors.primaryAccent.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    width = if (activeTab == 0) 1.5.dp else 1.dp,
                    color = if (activeTab == 0) appColors.primaryAccent else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accounts_active_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (activeTab == 0) appColors.primaryAccent else ColorSlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Active Loan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (activeTab == 0) appColors.primaryAccent else ColorSlateDark
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeTab == 0) appColors.primaryAccent else Color(0xFF94A3B8)
                    ) {
                        Text(
                            text = "${activeLoans.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Line 2: Past History count (one line below active loan)
            Surface(
                onClick = { activeTab = 1 },
                shape = RoundedCornerShape(12.dp),
                color = if (activeTab == 1) appColors.primaryAccent.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    width = if (activeTab == 1) 1.5.dp else 1.dp,
                    color = if (activeTab == 1) appColors.primaryAccent else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accounts_past_history_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (activeTab == 1) appColors.primaryAccent else ColorSlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Past History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (activeTab == 1) appColors.primaryAccent else ColorSlateDark
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeTab == 1) appColors.primaryAccent else Color(0xFF94A3B8)
                    ) {
                        Text(
                            text = "${paidHistory.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

        // Smooth bottom-edge slide-up / slide-down Scroll-to-Top button
        androidx.compose.animation.AnimatedVisibility(
            visible = customerPayments.size > 8 && scrollState.value > 300,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight * 2 },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = tween(220)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight * 2 },
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(appColors.primaryAccent, CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isGeneratingStatement) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {}, // Prevent interaction
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(color = appColors.primaryAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating Statement...",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveLoanSection(
    activeLoan: LoanCycle,
    payments: List<WeeklyPayment>,
    onAddPaymentClicked: () -> Unit,
    onMarkSettledClicked: () -> Unit,
    onEditPayment: (WeeklyPayment) -> Unit,
    onDeletePayment: (WeeklyPayment) -> Unit,
    onDeleteCycle: () -> Unit,
    onEditCycleClicked: () -> Unit,
    onShareClicked: () -> Unit,
    currentUserRole: String = "ADMIN"
) {
    val totalToBePaid = activeLoan.loanAmount + activeLoan.interestAmount
    val remaining = totalToBePaid - activeLoan.paidAmount
    val appColors = LocalAppThemeColors.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (currentUserRole != "USER") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddPaymentClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primaryAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.0f)
                ) {
                    Text("+ Collections")
                }

                if (remaining <= 0) {
                    Button(
                        onClick = onMarkSettledClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.secondaryAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Settle Account")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        var showMoreDetails by rememberSaveable(activeLoan.id) { mutableStateOf(false) }
        val timelineItems = remember(payments, activeLoan.startDate) {
            buildTimelineEntriesWithUnpaidWeeks(activeLoan.startDate, payments)
        }
        val unpaidCount = remember(timelineItems) {
            timelineItems.count { it is LoanTimelineEntry.UnpaidBubble }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction History",
                fontWeight = FontWeight.Bold,
                color = ColorSlateDark,
                fontSize = 14.sp
            )

            Surface(
                onClick = { showMoreDetails = !showMoreDetails },
                shape = RoundedCornerShape(20.dp),
                color = if (showMoreDetails) Color(0xFFFFF7ED) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (showMoreDetails) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.testTag("toggle_more_details_button_${activeLoan.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (showMoreDetails) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = if (showMoreDetails) Color(0xFFEA580C) else appColors.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (showMoreDetails) "Show Less Details" else if (unpaidCount > 0) "More Details ($unpaidCount)" else "More Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showMoreDetails) Color(0xFFEA580C) else appColors.primaryAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Receipt History", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))

        val hasPaidEntries = remember(timelineItems) {
            timelineItems.any { it is LoanTimelineEntry.PaidInstallment }
        }
        if (!hasPaidEntries && (!showMoreDetails || unpaidCount == 0)) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unpaidCount > 0) "No paid installments yet. Tap 'More Details' to view $unpaidCount unpaid week(s)." else "No payments recorded yet.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timelineItems.forEach { item ->
                    when (item) {
                        is LoanTimelineEntry.PaidInstallment -> {
                            PaymentHistoryRow(
                                payment = item.payment.copy(weekNumber = item.sequentialPaidWeekNumber),
                                onEdit = { onEditPayment(item.payment) },
                                onDelete = { onDeletePayment(item.payment) },
                                currentUserRole = currentUserRole
                            )
                        }
                        is LoanTimelineEntry.UnpaidBubble -> {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showMoreDetails,
                                enter = androidx.compose.animation.expandVertically(
                                    expandFrom = Alignment.CenterVertically,
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + androidx.compose.animation.scaleIn(
                                    initialScale = 0.55f,
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeIn(animationSpec = tween(220)),
                                exit = androidx.compose.animation.shrinkVertically(
                                    shrinkTowards = Alignment.CenterVertically,
                                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                ) + androidx.compose.animation.scaleOut(
                                    targetScale = 0.45f,
                                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(animationSpec = tween(180))
                            ) {
                                UnpaidWeekBubbleRow(
                                    unpaidDateMillis = item.dateMillis,
                                    betweenLabel = item.betweenLabel,
                                    explicitPayment = item.explicitUnpaidPayment,
                                    onDeleteExplicit = item.explicitUnpaidPayment?.let { p -> { onDeletePayment(p) } },
                                    currentUserRole = currentUserRole
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class LoanTimelineEntry {
    data class PaidInstallment(
        val payment: WeeklyPayment,
        val sequentialPaidWeekNumber: Int
    ) : LoanTimelineEntry()

    data class UnpaidBubble(
        val key: String,
        val dateMillis: Long,
        val betweenLabel: String,
        val explicitUnpaidPayment: WeeklyPayment? = null
    ) : LoanTimelineEntry()
}

fun buildTimelineEntriesWithUnpaidWeeks(
    loanStartDate: Long,
    payments: List<WeeklyPayment>
): List<LoanTimelineEntry> {
    val activeRecords = payments
        .filter { it.status != "DELETED" }
        .sortedWith(compareBy<WeeklyPayment> { it.paymentDate }.thenBy { it.id })

    val paidRecords = activeRecords.filter {
        it.amountPaid > 0.0 && !it.notes.equals("UNPAID", ignoreCase = true)
    }
    val explicitUnpaidRecords = activeRecords.filter {
        it.amountPaid <= 0.0 || it.notes.equals("UNPAID", ignoreCase = true)
    }.toMutableList()

    val result = mutableListOf<LoanTimelineEntry>()
    val oneWeekMs = 7L * 24 * 60 * 60 * 1000L
    val gapThresholdMs = 11L * 24 * 60 * 60 * 1000L // >11 days between payments indicates a missed weekly installment

    var prevDate = if (loanStartDate > 0L) loanStartDate else (paidRecords.firstOrNull()?.paymentDate ?: 0L)
    var prevWeekNum = 0

    paidRecords.forEachIndexed { index, paid ->
        val seqWeek = index + 1
        val currentPayDate = paid.paymentDate

        // 1. Insert any explicit unpaid records that occurred on or before this paid installment
        val matchingExplicit = explicitUnpaidRecords.filter { it.paymentDate <= currentPayDate }
        explicitUnpaidRecords.removeAll(matchingExplicit.toSet())

        val betweenText = if (prevWeekNum == 0) {
            "Before Week 1"
        } else {
            "Between Week $prevWeekNum & Week $seqWeek"
        }

        if (matchingExplicit.isNotEmpty()) {
            matchingExplicit.forEach { unpaidRec ->
                result.add(
                    LoanTimelineEntry.UnpaidBubble(
                        key = "explicit_${unpaidRec.id}_${unpaidRec.paymentDate}",
                        dateMillis = unpaidRec.paymentDate,
                        betweenLabel = betweenText,
                        explicitUnpaidPayment = unpaidRec
                    )
                )
            }
        } else if (prevDate > 0L && (currentPayDate - prevDate) >= gapThresholdMs) {
            // Synthesize missed weekly bubble(s) if no payment was made in that week window
            val missedWeeks = (((currentPayDate - prevDate) / oneWeekMs).toInt() - 1).coerceIn(1, 8)
            for (m in 1..missedWeeks) {
                val missedDate = prevDate + (m * oneWeekMs)
                result.add(
                    LoanTimelineEntry.UnpaidBubble(
                        key = "gap_${prevWeekNum}_${seqWeek}_$m",
                        dateMillis = missedDate,
                        betweenLabel = betweenText,
                        explicitUnpaidPayment = null
                    )
                )
            }
        }

        // 2. Add the paid installment row with strictly sequential paid week number
        result.add(
            LoanTimelineEntry.PaidInstallment(
                payment = paid,
                sequentialPaidWeekNumber = seqWeek
            )
        )

        prevDate = currentPayDate
        prevWeekNum = seqWeek
    }

    // 3. Append any remaining explicit unpaid records recorded after the last paid week
    val afterLabel = if (prevWeekNum == 0) "Unpaid Week (Week Nil)" else "After Week $prevWeekNum"
    explicitUnpaidRecords.forEach { unpaidRec ->
        result.add(
            LoanTimelineEntry.UnpaidBubble(
                key = "explicit_tail_${unpaidRec.id}_${unpaidRec.paymentDate}",
                dateMillis = unpaidRec.paymentDate,
                betweenLabel = afterLabel,
                explicitUnpaidPayment = unpaidRec
            )
        )
    }

    return result
}

@Composable
fun UnpaidWeekBubbleRow(
    unpaidDateMillis: Long,
    betweenLabel: String,
    explicitPayment: WeeklyPayment?,
    onDeleteExplicit: (() -> Unit)?,
    currentUserRole: String = "ADMIN"
) {
    val orangeText = Color(0xFFEA580C)
    val orangeBorder = Color(0xFFFB923C)
    val orangeBg = Color(0xFFFFF7ED)
    val dateStr = remember(unpaidDateMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(unpaidDateMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = orangeBg,
            border = BorderStroke(1.dp, orangeBorder),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(orangeText.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = dateStr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = orangeText
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paid: ₹0 — Not paid in that week",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = orangeText
                        )
                        Text(
                            text = "$betweenLabel • Week Nil",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = orangeText.copy(alpha = 0.8f)
                        )
                    }
                }

                if (explicitPayment != null && onDeleteExplicit != null && currentUserRole != "USER") {
                    IconButton(
                        onClick = onDeleteExplicit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove unpaid mark",
                            tint = orangeText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryRow(
    payment: WeeklyPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    currentUserRole: String = "ADMIN"
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left continuous vertical accent line representing credit success
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(ColorGainGreen)
            )

            Row(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge for Week Title
                        Box(
                            modifier = Modifier
                                .background(ColorGainGreenLight, RoundedCornerShape(6.dp))
                                .border(1.dp, ColorGainGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Week ${payment.weekNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorGainGreen,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Amount Received display with visual green indicator
                        Text(
                            text = "₹${CurrencyFormatter.format(payment.amountPaid)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorGainGreen
                        )
                        
                        Text(
                            text = "Paid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date and Time displayed in distinct visual chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Date Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(ColorSlateDark.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .border(1.dp, ColorSlateDark.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = ColorSlateDark.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(payment.paymentDate)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorSlateDark
                            )
                        }

                        // Time Chip (Clock icon before time)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(ColorAccentBlue.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .border(1.dp, ColorAccentBlue.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Payment time",
                                tint = ColorAccentBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(payment.paymentDate)).uppercase(Locale.getDefault()),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorAccentBlue
                            )
                        }

                        // Notes/Method Label if existing
                        if (payment.notes.isNotEmpty()) {
                            val notesColor = if (payment.notes.uppercase(Locale.getDefault()) == "CASH") Color(0xFF15803D) else Color(0xFF1D4ED8)
                            val notesBgColor = if (payment.notes.uppercase(Locale.getDefault()) == "CASH") Color(0xFFDCFCE7) else Color(0xFFDBEAFE)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(notesBgColor, RoundedCornerShape(6.dp))
                                    .border(1.dp, notesColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = payment.notes,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = notesColor
                                )
                            }
                        }
                    }
                }

                // Options action button
                if (currentUserRole != "USER") {
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Payment options",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Entry") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    expandedMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Entry", color = ColorLossRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ColorLossRed, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricLoanCard(
    historicCycle: LoanCycle,
    payments: List<WeeklyPayment>,
    onEditLoan: () -> Unit,
    onDeleteLoan: () -> Unit,
    onEditPayment: (WeeklyPayment) -> Unit,
    onDeletePayment: (WeeklyPayment) -> Unit,
    onAddPayment: () -> Unit,
    onShareClicked: (() -> Unit)? = null,
    currentUserRole: String = "ADMIN",
    loanLabel: String = "Loan"
) {
    val totalPaid = historicCycle.paidAmount
    val totalExpected = historicCycle.loanAmount + historicCycle.interestAmount
    
    val lastPayment = payments.maxByOrNull { it.paymentDate }
    val lastPaymentDate = lastPayment?.paymentDate ?: historicCycle.lastModified
    val completionTime = if (lastPaymentDate > 0) lastPaymentDate else historicCycle.lastModified
    val diffMs = completionTime - historicCycle.startDate
    val diffDays = (diffMs / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
    val actualWeeks = diffDays / 7.0
    val durationLabel = if (diffDays == 0) {
        "Paid in Same Day"
    } else {
        "$diffDays Days (~${String.format(Locale.getDefault(), "%.1f", actualWeeks)} Weeks)"
    }
    
    val flatRate = if (historicCycle.loanAmount > 0) (historicCycle.interestAmount / historicCycle.loanAmount) * 100.0 else 0.0
    val effectiveDays = maxOf(1, diffDays)
    val annualizedRate = flatRate * (365.0 / effectiveDays)

    // 6-Month Editable Retention & Post-6-Month Plain-Text Read-Only Archive
    val now = System.currentTimeMillis()
    val ageMs = (now - completionTime).coerceAtLeast(0L)
    val sixMonthsMs = 180L * 24 * 60 * 60 * 1000L
    val isArchivedReadOnly = ageMs >= sixMonthsMs || historicCycle.notes.startsWith("COMPACT_6M|")
    val remainingDays = ((sixMonthsMs - ageMs) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L).toInt()
    val retentionBadgeLabel = if (isArchivedReadOnly) {
        "Archived (>6M • Read-Only)"
    } else {
        "Editable (${remainingDays}d left)"
    }
    val effectiveRole = if (isArchivedReadOnly) "USER" else currentUserRole

    // Parse compact plain-text archive format if stored as COMPACT_6M|P=...|I=...|D=...|W=...|TW=...|PAID=...|SD=...|CD=...|REC=...
    val compactSummaryText = remember(historicCycle.notes, historicCycle, payments) {
        val sdfShort = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        if (historicCycle.notes.startsWith("COMPACT_6M|")) {
            val map = historicCycle.notes.removePrefix("COMPACT_6M|")
                .split("|")
                .mapNotNull { token ->
                    val idx = token.indexOf('=')
                    if (idx > 0) token.substring(0, idx) to token.substring(idx + 1) else null
                }.toMap()
            val recRaw = map["REC"].orEmpty()
            val recFormatted = if (recRaw.isBlank()) "None" else recRaw.split(";").joinToString("  •  ")
            "P: ₹${map["P"] ?: historicCycle.loanAmount.toLong()} | Int: ₹${map["I"] ?: historicCycle.interestAmount.toLong()} | Ded: ₹${map["D"] ?: historicCycle.deduction.toLong()} | Wk: ₹${map["W"] ?: historicCycle.weeklyAmount.toLong()} (${map["TW"] ?: historicCycle.totalWeeks}w) | Paid: ₹${map["PAID"] ?: totalPaid.toLong()}\nReceipts: $recFormatted"
        } else {
            val recFormatted = if (payments.isEmpty()) "None" else payments.sortedBy { it.weekNumber }.joinToString("  •  ") { p ->
                "W${p.weekNumber}:₹${p.amountPaid.toLong()}(${sdfShort.format(Date(p.paymentDate))})"
            }
            "P: ₹${historicCycle.loanAmount.toLong()} | Int: ₹${historicCycle.interestAmount.toLong()} | Ded: ₹${historicCycle.deduction.toLong()} | Wk: ₹${historicCycle.weeklyAmount.toLong()} (${historicCycle.totalWeeks}w) | Paid: ₹${totalPaid.toLong()}\nReceipts: $recFormatted"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Header & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Settled $loanLabel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorGainGreen
                        )
                        Box(
                            modifier = Modifier
                                .background(ColorGainGreenLight, RoundedCornerShape(6.dp))
                                .border(1.dp, ColorGainGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PAID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorGainGreen
                            )
                        }
                    }
                    Text(
                        text = "Disbursed: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(historicCycle.startDate))} • $durationLabel",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                // Top Right: 6-Month Retention Status badge, Share Statement & Delete button
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isArchivedReadOnly) Color(0xFFF1F5F9) else Color(0xFFEFF6FF),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isArchivedReadOnly) Color(0xFFCBD5E1) else Color(0xFFBFDBFE),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = retentionBadgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isArchivedReadOnly) Color(0xFF475569) else Color(0xFF1D4ED8)
                        )
                    }
                    if (onShareClicked != null) {
                        IconButton(onClick = onShareClicked, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ListAlt, contentDescription = "Share Closed Statement", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                        }
                    }
                    if (effectiveRole != "USER") {
                        IconButton(onClick = onDeleteLoan, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete past loan", tint = ColorLossRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full Loan Details Grid (Disbursed, Principal, Interest, Deduction, Weekly Instalment, Tenure)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Disbursed Amount", fontSize = 10.sp, color = Color.Gray)
                    Text("₹${CurrencyFormatter.format(historicCycle.loanAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorSlateDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Interest Charged", fontSize = 10.sp, color = Color.Gray)
                    Text("₹${CurrencyFormatter.format(historicCycle.interestAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Paid Back", fontSize = 10.sp, color = Color.Gray)
                    Text("₹${CurrencyFormatter.format(totalPaid)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorGainGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Weekly Instalment", fontSize = 10.sp, color = Color.Gray)
                    Text("₹${CurrencyFormatter.format(historicCycle.weeklyAmount)} / wk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ColorSlateDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tenure Weeks", fontSize = 10.sp, color = Color.Gray)
                    Text("${historicCycle.totalWeeks} Weeks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ColorSlateDark)
                }
                if (historicCycle.deduction > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Deduction", fontSize = 10.sp, color = Color.Gray)
                        Text("₹${CurrencyFormatter.format(historicCycle.deduction)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7C3AED))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Outstanding", fontSize = 10.sp, color = Color.Gray)
                        Text("₹0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorGainGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Plain-Text Archive Summary Block (saves storage and presents simply)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = compactSummaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ColorSlateDark,
                    lineHeight = 16.sp
                )
            }

            if (!isArchivedReadOnly) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                Spacer(modifier = Modifier.height(8.dp))

                // Action Row: Edit Loan Details & Add Payment buttons (active during 6-month window)
                if (effectiveRole != "USER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditLoan,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Loan Details", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onAddPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Receipt", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Receipts / Payment Transactions Breakdown for this Past Loan (editable within 6 months)
                var showMoreHistoricDetails by rememberSaveable(historicCycle.id) { mutableStateOf(false) }
                val historicTimeline = remember(payments, historicCycle.startDate) {
                    buildTimelineEntriesWithUnpaidWeeks(historicCycle.startDate, payments)
                }
                val historicPaidCount = remember(historicTimeline) {
                    historicTimeline.count { it is LoanTimelineEntry.PaidInstallment }
                }
                val historicUnpaidCount = remember(historicTimeline) {
                    historicTimeline.count { it is LoanTimelineEntry.UnpaidBubble }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipts Breakdown ($historicPaidCount)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorSlateDark
                    )
                    if (historicUnpaidCount > 0) {
                        Surface(
                            onClick = { showMoreHistoricDetails = !showMoreHistoricDetails },
                            shape = RoundedCornerShape(16.dp),
                            color = if (showMoreHistoricDetails) Color(0xFFFFF7ED) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (showMoreHistoricDetails) Color(0xFFEA580C) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (showMoreHistoricDetails) "Show Less Details" else "More Details ($historicUnpaidCount)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showMoreHistoricDetails) Color(0xFFEA580C) else ColorAccentBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (historicPaidCount == 0 && (!showMoreHistoricDetails || historicUnpaidCount == 0)) {
                    Text("No payment receipts recorded.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        historicTimeline.forEach { item ->
                            when (item) {
                                is LoanTimelineEntry.PaidInstallment -> {
                                    PaymentHistoryRow(
                                        payment = item.payment.copy(weekNumber = item.sequentialPaidWeekNumber),
                                        onEdit = { onEditPayment(item.payment) },
                                        onDelete = { onDeletePayment(item.payment) },
                                        currentUserRole = effectiveRole
                                    )
                                }
                                is LoanTimelineEntry.UnpaidBubble -> {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showMoreHistoricDetails,
                                        enter = androidx.compose.animation.expandVertically(
                                            expandFrom = Alignment.CenterVertically,
                                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                        ) + androidx.compose.animation.scaleIn(
                                            initialScale = 0.55f,
                                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                        ) + androidx.compose.animation.fadeIn(animationSpec = tween(220)),
                                        exit = androidx.compose.animation.shrinkVertically(
                                            shrinkTowards = Alignment.CenterVertically,
                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                        ) + androidx.compose.animation.scaleOut(
                                            targetScale = 0.45f,
                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                        ) + androidx.compose.animation.fadeOut(animationSpec = tween(180))
                                    ) {
                                        UnpaidWeekBubbleRow(
                                            unpaidDateMillis = item.dateMillis,
                                            betweenLabel = item.betweenLabel,
                                            explicitPayment = item.explicitUnpaidPayment,
                                            onDeleteExplicit = item.explicitUnpaidPayment?.let { p -> { onDeletePayment(p) } },
                                            currentUserRole = effectiveRole
                                        )
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


