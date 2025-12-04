package com.android.mySwissDorm.ui.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.DateTimeUi
import com.android.mySwissDorm.utils.LastSyncTracker
import com.android.mySwissDorm.utils.NetworkUtils
import kotlinx.coroutines.delay

/**
 * Displays an offline banner at the top of the screen when the device is offline.
 *
 * The banner shows:
 * - An offline icon
 * - "You're offline" message
 * - "Last updated X hours ago" (or days/date if too far)
 *
 * The banner automatically hides when the device comes back online.
 *
 * @param modifier Modifier to be applied to the banner.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var isOffline by remember { mutableStateOf(!NetworkUtils.isNetworkAvailable(context)) }
  var lastSyncTimestamp by remember {
    mutableStateOf(LastSyncTracker.getLastSyncTimestamp(context))
  }

  // Poll network state every 2 seconds to update the banner
  LaunchedEffect(Unit) {
    while (true) {
      isOffline = !NetworkUtils.isNetworkAvailable(context)
      lastSyncTimestamp = LastSyncTracker.getLastSyncTimestamp(context)
      delay(2000)
    }
  }

  // Only show banner when offline
  if (!isOffline) {
    return
  }

  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .background(MainColor)
              .padding(horizontal = 16.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center) {
        val lastUpdatedText =
            if (lastSyncTimestamp != null) {
              val relativeTime = DateTimeUi.formatRelative(lastSyncTimestamp, context = context)
              stringResource(R.string.offline_banner_last_updated, relativeTime)
            } else {
              stringResource(R.string.offline_banner_no_sync)
            }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Icon(
              imageVector = Icons.Filled.CloudOff,
              contentDescription = stringResource(R.string.offline_banner_icon_description),
              tint = White,
              modifier = Modifier.padding(end = 8.dp))
          Text(
              text = stringResource(R.string.offline_banner_you_are_offline),
              style = MaterialTheme.typography.bodyMedium,
              color = White,
              modifier = Modifier.weight(1f))
          Text(
              text = lastUpdatedText,
              style = MaterialTheme.typography.bodySmall,
              color = White.copy(alpha = 0.9f),
              textAlign = TextAlign.End)
        }
      }
}
