package com.android.mySwissDorm.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.White

/**
 * Speed-dial FAB that shows two actions in a small popup above it.
 *
 * Implemented with the help of AI
 */
@Composable
fun AddFabMenu(modifier: Modifier = Modifier, onAddListing: () -> Unit, onAddReview: () -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
      Box(
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f))
              .clickable { expanded = false }
              .testTag("fab_scrim"))
    }

    Column(
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End) {
          AnimatedVisibility(
              visible = expanded,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End) {
                      FabMiniAction(
                          text = "Add listing",
                          icon = {
                            Icon(Icons.Outlined.HomeWork, tint = White, contentDescription = null)
                          },
                          onClick = {
                            expanded = false
                            onAddListing()
                          },
                          tag = "fab_menu_listing")
                      FabMiniAction(
                          text = "Add review",
                          icon = {
                            Icon(Icons.Outlined.RateReview, tint = White, contentDescription = null)
                          },
                          onClick = {
                            expanded = false
                            onAddReview()
                          },
                          tag = "fab_menu_review")
                    }
              }

          FloatingActionButton(
              onClick = { expanded = !expanded },
              elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
              shape = CircleShape,
              containerColor = MainColor,
              modifier = Modifier.size(64.dp).testTag("fab_main")) {
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
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    tag: String
) {

  Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
      color = MainColor,
      contentColor = White,
      modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.large).testTag(tag)) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
              icon()
              Spacer(modifier = Modifier.size(8.dp))
              Text(text, color = White, fontWeight = FontWeight.Medium)
            }
      }
}
