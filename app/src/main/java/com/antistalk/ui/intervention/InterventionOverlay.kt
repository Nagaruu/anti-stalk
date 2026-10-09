package com.antistalk.ui.intervention

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.core.RoastBank
import com.antistalk.detection.OverlayManager
import com.antistalk.ui.theme.AccentLavender
import com.antistalk.ui.theme.AccentLavenderLight
import com.antistalk.ui.theme.AccentTerracotta
import com.antistalk.ui.theme.AccentTerracottaLight
import com.antistalk.ui.theme.BrandHotPink
import com.antistalk.ui.theme.BrandMossGreenLight
import com.antistalk.ui.theme.BrandViolet
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.MintGreen
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageDark
import com.antistalk.ui.theme.WarmSageLight
import java.util.Locale

/**
 * Shared composable for the intervention card. The system overlay
 * (from AccessibilityService via OverlayManager) mirrors this look
 * with classic Views. Adaptable to both Concept A (Midnight Clarity)
 * and Concept B (Soft Sanity).
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
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val roast = remember(s.personName, s.countToday, roastLevel, vi) {
        RoastBank.pick(roastLevel, s.countToday.coerceAtLeast(1), s.personName, vi)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xD90D0B14) else Color(0xB82C2420)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1A1628) else Color(0xFFFFFFFF)
            ),
            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0x4D8B5CF6) else Color(0xFFE5DFD5)
            )
        ) {
            Column {
                // Top decorative accent line
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, BrandMossGreenLight, Color.Transparent)
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(WarmSageDark, WarmSage, AccentTerracotta)
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(if (isDark) "😏" else "🧠", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (vi) {
                            if (isDark) "Lại tìm người ta à?" else "Khoan đã bạn ơi!"
                        } else "Looking them up again?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        roast,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    // Counter Pill Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDark) Color(0x336EAA81) else AccentTerracottaLight
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0x4D6EAA81) else Color(0x4DE8795A),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (vi) "🔁 Lần thứ ${s.countToday.coerceAtLeast(1)} hôm nay · ${s.personName}"
                            else "🔁 #${s.countToday.coerceAtLeast(1)} today · ${s.personName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFD6EADd) else AccentTerracotta
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (step == 0) {
                        Button(
                            onClick = {
                                onDecision(s.eventId, "STOPPED", "")
                                onDone()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) BrandMossGreenLight else WarmSageDark,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                if (vi) {
                                    if (isDark) "Thôi, tôi đi ra" else "🌿 Thôi, tôi đi ra!"
                                } else "Nah, I'm out",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                if (vi) "Tôi vẫn muốn xem..." else "I still want to see...",
                                color = cs.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            if (vi) "Tại sao bạn muốn xem? (1 tap)"
                            else "Why do you want to look?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = cs.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            REASONS.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { r ->
                                        FilterChip(
                                            selected = reason == r,
                                            onClick = { reason = r },
                                            label = { Text(reasonLabel(r, vi), fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(999.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isDark) BrandMossGreenLight else WarmSageDark,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onDecision(s.eventId, "STOPPED", reason)
                                    onDone()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) BrandMossGreenLight else WarmSageDark,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(if (vi) "Đi ra" else "Leave", fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    onDecision(s.eventId, "CONTINUED", reason)
                                    onDone()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (vi) "Vẫn xem" else "Continue",
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (vi) "Không cấm bạn. Chỉ bắt bạn nghĩ một lần. 💙"
                        else "Not blocking you. Just one second of thinking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
