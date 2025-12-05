package com.android.mySwissDorm.model.map

/** Provides a single instance of WalkingRouteService in the app. */
object WalkingRouteServiceProvider {
  private val _service: WalkingRouteService by lazy {
    WalkingRouteService(HttpClientProvider.client)
  }

  var service: WalkingRouteService = _service
}
