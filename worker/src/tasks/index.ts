import { identifyObject } from "./identify_object";
import type { AiTask } from "./types";

/** All tasks known to this code base; each deployment enables a subset with ENABLED_TASKS. */
const tasks: Record<string, AiTask> = {
	[identifyObject.id]: identifyObject,
};

export function findTask(id: string, enabled: Set<string>): AiTask | undefined {
	return enabled.has(id) ? tasks[id] : undefined;
}
