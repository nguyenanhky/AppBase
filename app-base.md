# Android App Base — Project Plan

> **Mục tiêu:** Xây dựng một "xương sống" (skeleton) chuẩn cho các Android app tương lai, bao gồm tất cả các module nền tảng được cấu hình sẵn và sẵn sàng clone/reuse.

---

## 📋 Overview

| Field | Value |
|-------|-------|
| **Project Type** | MOBILE (Android Native) |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Architecture** | Clean Architecture + MVVM |
| **DI** | Koin |
| **Module Structure** | Single Module (Monolith) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |
| **Package** | `com.viettel.appbase` |
| **Status** | 🟢 Implemented — external Firebase verification pending |

---

## ✅ Success Criteria

- [ ] Ứng dụng build thành công (0 errors, 0 warnings)
- [ ] Tất cả layers của Clean Architecture được tách biệt rõ ràng
- [ ] Authentication flow hoạt động end-to-end (Login → Token → Refresh)
- [ ] Navigation với Deep Link hoạt động đúng
- [ ] Networking layer có interceptors (Auth, Logging, Error handling)
- [ ] Room database có migration setup sẵn
- [ ] UI Design System: theme sáng/tối, typography, spacing, components cơ bản
- [ ] Koin DI hoạt động và inject đúng ở mọi layer
- [ ] Firebase Crashlytics ghi nhận crash
- [ ] Firebase Push Notification nhận được message
- [ ] Unit test coverage ≥ 60% trên domain/data layer
- [ ] README hướng dẫn clone và customize cho app mới

---

## 🛠️ Tech Stack

| Category | Library | Rationale |
|----------|---------|-----------|
| **Language** | Kotlin | Kotlin-first Android |
| **UI** | Jetpack Compose + Material3 | Declarative, modern |
| **Architecture** | Clean Arch + MVVM | Separation of concerns |
| **DI** | Koin 3.x | Lightweight, no annotation processing |
| **Networking** | Retrofit + OkHttp | Chuẩn mực, stable |
| **Local DB** | Room 2.x | SQLite abstraction |
| **Preferences** | DataStore (Proto) | Thay SharedPreferences |
| **Auth** | Firebase Auth | Google Auth, Email, Phone |
| **Push** | Firebase Cloud Messaging | Push notifications |
| **Crash** | Firebase Crashlytics | Production crash reporting |
| **Logging** | Timber 5.x | Structured logging |
| **Navigation** | Compose Navigation | Type-safe routes |
| **Async** | Kotlin Coroutines + Flow | Reactive streams |
| **Image** | Coil 2.x | Compose-friendly |
| **Testing** | JUnit4 + MockK + Turbine | Unit + Flow testing |

---

## 📁 File Structure

```
AppBase/
├── app/
│   └── src/main/java/com/viettel/appbase/
│       ├── AppApplication.kt          ← Koin init, Timber init
│       ├── MainActivity.kt            ← Entry point, NavHost
│       │
│       ├── core/                      ← Shared utilities
│       │   ├── base/
│       │   │   ├── BaseViewModel.kt
│       │   │   └── UiState.kt
│       │   ├── extensions/
│       │   ├── network/
│       │   │   ├── ApiClient.kt
│       │   │   ├── interceptors/
│       │   │   │   ├── AuthInterceptor.kt
│       │   │   │   ├── LoggingInterceptor.kt
│       │   │   │   └── ErrorInterceptor.kt
│       │   │   └── NetworkResult.kt
│       │   ├── storage/
│       │   │   ├── AppDatabase.kt
│       │   │   └── AppDataStore.kt
│       │   ├── di/
│       │   │   ├── NetworkModule.kt
│       │   │   ├── DatabaseModule.kt
│       │   │   └── AppModule.kt
│       │   └── utils/
│       │       ├── Logger.kt
│       │       └── Constants.kt
│       │
│       ├── ui/                        ← Design System
│       │   ├── theme/
│       │   │   ├── AppTheme.kt
│       │   │   ├── Color.kt
│       │   │   ├── Typography.kt
│       │   │   └── Shape.kt
│       │   └── components/
│       │       ├── AppButton.kt
│       │       ├── AppTextField.kt
│       │       ├── AppTopBar.kt
│       │       ├── LoadingDialog.kt
│       │       └── ErrorView.kt
│       │
│       ├── navigation/
│       │   ├── AppNavHost.kt
│       │   ├── AppRoutes.kt
│       │   └── DeepLinkHandler.kt
│       │
│       ├── data/
│       │   ├── remote/api/AuthApi.kt
│       │   ├── remote/dto/
│       │   ├── local/dao/
│       │   ├── local/entity/
│       │   └── repository/AuthRepositoryImpl.kt
│       │
│       ├── domain/
│       │   ├── model/
│       │   ├── repository/AuthRepository.kt
│       │   └── usecase/
│       │       ├── LoginUseCase.kt
│       │       ├── LogoutUseCase.kt
│       │       └── RefreshTokenUseCase.kt
│       │
│       └── feature/
│           ├── splash/SplashScreen.kt + SplashViewModel.kt
│           ├── auth/login/LoginScreen.kt + LoginViewModel.kt
│           ├── auth/register/RegisterScreen.kt + RegisterViewModel.kt
│           └── home/HomeScreen.kt + HomeViewModel.kt
│
├── gradle/libs.versions.toml
└── README.md
```

---

## 📦 Task Breakdown

### 🔵 PHASE 0: Foundation Setup

#### Task 0.1 — Version Catalog & Dependencies
- **Agent:** `mobile-developer` | **Priority:** P0 (Blocker)
- **INPUT:** `libs.versions.toml` chỉ có Compose BOM
- **OUTPUT:** Đầy đủ: Koin, Retrofit, Room, Firebase, Timber, Coil, Navigation, DataStore, MockK, Turbine
- **VERIFY:** `./gradlew dependencies` không conflict; project builds

#### Task 0.2 — Root & App Build Config
- **Agent:** `mobile-developer` | **Priority:** P0 | **Depends:** Task 0.1
- **INPUT:** `build.gradle.kts` hiện tại
- **OUTPUT:** Plugins: Koin, Firebase (google-services), KSP (Room); buildConfig; buildTypes debug/release
- **VERIFY:** `./gradlew assembleDebug` pass

#### Task 0.3 — Application Class & Koin Init
- **Agent:** `mobile-developer` | **Priority:** P0 | **Depends:** Task 0.2
- **INPUT:** Chưa có Application class
- **OUTPUT:** `AppApplication.kt` với `startKoin{}`, Timber `plant()`, Firebase init
- **VERIFY:** App khởi động không crash; Timber log xuất hiện trong Logcat

---

### 🔵 PHASE 1: Core Layer

#### Task 1.1 — Networking Layer
- **Agent:** `mobile-developer` | **Skill:** `@api-patterns` | **Priority:** P1 | **Depends:** Task 0.3
- **INPUT:** Chưa có network layer
- **OUTPUT:** `ApiClient.kt`, `AuthInterceptor`, `LoggingInterceptor` (debug only), `ErrorInterceptor`, `NetworkResult` sealed class, `NetworkModule.kt` (Koin)
- **VERIFY:** Unit test AuthInterceptor attach token đúng

#### Task 1.2 — Local Storage (Room + DataStore)
- **Agent:** `mobile-developer` | **Skill:** `@database-design` | **Priority:** P1 | **Depends:** Task 0.3
- **INPUT:** Chưa có local storage
- **OUTPUT:** `AppDatabase.kt` (Room, version 1, export schema), `AppDataStore.kt`, `DatabaseModule.kt` (Koin)
- **VERIFY:** `./gradlew test` pass; schema file generated tại `schemas/`

#### Task 1.3 — Base ViewModel & UiState
- **Agent:** `mobile-developer` | **Priority:** P1 | **Depends:** Task 0.3
- **INPUT:** Chưa có base class
- **OUTPUT:** `BaseViewModel.kt`, `UiState.kt` (sealed: Loading/Success/Error/Empty)
- **VERIFY:** Unit test state transitions

---

### 🔵 PHASE 2: Auth Feature (End-to-End)

#### Task 2.1 — Domain Layer (Auth)
- **Agent:** `mobile-developer` | **Priority:** P2 | **Depends:** Task 1.1, 1.2
- **INPUT:** Chưa có domain
- **OUTPUT:** `AuthRepository` interface, UseCases (Login/Logout/RefreshToken), domain models (User, AuthToken)
- **VERIFY:** Pure Kotlin, không có Android import; unit test mỗi UseCase

#### Task 2.2 — Data Layer (Auth)
- **Agent:** `mobile-developer` | **Priority:** P2 | **Depends:** Task 2.1
- **INPUT:** Domain interfaces
- **OUTPUT:** `AuthApi.kt` (Retrofit), DTOs, `AuthRepositoryImpl.kt`, `AuthModule.kt` (Koin)
- **VERIFY:** Integration test repository; AuthInterceptor inject token từ DataStore

#### Task 2.3 — Auth UI (Login + Register)
- **Agent:** `mobile-developer` | **Skill:** `@mobile-design` | **Priority:** P2 | **Depends:** Task 2.2, Phase 3
- **INPUT:** Domain + Data layer hoàn chỉnh
- **OUTPUT:** `LoginScreen.kt`, `LoginViewModel.kt`, `RegisterScreen.kt`, `RegisterViewModel.kt`; form validation; loading/error states
- **VERIFY:** Screen hiển thị đúng; ViewModel state transitions; form validation hoạt động

---

### 🔵 PHASE 3: UI Design System (parallel với Phase 2)

#### Task 3.1 — Theme & Design Tokens
- **Agent:** `mobile-developer` | **Skill:** `@mobile-design` | **Priority:** P2 | **Depends:** Task 0.2
- **INPUT:** Default Material3 theme
- **OUTPUT:** `AppTheme.kt` (light/dark), `Color.kt`, `Typography.kt`, `Shape.kt`
- **VERIFY:** App switch dark mode đúng; không có hardcoded colors

#### Task 3.2 — Reusable Components
- **Agent:** `mobile-developer` | **Priority:** P2 | **Depends:** Task 3.1
- **INPUT:** Theme từ Task 3.1
- **OUTPUT:** `AppButton` (primary/secondary/outlined), `AppTextField`, `AppTopBar`, `LoadingDialog`, `ErrorView`, `EmptyView`
- **VERIFY:** Compose Preview render đúng tất cả variants

---

### 🔵 PHASE 4: Navigation

#### Task 4.1 — Navigation Setup + Deep Link
- **Agent:** `mobile-developer` | **Priority:** P3 | **Depends:** Task 2.3
- **INPUT:** Các screens đã có
- **OUTPUT:** `AppRoutes.kt` (type-safe routes), `AppNavHost.kt`, `DeepLinkHandler.kt`, deep link manifest config
- **VERIFY:** `adb shell am start -a android.intent.action.VIEW -d "appbase://home"` navigate đúng

#### Task 4.2 — Bottom Navigation Bar
- **Agent:** `mobile-developer` | **Priority:** P3 | **Depends:** Task 4.1
- **INPUT:** NavHost từ Task 4.1
- **OUTPUT:** Bottom nav 3-5 tab placeholder; state preservation khi switch tabs
- **VERIFY:** Back stack behavior đúng; không reload screen khi switch tab

---

### 🔵 PHASE 5: Firebase Integration

#### Task 5.1 — Firebase Auth Integration
- **Agent:** `mobile-developer` | **Priority:** P3 | **Depends:** Task 2.2
- **INPUT:** `AuthRepositoryImpl.kt`
- **OUTPUT:** Firebase Auth SDK: signIn, createUser, token management với ID token
- **VERIFY:** Login/Register flow test với Firebase emulator

#### Task 5.2 — Firebase Cloud Messaging (FCM)
- **Agent:** `mobile-developer` | **Priority:** P3 | **Depends:** Task 0.3
- **INPUT:** `AppApplication.kt`
- **OUTPUT:** `AppFirebaseMessagingService.kt`, notification channel setup, token refresh handling
- **VERIFY:** Test message từ Firebase Console → notification hiện lên

#### Task 5.3 — Firebase Crashlytics
- **Agent:** `mobile-developer` | **Priority:** P3 | **Depends:** Task 0.3
- **INPUT:** `AppApplication.kt`, Timber setup
- **OUTPUT:** Crashlytics initialized; custom `CrashlyticsTree` cho Timber; crash keys
- **VERIFY:** Test exception xuất hiện trong Firebase Console

---

### 🔵 PHASE 6: Testing & Documentation

#### Task 6.1 — Unit Tests
- **Agent:** `mobile-developer` | **Skill:** `@testing-patterns` | **Priority:** P4 | **Depends:** Phases 0–5
- **INPUT:** Domain & Data layers
- **OUTPUT:** Tests: `LoginUseCaseTest`, `AuthRepositoryImplTest`, `BaseViewModelTest`, `AuthInterceptorTest`
- **VERIFY:** `./gradlew test` pass; coverage ≥ 60% trên domain layer

#### Task 6.2 — README & Setup Guide
- **Agent:** `documentation-writer` | **Priority:** P4 | **Depends:** All phases
- **INPUT:** Codebase hoàn chỉnh
- **OUTPUT:** `README.md`: overview, prerequisites, setup steps, how to use as template, Firebase config guide
- **VERIFY:** Dev mới clone → follow README → build thành công trong < 10 phút

---

## 🔗 Dependency Graph

```
Task 0.1 ──► Task 0.2 ──► Task 0.3
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
          Task 1.1         Task 1.2         Task 1.3
          (Network)        (Storage)        (BaseVM)
              │                │
              └────────────────┤
                               ▼
                          Task 2.1 (Domain Auth)
                               │
                          Task 2.2 (Data Auth) ──► Task 5.1 (Firebase Auth)
                               │
              ┌────────────────┤
              ▼                ▼
          Task 3.1         Task 2.3 (Auth UI)
          (Theme)              │
              │                │
          Task 3.2 ────────────┘
          (Components)         │
                               ▼
                          Task 4.1 (Navigation)
                               │
                          Task 4.2 (Bottom Nav)

Task 0.3 ──► Task 5.2 (FCM)
Task 0.3 ──► Task 5.3 (Crashlytics)
[All] ──► Task 6.1 (Tests) ──► Task 6.2 (Docs)
```

---

## ⚠️ Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| `google-services.json` chưa có | High | Dùng Firebase emulator cho dev; guide trong README |
| Room schema migration conflict | Medium | Export schema từ đầu; `@Database(exportSchema = true)` |
| Koin conflict với Compose ViewModel | Low | Dùng `koinViewModel()` thay `viewModel()` |
| Token refresh race condition | Medium | Implement mutex trong AuthInterceptor |
| Compose Navigation type-safety | Low | Dùng Navigation 2.8+ với serializable routes |

---

## ✅ Phase X: Verification Checklist

### Build
- [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [ ] `./gradlew assembleRelease` → BUILD SUCCESSFUL
- [ ] 0 errors, 0 warnings

### Tests
- [ ] `./gradlew test` → ALL TESTS PASS
- [ ] Coverage ≥ 60% trên `domain/` package

### Architecture
- [ ] Domain layer không có Android imports
- [ ] Data layer chỉ depend vào Domain interfaces
- [ ] UI layer không có business logic

### Firebase
- [ ] Crashlytics nhận test crash
- [ ] FCM nhận test push notification

### Code Quality
- [ ] Không có hardcoded strings (dùng `strings.xml`)
- [ ] Không có hardcoded colors (dùng theme)
- [ ] Timber thay thế hoàn toàn `Log.*`
- [ ] Không có TODO chưa resolved trong core layers

### Documentation
- [ ] `README.md` có setup guide hoàn chỉnh
- [ ] Dev mới clone và build thành công trong < 10 phút

---

## 🧪 Implementation Result

- `assembleDebug`, `bundleRelease`, `lintDebug` và 19 unit tests đã pass.
- Coverage kết hợp `domain + data` đạt khoảng 71% theo line coverage; domain use case đạt 89% instruction coverage.
- Room schema v1 đã được export tại `app/schemas/`.
- Firebase được cấu hình tùy chọn để clone mới vẫn build khi chưa có `google-services.json`.
- Firebase Auth emulator, FCM thật, Crashlytics Console và deep link qua ADB cần Firebase project/emulator hoặc thiết bị để xác minh bên ngoài.

*Plan created: 2026-09-11 | Implemented: 2026-09-11 | Status: ✅ Code complete, external verification pending*
