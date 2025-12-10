package com.android.mySwissDorm.ui.chat

import io.getstream.chat.android.models.Channel
import io.getstream.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Unit test to cover the default fetchChannels code path inside ChannelsScreen's helper
 * (defaultFetchChannelsForTest) without using MockK proxies that are flaky in CI.
 */
class ChannelsScreenDefaultFetchTest {

  @Test
  fun defaultFetchChannels_returnsResultWithoutMocks() = runBlocking {
    val channel = Channel(type = "messaging", id = "cid")

    val result =
        defaultFetchChannelsForTest(currentUserId = "uid") { Result.Success(listOf(channel)) }

    assert(result == listOf(channel))
  }
}
