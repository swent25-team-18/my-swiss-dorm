package com.android.mySwissDorm.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HandleScannedQrUrlTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  private fun invokeHandleScannedQrUrl(scannedUrl: String, navigationActions: NavigationActions) {
    val method =
        Class.forName("com.android.mySwissDorm.ui.navigation.AppNavHostKt")
            .getDeclaredMethod(
                "handleScannedQrUrl",
                String::class.java,
                NavigationActions::class.java,
                Context::class.java)
    method.isAccessible = true
    method.invoke(null, scannedUrl, navigationActions, context)
  }

  @Test
  fun handleScannedQrUrl_withValidListingUrl_navigatesToListingOverview() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val url = "https://my-swiss-dorm.web.app/listing/test-listing-uid"

    invokeHandleScannedQrUrl(url, navActions)

    verify { navActions.navigateTo(Screen.ListingOverview("test-listing-uid")) }
  }

  @Test
  fun handleScannedQrUrl_withValidReviewUrl_navigatesToReviewOverview() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val url = "https://my-swiss-dorm.web.app/review/test-review-uid"

    invokeHandleScannedQrUrl(url, navActions)

    verify { navActions.navigateTo(Screen.ReviewOverview("test-review-uid")) }
  }

  @Test
  fun handleScannedQrUrl_withTooFewPathSegments_doesNotNavigate() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val url = "https://my-swiss-dorm.web.app/listing"

    invokeHandleScannedQrUrl(url, navActions)
  }

  @Test
  fun handleScannedQrUrl_withUnknownType_doesNotNavigate() {
    val navActions = mockk<NavigationActions>(relaxed = true)
    val url = "https://my-swiss-dorm.web.app/unknownType/12345"

    invokeHandleScannedQrUrl(url, navActions)
  }
}
