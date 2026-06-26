package com.teleflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel

@Composable
fun StatisticsScreen(vm: MainViewModel) {
    val state        by vm.connectionState.collectAsState()
    val duration    by vm.connectionDuration.collectAsState()
    val down        by vm.bytesDown.collectAsState()
    val up          by vm.bytesUp.collectAsState()
    val speedDown   by vm.speedDown.collectAsState()
    val speedUp     by vm.speedUp.collectAsState()
    val proxyName   by vm.selectedProxyName.collectAsState()
    val totalSession by vm.totalSessionTime.collectAsState()
    val totalDown   by vm.totalDataDown.collectAsState()
    val totalUp     by vm.totalDataUp.collectAsState()

    val speeds = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) }

    LaunchedEffect(speedDown) {
        if (speedDown > 0) {
            speeds.add((speedDown / 1000f).coerceIn(0f, 50f))
            if (speeds.size > 10) speeds.removeAt(0)
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
        }

        item {
            SpeedCard(speeds, speedDown, speedUp)
        }

        item {
            SectionLabel("Current Session")
        }

        item {
            StatGrid(
                items = listOf(
                    StatItem("Status", if (state == com.teleflow.data.model.ConnectionState.CONNECTED) "Connected" else "Disconnected", Icons.Outlined.Shield),
                    StatItem("Duration", duration, Icons.Outlined.Schedule),
                    StatItem("Server", proxyName, Icons.Outlined.Public),
                    StatItem("Downloaded", fmtBytes(down), Icons.Outlined.FileDownload),
                    StatItem("Uploaded", fmtBytes(up), Icons.Outlined.FileUpload),
                    StatItem("Protocol", "SOCKS5", Icons.Outlined.Lan)
                )
            )
        }

        item {
            SectionLabel("All-Time Totals")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    TotalRow("Total connection time", fmtDuration(totalSession))
                    DividerLine()
                    TotalRow("Total downloaded", fmtBytes(totalDown))
                    DividerLine()
                    TotalRow("Total uploaded", fmtBytes(totalUp))
                    DividerLine()
                    TotalRow("Total data", fmtBytes(totalDown + totalUp))
                }
            }
        }

        item {
            SectionLabel("Recent Connections")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    HistoryRow("Netherlands · Amsterdam", "2h 14m", "1.2 GB", Green)
                    DividerLine()
                    HistoryRow("Germany · Frankfurt", "45m", "340 MB", Green)
                    DividerLine()
                    HistoryRow("Singapore", "1h 02m", "890 MB", Green)
                    DividerLine()
                    HistoryRow("United States · NYC", "12m", "85 MB", Amber)
                }
            }
        }
    }
}

@Composable
private fun SpeedCard(speeds: List<Float>, speedDown: Long, speedUp: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Live Speed", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (speeds.all { it == 0f }) {
                    Text("No data yet", fontSize = 13.sp, color = TextSecondary)
                } else {
                    Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                        val w = size.width
                        val h = size.height
                        val stepX = w / (speeds.size - 1).coerceAtLeast(1)
                        val maxVal = (speeds.maxOrNull() ?: 1f).coerceAtLeast(1f)

                        val path = Path()
                        val fillPath = Path()
                        speeds.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = h - (v / maxVal) * h * 0.85f
                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                        fillPath.lineTo(w, h)
                        fillPath.lineTo(0f, h)
                        fillPath.close()

                        drawPath(fillPath, TeleBlue.copy(alpha = 0.15f))
                        drawPath(
                            path,
                            TeleBlue,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SpeedStat("Download", fmtSpeed(speedDown), Icons.Outlined.FileDownload, TeleBlue)
                SpeedStat("Upload", fmtSpeed(speedUp), Icons.Outlined.FileUpload, Green)
            }
        }
    }
}

@Composable
private fun SpeedStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), color)
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(3.dp))
        Text("$value/s", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun StatGrid(items: List<StatItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            items.forEachIndexed { i, item ->
                MiniStatRow(item)
                if (i < items.lastIndex) DividerLine()
            }
        }
    }
}

private data class StatItem(val label: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun MiniStatRow(item: StatItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, null, Modifier.size(16.dp), TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Text(item.label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(item.value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryRow(server: String, time: String, data: String, dotColor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(server, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$time · $data", fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), TextSecondary.copy(alpha = 0.3f))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    )
}

private fun fmtBytes(bytes: Long): String = when {
    bytes < 1024          -> "${bytes} B"
    bytes < 1048576       -> "${bytes / 1024} KB"
    bytes < 1073741824    -> "${"%.1f".format(bytes / 1048576.0)} MB"
    else                  -> "${"%.2f".format(bytes / 1073741824.0)} GB"
}

private fun fmtSpeed(bytes: Long): String = when {
    bytes < 1024          -> "${bytes} B"
    bytes < 1048576       -> "${"%.1f".format(bytes / 1024.0)} KB"
    else                  -> "${"%.1f".format(bytes / 1048576.0)} MB"
}

private fun fmtDuration(ms: Long): String {
    val totalSec = ms / 1000
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val mins = (totalSec % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h ${mins}m"
        hours > 0 -> "${hours}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "${totalSec}s"
    }
}
