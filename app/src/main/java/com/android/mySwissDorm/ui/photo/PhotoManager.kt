package com.android.mySwissDorm.ui.photo

import android.net.Uri
import android.util.Log
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.model.photo.PhotoRepository
import com.android.mySwissDorm.model.photo.PhotoRepositoryCloud
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider

class PhotoManager(
    private val photoRepositoryLocal: PhotoRepository = PhotoRepositoryProvider.localRepository,
    private val photoRepositoryCloud: PhotoRepositoryCloud =
        PhotoRepositoryProvider.cloudRepository,
) {
  private var _photosLoaded: List<Photo> = emptyList()

  /** The current loaded photos by this [PhotoManager] */
  val photoLoaded: List<Photo>
    get() = _photosLoaded.toList()

  private val _deletedPhotos: MutableList<String> = emptyList<String>().toMutableList()

  /** The photos to be deleted in the cloud repository */
  val deletedPhotos: List<String>
    get() = _deletedPhotos.toList()

  private val _newPhotos: MutableList<Photo> = emptyList<Photo>().toMutableList()

  /** The photos to be commited to the cloud repository */
  val newPhotos: List<Photo>
    get() = _newPhotos.toList()

  /**
   * Initialize the loaded photos to the given list
   *
   * @param initialPhotos the initial photos
   */
  suspend fun initialize(initialPhotos: List<String>) {
    _photosLoaded =
        initialPhotos.mapNotNull { fileName ->
          try {
            photoRepositoryCloud.retrievePhoto(fileName)
          } catch (_: NoSuchElementException) {
            Log.d("PhotoManager", "Cannot retrieve the photo : $fileName")
            null
          }
        }
    _deletedPhotos.removeAll { true }
    _newPhotos.removeAll { true }
  }

  /**
   * This adds a photo to the local repository and the new photos to be commited
   *
   * @param photo the new photo
   */
  suspend fun addPhoto(photo: Photo) {
    // Manage the newly added images history in case of cancellation
    if (_deletedPhotos.contains(photo.fileName)) {
      _deletedPhotos.remove(photo.fileName)
    } else {
      _newPhotos.add(photo)
    }

    // Only upload if the image is not already on disk
    try {
      photoRepositoryLocal.retrievePhoto(photo.fileName)
    } catch (_: NoSuchElementException) {
      photoRepositoryLocal.uploadPhoto(photo)
    }
    val images = _photosLoaded.toMutableList()
    images.add(photo)
    _photosLoaded = images
  }

  /**
   * This removes a photo from the list of loaded photos. It could also delete them from the local
   * repository if [removeFromLocal] is true
   *
   * @param uri the [android.net.Uri] of the photo to be removed
   * @param removeFromLocal true if the photo should be removed from the local repository
   */
  suspend fun removePhoto(uri: Uri, removeFromLocal: Boolean) {
    // Manage the newly deleted images history in case of cancellation
    if (_newPhotos.map { it.image }.contains(uri)) {
      _newPhotos.remove(_newPhotos.find { it.image == uri })
    } else {
      _photosLoaded.find { it.image == uri }?.fileName?.let { _deletedPhotos.add(it) }
    }
    val photo = _photosLoaded.find { it.image == uri }
    val images = _photosLoaded.toMutableList()
    images.remove(photo)
    if (removeFromLocal) {
      photo?.let {
        if (photoRepositoryLocal.deletePhoto(photo.fileName)) {
          _photosLoaded = images
        }
      }
    } else {
      _photosLoaded = images
    }
  }

  /**
   * This delete all the initial photos from the cloud repository, and delete the new ones from the
   * local repository
   */
  suspend fun deleteAll() {

    _photosLoaded.forEach { photoRepositoryCloud.deletePhoto(it.fileName) }
    _newPhotos.forEach { photoRepositoryLocal.deletePhoto(it.fileName) }
    _deletedPhotos.forEach { photoRepositoryCloud.deletePhoto(it) }
  }

  /** This commit the changes to the cloud repository */
  suspend fun commitChanges() {
    _newPhotos.forEach { photoRepositoryCloud.uploadPhoto(it) }
    _newPhotos.removeAll { true }
    _deletedPhotos.forEach { photoRepositoryCloud.deletePhoto(it) }
    _deletedPhotos.removeAll { true }
    Log.d(
        "PhotoManager",
        "Sent to be added: ${_newPhotos.size}. Sent to be deleted: ${_deletedPhotos.size}")
  }
}
