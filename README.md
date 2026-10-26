# AppBase

Starter project Android production-ready dùng Kotlin, Jetpack Compose, Clean Architecture và MVVM. Project giữ cấu trúc single-module để clone nhanh nhưng tách package rõ theo `core`, `data`, `domain`, `feature`, `navigation` và `ui`.

## Yêu cầu

- Android Studio tương thích AGP 9.2+
- JDK 17 trở lên
- Android SDK 36.1
- Firebase project nếu dùng Auth, Crashlytics hoặc FCM thật

## Chạy project

```bash
./gradlew assembleMockDebug        # demo auth, không cần backend/Firebase
./gradlew testMockDebugUnitTest
```

| Flavor | Auth | `BASE_URL` | Ghi chú |
|---|---|---|---|
| `mock` | Fake (local) | dev | Chạy ngay sau khi clone. Nhập email hợp lệ và mật khẩu từ 6 ký tự. Không có bản release |
| `dev` | Firebase/REST thật | dev | Test backend thật trên bản debug |
| `prod` | Firebase/REST thật | production | Bản upload Google Play (`bundleProdRelease`) |

Debug build có suffix `.debug` nên cài song song được với bản release.

## Chất lượng code

```bash
./gradlew ktlintFormat          # tự sửa format
./gradlew ktlintCheck detekt    # CI chạy 2 lệnh này, lỗi là fail build
```

- **ktlint** (+ bộ luật Compose): format và quy ước Compose. Cấu hình trong `.editorconfig`.
- **detekt**: code smell (độ phức tạp, bắt exception quá rộng, số "magic"...). Cấu hình trong `config/detekt/detekt.yml`; chỉ tắt luật kèm lý do, và `@Suppress` tại chỗ phải có comment giải thích.

## Kiến trúc

- `core/`: networking, typed DataStore, Room, DI, logging và notification.
- `domain/`: model, repository interface và use case thuần Kotlin.
- `data/`: Firebase/REST implementation, DTO, DAO và entity.
- `feature/`: login, register và home/settings. Mỗi màn có ViewModel kế thừa `BaseViewModel<State, Event>`.
- `ui/`: Material 3 theme, token và component dùng lại.
- `navigation/`: route type-safe và deep link `appbase://home`.

Token đăng nhập được lưu trong typed DataStore (protobuf), toàn bộ file được mã hoá AES-GCM bằng khoá Android Keystore. Request nhận HTTP 401 sẽ tự refresh token một lần (`TokenAuthenticator`); refresh bị từ chối thì đăng xuất và auth gate trong `AppNavHost` đưa người dùng về Login.

### Viết màn hình mới

1. Tạo `XxxUiState` (data class) và, nếu cần, `XxxEvent` (sealed interface cho điều hướng/snackbar).
2. ViewModel kế thừa `BaseViewModel<XxxUiState, XxxEvent>`, dùng `updateState {}`, `sendEvent()` và `launchSafely(onError = ...)`.
3. Screen đọc `state` bằng `collectAsStateWithLifecycle()`, nhận event bằng `ObserveEvents`, hiển thị lỗi bằng `error.toUserMessage()`.
4. Route cần đăng nhập không phải làm gì thêm; route công khai thì thêm vào `requiresAuth()` trong `AppRoutes.kt`.

Lỗi đi qua các tầng dưới dạng `AppError`. Gọi Retrofit luôn bọc trong `apiCall {}` để HTTP/IO/parse error được đổi sang `AppError`. Room export schema vào `app/schemas`; khi đổi schema: tăng `AppDatabase.VERSION`, build để sinh file JSON mới, thêm `Migration` vào `AppDatabase.MIGRATIONS` và bổ sung `AppDatabaseMigrationTest` (chạy bằng Robolectric nên CI kiểm tra luôn, không cần emulator). Token, FCM token và database được loại khỏi Android cloud backup/device transfer.

## Tạo app sản phẩm mới

1. Đổi `rootProject.name`, `namespace`, `applicationId` và package Kotlin.
2. Đổi tên app, icon, màu thương hiệu và typography.
3. Đổi `BASE_URL` của từng flavor trong `app/build.gradle.kts`.
4. Đổi deep-link scheme/host trong `Constants`, `DeepLinkHandler` và `AndroidManifest.xml`.
5. Thay các màn hình demo trong `feature/` bằng tính năng sản phẩm.
6. Chọn backend auth trong `AppModule`: `FirebaseAuthBackend` (mặc định) hoặc `RetrofitAuthBackend` cho REST API riêng (sửa `AuthApi`/DTO theo contract server).

## Firebase

1. Tạo Android app trong Firebase Console với đúng `applicationId`.
2. Tải `google-services.json` vào `app/google-services.json`.
3. Bật Email/Password Auth, Crashlytics và Cloud Messaging.
4. Với Firebase Auth Emulator, cấu hình emulator trước khi gọi Auth trong `FirebaseProvider` ở debug build.
5. Chạy app, đăng nhập và gửi notification thử từ Firebase Console.

Google Services và Crashlytics Gradle plugin chỉ được bật khi file cấu hình tồn tại, nên clone mới vẫn build được. `google-services.json`, keystore và `keystore.properties` được ignore khỏi template.

## Đánh giá và cập nhật trong app (Google Play)

Code nằm trong `core/playstore/`. Cả hai chỉ hoạt động khi app được cài từ Google Play; bản mock, emulator hoặc APK cài tay sẽ bỏ qua yên lặng.

- **In-App Review**: gọi `rememberInAppReview()` rồi gọi callback ngay sau một khoảnh khắc tích cực (hoàn thành việc, đạt mục tiêu). `ReviewPolicy` chỉ cho hiện khi đã mở app ≥ 5 lần, cài ≥ 3 ngày và cách lần hỏi trước ≥ 60 ngày. Nút "Đánh giá ứng dụng" dùng `openPlayStoreListing()`, không dùng In-App Review (Google có hạn mức, hộp thoại có thể không hiện).
- **In-App Update**: `InAppUpdateHandler` ở gốc app kiểm tra mỗi lần app trở lại foreground. Priority ≥ 4 → bắt buộc cập nhật (immediate); priority ≥ 2 hoặc bản mới đã ra ≥ 3 ngày → cập nhật nền (flexible), tải xong thì hỏi khởi động lại. Priority đặt cho từng bản phát hành qua Play Developer API (`inAppUpdatePriority`); Play Console không có ô nhập priority.
- **Thử trên máy thật**: upload 2 bản (versionCode khác nhau) lên *Internal app sharing* hoặc track *Internal testing*, cài bản thấp từ link Play rồi mở app.

## Kiếm tiền: quảng cáo + gói "Gỡ quảng cáo"

Code nằm trong `core/monetization/`. **Mặc định tắt** (`ads.enabled=false` trong `gradle.properties`).

**Bật cho một app:**
1. Đặt `ads.enabled=true` trong `gradle.properties`.
2. Tạo app trên AdMob, điền `admob.appId`, `admob.bannerId`, `admob.interstitialId` (chỉ `prodRelease` dùng ID thật; mọi bản debug luôn dùng ID test của Google để tránh bị khoá tài khoản vì tự click).
3. Trong AdMob > *Privacy & messaging*: tạo thông điệp GDPR (và US states nếu cần). Không có thông điệp thì form đồng ý không hiện.
4. Trong Play Console: tạo sản phẩm mua một lần ID `remove_ads` (đổi bằng `billing.removeAdsProductId`), thêm tài khoản *License testing* để mua thử không mất tiền.
5. Thêm `app-ads.txt` lên website ghi trong trang Play Store.

**Luồng khởi động** (`MonetizationInitializer` ở gốc app): khôi phục giao dịch → nếu đã mua "Gỡ quảng cáo" thì dừng → xin đồng ý (UMP) → khởi tạo Mobile Ads SDK → tải sẵn quảng cáo xen kẽ.

- **Banner**: `AppBannerAd()` (adaptive, tự ẩn khi không được phép hiện).
- **Xen kẽ**: `rememberInterstitialAd()`; gọi ở điểm dừng tự nhiên. `FrequencyCap` chặn quá 1 quảng cáo / 3 phút, kể cả 3 phút đầu sau khi mở app.
- **Gỡ quảng cáo**: `BillingController` xác nhận (acknowledge) giao dịch, khôi phục khi cài lại, xử lý giao dịch chờ thanh toán và thu hồi khi bị hoàn tiền. Trạng thái được lưu nên offline vẫn không hiện quảng cáo. Xác minh phía client — đủ cho món rẻ mua một lần; bán thứ giá trị cao thì cần xác minh trên server.

Lưu ý: SDK AdMob vẫn nằm trong APK kể cả khi tắt (manifest luôn cần App ID, nếu không app crash khi khởi động). App chắc chắn không bao giờ dùng quảng cáo có thể xoá các dependency `admob`, `ump`, `billing`, thẻ `meta-data` App ID trong manifest, thư mục `core/monetization/`, `MonetizationModule` và các chỗ gọi (`MonetizationInitializer` trong `MainActivity`, banner/xen kẽ/`MonetizationSettings` ở màn Home).

## Release Google Play

```bash
./gradlew lintMockDebug testMockDebugUnitTest bundleProdRelease -PversionCode=2 -PversionName=1.0.1
```

Ký release: tạo `keystore.properties` ở thư mục gốc (đã ignore) hoặc đặt biến môi trường `SIGNING_*` trên CI:

```properties
storeFile=release.jks            # SIGNING_STORE_FILE
storePassword=...                # SIGNING_STORE_PASSWORD
keyAlias=...                     # SIGNING_KEY_ALIAS
keyPassword=...                  # SIGNING_KEY_PASSWORD
```

Không có cấu hình ký thì `bundleProdRelease` tạo AAB chưa ký. Bản release giữ số dòng trong stack trace (mapping được Crashlytics plugin upload); Crashlytics tắt thu thập ở bản debug. Trước khi upload cần đổi package, version, icon, privacy policy, Data safety form và kiểm tra target SDK theo yêu cầu Play hiện hành.

Workflow `.github/workflows/android.yml` chạy lint, unit test, build APK mock debug và AAB prod release. Firebase Console/FCM/Crashlytics là kiểm thử tích hợp bên ngoài, không thể xác minh chỉ bằng CI khi chưa có Firebase project và thiết bị/emulator.

Push notification có thể mở một màn cụ thể bằng data payload `{"deep_link": "appbase://home"}` (chỉ chấp nhận scheme của app).
