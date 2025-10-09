package com.android.mySwissDorm.model.university

import android.location.Location
import com.android.mySwissDorm.model.city.CitiesNames
import java.net.URL

data class University(
    val name: String,
    val location: Location,
    val city: CitiesNames,
    val email: String,
    val phone: String,
    val websiteURL: URL,
)

enum class UniversityNames(val value: String) {
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
