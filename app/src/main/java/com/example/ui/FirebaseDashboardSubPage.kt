package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.FirebaseAnalyticsManager
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.testTag

@Composable
fun FirebaseDashboardSubPage(
    language: String,
    viewModel: FinanceViewModel,
    appColors: AppThemeColors,
    context: Context
) {
    val scrollState = rememberScrollState()

    // Remote config states
    val welcomeMessage by com.example.network.FirebaseRemoteConfigManager.welcomeMessage.collectAsStateWithLifecycle()
    val defaultInterestRate by com.example.network.FirebaseRemoteConfigManager.defaultInterestRate.collectAsStateWithLifecycle()
    val enableUpiFeatures by com.example.network.FirebaseRemoteConfigManager.enableUpiFeatures.collectAsStateWithLifecycle()
    val lastFetchTime by com.example.network.FirebaseRemoteConfigManager.lastFetchTime.collectAsStateWithLifecycle()

    // FCM States
    var fcmToken by remember { mutableStateOf("") }
    var isFetchingToken by remember { mutableStateOf(false) }

    // Analytics state
    var customEventName by remember { mutableStateOf("") }
    var lastLoggedEvent by remember { mutableStateOf<String?>(null) }

    // RTDB States
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()
    var isRtdbConnected by remember { mutableStateOf(false) }
    var rtdbPingResult by remember { mutableStateOf<String?>(null) }
    var isPingingRtdb by remember { mutableStateOf(false) }

    // Observe RTDB Socket state
    DisposableEffect(Unit) {
        val listener = com.example.network.FirebaseConnectionManager.observeRtdbConnection { connected ->
            isRtdbConnected = connected
        }
        onDispose {
            try {
                com.google.firebase.database.FirebaseDatabase.getInstance(com.example.util.SecureConfig.firebaseDatabaseUrl)
                    .getReference(".info/connected")
                    .removeEventListener(listener)
            } catch (_: Exception) {}
        }
    }

    // Load FCM Token
    LaunchedEffect(Unit) {
        isFetchingToken = true
        try {
            // First check preference cache
            var token = com.example.network.MyFirebaseMessagingService.getSavedFcmToken(context)
            if (token.isBlank()) {
                // Fetch directly from FCM SDK
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    isFetchingToken = false
                    if (task.isSuccessful) {
                        fcmToken = task.result ?: ""
                    }
                }
            } else {
                fcmToken = token
                isFetchingToken = false
            }
        } catch (e: Exception) {
            isFetchingToken = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Banner Info
        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.primaryAccent.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, appColors.primaryAccent.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Setup",
                        tint = appColors.primaryAccent
                    )
                    Text(
                        text = "Firebase Unified Operations Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text = "This dashboard houses live integrations of Firebase Analytics, In-App Messaging, Remote Config, Cloud Messaging, and Crashlytics diagnostics for real-time monitoring.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Firebase Auth & Realtime Database Connection & Diagnostics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("rtdb_connection_card")
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
                    Text(
                        text = "Firebase RTDB Connection & Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )

                    // RTDB Socket status badge
                    Surface(
                        color = if (isRtdbConnected) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isRtdbConnected) Color(0xFF16A34A) else Color(0xFFDC2626))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isRtdbConnected) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isRtdbConnected) "RTDB Connected" else "RTDB Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRtdbConnected) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                // Auth Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auth Handshake:", fontSize = 12.sp, color = Color.Gray)
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val authText = if (currentUser != null) {
                        "Active (${currentUser.uid.take(12)}...)"
                    } else {
                        "Not Authenticated"
                    }
                    Text(
                        text = authText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentUser != null) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }

                // RTDB Endpoint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RTDB Endpoint:", fontSize = 12.sp, color = Color.Gray)
                    val rtdbUrl = com.example.util.SecureConfig.firebaseDatabaseUrl.replace("https://", "").take(32) + "..."
                    Text(
                        text = rtdbUrl,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }

                // Live Sync Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ledger Sync State:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = firebaseSyncStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primaryAccent
                    )
                }

                // Ping Diagnostic Output
                rtdbPingResult?.let { pingMsg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (pingMsg.contains("Live", ignoreCase = true) || pingMsg.contains("OK", ignoreCase = true)) 
                                Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (pingMsg.contains("Live", ignoreCase = true) || pingMsg.contains("OK", ignoreCase = true)) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pingMsg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (pingMsg.contains("Live", ignoreCase = true) || pingMsg.contains("OK", ignoreCase = true)) Color(0xFF166534) else Color(0xFF991B1B),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Diagnostic Buttons Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Test Ping Button
                        Button(
                            onClick = {
                                isPingingRtdb = true
                                rtdbPingResult = "Pinging Firebase RTDB endpoint..."
                                com.example.network.FirebaseConnectionManager.testRtdbPing { success, result ->
                                    isPingingRtdb = false
                                    rtdbPingResult = result
                                    Toast.makeText(context, if (success) "RTDB Ping Passed!" else "RTDB Ping Failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isPingingRtdb,
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isPingingRtdb) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test RTDB Ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Force Auth Reconnect
                        OutlinedButton(
                            onClick = {
                                com.example.network.FirebaseConnectionManager.initializeSilentCloudConnection(
                                    onSuccess = {
                                        Toast.makeText(context, "Silent Cloud Re-auth Success!", Toast.LENGTH_SHORT).show()
                                        viewModel.startFirebaseSyncListening()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Auth Failed: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Reconnect", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconnect RTDB", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Push Ledger to RTDB
                        OutlinedButton(
                            onClick = {
                                viewModel.uploadLocalDataToFirebaseCloud()
                                Toast.makeText(context, "Pushing Ledger to Firebase RTDB...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push to RTDB", fontSize = 11.sp)
                        }

                        // Pull Ledger from RTDB
                        OutlinedButton(
                            onClick = {
                                viewModel.startFirebaseSyncListening()
                                Toast.makeText(context, "Connecting to fetch RTDB ledger...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pull from RTDB", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Day Branch & Customer Sub-Branch Hierarchy Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("day_branches_rtdb_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day Branches & Customer Sub-Branches",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Text(
                            text = "7 Active Branches",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Firebase RTDB stores data hierarchically under Day branches (excluding Friday) and Customer sub-branches with active loan and payment histories:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                // List of Day Branches
                val dayBranches = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Saturday", "Sunday mrg", "Sunday eve")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ROOT: /days & /day_branches",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primaryAccent
                    )
                    dayBranches.forEach { dayName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Day Branch",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "📁 /$dayName → /cust_{id}_{uuid} (Sub-Branch)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.uploadLocalDataToFirebaseCloud()
                        Toast.makeText(context, "Day Branches & Customer Sub-Branches pushed to RTDB!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Sync Day Branches", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Day & Customer Branches Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Firebase Remote Config Live Status
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
                    Text(
                        text = "Firebase Remote Config Params",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = {
                            com.example.network.FirebaseRemoteConfigManager.initializeAndFetch()
                            Toast.makeText(context, "Force fetched Remote Config!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Config",
                            tint = appColors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Live configuration settings updated dynamically over the air:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Welcome Prompt:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(welcomeMessage, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Interest:", fontSize = 12.sp, color = Color.DarkGray)
                        Text("$defaultInterestRate%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UPI Features Enabled:", fontSize = 12.sp, color = Color.DarkGray)
                        Text(
                            text = if (enableUpiFeatures) "YES" else "NO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enableUpiFeatures) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }

                    if (lastFetchTime > 0) {
                        val dateStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(lastFetchTime))
                        Text(
                            text = "Last sync timestamp: $dateStr",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Firebase Cloud Messaging (FCM) Card
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
                    text = "Firebase Cloud Messaging (FCM) Device Token",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )

                Text(
                    text = "Copy this registration token to push direct test notifications from your Firebase console to this physical device:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (isFetchingToken) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        color = appColors.primaryAccent
                    )
                } else if (fcmToken.isNotBlank()) {
                    OutlinedTextField(
                        value = fcmToken,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("fcm_token", fcmToken)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "FCM Token copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy FCM Token",
                                    tint = appColors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = "FCM registration token pending. Click below to regenerate.",
                        fontSize = 11.sp,
                        color = Color.Red
                    )
                }

                Button(
                    onClick = {
                        isFetchingToken = true
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            isFetchingToken = false
                            if (task.isSuccessful) {
                                fcmToken = task.result ?: ""
                                Toast.makeText(context, "FCM Token refreshed successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "FCM Token refresh failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("FORCE REFRESH DEVICE TOKEN", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Firebase Analytics & In-App Messaging Custom event logger
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
                    text = "Firebase Analytics & In-App Message Triggers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "Log custom testing events directly to Firebase Analytics. If you configure In-App messages triggered by specific event campaigns, firing them here will launch the visual overlay prompt instantly.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = customEventName,
                    onValueChange = { customEventName = it },
                    label = { Text("Custom Event Name") },
                    placeholder = { Text("e.g. user_signed_up, campaign_trigger") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_analytics_event_input")
                )

                Button(
                    onClick = {
                        if (customEventName.isNotBlank()) {
                            val cleanName = customEventName.trim().replace("\\s+".toRegex(), "_")
                            FirebaseAnalyticsManager.logEvent(cleanName)
                            lastLoggedEvent = cleanName
                            customEventName = ""
                            Toast.makeText(context, "Logged Event: $cleanName to Firebase Analytics!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Event name cannot be blank.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("log_custom_event_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "Analytics Log",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOG EVENT NOW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                lastLoggedEvent?.let { name ->
                    Text(
                        text = "Last successfully logged: \"$name\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }

        // Firebase Crashlytics Diagnostic Card
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
                    text = "Firebase Crashlytics Diagnostic Tools",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "Verify your Firebase Crashlytics installation by reporting mock exceptions or initiating a simulated crash (the app will close and report the stack trace on the next launch).",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                throw RuntimeException("Diagnostics mock exception reported to Firebase Crashlytics.")
                            } catch (e: Exception) {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
                                Toast.makeText(context, "Non-fatal reported to Crashlytics!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("log_non_fatal_crashlytics_btn")
                    ) {
                        Text("REPORT NON-FATAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (com.example.BuildConfig.DEBUG) {
                        Button(
                            onClick = {
                                // Intentionally throw uncaught exception to test real-world Crashlytics collection
                                Toast.makeText(context, "Initiating simulated fatal crash...", Toast.LENGTH_SHORT).show()
                                throw RuntimeException("Simulated App Crash: Firebase Crashlytics is successfully verified!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("trigger_fatal_crash_btn")
                        ) {
                            Text("TRIGGER CRASH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
