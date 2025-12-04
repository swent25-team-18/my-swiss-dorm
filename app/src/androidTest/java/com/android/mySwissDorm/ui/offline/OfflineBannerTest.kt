package com.android.mySwissDorm.ui.offline

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.LastSyncTracker
import com.android.mySwissDorm.utils.NetworkUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineBannerTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Clear any existing sync data
    LastSyncTracker.clearLastSync(context)
  }

  @After
  fun tearDown() {
    unmockkAll()
    LastSyncTracker.clearLastSync(context)
  }

  @Test
  fun offlineBanner_notDisplayedWhenOnline() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns true

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertDoesNotExist()
  }

  @Test
  fun offlineBanner_displayedWhenOffline() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Wait a bit for LaunchedEffect to run
    runBlocking { delay(100) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertIsDisplayed()
  }

  @Test
  fun offlineBanner_showsNoSyncMessageWhenNoTimestamp() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    // Ensure no sync timestamp exists
    LastSyncTracker.clearLastSync(context)

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Wait a bit for LaunchedEffect to run
    runBlocking { delay(100) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_no_sync))
        .assertIsDisplayed()
  }

  @Test
  fun offlineBanner_showsLastUpdatedWhenTimestampExists() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    // Record a sync timestamp
    LastSyncTracker.recordSync(context)

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Wait a bit for LaunchedEffect to run
    runBlocking { delay(100) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertIsDisplayed()
    // Should show "Last updated" text (the exact relative time may vary)
    composeTestRule.onNodeWithText("Last updated", substring = true).assertIsDisplayed()
  }

  @Test
  fun offlineBanner_hidesWhenNetworkBecomesAvailable() {
    var isNetworkAvailable = false
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } answers { isNetworkAvailable }

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Initially offline - wait for banner to appear
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_you_are_offline))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertIsDisplayed()

    // Network becomes available
    isNetworkAvailable = true
    // Wait for polling interval and recomposition
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_you_are_offline))
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  @Test
  fun offlineBanner_showsWhenNetworkBecomesUnavailable() {
    var isNetworkAvailable = true
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } answers { isNetworkAvailable }

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Initially online - wait to ensure banner is not shown
    composeTestRule.waitUntil(3000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_you_are_offline))
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Network becomes unavailable
    isNetworkAvailable = false
    // Wait for polling interval and recomposition
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_you_are_offline))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_you_are_offline))
        .assertIsDisplayed()
  }

  @Test
  fun offlineBanner_updatesTimestampWhenSyncOccurs() {
    mockkObject(NetworkUtils)
    every { NetworkUtils.isNetworkAvailable(any()) } returns false

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBanner() } }

    // Initially no sync - wait for banner to appear with "no sync" message
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_no_sync))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onNodeWithText(context.getString(R.string.offline_banner_no_sync))
        .assertIsDisplayed()

    // Record a sync
    LastSyncTracker.recordSync(context)
    // Wait for polling interval and recomposition - "no sync" should disappear
    composeTestRule.waitUntil(5000) {
      composeTestRule
          .onAllNodesWithText(context.getString(R.string.offline_banner_no_sync))
          .fetchSemanticsNodes()
          .isEmpty()
    }
    // Should now show last updated
    composeTestRule.onNodeWithText("Last updated", substring = true).assertIsDisplayed()
  }
}
