package com.android.mySwissDorm.model.profile

/** This data class represents the user settings in the app. */
data class UserSettings(
    val language: Language = Language.ENGLISH,
    val isPublic: Boolean = false,
    val isPushNotified: Boolean = true,
    val darkMode: Boolean? = null // null means follow system, true = dark mode, false = light mode
)
