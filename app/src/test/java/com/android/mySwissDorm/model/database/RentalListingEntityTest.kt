package com.android.mySwissDorm.model.database

import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RentalStatus
import com.android.mySwissDorm.model.rental.RoomType
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class RentalListingEntityTest {
  @Test
  fun fromRentalListing_createsEntityCorrectly() {
    val listing = createTestRentalListing()
    val entity = RentalListingEntity.fromRentalListing(listing)

    assertEquals(listing.uid, entity.uid)
    assertEquals(listing.ownerId, entity.ownerId)
    assertEquals(listing.postedAt, entity.postedAt)
    assertEquals(listing.residencyName, entity.residencyName)
    assertEquals(listing.title, entity.title)
    assertEquals(listing.roomType.name, entity.roomType)
    assertEquals(listing.pricePerMonth, entity.pricePerMonth, 0.01)
    assertEquals(listing.areaInM2, entity.areaInM2)
    assertEquals(listing.startDate, entity.startDate)
    assertEquals(listing.description, entity.description)
    assertEquals(listing.imageUrls, entity.imageUrls)
    assertEquals(listing.status.name, entity.status)
    assertEquals(listing.location, entity.location)
  }

  @Test
  fun toRentalListing_createsListingCorrectly() {
    val entity = createTestRentalListingEntity()
    val listing = entity.toRentalListing()

    assertEquals(entity.uid, listing.uid)
    assertEquals(entity.ownerId, listing.ownerId)
    assertEquals(entity.postedAt, listing.postedAt)
    assertEquals(entity.residencyName, listing.residencyName)
    assertEquals(entity.title, listing.title)
    assertEquals(entity.roomType, listing.roomType.name)
    assertEquals(entity.pricePerMonth, listing.pricePerMonth, 0.01)
    assertEquals(entity.areaInM2, listing.areaInM2)
    assertEquals(entity.startDate, listing.startDate)
    assertEquals(entity.description, listing.description)
    assertEquals(entity.imageUrls, listing.imageUrls)
    assertEquals(entity.status, listing.status.name)
    assertEquals(entity.location, listing.location)
  }

  @Test
  fun roundTrip_preservesAllFields() {
    val originalListing = createTestRentalListing()
    val entity = RentalListingEntity.fromRentalListing(originalListing)
    val convertedListing = entity.toRentalListing()

    assertEquals(originalListing.uid, convertedListing.uid)
    assertEquals(originalListing.ownerId, convertedListing.ownerId)
    assertEquals(originalListing.postedAt.seconds, convertedListing.postedAt.seconds)
    assertEquals(originalListing.postedAt.nanoseconds, convertedListing.postedAt.nanoseconds)
    assertEquals(originalListing.residencyName, convertedListing.residencyName)
    assertEquals(originalListing.title, convertedListing.title)
    assertEquals(originalListing.roomType, convertedListing.roomType)
    assertEquals(originalListing.pricePerMonth, convertedListing.pricePerMonth, 0.01)
    assertEquals(originalListing.areaInM2, convertedListing.areaInM2)
    assertEquals(originalListing.startDate.seconds, convertedListing.startDate.seconds)
    assertEquals(originalListing.startDate.nanoseconds, convertedListing.startDate.nanoseconds)
    assertEquals(originalListing.description, convertedListing.description)
    assertEquals(originalListing.imageUrls, convertedListing.imageUrls)
    assertEquals(originalListing.status, convertedListing.status)
    assertEquals(originalListing.location.name, convertedListing.location.name)
    assertEquals(originalListing.location.latitude, convertedListing.location.latitude, 0.0001)
    assertEquals(originalListing.location.longitude, convertedListing.location.longitude, 0.0001)
  }

  @Test
  fun toRentalListing_handlesEmptyImageUrls() {
    val entity =
        RentalListingEntity(
            uid = "test-uid",
            ownerId = "owner-1",
            postedAt = Timestamp.now(),
            residencyName = "Residency",
            title = "Test",
            roomType = RoomType.STUDIO.name,
            pricePerMonth = 500.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "Description",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED.name,
            location = Location("Lausanne", 46.5197, 6.6323))

    val listing = entity.toRentalListing()

    assertTrue(listing.imageUrls.isEmpty())
  }

  @Test
  fun toRentalListing_handlesInvalidRoomType() {
    val entity =
        RentalListingEntity(
            uid = "test-uid",
            ownerId = "owner-1",
            postedAt = Timestamp.now(),
            residencyName = "Residency",
            title = "Test",
            roomType = "INVALID_TYPE",
            pricePerMonth = 500.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "Description",
            imageUrls = emptyList(),
            status = RentalStatus.POSTED.name,
            location = Location("Lausanne", 46.5197, 6.6323))

    val listing = entity.toRentalListing()

    assertEquals(RoomType.STUDIO, listing.roomType)
  }

  @Test
  fun toRentalListing_handlesInvalidStatus() {
    val entity =
        RentalListingEntity(
            uid = "test-uid",
            ownerId = "owner-1",
            postedAt = Timestamp.now(),
            residencyName = "Residency",
            title = "Test",
            roomType = RoomType.APARTMENT.name,
            pricePerMonth = 500.0,
            areaInM2 = 20,
            startDate = Timestamp.now(),
            description = "Description",
            imageUrls = emptyList(),
            status = "INVALID_STATUS",
            location = Location("Lausanne", 46.5197, 6.6323))

    val listing = entity.toRentalListing()

    assertEquals(RentalStatus.POSTED, listing.status)
  }

  @Test
  fun fromRentalListing_handlesAllRoomTypes() {
    RoomType.entries.forEach { roomType ->
      val listing = createTestRentalListing().copy(roomType = roomType)
      val entity = RentalListingEntity.fromRentalListing(listing)
      val converted = entity.toRentalListing()

      assertEquals(roomType, converted.roomType)
    }
  }

  @Test
  fun fromRentalListing_handlesAllStatuses() {
    RentalStatus.entries.forEach { status ->
      val listing = createTestRentalListing().copy(status = status)
      val entity = RentalListingEntity.fromRentalListing(listing)
      val converted = entity.toRentalListing()

      assertEquals(status, converted.status)
    }
  }

  private fun createTestRentalListing(): RentalListing {
    return RentalListing(
        uid = "listing-1",
        ownerId = "user-1",
        postedAt = Timestamp(1000, 500000),
        residencyName = "EPFL Residency",
        title = "Nice Studio",
        roomType = RoomType.STUDIO,
        pricePerMonth = 600.0,
        areaInM2 = 25,
        startDate = Timestamp(2000, 0),
        description = "A nice studio apartment",
        imageUrls = listOf("url1", "url2"),
        status = RentalStatus.POSTED,
        location = Location("Lausanne", 46.5197, 6.6323))
  }

  private fun createTestRentalListingEntity(): RentalListingEntity {
    return RentalListingEntity(
        uid = "listing-1",
        ownerId = "user-1",
        postedAt = Timestamp(1000, 500000),
        residencyName = "EPFL Residency",
        title = "Nice Studio",
        roomType = RoomType.STUDIO.name,
        pricePerMonth = 600.0,
        areaInM2 = 25,
        startDate = Timestamp(2000, 0),
        description = "A nice studio apartment",
        imageUrls = listOf("url1", "url2"),
        status = RentalStatus.POSTED.name,
        location = Location("Lausanne", 46.5197, 6.6323))
  }
}
