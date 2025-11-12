package com.android.mySwissDorm.model.photo

import android.content.Context
import android.util.Log
import androidx.core.net.toFile
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

/** The directory in which the photo are stored in storage */
const val DIR = "photos"

/** This is an implementation of a [PhotoRepositoryCloud] using the storage service of Firebase. */
class PhotoRepositoryStorage(
    private val context: Context,
    storageRef: StorageReference = Firebase.storage.reference,
    localRepository: PhotoRepository = PhotoRepositoryProvider.local_repository
) : PhotoRepositoryCloud(localRepository) {

  private val dirRef = storageRef.child("$DIR/")

  override suspend fun retrievePhoto(uid: String): Photo {
    try {
      Log.d("PhotoRepositoryStorage", "Try to retrieve $uid from the local repository")
      return super.retrievePhoto(uid)
    } catch (_: NoSuchElementException) {
      Log.d("PhotoRepositoryStorage", "Fail to retrieve from local, try on cloud")
      val photo = Photo.createNewTempPhoto(uid)
      try {
        val matchRef: StorageReference = findPhotoRef(uid = uid)
        matchRef.getFile(photo.image).await().let {
          Log.d("PhotoRepositoryStorage", "download ${it.bytesTransferred} bytes")
        }
        // keep on local repository the downloaded photo
        super.uploadPhoto(photo)
      } catch (_: StorageException) {
        Log.d("PhotoRepositoryStorage", " Fail to download $uid on cloud")
        throw NoSuchElementException("Error during the cloud retrieving")
      } finally {
        photo.image.toFile().delete()
      }
      return photo
    }
  }

  override suspend fun uploadPhoto(photo: Photo) {
    super.uploadPhoto(photo)
    val photoRef = dirRef.child(photo.fileName)
    val uploadTask = photoRef.putFile(photo.image)

    uploadTask.await().let {
      Log.d("PhotoRepositoryStorage", "upload ${it.bytesTransferred} bytes")
    }
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    val del1 = super.deletePhoto(uid)
    val del2 =
        try {
          findPhotoRef(uid).delete().await()
          true
        } catch (e: StorageException) {
          if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
            false
          } else {
            throw e
          }
        }
    return del1 || del2
  }

  private suspend fun findPhotoRef(uid: String): StorageReference {
    return dirRef.child(uid)
  }
}
