package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.map.MapPreview
import com.android.mySwissDorm.ui.photo.FullScreenImageViewer
import com.android.mySwissDorm.ui.photo.ImageGrid
import com.android.mySwissDorm.ui.share.ShareLinkDialog
import com.android.mySwissDorm.ui.theme.AlmostWhite
import com.android.mySwissDorm.ui.theme.Black
import com.android.mySwissDorm.ui.theme.DarkGray
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.OutlineColor
import com.android.mySwissDorm.ui.theme.PinkyWhite
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.Violet
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatRelative
import com.android.mySwissDorm.utils.NetworkUtils

/**
 * Screen that displays the full details of a rental listing and allows the user to contact the
 * owner or edit the listing.
 *
 * The screen shows title, metadata (price, area, start date), description, images, map preview, and
 * provides actions for bookmarking, sharing, applying (sending a contact message), or editing when
 * the current user is the owner.
 *
 * @param viewListingViewModel The [ViewListingViewModel] providing listing data and actions.
 * @param listingUid The unique identifier of the listing to display.
 * @param onGoBack Callback invoked when the user taps the back button.
 * @param onApply Callback invoked when the user taps the \"Apply\" button after writing a message.
 * @param onEdit Callback invoked when the owner taps the \"Edit\" button.
 * @param onViewProfile Callback invoked when the poster's name is tapped to view their profile.
 * @param onViewMap Callback invoked when the map preview is tapped to open the full map screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewListingScreen(
    viewListingViewModel: ViewListingViewModel = viewModel(),
    listingUid: String,
    onGoBack: () -> Unit = {},
    onApply: () -> Unit = {},
    onEdit: () -> Unit = {},
    onViewProfile: (ownerId: String) -> Unit = {},
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit =
        { _, _, _, _ ->
        }
) {
  val context = LocalContext.current
  LaunchedEffect(listingUid) { viewListingViewModel.loadListing(listingUid, context) }

  val listingUIState by viewListingViewModel.uiState.collectAsState()
  val listing = listingUIState.listing
  val fullNameOfPoster = listingUIState.fullNameOfPoster
  val errorMsg = listingUIState.errorMsg
  val hasMessage = listingUIState.contactMessage.any { !it.isWhitespace() }
  val isOwner = listingUIState.isOwner
  val isBlockedByOwner = listingUIState.isBlockedByOwner
  val isBookmarked = listingUIState.isBookmarked
  val hasExistingMessage = listingUIState.hasExistingMessage
  var showShareDialog by remember { mutableStateOf(false) }
  var isTranslated by remember { mutableStateOf(false) }
  var showTranslateButton by remember { mutableStateOf(false) }

  // Button is enabled only if there's a message, user is not blocked, and no existing message
  val canApply = hasMessage && !isBlockedByOwner && !hasExistingMessage
  // Button color: violet if blocked, red (MainColor) if normal
  val buttonColor = if (isBlockedByOwner && hasMessage) Violet else MainColor

  // Generate share link
  val shareLink = "https://my-swiss-dorm.web.app/listing/$listingUid"

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      onGoBack()
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewListingViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(listing) { viewListingViewModel.translateListing(context) }

  LaunchedEffect(listingUIState.translatedDescription) {
    showTranslateButton =
        if (listingUIState.translatedDescription == "") {
          false
        } else {
          listing.description != listingUIState.translatedDescription
        }
  }

  if (listingUIState.showFullScreenImages) {
    FullScreenImageViewer(
        imageUris = listingUIState.images.map { it.image },
        onDismiss = { viewListingViewModel.dismissFullScreenImages() },
        initialIndex = listingUIState.fullScreenImagesIndex)
    return
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.view_listing_title)) },
            navigationIcon = {
              IconButton(
                  onClick = { onGoBack() },
                  modifier = Modifier.testTag(C.ViewListingTags.BACK_BTN)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MainColor)
                  }
            },
            actions = {
              IconButton(
                  onClick = { showShareDialog = true },
                  modifier = Modifier.testTag(C.ShareLinkDialogTags.SHARE_BTN)) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = MainColor)
                  }
              if (!listingUIState.isGuest && !isOwner) {
                IconButton(
                    onClick = { viewListingViewModel.toggleBookmark(listingUid, context) },
                    modifier = Modifier.testTag(C.ViewListingTags.BOOKMARK_BTN)) {
                      Icon(
                          imageVector =
                              if (isBookmarked) Icons.Filled.Bookmark
                              else Icons.Outlined.BookmarkBorder,
                          contentDescription =
                              if (isBookmarked) "Remove bookmark" else "Add bookmark",
                          tint = MainColor)
                    }
              }
            })
      },
      content = { paddingValues ->
        if (isBlockedByOwner && !isOwner) {
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .padding(horizontal = 16.dp, vertical = 24.dp)
                      .testTag(C.ViewListingTags.ROOT),
              contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                      Text(
                          text = stringResource(R.string.view_listing_unavailable),
                          style = MaterialTheme.typography.titleLarge.copy(color = TextColor),
                          modifier = Modifier.testTag(C.ViewListingTags.BLOCKED_NOTICE))
                      Text(
                          text = stringResource(R.string.view_listing_blocked_text),
                          style = MaterialTheme.typography.bodyMedium.copy(color = DarkGray),
                          textAlign = TextAlign.Center)
                      Button(
                          onClick = onGoBack,
                          modifier =
                              Modifier.fillMaxWidth(0.6f)
                                  .testTag(C.ViewListingTags.BLOCKED_BACK_BTN),
                          shape = RoundedCornerShape(14.dp),
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = MainColor, contentColor = White)) {
                            Text(stringResource(R.string.go_back))
                          }
                    }
              }
        } else {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .verticalScroll(rememberScrollState())
                      .imePadding()
                      .testTag(C.ViewListingTags.ROOT),
              verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showTranslateButton) {
                  val clickableText =
                      if (isTranslated) {
                        context.getString(R.string.see_original)
                      } else {
                        context.getString(R.string.view_listing_translate_listing)
                      }
                  Text(
                      text = clickableText,
                      modifier =
                          Modifier.clickable(onClick = { isTranslated = !isTranslated })
                              .testTag(C.ViewListingTags.TRANSLATE_BTN),
                      color = MainColor)
                }
                val titleToDisplay =
                    if (isTranslated) listingUIState.translatedTitle else listing.title
                Text(
                    text = titleToDisplay,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 32.sp,
                    modifier = Modifier.testTag(C.ViewListingTags.TITLE),
                    color = TextColor)

                val context = LocalContext.current
                val baseTextStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.testTag(C.ViewListingTags.POSTED_BY),
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = "${stringResource(R.string.view_listing_posted_by)} ",
                          style = baseTextStyle,
                          color = Gray)
                      Text(
                          text =
                              fullNameOfPoster +
                                  if (isOwner)
                                      " ${stringResource(R.string.view_listing_owner_is_you)}"
                                  else "",
                          style =
                              baseTextStyle.copy(fontWeight = FontWeight.Bold, color = MainColor),
                          modifier =
                              Modifier.testTag(C.ViewListingTags.POSTED_BY_NAME).clickable {
                                // Allow navigation if online or if it's the current user's profile
                                if (NetworkUtils.isNetworkAvailable(context) || isOwner) {
                                  onViewProfile(listing.ownerId)
                                } else {
                                  Toast.makeText(
                                          context,
                                          context.getString(R.string.profile_offline_message),
                                          Toast.LENGTH_SHORT)
                                      .show()
                                }
                              })
                      Text(
                          text = " ${formatRelative(listing.postedAt, context = context)}",
                          style = baseTextStyle,
                          color = Gray)
                    }

                // Nearby Points of Interest - right after "posted by"
                val poiDistances = listingUIState.poiDistances
                Text(
                    stringResource(R.string.view_listing_nearby_points_of_interest),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.testTag(C.ViewListingTags.POI_DISTANCES))

                if (listingUIState.isLoadingPOIs) {
                  Row(
                      modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MainColor)
                        Text(
                            stringResource(R.string.poi_loading_message),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f)))
                      }
                } else if (poiDistances.isNotEmpty()) {
                  Column(
                      modifier = Modifier.padding(start = 16.dp),
                      verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        val groupedByTime = poiDistances.groupBy { it.walkingTimeMinutes }

                        groupedByTime
                            .toList()
                            .sortedBy { it.first }
                            .forEach { (timeMinutes, pois) ->
                              if (pois.size == 1) {
                                val poiDistance = pois.first()
                                val timeText =
                                    when (poiDistance.poiType) {
                                      POIDistance.TYPE_UNIVERSITY ->
                                          stringResource(
                                              R.string.view_listing_walking_time_university,
                                              poiDistance.walkingTimeMinutes,
                                              poiDistance.poiName)
                                      POIDistance.TYPE_SUPERMARKET ->
                                          stringResource(
                                              R.string.view_listing_walking_time_supermarket,
                                              poiDistance.walkingTimeMinutes,
                                              poiDistance.poiName)
                                      else -> ""
                                    }
                                Text(
                                    timeText,
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight =
                                                MaterialTheme.typography.bodySmall.lineHeight))
                              } else {
                                // Multiple POIs at the same time - combine them
                                val poiNames = pois.map { it.poiName }
                                val andString = stringResource(R.string.and)
                                val combinedNames =
                                    when (poiNames.size) {
                                      2 -> poiNames.joinToString(" $andString ")
                                      else ->
                                          poiNames.dropLast(1).joinToString(", ") +
                                              " $andString " +
                                              poiNames.last()
                                    }

                                val typeLabel =
                                    when {
                                      pois.all { it.poiType == POIDistance.TYPE_UNIVERSITY } ->
                                          stringResource(R.string.university)
                                      pois.all { it.poiType == POIDistance.TYPE_SUPERMARKET } -> ""
                                      else -> ""
                                    }

                                val timeText =
                                    if (typeLabel.isNotEmpty()) {
                                      stringResource(
                                          R.string.view_listing_walking_time_minutes_of_multiple,
                                          timeMinutes,
                                          combinedNames,
                                          typeLabel)
                                    } else {
                                      stringResource(
                                          R.string.view_listing_walking_time_minutes_of_no_type,
                                          timeMinutes,
                                          combinedNames)
                                    }

                                Text(
                                    timeText,
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight =
                                                MaterialTheme.typography.bodySmall.lineHeight))
                              }
                            }
                      }
                } else {
                  Text(
                      stringResource(R.string.view_listing_no_points_of_interest),
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color =
                                  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                      modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                }
                Spacer(Modifier.height(8.dp))

                // Bullet section
                SectionCard(modifier = Modifier.testTag(C.ViewListingTags.BULLETS)) {
                  BulletRow(listing.roomType.getName(context))
                  BulletRow(
                      "${listing.pricePerMonth}${stringResource(R.string.view_listing_price_per_month)}")
                  BulletRow("${listing.areaInM2}m²")
                  BulletRow("${stringResource(R.string.starting)} ${formatDate(listing.startDate)}")
                }

                // Description
                SectionCard(modifier = Modifier.testTag(C.ViewListingTags.DESCRIPTION)) {
                  Text(
                      "${stringResource(R.string.description)} :", fontWeight = FontWeight.SemiBold)
                  Spacer(Modifier.height(3.dp))
                  val descriptionToDisplay =
                      if (isTranslated) listingUIState.translatedDescription
                      else listing.description
                  Text(
                      descriptionToDisplay,
                      style = MaterialTheme.typography.bodyLarge,
                      modifier = Modifier.testTag(C.ViewListingTags.DESCRIPTION_TEXT))
                }

                ImageGrid(
                    imageUris = listingUIState.images.map { it.image }.toSet(),
                    isEditingMode = false,
                    onImageClick = { viewListingViewModel.onClickImage(it) },
                    onRemove = {})

                // Location placeholder
                viewListingViewModel.setLocationOfListing(listingUid)
                val location = listingUIState.locationOfListing
                if (location.latitude != 0.0 && location.longitude != 0.0) {
                  MapPreview(
                      location = location,
                      title = listing.title,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(180.dp)
                              .testTag(C.ViewListingTags.LOCATION),
                      onMapClick = {
                        onViewMap(
                            location.latitude,
                            location.longitude,
                            listing.title,
                            R.string.view_listing_listing_location)
                      })
                } else {
                  PlaceholderBlock(
                      text =
                          "${stringResource(R.string.location)} (${stringResource(R.string.not_available)})",
                      height = 180.dp,
                      modifier = Modifier.testTag(C.ViewListingTags.LOCATION))
                }

                if (listingUIState.isGuest) {
                  // The guest user has to sign in to apply to a listing when they view it
                  Box(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                      contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.view_listing_sign_in_to_apply),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MainColor,
                            fontWeight = FontWeight.Bold)
                      }
                } else if (isOwner) {
                  // Owner sees an Edit button centered, same size as Apply
                  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onEdit,
                        modifier =
                            Modifier.fillMaxWidth(0.55f)
                                .height(52.dp)
                                .testTag(C.ViewListingTags.EDIT_BTN),
                        shape = RoundedCornerShape(16.dp)) {
                          Text(
                              stringResource(R.string.edit),
                              style = MaterialTheme.typography.titleMedium,
                              color = MainColor)
                        }
                  }
                } else {
                  if (hasExistingMessage) {
                    // Show message that user has already sent a message
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) {
                          Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.view_listing_message_already_sent),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MainColor,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    stringResource(R.string.view_listing_please_wait_for_response),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                        }
                  } else {
                    // Contact message
                    OutlinedTextField(
                        value = listingUIState.contactMessage,
                        onValueChange = { viewListingViewModel.setContactMessage(it) },
                        placeholder = {
                          Text(
                              stringResource(R.string.view_listing_contact_announcer), color = Gray)
                        },
                        modifier = Modifier.fillMaxWidth().testTag(C.ViewListingTags.CONTACT_FIELD),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = false,
                        minLines = 1,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AlmostWhite,
                                unfocusedContainerColor = AlmostWhite,
                                disabledContainerColor = AlmostWhite,
                                focusedBorderColor = OutlineColor,
                                unfocusedBorderColor = OutlineColor,
                                cursorColor = Black,
                                focusedTextColor = Black))

                    // Apply now button (centered, half width, rounded, red or violet)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                      Button(
                          onClick = onApply,
                          enabled = canApply,
                          modifier =
                              Modifier.fillMaxWidth(0.55f)
                                  .height(52.dp)
                                  .testTag(C.ViewListingTags.APPLY_BTN),
                          shape = RoundedCornerShape(16.dp),
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = buttonColor,
                                  disabledContainerColor = PinkyWhite,
                                  disabledContentColor = White)) {
                            Text(
                                stringResource(R.string.view_listing_apply_now),
                                color = White,
                                style = MaterialTheme.typography.titleMedium)
                          }
                    }
                  }
                }
              }
        }
      })

  if (showShareDialog) {
    ShareLinkDialog(link = shareLink, onDismiss = { showShareDialog = false })
  }
}

/**
 * Card-like container used to group listing sections such as metadata or description.
 *
 * @param modifier Modifier applied to the card.
 * @param content Composable content displayed inside the card.
 */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      color = TextBoxColor,
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 0.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content)
      }
}

/** Single bullet row used to display a textual attribute of the listing. */
@Composable
private fun BulletRow(text: String) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = TextColor)
  }
}

/**
 * Placeholder block used when a particular section (e.g., location) is unavailable.
 *
 * @param text Text to display inside the placeholder.
 * @param height Desired height of the block.
 * @param modifier Modifier applied to the placeholder container.
 */
@Composable
private fun PlaceholderBlock(text: String, height: Dp, modifier: Modifier) {
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .height(height)
              .clip(RoundedCornerShape(16.dp))
              .background(TextBoxColor),
      contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextColor)
      }
}

@Composable
@Preview
private fun ViewListingScreenPreview() {
  ViewListingScreen(listingUid = "preview")
}
