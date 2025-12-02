package com.android.mySwissDorm.ui.authentification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.map.LocationRepositoryProvider
import com.android.mySwissDorm.model.residency.ResidenciesRepositoryProvider
import com.android.mySwissDorm.model.university.UniversitiesRepositoryProvider
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.screen.SignUpScreen
import com.android.mySwissDorm.utils.FirestoreTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
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
  private lateinit var viewModel: SignUpViewModel
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
    viewModel =
        SignUpViewModel(AuthRepositoryProvider.repository, LocationRepositoryProvider.repository)
  }

  private fun setSignUpContent(onBack: () -> Unit = {}, onContinue: () -> Unit = {}) {
    composeTestRule.setContent {
      SignUpScreen(signUpViewModel = viewModel, onBack = onBack, onContinue = onContinue)
    }
  }

  @Test
  fun allInitialComponentsAreDisplayed() = run {
    setSignUpContent()
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
    setSignUpContent()
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
    setSignUpContent()
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
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT, useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .assertIsDisplayed()
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
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_LAST_NAME_HELP_TEXT, useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(C.Tag.SIGN_UP_PHONE_NUMBER_HELP_TEXT, useUnmergedTree = true)
          .assertIsDisplayed()
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
          .assertIsDisplayed()
    }
  }

  @Test
  fun dropDownMenuAreDisplayedCorrectly() = run {
    setSignUpContent()
    step("Residency Dropdown menu is correctly displayed") {
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
            .assertIsDisplayed()
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
            .assertIsDisplayed()
        signUpResidencyDropDownBox.performClick()
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isNotDisplayed()
      }
    }
    step("Residency Dropdown menu display correctly all residencies") {
      val residencies = runBlocking { ResidenciesRepositoryProvider.repository.getAllResidencies() }
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        residencies.forEach {
          composeTestRule
              .onNodeWithTag(C.Tag.residencyNameTestTag(it.name), useUnmergedTree = true)
              .assertIsDisplayed()
        }
        signUpResidencyDropDownBox.performClick()
        composeTestRule
            .onNodeWithTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
            .isNotDisplayed()
      }
    }
    step("University Dropdown menu display correctly all universities") {
      val universities = runBlocking {
        UniversitiesRepositoryProvider.repository.getAllUniversities()
      }
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpUniversityField.assertIsDisplayed()
        signUpUniversityDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        universities.forEach {
          composeTestRule
              .onNodeWithTag(C.Tag.universityNameTestTag(it.name), useUnmergedTree = true)
              .assertIsDisplayed()
        }
      }
    }
  }

  @Test
  fun dropDownMenuActionsWorkWell() = run {
    setSignUpContent()
    step("Residency Dropdown entries are well displayed after click") {
      val residencies = runBlocking { ResidenciesRepositoryProvider.repository.getAllResidencies() }
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpResidencyField.assertIsDisplayed()
        signUpResidencyDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        val dropDownMenuNode =
            composeTestRule.onNodeWithTag(
                C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_MENU, useUnmergedTree = true)
        dropDownMenuNode.assertIsDisplayed()
        residencies.forEach {
          val residencyNode =
              composeTestRule.onNodeWithTag(
                  C.Tag.residencyNameTestTag(it.name), useUnmergedTree = true)
          residencyNode.assertIsDisplayed()
          residencyNode.performScrollTo()
          residencyNode.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isNotDisplayed()
          residencyNode.assertIsDisplayed()
          signUpResidencyDropDownBox.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.assertIsDisplayed()
        }
      }
    }
    step("University Dropdown entries are displayed after click") {
      val universities = runBlocking {
        UniversitiesRepositoryProvider.repository.getAllUniversities()
      }
      ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
        signUpUniversityField.assertIsDisplayed()
        signUpUniversityDropDownBox {
          assertIsDisplayed()
          performClick()
        }
        val dropDownMenuNode =
            composeTestRule.onNodeWithTag(
                C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_MENU, useUnmergedTree = true)
        dropDownMenuNode.assertIsDisplayed()
        universities.forEach {
          val universityNode =
              composeTestRule.onNodeWithTag(
                  C.Tag.universityNameTestTag(it.name), useUnmergedTree = true)
          universityNode.assertIsDisplayed()
          universityNode.performScrollTo()
          universityNode.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.isNotDisplayed()
          universityNode.assertIsDisplayed()
          signUpUniversityDropDownBox.performClick()
          composeTestRule.waitForIdle()
          dropDownMenuNode.assertIsDisplayed()
        }
      }
    }
  }

  @Test
  fun buttonAndHelpTextBehaveCorrectly() = run {
    setSignUpContent()
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
  fun fillingFormRedirectsToNextScreen() = runTest {
    val onContinueCalled = mutableStateOf(false)
    setSignUpContent(onContinue = { onContinueCalled.value = true })

    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpNameField.performTextInput(fakeName)
      signUpLastNameField.performTextInput(fakeLastName)
      signUpPhoneNumberField.performTextInput(fakePhoneNumber)
      signUpButton.performScrollTo()
      signUpButton.performClick()
    }
    composeTestRule.waitForIdle()
    assertTrue("Expected onContinue to be called", onContinueCalled.value)
  }

  @Test
  fun checkBackButtonWorkCorrectly() {
    val backed = mutableStateOf(false)
    setSignUpContent(onBack = { backed.value = true })
    ComposeScreen.onComposeScreen<SignUpScreen>(composeTestRule) {
      signUpBackButton {
        assertIsDisplayed()
        performClick()
      }
      assertTrue(backed.value)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
