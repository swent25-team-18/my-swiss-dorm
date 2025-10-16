package com.android.mySwissDorm.ui.authentification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.model.university.UniversityName
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.screen.SignUpScreen
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeJwtGenerator
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpScreenTest : FirestoreTest() {
  override fun createRepositories() {}

  @get:Rule val composeTestRule = createComposeRule()

  val fakeName = "John"
  val fakeName2 = "Alice"
  val badName = "QWERTZUIOPASDFGHJKLYXCVBNM"
  val fakeLastName = "Doe"
  val fakeLastName2 = "Capet"
  val badLastName = "QWERTZUIOPASDFGHJKLYXCVBNM"
  val fakePhoneNumber = "774321122"
  val fakePhoneNumber2 = "794321122"
  val badPhoneNumber1 = "12345678"
  val badPhoneNumber2 = "0123456789"

  @Before
  override fun setUp() {
    super.setUp()
    AuthRepositoryProvider.repository.signOut()
  }

  @Test
  fun allInitialComponentsAreDisplayed() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Check all basics elements are displayed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField { assertIsDisplayed() }
        signUpLastNameField { assertIsDisplayed() }
        signUpPhoneNumberField { assertIsDisplayed() }
        signUpResidencyField { assertIsDisplayed() }
        signUpUniversityField { assertIsDisplayed() }
        signUpButton { assertIsDisplayed() }
      }
    }
  }

  @Test
  fun canWriteInAllTextField() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Check that all fields can be modified") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField {
          assertIsDisplayed()
          performTextInput(fakeName)
          assertTextContains(fakeName)
        }
        signUpLastNameField {
          assertIsDisplayed()
          performTextInput(fakeLastName)
          assertTextContains(fakeLastName)
        }
        signUpPhoneNumberField {
          assertIsDisplayed()
          performTextInput(fakePhoneNumber)
          assertTextContains(fakePhoneNumber)
        }
      }
    }
    step("Check that all fields can be replaced") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField {
          performTextReplacement(fakeName2)
          assertTextContains(fakeName2)
        }
        signUpLastNameField {
          performTextReplacement(fakeLastName2)
          assertTextContains(fakeLastName2)
        }
        signUpPhoneNumberField {
          performTextReplacement(fakePhoneNumber2)
          assertTextContains(fakePhoneNumber2)
        }
      }
    }
    step("Check that all fields can be cleared") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField {
          performTextClearance()
          assertTrue(runCatching { assertTextContains(fakeName2) }.isFailure)
        }
        signUpLastNameField {
          performTextClearance()
          assertTrue(runCatching { assertTextContains(fakeLastName2) }.isFailure)
        }
        signUpPhoneNumberField {
          performTextClearance()
          assertTrue(runCatching { assertTextContains(fakePhoneNumber2) }.isFailure)
        }
      }
    }
  }

  @Test
  fun textFieldSupportTextAreCorrectlyDisplayed() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Fill each text field") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField {
          assertIsDisplayed()
          performTextInput(fakeName)
          assertTextContains(fakeName)
        }
        signUpLastNameField {
          assertIsDisplayed()
          performTextInput(fakeLastName)
          assertTextContains(fakeLastName)
        }
        signUpPhoneNumberField {
          assertIsDisplayed()
          performTextInput(fakePhoneNumber)
          assertTextContains(fakePhoneNumber)
        }
      }
    }
    step("Check no support text is displayed") {
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_NAME_HELP_TEXT, useUnmergedTree = true)
          .isNotDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT, useUnmergedTree = true)
          .isNotDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .isNotDisplayed()
    }
    step("Clear each field") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpNameField.performTextClearance()
        signUpLastNameField.performTextClearance()
        signUpPhoneNumberField.performTextClearance()
      }
    }

    composeTestRule.waitForIdle()

    step("Check that the support texts are displayed") {
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_NAME_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
    }

    step("Check support texts are displayed when fields are ill-formed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        assertIsDisplayed()
        signUpNameField {
          assertIsDisplayed()
          performTextInput(badName)
          assertTextContains(badName)
        }
        signUpLastNameField {
          assertIsDisplayed()
          performTextInput(badLastName)
          assertTextContains(badLastName)
        }
        signUpPhoneNumberField {
          assertIsDisplayed()
          performTextInput(badPhoneNumber1)
          assertTextContains(badPhoneNumber1)
        }
      }
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_NAME_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpPhoneNumberField {
          assertIsDisplayed()
          performTextReplacement(badPhoneNumber2)
          assertTextContains(badPhoneNumber2)
        }
      }
      composeTestRule.waitForIdle()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .isDisplayed()
    }
  }

  @Test
  fun dropDownMenuAreDisplayedCorrectly() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Residency Dropdown menu is correctly displayed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isDisplayed()
        signUpResidencyDropDownBox.performClick()
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isNotDisplayed()
      }
    }
    step("University Dropdown menu is correctly displayed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpUniversityField.assertIsDisplayed()
        signUpUniversityDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isDisplayed()
        signUpResidencyDropDownBox.performClick()
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isNotDisplayed()
      }
    }
    step("Residency Dropdown menu display correctly all residencies") {
      val residencies = ResidencyName.entries.toList()
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isDisplayed()
        residencies.forEach {
          composeTestRule
              .onNodeWithTag(C.Tag.residencyNameTestTag(it), useUnmergedTree = true)
              .isDisplayed()
        }
        signUpResidencyDropDownBox.performClick()
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isNotDisplayed()
      }
    }
    step("University Dropdown menu display correctly all universities") {
      val universities = UniversityName.entries.toList()
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpUniversityField.assertIsDisplayed()
        signUpUniversityDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isDisplayed()
        universities.forEach {
          composeTestRule
              .onNodeWithTag(C.Tag.universityNameTestTag(it), useUnmergedTree = true)
              .isDisplayed()
        }
      }
    }
  }

  @Test
  fun dropDownMenuActionsWorkWell() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Residency Dropdown entries are well displayed after click") {
      val residencies = ResidencyName.entries.toList()
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        val dropDownMenuNode =
            composeTestRule.onNodeWithTag(
                C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
        dropDownMenuNode.isDisplayed()
        residencies.forEach {
          val residencyNode =
              composeTestRule.onNodeWithTag(C.Tag.residencyNameTestTag(it), useUnmergedTree = true)
          residencyNode.isDisplayed()
          residencyNode.performScrollTo()
          residencyNode.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isNotDisplayed()
          residencyNode.isDisplayed()
          signUpResidencyDropDownBox.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isDisplayed()
        }
      }
    }
    step("University Dropdown entries are displayed after click") {
      val universities = UniversityName.entries.toList()
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpUniversityField.assertIsDisplayed()
        signUpUniversityDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        val dropDownMenuNode =
            composeTestRule.onNodeWithTag(
                C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
        dropDownMenuNode.isDisplayed()
        universities.forEach {
          val universityNode =
              composeTestRule.onNodeWithTag(C.Tag.universityNameTestTag(it), useUnmergedTree = true)
          universityNode.isDisplayed()
          universityNode.performScrollTo()
          universityNode.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isNotDisplayed()
          universityNode.isDisplayed()
          signUpUniversityDropDownBox.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isDisplayed()
        }
      }
    }
  }

  @Test
  fun buttonAndHelpTextBehaveCorrectly() = run {
    composeTestRule.setContent { SignUpScreen() }
    step("Check cannot be pressed if nothing is filled") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpButton.performScrollTo()
        signUpButton.assertIsNotEnabled()
        signUpHelpText.performScrollTo()
        signUpHelpText.assertIsDisplayed()
      }
    }
    step("Check cannot be pressed if one entry is ill-formed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpNameField.performTextInput(badName)
        signUpLastNameField.performTextInput(fakeLastName)
        signUpPhoneNumberField.performTextInput(fakePhoneNumber)
        signUpHelpText.performScrollTo()
        signUpButton.assertIsNotEnabled()
        signUpHelpText.assertIsDisplayed()

        signUpNameField.performScrollTo()
        signUpNameField.performTextReplacement(fakeName)
        signUpLastNameField.performTextReplacement(badLastName)
        signUpHelpText.performScrollTo()
        signUpButton.assertIsNotEnabled()
        signUpHelpText.assertIsDisplayed()

        signUpLastNameField.performTextReplacement(fakeLastName)
        signUpPhoneNumberField.performTextReplacement(badPhoneNumber1)
        signUpHelpText.performScrollTo()
        signUpButton.assertIsNotEnabled()
        signUpHelpText.assertIsDisplayed()
      }
    }
    step("Check can press if everything is good and help text is hidden") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpPhoneNumberField.performTextReplacement(fakePhoneNumber)
        signUpButton.performScrollTo()
        signUpButton.assertIsEnabled()
        signUpHelpText.assertIsNotDisplayed()
      }
    }
  }

  @Test
  fun canSignUpOnTheDatabase() = runTest {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("fakeIdToken", email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    val connected = mutableStateOf(false)
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = fakeCredentialManager, onSignedUp = { connected.value = true })
    }
    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpNameField.performTextInput(fakeName)
      signUpLastNameField.performTextInput(fakeLastName)
      signUpPhoneNumberField.performTextInput(fakePhoneNumber)
      signUpButton.performScrollTo()
      signUpButton.performClick()
    }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { connected.value }
    val userId = FirebaseEmulator.auth.uid ?: throw NoSuchElementException()
    val expectedProfile =
        Profile(
            ownerId = userId,
            userInfo =
                UserInfo(
                    name = fakeName,
                    lastName = fakeLastName,
                    phoneNumber = "+41$fakePhoneNumber",
                    email = FakeUser.FakeUser1.email),
            userSettings = UserSettings())
    assertEquals(expectedProfile, ProfileRepositoryProvider.repository.getProfile(userId))
  }

  @Test
  fun checkBackButtonWorkCorrectly() {
    val backed = mutableStateOf(false)
    composeTestRule.setContent { SignUpScreen(onBack = { backed.value = true }) }
    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpBackButton {
        assertIsDisplayed()
        performClick()
      }
      assertTrue(backed.value)
    }
  }

  @Test
  fun cannotSignUpTwoTimes() = runTest {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("fakeIdToken", email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    val connected = mutableStateOf(false)
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = fakeCredentialManager, onSignedUp = { connected.value = true })
    }
    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpNameField.performTextInput(fakeName)
      signUpLastNameField.performTextInput(fakeLastName)
      signUpPhoneNumberField.performTextInput(fakePhoneNumber)
      signUpButton.performScrollTo()
      signUpButton.performClick()
    }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { connected.value }
    connected.value = false

    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpNameField.performScrollTo()
      signUpNameField.performTextReplacement(fakeName2)
      signUpLastNameField.performTextReplacement(fakeLastName2)
      signUpPhoneNumberField.performTextReplacement(fakePhoneNumber2)
      signUpButton.performScrollTo()
      signUpButton.performClick()
    }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(5_000) { connected.value }
    val userId = FirebaseEmulator.auth.uid ?: throw NoSuchElementException()
    val expectedProfile =
        Profile(
            ownerId = userId,
            userInfo =
                UserInfo(
                    name = fakeName,
                    lastName = fakeLastName,
                    phoneNumber = "+41$fakePhoneNumber",
                    email = FakeUser.FakeUser1.email),
            userSettings = UserSettings())
    assertEquals(expectedProfile, ProfileRepositoryProvider.repository.getProfile(userId))
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
