package com.android.mySwissDorm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract

class FakeTakePictureContract private constructor(private val shouldSucceed: Boolean = true) :
    ActivityResultContract<Uri, Boolean>() {

  override fun createIntent(context: Context, input: Uri): Intent {
    return Intent("FAKE")
  }

  override fun getSynchronousResult(context: Context, input: Uri): SynchronousResult<Boolean>? {
    return SynchronousResult(shouldSucceed)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
    return shouldSucceed
  }

  companion object {
    fun success() = FakeTakePictureContract(true)

    fun failure() = FakeTakePictureContract(false)
  }
}

class FakeRequestPermissionContract private constructor(private val shouldSucceed: Boolean = true) :
    ActivityResultContract<String, Boolean>() {
  override fun createIntent(context: Context, input: String): Intent {
    return Intent("FAKE")
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
    return shouldSucceed
  }

  override fun getSynchronousResult(context: Context, input: String): SynchronousResult<Boolean>? {
    return SynchronousResult(shouldSucceed)
  }

  companion object {
    fun success() = FakeRequestPermissionContract(true)

    fun failure() = FakeRequestPermissionContract(false)
  }
}

class FakeGetContentContract(private val shouldSucceed: Boolean = true, val fakeUri: String) :
    ActivityResultContract<String, Uri?>() {

  override fun createIntent(context: Context, input: String): Intent {
    return Intent("FAKE")
  }

  override fun getSynchronousResult(context: Context, input: String): SynchronousResult<Uri?>? {
    return if (shouldSucceed) {
      SynchronousResult(Uri.parse(fakeUri))
    } else {
      SynchronousResult(null)
    }
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return if (shouldSucceed) {
      Uri.parse(fakeUri)
    } else {
      null
    }
  }
}

class FakePickMultipleVisualMediaContract(
    shouldSucceed: Boolean = true,
    fakeUri: String = Uri.EMPTY.toString(),
    fakeUri2: String = Uri.EMPTY.toString()
) : ActivityResultContract<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

  val list: List<@JvmSuppressWildcards Uri> =
      if (shouldSucceed) {
        listOf(Uri.parse(fakeUri), Uri.parse(fakeUri2))
      } else {
        emptyList()
      }

  override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
    return Intent()
  }

  override fun parseResult(resultCode: Int, intent: Intent?): List<@JvmSuppressWildcards Uri> {
    return list
  }

  override fun getSynchronousResult(
      context: Context,
      input: PickVisualMediaRequest
  ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? {
    return SynchronousResult(list)
  }
}
