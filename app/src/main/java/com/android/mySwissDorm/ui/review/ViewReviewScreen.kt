package com.android.mySwissDorm.ui.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.map.MapPreview
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatRelative
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
    onViewMap: (latitude: Double, longitude: Double, title: String, name: String) -> Unit =
        { _, _, _, _ ->
        }
) {
  LaunchedEffect(reviewUid) { viewReviewViewModel.loadReview(reviewUid) }

  val uiState by viewReviewViewModel.uiState.collectAsState()
  val review = uiState.review
  val fullNameOfPoster = uiState.fullNameOfPoster
  val errorMsg = uiState.errorMsg //
  val isOwner = uiState.isOwner

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      onGoBack()
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewReviewViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Review Details") },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
              Text(
                  text = review.title,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.SemiBold,
                  lineHeight = 32.sp,
                  modifier = Modifier.testTag(C.ViewReviewTags.TITLE),
                  color = TextColor)

              // tag we'll look for
              val tagProfile = "PROFILE_ID"

              // build the AnnotatedString tagging the name
              val annotatedPostedByString = buildAnnotatedString {
                append("Posted by ")
                pushStringAnnotation(tag = tagProfile, annotation = review.ownerId)
                // apply the style to the name
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MainColor)) {
                  append(fullNameOfPoster)
                  if (isOwner) append(" (You)")
                }
                // stop tagging
                pop()
                append(" ${formatRelative(review.postedAt)}")
              }

              // remember the TextLayoutResult
              var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

              Text(
                  text = annotatedPostedByString,
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant),
                  onTextLayout = { textLayoutResult = it },
                  modifier =
                      Modifier.testTag(C.ViewReviewTags.POSTED_BY)
                          .then(
                              // Only make clickable if review is not anonymous (privacy)
                              if (!review.isAnonymous) {
                                Modifier.pointerInput(Unit) {
                                  detectTapGestures { pos ->
                                    val l = textLayoutResult ?: return@detectTapGestures
                                    val offset = l.getOffsetForPosition(pos)

                                    // find any annotations at that exact offset
                                    annotatedPostedByString
                                        .getStringAnnotations(start = offset, end = offset)
                                        .firstOrNull {
                                          it.tag == tagProfile
                                        } // Check if it's our tag
                                        ?.let { annotation ->
                                          // trigger the callback with the stored ownerId
                                          onViewProfile(annotation.item)
                                        }
                                  }
                                }
                              } else {
                                Modifier
                              }))

              // Bullet section
              SectionCard(modifier = Modifier.testTag(C.ViewReviewTags.BULLETS)) {
                BulletRow("${review.roomType}")
                BulletRow("${review.pricePerMonth}.-/month")
                BulletRow("${review.areaInM2}m²")
                DisplayGrade(review.grade, 24.dp)
              }

              // Actual review
              SectionCard(modifier = Modifier.testTag(C.ViewReviewTags.REVIEW_TEXT)) {
                Text("Review :", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(review.reviewText, style = MaterialTheme.typography.bodyLarge)
              }

              // Photos placeholder
              PlaceholderBlock(
                  text = "PHOTOS (Not implemented yet)",
                  height = 220.dp,
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
                      onViewMap(location.latitude, location.longitude, review.title, "Review")
                    })
              } else {
                PlaceholderBlock(
                    text = "LOCATION (Not available)",
                    height = 180.dp,
                    modifier = Modifier.testTag(C.ViewReviewTags.LOCATION))
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
                    Text("Edit", style = MaterialTheme.typography.titleMedium, color = TextColor)
                  }
                }
              }
            }
      })
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
    Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
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

@Composable
@Preview
private fun ViewReviewScreenPreview() {
  ViewReviewScreen(reviewUid = "preview")
}
