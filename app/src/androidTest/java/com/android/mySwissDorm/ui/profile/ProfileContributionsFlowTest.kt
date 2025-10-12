package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class ProfileContributionsFlowTest {

  @get:Rule val rule = createComposeRule()

  @OptIn(ExperimentalTestApi::class)
  private fun ComposeContentTestRule.waitForTag(tag: String, timeoutMs: Long = 5_000) {
    waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMs)
  }

  private fun setContentWithTestNavHost() {
    rule.setContent {
      MySwissDormAppTheme {
        val nav = rememberNavController()

        NavHost(navController = nav, startDestination = "profile") {
          composable("profile") {
            // Two demo items so _0 ⇒ listing, _1 ⇒ request
            val items =
                listOf(
                    Contribution("Listing l1", "Nice room near EPFL"),
                    Contribution("Request r1", "Student interested in a room"))
            ProfileContributionsScreen(
                contributions = items,
                onBackClick = {},
                onContributionClick = { c ->
                  // Route by English prefix; mirrors our demo data
                  if (c.title.startsWith("Request", ignoreCase = true)) {
                    nav.navigate("request/${c.title}")
                  } else {
                    nav.navigate("listing/${c.title}")
                  }
                })
          }
          composable("listing/{id}") { back ->
            val id = back.arguments?.getString("id") ?: "l1"
            ListingDetailScreen(id = id, onBack = { nav.popBackStack() })
          }
          composable("request/{id}") { back ->
            val id = back.arguments?.getString("id") ?: "r1"
            RequestDetailScreen(id = id, onBack = { nav.popBackStack() })
          }
        }
      }
    }
  }

  @Test
  fun openListingDetail_thenBack_toProfile() {
    setContentWithTestNavHost()

    rule.waitForTag("btn_contrib_details_0")
    rule.onNodeWithTag("btn_contrib_details_0").performClick()

    rule.waitForTag("field_identifiant")
    rule.onNodeWithTag("field_identifiant").assertIsDisplayed()

    rule.onNodeWithTag("nav_back").performClick()

    // Title is in English now
    rule.onNodeWithText("My contributions").assertIsDisplayed()
  }

  @Test
  fun openRequestDetail_thenBack_toProfile() {
    setContentWithTestNavHost()

    rule.waitForTag("btn_contrib_details_1")
    rule.onNodeWithTag("btn_contrib_details_1").performClick()

    rule.waitForTag("req_field_identifiant")
    rule.onNodeWithTag("req_field_identifiant").assertIsDisplayed()

    rule.onNodeWithTag("nav_back").performClick()

    rule.onNodeWithText("My contributions").assertIsDisplayed()
  }
}
