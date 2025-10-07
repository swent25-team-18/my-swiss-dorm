package com.android.mySwissDorm.model

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {

    suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

    fun signOut(): Result<Unit>
}