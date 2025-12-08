package com.android.mySwissDorm.ui.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.resources.C
import org.junit.Rule
import org.junit.Test

class TranslatableTextTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun everythingIsDisplayed() {
    compose.setContent { TranslatableText("Text") }

    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATABLE_TEXT).assertIsDisplayed()
    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun translateButtonChangeOnClick() {
    compose.setContent { TranslatableText("C'est un chien") }

    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATABLE_TEXT).assertIsDisplayed()
    compose
        .onNodeWithTag(C.TranslatableTextTestTags.TRANSLATABLE_TEXT)
        .assertTextEquals("C'est un chien")

    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON).assertTextEquals("Translate")
    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON).performClick()

    compose.waitUntil(20_000) { compose.onNodeWithText("It's a dog").isDisplayed() }

    compose
        .onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON)
        .assertTextEquals("See original")
    compose
        .onNodeWithTag(C.TranslatableTextTestTags.TRANSLATABLE_TEXT)
        .assertTextEquals("It's a dog")
  }

  @Test
  fun translateButtonIsNotDisplayedWithBlankText() {
    compose.setContent { TranslatableText("") }

    compose.onNodeWithTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON).assertIsNotDisplayed()
  }
}
