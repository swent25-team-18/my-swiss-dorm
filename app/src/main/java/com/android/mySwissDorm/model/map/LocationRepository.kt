package com.android.mySwissDorm.model.map

interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
