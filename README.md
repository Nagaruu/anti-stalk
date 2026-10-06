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
   - Nhấn **GIẢ LẬP STALK** để xem màn hình cà khịa trong app.
   - Test thật: mở Facebook → gõ tên người đó vào ô search → overlay phải hiện trong ~1s. Nhấn THÔI/VẪN XEM → số liệu ở tab Hôm nay/Thống kê tăng.
5. Tắt battery optimization cho app (Xiaomi/Oppo/Samsung hay kill service).

## Cấu trúc
- `core/`: `TextNormalize` (bỏ dấu TV), `Constants` (package FB/Messenger/IG/Zalo + FreeLimits), `RoastBank` L1–L4 (vi/en).
- `data/local/`: Room (`persons, keywords, monitored_apps, events, streak_goals`) + DAO + `AntiStalkDb`.
- `data/AntiStalkRepository.kt`: seed app, add person + auto-keyword, log event, stats hôm nay, wipe.
- `detection/`: `Matcher` (thuần, dễ test), `AppDetector` (GenericDetector cho cả 4 app), `StalkAccessibilityService` (chỉ quan sát, KHÔNG gesture hộ user), `OverlayManager` (classic View overlay từ Service).
- `ui/`: `AppNav` (onboarding→permissions→main 4 tab), `MainViewModel`, `intervention/InterventionOverlay` (preview Compose), `screens/`.

## Giới hạn đã chốt (trung thực, không giả vờ)
- Chỉ 2 tín hiệu MVP: **SEARCH_INPUT** (gõ tên, HIGH) + **PROFILE_TITLE** (tiêu đề chat/profile, MEDIUM). Feed lướt qua tên: KHÔNG làm.
- Cooldown 30s/người/app chống spam. Keyword < 3 ký tự bị bỏ qua.
- FB/IG/Zalo đổi UI là detector có thể lệch → sửa ở `DetectorRegistry`/`GenericDetector`, không đụng service.
- Sideload nên chưa cần Play review. Khi lên Play: thêm prominent disclosure riêng + declaration video + privacy policy.

## Thêm app mới sau này
Thêm 1 dòng vào `MonitoredPackages.DEFAULTS` (package + label). `GenericDetector` tự phủ. App đặc thù mới cần subclass `AppDetector`.

## Monetization stub
`FreeLimits`: 1 người, 2 app. Chưa khóa gì ở MVP — khi làm premium thì check ở `PersonsScreen`/`toggleApp`.
