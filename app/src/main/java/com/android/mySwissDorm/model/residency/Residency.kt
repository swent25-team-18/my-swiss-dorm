package com.android.mySwissDorm.model.residency

import android.location.Location
import com.android.mySwissDorm.model.city.CitiesNames
import java.net.URL

data class Residency(
    val name: ResidencyNames,
    val description: String,
    val location: Location,
    val city: CitiesNames,
    val email: String,
    val phone: String,
    val website: URL,
)

enum class ResidencyNames(val value: String) {
  VORTEX("Vortex"),
  ATRIUM("Atrium"),
  CSJ("Cité St-Justin"),
  SALVATORHAUS("Salvatorhaus"),
  IHEID("IHEID"),
  CUG("Cité Universitaire de Genève"),
  WOKO("WOKO"),
  ETHZSH("ETH Zurich Student Housing");

  override fun toString(): String {
    return value
  }
}
