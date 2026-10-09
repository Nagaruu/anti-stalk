package com.antistalk.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.rotate
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

@Composable
fun HomeScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val persons by vm.persons.collectAsState()
    val apps by vm.apps.collectAsState()
    val diag by vm.detectionStatus.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // Resume = user may have just answered a roast overlay (STOPPED/CONTINUED
    // is written by the service, not by this VM), so stats must be re-read too.
    LaunchedEffect(tick) {
        vm.refreshDetectionStatus(ctx)
        vm.refreshStats()
    }

    val subtitle = when {
        stats.total == 0 -> "Sạch sẽ. Giữ phong độ này. 😌"
        stats.continued > 0 -> "Vẫn tò mò à? Không sao, mai làm lại. 😏"
        stats.stopped > 0 -> "Nể đấy. Dừng được hết. 💪"
        else -> "Đã chặn vài lần, quyết định sau nhé."
    }

    val disciplineRate = if (stats.total > 0) {
        ((stats.stopped * 100) / stats.total).coerceIn(0, 100)
    } else 100

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Header: Greeting & Mood Avatar ───────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isDark) "CHẾ ĐỘ BẢO VỆ TỐI" else "CHẾ ĐỘ TỰ NHIÊN",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isDark) "Hôm nay 🌙" else "Hôm nay 🌤️",
                        style = MaterialTheme.typography.displayMedium,
                        color = cs.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Avatar / Mood Ring
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(BrandViolet, BrandHotPink))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 20.sp)
                    }
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "mood_spin")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 8000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "angle"
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .rotate(angle)
                            .background(
                                Brush.sweepGradient(
                                    listOf(WarmSage, AccentTerracotta, AccentLavender, WarmSage)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // ─── If no persons to avoid ───────────────────────────────────────────
        if (persons.isEmpty()) {
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
                        Text("😌", fontSize = 32.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Chưa né ai cả. Sang tab Né ai thêm 1 người để bắt đầu kích hoạt bảo vệ nhé!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ─── Hero / Streak Banner (Concept A / B) ──────────────────────────────
        item {
            if (isDark) {
                // Concept A: Midnight Clarity Hero Glow Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0x388B5CF6), Color(0x22EC4899))
                            )
                        )
                        .border(1.dp, Color(0x4D8B5CF6), RoundedCornerShape(26.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            "LẦN MUỐN STALK HÔM NAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${stats.total}",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cs.onSurface,
                            lineHeight = 48.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (stats.topName.isNotBlank())
                                "Nhắc nhở nhiều nhất: ${stats.topName} (${stats.topCount} lần) 🏆"
                            else "Hệ thống đang hoạt động và bảo vệ bạn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Concept B: Soft Sanity Streak Banner (Sage Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(WarmSage, Color(0xFF406C4E))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            "LẦN MUỐN STALK HÔM NAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xD9FFFFFF),
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${stats.total}",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 48.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (stats.topName.isNotBlank())
                                "lần hôm nay · ${stats.topName} top 1 🏆"
                            else "Sạch sẽ! Chưa vi phạm lần nào 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xF2FFFFFF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ─── 3 Mini Stat Cards ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stopped Card
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💪",
                    valText = "${stats.stopped}",
                    label = "Đã dừng",
                    badgeText = "TỐT",
                    accentColor = if (isDark) MintGreen else WarmSage,
                    badgeBg = if (isDark) Color(0x3334D399) else WarmSageLight,
                    badgeFg = if (isDark) MintGreen else WarmSage,
                    isDark = isDark
                )

                // Continued Card
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "👀",
                    valText = "${stats.continued}",
                    label = "Vẫn xem",
                    badgeText = "ÔI",
                    accentColor = if (isDark) BrandHotPink else AccentTerracotta,
                    badgeBg = if (isDark) Color(0x33EC4899) else AccentTerracottaLight,
                    badgeFg = if (isDark) BrandHotPink else AccentTerracotta,
                    isDark = isDark
                )

                // Discipline Card
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🏅",
                    valText = "$disciplineRate%",
                    label = "Kỷ luật",
                    badgeText = "TỶ LỆ",
                    accentColor = if (isDark) cs.primary else WarmSage,
                    badgeBg = if (isDark) Color(0x338B5CF6) else WarmSageLight,
                    badgeFg = if (isDark) cs.primary else WarmSage,
                    isDark = isDark
                )
            }
        }

        // ─── Detection Status Card ────────────────────────────────────────────
        item {
            Text(
                "HỆ THỐNG PHÁT HIỆN",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
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
                    // Status Row: Accessibility Service
                    DetectionStatusRow(
                        icon = "🔍",
                        title = "Đọc màn hình",
                        isActive = diag.serviceEnabled,
                        activeLabel = "BẬT",
                        inactiveLabel = "TẮT",
                        isDark = isDark
                    )

                    // Status Row: Overlay Permission
                    DetectionStatusRow(
                        icon = "🖼️",
                        title = "Vẽ trên app khác",
                        isActive = diag.canOverlay,
                        activeLabel = "CẤP",
                        inactiveLabel = "CHƯA",
                        isDark = isDark
                    )

                    // Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${diag.personCount} người · ${diag.keywordCount} từ khóa · ${diag.enabledAppCount} app bật",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Fix buttons if missing permissions
                    if (!diag.serviceEnabled) {
                        Button(
                            onClick = {
                                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("BẬT ĐỌC MÀN HÌNH")
                        }
                    }

                    if (!diag.canOverlay) {
                        Button(
                            onClick = { OverlayManager.openOverlaySettings(ctx) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("CẤP QUYỀN VẼ OVERLAY")
                        }
                    }

                    if (!diag.serviceEnabled || !diag.canOverlay || diag.personCount == 0 || diag.enabledAppCount == 0) {
                        Text(
                            "Chưa đủ điều kiện nên màn hình cà khịa không hiện. Bật đủ rồi mở Facebook gõ tên người cần né nhé.",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { vm.testOverlay(ctx) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) BrandViolet else WarmSage
                            )
                        ) {
                            Text("🎯 TEST MÀN CÀ KHỊA", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { tick++; vm.refreshStats() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Kiểm tra lại")
                        }
                    }
                }
            }
        }

        // ─── Monitored Apps Section ───────────────────────────────────────────
        item {
            Text(
                "APP ĐANG THEO DÕI",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
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
                Column(modifier = Modifier.padding(14.dp)) {
                    apps.forEachIndexed { idx, app ->
                        val (appGradient, appIcon) = when {
                            app.packageName.contains("facebook.katana") -> FbGradient to "📘"
                            app.packageName.contains("instagram") -> IgGradient to "📷"
                            app.packageName.contains("orca") -> MsGradient to "💬"
                            app.packageName.contains("zalo") -> ZaloGradient to "🔵"
                            else -> DefaultAppGradient to "📱"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gradient icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(appGradient)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(appIcon, fontSize = 18.sp)
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = app.enabled,
                                onCheckedChange = { vm.toggleApp(app.packageName, app.label, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = if (isDark) BrandViolet else WarmSage
                                )
                            )
                        }

                        if (idx < apps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(if (isDark) cs.outlineVariant else cs.outline)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
        }
    }
}

// ─── Mini Stat Card Component ──────────────────────────────────────────────────
@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    valText: String,
    label: String,
    badgeText: String,
    accentColor: Color,
    badgeBg: Color,
    badgeFg: Color,
    isDark: Boolean
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) cs.outlineVariant else cs.outline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeFg
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                valText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant
            )
        }
    }
}

// ─── Detection Status Row Component ────────────────────────────────────────────
@Composable
private fun DetectionStatusRow(
    icon: String,
    title: String,
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    isDark: Boolean
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isActive) {
                        if (isDark) Color(0x3334D399) else WarmSageLight
                    } else {
                        if (isDark) Color(0x33F87171) else Color(0xFFFDE8E8)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                if (isActive) "✅ $activeLabel" else "❌ $inactiveLabel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) (if (isDark) MintGreen else WarmSage) else cs.error
            )
        }
    }
}

// ─── Persons Screen (Tab Né Ai - Block List Style) ──────────────────────────
@Composable
fun PersonsScreen(vm: MainViewModel, pendingGoal: String) {
    val persons by vm.persons.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var personToDelete by remember { mutableStateOf<com.antistalk.data.local.entity.AvoidedPerson?>(null) }

    // Dialog: Thêm người cần né
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                name = ""; extra = ""; note = ""
            },
            title = {
                Text(
                    "Thêm người cần né",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Trường 1: Tên hiển thị (Bắt buộc)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên người cần né *") },
                        placeholder = { Text("VD: Nguyễn Văn A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                "Tên hiển thị trong cảnh báo và để hệ thống tự động sinh từ khóa quét cơ bản.",
                                fontSize = 11.sp
                            )
                        }
                    )

                    // Gợi ý từ khóa tự động
                    val sug = remember(name) { vm.suggestedKeywords(name) }
                    if (name.isNotBlank() && sug.isNotEmpty()) {
                        Column {
                            Text(
                                "Gợi ý tự động nhận diện:",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sug.take(3).forEach { s ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isDark) cs.surfaceVariant else WarmSageLight)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            s,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) cs.onSurfaceVariant else WarmSage
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Trường 2: Từ khóa bổ sung (Tùy chọn)
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text("Từ khóa bổ sung (tùy chọn)") },
                        placeholder = { Text("VD: biệt danh, nick FB, nickname...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                "Thêm từ khóa nhận diện riêng khi tìm kiếm trên FB/IG/Zalo (cách nhau dấu phẩy).",
                                fontSize = 11.sp
                            )
                        }
                    )

                    // Trường 3: Lời nhắc bản thân (Tùy chọn)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Lời nhắc bản thân (tùy chọn)") },
                        placeholder = { Text("VD: Đừng vào xem nữa, tập trung cho bản thân") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                "Lời nhắn hiện trên màn hình can thiệp khi bạn chuẩn bị stalk người này.",
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.addPerson(
                            name.trim(),
                            note.trim(),
                            pendingGoal,
                            extra.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        showAddDialog = false
                        name = ""; note = ""; extra = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) BrandViolet else WarmSage
                    )
                ) {
                    Text("LƯU VÀO DANH SÁCH", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        name = ""; note = ""; extra = ""
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Xác nhận xóa người né
    if (personToDelete != null) {
        val target = personToDelete!!
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = {
                Text(
                    "Xóa khỏi danh sách né?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Bạn có chắc muốn xóa \"${target.displayName}\"? Hệ thống sẽ ngừng phát hiện người này.\n\n(Bạn có thể bấm \"Hoàn tác\" ngay sau khi xóa để khôi phục lại)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        personToDelete = null
                        vm.deletePersonWithUndo(target) { restoreAction ->
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Đã xóa \"${target.displayName}\"",
                                    actionLabel = "HOÀN TÁC",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    restoreAction()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xác nhận xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if (isDark) BrandViolet else Color(0xFF1E88E5),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm người cần né")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "BLOCK LIST",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Danh sách né", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "${persons.size} người đang được theo dõi và bảo vệ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) BrandViolet else Color(0xFF1E88E5))
                    ) {
                        Text("+ Thêm", fontWeight = FontWeight.Bold, color = if (isDark) BrandViolet else Color(0xFF1E88E5))
                    }
                }
            }

            // Clean list of persons matching the reference image
            items(persons, key = { it.id }) { p ->
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar circle (matches the blue avatar circle in reference screenshot)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isDark) listOf(BrandViolet, BrandHotPink)
                                        else listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                            if (p.note.isNotBlank()) {
                                Text(
                                    p.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            } else if (p.goal.isNotBlank()) {
                                Text(
                                    "Mục tiêu: ${p.goal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) BrandHotPink else AccentTerracotta
                                )
                            }
                        }

                        // Subtle block / remove icon button (matches reference image's ⊘ icon!)
                        IconButton(
                            onClick = { personToDelete = p }
                        ) {
                            Text(
                                "⊘",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (persons.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showAddDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🛡️", fontSize = 42.sp)
                            Text(
                                "Chưa có ai trong danh sách né",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bấm nút \"+\" bên dưới để thêm người đầu tiên bạn muốn tránh stalk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) BrandViolet else Color(0xFF1E88E5))
                            ) {
                                Text("+ Thêm người cần né", color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        "Đã từng xuất file sao lưu? Vào Cài đặt → Dữ liệu → Khôi phục để lấy người né và từ khóa về.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
