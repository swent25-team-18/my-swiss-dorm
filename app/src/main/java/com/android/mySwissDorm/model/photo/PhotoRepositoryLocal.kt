package com.android.mySwissDorm.model.photo

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import com.android.mySwissDorm.BuildConfig
import java.io.File
import java.util.Objects
import okio.FileNotFoundException

/**
 * An implementation of [PhotoRepository] on disk.
 *
 * @param context the context on which IO operations will be effectuated
 */
class PhotoRepositoryLocal(private val context: Context) : PhotoRepository {
  val extensionFormat = ".jpg"
  /** Gives the name of the file created when storing it on disk */
  fun fileName(uid: String): String = uid + extensionFormat

  override suspend fun retrievePhoto(uid: String): Photo {
    val file = File(context.filesDir, uid + extensionFormat)
    if (!file.exists()) throw FileNotFoundException("Photo with uid $uid does not exist")
    return Photo(
        image =
            FileProvider.getUriForFile(
                Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider", file),
        uid = uid)
  }

  override suspend fun uploadPhoto(photo: Photo) {
    val persistentFile = File(context.filesDir, photo.uid + extensionFormat)
    context.contentResolver.openInputStream(photo.image)?.use { inputStream ->
      persistentFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    }
    Log.d("PhotoRepositoryLocal", "File successfully moved to persistent files")
  }
}
