package com.android.mySwissDorm.listing

import AddListingScreen
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.mySwissDorm.model.rental.RentalListingRepositoryFirestore
import com.android.mySwissDorm.model.rental.RentalListingRepositoryProvider
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

class AddListingScreenTest : FirestoreTest() {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  override fun createRepositories() {
    RentalListingRepositoryProvider.repository =
        RentalListingRepositoryFirestore(FirebaseEmulator.firestore)
  }

  private fun setContentWith(onConfirmCapture: (String) -> Unit) {
    composeRule.setContent {
      AddListingScreen(
          onConfirm = { added -> onConfirmCapture(added.uid) },
          onOpenMap = {},
          onBack = { /* not implemented yet */})
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
  fun ui_ButtonDisabled_whenFormInvalid_andEnablesWhenValid_thenWritesToFirestore() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    var capturedUid: String? = null
    setContentWith { uid -> capturedUid = uid }
    val confirmBtn = composeRule.onNodeWithText("Confirm listing").assertExists()
    confirmBtn.assertIsNotEnabled()
    composeRule
        .onNode(hasText("Listing title") and hasSetTextAction())
        .performTextInput("Cozy studio")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("25")
    composeRule
        .onNode(hasText("Monthly rent (CHF)") and hasSetTextAction())
        .performTextInput("1200")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("Near EPFL")
    confirmBtn.assertIsEnabled()
    confirmBtn.performClick()
    runBlocking { delay(200) }
    runTest {
      assertEquals("UI should insert one listing into Firestore", 1, getRentalListingCount())
    }
    assertNotNull(capturedUid, "onConfirm must be called with the created listing")
  }

  @Test
  fun ui_ShowsInlineErrors_forInvalidPriceAndSize_andPreventsSubmit() = run {
    runTest { switchToUser(FakeUser.FakeUser2) }

    setContentWith { /* ignore */}

    val confirmBtn = composeRule.onNodeWithText("Confirm listing").assertExists()

    composeRule.onNode(hasText("Listing title") and hasSetTextAction()).performTextInput("X")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("Y")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("1000")
    composeRule
        .onNode(hasText("Monthly rent (CHF)") and hasSetTextAction())
        .performTextInput("10000")
    confirmBtn.assertIsNotEnabled()
    composeRule.onNodeWithText("Please enter a valid number under 1000.").assertExists()
    composeRule.onNodeWithText("Please enter a valid number under 10000.").assertExists()
    runTest { assertEquals(0, getRentalListingCount()) }
  }

  @Test
  fun ui_TypingFilter_allowsOnlyDigits_forPrice_andDecimalForSize() = run {
    runTest { switchToUser(FakeUser.FakeUser1) }
    setContentWith {}
    val priceNode = composeRule.onNode(hasText("Monthly rent (CHF)") and hasSetTextAction())
    priceNode.performTextInput("12a3!")
    composeRule.onNode(hasText("Listing title") and hasSetTextAction()).performTextInput("A")
    composeRule.onNode(hasText("Room size (m²)") and hasSetTextAction()).performTextInput("10")
    composeRule.onNode(hasText("Description") and hasSetTextAction()).performTextInput("B")

    composeRule.onNodeWithText("Confirm listing").assertIsNotEnabled()
  }
}
