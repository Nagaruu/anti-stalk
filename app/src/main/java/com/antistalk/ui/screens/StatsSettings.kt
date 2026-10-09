package com.antistalk.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antistalk.data.local.entity.StalkEvent
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

private fun friendlyTrigger(trigger: String): String = when (trigger.uppercase(Locale.ROOT)) {
    "SEARCH_INPUT" -> "Gõ tìm kiếm"
    "SEARCH_SUBMITTED" -> "Nhấn tìm kiếm"
    "PROFILE_TITLE" -> "Xem trang cá nhân"
    "MANUAL" -> "Thủ công"
    else -> "Phát hiện"
}

private fun triggerIcon(trigger: String): String = when (trigger.uppercase(Locale.ROOT)) {
    "SEARCH_INPUT" -> "🔍"
    "SEARCH_SUBMITTED" -> "⌨️"
    "PROFILE_TITLE" -> "👤"
    "MANUAL" -> "⚙️"
    else -> "🎯"
}

@Composable
fun MiniSparkline(
    color: Color,
    modifier: Modifier = Modifier,
    curveType: Int = 0
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val path = Path().apply {
            when (curveType) {
                0 -> {
                    // Gentle wave rising (Discipline Score)
                    moveTo(0f, height * 0.82f)
                    cubicTo(width * 0.2f, height * 0.72f, width * 0.35f, height * 0.45f, width * 0.5f, height * 0.54f)
                    cubicTo(width * 0.65f, height * 0.62f, width * 0.8f, height * 0.28f, width * 0.92f, height * 0.3f)
                    cubicTo(width * 0.96f, height * 0.32f, width * 0.98f, height * 0.2f, width, height * 0.16f)
                }
                1 -> {
                    // Wavy progress curve (Success Rate)
                    moveTo(0f, height * 0.86f)
                    cubicTo(width * 0.22f, height * 0.76f, width * 0.38f, height * 0.52f, width * 0.52f, height * 0.58f)
                    cubicTo(width * 0.68f, height * 0.64f, width * 0.84f, height * 0.36f, width * 0.94f, height * 0.32f)
                    lineTo(width, height * 0.26f)
                }
                else -> {
                    // Steady gentle gradient slope (Streak / Total interventions)
                    moveTo(0f, height * 0.90f)
                    cubicTo(width * 0.3f, height * 0.82f, width * 0.58f, height * 0.6f, width * 0.78f, height * 0.46f)
                    cubicTo(width * 0.88f, height * 0.42f, width * 0.96f, height * 0.38f, width, height * 0.34f)
                }
            }
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.01f)),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    titleColor: Color,
    value: String,
    subtitle: String,
    sparklineColor: Color,
    curveType: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp),
        border = BorderStroke(1.dp, if (isDark) cs.outlineVariant else cs.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = cs.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            MiniSparkline(
                color = sparklineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                curveType = curveType
            )
        }
    }
}

@Composable
fun BreakdownDetailCard(
    totalCount: Int,
    intervenedCount: Int,
    stoppedCount: Int,
    continuedCount: Int,
    dismissedCount: Int,
    disciplineRate: Int,
    isDark: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val mintColor = if (isDark) MintGreen else WarmSage
    val pinkColor = if (isDark) BrandHotPink else AccentTerracotta

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) cs.surface else Color(0xFFF9FAFB)
        ),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp),
        border = BorderStroke(1.dp, if (isDark) cs.outlineVariant else cs.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(
                    "PHÂN TÍCH KẾT QUẢ CAN THIỆP",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Phân định rõ: Ý định tìm kiếm · Can thiệp của app · Quyết định của bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            // Visual Segmented Progress Bar
            if (totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surfaceVariant)
                ) {
                    if (stoppedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(stoppedCount.toFloat())
                                .fillMaxSize()
                                .background(mintColor)
                        )
                    }
                    if (continuedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(continuedCount.toFloat())
                                .fillMaxSize()
                                .background(pinkColor)
                        )
                    }
                    if (dismissedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(dismissedCount.toFloat())
                                .fillMaxSize()
                                .background(if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                        )
                    }
                }
            }

            // 3 Clear Distinct Logical Blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Total Triggers
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "🎯 1. Ý định",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$totalCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = cs.onSurface
                        )
                        Text(
                            "Lần muốn stalk",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = cs.onSurfaceVariant
                        )
                    }
                }

                // 2. Interventions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "🛡️ 2. Can thiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$intervenedCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = cs.onSurface
                        )
                        Text(
                            "Lần hiện cảnh báo",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = cs.onSurfaceVariant
                        )
                    }
                }

                // 3. Stopped Outcome
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x2B34D399) else WarmSageLight)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "🛑 3. Kết quả",
                            style = MaterialTheme.typography.labelSmall,
                            color = mintColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$stoppedCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = mintColor
                        )
                        Text(
                            "Dừng được ($disciplineRate%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = mintColor
                        )
                    }
                }
            }

            // Outcome Footnotes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pinkColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Vẫn xem tiếp: $continuedCount lần",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                if (dismissedCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Bỏ qua/thoát nhanh: $dismissedCount lần",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonLeaderboardCard(
    rank: Int,
    personName: String,
    personEvents: List<StalkEvent>,
    todayStart: Long,
    fmt: SimpleDateFormat,
    fmtWithDate: SimpleDateFormat,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val total = personEvents.size
    val stopped = personEvents.count { it.decision == "STOPPED" }
    val continued = personEvents.count { it.decision == "CONTINUED" }
    val rate = if (total > 0) ((stopped * 100) / total).coerceIn(0, 100) else 100

    val avatarGradients = listOf(
        listOf(BrandViolet, BrandHotPink),
        listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(WarmSage, Color(0xFF10B981))
    )
    val grad = avatarGradients[(rank - 1).coerceAtLeast(0) % avatarGradients.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp),
        border = BorderStroke(1.dp, if (isDark) cs.outlineVariant else cs.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank number
                Text(
                    "$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )

                // Avatar with initial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(grad)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        personName.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "👤" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$total lần muốn xem · 🛑 $stopped · 👀 $continued",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Rate Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (rate >= 70) {
                                if (isDark) Color(0x3334D399) else WarmSageLight
                            } else {
                                if (isDark) Color(0x33EC4899) else AccentTerracottaLight
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$rate%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rate >= 70) {
                            if (isDark) MintGreen else WarmSage
                        } else {
                            if (isDark) BrandHotPink else AccentTerracotta
                        }
                    )
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    if (expanded) "▲" else "▼",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Lịch sử can thiệp của $personName:",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                    personEvents.forEach { e ->
                        EventItemRow(
                            e = e,
                            todayStart = todayStart,
                            fmt = fmt,
                            fmtWithDate = fmtWithDate,
                            isDark = isDark,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemRow(
    e: StalkEvent,
    todayStart: Long,
    fmt: SimpleDateFormat,
    fmtWithDate: SimpleDateFormat,
    isDark: Boolean,
    compact: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val (appGradient, appIcon) = when {
        e.packageName.contains("facebook.katana") -> FbGradient to "📘"
        e.packageName.contains("instagram") -> IgGradient to "📷"
        e.packageName.contains("orca") -> MsGradient to "💬"
        e.packageName.contains("zalo") -> ZaloGradient to "🔵"
        else -> DefaultAppGradient to "📱"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (compact) cs.surfaceVariant.copy(alpha = 0.45f) else cs.surface
        ),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 1.dp),
        border = BorderStroke(1.dp, if (isDark) cs.outlineVariant else cs.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
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
                if (!compact) {
                    Text(
                        e.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${triggerIcon(e.triggerType)} ${friendlyTrigger(e.triggerType)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        " · " + (if (e.createdAt >= todayStart) fmt.format(Date(e.createdAt))
                                else fmtWithDate.format(Date(e.createdAt))),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
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
                        else -> "⏳ Bỏ qua"
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

@Composable
fun StatsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()
    val events by vm.events.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val fmtWithDate = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Time period filter: "today" | "week" | "all"
    var selectedPeriod by remember { mutableStateOf("today") }

    val periodEvents = remember(events, selectedPeriod) {
        when (selectedPeriod) {
            "today" -> events.filter { it.createdAt >= todayStart }
            "week" -> {
                val weekStart = todayStart - 6 * 86_400_000L
                events.filter { it.createdAt >= weekStart }
            }
            else -> events
        }
    }

    val totalCount = periodEvents.size
    val stoppedCount = periodEvents.count { it.decision == "STOPPED" }
    val continuedCount = periodEvents.count { it.decision == "CONTINUED" }
    val dismissedCount = (totalCount - stoppedCount - continuedCount).coerceAtLeast(0)
    val intervenedCount = stoppedCount + continuedCount

    val disciplineRate = if (totalCount > 0) {
        ((stoppedCount * 100) / totalCount).coerceIn(0, 100)
    } else 100

    val successRate = if (intervenedCount > 0) {
        ((stoppedCount * 100) / intervenedCount).coerceIn(0, 100)
    } else if (totalCount > 0) disciplineRate else 100

    // Unique persons in current period
    val uniquePersons = remember(periodEvents) {
        periodEvents.map { it.personName }.distinct()
    }

    // View mode: "grouped" (Streak Leaderboard) vs "timeline" (Chronological with filter)
    var viewMode by remember { mutableStateOf("grouped") }
    var selectedPersonFilter by remember { mutableStateOf<String?>(null) }

    val groupedByPerson = remember(periodEvents) {
        periodEvents.groupBy { it.personName }
            .toList()
            .sortedByDescending { it.second.size }
    }

    val displayedTimelineEvents = remember(periodEvents, selectedPersonFilter) {
        if (selectedPersonFilter == null) periodEvents.take(50)
        else periodEvents.filter { it.personName == selectedPersonFilter }.take(50)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Header: Analytics ────────────────────────────────────────────────
        item {
            Text(
                "THỐNG KÊ & PHÂN TÍCH",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text("Analytics", style = MaterialTheme.typography.displayMedium)
            Text(
                "Theo dõi thói quen, mức độ kiên định và kết quả can thiệp 😌",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }

        // ─── Progress Summary Header + Period Pills (Weekly / Monthly / Today) ─
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tóm tắt tiến độ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                // Period Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val pills = listOf("today" to "Hôm nay", "week" to "Tuần này", "all" to "Tất cả")
                    pills.forEach { (key, label) ->
                        val selected = selectedPeriod == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) cs.surface else Color.Transparent)
                                .clickable { selectedPeriod = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) cs.onSurface else cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ─── 3 Progress Summary Metric Cards with Mini Sparklines ─────────────
        item {
            AnalyticsMetricCard(
                title = "Điểm kỷ luật (Discipline Score)",
                titleColor = if (isDark) BrandViolet else Color(0xFF7C3AED),
                value = "$disciplineRate.0%",
                subtitle = "Tỷ lệ bạn kiểm soát và dừng lại thành công",
                sparklineColor = Color(0xFF8B5CF6),
                curveType = 0,
                isDark = isDark
            )
        }

        item {
            AnalyticsMetricCard(
                title = "Tỉ lệ dừng stalk (Success Rate)",
                titleColor = if (isDark) BrandHotPink else AccentTerracotta,
                value = "$successRate.0%",
                subtitle = "Tỷ lệ chọn quay đầu sau khi hiện cảnh báo",
                sparklineColor = Color(0xFFEC4899),
                curveType = 1,
                isDark = isDark
            )
        }

        item {
            AnalyticsMetricCard(
                title = "Tổng lượt can thiệp (Interventions)",
                titleColor = Color(0xFFF59E0B),
                value = "$intervenedCount lần",
                subtitle = "Số lần hệ thống can thiệp và yêu cầu xác nhận",
                sparklineColor = Color(0xFFF59E0B),
                curveType = 2,
                isDark = isDark
            )
        }

        // ─── Breakdown Card: Phân biệt rõ tổng số lần, can thiệp & kết quả ──────
        item {
            BreakdownDetailCard(
                totalCount = totalCount,
                intervenedCount = intervenedCount,
                stoppedCount = stoppedCount,
                continuedCount = continuedCount,
                dismissedCount = dismissedCount,
                disciplineRate = disciplineRate,
                isDark = isDark
            )
        }

        // ─── Section: Leaderboard / Event Log Header ──────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CHI TIẾT THEO NGƯỜI",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                // View Mode Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (viewMode == "grouped") cs.surface else Color.Transparent)
                            .clickable { viewMode = "grouped" }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "👥 Nhóm theo người",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (viewMode == "grouped") FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == "grouped") cs.onSurface else cs.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (viewMode == "timeline") cs.surface else Color.Transparent)
                            .clickable { viewMode = "timeline" }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "⏱️ Dòng thời gian",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (viewMode == "timeline") FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == "timeline") cs.onSurface else cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (periodEvents.isEmpty()) {
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
                            "Không có ghi nhận nào trong khoảng thời gian này. Sạch sẽ tuyệt đối! 😌",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (viewMode == "grouped") {
            items(groupedByPerson) { (pName, pEvents) ->
                val rank = groupedByPerson.indexOfFirst { it.first == pName } + 1
                PersonLeaderboardCard(
                    rank = rank,
                    personName = pName,
                    personEvents = pEvents,
                    todayStart = todayStart,
                    fmt = fmt,
                    fmtWithDate = fmtWithDate,
                    isDark = isDark
                )
            }
        } else {
            // Timeline mode with filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedPersonFilter == null,
                            onClick = { selectedPersonFilter = null },
                            label = { Text("Tất cả (${periodEvents.size})") },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) BrandViolet else cs.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(uniquePersons) { pName ->
                        val count = periodEvents.count { it.personName == pName }
                        FilterChip(
                            selected = selectedPersonFilter == pName,
                            onClick = {
                                selectedPersonFilter = if (selectedPersonFilter == pName) null else pName
                            },
                            label = { Text("👤 $pName ($count)") },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) BrandViolet else cs.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            items(displayedTimelineEvents) { e ->
                EventItemRow(
                    e = e,
                    todayStart = todayStart,
                    fmt = fmt,
                    fmtWithDate = fmtWithDate,
                    isDark = isDark,
                    compact = false
                )
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

        // ─── Theme Mode Section (Appearance Preview) ──────────────────────────
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemePreviewItem(
                        mode = "light",
                        label = "Light",
                        isSelected = themeMode == "light",
                        onClick = { vm.setThemeMode("light") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemePreviewItem(
                        mode = "dark",
                        label = "Dark",
                        isSelected = themeMode == "dark",
                        onClick = { vm.setThemeMode("dark") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemePreviewItem(
                        mode = "system",
                        label = "System",
                        isSelected = themeMode == "system",
                        onClick = { vm.setThemeMode("system") },
                        modifier = Modifier.weight(1f)
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
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Mức độ cà khịa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Độ gắt của lời nhắc khi phát hiện stalk",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isDark) Color(0x3334D399) else WarmSageLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "⚡ Áp dụng ngay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MintGreen else WarmSage
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val levels = listOf(
                        Triple(1, "L1 — Nhẹ nhàng 😌", "Nhắc khéo nhẹ nhàng, không phán xét"),
                        Triple(2, "L2 — Bạn thân 😏 (mặc định)", "Châm biếm hóm hỉnh như đứa bạn thân"),
                        Triple(3, "L3 — Cà khịa 😈", "Đánh trúng tim đen, thức tỉnh lòng tự trọng"),
                        Triple(4, "L4 — Tàn nhẫn nhưng văn minh 💀", "Lời lẽ thẳng thắn, dập tắt ý định ngay lập tức")
                    )

                    levels.forEach { (lv, title, desc) ->
                        val isSelected = level == lv
                        val itemBorder by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) BrandViolet else WarmSage
                            } else {
                                if (isDark) cs.outlineVariant else cs.outline
                            },
                            label = "roast_border"
                        )
                        val itemBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                if (isDark) Color(0x2E8B5CF6) else WarmSageLight
                            } else {
                                if (isDark) cs.surfaceVariant else cs.surface
                            },
                            label = "roast_bg"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, color = itemBorder, shape = RoundedCornerShape(14.dp))
                                .background(itemBg)
                                .clickable {
                                    vm.setRoastLevel(lv)
                                    Toast.makeText(ctx, "Đã chọn Mức L$lv — Áp dụng ngay cho lần can thiệp tới!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) {
                                        if (isDark) BrandHotPink else WarmSage
                                    } else cs.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isDark) Color(0x3334D399) else WarmSageLight)
                                        .border(1.dp, if (isDark) MintGreen else WarmSage, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "ĐANG DÙNG ✓",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MintGreen else WarmSage
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1F8B5CF6) else WarmSageLight)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mức L$level đang kích hoạt. Sẽ áp dụng ngay lập tức cho lần can thiệp tiếp theo mà không cần khởi động lại app.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) BrandHotPink else WarmSage
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

@Composable
private fun ThemePreviewItem(
    mode: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme

    val selectedBorderColor = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1C1C1E)
    val unselectedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE5E7EB)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Outer Card Container Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) selectedBorderColor else unselectedBorderColor,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            when (mode) {
                "light" -> {
                    // Light mode preview: light gray base, white inner window
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFEDEDF0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, start = 14.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp))
                                .background(Color.White)
                        ) {
                            Text(
                                "Aa",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1C1C1E),
                                modifier = Modifier.padding(start = 10.dp, top = 8.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 6.dp, end = 6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1C1C1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                "dark" -> {
                    // Dark mode preview: charcoal base, deep dark inner window
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF3E4044))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp, start = 14.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp))
                                .background(Color(0xFF181A1D))
                        ) {
                            Text(
                                "Aa",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                modifier = Modifier.padding(start = 10.dp, top = 8.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 6.dp, end = 6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1C1C1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // System mode preview: 50/50 split
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left half (Dark)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF3E4044))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 16.dp, start = 10.dp)
                                    .clip(RoundedCornerShape(topStart = 10.dp))
                                    .background(Color(0xFF181A1D))
                            ) {
                                Text(
                                    "Aa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                                )
                            }
                        }
                        // Right half (Light)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFEDEDF0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 16.dp)
                                    .background(Color.White)
                            ) {
                                Text(
                                    "Aa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1C1C1E),
                                    modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(bottom = 6.dp, end = 6.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C1C1E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Text Label
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) cs.onSurface else cs.onSurfaceVariant
        )
    }
}
