package com.android.mySwissDorm.model.rentalListing

import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RentalListingRepositoryFirestoreAndroidTest : FirestoreTest() {
  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private val repo = RentalListingRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until 100).toSet<Int>().map { repo.getNewUid() }.toSet()
    TestCase.assertEquals(uids.size, numberIDs)
  }

  @Test
  fun canAddAndGetRentalListing() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    TestCase.assertEquals(1, getRentalListingCount())
    val rentalListings = repo.getAllRentalListings()
    assertEquals(1, rentalListings.size)
    val expectedRentalListing = rentalListing1.copy(uid = "none", ownerId = "none")
    val sortedRentalListing =
        rentalListings
            .first()
            .copy(uid = expectedRentalListing.uid, ownerId = expectedRentalListing.ownerId)
    assertEquals(sortedRentalListing, expectedRentalListing)
  }

  @Test
  fun addRentalListingWithTheCorrectID() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    assertEquals(1, getRentalListingCount())
    val storedRentalListing = repo.getRentalListing(rentalListing1.uid)
    assertEquals(storedRentalListing, rentalListing1)
  }

  @Test
  fun canAddMultipleRentalListings() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing3 =
        rentalListing3.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    repo.addRentalListing(rentalListing2)
    repo.addRentalListing(rentalListing3)
    assertEquals(3, getRentalListingCount())
    val rentalListings = repo.getAllRentalListings()
    assertEquals(3, rentalListings.size)
    val expectedRentalListings = setOf(rentalListing1, rentalListing2, rentalListing3)
    val sortedRentalListing = rentalListings.toSet()
    assertEquals(expectedRentalListings, sortedRentalListing)
  }

  @Test
  fun uidAreUnique() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    val duplicateUid = "dupUid"
    val rentalListing1Mod = rentalListing1.copy(uid = duplicateUid)
    val rentalListing2Mod = rentalListing2.copy(uid = duplicateUid)
    runCatching {
      repo.addRentalListing(rentalListing1Mod)
      repo.addRentalListing(rentalListing2Mod)
    }
    assertEquals(1, getRentalListingCount())
    val rentalListings = repo.getAllRentalListings()
    assertEquals(1, rentalListings.size)
    val sortedRentalListing = rentalListings.first()
    assertEquals(sortedRentalListing.uid, duplicateUid)
  }

  @Test
  fun canRetrieveARentalListingByID() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing3 =
        rentalListing3.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    repo.addRentalListing(rentalListing2)
    repo.addRentalListing(rentalListing3)
    assertEquals(3, getRentalListingCount())
    val storedRentalListing = repo.getRentalListing(rentalListing2.uid)
    assertEquals(storedRentalListing, rentalListing2)
  }

  @Test
  fun deleteRenalListingById() = runTest {
    switchToUser(FakeUser.FakeUser1)

    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing3 =
        rentalListing3.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    repo.addRentalListing(rentalListing2)
    repo.addRentalListing(rentalListing3)
    assertEquals(3, getRentalListingCount())
    repo.deleteRentalListing(rentalListing2.uid)
    assertEquals(2, getRentalListingCount())
    val rentalListings = repo.getAllRentalListings()
    assertEquals(rentalListings.size, 2)

    val expectedRentalListings = setOf(rentalListing1, rentalListing3)
    assertEquals(expectedRentalListings, rentalListings.toSet())
  }

  @Test
  fun canEditRentalListingById() = runTest {
    switchToUser(FakeUser.FakeUser1)
    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    val uid2 = repo.getNewUid()
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = uid2)
    repo.addRentalListing(rentalListing1)
    repo.addRentalListing(rentalListing2)
    assertEquals(2, getRentalListingCount())
    val rentalListings = repo.getAllRentalListings()
    assertEquals(2, rentalListings.size)

    val rentalListing2Edited =
        rentalListing2.copy(
            title = "Edited Title",
            description = "Edited Description",
            status = RentalStatus.ARCHIVED,
            residencyName = "Atrium")
    repo.editRentalListing(rentalListing2.uid, rentalListing2Edited)
    assertEquals(2, getRentalListingCount())
    val rentalListingsAfterEdit = repo.getAllRentalListings()
    assertEquals(2, rentalListingsAfterEdit.size)
    assertEquals(rentalListingsAfterEdit.toSet(), setOf(rentalListing1, rentalListing2Edited))
  }

  @Test
  fun canGetAllRentalListingsForUser() = runTest {
    switchToUser(FakeUser.FakeUser1)
    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    rentalListing2 =
        rentalListing2.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    repo.addRentalListing(rentalListing2)
    switchToUser(FakeUser.FakeUser2)
    val ownerIdNewUser =
        FirebaseEmulator.auth.currentUser?.uid ?: throw NullPointerException("No user logged in")
    val rentalListing4 = rentalListing3.copy(uid = repo.getNewUid(), ownerId = ownerIdNewUser)
    repo.addRentalListing(rentalListing4)
    assertEquals(1, repo.getAllRentalListingsByUser(ownerIdNewUser).size)
  }

  @Test
  fun getNonExistentRentalListingThrows() = runTest {
    switchToUser(FakeUser.FakeUser1)
    rentalListing1 =
        rentalListing1.copy(
            ownerId =
                FirebaseEmulator.auth.currentUser?.uid
                    ?: throw NullPointerException("No user logged in"),
            uid = repo.getNewUid())
    repo.addRentalListing(rentalListing1)
    assertEquals(1, getRentalListingCount())
    assertEquals(runCatching { repo.getRentalListing("NonExistentID") }.isFailure, true)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
