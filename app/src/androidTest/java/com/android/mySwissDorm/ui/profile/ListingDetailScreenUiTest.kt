package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class ListingDetailScreenUiTest {

  @get:Rule val rule = createComposeRule()

  // ----- Helpers ------------------------------------------------------------

  /** Attend qu'au moins un nœud avec ce tag existe dans l'arbre NON fusionné. */
  private fun ComposeContentTestRule.waitForTag(tag: String, timeoutMs: Long = 5_000) {
    waitUntil(timeoutMs) {
      onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /** Monte l'écran et attend le premier bloc "identifiant" comme point de synchro. */
  private fun setUpContent(id: String = "l1") {
    rule.setContent { MySwissDormAppTheme { ListingDetailScreen(id = id, onBack = {}) } }
    // Point d'ancrage initial
    rule.waitForTag("field_identifiant")
  }

  // ----- Tests ultra-safe (wait avant CHAQUE assertion) ---------------------

  @Test
  fun showsIdentifiantBlock() {
    setUpContent()
    rule.waitForTag("field_identifiant")
    rule.onNodeWithTag("field_identifiant", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun showsIdentifiantValue() {
    setUpContent()
    rule.waitForTag("field_identifiant_value")
    rule
        .onNodeWithTag("field_identifiant_value", useUnmergedTree = true)
        .assertTextContains("Annonce #l1")
  }

  @Test
  fun showsEditButton() {
    setUpContent()
    rule.waitForTag("btn_edit")
    rule.onNodeWithTag("btn_edit", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun showsCloseButton() {
    setUpContent()
    rule.waitForTag("btn_close")
    rule.onNodeWithTag("btn_close", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun showsBackIcon() {
    setUpContent()
    rule.waitForTag("nav_back")
    rule.onNodeWithTag("nav_back", useUnmergedTree = true).assertIsDisplayed()
  }
}
