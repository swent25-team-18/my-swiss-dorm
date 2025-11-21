package com.android.mySwissDorm.utils

import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud

class FakePhotoRepository(
    private val onRetrieve: () -> Photo,
    private val onUpload: () -> Unit,
    private val onDelete: Boolean
) : PhotoRepository {
  var retrieveCount = 0
  var uploadCount = 0
  var deleteCount = 0

  override suspend fun retrievePhoto(uid: String): Photo {
    ++retrieveCount
    return onRetrieve()
  }

  override suspend fun uploadPhoto(photo: Photo) {
    ++uploadCount
    onUpload()
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    ++deleteCount
    return onDelete
  }

  companion object {
    fun commonLocalRepo(
        onRetrieve: () -> Photo,
        onUpload: () -> Unit,
        onDelete: Boolean
    ): FakePhotoRepository {
      var i = 0
      return FakePhotoRepository(
          onRetrieve = {
            if (i > 0) {
              i++
              throw NoSuchElementException()
            } else {
              onRetrieve()
            }
          },
          onUpload,
          onDelete)
    }
  }
}

class FakePhotoRepositoryCloud(
    private val onRetrieve: () -> Photo,
    private val onUpload: () -> Unit,
    private val onDelete: Boolean,
    photoRepositoryLocal: PhotoRepository =
        FakePhotoRepository({ throw NoSuchElementException() }, {}, false)
) : PhotoRepositoryCloud() {
  var retrieveCount = 0
  var uploadCount = 0
  var deleteCount = 0

  override suspend fun retrievePhoto(uid: String): Photo {
    ++retrieveCount
    return onRetrieve()
  }

  override suspend fun uploadPhoto(photo: Photo) {
    ++uploadCount
    onUpload()
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    ++deleteCount
    return onDelete
  }
}
