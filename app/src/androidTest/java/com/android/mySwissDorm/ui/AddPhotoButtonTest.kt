package com.android.mySwissDorm.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mySwissDorm.resources.C
import org.junit.Rule
import org.junit.Test

class AddPhotoButtonTest {

  private val simpleTestTag = "simpleTestTag"

  @get:Rule val compose = createComposeRule()

  private fun setContentButton() {
    compose.setContent {
      AddPhotoButton({}) { Text("Add photo", modifier = Modifier.testTag(simpleTestTag)) }
    }
  }

  private fun setContentDialog() {
    compose.setContent { AddPhotoDialog({}, {}) }
  }

  @Test
  fun buttonIsDisplayed() {
    setContentButton()

    compose.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(simpleTestTag, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun dialogIsDisplayed() {
    setContentDialog()

    compose.onNodeWithTag(C.AddPhotoButtonTags.DIALOG).assertIsDisplayed()
    compose.onNodeWithTag(C.CameraButtonTag.TAG).assertIsDisplayed()
    compose.onNodeWithTag(C.GalleryButtonTag.TAG).assertIsDisplayed()
    compose
        .onNodeWithTag(C.AddPhotoButtonTags.CAMERA_BUTTON_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
    compose
        .onNodeWithTag(C.AddPhotoButtonTags.GALLERY_BUTTON_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun clickOnButtonDisplaysDialog() {
    setContentButton()

    compose.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(C.AddPhotoButtonTags.DIALOG).assertIsNotDisplayed()

    compose.onNodeWithTag(C.AddPhotoButtonTags.BUTTON).performClick()

    compose.waitForIdle()

    compose.onNodeWithTag(C.AddPhotoButtonTags.DIALOG).assertIsDisplayed()
  }
}
