package com.android.mySwissDorm.model.residency

import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.map.Location
import java.net.URL

data class Residency(
    val name: ResidencyName,
    val description: String,
    val location: Location,
    val city: CityName,
    val email: String?,
    val phone: String?,
    val website: URL?,
)

enum class ResidencyName(val value: String) {
  VORTEX("Vortex"),
  ATRIUM("Atrium"),
  CSJ("Cité St-Justin"),
  SALVATORHAUS("Salvatorhaus"),
  IHEID("IHEID"),
  CUG("Cité Universitaire de Genève"),
  WOKO("WOKO"),
  ETHZSH("ETH Zurich Student Housing"),
  PRIVATE("Private Accommodation"),
  ;

  override fun toString(): String {
    return value
  }
}
