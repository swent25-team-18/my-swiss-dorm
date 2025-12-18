package com.android.mySwissDorm.model.photo

/**
 * This represents a partial implementation of a cloud [PhotoRepository]. This kind of repositories
 * should first use the local repository before any cloud accesses.
 *
 * @param localRepository is the local [PhotoRepository]
 */
abstract class PhotoRepositoryCloud(
    private val localRepository: PhotoRepository = PhotoRepositoryProvider.localRepository
) : PhotoRepository {
  override suspend fun uploadPhoto(photo: Photo) {
    localRepository.uploadPhoto(photo)
  }

  override suspend fun retrievePhoto(uid: String): Photo {
    return localRepository.retrievePhoto(uid)
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    return localRepository.deletePhoto(uid)
  }
}
