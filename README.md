# AppBase

Starter project Android production-ready dùng Kotlin, Jetpack Compose, Clean Architecture và MVVM. Project giữ cấu trúc single-module để clone nhanh nhưng tách package rõ theo `core`, `data`, `domain`, `feature`, `navigation` và `ui`.

## Yêu cầu

- Android Studio tương thích AGP 9.2+
- JDK 17 trở lên
- Android SDK 36.1
- Firebase project nếu dùng Auth, Crashlytics hoặc FCM thật

## Chạy project

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Debug build dùng fake authentication để app chạy ngay cả khi chưa có Firebase. Nhập email hợp lệ và mật khẩu từ 6 ký tự. Release build luôn dùng Firebase Auth và sẽ trả lỗi có hướng dẫn nếu chưa cấu hình.

## Kiến trúc

- `core/`: networking, typed DataStore, Room, DI, logging và notification.
- `domain/`: model, repository interface và use case thuần Kotlin.
- `data/`: Firebase/REST implementation, DTO, DAO và entity.
- `feature/`: splash, login, register và home/settings.
- `ui/`: Material 3 theme, token và component dùng lại.
- `navigation/`: route type-safe và deep link `appbase://home`.

Token đăng nhập được lưu trong typed DataStore sử dụng protobuf binary serialization. Room export schema vào `app/schemas`; khi tăng database version, thêm `Migration` và đăng ký trong `AppDatabase.MIGRATIONS`. Token, FCM token và database được loại khỏi Android cloud backup/device transfer.

## Tạo app sản phẩm mới

1. Đổi `rootProject.name`, `namespace`, `applicationId` và package Kotlin.
2. Đổi tên app, icon, màu thương hiệu và typography.
3. Đổi `BASE_URL` trong `app/build.gradle.kts`.
4. Đổi deep-link scheme/host trong `Constants`, `DeepLinkHandler` và `AndroidManifest.xml`.
5. Thay các màn hình demo trong `feature/` bằng tính năng sản phẩm.
6. Đổi `USE_FAKE_AUTH` thành `false` cho mọi build variant dùng backend thật.

## Firebase

1. Tạo Android app trong Firebase Console với đúng `applicationId`.
2. Tải `google-services.json` vào `app/google-services.json`.
3. Bật Email/Password Auth, Crashlytics và Cloud Messaging.
4. Với Firebase Auth Emulator, cấu hình emulator trước khi gọi Auth trong `FirebaseProvider` ở debug build.
5. Chạy app, đăng nhập và gửi notification thử từ Firebase Console.

Google Services và Crashlytics Gradle plugin chỉ được bật khi file cấu hình tồn tại, nên clone mới vẫn build được. `google-services.json`, keystore và `keystore.properties` được ignore khỏi template.

## Release Google Play

```bash
./gradlew lintDebug testDebugUnitTest bundleRelease
```

`bundleRelease` tạo AAB chưa ký nếu chưa cấu hình signing. Tạo keystore ngoài repository, đọc thông tin ký từ environment/secret của CI và không commit mật khẩu. Trước khi upload cần đổi package, version, icon, privacy policy, Data safety form và kiểm tra target SDK theo yêu cầu Play hiện hành.

Workflow `.github/workflows/android.yml` chạy lint, unit test, build APK debug và AAB release. Firebase Console/FCM/Crashlytics là kiểm thử tích hợp bên ngoài, không thể xác minh chỉ bằng CI khi chưa có Firebase project và thiết bị/emulator.
