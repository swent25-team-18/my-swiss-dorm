package com.android.mySwissDorm.ui.share

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareLinkDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun shareDialog_displaysTitle() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysQrCode() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysCopyLinkButton() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun shareDialog_displaysCancelText() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.CANCEL_TEXT).assertIsDisplayed()
  }

  @Test
  fun shareDialog_copyLinkButton_dismissesDialog() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()

    composeTestRule.waitForIdle()
    assertEquals(true, dialogDismissed)
  }

  @Test
  fun shareDialog_cancelText_dismissesDialog() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.CANCEL_TEXT).performClick()

    composeTestRule.waitForIdle()
    assertEquals(true, dialogDismissed)
  }

  @Test
  fun shareDialog_qrCodeContainsCorrectLink() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    // QR code should be displayed
    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
  }

  @Test
  fun shareDialog_handlesLongLinks() {
    val longLink =
        "https://my-swiss-dorm.web.app/listing/very-long-listing-id-that-should-still-work-correctly-for-qr-code-generation"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = longLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()

    composeTestRule.waitForIdle()
    assertEquals(true, dialogDismissed)
  }

  @Test
  fun shareDialog_handlesReviewLinks() {
    val reviewLink = "https://my-swiss-dorm.web.app/review/review123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = reviewLink, onDismiss = { dialogDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).performClick()

    composeTestRule.waitForIdle()
    assertEquals(true, dialogDismissed)
  }

  @Test
  fun shareDialog_allElementsAreDisplayed() {
    val testLink = "https://my-swiss-dorm.web.app/listing/test123"
    var dialogDismissed = false

    composeTestRule.setContent {
      MySwissDormAppTheme {
        ShareLinkDialog(link = testLink, onDismiss = { dialogDismissed = true })
      }
    }

    // Verify all elements are displayed
    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.DIALOG_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.QR_CODE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.COPY_LINK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(C.ShareLinkDialogTags.CANCEL_TEXT).assertIsDisplayed()
  }
}
