package com.antistalk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antistalk.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val events by vm.events.collectAsState()
    val cs = MaterialTheme.colorScheme
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Thống kê", style = MaterialTheme.typography.headlineSmall)
            Text("Không biến bạn thành bệnh nhân. Chỉ đếm sự thật. 😌", color = cs.onSurfaceVariant)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("${stats.total} muốn stalk / ${stats.stopped} dừng / ${stats.continued} vẫn xem")
                    if (stats.topName.isNotBlank()) Text("Hay stalk nhất: ${stats.topName}")
                }
            }
        }
        if (events.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Chưa có lần nào hôm nay. Sạch sẽ! 😌",
                        modifier = Modifier.padding(18.dp), color = cs.onSurfaceVariant
                    )
                }
            }
        }
        items(events.take(50)) { e ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.personName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${e.packageName} · ${e.triggerType} · ${fmt.format(Date(e.createdAt))}",
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                        )
                    }
                    Text(
                        when (e.decision) { "STOPPED" -> "🛑" "CONTINUED" -> "👀" else -> "…" },
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val level by vm.roastLevel.collectAsState()
    val checking by vm.checkingUpdate.collectAsState()
    val updState by vm.updateState.collectAsState()
    val wifiOnly by vm.updateWifiOnly.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val eventLog by vm.eventLog.collectAsState()
    val kwsPreview by vm.keywordsPreview.collectAsState()
    val checkFailed by vm.updateCheckFailed.collectAsState()
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var diagTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                diagTick++
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(diagTick) { vm.refreshEventLog() }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cài đặt", style = MaterialTheme.typography.headlineSmall)
        Text("Giao diện", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("light" to "☀️ Sáng", "dark" to "🌙 Tối", "system" to "📱 Hệ thống").forEach { (m, label) ->
                FilterChip(selected = themeMode == m, onClick = { vm.setThemeMode(m) }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Mức độ cà khịa", style = MaterialTheme.typography.titleMedium)
        listOf(
            1 to "L1 — Nhẹ nhàng",
            2 to "L2 — Bạn thân (mặc định)",
            3 to "L3 — Cà khịa",
            4 to "L4 — Tàn nhẫn nhưng không độc hại"
        ).forEach { (lv, label) ->
            FilterChip(selected = level == lv, onClick = { vm.setRoastLevel(lv) }, label = { Text(label) })
        }
        Spacer(Modifier.height(8.dp))
        Text("Phiên bản: ${vm.versionLabel}", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Chỉ tải update qua Wi-Fi", fontWeight = FontWeight.Medium)
                Text("Tắt để cho tải bằng 4G", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Switch(checked = wifiOnly, onCheckedChange = { vm.setUpdateWifiOnly(it) })
        }
        when (val s = updState) {
            is MainViewModel.UpdateState.Downloading ->
                Text(
                    "Đang tải bản ${s.tag} trong nền… cứ dùng app bình thường.",
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                )
            is MainViewModel.UpdateState.ReadyToInstall ->
                OutlinedButton(onClick = { vm.openDownloadedInstaller(ctx) }) { Text("CÀI ĐẶT BẢN ${s.info.tag}") }
            else -> Unit
        }
        OutlinedButton(
            onClick = { vm.checkUpdate(ctx, force = true) },
            enabled = !checking
        ) { Text(if (checking) "ĐANG KIỂM TRA…" else "KIỂM TRA CẬP NHẬT") }
        if (checkFailed) {
            Text(
                "⚠️ Không kiểm tra được cập nhật (mất mạng, hoặc repo GitHub chưa Public / chưa có Release).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Ngôn ngữ theo máy (vi/en có sẵn). Privacy: dữ liệu chỉ trên máy.",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.wipeAll() }) { Text("XÓA TOÀN BỘ DỮ LIỆU LOCAL") }
        Spacer(Modifier.height(8.dp))
        Text(
            "MVP sideload — chưa cần Play review. Khi lên Play: thêm prominent disclosure + video demo.",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text("Chẩn đoán detect (debug)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Từ khóa đang theo dõi: ${if (kwsPreview.isEmpty()) "— chưa có —" else kwsPreview.joinToString(", ")}",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (eventLog.isEmpty()) {
                    Text(
                        "Chưa thấy event nào. Mở Facebook gõ vài chữ rồi quay lại bấm Tải lại log.",
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                    )
                } else {
                    eventLog.take(15).forEach { e ->
                        Text(vm.eventLogLine(e), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text(
            "Log chỉ lưu trên máy, mất khi tắt app. Dòng match/cooldown cho biết service có thấy chữ bạn gõ không.",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
        )
        OutlinedButton(onClick = { vm.refreshEventLog() }) { Text("TẢI LẠI LOG") }
    }
}
