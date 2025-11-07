package com.android.mySwissDorm.model.map

import okhttp3.OkHttpClient

/**
 * Provide an OkHttpClient client for network requests.
 *
 * Property `client` is mutable for testing purposes.
 */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}
