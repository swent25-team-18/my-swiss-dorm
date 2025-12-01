package com.android.mySwissDorm.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.review.VoteType
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.LightGray
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsByResidencyScreen(
    reviewsByResidencyViewModel: ReviewsByResidencyViewModel = viewModel(),
    residencyName: String,
    onSelectReview: (ReviewCardUI) -> Unit = {},
    onGoBack: () -> Unit = {},
) {
  val context = LocalContext.current

  LaunchedEffect(residencyName) { reviewsByResidencyViewModel.loadReviews(residencyName, context) }

  val uiState by reviewsByResidencyViewModel.uiState.collectAsState()

  val reviewsState = uiState.reviews

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = residencyName,
                  color = MainColor,
                  modifier = Modifier.testTag(C.ReviewsByResidencyTag.TOP_BAR_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = { onGoBack() },
                  modifier = Modifier.testTag(C.ReviewsByResidencyTag.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MainColor)
                  }
            })
      }) { pd ->
        Column(
            modifier = Modifier.padding(pd).fillMaxSize().testTag(C.ReviewsByResidencyTag.ROOT)) {
              when {
                reviewsState.loading -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp).testTag(C.ReviewsByResidencyTag.LOADING))
                  }
                }
                reviewsState.error != null -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = reviewsState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(C.ReviewsByResidencyTag.ERROR))
                  }
                }
                reviewsState.items.isEmpty() -> {
                  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.reviews_by_residency_no_reviews_yet),
                        color = TextColor,
                        modifier = Modifier.testTag(C.ReviewsByResidencyTag.EMPTY))
                  }
                }
                else -> {
                  LazyColumn(
                      modifier =
                          Modifier.fillMaxSize()
                              .padding(horizontal = 16.dp)
                              .testTag(C.ReviewsByResidencyTag.REVIEW_LIST),
                      contentPadding = PaddingValues(vertical = 12.dp),
                      verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(reviewsState.items) { item ->
                          ReviewCard(
                              item,
                              onSelectReview,
                              onUpvote = {
                                reviewsByResidencyViewModel.upvoteReview(item.reviewUid, context)
                              },
                              onDownvote = {
                                reviewsByResidencyViewModel.downvoteReview(item.reviewUid, context)
                              })
                        }
                      }
                }
              }
            }
      }
}

@Composable
private fun ReviewCard(
    data: ReviewCardUI,
    onClick: (ReviewCardUI) -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
  OutlinedCard(
      shape = RoundedCornerShape(16.dp),
      modifier =
          Modifier.fillMaxWidth().testTag(C.ReviewsByResidencyTag.reviewCard(data.reviewUid))) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick(data) },
            verticalAlignment = Alignment.CenterVertically) {
              // Image placeholder (left)
              Box(
                  modifier =
                      Modifier.height(140.dp)
                          .fillMaxWidth(0.35F)
                          .clip(RoundedCornerShape(12.dp))
                          .background(LightGray)
                          .testTag(
                              C.ReviewsByResidencyTag.reviewImagePlaceholder(data.reviewUid))) {
                    AsyncImage(
                        model = data.image,
                        contentDescription = null,
                        modifier =
                            Modifier.fillMaxSize()
                                .testTag(C.ReviewsByResidencyTag.reviewPhoto(data.reviewUid)),
                        contentScale = ContentScale.Crop)
                  }

              Spacer(Modifier.width(4.dp))

              Column(modifier = Modifier.weight(1f).height(140.dp).padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      val truncatedTitle = truncateText(data.title, 20)
                      Text(
                          text = truncatedTitle,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.SemiBold,
                          textAlign = TextAlign.Start,
                          color = TextColor,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier =
                              Modifier.fillMaxWidth(0.6f)
                                  .testTag(C.ReviewsByResidencyTag.reviewTitle(data.reviewUid)))
                      DisplayGrade(
                          data.grade, 16.dp, C.ReviewsByResidencyTag.reviewGrade(data.reviewUid))
                    }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.SpaceBetween) { // Review: posted at, review text, posted by
                      Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                              Text(
                                  text = stringResource(R.string.reviews_by_residency_review),
                                  color = TextColor,
                              )
                              Text(
                                  text = data.postDate,
                                  style = MaterialTheme.typography.bodySmall,
                                  fontWeight = FontWeight.Light,
                                  color = TextColor,
                                  modifier =
                                      Modifier.testTag(
                                          C.ReviewsByResidencyTag.reviewPostDate(data.reviewUid)))
                            }
                        val truncatedReview = truncateText(data.reviewText, 60)
                        Text( // Review Text
                            text = truncatedReview,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                            color = TextColor,
                            maxLines = 2,
                            modifier =
                                Modifier.testTag(
                                    C.ReviewsByResidencyTag.reviewDescription(data.reviewUid)))
                      }
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text =
                                    "${stringResource(R.string.reviews_by_residency_posted_by)} ${data.fullNameOfPoster}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Light,
                                color = TextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier.weight(1f, fill = false)
                                        .testTag(
                                            C.ReviewsByResidencyTag.reviewPosterName(
                                                data.reviewUid)))

                            // Vote buttons (always shown, but disabled for owner)
                            CompactVoteButtons(
                                netScore = data.netScore,
                                userVote = data.userVote,
                                isOwner = data.isOwner,
                                onUpvote = onUpvote,
                                onDownvote = onDownvote,
                                modifier =
                                    Modifier.testTag(
                                        C.ReviewsByResidencyTag.reviewVoteButtons(data.reviewUid)))
                          }
                    }
              }
            }
      }
}

// Compact version of vote buttons for use in review cards.
@Composable
private fun CompactVoteButtons(
    netScore: Int,
    userVote: VoteType,
    isOwner: Boolean,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Upvote button
        IconButton(
            onClick = onUpvote,
            enabled = !isOwner,
            modifier =
                Modifier.size(28.dp).testTag(C.ReviewsByResidencyTag.COMPACT_VOTE_UPVOTE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ArrowUpward,
                  contentDescription = "Helpful",
                  tint =
                      if (userVote == VoteType.UPVOTE) {
                        MainColor
                      } else {
                        TextColor.copy(alpha = 0.6f)
                      },
                  modifier = Modifier.size(18.dp))
            }

        // Net score display
        Text(
            text = netScore.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextColor,
            modifier = Modifier.testTag(C.ReviewsByResidencyTag.COMPACT_VOTE_SCORE),
            textAlign = TextAlign.Center)

        // Downvote button
        IconButton(
            onClick = onDownvote,
            enabled = !isOwner,
            modifier =
                Modifier.size(28.dp)
                    .testTag(C.ReviewsByResidencyTag.COMPACT_VOTE_DOWNVOTE_BUTTON)) {
              Icon(
                  imageVector = Icons.Filled.ArrowDownward,
                  contentDescription = "Not helpful",
                  tint =
                      if (userVote == VoteType.DOWNVOTE) {
                        MainColor
                      } else {
                        TextColor.copy(alpha = 0.6f)
                      },
                  modifier = Modifier.size(18.dp))
            }
      }
}

fun truncateText(text: String, maxLength: Int): String {
  return text.let {
    if (it.length <= maxLength) {
      it
    } else {
      val truncated = it.take(maxLength)
      val lastSpace = truncated.lastIndexOf(' ')
      if (lastSpace != -1) {
        "${truncated.substring(0, lastSpace)}..."
      } else {
        "$truncated..." // No space, probably unreadable text
      }
    }
  }
}
