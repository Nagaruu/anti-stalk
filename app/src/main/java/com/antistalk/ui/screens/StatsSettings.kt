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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
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
    }
}
