package com.antistalk.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antistalk.detection.OverlayManager
import com.antistalk.detection.StalkAccessibilityService
import com.antistalk.ui.theme.AccentTerracotta
import com.antistalk.ui.theme.BrandHotPink
import com.antistalk.ui.theme.BrandViolet
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.MintGreen
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageLight

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    // Auto re-check whenever the user returns from system Settings —
    // no need to tap "check again" manually.
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val accOn = remember(tick) { StalkAccessibilityService.isEnabled(ctx) }
    val overlayOn = remember(tick) { OverlayManager.canDrawOverlays(ctx) }
    val allGranted = accOn && overlayOn

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) cs.surfaceVariant else WarmSageLight)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "BƯỚC THIẾT LẬP BẮT BUỘC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isDark) BrandHotPink else WarmSage
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cấp quyền để bảo vệ",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Anti-Stalk chỉ hoạt động hiệu quả khi được cấp 2 quyền cốt lõi sau:",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }

        // Permission Card 1: Accessibility
        PermissionCard(
            title = "1. Trợ năng (Accessibility)",
            description = "Cho phép app nhận diện khi bạn gõ tên người cần né trong thanh tìm kiếm.",
            isGranted = accOn,
            icon = "👁️",
            buttonLabel = if (accOn) "Đã cấp quyền ✓" else "Mở cài đặt trợ năng",
            onAction = {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        )

        // Permission Card 2: Overlay
        PermissionCard(
            title = "2. Hiển thị trên ứng dụng khác",
            description = "Cho phép hiện màn hình 'cà khịa' nhắc nhở ngay khi phát hiện hành vi stalk.",
            isGranted = overlayOn,
            icon = "🪟",
            buttonLabel = if (overlayOn) "Đã cấp quyền ✓" else "Cấp quyền hiển thị",
            onAction = { OverlayManager.openOverlaySettings(ctx) }
        )

        // Status Feedback Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (allGranted) {
                    if (isDark) Color(0x2234D399) else WarmSageLight
                } else {
                    if (isDark) cs.surfaceVariant else Color(0xFFFBF4ED)
                }
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (allGranted) {
                    if (isDark) Color(0x4D34D399) else Color(0x665B8A6A)
                } else {
                    if (isDark) cs.outlineVariant else cs.outline
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (allGranted) "🎉" else "😏", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (allGranted) "Tuyệt vời! Đã đủ quyền để kích hoạt Anti-Stalk."
                    else "Bật đủ cả 2 quyền mới tiếp tục được nhé bạn thân.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (allGranted) {
                        if (isDark) MintGreen else WarmSage
                    } else cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Continue Button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = allGranted,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandViolet else WarmSage
            )
        ) {
            Text(
                "Tiếp tục",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
        }

        // Re-check text button
        TextButton(
            onClick = { tick++ },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "🔄 Kiểm tra lại trạng thái",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: String,
    buttonLabel: String,
    onAction: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme

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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) cs.surfaceVariant else WarmSageLight
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isGranted) {
                                if (isDark) Color(0x3334D399) else WarmSageLight
                            } else {
                                if (isDark) Color(0x33F87171) else Color(0xFFFDE8E8)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (isGranted) "✅ ĐÃ BẬT" else "❌ CHƯA BẬT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) (if (isDark) MintGreen else WarmSage) else cs.error
                    )
                }
            }

            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isGranted) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandViolet else WarmSage
                    )
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) MintGreen else WarmSage
                    )
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
