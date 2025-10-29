package com.android.mySwissDorm.model.residency

import com.android.mySwissDorm.model.map.Location
import java.net.URL

data class Residency(
    val name: String,
    val description: String,
    val location: Location,
    val city: String,
    val email: String?,
    val phone: String?,
    val website: URL?,
)
