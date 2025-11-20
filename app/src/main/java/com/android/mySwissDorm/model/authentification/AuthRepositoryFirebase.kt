package com.android.mySwissDorm.model.authentification

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/**
 * A Firebase implementation of [AuthRepository]
 *
 * Retrieves a Google ID token via Credential Manager and authenticates the user with Firebase.
 *
 * @param auth the [FirebaseAuth] instance for Firebase authentification
 */
class AuthRepositoryFirebase(private val auth: FirebaseAuth = Firebase.auth) : AuthRepository {
  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
        val user =
            auth.signInWithCredential(firebaseCred).await().user
                ?: return Result.failure(
                    IllegalStateException("Login failed: Could not retrieve user information"))
        return Result.success(user)
      } else {
        return Result.failure(
            IllegalStateException("Login failed: Credential is not of type Google ID"))
      }
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Login failed ${e.localizedMessage ?: "Unexpected error."}"))
    }
  }

  override suspend fun signInAnonymously(): Result<FirebaseUser> {
    val currentUser = auth.currentUser
    if (currentUser != null && currentUser.isAnonymous) {
      return Result.success(currentUser)
    }
    // will probably never need this  (just here for extra security)
    if (currentUser != null) {
      try {
        auth.signOut()
      } catch (e: Exception) {}
    }

    return try {
      val authResult = auth.signInAnonymously().await()
      val user =
          authResult.user
              ?: return Result.failure(
                  IllegalStateException("Guest login failed: Could not sign-in anonymously"))
      Result.success(user)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Guest login failed: ${e.localizedMessage ?: "Unknown error"}"))
    }
  }

  override fun signOut(): Result<Unit> {
    return try {
      auth.signOut()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Logout failed: ${e.localizedMessage ?: "Unexpected error."}"))
    }
  }
}
