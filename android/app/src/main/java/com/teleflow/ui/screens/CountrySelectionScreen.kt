package com.teleflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.data.model.ProxyServer
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val proxies  by vm.proxies.collectAsState()
    val selected by vm.selectedProxyId.collectAsState()
    val premium  by vm.isPremium.collectAsState()
    var query by remember { mutableStateOf("") }

    val grouped = remember(proxies, query) {
        val filtered = if (query.isBlank()) proxies else proxies.filter {
            it.country.contains(query, true) ||
            it.ip.contains(query, true) ||
            it.city.contains(query, true)
        }
        filtered.groupBy { regionOf(it.countryCode) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Select Location", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(TeleBlue),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text("Search country, city or IP…", fontSize = 16.sp, color = TextSecondary)
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Clear, null, Modifier.size(18.dp), TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "auto") {
                    ServerRow(
                        flag = "\u26A1",
                        name = "Automatic",
                        sub = "Best available server",
                        latency = 0,
                        load = 0,
                        isSelected = selected == "auto",
                        isLocked = false,
                        delay = 0,
                        onClick = { vm.selectProxy("auto"); onBack() }
                    )
                }

                grouped.forEach { (region, list) ->
                    item(key = "header_$region") {
                        Text(
                            region.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 56.dp, top = 18.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(list, key = { _, p -> p.ip }) { i, proxy ->
                        val locked = !premium
                        ServerRow(
                            flag = flag(proxy.countryCode),
                            name = proxy.country.ifEmpty { proxy.ip },
                            sub = if (proxy.city.isNotBlank()) "${proxy.city} · ${proxy.ip}" else proxy.ip,
                            latency = proxy.latency,
                            load = proxy.load,
                            isSelected = selected == proxy.ip,
                            isLocked = locked,
                            delay = i * 50,
                            onClick = {
                                if (!locked) { vm.selectProxy(proxy.ip); onBack() }
                            }
                        )
                    }
                }

                if (grouped.isEmpty() && query.isNotEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp), TextSecondary.copy(alpha = 0.4f))
                            Spacer(Modifier.height(12.dp))
                            Text("No results for \"$query\"", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRow(
    flag: String,
    name: String,
    sub: String,
    latency: Int,
    load: Int,
    isSelected: Boolean,
    isLocked: Boolean,
    delay: Int,
    onClick: () -> Unit
) {
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delay.toLong()); vis = true }

    AnimatedVisibility(
        vis,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 5 }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLocked, onClick = onClick)
                .background(if (isSelected) TeleBlue.copy(alpha = 0.06f) else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isSelected) TeleBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(flag, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(sub, fontSize = 12.sp, color = TextSecondary)
                if (load > 0 && !isLocked) {
                    Spacer(Modifier.height(6.dp))
                    LoadBar(load)
                }
            }

            when {
                isLocked -> Icon(Icons.Filled.Lock, null, Modifier.size(16.dp), Gold.copy(alpha = 0.7f))
                isSelected -> Icon(Icons.Filled.CheckCircle, null, Modifier.size(22.dp), TeleBlue)
                latency > 0 -> LatencyDot(latency)
            }
        }
    }
}

@Composable
private fun LoadBar(load: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(80.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(load / 100f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            load < 40  -> Green
                            load < 75  -> Amber
                            else       -> Red
                        }
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$load%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun LatencyDot(latency: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    when {
                        latency < 50  -> Green
                        latency < 150 -> Amber
                        else          -> Red
                    }
                )
        )
        Spacer(Modifier.width(6.dp))
        Text("${latency}ms", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
    }
}

private fun regionOf(code: String): String = when (code.uppercase()) {
    "NL", "DE", "GB", "FR", "CH", "SE", "PL", "IT", "ES" -> "Europe"
    "US", "CA", "BR" -> "Americas"
    "JP", "SG", "KR", "IN", "AU", "RU" -> "Asia Pacific"
    else -> "Other"
}

private fun flag(code: String): String = when (code.uppercase()) {
    "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
    "DE" -> "\uD83C\uDDE9\uD83C\uDDEA"
    "US" -> "\uD83C\uDDFA\uD83C\uDDF8"
    "JP" -> "\uD83C\uDDEF\uD83C\uDDF5"
    "SG" -> "\uD83C\uDDF8\uD83C\uDDEC"
    "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
    "FR" -> "\uD83C\uDDEB\uD83C\uDDF7"
    "CA" -> "\uD83C\uDDE8\uD83C\uDDE6"
    "RU" -> "\uD83C\uDDF7\uD83C\uDDFA"
    "KR" -> "\uD83C\uDDF0\uD83C\uDDF7"
    "AU" -> "\uD83C\uDDE6\uD83C\uDDFA"
    "BR" -> "\uD83C\uDDE7\uD83C\uDDF7"
    "IN" -> "\uD83C\uDDEE\uD83C\uDDF3"
    "IT" -> "\uD83C\uDDEE\uD83C\uDDF9"
    "ES" -> "\uD83C\uDDEA\uD83C\uDDF8"
    "CH" -> "\uD83C\uDDE8\uD83C\uDDED"
    "SE" -> "\uD83C\uDDF8\uD83C\uDDEA"
    "PL" -> "\uD83C\uDDF5\uD83C\uDDF1"
    else -> "\uD83C\uDF10"
}
