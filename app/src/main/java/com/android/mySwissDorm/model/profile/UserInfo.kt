package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.model.university.UniversityName

/** This data class represents the personal information related to the user. */
data class UserInfo(
    val name: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val universityName: UniversityName? = null,
    val location: Location? = null,
    val residencyName: ResidencyName? = null,
)
