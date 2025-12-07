package com.android.mySwissDorm.ui.residency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ResidencyImageProviderTest {

  @Test
  fun getDefaultImage_returnsImageForKnownResidency() {
    val image = ResidencyImageProvider.getDefaultImage("Vortex")
    assertNotNull("Vortex should have a default image", image)
    assertEquals(true, image?.startsWith("https://images.unsplash.com"))
  }

  @Test
  fun getDefaultImage_returnsNullForUnknownResidency() {
    val image = ResidencyImageProvider.getDefaultImage("Unknown Residency")
    assertNull("Unknown residency should not have a default image", image)
  }

  @Test
  fun getDefaultImages_returnsListForKnownResidency() {
    val images = ResidencyImageProvider.getDefaultImages("Vortex")
    assertEquals(1, images.size)
    assertEquals(true, images.first().startsWith("https://images.unsplash.com"))
  }

  @Test
  fun getDefaultImages_returnsEmptyListForUnknownResidency() {
    val images = ResidencyImageProvider.getDefaultImages("Unknown Residency")
    assertEquals(0, images.size)
  }

  @Test
  fun getDefaultImage_worksForMultipleResidencies() {
    val residencies = listOf("Vortex", "Atrium", "Cité St-Julien", "Salvatorhaus")
    residencies.forEach { residency ->
      val image = ResidencyImageProvider.getDefaultImage(residency)
      assertNotNull("$residency should have a default image", image)
    }
  }
}
