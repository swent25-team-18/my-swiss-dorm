package com.android.mySwissDorm.model.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.mySwissDorm.BuildConfig
import java.io.File

/** This class represent a photo in the application */
data class Photo(val image: Uri, val fileName: String) {
  companion object {
    /**
     * Create a new photo stored in cache
     *
     * @param context the [Context] in which the photo will be created
     * @param fileName the name of the file created (must be unique)
     * @return an empty [Photo]
     */
    fun createNewPhotoOnCache(context: Context, fileName: String): Photo {
      val file =
          File.createTempFile(
              fileName.substringBeforeLast('.'),
              fileName.substringAfterLast('.'),
              context.externalCacheDir)
      file.deleteOnExit()
      return Photo(
          image =
              FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file),
          fileName = fileName)
    }
  }
}
