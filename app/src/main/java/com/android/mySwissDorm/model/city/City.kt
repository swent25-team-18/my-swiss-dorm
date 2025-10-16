package com.android.mySwissDorm.model.city

import com.android.mySwissDorm.model.map.Location

data class City(
    val name: CityName,
    val description: String,
    val location: Location,
    val imageId: Int
)

enum class CityName(val value: String) {
  FRIBOURG("Fribourg"),
  GENEVA("Geneva"),
  LAUSANNE("Lausanne"),
  ZURICH("Zürich"),
  ;

  override fun toString(): String {
    return value
  }
}
