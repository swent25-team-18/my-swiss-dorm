package com.android.mySwissDorm.model.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamChatProviderTest {

  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun testInitialize() {
    try {
      StreamChatProvider.initialize(context)
    } catch (e: IllegalStateException) {
      assertFalse(StreamChatProvider.isInitialized())
      return
    }

    if (StreamChatProvider.isInitialized()) {
      assertNotNull(StreamChatProvider.getClient())
    }
  }

  @Test
  fun testGetClientThrowsIfNotInitialized() {
    if (!StreamChatProvider.isInitialized()) {
      var threw = false
      try {
        StreamChatProvider.getClient()
      } catch (e: IllegalStateException) {
        threw = true
      }
      assertTrue(threw)
    }
  }

  @Test
  fun testResetClient() {
    // Mock the client
    val mockClient = io.mockk.mockk<io.getstream.chat.android.client.ChatClient>()
    StreamChatProvider.setClient(mockClient)

    // Verify initialized
    assertTrue(StreamChatProvider.isInitialized())

    // Reset
    StreamChatProvider.resetClient()

    // Verify not initialized
    assertFalse(StreamChatProvider.isInitialized())
  }
}
