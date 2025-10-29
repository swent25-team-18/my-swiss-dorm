package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp

data class Review(
    val uid: String,
    val ownerId: String,
    val postedAt: Timestamp,
    val title: String,
    val reviewText: String,
    val grade: Double,
    val residencyName: String,
    val roomType: RoomType,
    val pricePerMonth: Double,
    val areaInM2: Int,
    val imageUrls: List<String>,
)
