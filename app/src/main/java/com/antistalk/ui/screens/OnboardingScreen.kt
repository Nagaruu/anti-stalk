package com.antistalk.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.antistalk.ui.theme.AccentTerracotta
import com.antistalk.ui.theme.AccentTerracottaLight
import com.antistalk.ui.theme.BrandHotPink
import com.antistalk.ui.theme.BrandViolet
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.MintGreen
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageLight

@Composable
fun OnboardingScreen(onDone: (goal: String) -> Unit) {
    var goal by remember { mutableStateOf("Người yêu cũ") }
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    val goals = listOf(
        "Người yêu cũ" to "💔",
        "Crush" to "🫣",
        "Người muốn ngừng stalk" to "🛑",
        "Khác" to "✨"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero Icon / Badge
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (isDark) listOf(BrandViolet, BrandHotPink)
                        else listOf(WarmSage, AccentTerracotta)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡️", fontSize = 38.sp)
        }

        // Header Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) cs.surfaceVariant else WarmSageLight)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "BẢO VỆ TÂM LÝ & SỰ BÌNH YÊN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isDark) BrandHotPink else WarmSage
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Anti-Stalk",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Không cấm bạn. Chỉ nhắc bạn dừng lại 5 giây để suy nghĩ trước khi bước vào hố sâu tò mò.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Card: Goal Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) cs.outlineVariant else cs.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Bạn muốn ngừng stalk ai nhất?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { (g, emoji) ->
                        val isSelected = goal == g
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) BrandViolet else WarmSage
                            } else {
                                if (isDark) cs.outlineVariant else cs.outline
                            },
                            label = "border_color"
                        )
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color(0x338B5CF6) else WarmSageLight
                            } else {
                                if (isDark) cs.surfaceVariant else cs.surface
                            },
                            label = "bg_color"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .clickable { goal = g }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    g,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) BrandHotPink else WarmSage
                                    } else cs.onSurface
                                )
                            }
                            if (isSelected) {
                                Text("✓", fontWeight = FontWeight.Bold, color = if (isDark) BrandViolet else WarmSage)
                            }
                        }
                    }
                }
            }
        }

        // Privacy Guarantee Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x1A34D399) else WarmSageLight
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0x3334D399) else Color(0x4D5B8A6A)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("🔒", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "An toàn & 100% riêng tư",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isDark) MintGreen else WarmSage
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Anti-Stalk chỉ đối chiếu tên trong danh sách né của bạn trực tiếp trên thiết bị. Không đọc tin nhắn, không cần tài khoản và không gửi bất kỳ dữ liệu nào lên internet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Action Button
        Button(
            onClick = { onDone(goal) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandViolet else WarmSage
            )
        ) {
            Text(
                "Bắt đầu ngay →",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
        }
    }
}
