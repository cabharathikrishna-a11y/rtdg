package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.*
import com.example.network.FirebaseUpdateManager
import com.example.network.UpdateStatus
import com.example.data.DatabaseProvider
import com.example.util.CsvBackupHelper
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. LanguageSubPage
// ==========================================
@Composable
fun LanguageSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = translate("Select which language of display you want to apply globally.", language),
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val langs = listOf(
                    "English" to "English (Default)",
                    "Tamil" to "தமிழ் (Tamil)",
                    "Hindi" to "हिन्दी (Hindi)",
                    "Telugu" to "తెలుగు (Telugu)"
                )

                langs.forEach { (code, label) ->
                    val isSel = language == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSel) ColorSlateDark.copy(alpha = 0.05f) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSel) ColorSlateDark else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setLanguage(code) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ColorSlateDark
                        )

                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.setLanguage(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = ColorSlateDark)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. FontSizeSubPage
// ==========================================
@Composable
fun FontSizeSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    fontSizeScale: Float,
    fontStyleKey: String = "SANS_SERIF"
) {
    val isDark = appColors.isDark
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else ColorSlateDark
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.DarkGray
    val activeFontFamily = com.example.ui.theme.getAppFontFamily(fontStyleKey)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Section 1: Font Style Choice
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = translate("Font Style (Typeface Family)", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = appColors.primaryAccent
                )
                Text(
                    text = translate("Select an in-built font family style to apply across all app screens, titles, ledgers and receipts.", language),
                    fontSize = 11.sp,
                    color = subTextColor
                )

                val fontStyles = listOf(
                    "SANS_SERIF" to ("Sans-Serif (Standard & Clean)" to "Clean, modern high-legibility geometric font"),
                    "SERIF" to ("Serif (Classic & Editorial)" to "Traditional, elegant serif letterforms"),
                    "MONOSPACE" to ("Monospace (Digital & Ledger)" to "Fixed-width digits for crisp accounting figures"),
                    "CURSIVE" to ("Cursive (Stylish Script)" to "Flowing decorative display typography"),
                    "DEFAULT" to ("System Default" to "Native Android OS default system typeface")
                )

                fontStyles.forEach { (key, pair) ->
                    val (label, desc) = pair
                    val isSel = fontStyleKey.equals(key, ignoreCase = true)
                    val styleFontFamily = com.example.ui.theme.getAppFontFamily(key)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSel) appColors.primaryAccent.copy(alpha = 0.08f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) appColors.primaryAccent else cardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setFontStyleKey(key) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = translate(label, language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = styleFontFamily,
                                color = textColor
                            )
                            Text(
                                text = "Sample: MD Finance ₹1,25,000 — $desc",
                                fontSize = 10.sp,
                                fontFamily = styleFontFamily,
                                color = subTextColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.setFontStyleKey(key) },
                            colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                        )
                    }
                }
            }
        }

        // Section 2: Font Size Scale
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = translate("Font Size Scale", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = appColors.primaryAccent
                )
                Text(
                    text = translate("Select text typography scale setting to fit comfortably on small screens.", language),
                    fontSize = 11.sp,
                    color = subTextColor
                )

                val sizes = listOf(
                    "Tiny" to ("Tiny / Extra Compact (0.75x)" to 0.75f),
                    "Compact" to ("Small / Compact (0.90x)" to 0.90f),
                    "Regular" to ("Regular / Comfort (1.00x)" to 1.00f),
                    "Normal" to ("Normal / Standard (1.15x)" to 1.15f),
                    "Medium" to ("Medium / Readable (1.25x)" to 1.25f),
                    "Large" to ("Large / Enlarged (1.35x)" to 1.35f),
                    "Extra Large" to ("Extra Large / XL (1.50x)" to 1.50f),
                    "Huge" to ("Huge / High Visibility (1.65x)" to 1.65f),
                    "Gigantic" to ("Gigantic / Maximum Zoom (1.80x)" to 1.80f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { (_, pair) ->
                        val (label, targetScale) = pair
                        val isSel = kotlin.math.abs(fontSizeScale - targetScale) < 0.051f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSel) appColors.primaryAccent.copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) appColors.primaryAccent else cardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setFontSizeScale(targetScale) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = translate(label, language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = activeFontFamily,
                                color = textColor
                            )

                            RadioButton(
                                selected = isSel,
                                onClick = { viewModel.setFontSizeScale(targetScale) },
                                colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Live Preview Across Display Places
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = translate("Typography Live Preview Across Display Places", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = appColors.primaryAccent
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Display Place 1: App Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = translate("Header Title", language),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Text(
                                text = "MD Finance App",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                fontFamily = activeFontFamily,
                                color = textColor
                            )
                        }

                        HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

                        // Display Place 2: Financial Currency Metric
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = translate("Dashboard Collection", language),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Text(
                                text = "₹ 1,25,000.00",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = activeFontFamily,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                            )
                        }

                        HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

                        // Display Place 3: Borrower List Item
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = translate("Customer List Item", language),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Text(
                                text = "S. Murugan (Route #101)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = activeFontFamily,
                                color = textColor
                            )
                            Text(
                                text = translate("Week 12 • Paid ₹1,000", language),
                                fontSize = 11.sp,
                                fontFamily = activeFontFamily,
                                color = subTextColor
                            )
                        }

                        HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

                        // Display Place 4: Badges & Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE LOAN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = activeFontFamily,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = translate("Collect Instalment", language),
                                    fontSize = 11.sp,
                                    fontFamily = activeFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ResetSubPage
// ==========================================
@Composable
fun ResetSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    context: Context
) {
    var showDiag by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = translate("Emergency ledger control resets. This resets local databases to clear space.", language),
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            Button(
                onClick = { showDiag = true },
                colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = translate("RESET LOCAL LEDGER DATABASE", language),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showDiag) {
        AlertDialog(
            onDismissRequest = { showDiag = false },
            title = {
                Text(
                    text = translate("⚠️ Absolute Reset Warning", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ColorLossRed
                )
            },
            text = {
                Text(
                    text = translate("Are you absolutely sure you want to completely erase the local database? All your clients, loan entries and weekly transaction logs will be permanently deleted from this device. Please create a CSV backup first.", language),
                    fontSize = 12.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiag = false
                        viewModel.clearAllLocalData(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, translate("All data successfully deleted!", language), Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "${translate("Delete failed:", language)} $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLossRed)
                ) {
                    Text(translate("Wipe All Data", language), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDiag = false },
                    border = BorderStroke(1.dp, ColorSlateDark)
                ) {
                    Text(translate("Cancel", language), color = ColorSlateDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ==========================================
// 4. SIMSubPage
// ==========================================
@Composable
fun SIMSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    simSelection: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = translate("Select which SIM Card is default on your physical Android slot for delivery reminders.", language),
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val slots = listOf(
                    "Ask Always" to "Default Slot (Ask Always)",
                    "SIM 1" to "SIM card Slot 1",
                    "SIM 2" to "SIM card Slot 2"
                )

                slots.forEach { (slotVal, label) ->
                    val isSel = simSelection == slotVal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSel) ColorSlateDark.copy(alpha = 0.05f) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSel) ColorSlateDark else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setSimSelection(slotVal) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = translate(label, language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ColorSlateDark
                        )

                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.setSimSelection(slotVal) },
                            colors = RadioButtonDefaults.colors(selectedColor = ColorSlateDark)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. BusinessUpiSubPage
// ==========================================
@Composable
fun BusinessUpiSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    businessName: String,
    upiId: String,
    upiLink: String,
    qrImageUri: String,
    qrPickerLauncher: ActivityResultLauncher<String>
) {
    val statementCustomizationCode by viewModel.statementCustomizationCode.collectAsStateWithLifecycle()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = translate("Customize billing identities & business notification footer settings.", language),
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            // Business Name
            OutlinedTextField(
                value = businessName,
                onValueChange = { viewModel.setBusinessName(it) },
                label = { Text("Business Name Signal Title (SMS footer)", fontSize = 11.sp) },
                placeholder = { Text("Example: Muneeswaran Finance", fontSize = 11.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedBorderColor = ColorSlateDark,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            // Merchant UPI ID
            OutlinedTextField(
                value = upiId,
                onValueChange = { viewModel.setUpiId(it) },
                label = { Text("Merchant UPI ID (for QR codes and SMS formats)", fontSize = 11.sp) },
                placeholder = { Text("Example: 9440736893@ptyes", fontSize = 11.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedBorderColor = ColorSlateDark,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            Text(
                text = "Statement Customization Override",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ColorSlateDark
            )
            Text(
                text = "Override dynamic components during ledger image sharing. Enter standard key-value configuration overrides below (e.g. TITLE, FOOTER, WATERMARK, WATERMARK_SUB, COLOR_START, COLOR_END, THEME_BORDER_COLOR).",
                fontSize = 11.sp,
                color = Color.Gray
            )

            OutlinedTextField(
                value = statementCustomizationCode,
                onValueChange = { viewModel.setStatementCustomizationCode(it) },
                label = { Text("Customization Configuration Code", fontSize = 11.sp) },
                placeholder = { Text("TITLE=COLLECTION STATEMENT REPORT\nFOOTER=Please retain this statement for your verification.\nCOLOR_START=#0F172A\nCOLOR_END=#1E1B4B\nTHEME_BORDER_COLOR=#4F46E5", fontSize = 11.sp) },
                minLines = 4,
                maxLines = 8,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedBorderColor = LocalAppThemeColors.current.darkBg,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ==========================================
// 6. CollectionGroupsSubPage
// ==========================================
@Composable
fun CollectionGroupsSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    collectionGroups: List<String>,
    customGroupName: String,
    onCustomGroupNameChange: (String) -> Unit,
    onRenameClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = translate("Configure active route days or groups for collection cycles.", language),
                fontSize = 11.sp,
                color = Color.DarkGray
            )

            // Add Group Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customGroupName,
                    onValueChange = onCustomGroupNameChange,
                    placeholder = { Text("Add Group Name e.g. Tuesday / செவ்வாய்", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = ColorSlateDark,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val trimmed = customGroupName.trim()
                        if (trimmed.isNotBlank()) {
                            if (collectionGroups.contains(trimmed)) {
                                // Already contains
                            } else {
                                val newList = collectionGroups.toMutableList().apply { add(trimmed) }
                                viewModel.updateCollectionGroups(newList)
                                onCustomGroupNameChange("")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Reorder and list area
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                collectionGroups.forEachIndexed { index, group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = group,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = ColorSlateDark,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Move Up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newList = collectionGroups.toMutableList().apply {
                                            val temp = get(index)
                                            set(index, get(index - 1))
                                            set(index - 1, temp)
                                        }
                                        viewModel.updateCollectionGroups(newList)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) ColorSlateDark else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Move Down
                            IconButton(
                                onClick = {
                                    if (index < collectionGroups.size - 1) {
                                        val newList = collectionGroups.toMutableList().apply {
                                            val temp = get(index)
                                            set(index, get(index + 1))
                                            set(index + 1, temp)
                                        }
                                        viewModel.updateCollectionGroups(newList)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (index < collectionGroups.size - 1) ColorSlateDark else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Rename
                            IconButton(
                                onClick = { onRenameClick(group) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename group",
                                    tint = ColorSlateDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete
                            IconButton(
                                onClick = { onDeleteClick(group) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete group",
                                    tint = ColorLossRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SystemUpdateSubPage
// ==========================================
@Composable
fun SystemUpdateSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    context: Context
) {
    val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsStateWithLifecycle()
    val forceUpdateEnabled by viewModel.forceUpdateEnabled.collectAsStateWithLifecycle()
    val pauseUpdatesEnabled by viewModel.pauseUpdatesEnabled.collectAsStateWithLifecycle()

    // Observe AppUpdateManager update states
    val updateStatus by FirebaseUpdateManager.updateStatus.collectAsStateWithLifecycle()
    val latestVersionCodeState by FirebaseUpdateManager.latestVersionCode.collectAsStateWithLifecycle()
    val updateErrorState by FirebaseUpdateManager.updateError.collectAsStateWithLifecycle()
    val latestVersionCode = latestVersionCodeState
    val latestVersionName = "Build $latestVersionCode" 
    val updateError = updateErrorState

    val currentVersionCode = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            pInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        1L
    }

    val currentVersionName = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Version Info & Update Status Box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("version_metadata_box")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = translate("Application Version Details", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = translate("Current details of the installed application package", language),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = appColors.primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Grid layout for Version Code & Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Version Name box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = translate("Version Name", language),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentVersionName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.primaryAccent
                            )
                        }
                    }

                    // Version Code box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = translate("Version Code", language),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "#$currentVersionCode",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.primaryAccent
                            )
                        }
                    }
                }

                // Dynamic Live Update Status Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    when (updateStatus) {
                        UpdateStatus.IDLE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = translate("Standby / Idle", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        UpdateStatus.CHECKING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = appColors.primaryAccent,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = translate("Checking for new update on Cloud...", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.primaryAccent
                                )
                            }
                        }
                        UpdateStatus.DOWNLOADING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = translate("Downloading update version v$latestVersionCode... Please wait.", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                        UpdateStatus.DOWNLOADED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = translate("New update fully downloaded & ready!", language),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Button(
                                    onClick = { FirebaseUpdateManager.triggerInstall(context, latestVersionCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = translate("INSTALL NOW", language),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        UpdateStatus.UPDATE_AVAILABLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = translate("New Version v$latestVersionCode is available!", language),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                        UpdateStatus.UP_TO_DATE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = translate("App is fully up-to-date (v$currentVersionCode)", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                        UpdateStatus.FAILED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = translate("Error checking/downloading updates.", language),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Red
                                    )
                                }
                                if (!updateError.isNullOrEmpty()) {
                                    Text(
                                        text = updateError,
                                        fontSize = 11.sp,
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Check Button Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = translate("Check for Updates Manually", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = translate("Instantly query Firebase RTDB for any newer APK releases and start automated installation with secure rollback backup.", language),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Button(
                    onClick = {
                        Toast.makeText(context, "Contacting Firebase Realtime Database...", Toast.LENGTH_SHORT).show()
                        FirebaseUpdateManager.checkForCloudUpdates(context, manualCheck = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("check_updates_manual_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Check",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate("CHECK FOR UPDATES NOW", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
                
                Button(
                    onClick = {
                        FirebaseUpdateManager.deleteDownloadedUpdate(context)
                        FirebaseUpdateManager.checkForCloudUpdates(context, manualCheck = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate("DELETE DOWNLOAD & RE-CHECK", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Settings Toggles Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = translate("OTA Auto-Update Parameters", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Toggle 1: Auto-Update Enabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translate("Auto Download & Install", language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = translate("Periodically checks and initiates background download when updates exist.", language),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = autoUpdateEnabled,
                        onCheckedChange = { viewModel.setAutoUpdateEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = appColors.primaryAccent
                        ),
                        modifier = Modifier.testTag("auto_update_enabled_switch")
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Toggle 2: Force Updates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translate("Force Updates", language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = translate("Aggressively request installation immediately upon new release detection.", language),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = forceUpdateEnabled,
                        onCheckedChange = { viewModel.setForceUpdateEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = appColors.primaryAccent
                        ),
                        modifier = Modifier.testTag("force_update_enabled_switch")
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Toggle 3: Pause All Updates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translate("Pause All Updates", language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = translate("Temporarily halt background checks and automated OTA downloads.", language),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = pauseUpdatesEnabled,
                        onCheckedChange = { viewModel.setPauseUpdatesEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = appColors.primaryAccent
                        ),
                        modifier = Modifier.testTag("pause_updates_enabled_switch")
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. GoogleContactsSyncSubPage
// ==========================================
@Composable
fun GoogleContactsSyncSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    context: Context
) {
    val syncEnabled by viewModel.googleContactsSyncEnabled.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.googleContactsSelectedAccount.collectAsStateWithLifecycle()
    val accountsList by viewModel.googleContactsAccountsList.collectAsStateWithLifecycle()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val writeOk = permissions[Manifest.permission.WRITE_CONTACTS] == true
        val readOk = permissions[Manifest.permission.READ_CONTACTS] == true
        permissionGranted = writeOk && readOk
        if (permissionGranted) {
            viewModel.fetchGoogleAccounts()
            Toast.makeText(context, "Contacts permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissions denied. Cannot sync contacts.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) {
            viewModel.fetchGoogleAccounts()
        }
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                viewModel.setGoogleContactsSelectedAccount(accountName)
                viewModel.setGoogleContactsSyncEnabled(true)
                Toast.makeText(context, "Account selected: $accountName. Contacts sync enabled!", Toast.LENGTH_LONG).show()
            }
        }
    }

    val triggerAccountPicker = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.accounts.AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    arrayOf("com.google"),
                    null,
                    null,
                    null,
                    null
                )
            } else {
                @Suppress("DEPRECATION")
                android.accounts.AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    arrayOf("com.google"),
                    false,
                    null,
                    null,
                    null,
                    null
                )
            }
            accountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening Google account picker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    var showDropdown by remember { mutableStateOf(false) }
    var manualAccountEmail by remember { mutableStateOf(selectedAccount) }

    // Sync progress tracking states
    var isSyncingAll by remember { mutableStateOf(false) }
    var syncProgressCurrent by remember { mutableStateOf(0) }
    var syncProgressTotal by remember { mutableStateOf(0) }
    var syncResultMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (permissionGranted) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (permissionGranted) Color(0xFFBBF7D0) else Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (permissionGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = "Permission Status",
                    tint = if (permissionGranted) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (permissionGranted) translate("Contacts Permission Active", language) else translate("Contacts Permission Required", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (permissionGranted) Color(0xFF15803D) else Color(0xFF991B1B)
                    )
                    Text(
                        text = if (permissionGranted) translate("The app has secure system access to update your contact books.", language) else translate("Please grant read/write permissions to synchronize client details directly to your account contacts.", language),
                        fontSize = 12.sp,
                        color = if (permissionGranted) Color(0xFF166534) else Color(0xFF7F1D1D)
                    )
                }
                if (!permissionGranted) {
                    Button(
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("request_contacts_permission_btn")
                    ) {
                        Text(
                            text = translate("GRANT", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Feature Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translate("Enable Contacts Sync", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = translate("When enabled, creating or editing client mobile numbers automatically pushes correct details to Google Contacts.", language),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = {
                            if (!permissionGranted && it) {
                                Toast.makeText(context, "Grant permissions first to enable syncing.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.setGoogleContactsSyncEnabled(it)
                                if (it && selectedAccount.isBlank()) {
                                    triggerAccountPicker()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = appColors.primaryAccent
                        ),
                        modifier = Modifier.testTag("contacts_sync_enabled_switch")
                    )
                }

                if (syncEnabled) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Text(
                        text = translate("Google Sync Account", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedAccount.isNotBlank()) selectedAccount else translate("No Google Account Selected", language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAccount.isNotBlank()) appColors.primaryAccent else Color.Gray
                            )

                            Button(
                                onClick = { triggerAccountPicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_google_account_popup_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPage,
                                    contentDescription = "Google Accounts Popup",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = translate("CHOOSE GOOGLE ACCOUNT VIA POP-UP", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Manual Email input fallback if needed
                    OutlinedTextField(
                        value = manualAccountEmail,
                        onValueChange = {
                            manualAccountEmail = it
                            viewModel.setGoogleContactsSelectedAccount(it)
                        },
                        label = { Text(translate("Or Enter Google Account Manually", language)) },
                        placeholder = { Text("e.g. name@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("google_account_manual_input")
                    )
                }
            }
        }

        // Full Sync Command Card
        if (syncEnabled && selectedAccount.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = translate("Manual Full Database Sync", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Text(
                        text = translate("Sync all existing clients with active phone numbers directly to Google Contacts now. This resolves any unsynced names or newly updated telephone entries.", language),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (isSyncingAll) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val progress = if (syncProgressTotal > 0) syncProgressCurrent.toFloat() / syncProgressTotal else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = appColors.primaryAccent,
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "Syncing contact $syncProgressCurrent of $syncProgressTotal...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.primaryAccent
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isSyncingAll = true
                                syncResultMessage = null
                                viewModel.syncAllBorrowersToGoogleContacts(
                                    onProgress = { current, total ->
                                        syncProgressCurrent = current
                                        syncProgressTotal = total
                                    },
                                    onComplete = { success, msg ->
                                        isSyncingAll = false
                                        syncResultMessage = msg
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sync_all_contacts_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Now",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = translate("SYNC ALL BORROWERS NOW", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    syncResultMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (msg.contains("success", ignoreCase = true) || msg.contains("synced", ignoreCase = true)) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. TemplatesSubPage
// ==========================================
// ==========================================
// Custom visual transformation and text field for wildcard bubbles
// ==========================================
class BubbleVisualTransformation(private val appColors: AppThemeColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        builder.append(text.text)
        
        val wildcards = listOf("{customer}", "{name}", "{amount}", "{business}", "{upi}", "{upi_link}", "{balance}", "{inst_amt}", "{date}")
        val isDark = appColors.isDark
        val bubbleBg = if (isDark) Color(0xFF312E81) else Color(0xFFE0E7FF)
        val bubbleText = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4F46E5)
        
        for (wildcard in wildcards) {
            var index = text.text.indexOf(wildcard)
            while (index != -1) {
                builder.addStyle(
                    style = SpanStyle(
                        color = bubbleText,
                        background = bubbleBg,
                        fontWeight = FontWeight.Bold
                    ),
                    start = index,
                    end = index + wildcard.length
                )
                index = text.text.indexOf(wildcard, index + 1)
            }
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun BubbleTemplatesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    appColors: AppThemeColors,
    textColor: Color,
    cardBorder: Color
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }

    LaunchedEffect(value) {
        if (textFieldValueState.text != value) {
            textFieldValueState = textFieldValueState.copy(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            textFieldValueState = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedLabelColor = textColor,
            unfocusedLabelColor = textColor,
            focusedPlaceholderColor = Color.DarkGray,
            unfocusedPlaceholderColor = Color.DarkGray,
            focusedBorderColor = appColors.primaryAccent,
            unfocusedBorderColor = cardBorder
        ),
        visualTransformation = remember(appColors) { BubbleVisualTransformation(appColors) },
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Backspace || event.nativeKeyEvent.keyCode == 67)) {
                    val selection = textFieldValueState.selection
                    if (selection.collapsed) {
                        val cursor = selection.start
                        val text = textFieldValueState.text
                        val wildcards = listOf("{customer}", "{name}", "{amount}", "{business}", "{upi}", "{upi_link}", "{balance}", "{inst_amt}", "{date}")
                        for (wildcard in wildcards) {
                            var index = text.indexOf(wildcard)
                            while (index != -1) {
                                val start = index
                                val end = index + wildcard.length
                                if (cursor in (start + 1)..end) {
                                    val newText = text.substring(0, start) + text.substring(end)
                                    val newSelection = TextRange(start)
                                    textFieldValueState = TextFieldValue(text = newText, selection = newSelection)
                                    onValueChange(newText)
                                    return@onPreviewKeyEvent true
                                }
                                index = text.indexOf(wildcard, index + 1)
                            }
                        }
                    }
                }
                false
            },
        maxLines = 4
    )
}

// ==========================================
// 9. TemplatesSubPage
// ==========================================
@Composable
fun TemplatesSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    smsPaused: Boolean,
    currentTemplateLang: String,
    smsNewLoanTemplate: String,
    smsPaymentTemplate: String,
    smsReminderTemplate: String,
    whatsappReminderTemplate: String
) {
    var showCustomerSmsSettings by remember { mutableStateOf(false) }
    val collectionGroups by viewModel.collectionGroups.collectAsStateWithLifecycle()
    val daysList = remember(collectionGroups) { 
        getOrderedDaysList(collectionGroups).filter { 
            !it.equals("Friday", ignoreCase = true) && !it.equals("Home", ignoreCase = true) 
        } 
    }
    var selectedSmsDay by remember { mutableStateOf("") }

    LaunchedEffect(daysList) {
        if (selectedSmsDay.isBlank() && daysList.isNotEmpty()) {
            selectedSmsDay = daysList.first()
        }
    }

    val isDark = appColors.isDark
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else ColorSlateDark
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.DarkGray

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (showCustomerSmsSettings) {
                // Header of Customer-wise SMS Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { showCustomerSmsSettings = false }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate("Customer Wise SMS Settings", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }

                HorizontalDivider(color = cardBorder)

                // Day Selection Tab Row (Horizontal Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    daysList.forEach { day ->
                        val isActive = day.trim().equals(selectedSmsDay.trim(), ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isActive) appColors.primaryAccent else (if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) appColors.primaryAccent else cardBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedSmsDay = day }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = translate(day, language).uppercase(java.util.Locale.getDefault()),
                                color = if (isActive) Color.White else (if (isDark) Color.White else Color.Black),
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Customer list below
                val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
                val filteredCustomers = remember(allCustomers, selectedSmsDay) {
                    allCustomers.filter { 
                        it.collectionDay.trim().equals(selectedSmsDay.trim(), ignoreCase = true) &&
                        it.phone.trim().isNotEmpty()
                    }.sortedBy { it.customOrder }
                }

                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = translate("No customers registered for this day.", language),
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredCustomers.forEach { customer ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                             text = "${customer.customOrder}. ${customer.name}",
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 14.sp,
                                             color = textColor,
                                             maxLines = 1,
                                             overflow = TextOverflow.Ellipsis,
                                             modifier = Modifier.weight(1f)
                                        )
                                        if (!customer.city.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = customer.city,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF334155),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateCustomerSmsSettings(
                                                        customer,
                                                        smsWeekly = !customer.smsWeeklyReminder,
                                                        smsConfirmation = customer.smsConfirmationOfEntry
                                                    )
                                                }
                                        ) {
                                            Checkbox(
                                                checked = customer.smsWeeklyReminder,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateCustomerSmsSettings(
                                                        customer,
                                                        smsWeekly = isChecked,
                                                        smsConfirmation = customer.smsConfirmationOfEntry
                                                    )
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = appColors.primaryAccent)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = translate("Weekly Reminder", language),
                                                fontSize = 11.sp,
                                                color = subTextColor
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateCustomerSmsSettings(
                                                        customer,
                                                        smsWeekly = customer.smsWeeklyReminder,
                                                        smsConfirmation = !customer.smsConfirmationOfEntry
                                                    )
                                                }
                                        ) {
                                            Checkbox(
                                                checked = customer.smsConfirmationOfEntry,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateCustomerSmsSettings(
                                                        customer,
                                                        smsWeekly = customer.smsWeeklyReminder,
                                                        smsConfirmation = isChecked
                                                    )
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = appColors.primaryAccent)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = translate("Entry Confirmation", language),
                                                fontSize = 11.sp,
                                                color = subTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Outbound SMS Controls Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = translate("Outbound SMS Sending", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (smsPaused) ColorLossRed else textColor
                        )
                        Text(
                            text = translate("Pause or enable outbound confirmation and draft SMS.", language),
                            fontSize = 10.sp,
                            color = subTextColor
                        )
                    }

                    Switch(
                        checked = !smsPaused,
                        onCheckedChange = { viewModel.setSmsPaused(!it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = appColors.primaryAccent,
                            checkedTrackColor = appColors.primaryAccent.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.LightGray.copy(alpha = 0.4f)
                        )
                    )
                }

                // Customer Wise SMS Settings Button
                Button(
                    onClick = { showCustomerSmsSettings = true },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customer Wise SMS Settings",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = translate("Customer Wise SMS Settings", language),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(color = cardBorder)

                Text(
                     text = translate("Configure custom templates for SMS/WhatsApp confirmations and alerts. Use the dynamic wildcards listed below. They will be autoreplaced when sending:", language),
                     fontSize = 11.sp,
                     color = subTextColor
                )

                // List variables
                val variables = listOf(
                    "{customer}" to "Client's registration name ({name} is also supported)",
                    "{amount}" to "Active payment / transaction amount",
                    "{business}" to "My custom businesssignature tag",
                    "{upi}" to "Configured business merchant UPI ID",
                    "{upi_link}" to "Direct UPI pay link (QR Link / URI)",
                    "{balance}" to "Remaining outstanding balance",
                    "{inst_amt}" to "Standard collection installment amount",
                    "{date}" to "Current formatted entry timestamp"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    variables.forEach { (token, label) ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = translate(label, language),
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                            Text(
                                text = token,
                                color = appColors.primaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                HorizontalDivider(color = cardBorder)

                Text(
                    text = translate("Select Language for Custom Templates:", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    val languages = listOf("English", "Tamil", "Hindi", "Telugu")
                    languages.forEach { langItem ->
                        val isSelected = langItem == currentTemplateLang
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) appColors.primaryAccent else (if (isDark) Color(0xFF0F172A) else Color.White),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) appColors.primaryAccent else cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setTemplateLanguage(langItem) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = translate(langItem, language),
                                color = if (isSelected) Color.White else textColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = cardBorder)

                // New Loan Custom Area
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("1. New Loan Confirmation SMS Template", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    BubbleTemplatesTextField(
                        value = smsNewLoanTemplate,
                        onValueChange = { viewModel.setSmsNewLoanTemplate(it) },
                        appColors = appColors,
                        textColor = textColor,
                        cardBorder = cardBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Confirmation Custom Area
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("2. Entry Confirmation SMS Template", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    BubbleTemplatesTextField(
                        value = smsPaymentTemplate,
                        onValueChange = { viewModel.setSmsPaymentTemplate(it) },
                        appColors = appColors,
                        textColor = textColor,
                        cardBorder = cardBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SMS Reminder Custom Area
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("3. Weekly SMS Reminder Template", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    BubbleTemplatesTextField(
                        value = smsReminderTemplate,
                        onValueChange = { viewModel.setSmsReminderTemplate(it) },
                        appColors = appColors,
                        textColor = textColor,
                        cardBorder = cardBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Whatsapp Reminder Custom Area
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("4. Weekly WhatsApp Reminder Template", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    BubbleTemplatesTextField(
                        value = whatsappReminderTemplate,
                        onValueChange = { viewModel.setWhatsappReminderTemplate(it) },
                        appColors = appColors,
                        textColor = textColor,
                        cardBorder = cardBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ==========================================
// 10. BackupSubPage
// ==========================================
@Composable
fun BackupSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    collectionGroups: List<String>,
    context: Context,
    currentUserRole: String = "ADMIN"
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var groupToImportInto by rememberSaveable { mutableStateOf<String?>(null) }

    val msgAllDataDeleted = translate("All data successfully deleted!", language)
    val msgDeleteFailed = translate("Delete failed:", language)
    val msgSuccessTail = translate("successfully imported!", language)
    val msgImportFailedHead = translate("Import failed:", language)
    val msgAllDaysLedger = translate("All days ledger", language)

    // CSV File picker launcher
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val targetGroup = groupToImportInto
        if (uri != null && targetGroup != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val csvTextContent = String(bytes, StandardCharsets.UTF_8).trim()
                    viewModel.importCsvGroupBackup(
                        context = context,
                        csvTextContent = csvTextContent,
                        groupName = targetGroup,
                        onSuccess = {
                            val displayGroupName = if (targetGroup == "ALL") msgAllDaysLedger else targetGroup
                            Toast.makeText(context, "$displayGroupName $msgSuccessTail", Toast.LENGTH_LONG).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "$msgImportFailedHead $err", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "$msgImportFailedHead Could not open selected file content stream.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "$msgImportFailedHead ${e.localizedMessage ?: e.toString()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    text = translate("⚠️ Delete All Local Data?", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = translate("WARNING: This will completely delete all clients, loan records, edits, and transaction histories from this local device. This is irreversible. You can import your saved backups later to restore.", language),
                    fontSize = 12.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.clearAllLocalData(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, msgAllDataDeleted, Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "$msgDeleteFailed $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text(translate("Delete All Data", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllDialog = false },
                    border = BorderStroke(1.dp, ColorSlateDark)
                ) {
                    Text(translate("Cancel", language), color = ColorSlateDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Dynamic sheets fix
    val defaultGroups = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Saturday", "Sunday mrg", "Sunday eve")
    val dynamicGroups = remember(collectionGroups) {
        val sanitizedIncoming = collectionGroups.map { it.trim() }
            .filter { 
                !it.equals("Friday", ignoreCase = true) && 
                !it.equals("Sunday", ignoreCase = true) &&
                !it.equals("Sunday Morning", ignoreCase = true) && 
                !it.equals("Sunday Evening", ignoreCase = true) 
            }
        
        // Append default groups first to safeguard structural layout precedence rules
        (defaultGroups + sanitizedIncoming).distinctBy { it.lowercase(Locale.US) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Banner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                     imageVector = Icons.Default.CloudOff,
                     contentDescription = "Offline Secure Icon",
                     tint = appColors.primaryAccent,
                     modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Fully Offline Local Backup Engine", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorSlateDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = translate("All data is loaded locally using standard CSV formats. No public clouds, databases or external URLs are connected to your ledger data.", language),
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Universal Full Backup & Restore Card
        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.primaryAccent.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, appColors.primaryAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = translate("Universal Full Ledger CSV Backup", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )

                Text(
                    text = if (currentUserRole == "USER") 
                        translate("Export all active days' customers, loans, and weekly payments in a single unified CSV spreadsheet template cleanly.", language)
                    else 
                        translate("Export or import all active days' customers, loans, and weekly payments in a single unified CSV spreadsheet template cleanly.", language),
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export ALL CSV Button
                    OutlinedButton(
                        onClick = { viewModel.exportCsvGroupBackup(context, "ALL") },
                        border = BorderStroke(1.5.dp, appColors.primaryAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = if (currentUserRole == "USER") Modifier.fillMaxWidth() else Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export All icon",
                            tint = appColors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = translate("Export All Days", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = appColors.primaryAccent
                        )
                    }

                    if (currentUserRole != "USER") {
                        // Import ALL CSV Button
                        Button(
                            onClick = {
                                groupToImportInto = "ALL"
                                csvPickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import All icon",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = translate("Import All Days", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Firebase Cloud Sync Card
        val syncPaused by viewModel.syncPaused.collectAsStateWithLifecycle()
        val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = translate("Firebase Real-time Cloud Sync", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )

                Text(
                    text = translate("Synchronize your ledger entries, customers, payments, and audits instantly with other devices connected to your Firebase Realtime Database cloud.", language),
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = translate("Cloud Integration Status", language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = ColorSlateDark
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val dotColor = when {
                                syncPaused -> Color.Gray
                                firebaseSyncStatus.contains("Error", ignoreCase = true) || firebaseSyncStatus.contains("Failed", ignoreCase = true) -> Color.Red
                                firebaseSyncStatus.contains("Synced", ignoreCase = true) -> Color(0xFF10B981)
                                else -> Color(0xFFF59E0B)
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color = dotColor, shape = CircleShape)
                            )
                            val statusTextColor = when {
                                syncPaused -> Color.Gray
                                firebaseSyncStatus.contains("Error", ignoreCase = true) || firebaseSyncStatus.contains("Failed", ignoreCase = true) -> Color.Red
                                firebaseSyncStatus.contains("Synced", ignoreCase = true) -> Color(0xFF047857)
                                else -> Color(0xFFB45309)
                            }
                            Text(
                                text = translate(firebaseSyncStatus, language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        }
                    }

                    Switch(
                        checked = !syncPaused,
                        onCheckedChange = { isChecked ->
                            viewModel.setSyncPaused(!isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = appColors.primaryAccent,
                            checkedTrackColor = appColors.primaryAccent.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("firebase_sync_pause_resume_switch")
                    )
                }

                if (!syncPaused) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Manual Synchronize Now
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (currentUserRole == "USER") Arrangement.Center else Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.startFirebaseSyncListening() },
                            border = BorderStroke(1.5.dp, appColors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = if (currentUserRole == "USER") Modifier.fillMaxWidth() else Modifier.weight(1f)
                        ) {
                            Text(
                                text = translate("Fetch from Cloud", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = appColors.primaryAccent
                            )
                        }

                        if (currentUserRole != "USER") {
                            Button(
                                onClick = { viewModel.forceUploadToFirebaseCloud() },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = translate("Upload to Cloud", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 11. AutomationSubPage
// ==========================================
@Composable
fun AutomationSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    smsPaused: Boolean,
    smsReaderPaused: Boolean,
    autoEntryPassing: Boolean,
    upiLinkSharing: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Outbound SMS Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translate("Outbound SMS Sending", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (smsPaused) ColorLossRed else ColorSlateDark
                    )
                    Text(
                        text = translate("Pause or enable outbound confirmation and draft SMS.", language),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Switch(
                    checked = !smsPaused,
                    onCheckedChange = { viewModel.setSmsPaused(!it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ColorSlateDark, checkedTrackColor = ColorSlateDark.copy(alpha = 0.4f))
                )
            }
        }
    }
}
