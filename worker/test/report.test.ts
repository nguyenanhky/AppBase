import { beforeEach, describe, expect, it } from "vitest";
import { handle } from "../src/index";
import { appEnv, authHeaders, deps, post } from "./helpers";

let user = "";
beforeEach(() => {
	user = crypto.randomUUID();
});

async function report(body: unknown) {
	return handle(post("/v1/report", body, await authHeaders(user)), appEnv, deps());
}

describe("POST /v1/report", () => {
	it("stores the report in D1", async () => {
		const response = await report({ task: "identify_object", result: { recognized: true, name: "X" }, reason: "user_flagged" });

		expect(response.status).toBe(204);
		const row = await appEnv.REPORTS.prepare("SELECT uid, task, result_json, reason FROM reports WHERE uid = ?").bind(user).first();
		expect(row).toEqual({
			uid: user,
			task: "identify_object",
			result_json: '{"recognized":true,"name":"X"}',
			reason: "user_flagged",
		});
	});

	it("rejects malformed bodies", async () => {
		expect((await report({ task: "identify_object" })).status).toBe(400);
	});

	it("limits reports per user per day", async () => {
		const body = { task: "identify_object", result: {}, reason: "spam" };
		expect((await report(body)).status).toBe(204);
		expect((await report(body)).status).toBe(204);
		expect((await report(body)).status).toBe(429);
	});
});

describe("routing", () => {
	it("serves health, rejects unknown paths and wrong methods", async () => {
		expect((await handle(new Request("https://worker.test/health"), appEnv, deps())).status).toBe(200);
		expect((await handle(new Request("https://worker.test/admin"), appEnv, deps())).status).toBe(404);
		expect((await handle(new Request("https://worker.test/v1/run"), appEnv, deps())).status).toBe(405);
	});
});
