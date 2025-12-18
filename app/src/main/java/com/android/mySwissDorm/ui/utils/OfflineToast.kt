package com.android.mySwissDorm.ui.utils

import android.content.Context
import android.widget.Toast
import com.android.mySwissDorm.R

/**
 * Utility function to show a toast message when a feature requires network connectivity.
 *
 * This centralizes the offline toast message to avoid code duplication across screens.
 *
 * @param context The Android context to show the toast.
 */
fun showOfflineToast(context: Context) {
  Toast.makeText(
          context, context.getString(R.string.offline_feature_unavailable), Toast.LENGTH_SHORT)
      .show()
}
