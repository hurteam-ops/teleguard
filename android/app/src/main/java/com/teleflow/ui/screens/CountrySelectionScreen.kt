package com.teleflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleflow.ui.theme.*
import com.teleflow.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val proxies   by vm.proxies.collectAsState()
    val selected  by vm.selectedProxyId.collectAsState()
    val premium   by vm.isPremium.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(proxies, query) {
        if (query.isBlank()) proxies
        else proxies.filter {
            it.country.contains(query, true) ||
            it.ip.contains(query, true) ||
            it.city.contains(query, true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Location") },
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(TeleBlue),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text("Search countries or IPs…", fontSize = 15.sp, color = TextSecondary)
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "auto") {
                    ServerRow(
                        flag = "\u26A1",
                        name = "Automatic",
                        sub = "Best available server",
                        latency = 0,
                        isSelected = selected == "auto",
                        isLocked = false,
                        delay = 0,
                        onClick = {
                            vm.selectProxy("auto")
                            onBack()
                        }
                    )
                }

                if (filtered.isNotEmpty()) {
                    item(key = "header") {
                        Text(
                            "ALL LOCATIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 54.dp, top = 20.dp, bottom = 6.dp)
                        )
                    }
                }

                itemsIndexed(filtered, key = { _, p -> p.ip }) { i, proxy ->
                    val locked = !premium
                    ServerRow(
                        flag = flag(proxy.countryCode),
                        name = proxy.country.ifEmpty { proxy.ip },
                        sub = "${proxy.ip}:${proxy.port}",
                        latency = proxy.latency,
                        isSelected = selected == proxy.ip,
                        isLocked = locked,
                        delay = i * 40,
                        onClick = {
                            if (!locked) {
                                vm.selectProxy(proxy.ip)
                                onBack()
                            }
                        }
                    )
                }

                if (filtered.isEmpty() && query.isNotEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp), TextSecondary.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text("No results for \"$query\"", color = TextSecondary)
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
    isSelected: Boolean,
    isLocked: Boolean,
    delay: Int,
    onClick: () -> Unit
) {
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delay.toLong()); vis = true }

    AnimatedVisibility(
        vis,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 5 }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLocked, onClick = onClick)
                .background(if (isSelected) TeleBlue.copy(alpha = 0.05f) else MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) TeleBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(flag, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(sub, fontSize = 13.sp, color = TextSecondary)
            }

            when {
                isLocked -> Icon(Icons.Filled.Lock, null, Modifier.size(15.dp), Gold.copy(alpha = 0.6f))
                isSelected -> Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp), TeleBlue)
                latency > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        "${latency}ms",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
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

private val CircleShape = androidx.compose.foundation.shape.CircleShape
