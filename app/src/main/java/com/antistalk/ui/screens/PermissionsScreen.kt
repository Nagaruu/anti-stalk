package com.antistalk.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.detection.OverlayManager
import com.antistalk.detection.StalkAccessibilityService

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_EXPRESSION")
    tick.let { }
    val accOn = remember(tick) { StalkAccessibilityService.isEnabled(ctx) }
    val overlayOn = remember(tick) { OverlayManager.canDrawOverlays(ctx) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Cấp quyền để bắt quả tang", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text("1) Accessibility: thấy tên bạn gõ trong app đã chọn.\n2) Vẽ trên app khác: hiện màn hình cà khịa đúng lúc.")
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Accessibility: ${if (accOn) "✅ đã bật" else "❌ chưa bật"}")
                Button(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }) { Text("MỞ CÀI ĐẶT ACCESSIBILITY") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Vẽ trên ứng dụng khác: ${if (overlayOn) "✅ đã bật" else "❌ chưa bật"}")
                Button(onClick = { OverlayManager.openOverlaySettings(ctx) }) { Text("CẤP QUYỀN OVERLAY") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { tick++ }, modifier = Modifier.fillMaxWidth()) { Text("KIỂM TRA LẠI") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDone, modifier = Modifier.fillMaxWidth(),
            enabled = accOn && overlayOn
        ) { Text("TIẾP TỤC") }
        if (!(accOn && overlayOn)) {
            Spacer(Modifier.height(8.dp))
            Text("Bật đủ 2 quyền mới tiếp tục được nhé bạn thân. 😏", fontSize = 13.sp)
        }
    }
}
