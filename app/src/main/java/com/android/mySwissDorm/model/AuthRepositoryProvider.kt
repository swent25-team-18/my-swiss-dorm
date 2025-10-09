package com.android.mySwissDorm.model

object AuthRepositoryProvider {
    private val _repository: AuthRepository by lazy { AuthRepositoryFirebase() }
    var repository: AuthRepository = _repository
}
