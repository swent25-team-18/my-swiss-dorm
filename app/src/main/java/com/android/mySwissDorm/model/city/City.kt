package com.android.mySwissDorm.model.city

import android.location.Location

data class City(
    val uid: String,
    val name: CitiesNames,
    val description: String,
    val location: Location?,
)

enum class CitiesNames(val value: String) {
  LAUSANNE("Lausanne"),
  GENEVA("Geneva"),
  ZÜRICH("Zürich"),
  FRIBOURG("Fribourg"),;

    override fun toString(): String{
        return value
    }
}
