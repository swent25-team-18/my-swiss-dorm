package com.android.mySwissDorm.model.review

import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp

data class Review(
    val uid: String, // Unique uid of the Review
    val ownerId: String, // Id of the owner (=the one who posts the review) of the review
    val postedAt: Timestamp, // Timestamp at which the Review has been posted
    val title: String, // Title of the review
    val reviewText: String, // Content of the review
    val grade: Double, // Grade of the room that is reviewed, between 1.0 and 5.0
    val residencyName: String, // Name of the residency of the room
    val roomType: RoomType, // Type of room in the review
    val pricePerMonth: Double, // Price per month of the room
    val areaInM2: Int, // Area of the room
    val imageUrls: List<String>, // List of images URL that can be added to the review
)
