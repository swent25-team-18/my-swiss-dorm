package com.android.mySwissDorm.ui.chat

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnsureConnectedToStreamTest {

  @Test
  fun returnsImmediately_whenOverrideIsProvided() = runTest {
    var connected = false
    var connecting = false
    var connectCalls = 0

    ensureConnectedToStream(
        isConnectedOverride = false,
        currentUserPresent = true,
        userStateProvider = { null },
        isConnecting = false,
        setIsConnected = { connected = it },
        setIsConnecting = { connecting = it },
        connectUser = { connectCalls += 1 },
        delayFn = {},
    )

    assertFalse(connected)
    assertFalse(connecting)
    assertEquals(0, connectCalls)
  }

  @Test
  fun setsConnected_whenUserAlreadyPresent() = runTest {
    var connected = false
    var connecting = false
    var connectCalls = 0

    ensureConnectedToStream(
        isConnectedOverride = null,
        currentUserPresent = false,
        userStateProvider = { Any() },
        isConnecting = false,
        setIsConnected = { connected = it },
        setIsConnecting = { connecting = it },
        connectUser = { connectCalls += 1 },
        delayFn = {},
    )

    assertTrue(connected)
    assertFalse(connecting)
    assertEquals(0, connectCalls)
  }

  @Test
  fun connectsAndSetsConnected_whenUserMissing_andCurrentUserPresent() = runTest {
    var connected = false
    val connectingEvents = mutableListOf<Boolean>()
    var connectCalls = 0
    val delays = mutableListOf<Long>()

    ensureConnectedToStream(
        isConnectedOverride = null,
        currentUserPresent = true,
        userStateProvider = { null },
        isConnecting = false,
        setIsConnected = { connected = it },
        setIsConnecting = { connectingEvents += it },
        connectUser = { connectCalls += 1 },
        delayFn = { ms -> delays += ms },
    )

    assertTrue(connected)
    assertEquals(listOf(true, false), connectingEvents)
    assertEquals(1, connectCalls)
    assertEquals(listOf(1500L), delays)
  }

  @Test
  fun onConnectFailure_setsConnectedBasedOnUserStateProvider_andResetsConnecting() = runTest {
    var connected = false
    val connectingEvents = mutableListOf<Boolean>()
    var connectCalls = 0
    val delays = mutableListOf<Long>()
    var userAfterFailure: Any? = null
    var logged = false

    ensureConnectedToStream(
        isConnectedOverride = null,
        currentUserPresent = true,
        userStateProvider = { userAfterFailure },
        isConnecting = false,
        setIsConnected = { connected = it },
        setIsConnecting = { connectingEvents += it },
        connectUser = {
          connectCalls += 1
          userAfterFailure = Any()
          throw RuntimeException("boom")
        },
        delayFn = { ms -> delays += ms },
        logError = { logged = true },
    )

    assertTrue("Should become connected because userAfterFailure is set", connected)
    assertEquals(listOf(true, false), connectingEvents)
    assertEquals(1, connectCalls)
    assertEquals(listOf(1000L), delays)
    assertTrue(logged)
  }
}
