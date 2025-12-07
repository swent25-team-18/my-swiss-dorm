package com.android.mySwissDorm.ui.residency

/**
 * Provides default images for residencies when no listing images are available. Uses publicly
 * available images from Unsplash and other sources.
 */
object ResidencyImageProvider {
  /**
   * Maps residency names to default image URLs. These are publicly available images that can be
   * used as placeholders.
   */
  private val residencyImageMap =
      mapOf(
          // Lausanne residencies
          "Vortex" to
              "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80&auto=format&fit=crop",
          "Atrium" to
              "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80&auto=format&fit=crop",
          "Cité St-Julien" to
              "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80&auto=format&fit=crop",
          // Fribourg residencies
          "Salvatorhaus" to
              "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80&auto=format&fit=crop",
          "Les Estudiantines" to
              "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80",
          "Les Cèdres" to "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
          "Les Vergers" to
              "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
          "Les Acacias" to
              "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80",
          "Les Tilleuls" to
              "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80",
          "Les Sapins" to "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
          "Les Chênes" to "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
          "Les Platanes" to
              "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80",
          "Les Erables" to
              "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80",
          "Les Bouleaux" to
              "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
          "Les Hêtres" to "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
          "Les Frênes" to "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80",
          "Les Ormes" to "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=800&q=80",
          "Les Peupliers" to
              "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
          "Les Mélèzes" to
              "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
          "Les Pins" to "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&q=80")

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
