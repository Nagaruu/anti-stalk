package com.antistalk.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.antistalk.ui.MainViewModel
import com.antistalk.ui.theme.AccentLavender
import com.antistalk.ui.theme.AccentLavenderLight
import com.antistalk.ui.theme.AccentTerracotta
import com.antistalk.ui.theme.AccentTerracottaLight
import com.antistalk.ui.theme.BrandHotPink
import com.antistalk.ui.theme.BrandViolet
import com.antistalk.ui.theme.DefaultAppGradient
import com.antistalk.ui.theme.FbGradient
import com.antistalk.ui.theme.IgGradient
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.MintGreen
import com.antistalk.ui.theme.MsGradient
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageLight
import com.antistalk.ui.theme.ZaloGradient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val events by vm.events.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    // Log rows span days (recent = up to 200 events), while the summary cards
    // are strictly "today" — without the date a row from yesterday looks current.
    val fmtWithDate = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val disciplineRate = if (stats.total > 0) {
        ((stats.stopped * 100) / stats.total).coerceIn(0, 100)
    } else 100

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "TỔNG KẾT HÔM NAY",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text("Thống kê", style = MaterialTheme.typography.displayMedium)
            Text(
                "Không biến bạn thành bệnh nhân. Chỉ đếm sự thật 😌",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }

        // Hero Discipline Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (isDark) {
                            Brush.linearGradient(listOf(Color(0x388B5CF6), Color(0x22EC4899)))
                        } else {
                            Brush.linearGradient(listOf(WarmSage, Color(0xFF406C4E)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isDark) Color(0x4D8B5CF6) else Color.Transparent,
                        RoundedCornerShape(26.dp)
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        "TỶ LỆ GIỮ MÌNH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xD9FFFFFF),
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$disciplineRate%",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 48.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    val dismissed = stats.dismissed
                    val detailText = buildString {
                        append("${stats.total} lần kích hoạt · ${stats.stopped} dừng được · ${stats.continued} vẫn xem")
                        if (dismissed > 0) {
                            append(" · $dismissed chưa chọn")
                        }
                    }
                    Text(
                        detailText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xF2FFFFFF)
                    )
                }
            }
        }

        if (events.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌟", fontSize = 32.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Chưa ghi nhận lần can thiệp nào. Sạch sẽ tuyệt đối! 😌",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "NHẬT KÝ CAN THIỆP GẦN ĐÂY",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(events.take(50)) { e ->
            val (appGradient, appIcon) = when {
                e.packageName.contains("facebook.katana") -> FbGradient to "📘"
                e.packageName.contains("instagram") -> IgGradient to "📷"
                e.packageName.contains("orca") -> MsGradient to "💬"
                e.packageName.contains("zalo") -> ZaloGradient to "🔵"
                else -> DefaultAppGradient to "📱"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) cs.outlineVariant else cs.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(appGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(appIcon, fontSize = 16.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            e.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Text(
                            "${e.triggerType} · " +
                                if (e.createdAt >= todayStart) fmt.format(Date(e.createdAt))
                                else fmtWithDate.format(Date(e.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    // Decision Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                when (e.decision) {
                                    "STOPPED" -> if (isDark) Color(0x3334D399) else WarmSageLight
                                    "CONTINUED" -> if (isDark) Color(0x33EC4899) else AccentTerracottaLight
                                    else -> cs.surfaceVariant
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            when (e.decision) {
                                "STOPPED" -> "🛑 Đã dừng"
                                "CONTINUED" -> "👀 Vẫn xem"
                                else -> "…"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (e.decision) {
                                "STOPPED" -> if (isDark) MintGreen else WarmSage
                                "CONTINUED" -> if (isDark) BrandHotPink else AccentTerracotta
                                else -> cs.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
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
    val eventLog by vm.eventLog.collectAsState()
    val kwsPreview by vm.keywordsPreview.collectAsState()
    val checkFailed by vm.updateCheckFailed.collectAsState()
    val signature by vm.signature.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var diagTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) diagTick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(diagTick) {
        vm.refreshEventLog()
        vm.refreshSignature(ctx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "TÙY CHỈNH ỨNG DỤNG",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text("Cài đặt", style = MaterialTheme.typography.displayMedium)
        }

        // ─── Theme Mode Section ───────────────────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Giao diện (Hybrid Theme)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "light" to "☀️ Sáng (Soft Sanity)",
                        "dark" to "🌙 Tối (Midnight)",
                        "system" to "📱 Hệ thống"
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = themeMode == m,
                            onClick = { vm.setThemeMode(m) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) BrandViolet else WarmSage,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        "💡 Hybrid Mode: Tự động chuyển đổi Soft Sanity (ấm áp, khích lệ) ban ngày và Midnight Clarity (riêng tư, tập trung) ban đêm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // ─── Roast Level Section ──────────────────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Mức độ cà khịa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        1 to "L1 — Nhẹ nhàng 😌",
                        2 to "L2 — Bạn thân 😏 (mặc định)",
                        3 to "L3 — Cà khịa 😈",
                        4 to "L4 — Tàn nhẫn nhưng văn minh 💀"
                    ).forEach { (lv, label) ->
                        FilterChip(
                            selected = level == lv,
                            onClick = { vm.setRoastLevel(lv) },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) BrandViolet else WarmSage,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // ─── Updates & Version ────────────────────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cập nhật ứng dụng",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Phiên bản hiện tại: ${vm.versionLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chỉ tải update qua Wi-Fi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Tắt để cho tải bằng 4G", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { vm.setUpdateWifiOnly(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isDark) BrandViolet else WarmSage
                        )
                    )
                }

                when (val s = updState) {
                    is MainViewModel.UpdateState.Downloading ->
                        Text(
                            "Đang tải bản ${s.tag} trong nền… cứ dùng app bình thường.",
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                        )
                    is MainViewModel.UpdateState.NeedsMigration ->
                        Button(
                            onClick = { vm.showMigrationAgain() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("XEM HƯỚNG DẪN CHUYỂN ĐỔI BẢN ${s.info.tag}")
                        }
                    is MainViewModel.UpdateState.ReadyToInstall ->
                        Button(
                            onClick = { vm.openDownloadedInstaller(ctx) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("CÀI ĐẶT BẢN ${s.info.tag}")
                        }
                    else -> Unit
                }

                OutlinedButton(
                    onClick = { vm.checkUpdate(ctx, force = true) },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (checking) "ĐANG KIỂM TRA…" else "KIỂM TRA CẬP NHẬT")
                }

                if (checkFailed) {
                    Text(
                        "⚠️ Không kiểm tra được cập nhật (mất mạng hoặc repo chưa có release mới).",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.error
                    )
                }
            }
        }

        // ─── Signing certificate diagnostics ─────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Chữ ký cài đặt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val sig = signature
                if (sig == null) {
                    Text("Đang đọc chữ ký…", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                } else {
                    Text(
                        "Bản đang cài: ${com.antistalk.core.SigningInfo.shortSha(sig.installedSha)}",
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                    )
                    Text(
                        "Khoá chính thức: ${com.antistalk.core.SigningInfo.shortSha(sig.expectedSha)}",
                        style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                    )
                    if (sig.isReleaseKey) {
                        Text(
                            "✓ Khớp khoá chính thức — cập nhật chạy 1 chạm.",
                            style = MaterialTheme.typography.labelSmall, color = WarmSage
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(cs.error.copy(alpha = 0.08f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "⚠️ Bản đang cài được ký bằng khoá cũ (build trước khi có khoá ổn định). " +
                                    "Android sẽ từ chối mọi bản cập nhật mới — chỉ cần chuyển đổi 1 lần là xong. " +
                                    "Nếu đang có bản cập nhật, bấm nút hướng dẫn trong mục Cập nhật ứng dụng.",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.error
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { vm.refreshSignature(ctx) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("KIỂM TRA LẠI CHỮ KÝ")
                }
            }
        }

        // ─── Backup / restore ─────────────────────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp)) {
                BackupSection(vm)
            }
        }

        // ─── Danger Zone: Wipe Data ───────────────────────────────────────────
        var showWipeConfirm by remember { mutableStateOf(false) }
        if (showWipeConfirm) {
            AlertDialog(
                onDismissRequest = { showWipeConfirm = false },
                title = { Text("Xác nhận xóa dữ liệu?") },
                text = { Text("Tất cả người cần né, từ khóa, nhật ký và thống kê stalk sẽ bị xóa hoàn toàn khỏi máy. Bạn có chắc không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showWipeConfirm = false
                            vm.wipeAll()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.error)
                    ) { Text("XÓA HẾT") }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirm = false }) { Text("HỦY") }
                }
            )
        }

        OutlinedButton(
            onClick = { showWipeConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error)
        ) {
            Text("XÓA TOÀN BỘ DỮ LIỆU LOCAL")
        }

        // ─── Debug Diagnostics ────────────────────────────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chẩn đoán phát hiện (Debug Log)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Từ khóa: ${if (kwsPreview.isEmpty()) "— chưa có —" else kwsPreview.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.surfaceVariant)
                        .padding(10.dp)
                ) {
                    if (eventLog.isEmpty()) {
                        Text(
                            "Chưa có event nào. Mở Facebook gõ tên người cần né rồi quay lại bấm Tải lại log.",
                            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            eventLog.take(15).forEach { e ->
                                Text(vm.eventLogLine(e), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { vm.refreshEventLog() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("TẢI LẠI LOG")
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
