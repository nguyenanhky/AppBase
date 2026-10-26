package com.viettel.appbase.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.viettel.appbase.core.firebase.FirebaseProvider
import com.viettel.appbase.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAuthBackend(private val firebaseProvider: FirebaseProvider) : AuthBackend {
    override suspend fun signInAnonymously(): BackendAuthSession = firebaseCall {
        val auth = requireAuth()
        val user = auth.currentUser ?: requireNotNull(auth.signInAnonymously().await().user)
        user.toSession()
    }

    override suspend fun login(email: String, password: String): BackendAuthSession = firebaseCall {
        val user = requireNotNull(requireAuth().signInWithEmailAndPassword(email, password).await().user)
        user.toSession()
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): BackendAuthSession = firebaseCall {
        val auth = requireAuth()
        val anonymous = auth.currentUser?.takeIf { it.isAnonymous }
        // Linking keeps the anonymous uid, so server-side quota and local history stay with the user.
        val user = if (anonymous != null) {
            anonymous.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await().user
        } else {
            auth.createUserWithEmailAndPassword(email, password).await().user
        }
        requireNotNull(user).toSession(displayName)
    }

    // Firebase keeps its own refresh token, so the stored one is not needed.
    override suspend fun refresh(refreshToken: String): BackendAuthSession = firebaseCall {
        val user = requireAuth().currentUser ?: throw AppError.Unauthorized()
        user.toSession(forceRefresh = true)
    }

    override fun logout() {
        firebaseProvider.authOrNull()?.signOut()
    }

    private fun requireAuth(): FirebaseAuth = requireNotNull(firebaseProvider.authOrNull()) {
        "Firebase is not configured. Add app/google-services.json or use the debug demo account."
    }

    private suspend fun FirebaseUser.toSession(
        preferredDisplayName: String = "",
        forceRefresh: Boolean = false,
    ): BackendAuthSession = BackendAuthSession(
        userId = uid,
        email = email.orEmpty(),
        displayName = preferredDisplayName.ifBlank { displayName.orEmpty() },
        accessToken = requireNotNull(getIdToken(forceRefresh).await().token) { "Firebase returned an empty token." },
        isAnonymous = isAnonymous,
    )

    // Order matters: WeakPassword is a subclass of InvalidCredentials.
    private inline fun <T> firebaseCall(block: () -> T): T = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: FirebaseNetworkException) {
        throw AppError.Network(exception)
    } catch (exception: FirebaseAuthWeakPasswordException) {
        throw AppError.WeakPassword(exception)
    } catch (exception: FirebaseAuthInvalidCredentialsException) {
        throw AppError.InvalidCredentials(exception)
    } catch (exception: FirebaseAuthInvalidUserException) {
        throw AppError.InvalidCredentials(exception)
    } catch (exception: FirebaseAuthUserCollisionException) {
        throw AppError.EmailAlreadyInUse(exception)
    }
}
