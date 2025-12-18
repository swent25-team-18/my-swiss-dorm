package com.android.mySwissDorm.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Common dimension values used throughout the app. These replace hard-coded values for better
 * maintainability and consistency.
 *
 * Note on Padding vs Spacing:
 * - Padding values should be used for Modifier.padding() - padding is spacing in all directions
 * - Spacing values should be used for Spacer components and Arrangement.spacedBy() While padding is
 *   conceptually spacing in all directions, we maintain separate constants for semantic clarity and
 *   to ensure consistency across the codebase.
 */
object Dimens {
  // Padding (for Modifier.padding() - spacing in all directions)
  val PaddingXSmall = 4.dp
  val PaddingSmall = 8.dp
  val PaddingMedium = 12.dp
  val PaddingDefault = 16.dp
  val PaddingLarge = 24.dp
  val PaddingHorizontalLarge = 32.dp
  val PaddingTopSmall = 10.dp

  // Spacing (for Spacer components and Arrangement.spacedBy())
  val SpacingXSmall = 3.dp
  val SpacingSmall = 4.dp
  val SpacingTiny = 5.dp
  val SpacingMedium = 6.dp
  val SpacingDefault = 8.dp
  val SpacingLarge = 12.dp
  val SpacingXLarge = 16.dp
  val SpacingXXLarge = 28.dp

  // Icon sizes
  val IconSizeSmall = 14.dp
  val IconSizeMedium = 16.dp
  val IconSizeDefault = 20.dp
  val IconSizeLarge = 24.dp
  val IconSizeXLarge = 28.dp
  val IconSizeXXLarge = 30.dp
  val IconSizeXXXLarge = 32.dp
  val IconSizeButton = 40.dp

  // Button dimensions
  val ButtonHeight = 52.dp
  val ButtonHeightLarge = 56.dp
  val ButtonIconSize = 24.dp

  // Image dimensions
  val ImageSizeSmall = 64.dp
  val ImageSizeMedium = 140.dp
  val ImageSizeLarge = 180.dp
  val ImageSizeXLarge = 200.dp
  val ImageSizeAvatar = 56.dp
  val ImageSizeAvatarLarge = 180.dp

  // Card dimensions
  val CardImageHeight = 150.dp
  val CardCornerRadius = 12.dp

  // Corner radius (for RoundedCornerShape - separate from padding for modularity)
  val CornerRadiusSmall = 4.dp
  val CornerRadiusMedium = 8.dp
  val CornerRadiusDefault = 12.dp
  val CornerRadiusLarge = 16.dp
  val CornerRadiusXLarge = 20.dp

  // Dialog dimensions
  val DialogWidth = 260.dp
  val DialogQRCodeSize = 280.dp

  // FAB dimensions
  val FABSize = 64.dp

  // Profile picture sizes
  val ProfilePictureSize = 64.dp
  val ProfilePictureSizeLarge = 180.dp

  // Spacer heights (for layout spacing)
  val SpacerHeightSmall = 160.dp
  val SpacerHeightLarge = 225.dp

  // CircularProgressIndicator dimensions
  val CircularProgressIndicatorSize = 24.dp
  val CircularProgressIndicatorSizeLarge = 64.dp
  val CircularProgressIndicatorStrokeWidth = 2.dp

  // Border width (for border modifiers)
  val BorderWidthXSmall = 2.dp
  val BorderWidthSmall = 3.dp
  val BorderWidthDefault = 4.dp

  // Alpha values
  val AlphaLow = 0.2f
  val AlphaDisabled = 0.3f
  val AlphaMedium = 0.5f
  val AlphaSecondary = 0.6f
  val AlphaHigh = 0.9f
}
