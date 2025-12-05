package com.android.mySwissDorm.model.profile

/** This enum class represents the supported languages in the application */
enum class Language(val displayLanguage: String, val codeLanguage: String) {
  ENGLISH("English", "en"),
  FRENCH("Français", "fr"),
  ;

  override fun toString(): String {
    return displayLanguage
  }
}
