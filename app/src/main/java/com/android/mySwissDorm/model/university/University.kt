package com.android.mySwissDorm.model.university

import android.location.Location
import com.android.mySwissDorm.model.city.CityName
import java.net.URL

data class University(
    val name: UniversityName,
    val location: Location,
    val city: CityName,
    val email: String,
    val phone: String,
    val websiteURL: URL,
)

enum class UniversityName(val value: String) {
  EPFL("EPFL"),
  UNIL("UNIL"),
  UNIGE("UNIGE"),
  ETHZ("ETHZ"),
  UZH("UZH"),
  UNIFR("UNIFR"),
  ;

  override fun toString(): String {
    return value
  }
}
