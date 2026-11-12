package com.example.ui

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val appLogo by viewModel.customAppLogo.collectAsStateWithLifecycle()
    val businessName by viewModel.businessName.collectAsStateWithLifecycle()

    var selectedEmail by remember { mutableStateOf<String?>(null) }
    var selectedDisplayName by remember { mutableStateOf<String?>(null) }
    var selectedIdToken by remember { mutableStateOf<String?>(null) }

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoBannerMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Fallback system account picker if GoogleSignIn throws ApiException (e.g. SHA-1 mismatch 10 / 12500)
    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = false
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            selectedEmail = accountName.trim()
            selectedDisplayName = accountName.substringBefore("@").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            selectedIdToken = null
            errorMessage = null
            infoBannerMessage = null
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            errorMessage = "Could not retrieve selected Google account. Please try again."
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            isLoading = false
            val email = account?.email ?: ""
            val displayName = account?.displayName ?: email.substringBefore("@")
            if (email.isNotBlank()) {
                selectedEmail = email
                selectedDisplayName = displayName
                selectedIdToken = account?.idToken
                errorMessage = null
                infoBannerMessage = null
            } else {
                errorMessage = "Selected Google account did not provide an email address."
            }
        } catch (e: ApiException) {
            // Even when GoogleSignIn throws status 10 / 12500 on custom keystores, inspect last signed-in account or launch OS Account Picker
            val fallbackAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (fallbackAccount?.email != null) {
                isLoading = false
                selectedEmail = fallbackAccount.email
                selectedDisplayName = fallbackAccount.displayName ?: fallbackAccount.email?.substringBefore("@")
                selectedIdToken = fallbackAccount.idToken
                errorMessage = null
                infoBannerMessage = null
            } else if (e.statusCode == 12501) {
                // User cancelled the dialog
                isLoading = false
            } else {
                // Launch native Android Google Account chooser so account selection always succeeds
                try {
                    val chooseIntent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        arrayOf("com.google"),
                        null,
                        null,
                        null,
                        null
                    )
                    accountChooserLauncher.launch(chooseIntent)
                } catch (ex: Exception) {
                    isLoading = false
                    errorMessage = "Google Sign-In error (${e.statusCode}). Please try again."
                }
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.localizedMessage ?: "Sign-in failed."
        }
    }

    fun launchGooglePicker() {
        errorMessage = null
        infoBannerMessage = null
        isLoading = true
        try {
            val webClientIdId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
            if (webClientIdId != 0) {
                val webClientId = context.getString(webClientIdId)
                if (webClientId.isNotBlank()) {
                    gsoBuilder.requestIdToken(webClientId)
                }
            }
            val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            try {
                val chooseIntent = AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    arrayOf("com.google"),
                    null,
                    null,
                    null,
                    null
                )
                accountChooserLauncher.launch(chooseIntent)
            } catch (ex: Exception) {
                isLoading = false
                errorMessage = "Failed to open Google account selector: ${e.localizedMessage}"
            }
        }
    }

    fun verifyPasswordAndLogin() {
        focusManager.clearFocus()
        errorMessage = null
        infoBannerMessage = null
        isLoading = true
        viewModel.verifyOrInitRtdbPassword(passwordInput) { matched, msg ->
            if (matched) {
                viewModel.loginWithGoogleAccount(
                    idToken = selectedIdToken,
                    email = selectedEmail,
                    displayName = selectedDisplayName,
                    onSuccess = {
                        isLoading = false
                        onLoginSuccess()
                    },
                    onError = { err ->
                        isLoading = false
                        errorMessage = err
                    }
                )
            } else {
                isLoading = false
                if (msg.contains("value 0", ignoreCase = true) || msg.contains("created", ignoreCase = true)) {
                    infoBannerMessage = msg
                } else {
                    errorMessage = msg
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "login_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App / Business Logo matching main app theme
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                        )
                    )
                    .border(2.5.dp, Color(0xFF6366F1).copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AppLogoContainer(
                    logoName = appLogo,
                    tintColor = Color(0xFFF59E0B),
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = businessName.ifBlank { "MD Finance" },
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Smart Weekly Collection & Ledger",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.94f)
                ),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedEmail == null) {
                        Text(
                            text = "Sign In with Google",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Select any Gmail account to continue, then enter your RTDB security password.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { launchGooglePicker() },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Connecting...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Google Sign In",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Step 2: Account selected -> Verify RTDB Password
                        Text(
                            text = "Verify Security Password",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected Account",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedDisplayName ?: "Google User",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = selectedEmail ?: "",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedEmail = null
                                        selectedDisplayName = null
                                        selectedIdToken = null
                                        passwordInput = ""
                                        errorMessage = null
                                        infoBannerMessage = null
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Switch Account",
                                        tint = Color(0xFF818CF8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            label = { Text("Enter App Password") },
                            placeholder = { Text("Match RTDB password") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = Color(0xFF818CF8)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { verifyPasswordAndLogin() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedLabelColor = Color(0xFF818CF8),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                cursorColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rtdb_password_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { verifyPasswordAndLogin() },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("verify_password_login_btn")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying with RTDB...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "Verify & Enter App",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = infoBannerMessage != null) {
                        Surface(
                            color = Color(0xFF1E3A8A).copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = infoBannerMessage ?: "",
                                color = Color(0xFFBFDBFE),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
