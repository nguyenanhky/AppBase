import { createRemoteJWKSet, errors, jwtVerify, type JWTVerifyGetKey } from "jose";
import type { Config } from "../env";
import { HttpError } from "../errors";

export interface KeySets {
	idToken: JWTVerifyGetKey;
	appCheck: JWTVerifyGetKey;
}

/** Google's public keys, fetched and cached per isolate by jose. Tests pass local key sets instead. */
export const googleKeySets: KeySets = {
	idToken: createRemoteJWKSet(
		new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
	),
	appCheck: createRemoteJWKSet(new URL("https://firebaseappcheck.googleapis.com/v1/jwks")),
};

const CLOCK_TOLERANCE_SECONDS = 5;

/** Firebase Auth ID token (anonymous users included). Returns the uid. */
export async function verifyIdToken(token: string, config: Config, keys: KeySets): Promise<string> {
	try {
		const { payload } = await jwtVerify(token, keys.idToken, {
			algorithms: ["RS256"],
			issuer: `https://securetoken.google.com/${config.projectId}`,
			audience: config.projectId,
			clockTolerance: CLOCK_TOLERANCE_SECONDS,
		});
		if (!payload.sub) throw new Error("missing sub");
		return payload.sub;
	} catch (error) {
		if (error instanceof errors.JWKSTimeout || error instanceof errors.JWKSInvalid) throw error;
		throw new HttpError(401, "unauthorized");
	}
}

/** Firebase App Check token: proves the request comes from our genuine app. Returns the Firebase app id. */
export async function verifyAppCheck(token: string, config: Config, keys: KeySets): Promise<string> {
	try {
		const { payload } = await jwtVerify(token, keys.appCheck, {
			algorithms: ["RS256"],
			issuer: `https://firebaseappcheck.googleapis.com/${config.projectNumber}`,
			audience: `projects/${config.projectNumber}`,
			clockTolerance: CLOCK_TOLERANCE_SECONDS,
		});
		if (!payload.sub || !config.allowedAppIds.has(payload.sub)) throw new Error("app not allowed");
		return payload.sub;
	} catch (error) {
		if (error instanceof errors.JWKSTimeout || error instanceof errors.JWKSInvalid) throw error;
		throw new HttpError(403, "app_check_failed");
	}
}

/**
 * App Check first (cheap rejection of bots), then the user. In monitor mode App Check failures are
 * only logged, so the app can be wired up before enforcement is switched on.
 */
export async function authenticate(request: Request, config: Config, keys: KeySets): Promise<string> {
	const appCheckToken = request.headers.get("X-Firebase-AppCheck");
	try {
		if (!appCheckToken) throw new HttpError(403, "app_check_failed");
		await verifyAppCheck(appCheckToken, config, keys);
	} catch (error) {
		if (config.enforceAppCheck || !(error instanceof HttpError)) throw error;
		console.warn(JSON.stringify({ event: "app_check_failed_monitor" }));
	}
	const authorization = request.headers.get("Authorization") ?? "";
	const idToken = authorization.startsWith("Bearer ") ? authorization.slice("Bearer ".length) : "";
	if (!idToken) throw new HttpError(401, "unauthorized");
	return verifyIdToken(idToken, config, keys);
}
