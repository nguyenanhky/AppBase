import { DurableObject } from "cloudflare:workers";

export interface QuotaResult {
	allowed: boolean;
	used: number;
}

/**
 * One instance per user (idFromName(uid)). Durable Objects process requests one at a time, so
 * concurrent scans cannot overshoot the limit. Each bucket keeps only the current day's counter.
 */
export class QuotaCounter extends DurableObject {
	async consume(bucket: string, day: string, cost: number, limit: number): Promise<QuotaResult> {
		const key = `${bucket}:${day}`;
		const used = (await this.ctx.storage.get<number>(key)) ?? 0;
		if (used + cost > limit) return { allowed: false, used };
		await this.ctx.storage.put(key, used + cost);
		await this.pruneOtherDays(bucket, key);
		return { allowed: true, used: used + cost };
	}

	/** Gives back units when the upstream call failed, so users are not charged for our errors. */
	async refund(bucket: string, day: string, cost: number): Promise<number> {
		const key = `${bucket}:${day}`;
		const used = Math.max(0, ((await this.ctx.storage.get<number>(key)) ?? 0) - cost);
		await this.ctx.storage.put(key, used);
		return used;
	}

	private async pruneOtherDays(bucket: string, currentKey: string) {
		const keys = await this.ctx.storage.list({ prefix: `${bucket}:` });
		const stale = [...keys.keys()].filter((key) => key !== currentKey);
		if (stale.length > 0) await this.ctx.storage.delete(stale);
	}
}
