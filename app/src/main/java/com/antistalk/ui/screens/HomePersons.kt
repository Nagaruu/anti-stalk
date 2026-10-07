package com.antistalk.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.antistalk.detection.OverlayManager
import com.antistalk.ui.MainViewModel

@Composable
fun HomeScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val persons by vm.persons.collectAsState()
    val apps by vm.apps.collectAsState()
    val diag by vm.detectionStatus.collectAsState()
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }

    // Re-check system permissions every time the user comes back —
    // no manual "check again" tap needed after granting in Settings.
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(tick) { vm.refreshDetectionStatus(ctx) }

    val subtitle = when {
        stats.total == 0 -> "Sạch sẽ. Giữ phong độ này. 😌"
        stats.continued > 0 -> "Vẫn tò mò à? Không sao, mai làm lại. 😏"
        stats.stopped > 0 -> "Nể đấy. Dừng được hết. 💪"
        else -> "Đã chặn vài lần, quyết định sau nhé."
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Hôm nay", style = MaterialTheme.typography.displayMedium)
            Text(subtitle, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        if (persons.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Chưa né ai cả. Sang tab Né ai thêm 1 người để bắt đầu. 😌",
                        modifier = Modifier.padding(18.dp), color = cs.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("${stats.total} lần muốn stalk", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${stats.stopped} lần bạn đã dừng · ${stats.continued} lần vẫn vào xem")
                    if (stats.topName.isNotBlank()) Text("Trigger nhiều nhất: ${stats.topName} (${stats.topCount})")
                }
            }
        }
        item {
            Text("Phát hiện có chạy không", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đọc màn hình: ${if (diag.serviceEnabled) "✅ đang bật" else "❌ chưa bật"}")
                    Text("Vẽ trên app khác: ${if (diag.canOverlay) "✅ đã cấp" else "❌ chưa cấp"}")
                    Text("Người đang né: ${diag.personCount} · Từ khóa: ${diag.keywordCount} · App bật: ${diag.enabledAppCount}")
                    if (!diag.serviceEnabled) {
                        Button(onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }) { Text("BẬT ĐỌC MÀN HÌNH") }
                    }
                    if (!diag.canOverlay) {
                        Button(onClick = { OverlayManager.openOverlaySettings(ctx) }) { Text("CẤP QUYỀN VẼ OVERLAY") }
                    }
                    if (!diag.serviceEnabled || !diag.canOverlay || diag.personCount == 0 || diag.enabledAppCount == 0) {
                        Text(
                            "Chưa đủ điều kiện nên màn hình cà khịa không hiện. Bật đủ rồi mở Facebook gõ tên người cần né nhé.",
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.testOverlay(ctx) }) { Text("TEST MÀN CÀ KHỊA") }
                        TextButton(onClick = { tick++; vm.refreshStats() }) { Text("Kiểm tra lại") }
                    }
                }
            }
        }
        item {
            Text("App đang theo dõi", style = MaterialTheme.typography.titleMedium)
        }
        items(apps, key = { it.packageName }) { app ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, fontWeight = FontWeight.Medium)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
                Switch(checked = app.enabled, onCheckedChange = { vm.toggleApp(app.packageName, app.label, it) })
            }
        }
        item {
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PersonsScreen(vm: MainViewModel, pendingGoal: String) {
    val persons by vm.persons.collectAsState()
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Người cần tránh", style = MaterialTheme.typography.headlineSmall)
            Text("Free: 1 người · 2 app. Thêm nữa tính sau.", color = cs.onSurfaceVariant)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị (VD: Nguyễn Văn A)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = extra, onValueChange = { extra = it }, label = { Text("Từ khóa thêm, cách nhau bằng dấu phẩy") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
                    val sug = remember(name) { vm.suggestedKeywords(name) }
                    if (name.isNotBlank()) Text(
                        "Tự nhận diện: ${sug.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                    )
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            vm.addPerson(name.trim(), note.trim(), pendingGoal, extra.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                            name = ""; note = ""; extra = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("THÊM NGƯỜI NÀY") }
                }
            }
        }
        items(persons, key = { it.id }) { p ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.displayName, fontWeight = FontWeight.Bold)
                        if (p.note.isNotBlank()) Text(p.note, fontSize = 13.sp)
                        Text(p.goal, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.deletePerson(p.id) }) { Text("Xóa") }
                }
            }
        }
    }
}
