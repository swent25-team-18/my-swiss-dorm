package com.android.mySwissDorm.model.profile

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RoomType

/** This data class represents the personal information related to the user. */
data class UserInfo(
    val name: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val universityName: String? = null,
    val location: Location? = null,
    val residencyName: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    val preferredRoomTypes: List<RoomType> = emptyList()
)
