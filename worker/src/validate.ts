import { Validator } from "@cfworker/json-schema";
import { HttpError } from "./errors";
import type { AiTask } from "./tasks/types";

export const MAX_BODY_BYTES = 4 * 1024 * 1024;
export const MAX_IMAGE_BYTES = 1.5 * 1024 * 1024;
const MAX_TEXT_CHARS = 2_000;
const MAX_REPORT_RESULT_CHARS = 10_000;
const MAX_REASON_CHARS = 200;
const ALLOWED_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

export interface RunImage {
	mimeType: string;
	data: string;
}

export interface RunRequest {
	task: string;
	locale: string;
	text?: string;
	images: RunImage[];
}

export interface ReportRequest {
	task: string;
	result: unknown;
	reason: string;
}

/** Reads a JSON body without trusting Content-Length. */
export async function readJson(request: Request): Promise<unknown> {
	const declared = Number(request.headers.get("content-length") ?? 0);
	if (declared > MAX_BODY_BYTES) throw new HttpError(413, "payload_too_large");
	const text = await request.text();
	if (text.length > MAX_BODY_BYTES) throw new HttpError(413, "payload_too_large");
	try {
		return JSON.parse(text);
	} catch {
		throw new HttpError(400, "invalid_request");
	}
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
	typeof value === "object" && value !== null && !Array.isArray(value);

export function parseRunRequest(body: unknown): RunRequest {
	if (!isRecord(body) || typeof body.task !== "string" || typeof body.locale !== "string") {
		throw new HttpError(400, "invalid_request");
	}
	const text = body.text ?? undefined;
	if (text !== undefined && (typeof text !== "string" || text.length > MAX_TEXT_CHARS)) {
		throw new HttpError(400, "invalid_request");
	}
	const images = body.images ?? [];
	if (!Array.isArray(images)) throw new HttpError(400, "invalid_request");
	return {
		task: body.task,
		locale: body.locale.slice(0, 35),
		text,
		images: images.map((image) => {
			if (!isRecord(image) || typeof image.mimeType !== "string" || typeof image.data !== "string") {
				throw new HttpError(400, "invalid_request");
			}
			return { mimeType: image.mimeType, data: image.data };
		}),
	};
}

export function checkImagesForTask(images: RunImage[], task: AiTask) {
	if (images.length < task.minImages || images.length > task.maxImages) throw new HttpError(400, "invalid_request");
	for (const image of images) {
		if (!ALLOWED_MIME_TYPES.has(image.mimeType) || !BASE64.test(image.data)) {
			throw new HttpError(400, "invalid_request");
		}
		if ((image.data.length * 3) / 4 > MAX_IMAGE_BYTES) throw new HttpError(413, "payload_too_large");
	}
}

export function parseReportRequest(body: unknown): ReportRequest {
	if (!isRecord(body) || typeof body.task !== "string" || typeof body.reason !== "string" || !("result" in body)) {
		throw new HttpError(400, "invalid_request");
	}
	if (JSON.stringify(body.result).length > MAX_REPORT_RESULT_CHARS) throw new HttpError(413, "payload_too_large");
	return { task: body.task.slice(0, 100), result: body.result, reason: body.reason.slice(0, MAX_REASON_CHARS) };
}

const validators = new Map<string, Validator>();

/** Gemini follows the schema in JSON mode, but the app must never receive a malformed result. */
export function matchesSchema(task: AiTask, value: unknown): boolean {
	let validator = validators.get(task.id);
	if (!validator) {
		validator = new Validator(task.responseSchema, "2020-12", false);
		validators.set(task.id, validator);
	}
	return validator.validate(value).valid;
}
