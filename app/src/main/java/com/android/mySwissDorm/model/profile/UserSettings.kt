package com.android.mySwissDorm.model.profile

data class UserSettings(
    val language: Language = Language.ENGLISH,
    val isPublic: Boolean = false,
    val isAnonymous: Boolean = true,
    val isPushNotified: Boolean = true
)
