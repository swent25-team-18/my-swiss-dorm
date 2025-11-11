package com.android.mySwissDorm.model.photo

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
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
      return super.retrievePhoto(uid)
    } catch (_: NoSuchElementException) {
      val matchRef: StorageReference = findPhotoRef(uid = uid)
      val photo = Photo.createNewPhotoOnCache(context, uid)
      matchRef.getFile(photo.image).await().let {
        Log.d("PhotoRepositoryStorage", "download ${it.bytesTransferred} bytes")
      }
      // keep on local repository the downloaded photo
      super.uploadPhoto(photo)
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
    return super.deletePhoto(uid) || findPhotoRef(uid).delete().isSuccessful
  }

  private suspend fun findPhotoRef(uid: String): StorageReference {
    return dirRef.listAll().await().items.find { item -> item.name == uid }
        ?: throw NoSuchElementException("No photo reference with uid : $uid")
  }
}
