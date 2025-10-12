package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.university.UniversityName
import com.google.firebase.Timestamp

data class UserInfo(
    val name: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val universityName: UniversityName,
    val location: Location? = null,
    val residency: Residency? = null,
    val birthDate: Timestamp? = null
)
