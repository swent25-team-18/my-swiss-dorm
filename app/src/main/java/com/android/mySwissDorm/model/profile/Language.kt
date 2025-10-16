package com.android.mySwissDorm.model.profile

/** This enum class represents the supported languages in the application */
enum class Language(val displayLanguage: String) {
  ENGLISH("English"),
  FRENCH("Français"),
  ;

  override fun toString(): String {
    return displayLanguage
  }
}
