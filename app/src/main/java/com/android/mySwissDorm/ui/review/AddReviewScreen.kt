package com.android.mySwissDorm.ui.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.android.mySwissDorm.model.review.Review
import com.android.mySwissDorm.resources.C
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    modifier: Modifier = Modifier,
    addReviewViewModel: AddReviewViewModel = viewModel(),
    onConfirm: (Review) -> Unit,
    onBack: () -> Unit
) {
  val reviewUIState by addReviewViewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.add_review_title)) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      },
      bottomBar = {
        Surface(shadowElevation = 8.dp) {
          Column(Modifier.padding(16.dp)) {
            val ui = reviewUIState
            Button(
                onClick = { addReviewViewModel.submitReviewForm(onConfirm) },
                enabled = ui.isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = MainColor),
                modifier =
                    Modifier.fillMaxWidth().height(52.dp).testTag(C.AddReviewTags.SUBMIT_BUTTON),
                shape = RoundedCornerShape(16.dp)) {
                  Text(stringResource(R.string.add_review_submit), color = Color.White)
                }
            Spacer(Modifier.height(8.dp))
            if (!ui.isFormValid) {
              Text(
                  stringResource(R.string.add_review_invalid_form_text),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }) { padding ->
        val ui = reviewUIState

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
                  onValueChange = { addReviewViewModel.setTitle(it) },
                  modifier = Modifier.testTag(C.AddReviewTags.TITLE_FIELD).fillMaxWidth())
              ResidencyDropdownResID(
                  selected = ui.residencyName,
                  onSelected = { addReviewViewModel.setResidencyName(it) },
                  residencies = ui.residencies,
                  accentColor = MainColor,
                  isListing = false,
                  modifier = Modifier.testTag(C.AddReviewTags.RESIDENCY_DROPDOWN).fillMaxWidth())

              HousingTypeDropdown(
                  selected = ui.roomType,
                  onSelected = { addReviewViewModel.setRoomType(it) },
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
                  onValueChange = { addReviewViewModel.setAreaInM2(it) },
                  externalErrorKey = sizeErrKey,
                  modifier = Modifier.testTag(C.AddReviewTags.SIZE_FIELD).fillMaxWidth())
              if (sizeInvalid) {
                Text(
                    text = stringResource(R.string.add_review_invalid_size_text),
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
                  onValueChange = { addReviewViewModel.setPricePerMonth(it) },
                  externalErrorKey = priceErrKey,
                  modifier = Modifier.testTag(C.AddReviewTags.PRICE_FIELD).fillMaxWidth())
              if (priceInvalid) {
                Text(
                    text = stringResource(R.string.add_review_invalid_price_text),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              DescriptionField(
                  value = ui.reviewText,
                  onValueChange = { addReviewViewModel.setReviewText(it) },
                  label = stringResource(R.string.review),
                  modifier = Modifier.testTag(C.AddReviewTags.DESC_FIELD).fillMaxWidth())
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.add_review_rating),
                        color = TextColor,
                        style = MaterialTheme.typography.titleMedium)
                    StarRatingBar(
                        rating = ui.grade,
                        onRatingChange = { addReviewViewModel.setGrade(it) },
                        activeColor = MainColor,
                        inactiveColor = TextBoxColor,
                        modifier = Modifier.testTag(C.AddReviewTags.GRADE_FIELD))
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
