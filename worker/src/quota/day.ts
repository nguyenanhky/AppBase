const MINUTE_MS = 60_000;

/**
 * The quota "day" in the configured timezone (offset from UTC, e.g. 420 for Vietnam), and when it
 * ends, as an ISO-8601 UTC instant the app can show.
 */
export function quotaDay(nowMs: number, utcOffsetMinutes: number): { day: string; resetAt: string } {
	const local = new Date(nowMs + utcOffsetMinutes * MINUTE_MS);
	const day = local.toISOString().slice(0, 10);
	const nextLocalMidnight = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() + 1);
	return { day, resetAt: new Date(nextLocalMidnight - utcOffsetMinutes * MINUTE_MS).toISOString() };
}
