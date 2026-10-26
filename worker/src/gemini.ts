import type { Config } from "./env";
import { HttpError } from "./errors";
import type { AiTask } from "./tasks/types";
import type { RunRequest } from "./validate";

const BLOCKED_REASONS = new Set([
	"SAFETY",
	"BLOCKLIST",
	"PROHIBITED_CONTENT",
	"SPII",
	"IMAGE_SAFETY",
	"IMAGE_PROHIBITED_CONTENT",
	"OTHER",
]);

interface GenerateContentResponse {
	promptFeedback?: { blockReason?: string };
	candidates?: { finishReason?: string; content?: { parts?: { text?: string }[] } }[];
}

/** Calls models.generateContent (JSON mode + JSON Schema). Throws HttpError on blocks and upstream failures. */
export async function generate(task: AiTask, request: RunRequest, config: Config): Promise<unknown> {
	const url = `${config.geminiBaseUrl}/v1beta/models/${task.model}:generateContent`;
	const parts: Record<string, unknown>[] = [
		{ text: `User locale: ${request.locale}` },
		...(request.text ? [{ text: request.text }] : []),
		...request.images.map((image) => ({ inlineData: { mimeType: image.mimeType, data: image.data } })),
	];
	const body = {
		systemInstruction: { parts: [{ text: task.systemPrompt }] },
		contents: [{ role: "user", parts }],
		generationConfig: {
			temperature: task.temperature,
			responseMimeType: "application/json",
			responseJsonSchema: task.responseSchema,
		},
	};

	let response: Response;
	try {
		response = await fetch(url, {
			method: "POST",
			headers: { "content-type": "application/json", "x-goog-api-key": config.geminiApiKey },
			body: JSON.stringify(body),
		});
	} catch (error) {
		console.error(JSON.stringify({ event: "gemini_unreachable", message: String(error) }));
		throw new HttpError(502, "upstream_error");
	}
	if (!response.ok) {
		// Includes "User location is not supported" (400 FAILED_PRECONDITION): see README.
		console.error(JSON.stringify({ event: "gemini_error", status: response.status, body: (await response.text()).slice(0, 500) }));
		throw new HttpError(502, "upstream_error");
	}

	const data = (await response.json()) as GenerateContentResponse;
	const candidate = data.candidates?.[0];
	if (data.promptFeedback?.blockReason || (candidate?.finishReason && BLOCKED_REASONS.has(candidate.finishReason))) {
		throw new HttpError(422, "content_blocked");
	}
	const text = candidate?.content?.parts?.map((part) => part.text ?? "").join("") ?? "";
	try {
		return JSON.parse(text);
	} catch {
		console.error(JSON.stringify({ event: "gemini_bad_json", finishReason: candidate?.finishReason }));
		throw new HttpError(502, "upstream_error");
	}
}
