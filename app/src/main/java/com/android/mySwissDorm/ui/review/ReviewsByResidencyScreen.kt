package com.android.mySwissDorm.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.Gray
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

  LaunchedEffect(residencyName) { reviewsByResidencyViewModel.loadReviews(residencyName) }

  val uiState by reviewsByResidencyViewModel.uiState.collectAsState()

  val reviewsState = uiState.reviews

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = residencyName,
                  color = MainColor,
              )
            },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { pd ->
        Column(modifier = Modifier.padding(pd).fillMaxSize()) {
          when {
            reviewsState.loading -> {
              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
              }
            }
            reviewsState.error != null -> {
              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = reviewsState.error,
                    color = MaterialTheme.colorScheme.error,
                )
              }
            }
            reviewsState.items.isEmpty() -> {
              Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No reviews yet.",
                    color = TextColor,
                )
              }
            }
            else -> {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                  contentPadding = PaddingValues(vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(reviewsState.items) { item -> ReviewCard(item, onSelectReview) }
                  }
            }
          }
        }
      }
}

@Composable
private fun ReviewCard(data: ReviewCardUI, onClick: (ReviewCardUI) -> Unit) {
  OutlinedCard(
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(),
      onClick = { onClick(data) }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          // Image placeholder (left)
          Box(
              modifier =
                  Modifier.height(140.dp)
                      .fillMaxWidth(0.35F)
                      .clip(RoundedCornerShape(12.dp))
                      .background(LightGray)) {
                Text(
                    "IMAGE",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray)
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
                      modifier = Modifier.fillMaxWidth(0.6f))
                  DisplayGrade(data.grade, 16.dp)
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
                              text = "Review :",
                              color = TextColor,
                          )
                          Text(
                              text = data.postDate,
                              style = MaterialTheme.typography.bodySmall,
                              fontWeight = FontWeight.Light,
                              color = TextColor,
                          )
                        }
                    val truncatedReview = truncateText(data.reviewText, 60)
                    Text( // Review Text
                        text = truncatedReview,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        color = TextColor,
                        maxLines = 2,
                    )
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Posted by ${data.fullNameOfPoster}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light,
                        color = TextColor,
                        maxLines = 1,
                    )
                  }
                }
          }
        }
      }
}

private fun truncateText(text: String, maxLength: Int): String {
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
