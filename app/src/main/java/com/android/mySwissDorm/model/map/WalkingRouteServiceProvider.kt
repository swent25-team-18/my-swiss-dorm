package com.android.mySwissDorm.model.map

import android.content.Context

/** Provides a single instance of WalkingRouteService in the app. */
object WalkingRouteServiceProvider {
  private var _service: WalkingRouteService? = null

  /**
   * Initializes the service with persistent caching. Should be called once during app startup with
   * Application context.
   */
  fun initialize(context: Context) {
    if (_service == null) {
      val persistentCache = RouteCacheManager(context.applicationContext)
      _service = WalkingRouteService(HttpClientProvider.client, persistentCache = persistentCache)
    }
  }

  /**
   * Gets the service instance. If not initialized, creates one without persistent cache. For best
   * results, call initialize() first with Application context.
   */
  val service: WalkingRouteService
    get() {
      if (_service == null) {
        _service = WalkingRouteService(HttpClientProvider.client)
      }
      return _service!!
    }
}
