import { env } from "cloudflare:workers";
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from "jose";
import type { KeySets } from "../src/auth/tokens";
import type { AppEnv } from "../src/env";
import type { Deps } from "../src/index";

export const PROJECT_ID = "test-project";
export const PROJECT_NUMBER = "123456789";
export const APP_ID = "1:123456789:android:abc";
/** 2026-10-26 17:00 in Vietnam (UTC+7). */
export const NOW = Date.UTC(2026, 9, 26, 10, 0);

export const appEnv = env as unknown as AppEnv;

const idKeyPair = await generateKeyPair("RS256", { extractable: true });
const appCheckKeyPair = await generateKeyPair("RS256", { extractable: true });

export const keys: KeySets = {
	idToken: createLocalJWKSet({ keys: [{ ...(await exportJWK(idKeyPair.publicKey)), kid: "id", alg: "RS256" }] }),
	appCheck: createLocalJWKSet({
		keys: [{ ...(await exportJWK(appCheckKeyPair.publicKey)), kid: "ac", alg: "RS256" }],
	}),
};

export const deps = (now = NOW): Deps => ({ keys, now: () => now });

const seconds = () => Math.floor(Date.now() / 1000);

export function idToken(
	overrides: { sub?: string; issuer?: string; audience?: string; expiresAt?: number } = {},
) {
	return new SignJWT({})
		.setProtectedHeader({ alg: "RS256", kid: "id" })
		.setIssuer(overrides.issuer ?? `https://securetoken.google.com/${PROJECT_ID}`)
		.setAudience(overrides.audience ?? PROJECT_ID)
		.setSubject(overrides.sub ?? "user-1")
		.setIssuedAt(seconds() - 60)
		.setExpirationTime(overrides.expiresAt ?? seconds() + 3600)
		.sign(idKeyPair.privateKey);
}

export function appCheckToken(overrides: { appId?: string; issuer?: string } = {}) {
	return new SignJWT({})
		.setProtectedHeader({ alg: "RS256", kid: "ac" })
		.setIssuer(overrides.issuer ?? `https://firebaseappcheck.googleapis.com/${PROJECT_NUMBER}`)
		.setAudience([`projects/${PROJECT_NUMBER}`, `projects/${PROJECT_ID}`])
		.setSubject(overrides.appId ?? APP_ID)
		.setIssuedAt(seconds() - 60)
		.setExpirationTime(seconds() + 3600)
		.sign(appCheckKeyPair.privateKey);
}

export async function authHeaders(user = "user-1"): Promise<Record<string, string>> {
	return { Authorization: `Bearer ${await idToken({ sub: user })}`, "X-Firebase-AppCheck": await appCheckToken() };
}

export const post = (path: string, body: unknown, headers: Record<string, string>) =>
	new Request(`https://worker.test${path}`, {
		method: "POST",
		headers: { "content-type": "application/json", ...headers },
		body: JSON.stringify(body),
	});

/** A tiny valid JPEG-ish base64 payload (content is not inspected by the Worker). */
export const IMAGE = { mimeType: "image/jpeg", data: "/9j/4AAQSkZJRgABAQ==" };

export const runBody = (overrides: Record<string, unknown> = {}) => ({
	task: "identify_object",
	locale: "vi-VN",
	images: [IMAGE],
	...overrides,
});

export const geminiJson = (result: unknown, finishReason = "STOP") =>
	new Response(
		JSON.stringify({ candidates: [{ finishReason, content: { parts: [{ text: JSON.stringify(result) }] } }] }),
		{ status: 200, headers: { "content-type": "application/json" } },
	);
