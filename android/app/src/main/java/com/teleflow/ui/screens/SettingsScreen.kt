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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            item { SwitchRow(Icons.Outlined.Shield, "Kill Switch", "Block all traffic if VPN drops", vm.repository.isKillSwitchEnabled(), { vm.repository.setKillSwitchEnabled(it) }) }
            item { DividerRow() }
            item { LabelRow(Icons.Outlined.Dns, "DNS", "1.1.1.1 · 1.0.0.1 (Cloudflare)") }
            item { DividerRow() }
            item { LabelRow(Icons.Outlined.Speed, "Protocol", "SOCKS5") }
            item { DividerRow() }
            item { SwitchRow(Icons.Outlined.NetworkCheck, "IPv6 Leak Protection", "Block IPv6 outside the tunnel", true, {}) }

            item { GroupLabel("ACCOUNT") }
            item { LabelRow(Icons.Outlined.Person, "Account", "Signed in via Telegram") }
            item { DividerRow() }
            item { SwitchRow(Icons.Outlined.Notifications, "Connection Alerts", "Show notification on state change", true, {}) }

            item { GroupLabel("PRIVACY & SECURITY") }
            item { LabelRow(Icons.Outlined.VerifiedUser, "No-Logs Policy", "Zero traffic data stored") }
            item { DividerRow() }
            item { LabelRow(Icons.Outlined.Code, "Open Source", "MIT License · Fully auditable") }

            item { GroupLabel("ABOUT") }
            item { LabelRow(Icons.Outlined.Info, "Version", "1.0.0 (build 1)") }
            item { DividerRow() }
            item { LabelRow(Icons.Outlined.Update, "Check for Updates", "") }

            item {
                Spacer(Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { vm.logout(); onBack() },
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp),
                    RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                ) {
                    Icon(Icons.Filled.Logout, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Medium)
                }
            }
        }
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
        modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 6.dp)
    )
}

@Composable
private fun DividerRow() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
private fun LabelRow(icon: ImageVector, title: String, subtitle: String) {
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
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
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
            checked, onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnBlue,
                checkedTrackColor = TeleBlue
            )
        )
    }
}
