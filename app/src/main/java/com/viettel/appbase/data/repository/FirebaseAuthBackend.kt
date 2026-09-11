package com.viettel.appbase.data.repository

import kotlinx.coroutines.tasks.await

class FirebaseAuthBackend(private val firebaseProvider: FirebaseProvider) : AuthBackend {
    override suspend fun login(email: String, password: String): BackendAuthSession {
        val auth = requireNotNull(firebaseProvider.authOrNull()) {
            "Firebase is not configured. Add app/google-services.json or use the debug demo account."
        }
        val user = requireNotNull(auth.signInWithEmailAndPassword(email, password).await().user)
        return user.toSession()
    }

    override suspend fun register(email: String, password: String, displayName: String): BackendAuthSession {
        val auth = requireNotNull(firebaseProvider.authOrNull()) {
            "Firebase is not configured. Add app/google-services.json or use the debug demo account."
        }
        val user = requireNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)
        return user.toSession(displayName)
    }

    override suspend fun refresh(): BackendAuthSession {
        val auth = requireNotNull(firebaseProvider.authOrNull()) { "Firebase is not configured." }
        val user = requireNotNull(auth.currentUser) { "No active session." }
        return user.toSession(forceRefresh = true)
    }

    override fun logout() {
        firebaseProvider.authOrNull()?.signOut()
    }

    private suspend fun com.google.firebase.auth.FirebaseUser.toSession(
        preferredDisplayName: String = "",
        forceRefresh: Boolean = false,
    ): BackendAuthSession = BackendAuthSession(
        userId = uid,
        email = email.orEmpty(),
        displayName = preferredDisplayName.ifBlank { displayName.orEmpty() },
        accessToken = requireNotNull(getIdToken(forceRefresh).await().token) { "Firebase returned an empty token." },
    )
}
