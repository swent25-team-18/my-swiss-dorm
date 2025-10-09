package com.android.mySwissDorm.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Rule
import org.junit.Test

class ListingDetailScreenUiTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun rendersBlocksAndButtons() {
        rule.setContent { MySwissDormAppTheme { ListingDetailScreen(id = "l1", onBack = {}) } }

        rule.onNodeWithTag("field_identifiant").assertIsDisplayed()
        rule.onNodeWithTag("field_identifiant_value").assertTextContains("Annonce #l1")
        rule.onNodeWithTag("btn_edit").assertIsDisplayed()
        rule.onNodeWithTag("btn_close").assertIsDisplayed()
        // Back arrow exists
        rule.onNodeWithTag("nav_back").assertIsDisplayed()
    }
}
