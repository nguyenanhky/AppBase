/** Error codes are part of the app contract (see README "API v1"); keep them stable. */
export type ErrorCode =
	| "invalid_request"
	| "unauthorized"
	| "app_check_failed"
	| "not_found"
	| "method_not_allowed"
	| "unknown_task"
	| "payload_too_large"
	| "content_blocked"
	| "quota_exceeded"
	| "upstream_error"
	| "internal_error";

export class HttpError extends Error {
	constructor(
		readonly status: number,
		readonly code: ErrorCode,
		readonly extra: Record<string, unknown> = {},
	) {
		super(code);
	}
}

export const json = (body: unknown, status = 200) =>
	new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

export const errorResponse = (error: HttpError) => json({ error: error.code, ...error.extra }, error.status);
