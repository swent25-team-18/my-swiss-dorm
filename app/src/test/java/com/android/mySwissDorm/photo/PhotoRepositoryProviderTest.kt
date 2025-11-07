package com.android.mySwissDorm.photo

import android.content.Context
import com.android.mySwissDorm.model.photo.PhotoRepositoryProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PhotoRepositoryProviderTest {
  @Test
  fun checkRepositoryIsNotNullAfterInitialization() {
    val context: Context = RuntimeEnvironment.getApplication()
    PhotoRepositoryProvider.initialize(context)
    assertNotNull(PhotoRepositoryProvider.local_repository)
  }
}
