package com.android.mySwissDorm.model.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.mySwissDorm.BuildConfig
import java.io.File
import java.util.NoSuchElementException
import java.util.Objects
import java.util.UUID

/** This class represent a photo in the application */
data class Photo(val image: Uri, val uid: String) {
  companion object {
    /**
     * Create a new photo stored in cache
     *
     * @param context the [Context] in which the photo will be created
     * @param uid the unique identifier of a photo
     * @return an empty [Photo]
     */
    fun createNewPhotoOnCache(context: Context, uid: String): Photo {
      val file = File.createTempFile("${UUID.randomUUID()}", ".jpg", context.externalCacheDir)
      return Photo(
          image =
              FileProvider.getUriForFile(
                  Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file),
          uid = uid)
    }

    fun deletePhoto(photo: Photo): Boolean {
      return try {
        File(photo.image.path ?: throw NoSuchElementException()).delete()
      } catch (_: kotlin.NoSuchElementException) {
        false
      }
    }
  }
}
