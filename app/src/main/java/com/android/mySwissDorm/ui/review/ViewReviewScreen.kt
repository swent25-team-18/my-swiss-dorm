package com.android.mySwissDorm.ui.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.map.MapPreview
import com.android.mySwissDorm.ui.photo.FullScreenImageViewer
import com.android.mySwissDorm.ui.photo.ImageGrid
import com.android.mySwissDorm.ui.share.ShareLinkDialog
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Gray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatRelative
import com.android.mySwissDorm.utils.NetworkUtils
import kotlin.math.floor

// This screen looks a lot like the ViewListingScreen,
// a lot of things may have been copied and pasted
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReviewScreen(
    viewReviewViewModel: ViewReviewViewModel = viewModel(),
    reviewUid: String,
    onGoBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onViewProfile: (ownerId: String) -> Unit = {},
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit =
        { _, _, _, _ ->
        }
) {
  val context = LocalContext.current
  LaunchedEffect(reviewUid) { viewReviewViewModel.loadReview(reviewUid, context) }

  val uiState by viewReviewViewModel.uiState.collectAsState()
  val review = uiState.review
  val fullNameOfPoster = uiState.fullNameOfPoster
  val errorMsg = uiState.errorMsg //
  val isOwner = uiState.isOwner
  var showShareDialog by remember { mutableStateOf(false) }
  var isTranslated by remember { mutableStateOf(false) }
  var showTranslateButton by remember { mutableStateOf(false) }

  // Generate share link
  val shareLink = "https://my-swiss-dorm.web.app/review/$reviewUid"

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      onGoBack()
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewReviewViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(review) { viewReviewViewModel.translateReview(context) }

  LaunchedEffect(uiState.translatedDescription) {
    showTranslateButton =
        review.reviewText != uiState.translatedDescription ||
            review.title != uiState.translatedTitle
  }

  if (uiState.showFullScreenImages) {
    FullScreenImageViewer(
        imageUris = uiState.images.map { it.image },
        onDismiss = { viewReviewViewModel.dismissFullScreenImages() },
        initialIndex = uiState.fullScreenImagesIndex)
    return
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.view_review_title)) },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            })
      },
      content = { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .testTag(C.ViewReviewTags.ROOT),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              if (showTranslateButton) {
                val clickableText =
                    if (isTranslated) {
                      context.getString(R.string.see_original)
                    } else {
                      context.getString(R.string.view_review_translate_review)
                    }
                Text(
                    text = clickableText,
                    modifier =
                        Modifier.clickable(onClick = { isTranslated = !isTranslated })
                            .testTag(C.ViewReviewTags.TRANSLATE_BTN),
                    color = MainColor)
              }
              val titleToDisplay = if (isTranslated) uiState.translatedTitle else review.title
              Text(
                  text = titleToDisplay,
                  style =
                      MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                  modifier = Modifier.testTag(C.ViewReviewTags.TITLE),
                  color = TextColor)

              val context = LocalContext.current
              val baseTextStyle =
                  MaterialTheme.typography.bodyMedium.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant)

              Row(
                  modifier = Modifier.testTag(C.ViewReviewTags.POSTED_BY),
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stringResource(R.string.view_review_posted_by)} ",
                        style = baseTextStyle,
                        color = Gray)
                    Text(
                        text =
                            fullNameOfPoster +
                                if (isOwner)
                                    " ${stringResource(R.string.view_review_posted_by_you)}"
                                else "",
                        style = baseTextStyle.copy(fontWeight = FontWeight.Bold, color = MainColor),
                        modifier =
                            Modifier.testTag(C.ViewReviewTags.POSTED_BY_NAME)
                                .then(
                                    // Only make clickable if review is not anonymous AND (online OR
                                    // owner)
                                    if (!review.isAnonymous &&
                                        (NetworkUtils.isNetworkAvailable(context) || isOwner)) {
                                      Modifier.clickable { onViewProfile(review.ownerId) }
                                    } else {
                                      Modifier
                                    }))
                    Text(
                        text = " ${formatRelative(review.postedAt, context = context)}",
                        style = baseTextStyle,
                        color = Gray)
                  }

              // Bullet section
              SectionCard(modifier = Modifier.testTag(C.ViewReviewTags.BULLETS)) {
                BulletRow(review.roomType.getName(context))
                BulletRow(
                    "${review.pricePerMonth}${stringResource(R.string.view_review_price_per_month)}")
                BulletRow("${review.areaInM2}m²")
                DisplayGrade(review.grade, 24.dp)
              }

              // Actual review
              SectionCard(modifier = Modifier.testTag(C.ViewReviewTags.REVIEW_TEXT)) {
                Text("${stringResource(R.string.review)}:", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                val descriptionToDisplay =
                    if (isTranslated) uiState.translatedDescription else review.reviewText
                Text(
                    descriptionToDisplay,
                    modifier = Modifier.testTag(C.ViewReviewTags.DESCRIPTION_TEXT),
                    style = MaterialTheme.typography.bodyLarge)
              }

              ImageGrid(
                  imageUris = uiState.images.map { it.image }.toSet(),
                  isEditingMode = false,
                  onImageClick = { viewReviewViewModel.onClickImage(it) },
                  onRemove = {},
                  modifier = Modifier.testTag(C.ViewReviewTags.PHOTOS))

              // Location placeholder
              viewReviewViewModel.setLocationOfReview(reviewUid)
              val location = uiState.locationOfReview
              if (location.latitude != 0.0 && location.longitude != 0.0) {
                MapPreview(
                    location = location,
                    title = review.title,
                    modifier =
                        Modifier.fillMaxWidth().height(180.dp).testTag(C.ViewReviewTags.LOCATION),
                    onMapClick = {
                      onViewMap(
                          location.latitude, location.longitude, review.title, R.string.review)
                    })
              } else {
                PlaceholderBlock(
                    text =
                        "${stringResource(R.string.location)} (${stringResource(R.string.not_available)})",
                    height = 180.dp,
                    modifier = Modifier.testTag(C.ViewReviewTags.LOCATION))
              }

              // Vote section (always shown, but disabled for owner) - at the bottom of the review
              Column(
                  modifier = Modifier.fillMaxWidth().testTag(C.ViewReviewTags.VOTE_BUTTONS),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.view_review_was_this_useful),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextColor,
                        modifier = Modifier.padding(bottom = 8.dp))
                    VoteButtons(
                        netScore = uiState.netScore,
                        userVote = uiState.userVote,
                        isOwner = isOwner,
                        onUpvote = { viewReviewViewModel.upvoteReview(context) },
                        onDownvote = { viewReviewViewModel.downvoteReview(context) })
                  }

              if (isOwner) {
                // Owner sees an Edit button centered
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Button(
                      onClick = onEdit,
                      modifier =
                          Modifier.fillMaxWidth(0.55f)
                              .height(52.dp)
                              .testTag(C.ViewReviewTags.EDIT_BTN),
                      shape = RoundedCornerShape(16.dp),
                      colors =
                          ButtonColors(
                              containerColor = MainColor,
                              contentColor = TextBoxColor,
                              disabledContainerColor = BackGroundColor,
                              disabledContentColor = BackGroundColor),
                  ) {
                    Text(
                        stringResource(R.string.edit),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextColor)
                  }
                }
              }
            }
      })

  if (showShareDialog) {
    ShareLinkDialog(link = shareLink, onDismiss = { showShareDialog = false })
  }
}

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

@Composable
private fun BulletRow(text: String) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = TextColor)
  }
}

@Composable
fun DisplayGrade(grade: Double, starSize: Dp, testTag: String = "") {
  val maxStars = 5
  val filledStars = floor(grade).toInt()
  val hasHalfStar = grade != floor(grade)

  Row(
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End) {
        for (i in 1..maxStars) {
          when {
            i <= filledStars -> { // Draw a filled star
              Icon(
                  imageVector = Icons.Filled.Star,
                  contentDescription = null,
                  tint = MainColor,
                  modifier = Modifier.size(starSize).testTag(C.ViewReviewTags.FILLED_STAR))
            }
            i == filledStars + 1 && hasHalfStar -> { // Draw a half filled star
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.StarHalf,
                  contentDescription = null,
                  tint = MainColor,
                  modifier = Modifier.size(starSize).testTag(C.ViewReviewTags.HALF_STAR))
            }
            else -> { // Draw a star's border only (not filled)
              Icon(
                  imageVector = Icons.Default.StarBorder,
                  contentDescription = null,
                  tint = MainColor,
                  modifier = Modifier.size(starSize).testTag(C.ViewReviewTags.EMPTY_STAR))
            }
          }
        }
      }
}

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

// Composable to display upvote and downvote buttons with the net score.
@Composable
private fun VoteButtons(
    netScore: Int,
    userVote: VoteType,
    isOwner: Boolean,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        // Upvote button
        IconButton(
            onClick = onUpvote,
            enabled = !isOwner,
            modifier = Modifier.testTag(C.ViewReviewTags.VOTE_UPVOTE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ArrowUpward,
                  contentDescription = "Helpful",
                  tint =
                      if (userVote == VoteType.UPVOTE) {
                        MainColor
                      } else {
                        TextColor.copy(alpha = 0.6f)
                      },
                  modifier = Modifier.size(32.dp))
            }

        Spacer(Modifier.width(16.dp))

        // Net score display
        Text(
            text = netScore.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            modifier = Modifier.testTag(C.ViewReviewTags.VOTE_SCORE),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(Modifier.width(16.dp))

        // Downvote button
        IconButton(
            onClick = onDownvote,
            enabled = !isOwner,
            modifier = Modifier.testTag(C.ViewReviewTags.VOTE_DOWNVOTE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ArrowDownward,
                  contentDescription = "Not helpful",
                  tint =
                      if (userVote == VoteType.DOWNVOTE) {
                        MainColor
                      } else {
                        TextColor.copy(alpha = 0.6f)
                      },
                  modifier = Modifier.size(32.dp))
            }
      }
}

@Composable
@Preview
private fun ViewReviewScreenPreview() {
  ViewReviewScreen(reviewUid = "preview")
}
