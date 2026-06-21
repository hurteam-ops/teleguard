package com.teleflow.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.BuildConfig
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel

@Composable
fun AuthScreen(
    vm: MainViewModel,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val loading   by vm.isLoading.collectAsState()
    val authed    by vm.isAuthenticated.collectAsState()

    LaunchedEffect(authed) { if (authed) onDone() }

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
            Spacer(Modifier.height(60.dp))
            Spacer(Modifier.weight(0.35f))

            AnimatedVisibility(
                show,
                enter = scaleIn(tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(400))
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(TeleBlue.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(42.dp), TeleBlue)
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                show,
                enter = fadeIn(tween(450, 120)) + slideInVertically(tween(500, 120)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Welcome to TeleFlow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sign in with your Telegram account.\nYour data stays on your device.",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.weight(0.45f))

            AnimatedVisibility(
                show,
                enter = fadeIn(tween(450, 260)) + slideInVertically(tween(500, 260)) { it / 4 }
            ) {
                Column {
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${BuildConfig.TG_BOT_USERNAME}?start=auth"))
                            )
                        },
                        Modifier.fillMaxWidth().height(52.dp),
                        RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBlue),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), TextOnBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Send, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Open Telegram", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("How it works", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            Step(1, "Open Telegram and search @${BuildConfig.TG_BOT_USERNAME}")
                            Step(2, "Tap Start, then Authorize")
                            Step(3, "Return to TeleFlow — you're in")
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(
        Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(TeleBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeleBlue)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
    }
}
