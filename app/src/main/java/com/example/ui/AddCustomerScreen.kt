package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddCustomerScreen(viewModel: FinanceViewModel) {
    val appColors = LocalAppThemeColors.current
    val uiState by viewModel.addCustomerUiState.collectAsStateWithLifecycle()
    val collectionGroups by viewModel.collectionGroups.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    
    val defaultDay = if (viewModel.selectedDay.value == "Home") "Sunday" else viewModel.selectedDay.value

    LaunchedEffect(Unit) {
        viewModel.resetAddCustomerState(defaultDay = defaultDay)
    }

    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(translate("Add Customer", language), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorSlateDark)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { text -> 
                    viewModel.updateAddCustomerState { it.copy(name = text, isNameError = false) }
                },
                label = { Text(translate("Customer Name", language)) },
                placeholder = { Text("Enter full name") },
                isError = uiState.isNameError,
                supportingText = {
                    if (uiState.isNameError) {
                        Text("Client name is required", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedBorderColor = ColorSlateDark,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { text -> 
                    viewModel.updateAddCustomerState { it.copy(phone = text, phoneErrorText = null) }
                },
                label = { Text(translate("Phone Number", language)) },
                placeholder = { Text("E.g., 9876543210") },
                isError = uiState.phoneErrorText != null,
                supportingText = {
                    if (uiState.phoneErrorText != null) {
                        Text(uiState.phoneErrorText!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedBorderColor = ColorSlateDark,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input")
            )

            val cleanPrimaryPhone = uiState.phone.filter { it.isDigit() }
            if (cleanPrimaryPhone.length >= 10) {
                OutlinedTextField(
                    value = uiState.phone2,
                    onValueChange = { text -> 
                        viewModel.updateAddCustomerState { it.copy(phone2 = text, phone2ErrorText = null) }
                    },
                    label = { Text(translate("Additional Phone Number (Optional)", language)) },
                    placeholder = { Text("E.g., 9876543211") },
                    isError = uiState.phone2ErrorText != null,
                    supportingText = {
                        if (uiState.phone2ErrorText != null) {
                            Text(uiState.phone2ErrorText!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        focusedPlaceholderColor = Color.DarkGray,
                        unfocusedPlaceholderColor = Color.DarkGray,
                        focusedBorderColor = ColorSlateDark,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone2_input")
                )
            }

            OutlinedTextField(
                value = uiState.city,
                onValueChange = { text -> viewModel.updateAddCustomerState { it.copy(city = text) } },
                label = { Text(translate("City", language)) },
                placeholder = { Text("E.g., Mumbai / Delhi / Chennai") },
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedPlaceholderColor = Color.DarkGray,
                    unfocusedPlaceholderColor = Color.DarkGray,
                    focusedBorderColor = ColorSlateDark,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_city_input")
            )

            if (uiState.phone.isNotBlank()) {
                Text(translate("Preferred Language for Notifications", language), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorSlateDark)
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val languages = listOf("English", "Tamil", "Hindi", "Telugu")
                    items(languages) { langItem ->
                        val isSelected = langItem == uiState.preferredLanguage
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) ColorSlateDark else Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ColorSlateDark else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.updateAddCustomerState { it.copy(preferredLanguage = langItem) } }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = translate(langItem, language),
                                color = if (isSelected) Color.White else ColorSlateDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (uiState.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(translate("SMS Confirmation", language), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorSlateDark)

                // Preferences checkbox stack - 2 options
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = uiState.phone.isNotBlank()) { 
                        viewModel.updateAddCustomerState { it.copy(smsWeeklyReminder = !it.smsWeeklyReminder) }
                    }
                ) {
                    Checkbox(
                        checked = if (uiState.phone.isBlank()) false else uiState.smsWeeklyReminder,
                        onCheckedChange = { value -> 
                            viewModel.updateAddCustomerState { it.copy(smsWeeklyReminder = value) }
                        },
                        enabled = uiState.phone.isNotBlank()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(translate("Automatic Weekly SMS Reminder", language), fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = uiState.phone.isNotBlank()) { 
                        viewModel.updateAddCustomerState { it.copy(smsConfirmationOfEntry = !it.smsConfirmationOfEntry) }
                    }
                ) {
                    Checkbox(
                        checked = if (uiState.phone.isBlank()) false else uiState.smsConfirmationOfEntry,
                        onCheckedChange = { value -> 
                            viewModel.updateAddCustomerState { it.copy(smsConfirmationOfEntry = value) }
                        },
                        enabled = uiState.phone.isNotBlank()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(translate("Automatic Entry Confirmation SMS", language), fontSize = 13.sp)
                }
            }

        }

        Button(
            onClick = {
                val cleanPhone = uiState.phone.filter { it.isDigit() }
                val cleanPhone2 = uiState.phone2.filter { it.isDigit() }
                val nameBlank = uiState.name.isBlank()
                var errorPhone: String? = null
                var errorPhone2: String? = null
                
                if (uiState.phone.isNotBlank() && cleanPhone.length != 10 && cleanPhone.length != 12) {
                    errorPhone = "Phone must be exactly 10 or 12 digits."
                }
                if (uiState.phone2.isNotBlank() && cleanPhone2.length != 10 && cleanPhone2.length != 12) {
                    errorPhone2 = "Phone must be exactly 10 or 12 digits."
                }

                if (nameBlank || errorPhone != null || errorPhone2 != null) {
                    viewModel.updateAddCustomerState {
                        it.copy(
                            isNameError = nameBlank,
                            phoneErrorText = errorPhone,
                            phone2ErrorText = errorPhone2
                        )
                    }
                } else {
                    viewModel.createCustomer(
                        name = uiState.name.trim(),
                        phone = if (uiState.phone.isBlank()) "" else cleanPhone,
                        phone2 = if (uiState.phone2.isBlank()) "" else cleanPhone2,
                        collectionDay = uiState.collectionDay,
                        city = uiState.city.trim(),
                        smsWeeklyReminder = if (uiState.phone.isBlank()) false else uiState.smsWeeklyReminder,
                        smsConfirmationOfEntry = if (uiState.phone.isBlank()) false else uiState.smsConfirmationOfEntry,
                        autoWeeklySms = if (uiState.phone.isBlank()) false else uiState.smsWeeklyReminder,
                        autoWeeklyWhatsapp = false,
                        upiNameAlias = "",
                        preferredLanguage = uiState.preferredLanguage
                    )
                    viewModel.navigateBack()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = appColors.primaryAccent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("save_customer_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(translate("Save", language), color = Color.White)
        }
    }
}
