package com.android.mySwissDorm.ui.chat

import com.android.mySwissDorm.model.profile.ProfileRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class ChatScreenDefaultsTest {

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun connectUserDefault_callsProfileAndStreamConnect() = runTest {
    val fakeUser = mockk<FirebaseUser>()
    every { fakeUser.uid } returns "uid-123"

    val fakeProfile = mockk<com.android.mySwissDorm.model.profile.Profile>()
    every { fakeProfile.userInfo.name } returns "John"
    every { fakeProfile.userInfo.lastName } returns "Doe"
    val fakeRepo = mockk<ProfileRepository>()
    coEvery { fakeRepo.getProfile("uid-123") } returns fakeProfile

    val connectorCalls = mutableListOf<Triple<String, String, String>>()

    connectUserWithRepo(
        firebaseUser = fakeUser,
        repo = fakeRepo,
        connector = { id, name, image -> connectorCalls.add(Triple(id, name, image)) })

    coVerify { fakeRepo.getProfile("uid-123") }
    assert(connectorCalls.size == 1)
    assert(connectorCalls.first() == Triple("uid-123", "John Doe", ""))
  }
}
