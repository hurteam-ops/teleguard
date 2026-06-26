package com.teleflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val killSwitch by remember { mutableStateOf(vm.repository.isKillSwitchEnabled()) }
    val autoConnect by remember { mutableStateOf(vm.repository.isAutoConnectEnabled()) }
    val ipv6 by remember { mutableStateOf(vm.repository.isIpv6ProtectionEnabled()) }
    val alerts by remember { mutableStateOf(vm.repository.isConnectionAlertsEnabled()) }
    val dns by remember { mutableStateOf(vm.repository.getDnsServer()) }
    val protocol by remember { mutableStateOf(vm.repository.getProtocol()) }
    val theme by remember { mutableStateOf(vm.repository.getThemePreference()) }
    val username by remember { mutableStateOf(vm.repository.getUsername()) }
    val premium by vm.isPremium.collectAsState()

    var showDnsDialog by remember { mutableStateOf(false) }
    var showProtocolDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item { GroupLabel("CONNECTION") }
            item {
                SwitchRow(
                    Icons.Outlined.Shield,
                    "Kill Switch",
                    "Block all traffic if VPN drops",
                    killSwitch
                ) { vm.repository.setKillSwitchEnabled(it); }
            }
            item { DividerRow() }
            item {
                SwitchRow(
                    Icons.Outlined.Autorenew,
                    "Auto-Connect",
                    "Connect automatically on app start",
                    autoConnect
                ) { vm.repository.setAutoConnectEnabled(it); }
            }
            item { DividerRow() }
            item {
                DialogRow(
                    Icons.Outlined.Dns,
                    "DNS Server",
                    dnsLabel(dns)
                ) { showDnsDialog = true }
            }
            item { DividerRow() }
            item {
                DialogRow(
                    Icons.Outlined.Lan,
                    "Protocol",
                    protocol.uppercase()
                ) { showProtocolDialog = true }
            }
            item { DividerRow() }
            item {
                SwitchRow(
                    Icons.Outlined.NetworkCheck,
                    "IPv6 Leak Protection",
                    "Block IPv6 traffic outside tunnel",
                    ipv6
                ) { vm.repository.setIpv6ProtectionEnabled(it); }
            }

            item { GroupLabel("ACCOUNT") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
                                    .background(TeleBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Person, null, Modifier.size(22.dp), TeleBlue)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (username.isNotBlank()) "@$username" else "Telegram User", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("Signed in via Telegram", fontSize = 13.sp, color = TextSecondary)
                            }
                            if (premium) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Gold.copy(alpha = 0.14f)) {
                                    Text(
                                        "PRO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Gold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                SwitchRow(
                    Icons.Outlined.Notifications,
                    "Connection Alerts",
                    "Notify on state changes",
                    alerts
                ) { vm.repository.setConnectionAlertsEnabled(it); }
            }

            item { GroupLabel("APPEARANCE") }
            item {
                DialogRow(
                    Icons.Outlined.Palette,
                    "Theme",
                    themeLabel(theme)
                ) { showThemeDialog = true }
            }

            item { GroupLabel("PRIVACY & SECURITY") }
            item {
                InfoRow(Icons.Outlined.VerifiedUser, "No-Logs Policy", "Zero traffic data stored")
            }
            item { DividerRow() }
            item {
                InfoRow(Icons.Outlined.Code, "Open Source", "MIT License · Fully auditable")
            }

            item { GroupLabel("ABOUT") }
            item {
                InfoRow(Icons.Outlined.Info, "Version", "1.0.0 (build 1)")
            }
            item { DividerRow() }
            item {
                InfoRow(Icons.Outlined.Update, "Check for Updates", "")
            }

            item {
                Spacer(Modifier.height(28.dp))
                OutlinedButton(
                    onClick = { vm.logout(); onBack() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                ) {
                    Icon(Icons.Filled.Logout, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showDnsDialog) {
        SelectionDialog(
            title = "DNS Server",
            options = listOf(
                "1.1.1.1" to "Cloudflare",
                "8.8.8.8" to "Google",
                "9.9.9.9" to "Quad9",
                "208.67.222.222" to "OpenDNS"
            ),
            selected = dns,
            onSelect = { choice -> vm.repository.setDnsServer(choice); showDnsDialog = false },
            onDismiss = { showDnsDialog = false }
        )
    }

    if (showProtocolDialog) {
        SelectionDialog(
            title = "Protocol",
            options = listOf(
                "socks5" to "SOCKS5",
                "socks5-tls" to "SOCKS5 over TLS"
            ),
            selected = protocol,
            onSelect = { choice -> vm.repository.setProtocol(choice); showProtocolDialog = false },
            onDismiss = { showProtocolDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "Theme",
            options = listOf(
                "system" to "Follow System",
                "light" to "Light",
                "dark" to "Dark"
            ),
            selected = theme,
            onSelect = { choice -> vm.repository.setThemePreference(choice); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 6.dp)
    )
}

@Composable
private fun DividerRow() {
    Divider(
        modifier = Modifier.padding(start = 56.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (subtitle.isNotEmpty())
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), TextSecondary.copy(alpha = 0.4f))
    }
}

@Composable
private fun DialogRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(value, fontSize = 13.sp, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), TextSecondary.copy(alpha = 0.4f))
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), TextSecondary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
        Switch(
            isChecked, { isChecked = it; onChecked(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnBlue,
                checkedTrackColor = TeleBlue
            )
        )
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (value == selected) {
                            Icon(Icons.Filled.Check, null, Modifier.size(20.dp), TeleBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun dnsLabel(dns: String): String = when (dns) {
    "1.1.1.1" -> "1.1.1.1 · Cloudflare"
    "8.8.8.8" -> "8.8.8.8 · Google"
    "9.9.9.9" -> "9.9.9.9 · Quad9"
    "208.67.222.222" -> "208.67.222.222 · OpenDNS"
    else -> dns
}

private fun protocolLabel(p: String): String = when (p) {
    "socks5" -> "SOCKS5"
    "socks5-tls" -> "SOCKS5 over TLS"
    else -> p.uppercase()
}

private fun themeLabel(t: String): String = when (t) {
    "system" -> "Follow System"
    "light" -> "Light"
    "dark" -> "Dark"
    else -> t
}
