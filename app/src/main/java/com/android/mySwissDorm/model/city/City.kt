package com.android.mySwissDorm.model.city

import com.android.mySwissDorm.model.map.Location

data class City(
    val name: String,
    val description: String,
    val location: Location,
    val imageId: Int
)
