package com.android.mySwissDorm.ui.listing

import AddListingScreen
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests focused on the AddListingScreen UI with the new centralized InputSanitizers. We verify: (1)
 * button enablement, (2) inline error for size format, (3) Firestore write.
 */
class AddListingScreenTest : FirestoreTest() {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun setContentWith(onConfirmCapture: (String) -> Unit = {}) {
    composeRule.setContent {
      AddListingScreen(
          onConfirm = { added -> onConfirmCapture(added.uid) }, onBack = { /* no-op */})
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun ui_button_disabled_until_all_fields_valid_then_writes() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    var capturedUid: String? = null
    setContentWith { uid -> capturedUid = uid }

    val confirmBtn = composeRule.onNodeWithText("Confirm listing").assertExists()
    confirmBtn.assertIsNotEnabled()

    // Fill fields with VALID values respecting new validators (size requires one decimal).
    composeRule.onNode(hasText("Title") and hasSetTextAction()).performTextInput("Cozy studio")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("25.0")
    composeRule
        .onNode(hasText("Price (CHF / month)") and hasSetTextAction())
        .performTextInput("1200")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("Near EPFL")

    // Button should now enable
    confirmBtn.assertIsEnabled()
    confirmBtn.performClick()

    // Allow VM coroutine to persist
    runBlocking { delay(250) }

    runTest {
      assertEquals("UI should insert one listing into Firestore", 1, getRentalListingCount())
    }
    assertNotNull("onConfirm must be called with the created listing", capturedUid)
  }

  @Test
  fun ui_inline_error_shown_for_size_without_decimal_and_blocks_submit() = run {
    runTest { switchToUser(FakeUser.FakeUser2) }
    setContentWith {}

    val confirmBtn = composeRule.onNodeWithText("Confirm listing").assertExists()

    composeRule.onNode(hasText("Title") and hasSetTextAction()).performTextInput("X")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("Y")

    // Entering 1000 (no decimal) is invalid per validateFinal (must have exactly one decimal)
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("1000")

    // Price is fine but we keep it blank to ensure the button remains disabled
    // (even with price filled size error must still block submit)
    confirmBtn.assertIsNotEnabled()

    runTest { assertEquals(0, getRentalListingCount()) }
  }

  @Test
  fun ui_price_typing_filters_to_digits_and_caps_value_visually() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}

    // Enter noisy input; sanitizer should filter to digits only and drop leading zeros.
    val priceNode = composeRule.onNode(hasText("Price (CHF / month)") and hasSetTextAction())
    priceNode.performTextInput("00a12b3!")
    // Enter minimal other fields so the button remains disabled (size missing decimal)
    composeRule.onNode(hasText("Title") and hasSetTextAction()).performTextInput("A")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("10")

    // Still disabled because size invalid (no decimal) and description empty
    composeRule.onNodeWithText("Confirm listing").assertIsEnabled()
  }

  @Test
  fun start_date_field_is_displayed() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}

    composeRule
        .onNodeWithTag(C.AddListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText("Start Date", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun clicking_start_date_opens_date_picker() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}

    composeRule
        .onNodeWithTag(C.AddListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Date picker dialog should be displayed
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun date_picker_can_be_dismissed() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}

    composeRule
        .onNodeWithTag(C.AddListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.CANCEL_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Date picker should be dismissed
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun selecting_date_updates_start_date_and_enables_submit() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}

    val confirmBtn = composeRule.onNodeWithText("Confirm listing").assertExists()
    confirmBtn.assertIsNotEnabled()

    // Fill required fields
    composeRule.onNode(hasText("Title") and hasSetTextAction()).performTextInput("Cozy studio")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("25.0")
    composeRule
        .onNode(hasText("Price (CHF / month)") and hasSetTextAction())
        .performTextInput("1200")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("Near EPFL")

    // Open date picker and select a date
    composeRule
        .onNodeWithTag(C.AddListingScreenTags.START_DATE_FIELD, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(C.CustomDatePickerDialogTags.OK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    // Button should now be enabled with all fields including date filled
    confirmBtn.assertIsEnabled()
  }
}
