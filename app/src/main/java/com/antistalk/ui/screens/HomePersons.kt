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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.ui.MainViewModel
import com.antistalk.ui.intervention.InterventionOverlay
import com.antistalk.ui.intervention.InterventionState

@Composable
fun HomeScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val persons by vm.persons.collectAsState()
    val apps by vm.apps.collectAsState()
    val roastLvl by vm.roastLevel.collectAsState()
    var preview by remember { mutableStateOf<InterventionState?>(null) }

    if (preview != null) {
        InterventionOverlay(
            state = preview, roastLevel = roastLvl,
            onDecision = { _, _, _ -> }, onDone = { preview = null; vm.refreshStats() }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Hôm nay", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Tiến bộ đấy. 😏", color = androidx.compose.ui.graphics.Color.Gray)
            Spacer(Modifier.height(4.dp))
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
            Text("App đang theo dõi", fontWeight = FontWeight.SemiBold)
        }
        items(apps, key = { it.packageName }) { app ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, fontWeight = FontWeight.Medium)
                    Text(app.packageName, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                }
                Switch(checked = app.enabled, onCheckedChange = { vm.toggleApp(app.packageName, app.label, it) })
            }
        }
        item {
            Text("Test ngay (không cần mở Facebook)", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val name = persons.firstOrNull()?.displayName ?: "Nguyễn Văn A"
                    vm.previewTrigger(name, "com.facebook.katana")
                    preview = InterventionState(-1, name, "com.facebook.katana", "MANUAL", "LOW", stats.total + 1)
                }) { Text("GIẢ LẬP STALK") }
                TextButton(onClick = { vm.refreshStats() }) { Text("Tải lại") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PersonsScreen(vm: MainViewModel, pendingGoal: String) {
    val persons by vm.persons.collectAsState()
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Người cần tránh", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Free: 1 người · 2 app. Thêm nữa tính sau.", color = androidx.compose.ui.graphics.Color.Gray)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị (VD: Nguyễn Văn A)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = extra, onValueChange = { extra = it }, label = { Text("Từ khóa thêm, cách nhau bằng dấu phẩy") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú (tùy chọn)") }, modifier = Modifier.fillMaxWidth())
                    val sug = remember(name) { vm.suggestedKeywords(name) }
                    if (name.isNotBlank()) Text("Tự nhận diện: ${sug.joinToString(", ")}", fontSize = 12.sp)
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
                        Text(p.goal, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                    }
                    TextButton(onClick = { vm.deletePerson(p.id) }) { Text("Xóa") }
                }
            }
        }
    }
}
