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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.review.Review
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
            title = { Text("Add Review") },
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                  Text("Submit Review", color = Color.White)
                }
            Spacer(Modifier.height(8.dp))
            if (!ui.isFormValid) {
              Text(
                  "Please complete all required fields (valid grade, size, price, etc.).",
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
                  modifier = Modifier.testTag("reviewTitleField").fillMaxWidth())
              ResidencyDropdownResID(
                  selected = ui.residencyName,
                  onSelected = { addReviewViewModel.setResidencyName(it) },
                  residencies = ui.residencies,
                  accentColor = MainColor,
                  modifier = Modifier.testTag("residencyDropdown").fillMaxWidth())

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
                  onValueChange = { addReviewViewModel.setPricePerMonth(it) },
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
                  onValueChange = { addReviewViewModel.setReviewText(it) },
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
                        onRatingChange = { addReviewViewModel.setGrade(it) },
                        activeColor = MainColor,
                        inactiveColor = TextBoxColor,
                        modifier = Modifier.testTag("gradeField"))
                  }

              Text("Photos", style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultAddPhotoButton(onSelectPhoto = {}) // TODO display the photo
              }
            }
      }
}
