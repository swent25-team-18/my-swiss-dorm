package com.android.mySwissDorm.ui.overview

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.showOfflineToast

/**
 * Speed-dial FAB that shows two actions in a small popup above it.
 *
 * Implemented with the help of AI
 */
@Composable
fun AddFabMenu(
    modifier: Modifier = Modifier,
    onAddListing: () -> Unit,
    onAddReview: () -> Unit,
    isGuest: Boolean = false,
    isOffline: Boolean = false
) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }
  val isDisabled = isGuest || isOffline
  if (isDisabled && expanded) {
    expanded = false
  }

  Box(modifier = modifier.testTag(C.BrowseCityTags.FABSCRIM)) {
    AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
      Box(Modifier.fillMaxSize().clickable { expanded = false })
    }

    Column(
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(end = Dimens.PaddingDefault, bottom = Dimens.PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge),
        horizontalAlignment = Alignment.End) {
          AnimatedVisibility(
              visible = expanded,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingDefault),
                    horizontalAlignment = Alignment.End) {
                      FabMiniAction(
                          textId = R.string.add_button_add_listing,
                          icon = {
                            Icon(Icons.Outlined.HomeWork, tint = White, contentDescription = null)
                          },
                          onClick = {
                            expanded = false
                            onAddListing()
                          },
                          tag = C.BrowseCityTags.FABMENULISTING)
                      FabMiniAction(
                          textId = R.string.add_button_add_review,
                          icon = {
                            Icon(Icons.Outlined.RateReview, tint = White, contentDescription = null)
                          },
                          onClick = {
                            expanded = false
                            onAddReview()
                          },
                          tag = C.BrowseCityTags.FABMENUREVIEW)
                    }
              }

          FloatingActionButton(
              onClick = {
                if (isDisabled) {
                  if (isOffline) {
                    showOfflineToast(context)
                  }
                } else {
                  expanded = !expanded
                }
              },
              elevation =
                  FloatingActionButtonDefaults.elevation(defaultElevation = Dimens.SpacingMedium),
              shape = CircleShape,
              containerColor = if (isDisabled) Gray else MainColor,
              modifier = Modifier.size(Dimens.FABSize).testTag(C.BrowseCityTags.FABMENU)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    tint = White,
                    contentDescription = if (expanded) "Close add menu" else "Open add menu")
              }
        }
  }
}

@Composable
private fun FabMiniAction(
    @StringRes textId: Int,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    tag: String
) {

  Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = Dimens.SpacingMedium,
      shadowElevation = Dimens.PaddingSmall,
      color = MainColor,
      contentColor = White,
      modifier = Modifier.shadow(Dimens.PaddingSmall, MaterialTheme.shapes.large).testTag(tag)) {
        TextButton(
            onClick = onClick,
            contentPadding =
                PaddingValues(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall)) {
              icon()
              Spacer(modifier = Modifier.size(Dimens.SpacingDefault))
              Text(stringResource(textId), color = White, fontWeight = FontWeight.Medium)
            }
      }
}
