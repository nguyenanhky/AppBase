package com.viettel.appbase.core.ai

/**
 * Makes sure a (possibly anonymous) session exists before calling the Worker, which needs an ID
 * token to count quota. Covers the case where the anonymous sign-in at startup failed offline.
 */
interface AiSessionGuard {
    suspend fun ensureSession()
}
