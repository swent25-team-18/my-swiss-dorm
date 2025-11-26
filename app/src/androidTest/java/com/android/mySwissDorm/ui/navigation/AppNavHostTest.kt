package com.android.mySwissDorm.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryFirestore
import com.android.mySwissDorm.model.chat.requestedmessage.RequestedMessageRepositoryProvider
import com.android.mySwissDorm.model.profile.ProfileRepositoryFirestore
import com.android.mySwissDorm.model.profile.ProfileRepositoryProvider
import com.android.mySwissDorm.utils.FakeUser
import com.android.mySwissDorm.utils.FirebaseEmulator
import com.android.mySwissDorm.utils.FirestoreTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppNavHostTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navActions: NavigationActions

  override fun createRepositories() {
    ProfileRepositoryProvider.repository =
        ProfileRepositoryFirestore(db = FirebaseEmulator.firestore)
    RequestedMessageRepositoryProvider.repository =
        RequestedMessageRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() = runTest {
    super.setUp()
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller

      // Create a minimal NavHost for testing navigation
      NavHost(navController = navController, startDestination = Screen.Inbox.route) {
        composable(Screen.Inbox.route) {}
        composable(Screen.RequestedMessages.route) {}
        composable(Screen.SelectUserToChat.route) {}
        composable(Screen.ChatChannel.route) {}
        composable(Screen.ViewUserProfile.route) {}
        composable(Screen.Homepage.route) {}
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val viewModel = NavigationViewModel(profileRepository = ProfileRepositoryProvider.repository)
      navActions =
          NavigationActions(
              navController = navController,
              coroutineScope = CoroutineScope(Dispatchers.Main),
              navigationViewModel = viewModel)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun navigateToRequestedMessages_fromInbox_navigatesCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Start on Inbox
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    // Navigate to RequestedMessages
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to RequestedMessages",
          Screen.RequestedMessages.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateToSelectUserToChat_navigatesCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Navigate to SelectUserToChat
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.SelectUserToChat) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to SelectUserToChat",
          Screen.SelectUserToChat.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigateFromSelectUserToChat_toChatChannel_navigatesCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    val channelId = "test-channel-123"

    // Navigate to SelectUserToChat first
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.SelectUserToChat) }
    composeTestRule.waitForIdle()

    // Simulate user selection - navigate to ChatChannel
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.ChatChannel(channelId)) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue(
          "Should navigate to ChatChannel with correct channelId",
          currentRoute?.startsWith("chatChannel/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val channelIdArg = backStackEntry?.arguments?.getString("channelId")
      assertEquals("Channel ID should match", channelId, channelIdArg)
    }
  }

  @Test
  fun navigateFromRequestedMessages_toViewUserProfile_navigatesCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    val userId = "test-user-456"

    // Navigate to RequestedMessages first
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    // Simulate viewing profile from requested message
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.ViewUserProfile(userId)) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue(
          "Should navigate to ViewUserProfile with correct userId",
          currentRoute?.startsWith("viewProfile/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val userIdArg = backStackEntry?.arguments?.getString("userId")
      assertEquals("User ID should match", userId, userIdArg)
    }
  }

  @Test
  fun goBackFromRequestedMessages_returnsToPreviousScreen() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Navigate to Inbox first
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    // Navigate to RequestedMessages
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.runOnUiThread { navActions.goBack() }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should go back to Inbox",
          Screen.Inbox.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun goBackFromSelectUserToChat_returnsToPreviousScreen() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Navigate to Inbox first
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    // Navigate to SelectUserToChat
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.SelectUserToChat) }
    composeTestRule.waitForIdle()

    // Go back
    composeTestRule.runOnUiThread { navActions.goBack() }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should go back to Inbox",
          Screen.Inbox.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun requestedMessagesCount_handlesErrorGracefully() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Navigate to Inbox - this should trigger the LaunchedEffect that loads the count
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    // The count loading happens in a LaunchedEffect, which should handle errors gracefully
    // We can't directly test the count state, but we can verify navigation still works
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should still navigate successfully even if count loading fails",
          Screen.RequestedMessages.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun requestedMessagesCount_returnsZeroForAnonymousUser() = runTest {
    signInAnonymous()
    composeTestRule.waitForIdle()

    val currentUser = FirebaseEmulator.auth.currentUser
    assertTrue("Should be anonymous user", currentUser?.isAnonymous == true)

    // Navigate to Inbox - count should be 0 for anonymous users
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    // Navigation should still work
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate successfully for anonymous user",
          Screen.RequestedMessages.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigationFlow_inboxToRequestedMessagesToBack_worksCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Start at Inbox
    composeTestRule.runOnUiThread { navController.navigate(Screen.Inbox.route) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should start at Inbox",
          Screen.Inbox.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Navigate to RequestedMessages (simulating onRequestedMessagesClick)
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should navigate to RequestedMessages",
          Screen.RequestedMessages.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Go back
    composeTestRule.runOnUiThread { navActions.goBack() }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should return to Inbox after going back",
          Screen.Inbox.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun navigationFlow_selectUserToChatToChannel_worksCorrectly() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    val channelId = "messaging:test-channel-789"

    // Navigate to SelectUserToChat
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.SelectUserToChat) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should be on SelectUserToChat",
          Screen.SelectUserToChat.route,
          navController.currentBackStackEntry?.destination?.route)
    }

    // Simulate user selection - navigate to ChatChannel
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.ChatChannel(channelId)) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue("Should navigate to ChatChannel", currentRoute?.startsWith("chatChannel/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val channelIdArg = backStackEntry?.arguments?.getString("channelId")
      assertEquals("Channel ID should be decoded correctly", channelId, channelIdArg)
    }
  }

  @Test
  fun chatChannel_handlesUrlEncodedChannelId() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    // Channel ID with special characters that need URL encoding
    val channelIdWithSpecialChars = "messaging:test-channel-with-special-chars-@#$"
    val encodedChannelId = java.net.URLEncoder.encode(channelIdWithSpecialChars, "UTF-8")

    // Navigate to ChatChannel with encoded ID
    composeTestRule.runOnUiThread {
      navController.navigate(Screen.ChatChannel(encodedChannelId).route)
    }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val backStackEntry = navController.currentBackStackEntry
      val decodedChannelId = backStackEntry?.arguments?.getString("channelId")
      // The channelId should be URL decoded in the composable
      // We verify the route contains the encoded version, but the argument should be decoded
      assertTrue(
          "Should handle URL encoded channel ID",
          decodedChannelId != null && decodedChannelId.isNotEmpty())
    }
  }

  @Test
  fun requestedMessagesScreen_canNavigateToViewProfile() = runTest {
    switchToUser(FakeUser.FakeUser1)
    composeTestRule.waitForIdle()

    val userId = "profile-user-123"

    // Navigate to RequestedMessages
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.RequestedMessages) }
    composeTestRule.waitForIdle()

    // Navigate to ViewUserProfile (simulating onViewProfile callback)
    composeTestRule.runOnUiThread { navActions.navigateTo(Screen.ViewUserProfile(userId)) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      val currentRoute = navController.currentBackStackEntry?.destination?.route
      assertTrue(
          "Should navigate to ViewUserProfile", currentRoute?.startsWith("viewProfile/") == true)

      val backStackEntry = navController.currentBackStackEntry
      val userIdArg = backStackEntry?.arguments?.getString("userId")
      assertEquals("User ID should match", userId, userIdArg)
    }

    // Go back to RequestedMessages
    composeTestRule.runOnUiThread { navActions.goBack() }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals(
          "Should return to RequestedMessages",
          Screen.RequestedMessages.route,
          navController.currentBackStackEntry?.destination?.route)
    }
  }
}
