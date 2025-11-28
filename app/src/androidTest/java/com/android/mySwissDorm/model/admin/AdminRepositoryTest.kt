package com.android.mySwissDorm.model.admin

import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminRepositoryTest : FirestoreTest() {
  private lateinit var adminRepo: AdminRepository

  override fun createRepositories() {
    // No repository providers needed for this test
  }

  @Before
  override fun setUp() {
    super.setUp()
    adminRepo = AdminRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @Test
  fun isAdmin_whenAdminDoesNotExist_returnsFalse() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val result = adminRepo.isAdmin("nonexistent@example.com")
    assertFalse("Non-existent admin should return false", result)
  }

  @Test
  fun isAdmin_whenAdminExistsAndActive_returnsTrue() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "admin@example.com"

    // Add admin first
    adminRepo.addAdmin(email)

    // Check if admin exists
    val result = adminRepo.isAdmin(email)
    assertTrue("Existing active admin should return true", result)
  }

  @Test
  fun isAdmin_whenAdminExistsButInactive_returnsFalse() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "inactive@example.com"

    // Add admin with active = false
    FirebaseEmulator.firestore
        .collection("admins")
        .document(email.lowercase().trim())
        .set(mapOf("active" to false))
        .await()

    val result = adminRepo.isAdmin(email)
    assertFalse("Inactive admin should return false", result)
  }

  @Test
  fun isCurrentUserAdmin_whenUserIsAdmin_returnsTrue() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = FakeUser.FakeUser1.email

    // Add current user as admin
    adminRepo.addAdmin(email)

    val result = adminRepo.isCurrentUserAdmin()
    assertTrue("Current user should be admin", result)
  }

  @Test
  fun isCurrentUserAdmin_whenUserIsNotAdmin_returnsFalse() = runTest {
    switchToUser(FakeUser.FakeUser1)
    // Don't add as admin

    val result = adminRepo.isCurrentUserAdmin()
    assertFalse("Current user should not be admin", result)
  }

  @Test
  fun addAdmin_createsAdminDocumentWithActiveTrue() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "newadmin@example.com"

    adminRepo.addAdmin(email)

    // Verify admin was created
    val doc =
        FirebaseEmulator.firestore
            .collection("admins")
            .document(email.lowercase().trim())
            .get()
            .await()

    assertTrue("Admin document should exist", doc.exists())
    assertTrue("Admin should be active", doc.getBoolean("active") ?: false)
  }

  @Test
  fun addAdmin_normalizesEmailToLowerCase() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "ADMIN@EXAMPLE.COM"
    val normalizedEmail = email.lowercase().trim()

    adminRepo.addAdmin(email)

    // Check with lowercase
    val result = adminRepo.isAdmin(normalizedEmail)
    assertTrue("Admin should be found with lowercase email", result)

    // Check with original case
    val result2 = adminRepo.isAdmin(email)
    assertTrue("Admin should be found with original case email", result2)
  }

  @Test
  fun addAdmin_overwritesExistingAdmin() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "existing@example.com"

    // Add admin first
    adminRepo.addAdmin(email)

    // Add again (should overwrite)
    adminRepo.addAdmin(email)

    // Should still exist and be active
    val result = adminRepo.isAdmin(email)
    assertTrue("Admin should still exist after overwrite", result)
  }

  @Test
  fun isAdmin_checksDuplicateAdminCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    val email = "duplicate@example.com"

    // First check - should be false
    val beforeAdd = adminRepo.isAdmin(email)
    assertFalse("Admin should not exist before adding", beforeAdd)

    // Add admin
    adminRepo.addAdmin(email)

    // Second check - should be true
    val afterAdd = adminRepo.isAdmin(email)
    assertTrue("Admin should exist after adding", afterAdd)
  }
}
