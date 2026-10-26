import { describe, expect, it } from "vitest";
import { authenticate } from "../src/auth/tokens";
import { readConfig } from "../src/env";
import { HttpError } from "../src/errors";
import { appCheckToken, appEnv, idToken, keys } from "./helpers";

const config = readConfig(appEnv);

const request = (headers: Record<string, string>) => new Request("https://worker.test/v1/run", { headers });

async function status(headers: Record<string, string>, cfg = config) {
	try {
		await authenticate(request(headers), cfg, keys);
		return 200;
	} catch (error) {
		return (error as HttpError).status;
	}
}

describe("authenticate", () => {
	it("returns the uid for valid ID and App Check tokens", async () => {
		const uid = await authenticate(
			request({ Authorization: `Bearer ${await idToken({ sub: "anon-42" })}`, "X-Firebase-AppCheck": await appCheckToken() }),
			config,
			keys,
		);
		expect(uid).toBe("anon-42");
	});

	it("rejects expired, foreign-project and missing ID tokens with 401", async () => {
		const appCheck = await appCheckToken();
		const expired = await idToken({ expiresAt: Math.floor(Date.now() / 1000) - 3600 });
		const wrongIssuer = await idToken({ issuer: "https://securetoken.google.com/other" });
		const wrongAudience = await idToken({ audience: "other" });
		for (const token of [expired, wrongIssuer, wrongAudience]) {
			expect(await status({ Authorization: `Bearer ${token}`, "X-Firebase-AppCheck": appCheck })).toBe(401);
		}
		expect(await status({ "X-Firebase-AppCheck": appCheck })).toBe(401);
	});

	it("rejects missing or foreign App Check tokens with 403 before looking at the user", async () => {
		const id = `Bearer ${await idToken()}`;
		expect(await status({ Authorization: id })).toBe(403);
		expect(await status({ Authorization: id, "X-Firebase-AppCheck": await appCheckToken({ appId: "1:9:android:x" }) })).toBe(403);
		expect(await status({ Authorization: id, "X-Firebase-AppCheck": "garbage" })).toBe(403);
	});

	it("only logs App Check failures in monitor mode", async () => {
		const monitor = { ...config, enforceAppCheck: false };
		expect(await status({ Authorization: `Bearer ${await idToken()}` }, monitor)).toBe(200);
		expect(await status({}, monitor)).toBe(401);
	});
});
