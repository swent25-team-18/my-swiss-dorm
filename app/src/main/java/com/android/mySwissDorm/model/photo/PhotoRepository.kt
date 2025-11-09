package com.android.mySwissDorm.model.photo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile

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

  /**
   * Delete a photo in the repository
   *
   * @param uid the uid of the photo to be deleted
   * @return true if the photo has been deleted with success
   */
  suspend fun deletePhoto(uid: String): Boolean

  companion object {
    /**
     * Helper functions that return the extension of the image pointed by the Uri
     *
     * @param context the [Context] in which the image is defined
     * @param uri the [Uri] that points to the image
     * @return the extension of the image in the format ".$extension" (for example : ".jpg",
     *   ".png",...)
     * @throws IllegalArgumentException if the file pointed by [uri] is not an image
     */
    fun getExtensionFromUri(context: Context, uri: Uri): String {
      return when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT ->
            context.contentResolver.getType(uri)?.let { mimeType ->
              if (!mimeType.startsWith("image/"))
                  throw IllegalArgumentException("The file is not an image : $mimeType")
              ".${mimeType.removePrefix("image/")}"
            }
        ContentResolver.SCHEME_FILE -> ".${uri.toFile().extension}"
        else -> null
      } ?: throw IllegalArgumentException("Unknown file type")
    }
  }
}
