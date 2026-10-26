import { authenticate, googleKeySets, type KeySets } from "./auth/tokens";
import { type AppEnv, type Config, readConfig } from "./env";
import { errorResponse, HttpError, json } from "./errors";
import { generate } from "./gemini";
import { quotaDay } from "./quota/day";
import { findTask } from "./tasks";
import { checkImagesForTask, matchesSchema, parseReportRequest, parseRunRequest, readJson } from "./validate";

export { QuotaCounter } from "./quota/QuotaCounter";

export interface Deps {
	keys: KeySets;
	now: () => number;
}

export default {
	fetch: (request: Request, env: AppEnv) => handle(request, env, { keys: googleKeySets, now: Date.now }),
} satisfies ExportedHandler<AppEnv>;

/** Exported for tests, which inject local signing keys and a fixed clock. */
export async function handle(request: Request, env: AppEnv, deps: Deps): Promise<Response> {
	const { pathname } = new URL(request.url);
	try {
		if (pathname === "/health") return json({ ok: true });
		if (pathname !== "/v1/run" && pathname !== "/v1/report") throw new HttpError(404, "not_found");
		if (request.method !== "POST") throw new HttpError(405, "method_not_allowed");
		const config = readConfig(env);
		const uid = await authenticate(request, config, deps.keys);
		return pathname === "/v1/run"
			? await run(request, env, config, uid, deps)
			: await report(request, env, config, uid, deps);
	} catch (error) {
		if (error instanceof HttpError) return errorResponse(error);
		console.error(JSON.stringify({ event: "internal_error", message: String(error) }));
		return errorResponse(new HttpError(500, "internal_error"));
	}
}

async function run(request: Request, env: AppEnv, config: Config, uid: string, deps: Deps): Promise<Response> {
	const body = parseRunRequest(await readJson(request));
	const task = findTask(body.task, config.enabledTasks);
	if (!task) throw new HttpError(404, "unknown_task");
	checkImagesForTask(body.images, task);

	const { day, resetAt } = quotaDay(deps.now(), config.utcOffsetMinutes);
	const quota = env.QUOTA.get(env.QUOTA.idFromName(uid));
	const consumed = await quota.consume("run", day, task.cost, config.dailyLimit);
	if (!consumed.allowed) throw new HttpError(429, "quota_exceeded", { resetAt });

	let result: unknown;
	try {
		result = await generate(task, body, config);
	} catch (error) {
		// Blocked content still costs a run (it was processed); our/upstream failures do not.
		if (!(error instanceof HttpError && error.code === "content_blocked")) await quota.refund("run", day, task.cost);
		throw error;
	}
	if (!matchesSchema(task, result)) {
		await quota.refund("run", day, task.cost);
		console.error(JSON.stringify({ event: "schema_mismatch", task: task.id }));
		throw new HttpError(502, "upstream_error");
	}
	console.log(JSON.stringify({ event: "run", task: task.id, used: consumed.used }));
	return json({
		result,
		quota: { remaining: config.dailyLimit - consumed.used, limit: config.dailyLimit, resetAt },
	});
}

async function report(request: Request, env: AppEnv, config: Config, uid: string, deps: Deps): Promise<Response> {
	const body = parseReportRequest(await readJson(request));
	const { day } = quotaDay(deps.now(), config.utcOffsetMinutes);
	const quota = env.QUOTA.get(env.QUOTA.idFromName(uid));
	if (!(await quota.consume("report", day, 1, config.reportDailyLimit)).allowed) {
		throw new HttpError(429, "quota_exceeded");
	}
	await env.REPORTS.prepare(
		"INSERT INTO reports (uid, task, result_json, reason, created_at) VALUES (?, ?, ?, ?, ?)",
	)
		.bind(uid, body.task, JSON.stringify(body.result), body.reason, new Date(deps.now()).toISOString())
		.run();
	return new Response(null, { status: 204 });
}
