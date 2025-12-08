package com.android.mySwissDorm.ui.residency

/**
 * Provides default images for residencies when no listing images are available. Uses publicly
 * available images from Unsplash and other sources.
 */
object ResidencyImageProvider {
  // Unsplash image URL constants to avoid duplication
  private const val UNSPLASH_IMAGE_1 =
      "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80"
  private const val UNSPLASH_IMAGE_2 =
      "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80"
  private const val UNSPLASH_IMAGE_3 =
      "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80"
  private const val UNSPLASH_IMAGE_4 =
      "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80"
  private const val UNSPLASH_IMAGE_1_WITH_CROP = "$UNSPLASH_IMAGE_1&auto=format&fit=crop"
  private const val UNSPLASH_IMAGE_2_WITH_CROP = "$UNSPLASH_IMAGE_2&auto=format&fit=crop"
  private const val UNSPLASH_IMAGE_3_WITH_CROP = "$UNSPLASH_IMAGE_3&auto=format&fit=crop"
  private const val UNSPLASH_IMAGE_4_WITH_CROP = "$UNSPLASH_IMAGE_4&auto=format&fit=crop"

  /**
   * Maps residency names to default image URLs. These are publicly available images that can be
   * used as placeholders.
   */
  private val residencyImageMap =
      mapOf(
          // Lausanne residencies
          "Vortex" to UNSPLASH_IMAGE_1_WITH_CROP,
          "Atrium" to UNSPLASH_IMAGE_2_WITH_CROP,
          "Cité St-Julien" to UNSPLASH_IMAGE_3_WITH_CROP,
          // Fribourg residencies
          "Salvatorhaus" to UNSPLASH_IMAGE_4_WITH_CROP,
          "Les Estudiantines" to UNSPLASH_IMAGE_3,
          "Les Cèdres" to UNSPLASH_IMAGE_4,
          "Les Vergers" to UNSPLASH_IMAGE_2,
          "Les Acacias" to UNSPLASH_IMAGE_1,
          "Les Tilleuls" to UNSPLASH_IMAGE_3,
          "Les Sapins" to UNSPLASH_IMAGE_4,
          "Les Chênes" to UNSPLASH_IMAGE_2,
          "Les Platanes" to UNSPLASH_IMAGE_1,
          "Les Erables" to UNSPLASH_IMAGE_3,
          "Les Bouleaux" to UNSPLASH_IMAGE_4,
          "Les Hêtres" to UNSPLASH_IMAGE_2,
          "Les Frênes" to UNSPLASH_IMAGE_1,
          "Les Ormes" to UNSPLASH_IMAGE_3,
          "Les Peupliers" to UNSPLASH_IMAGE_4,
          "Les Mélèzes" to UNSPLASH_IMAGE_2,
          "Les Pins" to UNSPLASH_IMAGE_1)

  /**
   * Gets a default image URL for a residency.
   *
   * @param residencyName The name of the residency.
   * @return The image URL if available, null otherwise.
   */
  fun getDefaultImage(residencyName: String): String? {
    return residencyImageMap[residencyName]
  }

  /**
   * Gets all available default images for a residency. Returns a list with the default image if
   * available.
   *
   * @param residencyName The name of the residency.
   * @return A list containing the default image URL if available, empty list otherwise.
   */
  fun getDefaultImages(residencyName: String): List<String> {
    return getDefaultImage(residencyName)?.let { listOf(it) } ?: emptyList()
  }
}
