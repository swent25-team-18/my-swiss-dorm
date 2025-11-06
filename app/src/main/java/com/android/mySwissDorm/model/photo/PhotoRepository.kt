package com.android.mySwissDorm.model.photo

/** Represents a repository that manages photos */
interface PhotoRepository {
  /**
   * Retrieve a photo given the identifier
   *
   * @param uid the unique identifier of the photo to be retrieved
   * @return the corresponding [Photo]
   * @throws NoSuchElementException if the the uid does not correspond to any photos in this
   *   repository
   */
  suspend fun retrievePhoto(uid: String): Photo

  /**
   * Upload a photo in the repository. A successful upload should NOT delete the given [Photo]
   *
   * @param photo the photo to be uploaded
   * @throws java.io.FileNotFoundException if no data is associated to the [Photo]
   */
  suspend fun uploadPhoto(photo: Photo)
}
