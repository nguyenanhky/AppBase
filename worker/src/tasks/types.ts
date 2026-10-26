/**
 * An AI task the app can run by name. Prompts and schemas live here (server-side), so they can be
 * improved without an app release and are not exposed in the APK.
 */
export interface AiTask {
	id: string;
	/** Gemini model id; check https://ai.google.dev/gemini-api/docs/models before changing. */
	model: string;
	systemPrompt: string;
	/** JSON Schema of the result. Must match the app's @Serializable result class. */
	responseSchema: Record<string, unknown>;
	/** Quota units charged per run. */
	cost: number;
	minImages: number;
	maxImages: number;
	temperature: number;
}
