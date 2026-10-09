package com.antistalk.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.ui.theme.BrandViolet
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.MintGreen
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageLight

/**
 * Prominent Accessibility disclosure (Play policy).
 * MUST stay a standalone screen with its own checkbox — never merged into
 * Terms, onboarding marketing copy, or the privacy policy dialog.
 * Shown BEFORE the system Accessibility settings step.
 */
@Composable
fun DisclosureScreen(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    onPreviewPermissions: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    var checked by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isDark) cs.surfaceVariant else WarmSageLight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "MINH BẠCH QUYỀN TRỢ NĂNG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (isDark) MintGreen else WarmSage
            )
        }
        Text(
            "App sẽ đọc gì khi bạn bật Trợ năng?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = cs.onBackground
        )
        Text(
            "Đọc kỹ trước khi tiếp tục. Bạn phải tick đồng ý thì nút Tiếp tục mới mở.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )

        DisclosureCard(
            icon = "👁️",
            title = "App ĐỌC gì",
            body = "Chữ bạn gõ trong ô tìm kiếm và tiêu đề màn hình (tên trang cá nhân/đoạn chat) trong Facebook, Messenger, Instagram, Zalo mà BẠN đã bật theo dõi — chỉ để đối chiếu với danh sách tên bạn tự nhập để né."
        )
        DisclosureCard(
            icon = "🚫",
            title = "App KHÔNG làm gì",
            body = "Không đọc nội dung tin nhắn, mật khẩu hay danh bạ. Không chụp màn hình, không ghi âm. Mọi đối chiếu chạy trên máy. Không gửi dữ liệu của bạn lên internet hay cho bên thứ ba."
        )
        DisclosureCard(
            icon = "🪟",
            title = "Popup nhắc nhở",
            body = "Khi phát hiện bạn sắp stalk, app hiện popup trên app khác. Bấm “Thôi, tôi đi ra” thì app đưa bạn về màn hình chính. Tắt quyền overlay hoặc Trợ năng bất cứ lúc nào là app ngừng ngay."
        )

        // Separate consent checkbox (Play: affirmative user action, not bundled).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceVariant.copy(alpha = 0.5f))
                .clickable { checked = !checked }
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Spacer(Modifier.width(8.dp))
            Text(
                "Tôi hiểu và đồng ý cho Anti-Stalk dùng Trợ năng đúng như mô tả trên.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onAccept,
            enabled = checked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandViolet else WarmSage
            )
        ) {
            Text(
                "Đồng ý & tiếp tục",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (!checked) {
            Text(
                "⬆️ Tick vào ô trên để mở khóa nút tiếp tục nhé.",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("← Quay lại") }
            TextButton(onClick = { showPrivacy = true }) { Text("Chính sách riêng tư") }
        }
        // Quiet preview link for reviewers: permissions list without leaving flow.
        TextButton(
            onClick = onPreviewPermissions,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "Xem trước 2 quyền cần cấp ở bước sau",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisclosureCard(icon: String, title: String, body: String) {
    val isDark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
