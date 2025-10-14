package com.android.mySwissDorm.ui.listing

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.profile.*
import com.android.mySwissDorm.model.rental.*
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ViewListingScreenFirestoreTest : FirestoreTest() {

    @get:Rule val compose = createComposeRule()

    // real repos (created in createRepositories)
    private lateinit var profileRepo: ProfileRepository
    private lateinit var listingsRepo: RentalListingRepository

    // test data
    private lateinit var ownerUid: String
    private lateinit var otherUid: String
    private lateinit var ownerListing: RentalListing
    private lateinit var otherListing: RentalListing

    override fun createRepositories() {
        profileRepo = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
        listingsRepo = RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    }

    @Before
    override fun setUp() {
        super.setUp()
        // Prepare two users in the emulator and capture their UIDs
        runTest {
            switchToUser(FakeUser.FakeUser1)
            ownerUid = FirebaseEmulator.auth.currentUser!!.uid
            profileRepo.createProfile(sampleProfile(ownerUid, "Mansour", "Kanaan"))

            switchToUser(FakeUser.FakeUser2)
            otherUid = FirebaseEmulator.auth.currentUser!!.uid
            profileRepo.createProfile(sampleProfile(otherUid, "Alice", "Queen"))

            // back to owner for the rest of set up
            switchToUser(FakeUser.FakeUser1)
        }

        // Write minimal but valid profiles with the real repo
        runTest {
        }

        // Create two listings via the real repo (one per user)
        ownerListing = sampleListing(ownerUid, "Coloc in Renens (Owner)")
        otherListing = sampleListing(otherUid, "Coloc in Renens (Non-owner)")

        runTest {
            listingsRepo.addRentalListing(ownerListing)
            listingsRepo.addRentalListing(otherListing)
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    // -------------------- TESTS --------------------

    @Test
    fun nonOwner_showsContactAndApply_enablesAfterTyping() = run {
        // Log in as owner but open OTHER user's listing -> non-owner branch
        runTest { switchToUser(FakeUser.FakeUser1) }

        compose.setContent {
            val vm = ViewListingViewModel(listingsRepo, profileRepo)
            ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
        }

        compose.onNodeWithText("Coloc in Renens (Non-owner)").assertIsDisplayed()
        compose.onNodeWithText("Contact the announcer").assertIsDisplayed()

        // Apply disabled until user types
        compose.onNodeWithText("Apply now !").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Contact the announcer").performTextInput("Hello! I'm interested.")
        compose.onNodeWithText("Apply now !").assertIsEnabled()
    }

    @Test
    fun owner_showsOnlyEdit() = run {
        // Log in as owner and open his own listing -> owner branch
        runTest { switchToUser(FakeUser.FakeUser1) }

        compose.setContent {
            val vm = ViewListingViewModel(listingsRepo, profileRepo)
            ViewListingScreen(viewListingViewModel = vm, listingUid = ownerListing.uid)
        }

        compose.onNodeWithText("Coloc in Renens (Owner)").assertIsDisplayed()
        compose.onNodeWithText("Edit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply now !").assertDoesNotExist()
        compose.onNodeWithText("Contact the announcer").assertDoesNotExist()
    }

    @Test
    fun canScrollToBottomButton() = run {
        runTest { switchToUser(FakeUser.FakeUser1) }

        compose.setContent {
            val vm = ViewListingViewModel(listingsRepo, profileRepo)
            ViewListingScreen(viewListingViewModel = vm, listingUid = otherListing.uid)
        }

        compose.onNodeWithText("Apply now !").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun repositoryError_callsOnGoBack() = run {
        var navigatedBack = false

        compose.setContent {
            val vm = ViewListingViewModel(listingsRepo, profileRepo)
            // Pass a non-existing uid so real repo throws
            ViewListingScreen(
                viewListingViewModel = vm,
                listingUid = "missing-" + UUID.randomUUID(),
                onGoBack = { navigatedBack = true }
            )
        }

        compose.waitUntil(4_000) { navigatedBack }
    }

    // -------------------- HELPERS --------------------

    private fun sampleProfile(uid: String, name: String, last: String): Profile =
        Profile(
            userInfo = UserInfo(
                name = name,
                lastName = last,
                email = "$name.$last@example.com",
                phoneNumber = "+41000000000",
                universityName = null,
                location = null,
                residency = null
            ),
            userSettings = UserSettings(),
            ownerId = uid
        )

    private fun sampleListing(owner: String, title: String): RentalListing {
        val loc = Location("test").apply { latitude = 46.5191; longitude = 6.5668 }
        return RentalListing(
            uid = UUID.randomUUID().toString(),
            ownerId = owner,
            postedAt = Timestamp.now(),
            residency = Residency(
                name = ResidencyName.PRIVATE,
                description = "",
                location = loc,
                city = CityName.LAUSANNE,
                email = null,
                phone = null,
                website = null
            ),
            title = title,
            roomType = RoomType.COLOCATION,
            pricePerMonth = 600.0,
            areaInM2 = 19,
            startDate = Timestamp.now(),
            description = "A place is freeing up in our super coloc.",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED
        )
    }
}
