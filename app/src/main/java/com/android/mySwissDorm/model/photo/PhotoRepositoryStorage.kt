package com.android.mySwissDorm.model.photo

import android.util.Log
import androidx.core.net.toFile
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

/** The directory in which the photo are stored in storage */
const val DIR = "photos"

/**
 * This is an implementation of a [PhotoRepositoryCloud] using the storage service of Firebase.
 *
 * @param photoSubDir this specify in which subdirectory the operations should be done. This must
 *   respect this format: "path/to/subdirectory/"
 */
class PhotoRepositoryStorage(
    storageRef: StorageReference = Firebase.storage.reference,
    localRepository: PhotoRepository = PhotoRepositoryProvider.localRepository,
    photoSubDir: String = ""
) : PhotoRepositoryCloud(localRepository) {

  init {
    require(photoSubDir.firstOrNull()?.isLetter() ?: true)
    require(photoSubDir.isEmpty() || photoSubDir.last() == '/')
  }

  private val dirRef = storageRef.child("$DIR/$photoSubDir")

  override suspend fun retrievePhoto(uid: String): Photo {
    try {
      Log.d("PhotoRepositoryStorage", "Try to retrieve $uid from the local repository")
      return super.retrievePhoto(uid)
    } catch (_: NoSuchElementException) {
      Log.d("PhotoRepositoryStorage", "Fail to retrieve from local, try on cloud")
      val photo = Photo.createNewTempPhoto(uid)
      try {
        val matchRef: StorageReference = findPhotoRef(uid = uid)
        val taskSnapshot = matchRef.getFile(photo.image).await()
        Log.d("PhotoRepositoryStorage", "download ${taskSnapshot.bytesTransferred} bytes")

        // Validate that we actually downloaded something
        val downloadedFile = photo.image.toFile()
        if (downloadedFile.length() == 0L) {
          Log.e("PhotoRepositoryStorage", "Downloaded file is empty (0 bytes): $uid")
          downloadedFile.delete()
          throw NoSuchElementException("Downloaded file is corrupted/empty: $uid")
        }

        // keep on local repository the downloaded photo
        super.uploadPhoto(photo)
        return photo
      } catch (_: StorageException) {
        Log.d("PhotoRepositoryStorage", " Fail to download $uid on cloud")
        photo.image.toFile().delete()
        throw NoSuchElementException("Error during the cloud retrieving")
      } catch (e: Exception) {
        Log.e("PhotoRepositoryStorage", "Error downloading photo: $uid - ${e.message}")
        photo.image.toFile().delete()
        throw e
      }
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

  private fun findPhotoRef(uid: String): StorageReference {
    return dirRef.child(uid)
  }
}

/**
 * Returns a Firebase Storage download URL for a stored photo filename (in `photos/`).
 *
 * Kept here (photo layer) so UI can reuse it when a URL is required (e.g., Stream Chat user image).
 */
suspend fun getPhotoDownloadUrl(
    fileName: String,
    storageRef: StorageReference = Firebase.storage.reference,
): String {
  return storageRef.child("$DIR/$fileName").downloadUrl.await().toString()
}
