import com.android.mySwissDorm.model.map.Location

/**
 * A data class that holds a list of locations along with the timestamp of when they were fetched.
 *
 * @property locations The list of cached locations.
 * @property fetchedTime The timestamp (in milliseconds) when the locations were fetched.
 */
data class CachedLocations(val locations: List<Location>, val fetchedTime: Long)

interface GeoCache {

  suspend fun get(key: String): CachedLocations?

  suspend fun put(key: String, value: CachedLocations)

  val timeToLive: Long
}
/**
 * An in-memory implementation of the GeoCache interface.
 *
 * This cache stores location data in memory with a specified time-to-live. The default time to live
 * is set to 30 days (in milliseconds).
 *
 * @property timeToLive The time-to-live for cached entries in milliseconds. Default is 30 days.
 */
class InMemoryGeoCache(
    override val timeToLive: Long = 30L * 24 * 60 * 60 * 1000 // 30 days in milliseconds
) : GeoCache {
  private val map = LinkedHashMap<String, CachedLocations>(128, 0.75f, true)

  override suspend fun get(key: String) = map[key]

  override suspend fun put(key: String, value: CachedLocations) {
    map[key] = value
  }
}
