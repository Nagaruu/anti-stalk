package com.antistalk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onDone: (goal: String) -> Unit) {
    var goal by remember { mutableStateOf("Người yêu cũ") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("🛡️", fontSize = 48.sp)
        Text("Anti-Stalk", fontSize = 36.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            "Không cấm bạn. Chỉ bắt bạn suy nghĩ một lần trước khi stalk người yêu cũ / crush.",
            fontSize = 16.sp, color = Color(0xFF4A4458)
        )
        Spacer(Modifier.height(16.dp))
        Text("Bạn muốn ngừng stalk ai?", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        listOf("Người yêu cũ", "Crush", "Người tôi muốn ngừng stalk", "Khác").chunked(2).forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { g ->
                    FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text(g) })
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "App dùng Accessibility để thấy TÊN bạn gõ trong app đã chọn. Không đọc tin nhắn, không cần login, xử lý trên máy.",
            fontSize = 13.sp, color = Color(0xFF6B6577)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDone(goal) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF))
        ) { Text("BẮT ĐẦU") }
    }
}
