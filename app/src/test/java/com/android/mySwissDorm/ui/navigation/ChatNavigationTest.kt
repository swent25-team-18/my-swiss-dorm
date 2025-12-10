package com.android.mySwissDorm.ui.navigation

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import java.net.URLEncoder
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatNavigationTest {

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun invokeOpenChatChannel(channelId: String, navActions: NavigationActions) {
    val method =
        Class.forName("com.android.mySwissDorm.ui.navigation.AppNavHostKt")
            .getDeclaredMethod("openChatChannel", String::class.java, NavigationActions::class.java)
    method.isAccessible = true
    method.invoke(null, channelId, navActions)
  }

  @Test
  fun openChatChannel_encodesAndNavigates() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val rawChannelId = "messaging:userA/userB:chat"
    val expectedEncodedId = URLEncoder.encode(rawChannelId, "UTF-8")

    invokeOpenChatChannel(rawChannelId, navActions)

    verify { navActions.navigateTo(Screen.ChatChannel(expectedEncodedId)) }
  }

  @Test
  fun openChatChannel_withSimpleId_navigatesDirectly() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val rawChannelId = "messaging-userA-userB"
    val expectedEncodedId = URLEncoder.encode(rawChannelId, "UTF-8")

    invokeOpenChatChannel(rawChannelId, navActions)

    verify { navActions.navigateTo(Screen.ChatChannel(expectedEncodedId)) }
  }
}
