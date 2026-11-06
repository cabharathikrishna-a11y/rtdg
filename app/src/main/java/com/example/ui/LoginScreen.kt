package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    // Dynamic App Brand & Theme State from ViewModel
    val customAppName by viewModel.customAppName.collectAsStateWithLifecycle()
    val customAppLogo by viewModel.customAppLogo.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    
    val appColors = LocalAppThemeColors.current

    val displayAppName = if (customAppName.isNotBlank()) customAppName else "MD FINANCE"

    // Configure Google Sign In Options
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    // Google Launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    isLoading = true
                    viewModel.loginWithGoogleAccount(
                        idToken = account.idToken,
                        email = account.email,
                        displayName = account.displayName,
                        onSuccess = {
                            isLoading = false
                        },
                        onError = { err ->
                            isLoading = false
                            errorMessage = err
                        }
                    )
                } else {
                    isLoading = false
                    errorMessage = "Failed to obtain Google account details."
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google Sign In failed (Status Code: ${e.statusCode})"
            }
        } else {
            isLoading = false
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = if (appColors.isDark) {
            listOf(appColors.mainBg, appColors.darkBg, Color(0xFF020617))
        } else {
            listOf(
                appColors.darkBg,
                appColors.primaryAccent.copy(alpha = 0.85f),
                appColors.darkBg
            )
        }
    )

    val cardBg = if (appColors.isDark) Color(0xFF1E293B) else Color.White
    val cardTextColor = if (appColors.isDark) Color.White else Color(0xFF0F172A)
    val cardSubTextColor = if (appColors.isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Custom App Logo Badge Container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (appColors.isDark) Color(0xFF1E293B) else Color.White,
                        shape = CircleShape
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(appColors.primaryAccent, appColors.secondaryAccent)
                        ),
                        shape = CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                AppLogoContainer(
                    logoName = customAppLogo,
                    modifier = Modifier.size(54.dp),
                    tintColor = getLogoRealColor(customAppLogo)
                )
            }

            // App Brand Name & Tagline
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayAppName.uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in using your Google Account",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Google Authentication Card Frame
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, appColors.primaryAccent.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error Display Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        errorMessage?.let { error ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF991B1B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error,
                                    color = Color(0xFFFECACA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = "Google Sign-In Portal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardTextColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Access $displayAppName securely with your Google Workspace or Personal Account.",
                        fontSize = 12.sp,
                        color = cardSubTextColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Google Sign-In Button
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (appColors.isDark) Color.White else appColors.primaryAccent,
                            contentColor = if (appColors.isDark) Color(0xFF1E293B) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signin_button"),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = if (appColors.isDark) appColors.primaryAccent else Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Google G Badge
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(Color(0xFF4285F4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (appColors.isDark) Color(0xFF0F172A) else Color.White
                                )
                            }
                        }
                    }

                    // Quick One-Tap Continuation for previously selected Google account
                    val lastAccount = remember(context) { GoogleSignIn.getLastSignedInAccount(context) }
                    if (lastAccount != null) {
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                viewModel.loginWithGoogleAccount(
                                    idToken = lastAccount.idToken,
                                    email = lastAccount.email,
                                    displayName = lastAccount.displayName,
                                    onSuccess = { isLoading = false },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, appColors.primaryAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = appColors.primaryAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_as_google_user_button"),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Account Icon",
                                tint = appColors.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continue as ${lastAccount.displayName ?: lastAccount.email ?: "Google User"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (appColors.isDark) Color.White else appColors.primaryAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Security Disclaimer Footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted Icon",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secured by Google Identity & Firebase Auth Platform",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

