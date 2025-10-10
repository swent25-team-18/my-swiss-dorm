package com.android.mySwissDorm.ui.authentification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.authentification.AuthRepositoryFirebase
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.screen.SignInScreen
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeJwtGenerator
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest : TestCase() {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeIdToken = "123"
  private val fakeEmailToken = "john.doe@bob.com"

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running for these tests" }
  }

  @Before
  fun setup() {
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(auth = FirebaseEmulator.auth)
  }

  @Test
  fun testTagsCorrectlySets() = run {
    step("SignIn screen") {
      composeTestRule.setContent { SignInScreen() }
      ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
        assertIsDisplayed()
        app_logo { assertIsDisplayed() }
        log_in_button { assertIsDisplayed() }
        sign_up_button { assertIsDisplayed() }
      }
    }
  }

  @Test
  fun canSignInWithGoogle() = run {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(fakeIdToken, email = fakeEmailToken)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    val connected = mutableStateOf(false)
    step("Click on Log In") {
      composeTestRule.setContent {
        SignInScreen(
            credentialManager = fakeCredentialManager, onSignedIn = { connected.value = true })
      }

      ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
        assertIsDisplayed()
        log_in_button {
          assertIsDisplayed()
          performClick()
        }
      }

      composeTestRule.waitForIdle()

      composeTestRule.waitUntil(5000L) { connected.value }
    }
  }

  @After
  fun tearDown() {
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
