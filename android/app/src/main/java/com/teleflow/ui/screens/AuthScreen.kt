package com.teleflow.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.BuildConfig
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    vm: MainViewModel,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val authed by vm.isAuthenticated.collectAsState()
    val error by vm.error.collectAsState()

    var authCode by remember { mutableStateOf<String?>(null) }
    var authStatus by remember { mutableStateOf<String?>(null) }
    var isPolling by remember { mutableStateOf(false) }

    LaunchedEffect(authed) { if (authed) onDone() }

    LaunchedEffect(isPolling, authCode) {
        if (isPolling && authCode != null) {
            val code = authCode!!
            while (true) {
                delay(3000)
                val result = vm.repository.checkPendingAuth(code)
                result.onSuccess { resp ->
                    when (resp.status) {
                        "claimed" -> {
                            authStatus = "claimed"
                            if (resp.token != null) {
                                vm.completeAuth(resp.token, resp.user)
                            }
                            return@LaunchedEffect
                        }
                    }
                }
            }
        }
    }

    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                show,
                enter = scaleIn(tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(400))
            ) {
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(TeleBlue.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, null, Modifier.size(42.dp), TeleBlue)
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                show,
                enter = fadeIn(tween(450, 120)) + slideInVertically(tween(500, 120)) { it / 3 }
            ) {
                Text(
                    if (authStatus == "claimed") "Authentication successful!"
                    else if (authCode != null) "Your auth code"
                    else "Sign in with Telegram",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (authStatus == "claimed") {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(Green.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(32.dp), Green)
                    }
                    Spacer(Modifier.height(24.dp))
                } else if (authCode != null) {
                    Text(
                        "Send this code to @${BuildConfig.TG_BOT_USERNAME}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                authCode!!,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 8.sp,
                                color = TeleBlue
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Or type /start ${authCode!!}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${BuildConfig.TG_BOT_USERNAME}?start=${authCode}"))
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
                    ) {
                        Icon(Icons.Filled.Send, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Open Telegram", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = TeleBlue, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Waiting for you to send the code…", fontSize = 13.sp, color = TextSecondary)
                    }
                } else {
                    Text(
                        "Authenticate with your Telegram account\nto start using TeleFlow.",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            val code = generateCode()
                            authCode = code
                            authStatus = "pending"
                            isPolling = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
                    ) {
                        Icon(Icons.Filled.Send, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Sign in with Telegram", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("How it works", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            Step(1, "Tap \"Sign in with Telegram\"")
                            Step(2, "A code will appear — send it to @${BuildConfig.TG_BOT_USERNAME}")
                            Step(3, "Return here — you're authenticated")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(40.dp))
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = { TextButton({ vm.clearError() }) { Text("OK") } }
        )
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(TeleBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeleBlue)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
    }
}

private fun generateCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
