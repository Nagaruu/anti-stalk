# Chính sách riêng tư — Anti-Stalk

> File này là bản chuẩn để đăng lên URL công khai trước khi submit Play:
> **https://nagaruu.github.io/anti-stalk/privacy**
> (tạo GitHub Pages cho repo hoặc dán nội dung này lên site khác rồi đổi
> `PRIVACY_POLICY_URL` trong `app/src/main/java/com/antistalk/ui/screens/PrivacyPolicyScreen.kt`).
> Nội dung trong app (`PrivacyPolicyContent`) phải giữ giống hệt file này.

Cập nhật: tháng 10/2026.

## 1. Anti-Stalk làm gì

Anti-Stalk giúp bạn bớt stalk người cần tránh: khi phát hiện bạn sắp tìm/xem
trang của họ trong các app mạng xã hội đã chọn, app hiện một popup nhắc nhở và
(chỉ khi bạn bấm nút) đưa bạn về màn hình chính.

## 2. Dữ liệu đọc qua Trợ năng (Accessibility)

Khi bạn bật dịch vụ Trợ năng, app đọc:

- (a) chữ trong ô tìm kiếm của Facebook, Messenger, Instagram, Zalo mà bạn đã
  bật theo dõi;
- (b) tiêu đề màn hình (tên trang cá nhân/đoạn chat).

App chỉ đối chiếu với danh sách tên BẠN tự nhập để né.

## 3. Dữ liệu KHÔNG bao giờ thu thập

Không đọc nội dung tin nhắn, mật khẩu, hình ảnh, danh bạ. Không yêu cầu tài
khoản. Không ghi âm, không chụp màn hình.

## 4. Lưu trữ 100% trên máy

Tên cần né, từ khóa, lịch sử can thiệp và cài đặt lưu trong máy bạn (Room
database + SharedPreferences). Không có máy chủ nào của chúng tôi nhận dữ liệu
này. Gỡ app là hết.

## 5. Không chia sẻ cho bên thứ ba

Không bán, không chia sẻ, không gửi dữ liệu cho quảng cáo hay bên phân tích
nào. Không SDK theo dõi.

## 6. Các quyền khác

- **Hiển thị trên ứng dụng khác:** chỉ để hiện popup nhắc nhở, có thể tắt bất
  cứ lúc nào.
- **Internet:** bản sideload dùng để kiểm tra bản mới trên GitHub Releases;
  bản CH Play cập nhật qua CH Play và không tự tải APK.

## 7. Quyền của bạn

Tắt Trợ năng hoặc thu hồi quyền overlay bất cứ lúc nào trong Cài đặt Android
là app ngừng quan sát ngay. Nút “Xóa toàn bộ dữ liệu local” trong app xóa sạch
danh sách né và lịch sử.

## 8. Liên hệ

Mở issue trên trang GitHub của dự án.
