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

// ...imports unchanged...
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
            // keep your demo list
            val items =
                listOf(
                    Contribution("Listing l1", "Nice room near EPFL"),
                    Contribution("Request r1", "Student interested in a room"))
            ProfileContributionsScreen(
                contributions = items,
                onBackClick = {},
                onContributionClick = { c ->
                  if (c.title.startsWith("Request", ignoreCase = true)) {
                    nav.navigate("request/${c.title.removePrefix("Request ").trim()}")
                  } else {
                    nav.navigate("listing/${c.title.removePrefix("Listing ").trim()}")
                  }
                })
          }
          composable("listing/{id}") { /* not asserted here */}
          composable("request/{id}") { back ->
            val id = back.arguments?.getString("id") ?: "r1"
            // after aligning API, call the screen directly (like your teammate does)
            RequestDetailScreen(id = id, onBack = { nav.popBackStack() })
          }
        }
      }
    }
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
