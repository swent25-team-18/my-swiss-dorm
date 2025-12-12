package com.android.mySwissDorm.ui.chat

import com.android.mySwissDorm.model.profile.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class ConnectUserByIdTest {

  @After fun tearDown() = unmockkAll()

  @Test
  fun connectUserById_callsProfileAndConnector() = runTest {
    val fakeUserInfo =
        mockk<com.android.mySwissDorm.model.profile.UserInfo> {
          every { name } returns "John"
          every { lastName } returns "Doe"
        }
    val fakeProfile =
        mockk<com.android.mySwissDorm.model.profile.Profile> {
          every { userInfo } returns fakeUserInfo
        }

    val fakeRepo = mockk<ProfileRepository>()
    coEvery { fakeRepo.getProfile("uid-123") } returns fakeProfile

    val connector = mockk<suspend (String, String, String) -> Unit>(relaxed = true)

    connectUserById("uid-123", repository = fakeRepo, connector = connector)

    coVerify { fakeRepo.getProfile("uid-123") }
    coVerify { connector.invoke("uid-123", "John Doe", "") }
  }
}
