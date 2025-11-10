package com.android.mySwissDorm.ui.review

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.DescriptionField
import com.android.mySwissDorm.ui.HousingTypeDropdown
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.android.mySwissDorm.ui.PriceField
import com.android.mySwissDorm.ui.ResidencyDropdownResID
import com.android.mySwissDorm.ui.RoomSizeField
import com.android.mySwissDorm.ui.TitleField
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import kotlin.math.roundToInt

/**
 * Edit screen for an existing review.
 *
 * All documentation was made with the help of AI
 *
 * This composable provides a form interface for editing an existing review. It loads the review
 * data when first composed or when [reviewID] changes, displays all review fields in a validated
 * form bound to [EditReviewViewModel.uiState], and handles saving or deleting the review.
 *
 * Responsibilities:
 * - Loads the target review (via [EditReviewViewModel.getReview]) when the composable is first
 *   composed or when [reviewID] changes.
 * - Displays a validated form bound to [EditReviewViewModel.uiState] with fields for title,
 *   residency, room type, price, area, review text, and rating.
 * - Exposes three navigation callbacks to the host:
 *     - [onBack]: Invoked by the top-left back arrow and the "Cancel" button.
 *     - [onConfirm]: Invoked after a successful save (only when the form is valid).
 *     - [onDelete]: Invoked after deletion; receives the residency name of the review to let the
 *       host decide navigation.
 *
 * Notes:
 * - Input fields are normalized and validated using centralized [InputSanitizers].
 * - Error helper texts under Size/Price are shown only when the user has typed something invalid.
 * - The rating is displayed as an interactive star rating bar that supports half-star ratings.
 * - Photo upload functionality is not yet implemented.
 *
 * @param modifier Standard Compose [Modifier] for styling the screen.
 * @param editReviewViewModel ViewModel that owns the edit state and repository calls. Defaults to a
 *   viewModel instance scoped to the composition.
 * @param onConfirm Called after a successful save of the edited review.
 * @param onBack Called when the user wants to cancel editing (back arrow or Cancel button).
 * @param reviewID The unique identifier of the review to edit.
 * @param onDelete Called after deletion with the residency name (used by host for navigation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReviewScreen(
    modifier: Modifier = Modifier,
    editReviewViewModel: EditReviewViewModel = viewModel(),
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    reviewID: String,
    onDelete: (String) -> Unit,
) {
  val editReviewUIState by editReviewViewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  LaunchedEffect(reviewID) { editReviewViewModel.getReview(reviewID) }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Edit Review") },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            },
            actions = {
              IconButton(
                  onClick = {
                    editReviewViewModel.deleteReview(reviewID)
                    onDelete(editReviewUIState.residencyName)
                  },
                  modifier = Modifier.testTag("deleteButton") // ← add this
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MainColor)
                  }
            })
      },
      bottomBar = {
        Surface(shadowElevation = 8.dp) {
          Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ui = editReviewUIState
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                  Button(
                      onClick = onBack,
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = TextBoxColor, contentColor = MainColor),
                      modifier = Modifier.weight(1f).height(52.dp),
                      shape = RoundedCornerShape(16.dp)) {
                        Text("Cancel")
                      }

                  Button(
                      onClick = { if (editReviewViewModel.editReview(reviewID)) onConfirm() },
                      enabled = ui.isFormValid,
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor,
                              disabledContainerColor = MainColor.copy(alpha = 0.3f)),
                      modifier =
                          Modifier.weight(1f).height(52.dp).testTag("saveButton"), // ← add this
                      shape = RoundedCornerShape(16.dp)) {
                        Text("Save", color = Color.White)
                      }
                }
            Spacer(Modifier.height(8.dp))
            if (!ui.isFormValid) {
              Text(
                  "Please complete all required fields (valid size, price, and starting date).",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }) { padding ->
        val ui = editReviewUIState

        val sizeInvalid =
            ui.areaInM2.isNotBlank() &&
                !InputSanitizers.validateFinal<Double>(FieldType.RoomSize, ui.areaInM2).isValid
        val priceInvalid =
            ui.pricePerMonth.isNotBlank() &&
                !InputSanitizers.validateFinal<Int>(FieldType.Price, ui.pricePerMonth).isValid

        Column(
            modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              TitleField(
                  value = ui.title,
                  onValueChange = { editReviewViewModel.setTitle(it) },
                  modifier = Modifier.testTag("reviewTitleField").fillMaxWidth())
              ResidencyDropdownResID(
                  selected = ui.residencyName,
                  onSelected = { editReviewViewModel.setResidencyName(it) },
                  residencies = ui.residencies,
                  accentColor = MainColor,
                  modifier = Modifier.testTag("residencyDropdown").fillMaxWidth())

              HousingTypeDropdown(
                  selected = ui.roomType,
                  onSelected = { editReviewViewModel.setRoomType(it) },
                  accentColor = MainColor)

              val sizeErrKey =
                  ui.areaInM2
                      .takeIf { it.isNotBlank() }
                      ?.let {
                        val r = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, it)
                        if (r.isValid) null else r.errorKey
                      }
              RoomSizeField(
                  value = ui.areaInM2,
                  onValueChange = { editReviewViewModel.setAreaInM2(it) },
                  externalErrorKey = sizeErrKey,
                  modifier = Modifier.testTag("sizeField").fillMaxWidth())
              if (sizeInvalid) {
                Text(
                    text = "Enter 1.0–1000.0 with one decimal (e.g., 18.5).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              val priceErrKey =
                  ui.pricePerMonth
                      .takeIf { it.isNotBlank() }
                      ?.let {
                        val r = InputSanitizers.validateFinal<Int>(FieldType.Price, it)
                        if (r.isValid) null else r.errorKey
                      }
              PriceField(
                  value = ui.pricePerMonth,
                  onValueChange = { editReviewViewModel.setPricePerMonth(it) },
                  externalErrorKey = priceErrKey,
                  modifier = Modifier.testTag("priceField").fillMaxWidth())
              if (priceInvalid) {
                Text(
                    text = "Enter an integer between 1 and 10000.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              DescriptionField(
                  value = ui.reviewText,
                  onValueChange = { editReviewViewModel.setReviewText(it) },
                  label = "Review",
                  modifier = Modifier.testTag("descField").fillMaxWidth())
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rating: ",
                        color = TextColor,
                        style = MaterialTheme.typography.titleMedium)
                    StarRatingBar(
                        rating = ui.grade,
                        onRatingChange = { editReviewViewModel.setGrade(it) },
                        activeColor = MainColor,
                        inactiveColor = TextBoxColor,
                        modifier = Modifier.testTag("gradeField"))
                  }

              Text("Photos", style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { /* not yet implemented */},
                        colors =
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = TextBoxColor, contentColor = MainColor),
                        shape = RoundedCornerShape(14.dp)) {
                          Icon(Icons.Default.AddAPhoto, null, tint = MainColor)
                          Spacer(Modifier.width(8.dp))
                          Text("Add pictures")
                        }
                  }
            }
      }
}

/**
 * A custom, dependency-free Composable for star ratings.
 *
 * This component displays a 5-star rating bar that supports both full and half-star ratings. Users
 * can tap anywhere on the bar to set a rating, and the component will calculate the appropriate
 * star value (0.5, 1.0, 1.5, ..., 5.0) based on the tap position.
 *
 * The rating is visually displayed using filled stars, half-filled stars, and outlined stars to
 * represent the current rating value. The minimum rating that can be set is 0.5.
 *
 * @param modifier Standard Compose [Modifier] for styling the rating bar.
 * @param rating The current rating value (0.0 to 5.0, in 0.5 increments).
 * @param onRatingChange Callback invoked when the user taps to change the rating.
 * @param activeColor The color for filled and half-filled stars.
 * @param inactiveColor The color for empty/outlined stars.
 */
@Composable
private fun StarRatingBar(
    modifier: Modifier = Modifier,
    rating: Double,
    onRatingChange: (Double) -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
  var rowSize by remember { mutableStateOf(IntSize.Zero) }
  Row(
      modifier =
          modifier
              .width(160.dp)
              .onSizeChanged { rowSize = it }
              .pointerInput(Unit) {
                detectTapGestures { offset ->
                  val widthInPx = rowSize.width.toFloat()
                  if (widthInPx <= 0) return@detectTapGestures
                  val xFraction = (offset.x / widthInPx).coerceIn(0f, 1f)
                  val rawRating = xFraction * 5
                  val newRating = (rawRating * 2).roundToInt() / 2.0
                  onRatingChange(newRating.coerceAtLeast(0.5))
                }
              }) {
        // For the rating stars
        for (i in 1..5) {
          val icon =
              when {
                i <= rating -> Icons.Filled.Star
                i - 0.5 <= rating -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
              }
          Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (i - 0.5 <= rating) activeColor else inactiveColor,
              modifier = Modifier.weight(1f).height(30.dp))
        }
      }
}
