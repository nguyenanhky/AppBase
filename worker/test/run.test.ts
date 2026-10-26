import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { handle } from "../src/index";
import { appEnv, authHeaders, deps, geminiJson, IMAGE, post, runBody } from "./helpers";

const ladybug = { recognized: true, name: "Bọ rùa", confidence: 0.9, advice: ["Không cần diệt."] };

// Storage persists across tests, so each test is its own user (as in production: one counter per uid).
let user = "";
beforeEach(() => {
	user = crypto.randomUUID();
});

async function run(body = runBody(), as = user) {
	return handle(post("/v1/run", body, await authHeaders(as)), appEnv, deps());
}

afterEach(() => vi.restoreAllMocks());

describe("POST /v1/run", () => {
	it("calls Gemini with the server-side prompt and returns the result with quota", async () => {
		const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(geminiJson(ladybug));

		const response = await run();

		expect(response.status).toBe(200);
		expect(await response.json()).toEqual({
			result: ladybug,
			quota: { remaining: 2, limit: 3, resetAt: "2026-10-26T17:00:00.000Z" },
		});
		const [url, init] = fetchSpy.mock.calls[0]!;
		expect(String(url)).toContain("/v1beta/models/gemini-3.5-flash-lite:generateContent");
		expect((init?.headers as Record<string, string>)["x-goog-api-key"]).toBe("test-key");
		const sent = JSON.parse(String(init?.body));
		expect(sent.systemInstruction.parts[0].text).toContain("recognized=false");
		expect(sent.contents[0].parts).toContainEqual({ inlineData: IMAGE });
		expect(sent.generationConfig.responseMimeType).toBe("application/json");
	});

	it("accepts a 'not recognized' answer", async () => {
		vi.spyOn(globalThis, "fetch").mockResolvedValue(geminiJson({ recognized: false }));

		expect((await run()).status).toBe(200);
	});

	it("enforces the daily quota per user", async () => {
		vi.spyOn(globalThis, "fetch").mockImplementation(async () => geminiJson(ladybug));
		for (let i = 0; i < 3; i++) expect((await run()).status).toBe(200);

		const blocked = await run();

		expect(blocked.status).toBe(429);
		expect(await blocked.json()).toEqual({ error: "quota_exceeded", resetAt: "2026-10-26T17:00:00.000Z" });
		expect((await run(runBody(), crypto.randomUUID())).status).toBe(200);
	});

	it("maps safety blocks to content_blocked and still counts the run", async () => {
		vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
			new Response(JSON.stringify({ promptFeedback: { blockReason: "SAFETY" } })),
		);

		const response = await run();

		expect(response.status).toBe(422);
		expect(await response.json()).toEqual({ error: "content_blocked" });
		vi.spyOn(globalThis, "fetch").mockResolvedValue(geminiJson(ladybug));
		expect(((await (await run()).json()) as { quota: { remaining: number } }).quota.remaining).toBe(1);
	});

	it("refunds the run when Gemini fails", async () => {
		vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(new Response("boom", { status: 500 }));

		expect((await run()).status).toBe(502);

		vi.spyOn(globalThis, "fetch").mockResolvedValue(geminiJson(ladybug));
		expect(((await (await run()).json()) as { quota: { remaining: number } }).quota.remaining).toBe(2);
	});

	it("never returns output that breaks the schema, and refunds", async () => {
		vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(geminiJson({ recognized: "yes", hacked: true }));

		const response = await run();

		expect(response.status).toBe(502);
		expect(await response.json()).toEqual({ error: "upstream_error" });
		vi.spyOn(globalThis, "fetch").mockResolvedValue(geminiJson(ladybug));
		expect(((await (await run()).json()) as { quota: { remaining: number } }).quota.remaining).toBe(2);
	});

	it("rejects unknown tasks and invalid images before spending quota or calling Gemini", async () => {
		const fetchSpy = vi.spyOn(globalThis, "fetch");

		expect((await run(runBody({ task: "steal_prompts" }))).status).toBe(404);
		expect((await run(runBody({ images: [] }))).status).toBe(400);
		expect((await run(runBody({ images: [IMAGE, IMAGE] }))).status).toBe(400);
		expect((await run(runBody({ images: [{ mimeType: "image/gif", data: IMAGE.data }] }))).status).toBe(400);
		expect((await run(runBody({ images: [{ mimeType: "image/jpeg", data: "not base64!" }] }))).status).toBe(400);
		expect(fetchSpy).not.toHaveBeenCalled();
	});

	it("requires authentication", async () => {
		const response = await handle(post("/v1/run", runBody(), {}), appEnv, deps());

		expect(response.status).toBe(403);
		expect(await response.json()).toEqual({ error: "app_check_failed" });
	});
});
