package com.android.mySwissDorm.model.university

import com.android.mySwissDorm.model.map.Location
import java.net.URL

data class University(
    val name: String,
    val location: Location,
    val city: String,
    val email: String,
    val phone: String,
    val websiteURL: URL,
)
