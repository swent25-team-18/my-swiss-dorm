package com.android.mySwissDorm.ui.offline

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import com.android.mySwissDorm.utils.LastSyncTracker
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
    LastSyncTracker.clearLastSync(context)
  }

  @Test
  fun offlineBanner_notDisplayedWhenOnline() {
    // Pass isOffline = false directly. No mocks, no flows, no timeouts!
    composeTestRule.setContent { MySwissDormAppTheme { OfflineBannerContent(isOffline = false) } }

    composeTestRule.waitForIdle()

    // Banner should not be displayed when online
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.BANNER_ROOT).assertDoesNotExist()
  }

  @Test
  fun offlineBanner_displayedWhenOffline() {
    // Pass isOffline = true
    composeTestRule.setContent { MySwissDormAppTheme { OfflineBannerContent(isOffline = true) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(C.OfflineBannerTags.BANNER_ROOT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.OFFLINE_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun offlineBanner_showsNoSyncMessageWhenNoTimestamp() {
    // Ensure no sync timestamp exists
    LastSyncTracker.clearLastSync(context)

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBannerContent(isOffline = true) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(C.OfflineBannerTags.BANNER_ROOT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.OFFLINE_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.NO_SYNC_TEXT).assertIsDisplayed()
  }

  @Test
  fun offlineBanner_showsLastUpdatedWhenTimestampExists() {
    // Record a sync timestamp
    LastSyncTracker.recordSync(context)

    composeTestRule.setContent { MySwissDormAppTheme { OfflineBannerContent(isOffline = true) } }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(C.OfflineBannerTags.BANNER_ROOT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.OFFLINE_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.OfflineBannerTags.LAST_UPDATED_TEXT).assertIsDisplayed()
  }
}
