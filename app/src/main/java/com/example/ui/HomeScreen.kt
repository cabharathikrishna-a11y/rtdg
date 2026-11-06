package com.example.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.util.CurrencyFormatter

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val appColors = LocalAppThemeColors.current
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val statsMap by viewModel.dashboardStatsMap.collectAsStateWithLifecycle()
    val overviewList by viewModel.customerOverviewList.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val activeDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val collectionGroups by viewModel.collectionGroups.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val customerSortMode by viewModel.customerSortMode.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val allLoanCycles by viewModel.allLoanCycles.collectAsStateWithLifecycle()
    val unmappedPayments by viewModel.unmappedPayments.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val activeLoanCycles by viewModel.activeLoanCycles.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    var linkingPayment by remember { mutableStateOf<UnmappedPayment?>(null) }

    // Rule 1 Enforcement: Strip Friday completely out of any day lists
    val daysList = remember(collectionGroups) { 
        getOrderedDaysList(collectionGroups).filter { !it.equals("Friday", ignoreCase = true) } 
    }

    val context = LocalContext.current
    var customerToReorder by remember { mutableStateOf<CustomerCollectionItem?>(null) }
    var inputPositionText by remember { mutableStateOf("") }

    var dragAmount by remember(activeDay) { mutableStateOf(0f) }

    var confirmEditPaymentTarget by remember { mutableStateOf<WeeklyPayment?>(null) }
    var editingPaymentTarget by remember { mutableStateOf<WeeklyPayment?>(null) }
    var todayPaymentsListTarget by remember { mutableStateOf<List<WeeklyPayment>?>(null) }
    var deletingPaymentTarget by remember { mutableStateOf<WeeklyPayment?>(null) }

    if (linkingPayment != null) {
        val payment = linkingPayment!!
        var customerSearchText by remember { mutableStateOf("") }
        var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
        var linkAliasChecked by remember { mutableStateOf(true) }

        val customersWithActiveLoans = remember(allCustomers, activeLoanCycles) {
            allCustomers.filter { c ->
                activeLoanCycles.any { l -> l.customerId == c.id && l.status == "ACTIVE" }
            }
        }

        val filteredCustomers = remember(customersWithActiveLoans, customerSearchText) {
            if (customerSearchText.isBlank()) {
                customersWithActiveLoans
            } else {
                customersWithActiveLoans.filter {
                    it.name.contains(customerSearchText, ignoreCase = true) ||
                    it.phone.contains(customerSearchText)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { linkingPayment = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(translate("Link Unmapped UPI Payment", language), color = Color.Black, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(translate("Payment details:", language), color = Color.Black)
                    Text("• Amount: ₹${payment.amount.toLong()}", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("• Sender: ${payment.sender}", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("• UTR: ${payment.txnId}", color = Color.Black)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(translate("Select Borrower to map:", language), fontWeight = FontWeight.SemiBold, color = Color.Black)

                    OutlinedTextField(
                        value = customerSearchText,
                        onValueChange = { customerSearchText = it },
                        label = { Text(translate("Search Borrower", language)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredCustomers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(translate("No borrower with active loan matches.", language), fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredCustomers) { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFE2E8F0) else Color.Transparent)
                                        .clickable { selectedCustomer = cust }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                        Text(cust.phone, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = appColors.primaryAccent)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { linkAliasChecked = !linkAliasChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = linkAliasChecked, onCheckedChange = { linkAliasChecked = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(translate("Remember as UPI alias name for this customer", language), fontSize = 11.sp, color = Color.Black)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cust = selectedCustomer
                        if (cust != null) {
                            viewModel.mapUnmappedPaymentToCustomer(
                                customerId = cust.id,
                                txnId = payment.txnId,
                                amount = payment.amount,
                                linkAlias = linkAliasChecked
                            )
                            linkingPayment = null
                        }
                    },
                    enabled = selectedCustomer != null,
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                ) {
                    Text(translate("Confirm Link", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { linkingPayment = null }) {
                    Text(translate("Cancel", language))
                }
            }
        )
    }

    CylindricalTurnContainer(
        targetState = activeDay,
        directionProvider = { from, to -> getDayTransitionDirection(from, to, daysList) },
        modifier = Modifier.fillMaxSize()
    ) { currentDayVal ->
        val scrollState = rememberSaveable(currentDayVal, saver = LazyListState.Saver) {
            LazyListState(
                firstVisibleItemIndex = viewModel.getScrollIndexForDay(currentDayVal),
                firstVisibleItemScrollOffset = viewModel.getScrollOffsetForDay(currentDayVal)
            )
        }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(scrollState, currentDayVal) {
            snapshotFlow { Pair(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) }
                .collect { (index, offset) ->
                    viewModel.saveScrollStateForDay(currentDayVal, index, offset)
                }
        }

        LaunchedEffect(currentDayVal) {
            viewModel.recalibrateCalculationsSilent()
        }

        val currentOverviewList = remember(overviewList, currentDayVal, allCustomers, activeLoanCycles, allLoanCycles, allPayments, searchText, customerSortMode) {
            getOverviewListForDay(
                day = currentDayVal,
                allCustomers = allCustomers,
                activeLoanCycles = activeLoanCycles,
                allLoanCycles = allLoanCycles,
                allPayments = allPayments,
                search = searchText,
                sortMode = customerSortMode
            ).filter { !it.customer.collectionDay.trim().equals("Friday", ignoreCase = true) }
        }

        val currentStats = remember(statsMap, currentDayVal) {
            statsMap[currentDayVal] ?: DashboardStats()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentDayVal) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                        },
                        onDragEnd = {
                            if (totalDragX > 150f) {
                                val cIndex = daysList.indexOf(currentDayVal)
                                if (cIndex != -1) {
                                    val pIndex = (cIndex - 1 + daysList.size) % daysList.size
                                    viewModel.selectDay(daysList[pIndex])
                                }
                            } else if (totalDragX < -150f) {
                                val cIndex = daysList.indexOf(currentDayVal)
                                if (cIndex != -1) {
                                    val nIndex = (cIndex + 1) % daysList.size
                                    viewModel.selectDay(daysList[nIndex])
                                }
                            }
                        },
                        onDragCancel = {
                            totalDragX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount
                        }
                    )
                }
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Dynamic Selector for interest-earning collection group dashboards (3D Cylindrical Day Selector)
                item {
                    CylindricalDaySelector(
                        activeDay = currentDayVal,
                        daysList = daysList,
                        language = language,
                        onDaySelected = { viewModel.selectDay(it) }
                    )
                }

                // Quick Stats reporting panel at the top
                item {
                    StatsReportingCard(
                        stats = currentStats,
                        selectedDay = currentDayVal,
                        language = language,
                        onCardClick = { type ->
                            viewModel.navigateTo(Screen.CalculationDetail(type, currentDayVal))
                        }
                    )
                }

                if (currentDayVal == "Home") {
                    item {
                        CashBalanceBoard(viewModel = viewModel, language = language)
                    }
                }

                if (unmappedPayments.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626)
                                )
                                Text(
                                    text = translate("Unmapped UPI Payments Pending", language) + " (${unmappedPayments.size})",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            unmappedPayments.forEach { payment ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "₹${payment.amount.toLong()} from '${payment.sender}'",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "UTR: ${payment.txnId}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { viewModel.ignoreUnmappedPayment(payment.txnId) }
                                            ) {
                                                Text(translate("Ignore", language), color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = { linkingPayment = payment },
                                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                            ) {
                                                Text(translate("Link", language), color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar & Add Customer trigger
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                            // Active Live Search Bar
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { viewModel.updateSearchText(it) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                                placeholder = {
                                    Text(
                                        text = translate("Search by name or phone...", language),
                                        fontSize = 15.sp,
                                        color = Color.Black.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Black.copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = if (searchText.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = { viewModel.updateSearchText("") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = appColors.primaryAccent,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("search_customer_bar")
                            )

                            // Add Customer Button
                            if (currentUserRole != "USER") Button(
                                onClick = { viewModel.navigateTo(Screen.AddCustomer) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appColors.primaryAccent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("add_customer_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Customer", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(translate("Add", language), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    if (currentUserRole != "USER") {
                        item {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.BulkEntry(currentDayVal)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appColors.primaryAccent.copy(alpha = 0.08f),
                                    contentColor = appColors.primaryAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, appColors.primaryAccent.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .padding(vertical = 2.dp)
                                    .testTag("bulk_entry_trigger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = "Bulk Entry",
                                    tint = appColors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = translate("Bulk Update", language).uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = appColors.primaryAccent,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                if (currentDayVal == "Home") {
                    item {
                        HomePerformanceDashboard(
                            allPayments = allPayments,
                            allLoanCycles = allLoanCycles,
                            language = language,
                            appColors = appColors,
                            onViewMoreClicked = { viewModel.navigateTo(Screen.FullLedgerHistory) }
                        )
                    }
                } else {
                    // Customer Debt List Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bookLabel = translate(currentDayVal, language)
                                Text(
                                    text = "$bookLabel (${currentOverviewList.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                            }

                            if (currentOverviewList.isNotEmpty()) {
                                Text(
                                    text = translate("Arrange Customers By:", language),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sortOptions = listOf(
                                        Triple("ROUTE", translate("Route Order", language), Icons.Default.Place),
                                        Triple("NAME", translate("Name (A-Z)", language), Icons.Default.SortByAlpha),
                                        Triple("OUTSTANDING_DESC", translate("Outstanding (High-Low)", language), Icons.Filled.TrendingDown),
                                        Triple("LAST_PAYMENT_ASC", translate("Pending Longest", language), Icons.Default.DateRange),
                                        Triple("WEEKLY_DUE_DESC", translate("Weekly Due (High-Low)", language), Icons.Default.Star)
                                    )

                                    sortOptions.forEach { (mode, label, icon) ->
                                        val isSelected = (customerSortMode == mode)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) appColors.primaryAccent else Color(0xFFF1F5F9),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) appColors.primaryAccent else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier
                                                .clickable { viewModel.setCustomerSortMode(mode) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = if (isSelected) Color.White else Color.Black.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (currentOverviewList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.List,
                                        contentDescription = "Empty",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = if (searchText.isBlank()) "No customers in $currentDayVal's list yet." else "No search matches found.",
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    if (searchText.isBlank() && currentDayVal != "Home" && currentUserRole != "USER") {
                                        Button(
                                            onClick = { viewModel.navigateTo(Screen.AddCustomer) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentBlue)
                                        ) {
                                            Text("Create First Customer on $currentDayVal")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            count = currentOverviewList.size,
                            key = { index -> currentOverviewList[index].customer.id }
                        ) { index ->
                            val item = currentOverviewList[index]
                            
                            // Attention Checklist Shaking Optimization Logic (Previous 2 days + Today checklist verification window)
                            val hasPaymentInPast2Days = remember(allPayments, item.activeLoans) {
                                if (item.activeLoans.isEmpty()) true
                                else {
                                    val todayCal = Calendar.getInstance()
                                    val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
                                    val customerDayOfWeek = when {
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("monday") -> Calendar.MONDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("tuesday") -> Calendar.TUESDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("wednesday") -> Calendar.WEDNESDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("thursday") -> Calendar.THURSDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("friday") -> Calendar.FRIDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("saturday") -> Calendar.SATURDAY
                                        item.customer.collectionDay.lowercase(Locale.getDefault()).contains("sunday") -> Calendar.SUNDAY
                                        else -> -1
                                    }
                                    val diff = if (customerDayOfWeek != -1) {
                                        (todayDayOfWeek - customerDayOfWeek + 7) % 7
                                    } else {
                                        -1
                                    }
                                    val isCollectionWindow = diff in 0..2
                                    
                                    val midnightToday = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    val twoDaysAgoStart = midnightToday - java.util.concurrent.TimeUnit.DAYS.toMillis(2)
                                    
                                    val anyActiveLoanNeedsAttention = item.activeLoans.any { activeLoan ->
                                        val totalAmt = activeLoan.loanAmount + activeLoan.interestAmount
                                        val balance = totalAmt - activeLoan.paidAmount
                                        if (balance <= 0.0) return@any false
                                        
                                        val loanHasPayment = allPayments.any { 
                                            it.loanCycleId == activeLoan.id && 
                                            it.paymentDate >= twoDaysAgoStart && 
                                            it.status.uppercase() != "DELETED" && 
                                            it.amountPaid > 0.0 
                                        }
                                        isCollectionWindow && !loanHasPayment
                                    }
                                    !anyActiveLoanNeedsAttention
                                }
                            }

                            CustomerOverviewCard(
                                item = item,
                                displayIndex = item.originalGroupIndex,
                                viewModel = viewModel,
                                language = language,
                                activeDay = currentDayVal,
                                fontSizeScale = fontSizeScale,
                                showReorder = (currentDayVal != "Home" && customerSortMode == "ROUTE"),
                                onCardClicked = { viewModel.navigateTo(Screen.CustomerDetail(item.customer.id)) },
                                onMoveUp = { viewModel.moveCustomerUp(item) },
                                onMoveDown = { viewModel.moveCustomerDown(item) },
                                onReceiveClicked = { viewModel.navigateTo(Screen.RecordPayment(it)) },
                                onAddLoanClicked = { viewModel.navigateTo(Screen.AddLoan(item.customer.id)) },
                                onIndexClicked = {
                                    customerToReorder = item
                                    inputPositionText = item.originalGroupIndex.toString()
                                },
                                isScrolling = scrollState.isScrollInProgress || !hasPaymentInPast2Days, // Trigger attention checklist shaker animation
                                hasPaymentInPast2Days = hasPaymentInPast2Days,
                                onEditPaymentClicked = { activeLoanId ->
                                    val tCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
                                    val startOfToday = tCal.timeInMillis
                                    val startOfYesterday = startOfToday - java.util.concurrent.TimeUnit.DAYS.toMillis(1)

                                    val todayEntries = allPayments.filter { 
                                        it.loanCycleId == activeLoanId && it.paymentDate >= startOfToday && it.status.uppercase() != "DELETED" 
                                    }.sortedByDescending { it.paymentDate }

                                    if (todayEntries.size > 1) {
                                        todayPaymentsListTarget = todayEntries
                                    } else if (todayEntries.size == 1) {
                                        confirmEditPaymentTarget = todayEntries.first()
                                    } else {
                                        val recentEntries = allPayments.filter { 
                                            it.loanCycleId == activeLoanId && it.paymentDate >= startOfYesterday && it.status.uppercase() != "DELETED" 
                                        }.sortedByDescending { it.paymentDate }

                                        if (recentEntries.size > 1) {
                                            todayPaymentsListTarget = recentEntries
                                        } else if (recentEntries.isNotEmpty()) {
                                            confirmEditPaymentTarget = recentEntries.first()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }

            if (currentDayVal != "Home" && scrollState.firstVisibleItemIndex > 5) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 76.dp, end = 16.dp)
                        .size(48.dp)
                        .background(appColors.primaryAccent, CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(0)
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
        }
    }

    if (customerToReorder != null) {
        val targetItem = customerToReorder!!
        // Compute full unfiltered list for the day to ensure we have the correct total count and positions
        val allC = viewModel.allCustomers.value
        val currentDayList = allC.filter {
            activeDay == "Home" || it.collectionDay.trim().equals(activeDay.trim(), ignoreCase = true)
        }.sortedBy { it.customOrder }
        
        AlertDialog(
            onDismissRequest = { customerToReorder = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(translate("Set List Position", language), color = Color.Black, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (language == "Tamil") 
                            "எந்த வரிசையில் '${targetItem.customer.name}' இருக்க வேண்டும்:" 
                        else if (language == "Hindi")
                            "उधारकर्ता '${targetItem.customer.name}' के लिए स्थिति सेट करें:"
                        else if (language == "Telugu")
                            "సమూహంలో '${targetItem.customer.name}' స్థానాన్ని సెట్ చేయండి:"
                        else 
                            "Set position for '${targetItem.customer.name}' in $activeDay group:",
                        color = Color.Black
                    )
                    
                    if (currentDayList.isNotEmpty()) {
                        Text(
                            text = translate("Quick Jump (Tap a number below):", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        
                        val chunks = (1..currentDayList.size).chunked(5)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                chunks.forEach { rowNumbers ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowNumbers.forEach { posNumber ->
                                            val isCurrent = currentDayList.getOrNull(posNumber - 1)?.id == targetItem.customer.id
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .background(
                                                        color = if (isCurrent) appColors.primaryAccent else Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isCurrent) appColors.primaryAccent else Color(0xFFCBD5E1),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.reorderCustomerToPosition(targetItem, posNumber)
                                                        customerToReorder = null
                                                    }
                                            ) {
                                                Text(
                                                    text = "$posNumber",
                                                    color = if (isCurrent) Color.White else Color.Black,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        val remaining = 5 - rowNumbers.size
                                        if (remaining > 0) {
                                            repeat(remaining) {
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputPositionText,
                        onValueChange = { inputPositionText = it },
                        label = { Text(translate("Position", language) + " (1 to ${currentDayList.size})") },
                        placeholder = { Text("E.g. 1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = ColorSlateDark,
                            unfocusedContainerColor = ColorSlateDark,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = appColors.primaryAccent,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pos = inputPositionText.toIntOrNull()
                        if (pos != null && pos in 1..currentDayList.size) {
                            viewModel.reorderCustomerToPosition(targetItem, pos)
                            customerToReorder = null
                        } else {
                            Toast.makeText(
                                context, 
                                if (language == "Tamil") 
                                    "தயவுசெய்து 1 மற்றும் ${currentDayList.size} க்குள் சரியான எண்ணை உள்ளிடவும்" 
                                else if (language == "Hindi")
                                    "कृपया 1 और ${currentDayList.size} के बीच एक मान्य स्थिति दर्ज करें"
                                else if (language == "Telugu")
                                    "దయచేసి 1 మరియు ${currentDayList.size} మధ్య చెల్లుబాటు అయ్యే స్థానాన్ని నమోదు చేయండి"
                                else 
                                    "Please enter a valid position between 1 and ${currentDayList.size}", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                ) {
                    Text(translate("Save", language))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { customerToReorder = null }
                ) {
                    Text(translate("Cancel", language))
                }
            }
        )
    }

    if (todayPaymentsListTarget != null) {
        val entriesList = todayPaymentsListTarget!!
        val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
        val totalSum = entriesList.sumOf { it.amountPaid }

        AlertDialog(
            onDismissRequest = { todayPaymentsListTarget = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = {
                Column {
                    Text(
                        text = translate("Today's Collection Entries", language),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${translate("Total", language)}: ₹${CurrencyFormatter.format(totalSum)} (${entriesList.size} ${translate("entries", language)})",
                        fontSize = 13.sp,
                        color = ColorGainGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(entriesList) { itemEntry ->
                        val initialNote = itemEntry.notes.ifBlank { "Cash" }
                        val modeLabel = if (initialNote.equals("Online", ignoreCase = true)) "UPI" else initialNote
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (itemEntry.amountPaid <= 0.0) "₹0 (UNPAID)" else "₹${CurrencyFormatter.format(itemEntry.amountPaid)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (itemEntry.amountPaid <= 0.0) ColorLossRed else ColorGainGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val displayWeekNum = if (itemEntry.weekNumber > 0) {
                                        itemEntry.weekNumber
                                    } else {
                                        (allPayments.filter {
                                            it.loanCycleId == itemEntry.loanCycleId &&
                                            it.status.uppercase() != "DELETED" &&
                                            it.id != itemEntry.id &&
                                            it.amountPaid > 0.0 &&
                                            it.weekNumber > 0
                                        }.maxOfOrNull { it.weekNumber } ?: 0) + 1
                                    }
                                    Text(
                                        text = "${translate("Week", language)} $displayWeekNum • $modeLabel • ${timeFormat.format(java.util.Date(itemEntry.paymentDate))}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingPaymentTarget = itemEntry
                                            todayPaymentsListTarget = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Entry",
                                            tint = appColors.primaryAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            deletingPaymentTarget = itemEntry
                                            todayPaymentsListTarget = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Entry",
                                            tint = ColorLossRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { todayPaymentsListTarget = null }
                ) {
                    Text(translate("Close", language), color = appColors.primaryAccent, fontWeight = FontWeight.Bold)
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
            title = { Text(translate("Delete Instalment Entry", language), fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("${translate("Are you sure you want to delete this payment record of", language)} ₹${CurrencyFormatter.format(targetPayment.amountPaid)}?", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(targetPayment.id, targetPayment.loanCycleId)
                        deletingPaymentTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed)
                ) {
                    Text(translate("Delete Entry", language), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingPaymentTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text(translate("Cancel", language))
                }
            }
        )
    }

    if (confirmEditPaymentTarget != null) {
        val entry = confirmEditPaymentTarget!!
        AlertDialog(
            onDismissRequest = { confirmEditPaymentTarget = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(translate("Edit Collection Entry", language), fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text(translate("Do you want to edit this collection entry?", language), color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        editingPaymentTarget = entry
                        confirmEditPaymentTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                ) {
                    Text(translate("Yes", language), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmEditPaymentTarget = null }
                ) {
                    Text(translate("No", language), color = Color.Gray)
                }
            }
        )
    }

    if (editingPaymentTarget != null) {
        val entry = editingPaymentTarget!!
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
            mutableStateOf(if (entry.amountPaid <= 0.0) "" else entry.amountPaid.toInt().toString()) 
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
        var dateText by remember(entry.id) { mutableStateOf(sdfEdit.format(java.util.Date(entry.paymentDate))) }

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
                    dateText = sdfEdit.format(java.util.Date(newCalendar.timeInMillis))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog(
            onDismissRequest = { editingPaymentTarget = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(translate("Edit Instalment Entry", language), fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amtText,
                        onValueChange = { input ->
                            amtText = input.filter { it.isDigit() }
                        },
                        label = { Text(translate("Amount Collected (₹)", language), color = Color.Gray) },
                        placeholder = { Text("0", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
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
                        label = { Text(translate("Week Number", language), color = Color.Gray) },
                        placeholder = { Text(initialCalculatedWeek.toString(), color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        label = { Text(translate("Collection Date & Time", language), color = Color.Gray) },
                        readOnly = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
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
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Pick Date", tint = appColors.primaryAccent)
                                }
                                IconButton(onClick = { showTimePicker() }) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Schedule, contentDescription = "Pick Time", tint = appColors.primaryAccent)
                                }
                            }
                        }
                    )

                    if (showDatePickerState) {
                        com.example.ui.components.AdvancedDatePickerDialog(
                            initialTimeMs = parsedTimestamp,
                            onDismissRequest = { showDatePickerState = false },
                            onDateSelected = { selectedTimeMs ->
                                dateText = sdfEdit.format(java.util.Date(selectedTimeMs))
                                showDatePickerState = false
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { noteText = "Cash" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (noteText == "Cash") appColors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (noteText == "Cash") appColors.primaryAccent else Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(translate("Cash", language), color = if (noteText == "Cash") appColors.primaryAccent else Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                        }
                        Surface(
                            onClick = { noteText = "UPI" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (noteText == "UPI") appColors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (noteText == "UPI") appColors.primaryAccent else Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(translate("UPI", language), color = if (noteText == "UPI") appColors.primaryAccent else Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
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
                            // If amt not recorded or 0 then consider no entry that is its a unpaid 0
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
                        editingPaymentTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGainGreen)
                ) {
                    Text(translate("Save Changes", language), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPaymentTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                ) {
                    Text(translate("Cancel", language))
                }
            }
        )
    }
}

@Composable
fun CylindricalDaySelector(
    activeDay: String,
    daysList: List<String>,
    language: String,
    onDaySelected: (String) -> Unit
) {
    val appColors = LocalAppThemeColors.current
    val size = daysList.size
    if (size == 0) return

    val lazyListState = rememberLazyListState()

    val activeIndex = daysList.indexOf(activeDay).coerceAtLeast(0)
    LaunchedEffect(activeIndex, size) {
        if (activeIndex in 0 until size) {
            lazyListState.animateScrollToItem(activeIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth().testTag("horizontal_day_scroll_row"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysList.size) { index ->
                val day = daysList[index]
                val isActive = day == activeDay
                val animatedElevation = animateDpAsState(
                    targetValue = if (isActive) 6.dp else 2.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "chip_elevation"
                )
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .threeDDepthCard(elevation = animatedElevation.value, shape = RoundedCornerShape(22.dp))
                        .background(
                            color = if (isActive) appColors.primaryAccent else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .threeDPressable { onDaySelected(day) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("day_selector_chip_$day"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = translate(day, language).uppercase(Locale.getDefault()),
                        color = if (isActive) Color.White else Color.Black,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                        fontSize = if (isActive) 13.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBalanceBoard(viewModel: FinanceViewModel, language: String) {
    val appColors = LocalAppThemeColors.current
    val cashBalanceLogs by viewModel.allCashBalanceLogs.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val allLoanCycles by viewModel.allLoanCycles.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Last recorded cash in hand (before today)
    val lastLogBeforeToday = remember(cashBalanceLogs, startOfToday) {
        cashBalanceLogs.firstOrNull { it.date < startOfToday }
    }
    val yesterdayCashInHand = lastLogBeforeToday?.actualCash ?: 0.0

    // Today's collections in Cash
    val todayCashCollections = remember(allPayments, allLoanCycles, allCustomers, startOfToday) {
        val activeCustomerIds = allCustomers.map { it.id }.toSet()
        val loanMap = allLoanCycles.associateBy { it.id }
        allPayments
            .filter { p ->
                val loan = loanMap[p.loanCycleId]
                p.paymentDate >= startOfToday && 
                p.status.uppercase() != "DELETED" && 
                loan != null && 
                loan.customerId in activeCustomerIds
            }
            .sumOf { p ->
                val notes = p.notes.trim()
                if (notes.startsWith("Multiple - ", ignoreCase = true)) {
                    try {
                        val cashMarker = "Cash: ₹"
                        val cashStart = notes.indexOf(cashMarker)
                        if (cashStart != -1) {
                            parseAmountFromString(notes.substring(cashStart + cashMarker.length))
                        } else {
                            p.amountPaid
                        }
                    } catch (e: Exception) {
                        p.amountPaid
                    }
                } else {
                    val isOnline = !p.upiTxnId.isNullOrEmpty() || 
                                   p.notes.contains("Online", ignoreCase = true) || 
                                   p.notes.contains("UPI", ignoreCase = true) || 
                                   p.notes.contains("GPay", ignoreCase = true) || 
                                   p.notes.contains("PhonePe", ignoreCase = true) || 
                                   p.notes.contains("Paytm", ignoreCase = true) || 
                                   p.notes.contains("Bank", ignoreCase = true) ||
                                   p.notes.contains("Google Pay", ignoreCase = true) ||
                                   p.notes.contains("Phone Pe", ignoreCase = true) ||
                                   p.notes.contains("IMPS", ignoreCase = true) ||
                                   p.notes.contains("NEFT", ignoreCase = true) ||
                                   p.notes.contains("RTGS", ignoreCase = true) ||
                                   p.notes.contains("Net", ignoreCase = true) ||
                                   p.notes.contains("Transfer", ignoreCase = true)
                    if (isOnline) 0.0 else p.amountPaid
                }
            }
    }

    // Today's cash disbursals (loan cycles created today, cash given out is principal minus deduction)
    val todayCashDisbursals = remember(allLoanCycles, allCustomers, startOfToday) {
        val activeCustomerIds = allCustomers.map { it.id }.toSet()
        allLoanCycles
            .filter { l ->
                l.startDate >= startOfToday && 
                l.status.uppercase() != "DELETED" && 
                l.customerId in activeCustomerIds
            }
            .sumOf { l ->
                val notes = l.notes.trim()
                val totalDisbursed = l.loanAmount - l.deduction
                if (notes.startsWith("Multiple - ", ignoreCase = true)) {
                    try {
                        val cashMarker = "Cash: ₹"
                        val cashStart = notes.indexOf(cashMarker)
                        if (cashStart != -1) {
                            parseAmountFromString(notes.substring(cashStart + cashMarker.length))
                        } else {
                            totalDisbursed
                        }
                    } catch (e: Exception) {
                        totalDisbursed
                    }
                } else {
                    val isOnline = l.notes.contains("Online", ignoreCase = true) || 
                                   l.notes.contains("UPI", ignoreCase = true) || 
                                   l.notes.contains("GPay", ignoreCase = true) || 
                                   l.notes.contains("PhonePe", ignoreCase = true) || 
                                   l.notes.contains("Paytm", ignoreCase = true) || 
                                   l.notes.contains("Bank", ignoreCase = true) ||
                                   l.notes.contains("Google Pay", ignoreCase = true) ||
                                   l.notes.contains("Phone Pe", ignoreCase = true) ||
                                   l.notes.contains("IMPS", ignoreCase = true) ||
                                   l.notes.contains("NEFT", ignoreCase = true) ||
                                   l.notes.contains("RTGS", ignoreCase = true) ||
                                   l.notes.contains("Net", ignoreCase = true) ||
                                   l.notes.contains("Transfer", ignoreCase = true)
                    if (isOnline) 0.0 else totalDisbursed
                }
            }
    }

    // Calculated System Cash in Hand
    val systemCashInHand = yesterdayCashInHand + todayCashCollections - todayCashDisbursals

    // Today's logged balance
    val todayLog = remember(cashBalanceLogs, startOfToday) {
        cashBalanceLogs.firstOrNull { it.date >= startOfToday }
    }

    LaunchedEffect(todayLog, systemCashInHand, todayCashCollections, todayCashDisbursals) {
        val currentLog = todayLog
        if (currentLog != null) {
            val liveExpenses = systemCashInHand - currentLog.actualCash
            if (currentLog.systemCash != systemCashInHand ||
                currentLog.collectionAmount != todayCashCollections ||
                currentLog.disbursalAmount != todayCashDisbursals ||
                currentLog.expenses != liveExpenses
            ) {
                viewModel.logCashBalance(
                    actualCash = currentLog.actualCash,
                    systemCash = systemCashInHand,
                    collectionAmount = todayCashCollections,
                    disbursalAmount = todayCashDisbursals,
                    expenses = liveExpenses,
                    date = currentLog.date,
                    logId = currentLog.id
                )
            }
        }
    }

    var inputActualCash by rememberSaveable { mutableStateOf("") }
    var inputYesterdayCash by rememberSaveable { mutableStateOf("") }
    var isYesterdayFocused by remember { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(yesterdayCashInHand) {
        if (!isYesterdayFocused) {
            inputYesterdayCash = yesterdayCashInHand.toLong().toString()
        }
    }

    LaunchedEffect(inputYesterdayCash) {
        if (isYesterdayFocused && inputYesterdayCash.isNotBlank()) {
            val actualVal = inputYesterdayCash.toDoubleOrNull()
            if (actualVal != null && actualVal != yesterdayCashInHand) {
                delay(800)
                val targetLog = lastLogBeforeToday
                val systemVal = targetLog?.systemCash ?: 0.0
                val collVal = targetLog?.collectionAmount ?: 0.0
                val disbVal = targetLog?.disbursalAmount ?: 0.0
                val expensesVal = systemVal - actualVal
                val dateVal = targetLog?.date ?: (startOfToday - 12 * 3600 * 1000)
                val logIdVal = targetLog?.id ?: 0
                
                viewModel.logCashBalance(
                    actualCash = actualVal,
                    systemCash = systemVal,
                    collectionAmount = collVal,
                    disbursalAmount = disbVal,
                    expenses = expensesVal,
                    date = dateVal,
                    logId = logIdVal
                )
            }
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translate("CASH IN HAND TRACKER", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Unified Table Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Yesterday's Cash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("Yesterday's Cash", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    BasicTextField(
                        value = inputYesterdayCash,
                        onValueChange = { inputYesterdayCash = it.filter { c -> c.isDigit() } },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .width(120.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .onFocusChanged { focusState ->
                                isYesterdayFocused = focusState.isFocused
                                if (!focusState.isFocused) {
                                    if (inputYesterdayCash.isBlank()) {
                                        inputYesterdayCash = yesterdayCashInHand.toLong().toString()
                                    } else {
                                        val actualVal = inputYesterdayCash.toDoubleOrNull()
                                        if (actualVal != null && actualVal != yesterdayCashInHand) {
                                            val targetLog = lastLogBeforeToday
                                            val systemVal = targetLog?.systemCash ?: 0.0
                                            val collVal = targetLog?.collectionAmount ?: 0.0
                                            val disbVal = targetLog?.disbursalAmount ?: 0.0
                                            val expensesVal = systemVal - actualVal
                                            val dateVal = targetLog?.date ?: (startOfToday - 12 * 3600 * 1000)
                                            val logIdVal = targetLog?.id ?: 0
                                            
                                            viewModel.logCashBalance(
                                                actualCash = actualVal,
                                                systemCash = systemVal,
                                                collectionAmount = collVal,
                                                disbursalAmount = disbVal,
                                                expenses = expensesVal,
                                                date = dateVal,
                                                logId = logIdVal
                                            )
                                        }
                                    }
                                }
                            }
                            .testTag("yesterday_cash_input"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (inputYesterdayCash.isEmpty()) {
                                    Text(
                                        text = "₹ 0",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        textAlign = TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Row 2: Today's Cash Collections
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.navigateTo(Screen.CalculationDetail("COLLECTION", "Home", "CASH"))
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("Today's Cash Collections", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "+ ₹${todayCashCollections.toLong()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                // Row 3: Today's Cash Disbursals
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.navigateTo(Screen.CalculationDetail("DISBURSAL", "Home", "CASH"))
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("Today's Cash Disbursals", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "- ₹${todayCashDisbursals.toLong()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Row 4: System Cash in Hand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("System Cash in Hand", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "₹${systemCashInHand.toLong()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primaryAccent
                    )
                }

                // Row 5: My Recorded Cash in Hand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("My Recorded Cash", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    if (todayLog != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val currentLog = todayLog
                                    if (currentLog != null) {
                                        viewModel.deleteCashBalanceLog(currentLog)
                                        inputActualCash = currentLog.actualCash.toLong().toString()
                                    }
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("edit_cash_log_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Cash Log",
                                    tint = appColors.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "₹${todayLog!!.actualCash.toLong()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.widthIn(max = 180.dp)
                        ) {
                            BasicTextField(
                                value = inputActualCash,
                                onValueChange = { inputActualCash = it.filter { c -> c.isDigit() } },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.End
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                    .testTag("actual_cash_input"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (inputActualCash.isEmpty()) {
                                            Text(
                                                text = "₹ 0",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            IconButton(
                                onClick = {
                                    val actualVal = inputActualCash.toDoubleOrNull()
                                    if (actualVal != null) {
                                        val expensesVal = systemCashInHand - actualVal
                                        viewModel.logCashBalance(
                                            actualCash = actualVal,
                                            systemCash = systemCashInHand,
                                            collectionAmount = todayCashCollections,
                                            disbursalAmount = todayCashDisbursals,
                                            expenses = expensesVal
                                        )
                                        inputActualCash = ""
                                    }
                                },
                                enabled = inputActualCash.isNotBlank(),
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (inputActualCash.isNotBlank()) appColors.primaryAccent else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("save_cash_log_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save Cash Log",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Row 6: Auto-Calculated Expenses
                val finalExpenses = if (todayLog != null) {
                    systemCashInHand - todayLog!!.actualCash
                } else {
                    val enteredCash = inputActualCash.toDoubleOrNull() ?: 0.0
                    if (inputActualCash.isNotBlank()) {
                        systemCashInHand - enteredCash
                    } else {
                        null
                    }
                }

                if (finalExpenses != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = translate("Calculated Expenses", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "₹${finalExpenses.toLong()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (finalExpenses > 0) Color(0xFFDC2626) else if (finalExpenses < 0) Color(0xFF16A34A) else Color.DarkGray
                        )
                    }
                }
            }

            // Expandable History Section
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showHistory) translate("Hide Log History", language) else translate("View Log History", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primaryAccent
                    )
                }

                androidx.compose.animation.AnimatedVisibility(visible = showHistory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val historyLogs = cashBalanceLogs.take(5)
                        if (historyLogs.isEmpty()) {
                            Text(
                                text = translate("No cash logs recorded yet.", language),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        } else {
                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
                            historyLogs.forEach { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dateFormat.format(java.util.Date(log.date)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appColors.primaryAccent
                                            )
                                            Text(
                                                text = "${translate("Recorded Cash", language)}: ₹${log.actualCash.toLong()}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${translate("Collections", language)}: +₹${log.collectionAmount.toLong()}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF16A34A),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${translate("Disbursals", language)}: -₹${log.disbursalAmount.toLong()}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFDC2626),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${translate("System", language)}: ₹${log.systemCash.toLong()}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "${translate("Expenses", language)}: ₹${log.expenses.toLong()}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (log.expenses > 0) Color(0xFFDC2626) else if (log.expenses < 0) Color(0xFF16A34A) else Color.DarkGray
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
    }
}

@Composable
fun StatsReportingCard(
    stats: DashboardStats,
    selectedDay: String,
    language: String,
    onCardClick: ((String) -> Unit)? = null
) {
    val appColors = LocalAppThemeColors.current
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = appColors.headerCardBg
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, appColors.headerCardBorder, RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translate("DASHBOARD", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textOnHeader,
                    letterSpacing = 1.5.sp
                )
                // Tag-like Selected Day pills
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = appColors.primaryAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, appColors.primaryAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = translate(selectedDay, language).uppercase(),
                        color = if (appColors.isDark) appColors.primaryAccent else appColors.textOnHeader,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            val isHome = selectedDay.equals("Home", ignoreCase = true)
            val showCollection = if (isHome) stats.todaysCollectedAmount else stats.groupTodaysCollectedAmount
            val showDisbursed = if (isHome) stats.todaysDisbursedAmount else stats.groupTodaysDisbursedAmount
            val showInterest = if (isHome) stats.todaysInterestAmount else stats.groupTodaysInterestAmount
            val showDeductions = if (isHome) stats.todaysDeductionsAmount else stats.groupTodaysDeductionsAmount
            
            val showCollectionPill = showCollection != 0.0
            val showDisbursedPill = showDisbursed != 0.0
            val showInterestPill = showInterest != 0.0
            val showDeductionsPill = showDeductions != 0.0
            val hasAnyPills = showCollectionPill || showDisbursedPill || showInterestPill || showDeductionsPill

            if (hasAnyPills) {
                Spacer(modifier = Modifier.height(10.dp))

                // Beautiful vertical integration layout for dashboard stats to prevent horizontal truncations (no more dots / ellipses)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Collected
                    if (showCollectionPill) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDDepthCard(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                .background(appColors.todayCollectionBg, RoundedCornerShape(12.dp))
                                .threeDPressable(enabled = onCardClick != null) { onCardClick?.invoke("COLLECTION") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("stats_card_collection_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = "Collected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = translate("Collected", language),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            AnimatedNumberText(
                                targetValue = showCollection,
                                prefix = "₹ ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                delayMillis = 0,
                                durationMillis = 1200
                            )
                        }
                    }

                    // 2. Disbursed
                    if (showDisbursedPill) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDDepthCard(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                .background(appColors.todayDueCreatedBg, RoundedCornerShape(12.dp))
                                .threeDPressable(enabled = onCardClick != null) { onCardClick?.invoke("DISBURSAL") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("stats_card_disbursal_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingDown,
                                    contentDescription = "Disbursed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = translate("Disbursed", language),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            AnimatedNumberText(
                                targetValue = showDisbursed,
                                prefix = "₹ ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                delayMillis = 150,
                                durationMillis = 1200
                            )
                        }
                    }

                    // 3. Interest
                    if (showInterestPill) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDDepthCard(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                .background(appColors.todayInterestBg, RoundedCornerShape(12.dp))
                                .threeDPressable(enabled = onCardClick != null) { onCardClick?.invoke("PROFIT") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("stats_card_profit_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = "Profit",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = translate("Profit", language),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            AnimatedNumberText(
                                targetValue = showInterest,
                                prefix = "₹ ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                delayMillis = 300,
                                durationMillis = 1200
                            )
                        }
                    }

                    // 4. Deductions
                    if (showDeductionsPill) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDDepthCard(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                .background(Color(0xFF8B5CF6), RoundedCornerShape(12.dp))
                                .threeDPressable(enabled = onCardClick != null) { onCardClick?.invoke("DEDUCTIONS") }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("stats_card_deductions_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoneyOff,
                                    contentDescription = "Deductions",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = translate("Deductions", language),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            AnimatedNumberText(
                                targetValue = showDeductions,
                                prefix = "₹ ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                delayMillis = 450,
                                durationMillis = 1200
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(if (hasAnyPills) 16.dp else 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Outstanding Amt", language),
                        fontSize = 12.sp,
                        color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.7f)
                    )
                    AnimatedNumberText(
                        targetValue = stats.totalOutstandingDue,
                        prefix = "₹",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = appColors.textOnHeader,
                        delayMillis = 0,
                        durationMillis = 1600
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Active Accounts", language),
                        fontSize = 11.sp,
                        color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.7f)
                    )
                    AnimatedNumberText(
                        targetValue = stats.activeCyclesCount.toDouble(),
                        prefix = "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textOnHeader,
                        delayMillis = 200,
                        durationMillis = 1600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (appColors.isDark) Color.DarkGray else appColors.textOnHeader.copy(alpha = 0.15f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Outstanding Interest Receivable Calculation Visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Outstanding Principle", language),
                        fontSize = 11.sp,
                        color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.7f)
                    )
                    AnimatedNumberText(
                        targetValue = stats.outstandingPrincipal,
                        prefix = "₹",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textOnHeader,
                        delayMillis = 400,
                        durationMillis = 1600
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Outstanding Int", language),
                        fontSize = 11.sp,
                        color = if (appColors.isDark) Color.LightGray else appColors.textOnHeader.copy(alpha = 0.7f)
                    )
                    AnimatedNumberText(
                        targetValue = stats.outstandingInterest,
                        prefix = "₹",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textOnHeader,
                        delayMillis = 600,
                        durationMillis = 1600
                    )
                }
            }
        }
        }
    }
}

@Composable
fun CustomerOverviewCard(
    item: CustomerCollectionItem,
    displayIndex: Int,
    viewModel: FinanceViewModel,
    language: String,
    activeDay: String,
    fontSizeScale: Float,
    showReorder: Boolean,
    onCardClicked: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReceiveClicked: (Int) -> Unit,
    onAddLoanClicked: () -> Unit,
    onIndexClicked: () -> Unit,
    isScrolling: Boolean = false,
    onEditPaymentClicked: ((Int) -> Unit)? = null,
    hasPaymentInPast2Days: Boolean? = null
) {
    val appColors = LocalAppThemeColors.current
    val context = LocalContext.current
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, appColors.primaryAccent.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .threeDDepthCard(elevation = 3.dp, shape = RoundedCornerShape(14.dp))
            .threeDPressable { onCardClicked() }
            .testTag("customer_card_${item.customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sequence Re-order controls
            if (activeDay != "Home") {
                if (showReorder) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onIndexClicked() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$displayIndex",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), shape = CircleShape)
                            .clickable {
                                Toast.makeText(
                                    viewModel.getApplication(),
                                    translate("Auto-sorting is active. Select 'Route Order' to arrange manually.", language),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    ) {
                        Text(
                            text = "$displayIndex",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Customer Details & Active Accounts
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(horizontal = 4.dp)
            ) {
                // Customer Name on left and City on far right corner in the same straight line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorSlateDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.customer.city.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.customer.city,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorSlateDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (item.customer.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Black)
                        Text(
                            text = if (item.customer.phone2.isNotBlank()) "${item.customer.phone}, ${item.customer.phone2}" else item.customer.phone,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val todayCal = remember { Calendar.getInstance() }
                val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
                val activeDayLower = activeDay.lowercase(Locale.getDefault())
                val isCurrentDayMyDay = when {
                    activeDayLower.contains("monday") -> todayDayOfWeek == Calendar.MONDAY
                    activeDayLower.contains("tuesday") -> todayDayOfWeek == Calendar.TUESDAY
                    activeDayLower.contains("wednesday") -> todayDayOfWeek == Calendar.WEDNESDAY
                    activeDayLower.contains("thursday") -> todayDayOfWeek == Calendar.THURSDAY
                    activeDayLower.contains("friday") -> todayDayOfWeek == Calendar.FRIDAY
                    activeDayLower.contains("saturday") -> todayDayOfWeek == Calendar.SATURDAY
                    activeDayLower.contains("sunday") -> todayDayOfWeek == Calendar.SUNDAY
                    else -> false
                }

                if (item.activeLoans.isNotEmpty()) {
                    // Loop over all active accounts of this customer
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item.activeLoans.forEachIndexed { index, activeLoan ->
                            if (index > 0) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                            
                            val totalAmt = activeLoan.loanAmount + activeLoan.interestAmount
                            val progress = if (totalAmt > 0) (activeLoan.paidAmount / totalAmt).toFloat() else 1f
                            val balance = totalAmt - activeLoan.paidAmount
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = "Collected ₹${CurrencyFormatter.format(activeLoan.paidAmount)} / ₹${CurrencyFormatter.format(totalAmt)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorGainGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = ColorGainGreen,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Due Bal: ₹${CurrencyFormatter.format(balance)}  •  ₹${CurrencyFormatter.format(activeLoan.weeklyAmount)}/wk",
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                }
                                
                                val todayPaidAmt = item.todaysPayments[activeLoan.id]

                                val actualHasPaymentInPast2Days = remember(viewModel.allPayments.value, activeLoan.id) {
                                    val midnightToday = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    val twoDaysAgoStart = midnightToday - java.util.concurrent.TimeUnit.DAYS.toMillis(2)
                                    viewModel.allPayments.value.any {
                                        it.loanCycleId == activeLoan.id &&
                                        it.paymentDate >= twoDaysAgoStart &&
                                        it.status.uppercase() != "DELETED" &&
                                        it.amountPaid > 0.0
                                    }
                                }

                                val customerDayOfWeek = when {
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("monday") -> Calendar.MONDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("tuesday") -> Calendar.TUESDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("wednesday") -> Calendar.WEDNESDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("thursday") -> Calendar.THURSDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("friday") -> Calendar.FRIDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("saturday") -> Calendar.SATURDAY
                                    item.customer.collectionDay.lowercase(Locale.getDefault()).contains("sunday") -> Calendar.SUNDAY
                                    else -> -1
                                }
                                val diff = if (customerDayOfWeek != -1) {
                                    (todayDayOfWeek - customerDayOfWeek + 7) % 7
                                } else {
                                    -1
                                }

                                val isCustomerCollectionDayToday = (customerDayOfWeek == todayDayOfWeek)

                                val needsAttention = balance > 0.0 && 
                                                     (diff in 0..2) && 
                                                     !actualHasPaymentInPast2Days

                                val showRedMultiple = needsAttention && isCustomerCollectionDayToday

                                if (todayPaidAmt != null) {
                                    Text(
                                        text = if (todayPaidAmt == 0.0) "0" else "₹${CurrencyFormatter.format(todayPaidAmt)}",
                                        color = ColorGainGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                            .clickable {
                                                onEditPaymentClicked?.invoke(activeLoan.id)
                                            }
                                    )
                                } else {
                                    val isCreatedToday = (activeLoan.startDate >= (remember {
                                         val cal = Calendar.getInstance()
                                         cal.set(Calendar.HOUR_OF_DAY, 0)
                                         cal.set(Calendar.MINUTE, 0)
                                         cal.set(Calendar.SECOND, 0)
                                         cal.set(Calendar.MILLISECOND, 0)
                                         cal.timeInMillis
                                     }))
                                    if (isCreatedToday && isCurrentDayMyDay) {
                                        Text(
                                            text = "₹${CurrencyFormatter.format(totalAmt)}",
                                            color = Color(0xFFFF5722),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                                .clickable { onReceiveClicked(activeLoan.id) }
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            if (showRedMultiple) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                         .background(Color.Red, CircleShape)
                                                        .clickable {
                                                            viewModel.recordWeeklyPayment(
                                                                loanCycleId = activeLoan.id,
                                                                amount = 0.0,
                                                                weekNum = (viewModel.allPayments.value.filter { it.loanCycleId == activeLoan.id && it.status == "ACTIVE" && it.amountPaid > 0.0 && it.weekNumber > 0 }.maxOfOrNull { it.weekNumber } ?: 0) + 1,
                                                                notes = "UNPAID"
                                                            )
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "×",
                                                        color = Color.White,
                                                        fontSize = 15.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         modifier = Modifier
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                            }
                                            AttentionShakingGotButton(
                                                shouldShake = showRedMultiple,
                                                testTag = "quick_received_payment_${item.customer.id}_${activeLoan.id}",
                                                modifier = if (currentUserRole == "USER") Modifier.requiredSize(0.dp) else Modifier,
                                                onClick = { onReceiveClicked(activeLoan.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No active loans
                    val isDisbursedToday = item.todaysDisbursedLoans.isNotEmpty()
                    val totalDisbursedAmt = if (isDisbursedToday) item.todaysDisbursedLoans.sumOf { it.loanAmount - it.deduction } else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isDisbursedToday) {
                                        translate("New loan cycle disbursed today", language)
                                    } else {
                                        translate("No active cycle", language)
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isCurrentDayMyDay) {
                            if (isDisbursedToday) {
                                Text(
                                    text = "₹${CurrencyFormatter.format(totalDisbursedAmt)}",
                                    color = Color(0xFFFF5722), // Red-orangish color
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            } else {
                                // Hide the + DUE button completely stating 1 blank line in black colour
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text("—", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onAddLoanClicked() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 55.dp, minHeight = 28.dp).then(if (currentUserRole == "USER") Modifier.requiredSize(0.dp) else Modifier)
                                    .padding(start = 4.dp)
                            ) {
                                Text(
                                    text = "+ DUE",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    maxLines = 1
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
fun HomePerformanceDashboard(
    allPayments: List<WeeklyPayment>,
    allLoanCycles: List<LoanCycle>,
    language: String,
    appColors: AppThemeColors,
    onViewMoreClicked: () -> Unit
) {
    val dailyStatsList = remember(allPayments, allLoanCycles, language) {
        val list = mutableListOf<DailyStats>()
        val currentLocale = when(language) {
            "Tamil" -> Locale("ta")
            "Hindi" -> Locale("hi", "IN")
            "Telugu" -> Locale("te", "IN")
            else -> Locale.US
        }
        val sdf = SimpleDateFormat("dd-MMM", currentLocale)
        val dayFormat = SimpleDateFormat("EEE", currentLocale)
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        
        for (i in 0..6) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            
            val startCal = Calendar.getInstance().apply {
                timeInMillis = dayCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = dayCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            
            val startMs = startCal.timeInMillis
            val endMs = endCal.timeInMillis
            
            val dayPayments = allPayments.filter { it.paymentDate in startMs..endMs && it.status.uppercase() != "DELETED" }
            val dayPaymentsSum = dayPayments.sumOf { it.amountPaid }
            val dayLoanSum = allLoanCycles.filter { it.startDate in startMs..endMs && it.status.uppercase() != "DELETED" }.sumOf { it.loanAmount - it.deduction }
            val loanMap = allLoanCycles.associateBy { it.id }
            val dayInterestSum = dayPayments.sumOf { p ->
                val loan = loanMap[p.loanCycleId]
                if (loan != null && loan.status.uppercase() != "DELETED") {
                    val total = loan.loanAmount + loan.interestAmount
                    val ratio = if (total > 0.0) loan.interestAmount / total else 0.0
                    p.amountPaid * ratio
                } else {
                    0.0
                }
            }
            
            val dateLabel = "${sdf.format(dayCal.time)} (${dayFormat.format(dayCal.time)})"
            list.add(DailyStats(dateLabel, dayPaymentsSum, dayLoanSum, dayInterestSum))
        }
        list
    }

    val performanceStats = remember(allPayments, allLoanCycles) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        
        val endOfTodayMs = cal.timeInMillis
        
        val startOfCurrentWeekCal = Calendar.getInstance().apply {
            timeInMillis = endOfTodayMs
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfCurrentWeekMs = startOfCurrentWeekCal.timeInMillis
        
        val startOfPrevWeekCal = Calendar.getInstance().apply {
            timeInMillis = endOfTodayMs
            add(Calendar.DAY_OF_YEAR, -13)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfPrevWeekMs = startOfPrevWeekCal.timeInMillis
        
        val endOfPrevWeekCal = Calendar.getInstance().apply {
            timeInMillis = endOfTodayMs
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfPrevWeekMs = endOfPrevWeekCal.timeInMillis
        
        val curCollection = allPayments.filter { it.paymentDate in startOfCurrentWeekMs..endOfTodayMs && it.status.uppercase() != "DELETED" }.sumOf { it.amountPaid }
        val prevCollection = allPayments.filter { it.paymentDate in startOfPrevWeekMs..endOfPrevWeekMs && it.status.uppercase() != "DELETED" }.sumOf { it.amountPaid }
        
        val curDueCreated = allLoanCycles.filter { it.startDate in startOfCurrentWeekMs..endOfTodayMs && it.status.uppercase() != "DELETED" }.sumOf { it.loanAmount - it.deduction }
        val prevDueCreated = allLoanCycles.filter { it.startDate in startOfPrevWeekMs..endOfPrevWeekMs && it.status.uppercase() != "DELETED" }.sumOf { it.loanAmount - it.deduction }
        
        val loanMap = allLoanCycles.associateBy { it.id }
        val curPayments = allPayments.filter { it.paymentDate in startOfCurrentWeekMs..endOfTodayMs && it.status.uppercase() != "DELETED" }
        val prevPayments = allPayments.filter { it.paymentDate in startOfPrevWeekMs..endOfPrevWeekMs && it.status.uppercase() != "DELETED" }

        val curInterest = curPayments.sumOf { p ->
            val loan = loanMap[p.loanCycleId]
            if (loan != null && loan.status.uppercase() != "DELETED") {
                val total = loan.loanAmount + loan.interestAmount
                val ratio = if (total > 0.0) loan.interestAmount / total else 0.0
                p.amountPaid * ratio
            } else {
                0.0
            }
        }
        val prevInterest = prevPayments.sumOf { p ->
            val loan = loanMap[p.loanCycleId]
            if (loan != null && loan.status.uppercase() != "DELETED") {
                val total = loan.loanAmount + loan.interestAmount
                val ratio = if (total > 0.0) loan.interestAmount / total else 0.0
                p.amountPaid * ratio
            } else {
                0.0
            }
        }

        val colDiff = curCollection - prevCollection
        val dueDiff = curDueCreated - prevDueCreated
        val interestDiff = curInterest - prevInterest
        
        val colPct = if (prevCollection > 0.0) (colDiff / prevCollection) * 100.0 else if (curCollection > 0.0) 100.0 else 0.0
        val duePct = if (prevDueCreated > 0.0) (dueDiff / prevDueCreated) * 100.0 else if (curDueCreated > 0.0) 100.0 else 0.0
        val interestPct = if (prevInterest > 0.0) (interestDiff / prevInterest) * 100.0 else if (curInterest > 0.0) 100.0 else 0.0
        
        object {
            val currentCollection = curCollection
            val previousCollection = prevCollection
            val currentDueCreated = curDueCreated
            val previousDueCreated = prevDueCreated
            val currentInterest = curInterest
            val previousInterest = prevInterest
            val collectionDiff = colDiff
            val dueCreatedDiff = dueDiff
            val interestDiff = interestDiff
            val collectionPct = colPct
            val dueCreatedPct = duePct
            val interestPct = interestPct
            val collectionUp = colDiff >= 0.0
            val dueCreatedUp = dueDiff >= 0.0
            val interestUp = interestDiff >= 0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = translate("Weekly Performance", language),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.8f)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerformanceCard(
                title = translate("Collections", language),
                currentAmount = performanceStats.currentCollection,
                previousAmount = performanceStats.previousCollection,
                isUp = performanceStats.collectionUp,
                diffAmount = performanceStats.collectionDiff,
                percent = performanceStats.collectionPct,
                badgeColor = if (performanceStats.collectionUp) Color(0xFF22C55E) else Color(0xFFEF4444),
                badgeBg = if (performanceStats.collectionUp) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                language = language,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            PerformanceCard(
                title = translate("Dues Created", language),
                currentAmount = performanceStats.currentDueCreated,
                previousAmount = performanceStats.previousDueCreated,
                isUp = performanceStats.dueCreatedUp,
                diffAmount = performanceStats.dueCreatedDiff,
                percent = performanceStats.dueCreatedPct,
                badgeColor = if (performanceStats.dueCreatedUp) Color(0xFF3B82F6) else Color(0xFFF59E0B),
                badgeBg = if (performanceStats.dueCreatedUp) Color(0xFFDBEAFE) else Color(0xFFFEF3C7),
                language = language,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            PerformanceCard(
                title = translate("Profit", language),
                currentAmount = performanceStats.currentInterest,
                previousAmount = performanceStats.previousInterest,
                isUp = performanceStats.interestUp,
                diffAmount = performanceStats.interestDiff,
                percent = performanceStats.interestPct,
                badgeColor = if (performanceStats.interestUp) Color(0xFF8B5CF6) else Color(0xFFEF4444),
                badgeBg = if (performanceStats.interestUp) Color(0xFFF3E8FF) else Color(0xFFFEE2E2),
                language = language,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translate("Daily Ledger", language),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewMoreClicked() }
                    .background(appColors.primaryAccent.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = translate("View More", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = appColors.primaryAccent
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = appColors.primaryAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("Date / Day", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = translate("Collections", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF15803D),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = translate("Disbursed", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1D4ED8),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = translate("Profit", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF7E22CE),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                dailyStatsList.forEachIndexed { idx, stats ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stats.dateLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = if (stats.collection > 0) "₹${CurrencyFormatter.format(stats.collection)}" else "—",
                            fontSize = 12.sp,
                            fontWeight = if (stats.collection > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (stats.collection > 0) Color(0xFF16A34A) else Color.Gray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (stats.dueCreated > 0) "₹${CurrencyFormatter.format(stats.dueCreated)}" else "—",
                            fontSize = 12.sp,
                            fontWeight = if (stats.dueCreated > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (stats.dueCreated > 0) Color(0xFF2563EB) else Color.Gray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (stats.interestCollected > 0) "₹${CurrencyFormatter.format(stats.interestCollected)}" else "—",
                            fontSize = 12.sp,
                            fontWeight = if (stats.interestCollected > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (stats.interestCollected > 0) Color(0xFF9333EA) else Color.Gray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    if (idx < dailyStatsList.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceCard(
    title: String,
    currentAmount: Double,
    previousAmount: Double,
    isUp: Boolean,
    diffAmount: Double,
    percent: Double,
    badgeColor: Color,
    badgeBg: Color,
    language: String,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.5f)
            )

            AnimatedNumberText(
                targetValue = currentAmount,
                prefix = "₹",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                delayMillis = 100,
                durationMillis = 1400
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
                AnimatedNumberText(
                    targetValue = Math.abs(diffAmount),
                    prefix = "${if (isUp) "+" else ""}₹",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    delayMillis = 250,
                    durationMillis = 1400
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedNumberText(
                targetValue = previousAmount,
                prefix = translate("Prev: ₹", language),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.5f),
                delayMillis = 400,
                durationMillis = 1400
            )
        }
    }
}

private fun getDayTransitionDirection(fromDay: String, toDay: String, daysList: List<String>): Int {
    val fromIndex = daysList.indexOf(fromDay)
    val toIndex = daysList.indexOf(toDay)
    if (fromIndex == -1 || toIndex == -1) return 1
    val size = daysList.size
    val diff = (toIndex - fromIndex + size) % size
    return if (diff <= size / 2) 1 else -1
}

fun getOverviewListForDay(
    day: String,
    allCustomers: List<Customer>,
    activeLoanCycles: List<LoanCycle>,
    allLoanCycles: List<LoanCycle>,
    allPayments: List<WeeklyPayment>,
    search: String,
    sortMode: String
): List<CustomerCollectionItem> {
    val cleanQuery = search.trim()
    val isSearching = cleanQuery.isNotEmpty()

    val filteredByDay = if (day.equals("Home", ignoreCase = true) || day.equals("ALL", ignoreCase = true)) {
        allCustomers.filter { !it.collectionDay.trim().equals("Friday", ignoreCase = true) }
    } else {
        allCustomers.filter { 
            !it.collectionDay.trim().equals("Friday", ignoreCase = true) &&
            it.collectionDay.trim().equals(day.trim(), ignoreCase = true) 
        }
    }
    
    val sortedByOrder = filteredByDay.sortedBy { it.customOrder }
    
    val activeLoansByCustomer = activeLoanCycles.groupBy { it.customerId }
    val allLoansByCustomer = allLoanCycles.groupBy { it.customerId }
    val loanCustomerIdMap = allLoanCycles.associate { it.id to it.customerId }
    val paymentsByCustomer = allPayments.groupBy { loanCustomerIdMap[it.loanCycleId] }

    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis

    val mapped = sortedByOrder.mapIndexed { idx, customer ->
        val customerPayments = paymentsByCustomer[customer.id] ?: emptyList()
        val customerActiveLoans = (activeLoansByCustomer[customer.id] ?: emptyList()).map { loan ->
            val actualPaid = customerPayments.filter { it.loanCycleId == loan.id && it.status.uppercase() != "DELETED" }.sumOf { it.amountPaid }
            loan.copy(paidAmount = actualPaid)
        }
        val customerAllLoans = allLoansByCustomer[customer.id] ?: emptyList()
        val latestPayment = customerPayments.maxByOrNull { it.paymentDate }
        
        val customerTodaysPayments = customerPayments.filter { it.paymentDate >= startOfToday && it.status.uppercase() != "DELETED" }
            .groupBy { it.loanCycleId }
            .mapValues { entry -> entry.value.sumOf { it.amountPaid } }
        val customerTodaysDisbursed = customerAllLoans.filter { it.startDate >= startOfToday }

        CustomerCollectionItem(
            customer = customer,
            activeLoans = customerActiveLoans,
            lastPaymentDate = latestPayment?.paymentDate,
            lastPaymentAmount = latestPayment?.amountPaid,
            originalGroupIndex = idx + 1,
            todaysPayments = customerTodaysPayments,
            todaysDisbursedLoans = customerTodaysDisbursed
        )
    }
    
    val filtered = if (!isSearching) {
        mapped
    } else {
        val tokens = cleanQuery.lowercase(java.util.Locale.getDefault()).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        mapped.filter { item ->
            val c = item.customer
            val nameLower = c.name.lowercase(java.util.Locale.getDefault())
            val cityLower = c.city.lowercase(java.util.Locale.getDefault())
            val groupLower = c.collectionDay.lowercase(java.util.Locale.getDefault())
            val phoneRaw = c.phone.lowercase(java.util.Locale.getDefault())
            val phone2Raw = c.phone2.lowercase(java.util.Locale.getDefault())
            val phoneDigits = c.phone.filter { it.isDigit() }
            val phone2Digits = c.phone2.filter { it.isDigit() }
            val orderStr = c.customOrder.toString()
            val idStr = c.id.toString()
            val activeLoanIds = item.activeLoans.map { it.id.toString() }

            tokens.all { token ->
                val tokenDigits = token.filter { it.isDigit() }

                val matchesName = nameLower.contains(token)
                val matchesCity = cityLower.contains(token)
                val matchesGroup = groupLower.contains(token)
                val matchesPhoneRaw = phoneRaw.contains(token) || phone2Raw.contains(token)
                val matchesPhoneDigits = tokenDigits.isNotEmpty() && (
                    phoneDigits.contains(tokenDigits) || 
                    phone2Digits.contains(tokenDigits)
                )
                val matchesOrder = orderStr.contains(token) || idStr.contains(token)
                val matchesLoan = activeLoanIds.any { it.contains(token) }

                matchesName || matchesCity || matchesGroup || matchesPhoneRaw || matchesPhoneDigits || matchesOrder || matchesLoan
            }
        }
    }

    return when (sortMode) {
        "NAME" -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.customer.name })
        "OUTSTANDING_DESC" -> filtered.sortedByDescending { item ->
            item.activeLoans.sumOf { (it.loanAmount + it.interestAmount) - it.paidAmount }
        }
        "WEEKLY_DUE_DESC" -> filtered.sortedByDescending { item ->
            item.activeLoans.sumOf { it.weeklyAmount }
        }
        "LAST_PAYMENT_ASC" -> filtered.sortedWith(
            compareBy<CustomerCollectionItem> { it.lastPaymentDate != null }
                .thenBy { it.lastPaymentDate ?: 0L }
        )
        "LAST_PAYMENT_DESC" -> filtered.sortedWith(
            compareBy<CustomerCollectionItem> { it.lastPaymentDate == null }
                .thenByDescending { it.lastPaymentDate ?: 0L }
        )
        else -> filtered
    }
}

@Composable
private fun AttentionShakingGotButton(
    modifier: Modifier = Modifier,
    shouldShake: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    if (shouldShake) {
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        val translationX = infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(120, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "x"
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = ColorGainGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = modifier
                .testTag(testTag)
                .graphicsLayer {
                    this.translationX = translationX.value
                }
                .defaultMinSize(minWidth = 55.dp, minHeight = 28.dp)
                .padding(start = 4.dp)
        ) {
            Text(
                text = "+ GOT",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = ColorGainGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = modifier
                .testTag(testTag)
                .defaultMinSize(minWidth = 55.dp, minHeight = 28.dp)
                .padding(start = 4.dp)
        ) {
            Text(
                text = "+ GOT",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}


