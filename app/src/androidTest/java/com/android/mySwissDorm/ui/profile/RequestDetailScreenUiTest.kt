package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class RequestDetailScreenUiTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun showsRequestInfoAndActions() {
    rule.setContent { MySwissDormAppTheme { RequestDetailScreen(id = "r1", onBack = {}) } }

    rule.onNodeWithTag("req_field_identifiant").assertIsDisplayed()
    rule.onNodeWithTag("req_field_identifiant_value").assertTextContains("Demande #r1")
    rule.onNodeWithTag("req_field_requester").assertIsDisplayed()
    rule.onNodeWithTag("req_field_message").assertIsDisplayed()
    rule.onNodeWithTag("btn_reject").assertIsDisplayed()
    rule.onNodeWithTag("btn_accept").assertIsDisplayed()
  }
}
