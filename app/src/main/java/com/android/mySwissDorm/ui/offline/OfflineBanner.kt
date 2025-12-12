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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.DateTimeUi
import com.android.mySwissDorm.utils.LastSyncTracker
import com.android.mySwissDorm.utils.NetworkUtils

/**
 * Entry point that manages the network state connection.
 *
 * This is the "smart" component that collects the network state flow and passes it to the UI.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  // Reactively observe network state changes instead of polling
  val isNetworkAvailable by
      NetworkUtils.networkStateFlow(context)
          .collectAsState(initial = NetworkUtils.isNetworkAvailable(context))
  val isOffline = !isNetworkAvailable

  // Pass the pure boolean to the content (The UI)
  OfflineBannerContent(isOffline = isOffline, modifier = modifier)
}

/**
 * Stateless component that is easy to test.
 *
 * This is the "dumb" component that just takes an `isOffline` boolean and displays the banner. Pass
 * 'isOffline' directly to test different states without mocking NetworkUtils.
 *
 * @param isOffline Whether the device is currently offline.
 * @param modifier Modifier to be applied to the banner.
 */
@Composable
fun OfflineBannerContent(isOffline: Boolean, modifier: Modifier = Modifier) {
  val context = LocalContext.current

  // Only update sync timestamp when we become offline (banner becomes visible)
  // This avoids unnecessary reads when the banner is hidden
  var lastSyncTimestamp by remember {
    mutableStateOf(LastSyncTracker.getLastSyncTimestamp(context))
  }

  // Update sync timestamp reactively when network state changes to offline
  // Use rememberUpdatedState to capture the latest context
  val currentContext by rememberUpdatedState(context)
  LaunchedEffect(isOffline) {
    if (isOffline) {
      // Only read timestamp when banner becomes visible (offline)
      lastSyncTimestamp = LastSyncTracker.getLastSyncTimestamp(currentContext)
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
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag(C.OfflineBannerTags.BANNER_ROOT),
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
              modifier = Modifier.weight(1f).testTag(C.OfflineBannerTags.OFFLINE_MESSAGE))
          Text(
              text = lastUpdatedText,
              style = MaterialTheme.typography.bodySmall,
              color = White.copy(alpha = Dimens.AlphaHigh),
              textAlign = TextAlign.End,
              modifier =
                  Modifier.testTag(
                      if (lastSyncTimestamp != null) {
                        C.OfflineBannerTags.LAST_UPDATED_TEXT
                      } else {
                        C.OfflineBannerTags.NO_SYNC_TEXT
                      }))
        }
      }
}
