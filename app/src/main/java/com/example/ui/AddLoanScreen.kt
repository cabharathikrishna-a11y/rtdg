package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddLoanScreen(
    customerId: Int,
    viewModel: FinanceViewModel
) {
    val appColors = LocalAppThemeColors.current
    val uiState by viewModel.addLoanUiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val allLoanCycles by viewModel.allLoanCycles.collectAsStateWithLifecycle()

    val blackTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black,
        focusedPlaceholderColor = Color.DarkGray,
        unfocusedPlaceholderColor = Color.DarkGray,
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black
    )

    val customerList by viewModel.allCustomers.collectAsStateWithLifecycle()
    val customer = customerList.find { it.id == customerId }
    var selectedSmsPhoneByUser by remember(customer?.id) { mutableStateOf(customer?.phone ?: "") }

    LaunchedEffect(customerId) {
        viewModel.resetAddLoanState()
        val (onlineTime, synced) = com.example.util.OnlineTimeHelper.getOnlineTimeOrLocal()
        viewModel.updateAddLoanState {
            it.copy(
                loanTimestamp = onlineTime,
                isTimeSynced = synced,
                isSyncingTime = false
            )
        }
    }

    var showDatePickerState by remember { mutableStateOf(false) }

    val showTimePicker = {
        android.app.TimePickerDialog(
            context.findActivity() ?: context,
            { _, hourOfDay, minute ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = uiState.loanTimestamp
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                viewModel.updateAddLoanState { it.copy(loanTimestamp = calendar.timeInMillis) }
            },
            Calendar.getInstance().apply { timeInMillis = uiState.loanTimestamp }.get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().apply { timeInMillis = uiState.loanTimestamp }.get(Calendar.MINUTE),
            false
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Give Loan: Create New Cycle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorSlateDark)



            if (uiState.isMultipleMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.cashPrincipalStr,
                        onValueChange = { input -> 
                            val cashStr = input.filter { it.isDigit() }
                            val onlineStr = uiState.onlinePrincipalStr
                            val p = (cashStr.toDoubleOrNull() ?: 0.0) + (onlineStr.toDoubleOrNull() ?: 0.0)
                            val w = uiState.tenureWeeks.toIntOrNull() ?: 0
                            val calculatedWeekly = if (!uiState.isWeeklyInstalmentManuallyEdited && w > 0) {
                                Math.round(p / w).toString()
                            } else {
                                uiState.weeklyInstalment
                            }
                            viewModel.updateAddLoanState {
                                it.copy(
                                    cashPrincipalStr = cashStr,
                                    loanAmountError = null,
                                    loanAmount = p.toLong().toString(),
                                    weeklyInstalment = calculatedWeekly
                                )
                            }
                        },
                        label = { Text("Cash Principal (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp), singleLine = true,
                        colors = blackTextFieldColors,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.onlinePrincipalStr,
                        onValueChange = { input -> 
                            val onlineStr = input.filter { it.isDigit() }
                            val cashStr = uiState.cashPrincipalStr
                            val p = (cashStr.toDoubleOrNull() ?: 0.0) + (onlineStr.toDoubleOrNull() ?: 0.0)
                            val w = uiState.tenureWeeks.toIntOrNull() ?: 0
                            val calculatedWeekly = if (!uiState.isWeeklyInstalmentManuallyEdited && w > 0) {
                                Math.round(p / w).toString()
                            } else {
                                uiState.weeklyInstalment
                            }
                            viewModel.updateAddLoanState {
                                it.copy(
                                    onlinePrincipalStr = onlineStr,
                                    loanAmountError = null,
                                    loanAmount = p.toLong().toString(),
                                    weeklyInstalment = calculatedWeekly
                                )
                            }
                        },
                        label = { Text("UPI Principal (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp), singleLine = true,
                        colors = blackTextFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (uiState.loanAmountError != null) {
                    Text(uiState.loanAmountError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            } else {
                OutlinedTextField(
                    value = uiState.loanAmount,
                    onValueChange = { input -> 
                        val filtered = input.filter { it.isDigit() }
                        val p = filtered.toDoubleOrNull() ?: 0.0
                        val w = uiState.tenureWeeks.toIntOrNull() ?: 0
                        val calculatedWeekly = if (!uiState.isWeeklyInstalmentManuallyEdited && w > 0) {
                            Math.round(p / w).toString()
                        } else {
                            uiState.weeklyInstalment
                        }
                        viewModel.updateAddLoanState {
                            it.copy(
                                loanAmount = filtered,
                                loanAmountError = null,
                                weeklyInstalment = calculatedWeekly
                            )
                        }
                    },
                    label = { Text("Loan Principal (₹)") },
                    placeholder = { Text("E.g., 10000") },
                    isError = uiState.loanAmountError != null,
                    supportingText = {
                        if (uiState.loanAmountError != null) {
                            Text(uiState.loanAmountError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = blackTextFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_principal_input")
                )
            }

            OutlinedTextField(
                value = uiState.interestAmount,
                onValueChange = { input -> 
                    val filtered = input.filter { it.isDigit() }
                    viewModel.updateAddLoanState { it.copy(interestAmount = filtered, interestError = null) }
                },
                label = { Text("Interest (₹)") },
                placeholder = { Text("E.g., 2000 (Set 0 if none)") },
                isError = uiState.interestError != null,
                supportingText = {
                    if (uiState.interestError != null) {
                        Text(uiState.interestError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = blackTextFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.deductionAmount,
                onValueChange = { input -> 
                    val filtered = input.filter { it.isDigit() }
                    viewModel.updateAddLoanState { it.copy(deductionAmount = filtered, deductionError = null) }
                },
                label = { Text("Deduction (Realized Profit) (₹)") },
                placeholder = { Text("E.g., 500 (Set 0 if none)") },
                isError = uiState.deductionError != null,
                supportingText = {
                    if (uiState.deductionError != null) {
                        Text(uiState.deductionError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = blackTextFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("loan_deduction_input")
            )

            OutlinedTextField(
                value = uiState.weeklyInstalment,
                onValueChange = { input -> 
                    val filtered = input.filter { it.isDigit() }
                    viewModel.updateAddLoanState { 
                        it.copy(
                            weeklyInstalment = filtered, 
                            instalmentError = null,
                            isWeeklyInstalmentManuallyEdited = true
                        ) 
                    }
                },
                label = { Text("Per week collection amount (₹)") },
                placeholder = { Text("E.g., 1200") },
                isError = uiState.instalmentError != null,
                supportingText = {
                    if (uiState.instalmentError != null) {
                        Text(uiState.instalmentError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp), singleLine = true,
                colors = blackTextFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_weekly_instalment_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.tenureWeeks,
                    onValueChange = { input -> 
                        val filtered = input.filter { it.isDigit() }
                        val p = uiState.loanAmount.toDoubleOrNull() ?: 0.0
                        val w = filtered.toIntOrNull() ?: 0
                        val calculatedWeekly = if (!uiState.isWeeklyInstalmentManuallyEdited && w > 0) {
                            Math.round(p / w).toString()
                        } else {
                            uiState.weeklyInstalment
                        }
                        viewModel.updateAddLoanState {
                            it.copy(
                                tenureWeeks = filtered,
                                tenureError = null,
                                weeklyInstalment = calculatedWeekly
                            )
                        }
                    },
                    label = { Text("Weeks tenure") },
                    isError = uiState.tenureError != null,
                    supportingText = {
                        if (uiState.tenureError != null) {
                            Text(uiState.tenureError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = blackTextFieldColors,
                    modifier = Modifier.weight(1.0f)
                )

                if (!uiState.isMultipleMode) {
                    val currentMode = uiState.disbursalMode
                    val isCash = currentMode == "Cash"
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .clickable {
                                val nextMode = if (isCash) "UPI" else "Cash"
                                viewModel.updateAddLoanState { it.copy(disbursalMode = nextMode) }
                            }
                    ) {
                        OutlinedTextField(
                            value = if (isCash) "CASH" else "UPI",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Disbursal Mode") },
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (isCash) Color(0xFF15803D) else Color(0xFF1D4ED8),
                                disabledContainerColor = if (isCash) Color(0xFFDCFCE7) else Color(0xFFDBEAFE),
                                disabledBorderColor = if (isCash) Color(0xFF16A34A) else Color(0xFF2563EB),
                                disabledLabelColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1.0f))
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { text -> viewModel.updateAddLoanState { it.copy(notes = text) } },
                label = { Text("Remarks (Optional)") },
                placeholder = { Text("Remarks details") },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = blackTextFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))

            // 📅 Date & Time Selection side-by-side boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Disbursal Date & Time",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSlateDark
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (uiState.isSyncingTime) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = appColors.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Syncing...",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (uiState.isTimeSynced) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🟢 ONLINE SYNCED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF3E0))
                                .clickable {
                                    viewModel.updateAddLoanState { it.copy(isSyncingTime = true) }
                                    coroutineScope.launch {
                                        val (onlineTime, synced) = com.example.util.OnlineTimeHelper.getOnlineTimeOrLocal()
                                        viewModel.updateAddLoanState {
                                            it.copy(
                                                loanTimestamp = onlineTime,
                                                isTimeSynced = synced,
                                                isSyncingTime = false
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚠️ OFFLINE (TAP TO SYNC)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorSlateDark.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, ColorSlateDark.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { showDatePickerState = true }
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Date", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(uiState.loanTimestamp)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorSlateDark
                        )
                    }
                }

                if (showDatePickerState) {
                    com.example.ui.components.AdvancedDatePickerDialog(
                        initialTimeMs = uiState.loanTimestamp,
                        onDismissRequest = { showDatePickerState = false },
                        onDateSelected = { selectedTimeMs ->
                            viewModel.updateAddLoanState { it.copy(loanTimestamp = selectedTimeMs) }
                            showDatePickerState = false
                        }
                    )
                }

                // Time Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorAccentBlue.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, ColorAccentBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { showTimePicker() }
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Time", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(uiState.loanTimestamp)).uppercase(Locale.getDefault()),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorAccentBlue
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        val newMode = !uiState.isMultipleMode
                        viewModel.updateAddLoanState { it.copy(isMultipleMode = newMode, disbursalMode = if (newMode) "Multiple" else "Cash") }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Multiple Modes (Cash + UPI)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorSlateDark)
                Checkbox(
                    checked = uiState.isMultipleMode,
                    onCheckedChange = { checked -> 
                        viewModel.updateAddLoanState { it.copy(isMultipleMode = checked, disbursalMode = if (checked) "Multiple" else "Cash") }
                    },
                    colors = CheckboxDefaults.colors(checkedColor = appColors.primaryAccent)
                )
            }

            val pForBtn = if (uiState.isMultipleMode) {
                (uiState.cashPrincipalStr.toDoubleOrNull() ?: 0.0) + (uiState.onlinePrincipalStr.toDoubleOrNull() ?: 0.0)
            } else {
                uiState.loanAmount.toDoubleOrNull()
            }
            val tForBtn = uiState.tenureWeeks.toIntOrNull()
            val wForBtn = if (uiState.weeklyInstalment.isBlank()) {
                if (pForBtn != null && tForBtn != null && tForBtn > 0) ((pForBtn + (uiState.interestAmount.toDoubleOrNull() ?: 0.0)) / tForBtn) else 0.0
            } else {
                uiState.weeklyInstalment.toDoubleOrNull()
            }
            val isSaveEnabled = pForBtn != null && pForBtn > 0.0 && tForBtn != null && tForBtn > 0 && wForBtn != null && wForBtn > 0.0

            if (customer != null && customer.phone2.isNotBlank() && customer.smsConfirmationOfEntry) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Send New Loan SMS to:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ColorSlateDark
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSmsPhoneByUser = customer.phone }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSmsPhoneByUser == customer.phone),
                                onClick = { selectedSmsPhoneByUser = customer.phone },
                                colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                            )
                            Text(
                                text = "Primary: ${customer.phone}",
                                fontSize = 12.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSmsPhoneByUser = customer.phone2 }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSmsPhoneByUser == customer.phone2),
                                onClick = { selectedSmsPhoneByUser = customer.phone2 },
                                colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                            )
                            Text(
                                text = "Secondary: ${customer.phone2}",
                                fontSize = 12.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val p = if (uiState.isMultipleMode) {
                        (uiState.cashPrincipalStr.toDoubleOrNull() ?: 0.0) + (uiState.onlinePrincipalStr.toDoubleOrNull() ?: 0.0)
                    } else {
                        uiState.loanAmount.toDoubleOrNull()
                    }
                    
                    val interestVal = uiState.interestAmount.toDoubleOrNull() ?: 0.0
                    val deductionVal = uiState.deductionAmount.toDoubleOrNull() ?: 0.0
                    val t = uiState.tenureWeeks.toIntOrNull()
                    val w = if (uiState.weeklyInstalment.isBlank()) {
                        if (p != null && t != null && t > 0) ((p + interestVal) / t) else 0.0
                    } else {
                        uiState.weeklyInstalment.toDoubleOrNull()
                    }
                    
                    val finalNotes = if (uiState.isMultipleMode) {
                        "Multiple - Cash: ₹${uiState.cashPrincipalStr.ifBlank { "0" }}, UPI: ₹${uiState.onlinePrincipalStr.ifBlank { "0" }}. ${uiState.notes}"
                    } else if (uiState.disbursalMode == "Online" || uiState.disbursalMode == "UPI") {
                        "UPI - ${uiState.notes}"
                    } else {
                        uiState.notes
                    }

                    var hasError = false
                    var errorLoanAmount: String? = null
                    var errorInterest: String? = null
                    var errorDeduction: String? = null
                    var errorTenure: String? = null
                    var errorInstalment: String? = null
                    
                    if (p == null || p <= 0.0) {
                        errorLoanAmount = "Loan Principal must be greater than 0"
                        hasError = true
                    }

                    if (uiState.interestAmount.isNotBlank() && (uiState.interestAmount.toDoubleOrNull() == null || interestVal < 0.0)) {
                        errorInterest = "Interest Fee must be greater than or equal to 0"
                        hasError = true
                    }

                    if (uiState.deductionAmount.isNotBlank() && (uiState.deductionAmount.toDoubleOrNull() == null || deductionVal < 0.0)) {
                        errorDeduction = "Deduction must be greater than or equal to 0"
                        hasError = true
                    }

                    if (t == null || t <= 0) {
                        errorTenure = "Tenure must be greater than 0"
                        hasError = true
                    }

                    if (w == null || w <= 0.0) {
                        errorInstalment = "Weekly target installment must be greater than 0"
                        hasError = true
                    }

                    if (hasError) {
                        viewModel.updateAddLoanState {
                            it.copy(
                                loanAmountError = errorLoanAmount,
                                interestError = errorInterest,
                                deductionError = errorDeduction,
                                tenureError = errorTenure,
                                instalmentError = errorInstalment
                            )
                        }
                    } else {
                        val daySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val targetDateStr = daySdf.format(Date(uiState.loanTimestamp))
                        val isDuplicate = allLoanCycles.any { existing ->
                            existing.customerId == customerId &&
                            existing.status.uppercase() != "DELETED" &&
                            existing.loanAmount == p!! &&
                            existing.interestAmount == interestVal &&
                            existing.deduction == deductionVal &&
                            daySdf.format(Date(existing.startDate)) == targetDateStr
                        }

                        if (isDuplicate) {
                            android.widget.Toast.makeText(context, "Duplicate entry: Loan already exists on this date!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.createLoanCycle(
                                customerId = customerId,
                                amount = p!!,
                                interest = interestVal,
                                weeklyInstalment = w!!,
                                tenureWeeks = t!!,
                                notes = finalNotes,
                                startDate = uiState.loanTimestamp,
                                deduction = deductionVal,
                                customSmsPhone = if (customer != null && customer.phone2.isNotBlank() && customer.smsConfirmationOfEntry) selectedSmsPhoneByUser else null
                            )
                            viewModel.navigateBack()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primaryAccent,
                    contentColor = Color.White,
                    disabledContainerColor = appColors.primaryAccent.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("save_loan_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
