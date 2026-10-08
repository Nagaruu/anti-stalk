# Anti-Stalk — Android MVP (sideload)

Privacy-first · Local-first · Offline-first. Kotlin + Jetpack Compose + Room.
Detection = AccessibilityService (passive observer) + overlay `TYPE_APPLICATION_OVERLAY`.

## Mở & chạy
1. Cài Android Studio Ladybug+ (JDK 17).
2. Open project: thư mục `anti-stalk/` (đã có `settings.gradle`, `app/build.gradle`).
3. Sync Gradle (cần mạng lần đầu). `minSdk 26, target/compile 34`.
4. Run `app` lên máy thật (không dùng emulator cho test detect):
   - Mở app → Onboarding → bật **Accessibility** (tìm Anti-Stalk) → bật **Vẽ trên ứng dụng khác** → Kiểm tra lại → Tiếp tục.
   - Thêm 1 người (VD: `Nguyễn Văn A`) → tab Hôm nay bật Facebook/Messenger/Instagram/Zalo.
   - Kiểm tra card **Phát hiện có chạy không** ở tab Hôm nay: cả 3 dòng phải ✅, nếu ❌ thì bấm nút mở cài đặt ngay trong card.
   - Test thật: mở Facebook → gõ tên người đó vào ô search → overlay phải hiện ngay khi gõ đủ tên. Bấm tìm/enter → overlay hiện thêm lần nữa. Nhấn THÔI/VẪN XEM → số liệu ở tab Hôm nay/Thống kê tăng.
5. Tắt battery optimization cho app (Xiaomi/Oppo/Samsung hay kill service).

## Cấu trúc
- `core/`: `TextNormalize` (bỏ dấu TV), `Constants` (package FB/Messenger/IG/Zalo + FreeLimits), `RoastBank` L1–L4 (vi/en), `SigningInfo` (đọc/chuẩn hoá SHA-256 chữ ký cài đặt vs APK), `BackupFiles` (copy stream qua SAF URI), `AppUpdater`.
- `data/local/`: Room (`persons, keywords, monitored_apps, events, streak_goals`) + DAO + `AntiStalkDb`.
- `data/AntiStalkRepository.kt`: seed app, add person + auto-keyword, log event, stats hôm nay, wipe, `exportBackupJson`/`importBackupJson` (JSON sao lưu/khôi phục).
- `detection/`: `Matcher` (thuần, dễ test), `AppDetector` (GenericDetector cho cả 4 app), `StalkAccessibilityService` (chỉ quan sát, KHÔNG gesture hộ user), `OverlayManager` (classic View overlay từ Service).
- `ui/`: `AppNav` (onboarding→permissions→main 4 tab + dialog chuyển đổi khoá ký), `MainViewModel`, `intervention/InterventionOverlay` (card cà khịa Compose), `screens/` (tab Hôm nay có card chẩn đoán phát hiện, `BackupSection` xuất/khôi phục JSON).

## Ký bản phát hành — không bao giờ xoay khoá
- Mọi release ký bằng `keystore/debug.keystore` (alias `androiddebugkey`, bản sao ở `keystore-backup/`). CI (`.github/workflows/build-apk.yml`) assert SHA-256 cert = `162de3ba4d8a6c943e04080bbfe0cd5958d6482124fc2fd8610e2de3c62e8d82` sau mỗi build.
- **Không bao giờ xoay/thay khoá này.** Android từ chối update có người ký khác (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) — đổi khoá là mọi máy đang cài sẽ không nhận update nữa.
- Các bản build trước khi khoá ổn định (≤ v27) mang khoá debug ngẫu nhiên, nên chúng đâm thẳng vào `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. App phát hiện lệch khoá (`core/SigningInfo.kt` → `MainViewModel.UpdateState.NeedsMigration`) và dẫn user qua luồng chuyển đổi 1 lần ngay trong app: xuất sao lưu → lưu APK → gỡ cài đặt → cài APK vừa lưu → khôi phục. Không cần gõ lệnh, không cần cài Android Studio.
- Sao lưu: `Cài đặt → Dữ liệu → Xuất sao lưu JSON / Khôi phục`. JSON chứa persons, keywords (gắn theo thứ tự người), monitored apps, prefs — không chứa nhật ký sự kiện (dữ liệu nhạy cảm).

## Giới hạn đã chốt (trung thực, không giả vờ)
- 3 tín hiệu MVP: **SEARCH_INPUT** (gõ đủ tên, HIGH) + **SEARCH_SUBMITTED** (mở trang kết quả trong 5s sau khi gõ, HIGH, bypass cooldown 30s) + **PROFILE_TITLE** (tiêu đề chat/profile, MEDIUM). Feed lướt qua tên: KHÔNG làm.
- Cooldown 30s/người/app chống spam. Keyword < 3 ký tự bị bỏ qua.
- FB/IG/Zalo đổi UI là detector có thể lệch → sửa ở `DetectorRegistry`/`GenericDetector`, không đụng service.
- Sideload nên chưa cần Play review. Khi lên Play: thêm prominent disclosure riêng + declaration video + privacy policy.

## Thêm app mới sau này
Thêm 1 dòng vào `MonitoredPackages.DEFAULTS` (package + label). `GenericDetector` tự phủ. App đặc thù mới cần subclass `AppDetector`.

## Monetization stub
`FreeLimits`: 1 người, 2 app. Chưa khóa gì ở MVP — khi làm premium thì check ở `PersonsScreen`/`toggleApp`.
