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
  /** Gives the name of the file created when storing it on disk */
  fun fileName(uid: String, extension: String): String = uid + extension

  init {
    if (!photosDir.exists()) photosDir.mkdirs()
  }

  override suspend fun retrievePhoto(uid: String): Photo {
    val file: File =
        photosDir.listFiles()?.firstOrNull() { it.nameWithoutExtension == uid }
            ?: throw FileNotFoundException("Photo with uid $uid does not exist")
    if (!file.exists()) throw FileNotFoundException("Photo with uid $uid does not exist")
    return Photo(
        image = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file),
        uid = uid)
  }

  override suspend fun uploadPhoto(photo: Photo) {
    val persistentFile =
        File(
            photosDir,
            fileName(
                uid = photo.uid,
                extension =
                    PhotoRepository.getExtensionFromUri(context = context, uri = photo.image)))
    context.contentResolver.openInputStream(photo.image)?.use { inputStream ->
      persistentFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    }
    Log.d("PhotoRepositoryLocal", "File successfully moved to persistent files")
  }

  override suspend fun deletePhoto(uid: String): Boolean {
    val file =
        photosDir.listFiles()?.firstOrNull() { it.nameWithoutExtension == uid } ?: return false
    if (file.exists()) return file.delete()
    return false
  }

  /** Delete every [Photo] known to this repository */
  fun clearRepository() {
    photosDir.listFiles()?.forEach { file -> file.delete() }
  }
}
