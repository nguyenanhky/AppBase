import { describe, expect, it } from "vitest";
import { quotaDay } from "../src/quota/day";

const VIETNAM = 420;

describe("quotaDay", () => {
	it("uses the Vietnam calendar day and resets at local midnight", () => {
		// 2026-10-26 23:59 in Vietnam.
		expect(quotaDay(Date.UTC(2026, 9, 26, 16, 59), VIETNAM)).toEqual({
			day: "2026-10-26",
			resetAt: "2026-10-26T17:00:00.000Z",
		});
	});

	it("rolls over exactly at 00:00 Vietnam time", () => {
		expect(quotaDay(Date.UTC(2026, 9, 26, 17, 0), VIETNAM).day).toBe("2026-10-27");
	});

	it("handles month boundaries", () => {
		expect(quotaDay(Date.UTC(2026, 9, 31, 18, 0), VIETNAM)).toEqual({
			day: "2026-11-01",
			resetAt: "2026-11-01T17:00:00.000Z",
		});
	});
});
