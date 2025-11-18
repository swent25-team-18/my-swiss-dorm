package com.android.mySwissDorm.ui.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.DefaultAddPhotoButton
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
import com.android.mySwissDorm.ui.utils.StarRatingBar

/**
 * Edit screen for an existing review.
 *
 * All documentation was made with the help of AI
 *
 * This composable provides a form interface for editing an existing review. The ViewModel
 * automatically loads the review data on initialization, displays all review fields in a validated
 * form bound to [EditReviewViewModel.uiState], and handles saving or deleting the review.
 *
 * Responsibilities:
 * - Displays a validated form bound to [EditReviewViewModel.uiState] with fields for title,
 *   residency, room type, price, area, review text, and rating.
 * - Exposes three navigation callbacks to the host:
 *     - [onBack]: Invoked by the top-left back arrow and the "Cancel" button.
 *     - [onConfirm]: Invoked after a successful save (only when the form is valid).
 *     - [onDelete]: Invoked after deletion.
 *
 * Notes:
 * - The ViewModel automatically loads the review data on initialization via its init block.
 * - Input fields are normalized and validated using centralized [InputSanitizers].
 * - Error helper texts under Size/Price are shown only when the user has typed something invalid.
 * - The rating is displayed as an interactive star rating bar that supports half-star ratings.
 * - Photo upload functionality is not yet implemented.
 *
 * @param modifier Standard Compose [Modifier] for styling the screen.
 * @param onConfirm Called after a successful save of the edited review.
 * @param onBack Called when the user wants to cancel editing (back arrow or Cancel button).
 * @param reviewID The unique identifier of the review to edit. Used to create the ViewModel.
 * @param onDelete Called after deletion.
 * @param editReviewViewModel ViewModel that owns the edit state and repository calls. Created
 *   automatically with the [reviewID] parameter, but can be provided for testing purposes.
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

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.edit_review_title)) },
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
                        Text(stringResource(R.string.cancel))
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
                        Text(stringResource(R.string.save), color = Color.White)
                      }
                }
            Spacer(Modifier.height(8.dp))
            if (!ui.isFormValid) {
              Text(
                  stringResource(R.string.edit_review_invalid_form_text),
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
                    text = stringResource(R.string.edit_review_invalid_size_text),
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
                    text = stringResource(R.string.edit_review_invalid_price_text),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              DescriptionField(
                  value = ui.reviewText,
                  onValueChange = { editReviewViewModel.setReviewText(it) },
                  label = stringResource(R.string.review),
                  modifier = Modifier.testTag("descField").fillMaxWidth())
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.edit_review_rating),
                        color = TextColor,
                        style = MaterialTheme.typography.titleMedium)
                    StarRatingBar(
                        rating = ui.grade,
                        onRatingChange = { editReviewViewModel.setGrade(it) },
                        activeColor = MainColor,
                        inactiveColor = TextBoxColor,
                        modifier = Modifier.testTag("gradeField"))
                  }

              Text(stringResource(R.string.photos), style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultAddPhotoButton(onSelectPhoto = {}) // TODO display the photo
              }
            }
      }
}
