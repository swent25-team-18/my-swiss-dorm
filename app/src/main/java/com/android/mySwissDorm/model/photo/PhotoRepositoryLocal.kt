package com.android.mySwissDorm.model.photo

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import com.android.mySwissDorm.BuildConfig
import java.io.File
import okio.FileNotFoundException

/**
 * An implementation of [PhotoRepository] on disk.
 *
 * @param context the context on which IO operations will be effectuated
 */
class PhotoRepositoryLocal(private val context: Context) : PhotoRepository {
  val photosDir = File(context.filesDir, "photos")

  init {
    if (!photosDir.exists()) photosDir.mkdirs()
  }

  override suspend fun retrievePhoto(uid: String): Photo {
    val file: File =
        photosDir.listFiles()?.firstOrNull() { it.name == uid }
            ?: throw FileNotFoundException("Photo with uid $uid does not exist")
    if (!file.exists()) throw FileNotFoundException("Photo with uid $uid does not exist")
    return Photo(
        image = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file),
        fileName = uid)
  }

  override suspend fun uploadPhoto(photo: Photo) {
    val persistentFile = File(photosDir, photo.fileName)
    context.contentResolver.openInputStream(photo.image)?.use { inputStream ->
      persistentFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    }
    Log.d("PhotoRepositoryLocal", "File successfully moved to persistent files")
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    val file = photosDir.listFiles()?.firstOrNull() { it.name == uid } ?: return false
    if (file.exists()) return file.delete()
    return false
  }

  /** Delete every [Photo] known to this repository */
  fun clearRepository() {
    photosDir.listFiles()?.forEach { file -> file.delete() }
  }
}
