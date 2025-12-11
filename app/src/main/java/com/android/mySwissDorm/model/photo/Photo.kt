package com.android.mySwissDorm.model.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/** This class represent a photo in the application */
data class Photo(val image: Uri, val fileName: String) {
  companion object {
    /**
     * Create a new photo stored in cache
     *
     * @param fileName the name of the file created (must be unique)
     * @return an empty [Photo]
     */
    fun createNewTempPhoto(fileName: String): Photo {
      val file =
          File.createTempFile(
              fileName.substringBeforeLast('.'), "." + fileName.substringAfterLast('.'))
      return Photo(image = file.toUri(), fileName = fileName)
    }

    fun createCapturablePhoto(context: Context, fileName: String): Photo {
      val file =
          File.createTempFile(
              fileName.substringBeforeLast('.'),
              "." + fileName.substringAfterLast('.'),
              context.cacheDir)
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
      return Photo(image = uri, fileName = fileName)
    }

    fun deleteCapturablePhoto(context: Context, uri: Uri): Int {
      return context.contentResolver.delete(uri, null, null)
    }
  }
}
