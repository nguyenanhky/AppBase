import type { AiTask } from "./types";

/** Demo task used by the app's "AI scan" tab (DemoScanResult). Copy it to create app-specific tasks. */
export const identifyObject: AiTask = {
	id: "identify_object",
	model: "gemini-3.5-flash-lite",
	cost: 1,
	minImages: 1,
	maxImages: 1,
	temperature: 0.2,
	systemPrompt: [
		"You identify the main insect, plant or animal in a user's photo.",
		"Rules:",
		"- If the photo does not clearly show an insect, plant or animal (for example an object, a screen, a person, or a blurry/dark photo), return recognized=false and no other claims.",
		"- Never invent a species. If unsure of the species, give the most specific group you are confident about (genus or family) and lower the confidence.",
		"- confidence is your honest probability (0..1) that the name is correct.",
		"- summary: 1-2 short sentences: what it is and whether it is harmful or dangerous to people, pets or crops.",
		"- advice: at most 3 short, practical, safe tips. Do not give pesticide or medicine dosages; for venomous or dangerous species, tell the user to keep away and contact an expert.",
		"- Write name, summary and advice in the language given by the user's locale. scientificName stays in Latin.",
	].join("\n"),
	responseSchema: {
		type: "object",
		properties: {
			recognized: { type: "boolean" },
			name: { type: "string" },
			scientificName: { type: "string" },
			confidence: { type: "number", minimum: 0, maximum: 1 },
			summary: { type: "string" },
			advice: { type: "array", items: { type: "string" }, maxItems: 3 },
		},
		required: ["recognized"],
		additionalProperties: false,
	},
};
