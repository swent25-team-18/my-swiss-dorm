package com.android.mySwissDorm.ui.residency

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.profile.ProfileRepository
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.rental.RentalListingRepository
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryFirestore
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewResidencyScreenTest : FirestoreTest() {

  @get:Rule val compose = createComposeRule()

  private lateinit var residenciesRepo: ResidenciesRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var listingsRepo: RentalListingRepository

  private val context = ApplicationProvider.getApplicationContext<Context>()

  override fun createRepositories() {
    ResidenciesRepositoryProvider.repository =
        ResidenciesRepositoryFirestore(FirebaseEmulator.firestore)
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
    runBlocking { ResidenciesRepositoryProvider.repository.addResidency(resTest) }
  }

  @Before
  override fun setUp() {
    runTest {
      super.setUp()
      residenciesRepo = ResidenciesRepositoryProvider.repository
      profileRepo = ProfileRepositoryProvider.repository
      listingsRepo = RentalListingRepositoryProvider.repository
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private fun waitForScreenRoot() {
    compose.waitUntil(5_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun scrollListTo(childTag: String) {
    compose.onNodeWithTag(C.ViewResidencyTags.ROOT).performScrollToNode(hasTestTag(childTag))
  }

  @Test
  fun residencyDetails_displaysCorrectly() = runTest {
    compose.setContent { ViewResidencyScreen(residencyName = resTest.name) }
    waitForScreenRoot()

    compose.waitUntil(10_000) {
      compose
          .onAllNodesWithTag(C.ViewResidencyTags.NAME, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    compose.onNodeWithTag(C.ViewResidencyTags.NAME, useUnmergedTree = true).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewResidencyTags.DESCRIPTION, useUnmergedTree = true)
        .assertIsDisplayed()
    compose.onNodeWithTag(C.ViewResidencyTags.PHOTOS, useUnmergedTree = true).assertIsDisplayed()
    compose
        .onNodeWithTag(C.ViewResidencyTags.POI_DISTANCES, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun backButton_callsOnGoBack() = runTest {
    var backCalled = false

    compose.setContent {
      ViewResidencyScreen(residencyName = resTest.name, onGoBack = { backCalled = true })
    }
    waitForScreenRoot()

    compose
        .onNodeWithTag(C.ViewResidencyTags.BACK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    assertEquals(true, backCalled)
  }
}
