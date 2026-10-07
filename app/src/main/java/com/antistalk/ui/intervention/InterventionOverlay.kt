package com.antistalk.ui.intervention

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.core.RoastBank
import com.antistalk.detection.OverlayManager
import java.util.Locale

/**
 * Shared composable for the intervention card. The system overlay
 * (from AccessibilityService via OverlayManager) mirrors this look
 * with classic Views. Keep it dependency-light on purpose.
 */
@Composable
fun InterventionOverlay(
    state: InterventionState? = OverlayManager.current,
    vi: Boolean = Locale.getDefault().language == "vi",
    roastLevel: Int = 2,
    onDecision: (eventId: Long, decision: String, reason: String) -> Unit = { _, _, _ -> },
    onDone: () -> Unit = {}
) {
    val s = state ?: return
    var reason by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) } // 0 = roast, 1 = why
    val roast = remember(s.personName, s.countToday, roastLevel, vi) {
        RoastBank.pick(roastLevel, s.countToday.coerceAtLeast(1), s.personName, vi)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("😏", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (vi) "Lại tìm người ta à?" else "Looking them up again?",
                    fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF14101F)
                )
                Spacer(Modifier.height(8.dp))
                Text(roast, fontSize = 16.sp, color = Color(0xFF4A4458))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (vi) "Lần thứ ${s.countToday.coerceAtLeast(1)} hôm nay · ${s.personName}"
                    else "#${s.countToday.coerceAtLeast(1)} today · ${s.personName}",
                    fontSize = 13.sp, color = Color(0xFF8A8496)
                )
                Spacer(Modifier.height(12.dp))

                if (step == 0) {
                    Button(
                        onClick = {
                            onDecision(s.eventId, "STOPPED", "")
                            step = 1
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF))
                    ) { Text(if (vi) "THÔI, TÔI ĐI RA" else "NAH, I'M OUT") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (vi) "TÔI VẪN MUỐN XEM" else "I STILL WANT TO SEE") }
                } else {
                    Text(
                        if (vi) "Tại sao bạn muốn xem? (1 tap, bỏ qua được)"
                        else "Why do you want to look? (1 tap, skippable)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        REASONS.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { r ->
                                    FilterChip(
                                        selected = reason == r,
                                        onClick = { reason = r },
                                        label = { Text(reasonLabel(r, vi)) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onDecision(s.eventId, "STOPPED", reason)
                            onDone()
                        }) { Text(if (vi) "Đi ra" else "Leave") }
                        TextButton(onClick = {
                            onDecision(s.eventId, "CONTINUED", reason)
                            onDone()
                        }) { Text(if (vi) "Vẫn xem" else "Continue") }
                    }
                }
                Text(
                    if (vi) "Không cấm bạn. Chỉ bắt bạn nghĩ một lần."
                    else "Not blocking you. Just one second of thinking.",
                    fontSize = 12.sp, color = Color(0xFF8A8496),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
