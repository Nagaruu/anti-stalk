package com.antistalk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single source of truth for the Privacy Policy.
 * - Shown in-app (dialog) from Disclosure + Settings.
 * - Mirrored verbatim in PRIVACY_POLICY.md at repo root, which must be
 *   published to [PRIVACY_POLICY_URL] before the Play submission.
 */
const val PRIVACY_POLICY_URL = "https://nagaruu.github.io/anti-stalk/privacy"

@Composable
fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Chính sách riêng tư — Anti-Stalk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Cập nhật: tháng 10/2026. Liên hệ: xem trang GitHub dự án.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrivacySection(
            "1. Anti-Stalk làm gì",
            "Anti-Stalk giúp bạn bớt stalk người cần tránh: khi phát hiện bạn sắp tìm/xem trang của họ trong các app mạng xã hội đã chọn, app hiện một popup nhắc nhở và (chỉ khi bạn bấm nút) đưa bạn về màn hình chính."
        )
        PrivacySection(
            "2. Dữ liệu đọc qua Trợ năng (Accessibility)",
            "Khi bạn bật dịch vụ Trợ năng, app đọc: (a) chữ trong ô tìm kiếm của Facebook, Messenger, Instagram, Zalo mà bạn đã bật theo dõi; (b) tiêu đề màn hình (tên trang cá nhân/đoạn chat). App chỉ đối chiếu với danh sách tên BẠN tự nhập để né."
        )
        PrivacySection(
            "3. Dữ liệu KHÔNG bao giờ thu thập",
            "Không đọc nội dung tin nhắn, mật khẩu, hình ảnh, danh bạ. Không yêu cầu tài khoản. Không ghi âm, không chụp màn hình."
        )
        PrivacySection(
            "4. Lưu trữ 100% trên máy",
            "Tên cần né, từ khóa, lịch sử can thiệp và cài đặt lưu trong máy bạn (Room database + SharedPreferences). Không có máy chủ nào của chúng tôi nhận dữ liệu này. Gỡ app là hết."
        )
        PrivacySection(
            "5. Không chia sẻ cho bên thứ ba",
            "Không bán, không chia sẻ, không gửi dữ liệu cho quảng cáo hay bên phân tích nào. Không SDK theo dõi."
        )
        PrivacySection(
            "6. Các quyền khác",
            "Hiển thị trên ứng dụng khác: chỉ để hiện popup nhắc nhở, có thể tắt bất cứ lúc nào. Internet: bản sideload dùng để kiểm tra bản mới trên GitHub Releases; bản CH Play cập nhật qua CH Play và không tự tải APK."
        )
        PrivacySection(
            "7. Quyền của bạn",
            "Tắt Trợ năng hoặc thu hồi quyền overlay bất cứ lúc nào trong Cài đặt Android là app ngừng quan sát ngay. Nút “Xóa toàn bộ dữ liệu local” trong app xóa sạch danh sách né và lịch sử."
        )
        Text(
            "Bản đầy đủ đăng tại: $PRIVACY_POLICY_URL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chính sách riêng tư") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PrivacyPolicyContent()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
