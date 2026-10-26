import { cloudflareTest, readD1Migrations } from "@cloudflare/vitest-plugin";
import { defineConfig } from "vitest/config";

// Tests run inside workerd with the dev environment's bindings; vars below override wrangler.jsonc.
export default defineConfig(async () => {
	const migrations = await readD1Migrations("./migrations");
	return {
		plugins: [
			cloudflareTest({
				wrangler: { configPath: "./wrangler.jsonc", environment: "dev" },
				miniflare: {
					bindings: {
						TEST_MIGRATIONS: migrations,
						GEMINI_API_KEY: "test-key",
						FIREBASE_PROJECT_ID: "test-project",
						FIREBASE_PROJECT_NUMBER: "123456789",
						ALLOWED_APP_IDS: "1:123456789:android:abc",
						APP_CHECK_MODE: "enforce",
						DAILY_LIMIT: "3",
						REPORT_DAILY_LIMIT: "2",
					},
				},
			}),
		],
		test: { setupFiles: ["./test/setup.ts"] },
	};
});
