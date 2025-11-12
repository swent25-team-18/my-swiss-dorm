package com.android.mySwissDorm.model.photo

import android.net.Uri
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
      file.deleteOnExit()
      return Photo(image = file.toUri(), fileName = fileName)
    }
  }
}
