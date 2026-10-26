import type { QuotaCounter } from "./quota/QuotaCounter";

/** Bindings, vars (wrangler.jsonc) and secrets (wrangler secret put) of one deployment. */
export interface AppEnv {
	QUOTA: DurableObjectNamespace<QuotaCounter>;
	REPORTS: D1Database;
	GEMINI_API_KEY: string;
	FIREBASE_PROJECT_ID: string;
	FIREBASE_PROJECT_NUMBER: string;
	ALLOWED_APP_IDS: string;
	APP_CHECK_MODE: string;
	ENABLED_TASKS: string;
	DAILY_LIMIT: string;
	REPORT_DAILY_LIMIT: string;
	QUOTA_UTC_OFFSET_MINUTES: string;
	GEMINI_BASE_URL: string;
}

export interface Config {
	projectId: string;
	projectNumber: string;
	allowedAppIds: Set<string>;
	enforceAppCheck: boolean;
	enabledTasks: Set<string>;
	dailyLimit: number;
	reportDailyLimit: number;
	utcOffsetMinutes: number;
	geminiBaseUrl: string;
	geminiApiKey: string;
}

const list = (value: string) =>
	new Set(
		value
			.split(",")
			.map((item) => item.trim())
			.filter(Boolean),
	);

const positiveInt = (name: string, value: string) => {
	const parsed = Number.parseInt(value, 10);
	if (!Number.isFinite(parsed) || parsed < 0) throw new Error(`Invalid ${name}: ${value}`);
	return parsed;
};

export function readConfig(env: AppEnv): Config {
	return {
		projectId: env.FIREBASE_PROJECT_ID,
		projectNumber: env.FIREBASE_PROJECT_NUMBER,
		allowedAppIds: list(env.ALLOWED_APP_IDS),
		enforceAppCheck: env.APP_CHECK_MODE !== "monitor",
		enabledTasks: list(env.ENABLED_TASKS),
		dailyLimit: positiveInt("DAILY_LIMIT", env.DAILY_LIMIT),
		reportDailyLimit: positiveInt("REPORT_DAILY_LIMIT", env.REPORT_DAILY_LIMIT),
		utcOffsetMinutes: Number.parseInt(env.QUOTA_UTC_OFFSET_MINUTES, 10) || 0,
		geminiBaseUrl: env.GEMINI_BASE_URL.replace(/\/+$/, ""),
		geminiApiKey: env.GEMINI_API_KEY,
	};
}
