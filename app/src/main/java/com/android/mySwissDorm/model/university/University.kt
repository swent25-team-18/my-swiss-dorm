package com.android.mySwissDorm.model.university

import com.android.mySwissDorm.model.map.Location
import java.net.URL

data class University(
    val name: UniversityName,
    val location: Location,
    val city: String,
    val email: String,
    val phone: String,
    val websiteURL: URL,
)

enum class UniversityName(val value: String) {
  EPFL("EPFL"),
  EHL("EHL"),
  ECAL("ECAL"),
  UNIL("UNIL"),
  UNIGE("UNIGE"),
  IHEID("IHEID"),
  IIG("IIG"),
  GBS("GBS"),
  ETHZ("ETHZ"),
  UZH("UZH"),
  ZHDK("ZHdK"),
  ZHAW("ZHAW"),
  UNIFR("UNIFR"),
  HESSOFR("HES-SO FR"),
  HEPPHFR("HEP PH FR"),
  HEIAFR("HEIA-FR"),
  ;

  override fun toString(): String {
    return value
  }
}
