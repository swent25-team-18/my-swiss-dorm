package com.android.mySwissDorm.model.profile

/** This data class represents the user settings in the app. */
data class UserSettings(
    val language: Language = Language.ENGLISH,
    val isPublic: Boolean = false,
    val isAnonymous: Boolean = true,
    val isPushNotified: Boolean = true
)
