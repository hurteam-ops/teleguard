package com.teleflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.data.model.ConnectionState
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel

@Composable
fun MainScreen(
    vm: MainViewModel,
    onAuth: () -> Unit,
    onCountries: () -> Unit,
    onSettings: () -> Unit
) {
    val state       by vm.connectionState.collectAsState()
    val authed      by vm.isAuthenticated.collectAsState()
    val premium     by vm.isPremium.collectAsState()
    val proxyId     by vm.selectedProxyId.collectAsState()
    val duration    by vm.connectionDuration.collectAsState()
    val down        by vm.bytesDown.collectAsState()
    val up          by vm.bytesUp.collectAsState()
    val error       by vm.error.collectAsState()

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "scale"
    )

    val inf = rememberInfiniteTransition("loop")

    val ring1Alpha by inf.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1400, easing = EaseOutSine), RepeatMode.Restart), label = "")
    val ring1Scale by inf.animateFloat(1.0f, 1.7f, infiniteRepeatable(tween(1400, easing = EaseOutCubic), RepeatMode.Restart), label = "")

    val ring2Alpha by inf.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1400, 350, EaseOutSine), RepeatMode.Restart), label = "")
    val ring2Scale by inf.animateFloat(1.0f, 1.55f, infiniteRepeatable(tween(1400, 350, EaseOutCubic), RepeatMode.Restart), label = "")

    val ring3Alpha by inf.animateFloat(0.35f, 0f, infiniteRepeatable(tween(1400, 700, EaseOutSine), RepeatMode.Restart), label = "")
    val ring3Scale by inf.animateFloat(1.0f, 1.4f, infiniteRepeatable(tween(1400, 700, EaseOutCubic), RepeatMode.Restart), label = "")

    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart), label = "")

    val targetCol = when (state) {
        ConnectionState.CONNECTED     -> Green
        ConnectionState.CONNECTING,
        ConnectionState.DISCONNECTING -> Amber
        ConnectionState.ERROR         -> Red
        ConnectionState.DISCONNECTED  -> TeleBlue
    }
    val btnColor by animateColorAsState(targetCol, tween(350), label = "col")

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TeleFlow", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.weight(1f))
                if (premium) {
                    Surface(RoundedCornerShape(6.dp), color = Gold.copy(alpha = 0.12f)) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Bolt, null, Modifier.size(13.dp), Gold)
                            Spacer(Modifier.width(3.dp))
                            Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, Gold)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    state,
                    transitionSpec = {
                        fadeIn(tween(180)) + slideInVertically(tween(180)) { h -> h / 3 } togetherWith
                        fadeOut(tween(120)) + slideOutVertically(tween(120)) { h -> -h / 3 }
                    },
                    label = "status"
                ) { s ->
                    Text(
                        when (s) {
                            ConnectionState.CONNECTED     -> "Connected"
                            ConnectionState.CONNECTING    -> "Connecting…"
                            ConnectionState.DISCONNECTING -> "Disconnecting…"
                            ConnectionState.ERROR         -> "Connection failed"
                            ConnectionState.DISCONNECTED  -> "Not connected"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = when (s) {
                            ConnectionState.CONNECTED  -> Green
                            ConnectionState.CONNECTING -> Amber
                            ConnectionState.ERROR      -> Red
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                }

                AnimatedVisibility(visible = state == ConnectionState.CONNECTED) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(4.dp))
                        Text(duration, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "\u2193 ${fmt(down)}  \u2191 ${fmt(up)}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                if (state == ConnectionState.CONNECTING) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        drawCircle(SolidColor(btnColor), r * ring1Scale, alpha = ring1Alpha)
                        drawCircle(SolidColor(btnColor), r * ring2Scale, alpha = ring2Alpha)
                        drawCircle(SolidColor(btnColor), r * ring3Scale, alpha = ring3Alpha)
                    }
                }

                Box(
                    Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            scaleX = btnScale; scaleY = btnScale
                            shadowElevation = if (state == ConnectionState.CONNECTED) 0f else 24f
                            shape = CircleShape
                            clip = true
                        }
                        .clip(CircleShape)
                        .background(btnColor)
                        .clickable(source, indication = null, enabled = authed) {
                            when (state) {
                                ConnectionState.DISCONNECTED,
                                ConnectionState.ERROR         -> vm.connect()
                                ConnectionState.CONNECTED     -> vm.disconnect()
                                else -> {}
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            when (state) {
                                ConnectionState.CONNECTED     -> Icons.Filled.Shield
                                ConnectionState.CONNECTING    -> Icons.Filled.Sync
                                ConnectionState.DISCONNECTING -> Icons.Filled.PowerSettingsNew
                                ConnectionState.ERROR         -> Icons.Filled.Refresh
                                ConnectionState.DISCONNECTED  -> Icons.Filled.PowerSettingsNew
                            },
                            null,
                            Modifier
                                .size(52.dp)
                                .then(
                                    if (state == ConnectionState.CONNECTING)
                                        Modifier.graphicsLayer { rotationZ = rot }
                                    else Modifier
                                ),
                            Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (state) {
                                ConnectionState.CONNECTED     -> "STOP"
                                ConnectionState.CONNECTING    -> "CONNECTING"
                                ConnectionState.DISCONNECTING -> "STOPPING"
                                ConnectionState.ERROR         -> "RETRY"
                                ConnectionState.DISCONNECTED  -> "CONNECT"
                            },
                            Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 2.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                authed,
                transitionSpec = {
                    fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 } togetherWith
                    fadeOut(tween(180))
                },
                label = "bottom"
            ) { logged ->
                if (!logged) AuthPrompt(onAuth, Modifier.padding(horizontal = 20.dp))
                else InfoSection(state, premium, proxyId, onCountries, Modifier.padding(horizontal = 20.dp))
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabIcon(Icons.Filled.Home, "Home", selected = true)
                TabIcon(Icons.Outlined.Person, "Profile", onClick = onAuth)
                TabIcon(Icons.Outlined.Settings, "Settings", onClick = onSettings)
            }

            Spacer(Modifier.height(8.dp))
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
private fun TabIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick) {
        Icon(icon, label, tint = if (selected) TeleBlue else TextSecondary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AuthPrompt(onTap: () -> Unit, mod: Modifier = Modifier) {
    Card(mod.fillMaxWidth(), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(56.dp), CircleShape, TeleBlue.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(26.dp), TeleBlue)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Sign in with Telegram", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your Telegram account is your key to TeleFlow",
                fontSize = 14.sp, color = TextSecondary
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onTap,
                Modifier.fillMaxWidth().height(48.dp),
                RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
            ) {
                Text("Continue with Telegram", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoSection(
    state: ConnectionState,
    premium: Boolean,
    proxyId: String,
    onCountries: () -> Unit,
    mod: Modifier = Modifier
) {
    Column(mod.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onCountries).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Public, null, Modifier.size(22.dp), TextSecondary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Location", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        if (proxyId == "auto") "Automatic (Fastest)" else proxyId,
                        fontWeight = FontWeight.Medium, fontSize = 15.sp
                    )
                }
                Text("Change", color = TeleBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Card(
            RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Speed, null, Modifier.size(22.dp), TextSecondary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Protocol", fontSize = 12.sp, color = TextSecondary)
                    Text("SOCKS5 / Proxy", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                if (state == ConnectionState.CONNECTED) {
                    Surface(RoundedCornerShape(6.dp), Green.copy(alpha = 0.12f)) {
                        Text(
                            "Active",
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, Green
                        )
                    }
                }
            }
        }

        if (!premium) {
            Card(
                RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(Gold.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(22.dp), Gold)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Unlock all locations", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Requires Telegram Premium", fontSize = 13.sp, color = TextSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), TextSecondary)
                }
            }
        }
    }
}

private fun fmt(bytes: Long): String = when {
    bytes < 1024          -> "${bytes} B"
    bytes < 1048576       -> "${bytes / 1024} KB"
    bytes < 1073741824    -> "${"%.1f".format(bytes / 1048576.0)} MB"
    else                  -> "${"%.2f".format(bytes / 1073741824.0)} GB"
}
