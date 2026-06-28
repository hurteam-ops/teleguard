package com.teleflow.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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

@Composable
fun AuthScreen(vm: MainViewModel, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val authed by vm.isAuthenticated.collectAsState()
    val authCode by vm.authCode.collectAsState()
    val authStatus by vm.authStatus.collectAsState()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(authed) { if (authed) onDone() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            if (authStatus == "claimed") {
                Spacer(Modifier.weight(0.3f))
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(36.dp))
                        .background(Green.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, Modifier.size(40.dp), Green) }
                Spacer(Modifier.height(20.dp))
                Text("Signed in!", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Green)
                Spacer(Modifier.height(8.dp))
                Text("You can now use TeleFlow.", fontSize = 15.sp, color = TextSecondary)
                Spacer(Modifier.weight(0.7f))

            } else if (authCode != null) {
                Text("Step 2: Send the code", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open Telegram and send this code to @${BuildConfig.TG_BOT_USERNAME}",
                    fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("YOUR CODE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        SelectionContainer {
                            Text(
                                authCode!!,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 10.sp,
                                color = TeleBlue
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("or type  /start ${authCode!!}", fontSize = 13.sp, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("auth_code", authCode))
                            copied = true
                            Toast.makeText(ctx, "Code copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copied!" else "Copy", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${BuildConfig.TG_BOT_USERNAME}?start=${authCode}"))
                            )
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
                    ) {
                        Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Telegram", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, Modifier.size(20.dp), Amber)
                        Spacer(Modifier.width(12.dp))
                        Text("Waiting for you to send the code…", fontSize = 13.sp, color = TextSecondary)
                    }
                }

            } else {
                Spacer(Modifier.height(16.dp))
                Text("Step 1: Start sign in", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Step(1, "Tap the button below")
                        Step(2, "A 6-letter code appears")
                        Step(3, "Send it to @${BuildConfig.TG_BOT_USERNAME} in Telegram")
                        Step(4, "Come back — you're signed in!")
                    }
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { vm.startAuth() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
                ) {
                    Icon(Icons.Filled.Send, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Sign in with Telegram", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Make sure you have Telegram installed",
                    fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
                .background(TeleBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Text("$n", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeleBlue) }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}
