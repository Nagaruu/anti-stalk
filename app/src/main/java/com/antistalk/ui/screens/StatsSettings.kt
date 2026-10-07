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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Thống kê", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Không biến bạn thành bệnh nhân. Chỉ đếm sự thật. 😌")
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("${stats.total} muốn stalk / ${stats.stopped} dừng / ${stats.continued} vẫn xem")
                    if (stats.topName.isNotBlank()) Text("Hay stalk nhất: ${stats.topName}")
                }
            }
        }
        items(events.take(50)) { e ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.personName, fontWeight = FontWeight.SemiBold)
                        Text("${e.packageName} · ${e.triggerType} · ${fmt.format(Date(e.createdAt))}", fontSize = 12.sp, color = Color.Gray)
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
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cài đặt", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Mức độ cà khịa", fontWeight = FontWeight.SemiBold)
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
                Text("Tắt để cho tải bằng 4G", fontSize = 12.sp, color = Color.Gray)
            }
            Switch(checked = wifiOnly, onCheckedChange = { vm.setUpdateWifiOnly(it) })
        }
        when (val s = updState) {
            is MainViewModel.UpdateState.Downloading ->
                Text("Đang tải bản ${s.tag} trong nền… cứ dùng app bình thường.", fontSize = 13.sp, color = Color.Gray)
            is MainViewModel.UpdateState.ReadyToInstall ->
                OutlinedButton(onClick = { vm.openDownloadedInstaller(ctx) }) { Text("CÀI ĐẶT BẢN ${s.info.tag}") }
            else -> Unit
        }
        OutlinedButton(
            onClick = { vm.checkUpdate(ctx, force = true) },
            enabled = !checking
        ) { Text(if (checking) "ĐANG KIỂM TRA…" else "KIỂM TRA CẬP NHẬT") }
        Spacer(Modifier.height(8.dp))
        Text("Ngôn ngữ theo máy (vi/en có sẵn). Privacy: dữ liệu chỉ trên máy.", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.wipeAll() }) { Text("XÓA TOÀN BỘ DỮ LIỆU LOCAL") }
        Spacer(Modifier.height(8.dp))
        Text("MVP sideload — chưa cần Play review. Khi lên Play: thêm prominent disclosure + video demo.", fontSize = 12.sp, color = Color.Gray)
    }
}
