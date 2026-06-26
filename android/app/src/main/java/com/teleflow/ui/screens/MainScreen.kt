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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
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
    onCountries: () -> Unit
) {
    val state    by vm.connectionState.collectAsState()
    val authed   by vm.isAuthenticated.collectAsState()
    val premium  by vm.isPremium.collectAsState()
    val proxyName by vm.selectedProxyName.collectAsState()
    val duration by vm.connectionDuration.collectAsState()
    val down     by vm.bytesDown.collectAsState()
    val up       by vm.bytesUp.collectAsState()
    val speedDown by vm.speedDown.collectAsState()
    val speedUp   by vm.speedUp.collectAsState()
    val myIp     by vm.myIp.collectAsState()
    val error    by vm.error.collectAsState()

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "scale"
    )

    val inf = rememberInfiniteTransition("loop")

    val ring1A by inf.animateFloat(0.50f, 0f, infiniteRepeatable(tween(1600, easing = EaseOutSine), RepeatMode.Restart), label = "r1a")
    val ring1S by inf.animateFloat(1.0f, 1.65f, infiniteRepeatable(tween(1600, easing = EaseOutCubic), RepeatMode.Restart), label = "r1s")

    val ring2A by inf.animateFloat(0.38f, 0f, infiniteRepeatable(tween(1600, 400, EaseOutSine), RepeatMode.Restart), label = "r2a")
    val ring2S by inf.animateFloat(1.0f, 1.45f, infiniteRepeatable(tween(1600, 400, EaseOutCubic), RepeatMode.Restart), label = "r2s")

    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "rot")

    val targetCol = when (state) {
        ConnectionState.CONNECTED      -> Green
        ConnectionState.CONNECTING,
        ConnectionState.DISCONNECTING -> Amber
        ConnectionState.ERROR         -> Red
        ConnectionState.DISCONNECTED  -> TeleBlue
    }
    val btnColor by animateColorAsState(targetCol, tween(400), label = "col")

    val pulseAlpha by inf.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(TeleBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, null, Modifier.size(18.dp), Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Text("TeleFlow", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            if (premium) {
                Surface(shape = RoundedCornerShape(6.dp), color = Gold.copy(alpha = 0.14f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Bolt, null, Modifier.size(13.dp), Gold)
                        Spacer(Modifier.width(3.dp))
                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ConnectionCard(
            state = state,
            duration = duration,
            myIp = myIp,
            speedDown = speedDown,
            speedUp = speedUp,
            pulseAlpha = pulseAlpha
        )

        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            if (state == ConnectionState.CONNECTING || state == ConnectionState.DISCONNECTING) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(SolidColor(btnColor), r * ring1S, alpha = ring1A)
                    drawCircle(SolidColor(btnColor), r * ring2S, alpha = ring2A)
                }
            }
            if (state == ConnectionState.CONNECTED) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(SolidColor(Green), r * 1.15f, alpha = 0.06f)
                }
            }

            Box(
                Modifier
                    .size(168.dp)
                    .graphicsLayer {
                        scaleX = btnScale; scaleY = btnScale
                        shadowElevation = if (state == ConnectionState.CONNECTED) 8f else 28f
                        shape = CircleShape
                        clip = true
                    }
                    .clip(CircleShape)
                    .background(btnColor)
                    .clickable(source, indication = null, enabled = authed) {
                        when (state) {
                            ConnectionState.DISCONNECTED,
                            ConnectionState.ERROR     -> vm.connect()
                            ConnectionState.CONNECTED -> vm.disconnect()
                            else -> {}
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        when (state) {
                            ConnectionState.CONNECTED      -> Icons.Filled.Shield
                            ConnectionState.CONNECTING     -> Icons.Filled.Sync
                            ConnectionState.DISCONNECTING   -> Icons.Filled.PowerSettingsNew
                            ConnectionState.ERROR           -> Icons.Filled.Refresh
                            ConnectionState.DISCONNECTED    -> Icons.Filled.PowerSettingsNew
                        },
                        null,
                        Modifier
                            .size(44.dp)
                            .then(
                                if (state == ConnectionState.CONNECTING)
                                    Modifier.graphicsLayer { rotationZ = rot }
                                else Modifier
                            ),
                        Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (state) {
                            ConnectionState.CONNECTED      -> "DISCONNECT"
                            ConnectionState.CONNECTING      -> "CANCEL"
                            ConnectionState.DISCONNECTING   -> "STOPPING"
                            ConnectionState.ERROR           -> "RETRY"
                            ConnectionState.DISCONNECTED    -> "CONNECT"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        LocationCard(proxyName = proxyName, state = state, onCountries = onCountries)

        if (!authed) {
            Spacer(Modifier.height(10.dp))
            AuthPrompt(onAuth)
        }

        if (!premium && authed) {
            Spacer(Modifier.height(10.dp))
            UpgradeCard()
        }

        Spacer(Modifier.height(20.dp))
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
private fun ConnectionCard(
    state: ConnectionState,
    duration: String,
    myIp: String,
    speedDown: Long,
    speedUp: Long,
    pulseAlpha: Float
) {
    val statusColor = when (state) {
        ConnectionState.CONNECTED  -> Green
        ConnectionState.CONNECTING -> Amber
        ConnectionState.ERROR      -> Red
        else                        -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(statusColor)
                        .then(
                            if (state == ConnectionState.CONNECTING || state == ConnectionState.CONNECTED)
                                Modifier.graphicsLayer { alpha = pulseAlpha }
                            else Modifier
                        )
                )
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    state,
                    transitionSpec = {
                        fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 } togetherWith
                        fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 4 }
                    },
                    label = "status"
                ) { s ->
                    Text(
                        when (s) {
                            ConnectionState.CONNECTED      -> "Connected"
                            ConnectionState.CONNECTING     -> "Connecting…"
                            ConnectionState.DISCONNECTING  -> "Disconnecting…"
                            ConnectionState.ERROR          -> "Connection failed"
                            ConnectionState.DISCONNECTED   -> "Not connected"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = state == ConnectionState.CONNECTED && myIp.isNotEmpty(),
                transitionSpec = {
                    fadeIn(tween(200)) + expandVertically(tween(200)) togetherWith
                    fadeOut(tween(150)) + shrinkVertically(tween(150))
                },
                label = "ip"
            ) { showIp ->
                if (showIp) {
                    Text(
                        myIp,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Time", duration, Icons.Outlined.Schedule)
                VerticalDivider()
                StatColumn("Download", "${fmtSpeed(speedDown)}/s", Icons.Outlined.FileDownload)
                VerticalDivider()
                StatColumn("Upload", "${fmtSpeed(speedUp)}/s", Icons.Outlined.FileUpload)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(13.dp), TextSecondary)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

@Composable
private fun LocationCard(proxyName: String, state: ConnectionState, onCountries: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCountries),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(TeleBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Public, null, Modifier.size(20.dp), TeleBlue)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Location", fontSize = 12.sp, color = TextSecondary)
                Text(proxyName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp), TextSecondary.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun AuthPrompt(onTap: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(TeleBlue.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(28.dp), TeleBlue)
            Spacer(Modifier.height(10.dp))
            Text("Sign in required", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("Authenticate with Telegram to connect", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onTap,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TeleBlue)
            ) {
                Text("Continue with Telegram", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UpgradeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                Text("Unlock all locations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Requires Telegram Premium", fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), TextSecondary.copy(alpha = 0.4f))
        }
    }
}

private fun fmtSpeed(bytes: Long): String = when {
    bytes < 1024          -> "${bytes} B"
    bytes < 1048576       -> "${"%.1f".format(bytes / 1024.0)} KB"
    else                  -> "${"%.1f".format(bytes / 1048576.0)} MB"
}
