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
