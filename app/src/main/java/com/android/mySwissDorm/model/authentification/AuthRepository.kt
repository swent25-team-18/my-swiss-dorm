package com.android.mySwissDorm.model.authentification

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

/** Handles authentification operations */
interface AuthRepository {
  /**
   * Signs in the user using a Google account through the Credential Manager API.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>
  /**
   * Signs in the user anonymously (Guest Mode).
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInAnonymously(): Result<FirebaseUser>

  /**
   * Signs out the currently authenticated user and clears the credential state.
   *
   * @return a [Result] indicating success or failure.
   */
  fun signOut(): Result<Unit>
}
