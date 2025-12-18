package com.android.mySwissDorm.ui.authentification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.Credential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.authentification.AuthRepository
import com.android.mySwissDorm.model.authentification.AuthRepositoryFirebase
import com.android.mySwissDorm.model.authentification.AuthRepositoryProvider
import com.android.mySwissDorm.model.profile.Profile
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.model.profile.UserInfo
import com.android.mySwissDorm.model.profile.UserSettings
import com.android.mySwissDorm.screen.SignInScreen
import com.android.mySwissDorm.utils.FakeCredentialManager
import com.android.mySwissDorm.utils.FakeJwtGenerator
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import com.google.firebase.auth.FirebaseUser
import io.github.kakaocup.compose.node.element.ComposeScreen
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest : FirestoreTest() {

  override fun createRepositories() {
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  @Test
  fun testTagsCorrectlySets() = run {
    step("SignIn screen") {
      composeTestRule.setContent { SignInScreen() }
      ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
        assertIsDisplayed()
        appLogo { assertIsDisplayed() }
        logInButton { assertIsDisplayed() }
        signUpButton {
          performScrollTo()
          assertIsDisplayed()
        }
      }
    }
  }

  @Test
  fun canSignInWithGoogle() = runTest {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    val connected = mutableStateOf(false)
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(FirebaseEmulator.auth)
    composeTestRule.setContent {
      SignInScreen(
          credentialManager = fakeCredentialManager, onSignedIn = { connected.value = true })
    }
    switchToUser(FakeUser.FakeUser1)
    ProfileRepositoryProvider.repository.createProfile(
        Profile(
            ownerId = FirebaseEmulator.auth.currentUser?.uid ?: throw NoSuchElementException(),
            userInfo =
                UserInfo(
                    name = "John",
                    lastName = "Doe",
                    email = FirebaseEmulator.auth.currentUser?.email ?: "",
                    phoneNumber = ""),
            userSettings = UserSettings()))
    FirebaseEmulator.auth.signOut()
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      logInButton {
        assertIsDisplayed()
        performClick()
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(5000L) { connected.value }
  }

  @Test
  fun cannotSignInIfNotRegistered() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    val connected = mutableStateOf(false)
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(FirebaseEmulator.auth)
    composeTestRule.setContent {
      SignInScreen(
          credentialManager = fakeCredentialManager, onSignedIn = { connected.value = true })
    }
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      logInButton {
        assertIsDisplayed()
        performClick()
      }
      composeTestRule.waitForIdle()
      assertIsDisplayed()
    }
  }

  @Test
  fun credentialCancellationDoesNotThrow() {
    val fakeAuthRepository: AuthRepository =
        object : AuthRepository {
          override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
            throw GetCredentialCancellationException()
          }

          override suspend fun signInAnonymously(): Result<FirebaseUser> {
            throw GetCredentialInterruptedException()
          }

          override fun signOut(): Result<Unit> {
            return Result.success(Unit)
          }
        }

    AuthRepositoryProvider.repository = fakeAuthRepository

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      logInButton {
        assertIsDisplayed()
        assertTrue(runCatching { performClick() }.isSuccess)
      }
    }
  }

  @Test
  fun credentialExceptionDoesNotThrow() {
    val fakeAuthRepository: AuthRepository =
        object : AuthRepository {
          override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
            throw GetCredentialInterruptedException()
          }

          override suspend fun signInAnonymously(): Result<FirebaseUser> {
            throw GetCredentialInterruptedException()
          }

          override fun signOut(): Result<Unit> {
            return Result.success(Unit)
          }
        }

    AuthRepositoryProvider.repository = fakeAuthRepository

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      logInButton {
        assertIsDisplayed()
        assertTrue(runCatching { performClick() }.isSuccess)
      }
    }
  }

  @Test
  fun exceptionDoesNotThrow() {
    val fakeAuthRepository: AuthRepository =
        object : AuthRepository {
          override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
            throw Exception()
          }

          override suspend fun signInAnonymously(): Result<FirebaseUser> {
            throw GetCredentialInterruptedException()
          }

          override fun signOut(): Result<Unit> {
            return Result.success(Unit)
          }
        }

    AuthRepositoryProvider.repository = fakeAuthRepository

    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(
            FakeUser.FakeUser1.userName, email = FakeUser.FakeUser1.email)
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent { SignInScreen(credentialManager = fakeCredentialManager) }
    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      logInButton {
        assertIsDisplayed()
        assertTrue(runCatching { performClick() }.isSuccess)
      }
    }
  }

  @Test
  fun canSignInAnonymously() = runTest {
    val connected = mutableStateOf(false)
    AuthRepositoryProvider.repository = AuthRepositoryFirebase(FirebaseEmulator.auth)
    composeTestRule.setContent { SignInScreen(onSignedIn = { connected.value = true }) }

    ComposeScreen.onComposeScreen<SignInScreen>(composeTestRule) {
      assertIsDisplayed()
      guestButton {
        assertIsDisplayed()
        performClick()
      }
    }
    composeTestRule.waitUntil(5000L) { connected.value }
    val currentUser = FirebaseEmulator.auth.currentUser
    assertTrue("User should be signed in", currentUser != null)
    assertTrue("User should be anonymous", currentUser!!.isAnonymous)
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }
}
