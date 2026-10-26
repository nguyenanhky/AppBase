package com.viettel.appbase.domain.model

/**
 * Users get an anonymous session automatically and may upgrade it to an account later.
 * [SignedOut] only lasts until a new anonymous session is created (e.g. offline at first launch).
 */
enum class AuthState { SignedOut, Anonymous, SignedIn }
