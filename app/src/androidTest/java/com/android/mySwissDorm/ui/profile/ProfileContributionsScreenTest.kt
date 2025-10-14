package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileContributionsScreenTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun rendersItems_andCallsOnContributionClick_viaButton() {
    val items =
        listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room"))
    var clickedTitle: String? = null

    rule.setContent {
      MySwissDormAppTheme {
        ProfileContributionsScreen(
            contributions = items,
            onBackClick = {},
            onContributionClick = { clickedTitle = it.title })
      }
    }

    // Top bar
    rule.onNodeWithText("My contributions").assertIsDisplayed()
    rule.onNodeWithContentDescription("Back").assertIsDisplayed()

    // Item texts
    rule.onNodeWithText("Listing l1").assertIsDisplayed()
    rule.onNodeWithText("Nice room near EPFL").assertIsDisplayed()
    rule.onNodeWithText("Request r1").assertIsDisplayed()
    rule.onNodeWithText("Student interested in a room").assertIsDisplayed()

    // Click second item's "View details" button
    rule.onNodeWithTag("btn_contrib_details_1").assertIsDisplayed().performClick()
    assertEquals("Request r1", clickedTitle)
  }

  @Test
  fun cardClick_triggersCallback_forFirstItem() {
    val items =
        listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room"))
    var clickedTitle: String? = null

    rule.setContent {
      MySwissDormAppTheme {
        ProfileContributionsScreen(
            contributions = items,
            onBackClick = {},
            onContributionClick = { clickedTitle = it.title })
      }
    }

    // Click the CARD itself for the first item (find a clickable node that contains the title)
    rule
        .onNode(
            hasClickAction() and androidx.compose.ui.test.hasText("Listing l1"),
            useUnmergedTree = true)
        .performClick()

    assertEquals("Listing l1", clickedTitle)
  }

  @Test
  fun backButton_invokesCallback() {
    var backCalled = false
    rule.setContent {
      MySwissDormAppTheme {
        ProfileContributionsScreen(
            contributions = emptyList(),
            onBackClick = { backCalled = true },
            onContributionClick = {})
      }
    }
    rule.onNodeWithContentDescription("Back").performClick()
    assertEquals(true, backCalled)
  }
}
