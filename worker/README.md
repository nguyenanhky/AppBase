# AppBase AI Worker (Cloudflare)

Backend nhỏ cho tính năng AI của app. Worker giữ **key Gemini, prompt, JSON schema và quota**; app chỉ gửi *tên task* + ảnh. Không có key AI nào nằm trong APK.

```
App ──(Firebase ID token + App Check token)──► Worker ──► Gemini (generateContent)
                                                 ├─ Durable Object: quota mỗi người/ngày (0h giờ VN)
                                                 └─ D1: báo cáo nội dung AI
```

Mỗi app clone từ base có **Worker riêng** (env `dev` và `prod` trong `wrangler.jsonc`).

## Cấu trúc

| Đường dẫn | Vai trò |
|---|---|
| `src/index.ts` | Router `GET /health`, `POST /v1/run`, `POST /v1/report` |
| `src/auth/tokens.ts` | Xác minh App Check (403) rồi Firebase ID token (401), cả người dùng ẩn danh |
| `src/quota/` | `QuotaCounter` (Durable Object, mỗi uid một instance) + tính ngày theo múi giờ |
| `src/tasks/` | Task AI: model, prompt, schema, số lượt tốn. **Thêm task mới ở đây** |
| `src/gemini.ts` | Gọi Gemini, nhận diện nội dung bị chặn |
| `src/validate.ts` | Kiểm tra request, kiểm tra kết quả khớp schema |
| `migrations/` | Schema D1 (bảng `reports`) |

## Hành vi `POST /v1/run`

1. App Check sai/thiếu → `403 app_check_failed` (chế độ `monitor` chỉ ghi log).
2. ID token sai/hết hạn → `401` (app tự refresh token rồi gửi lại).
3. Task không bật → `404 unknown_task`; ảnh sai số lượng/định dạng → `400`; quá lớn → `413`.
4. Hết lượt → `429 quota_exceeded` + `resetAt`.
5. Gemini chặn nội dung → `422 content_blocked` (**vẫn tính lượt**).
6. Gemini lỗi hoặc trả JSON sai schema → `502 upstream_error` (**hoàn lượt**).
7. Thành công → `200 { result, quota: { remaining, limit, resetAt } }`.

## Phát triển

```bash
npm ci
npm test           # vitest chạy trong workerd: token tự ký, Gemini giả lập, D1 + Durable Object thật
npm run typecheck
cp .dev.vars.example .dev.vars   # điền GEMINI_API_KEY để chạy thử
npm run dev        # http://localhost:8787/health
```

Sau khi sửa `wrangler.jsonc`, chạy `npm run types` để cập nhật `worker-configuration.d.ts`.

## Deploy lần đầu (env `dev`)

**Chuẩn bị** (một lần):
- Firebase project có app Android (`com.viettel.appbase.debug` cho dev), bật **Email/Password** và **Anonymous** sign-in.
- Firebase Console → App Check → đăng ký app với **Play Integrity**.
- Gemini API key từ Google AI Studio, dùng **gói trả phí** (ảnh người dùng không được dùng để huấn luyện) và đặt **budget alert** trên Google Cloud.
- Tài khoản Cloudflare.

**Các bước:**

```bash
npx wrangler login
npx wrangler d1 create appbase-ai-reports-dev        # copy database_id vào env.dev trong wrangler.jsonc
npx wrangler d1 migrations apply appbase-ai-reports-dev --env dev --remote
npx wrangler secret put GEMINI_API_KEY --env dev
npm run deploy:dev                                    # in ra URL https://appbase-ai-dev.<account>.workers.dev
```

Điền trong `wrangler.jsonc` (env `dev`):
- `FIREBASE_PROJECT_ID`, `FIREBASE_PROJECT_NUMBER`: Firebase Console → Project settings.
- `ALLOWED_APP_IDS`: Firebase App ID của app (dạng `1:123:android:abc`), nhiều app thì ngăn cách bằng dấu phẩy.

Phía app: đặt `ai.workerUrl.dev=https://appbase-ai-dev.<account>.workers.dev/` trong `gradle.properties`, thêm `app/google-services.json`, chạy `devDebug`.

**App Check khi debug:** bản debug dùng debug provider. Lần chạy đầu, logcat in ra debug token (tìm `DebugAppCheckProvider`) → Firebase Console → App Check → *Manage debug tokens* → thêm token đó. Khi app đã gửi App Check hợp lệ (log Worker không còn `app_check_failed_monitor`), đổi `APP_CHECK_MODE` sang `"enforce"` và deploy lại.

`prod` làm tương tự với `--env prod` (đã để sẵn `APP_CHECK_MODE: "enforce"`).

## Vận hành

```bash
npx wrangler tail --env dev                                                     # xem log trực tiếp
npx wrangler d1 execute appbase-ai-reports-dev --env dev --remote \
  --command "SELECT created_at, uid, task, reason, result_json FROM reports ORDER BY id DESC LIMIT 20"
```

- **Đổi model:** sửa `model` trong file task. Kiểm tra tên model hiện hành ở https://ai.google.dev/gemini-api/docs/models.
- **Đổi giới hạn:** `DAILY_LIMIT`, `REPORT_DAILY_LIMIT`, múi giờ `QUOTA_UTC_OFFSET_MINUTES` (420 = giờ Việt Nam).
- **Lỗi "User location is not supported"** (Gemini từ chối vùng mà Worker đang chạy, log `gemini_error` status 400): bật Smart Placement (`"placement": { "mode": "smart" }` trong env) hoặc đi qua Cloudflare AI Gateway bằng cách đổi `GEMINI_BASE_URL`.

## Thêm task AI cho app mới

1. Tạo `src/tasks/<ten_task>.ts` theo mẫu `identify_object.ts`: prompt (luôn có cách trả lời "không nhận diện được"), `responseSchema`, `model`, `cost`, số ảnh.
2. Đăng ký trong `src/tasks/index.ts` và thêm tên vào `ENABLED_TASKS`.
3. Phía app: data class `@Serializable` **khớp đúng** schema + mẫu JSON cho `FakeAiClient` (flavor `mock`).
4. Thêm test trong `test/run.test.ts`, `npm test`, deploy.
