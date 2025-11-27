package com.android.mySwissDorm.model.admin

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

  suspend fun isCurrentUserAdmin(): Boolean {
    // I added logs for debugging i will remove them later
    val email = auth.currentUser?.email ?: return false
    Log.d("AdminRepository", "Checking admin for email: $email")

    val doc = firestore.collection("admins").document(email).get().await()
    Log.d("AdminRepository", "Doc exists=${doc.exists()}, active=${doc.getBoolean("active")}")

    val active = doc.getBoolean("active") ?: false
    return doc.exists() && active
  }

  suspend fun isAdmin(email: String): Boolean {
    val normalizedEmail = email.lowercase().trim()
    val doc = firestore.collection("admins").document(normalizedEmail).get().await()
    val active = doc.getBoolean("active") ?: false
    return doc.exists() && active
  }

  suspend fun addAdmin(email: String) {
    val adminData = mapOf("active" to true)
    firestore.collection("admins").document(email.lowercase().trim()).set(adminData).await()
  }
}
