package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class ProfileContributionsFlowTest {

  @get:Rule val rule = createComposeRule()

  private fun setContentWithTestNavHost() {
    rule.setContent {
      MySwissDormAppTheme {
        val nav = rememberNavController()

        NavHost(navController = nav, startDestination = "profile") {
          composable("profile") {
            // two demo items so _0 => listing, _1 => request
            val items =
                listOf(
                    Contribution("Annonce l1", "Jolie chambre proche EPFL"),
                    Contribution("Demande r1", "Étudiant intéressé par une chambre"))
            ProfileContributionsScreen(
                contributions = items,
                onBackClick = {}, // not used in test
                onContributionClick = { c ->
                  if (c.title.startsWith("Demande", ignoreCase = true)) {
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

    rule.onNodeWithText("Mes contributions").assertIsDisplayed()
    rule.onNodeWithTag("btn_contrib_details_0").performClick()

    rule.onNodeWithTag("field_identifiant").assertIsDisplayed()
    rule.onNodeWithTag("nav_back").performClick()

    rule.onNodeWithText("Mes contributions").assertIsDisplayed()
  }

  @Test
  fun openRequestDetail_thenBack_toProfile() {
    setContentWithTestNavHost()

    rule.onNodeWithTag("btn_contrib_details_1").performClick()

    rule.onNodeWithTag("req_field_identifiant").assertIsDisplayed()
    rule.onNodeWithTag("nav_back").performClick()

    rule.onNodeWithText("Mes contributions").assertIsDisplayed()
  }
}
