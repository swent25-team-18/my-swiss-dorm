package com.android.mySwissDorm.ui.authentification

import android.content.Context
import androidx.credentials.GetCredentialRequest
import com.android.mySwissDorm.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

object GoogleHelper {
  private fun getSignInOptions(context: Context): GetSignInWithGoogleOption {
    return GetSignInWithGoogleOption.Builder(
            serverClientId = context.getString(R.string.default_web_client_id))
        .build()
  }

  fun getSignInRequest(context: Context): GetCredentialRequest {
    val signInOptions = getSignInOptions(context)
    return GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()
  }
}
