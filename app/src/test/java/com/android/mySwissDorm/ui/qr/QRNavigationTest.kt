package com.android.mySwissDorm.ui.qr

import android.content.Context
import androidx.navigation.NavController
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
class QRNavigationTest {

  private lateinit var context: Context
  private lateinit var navController: NavController

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    navController = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun handleMySwissDormQr_withValidListingUrl_navigatesToListingOverview() {
    val url = "https://my-swiss-dorm.web.app/listing/test-listing-uid"

    handleMySwissDormQr(
        scannedText = url,
        navController = navController,
        context = context,
    )

    verify { navController.navigate("listingOverview/test-listing-uid") }
  }

  @Test
  fun handleMySwissDormQr_withValidReviewUrl_navigatesToReviewOverview() {
    val url = "https://my-swiss-dorm.web.app/review/test-review-uid"

    handleMySwissDormQr(
        scannedText = url,
        navController = navController,
        context = context,
    )

    verify { navController.navigate("reviewOverview/test-review-uid") }
  }

  @Test
  fun handleMySwissDormQr_withNonMySwissDormHost_doesNotNavigate() {
    val url = "https://example.com/listing/test-listing-uid"

    handleMySwissDormQr(
        scannedText = url,
        navController = navController,
        context = context,
    )
  }

  @Test
  fun handleMySwissDormQr_withTooFewPathSegments_doesNotNavigate() {
    val url = "https://my-swiss-dorm.web.app/listing"

    handleMySwissDormQr(
        scannedText = url,
        navController = navController,
        context = context,
    )
  }

  @Test
  fun handleMySwissDormQr_withUnknownType_doesNotNavigate() {
    val url = "https://my-swiss-dorm.web.app/unknownType/12345"

    handleMySwissDormQr(
        scannedText = url,
        navController = navController,
        context = context,
    )
  }
}
