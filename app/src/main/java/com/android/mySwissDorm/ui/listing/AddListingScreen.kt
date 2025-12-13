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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.rental.RentalListing
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
import com.android.mySwissDorm.ui.listing.AddListingViewModel
import com.android.mySwissDorm.ui.photo.FullScreenImageViewer
import com.android.mySwissDorm.ui.photo.ImageGrid
import com.android.mySwissDorm.ui.theme.DarkGray
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.theme.White
import com.android.mySwissDorm.ui.utils.CustomDatePickerDialog
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.ui.utils.onUserLocationClickFunc
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    modifier: Modifier = Modifier,
    addListingViewModel: AddListingViewModel = viewModel(),
    onConfirm: (RentalListing) -> Unit,
    onBack: () -> Unit
) {
  val listingUIState by addListingViewModel.uiState.collectAsState()
  val scrollState = rememberScrollState()
  var showDatePicker by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val onUseCurrentLocationClick = onUserLocationClickFunc(context, addListingViewModel)
  if (listingUIState.showFullScreenImages) {
    FullScreenImageViewer(
        imageUris = listingUIState.pickedImages.map { it.image },
        onDismiss = { addListingViewModel.dismissFullScreenImages() },
        initialIndex = listingUIState.fullScreenImagesIndex)
    return
  }
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.add_listing_text)) },
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
        Surface(shadowElevation = Dimens.PaddingSmall) {
          Column(Modifier.padding(Dimens.PaddingDefault)) {
            val ui = listingUIState
            val isButtonEnabled =
                ui.isFormValid &&
                    !(FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true) &&
                    !ui.isSubmitting
            Button(
                onClick = { addListingViewModel.submitForm(onConfirm, context) },
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = MainColor),
                modifier =
                    Modifier.fillMaxWidth()
                        .height(Dimens.ButtonHeight)
                        .testTag(C.AddListingScreenTags.CONFIRM_BUTTON),
                shape = RoundedCornerShape(Dimens.CardCornerRadius)) {
                  if (ui.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.IconSizeDefault),
                        color = White,
                        strokeWidth = 2.dp)
                  } else {
                    Text(stringResource(R.string.confirm_listing), color = White)
                  }
                }
            Spacer(Modifier.height(Dimens.PaddingSmall))
            if (!ui.isFormValid || FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true) {
              Text(
                  stringResource(R.string.add_listing_invalid_form_text),
                  style = MaterialTheme.typography.bodySmall,
                  color = DarkGray,
                  modifier = Modifier.testTag(C.AddListingScreenTags.ERROR_MESSAGE))
            }
          }
        }
      }) { padding ->
        val ui = listingUIState

        // Inline error flags based on central validators (keeps UI dumb)
        val sizeInvalid =
            ui.sizeSqm.isNotBlank() &&
                !InputSanitizers.validateFinal<Double>(FieldType.RoomSize, ui.sizeSqm).isValid
        val priceInvalid =
            ui.price.isNotBlank() &&
                !InputSanitizers.validateFinal<Int>(FieldType.Price, ui.price).isValid
        Column(
            modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.PaddingDefault, vertical = Dimens.PaddingTopSmall)
                .verticalScroll(scrollState)
                .testTag(C.AddListingScreenTags.ROOT),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
              TitleField(
                  value = ui.title,
                  onValueChange = { addListingViewModel.setTitle(it) },
                  modifier = Modifier.testTag(C.AddListingScreenTags.TITLE_FIELD).fillMaxWidth())

              ResidencyDropdownResID(
                  selected = ui.residencyName,
                  onSelected = { addListingViewModel.setResidency(it) },
                  residencies = ui.residencies,
                  isListing = true,
                  accentColor = MainColor,
                  modifier = Modifier.testTag(C.AddListingScreenTags.RESIDENCY_DROPDOWN))

              HousingTypeDropdown(
                  selected = ui.housingType.getName(context),
                  onSelected = { addListingViewModel.setHousingType(it) },
                  accentColor = MainColor)

              if (ui.residencyName == "Private Accommodation") {
                OutlinedButton(
                    onClick = { addListingViewModel.onCustomLocationClick(ui.customLocation) },
                    modifier =
                        Modifier.testTag(C.AddListingScreenTags.CUSTOM_LOCATION_BUTTON)
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightLarge),
                    shape = RoundedCornerShape(Dimens.CardCornerRadius),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor)) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.location), color = TextColor)
                            Text(
                                ui.customLocation?.name
                                    ?: stringResource(R.string.add_listing_select_location),
                                color = TextColor,
                                style = MaterialTheme.typography.bodyMedium)
                          }
                    }
              }

              // Inline validations -> map to external error keys for uniform error text
              val sizeErrKey =
                  ui.sizeSqm
                      .takeIf { it.isNotBlank() }
                      ?.let {
                        val r = InputSanitizers.validateFinal<Double>(FieldType.RoomSize, it)
                        if (r.isValid) null else r.errorKey
                      }

              val priceErrKey =
                  ui.price
                      .takeIf { it.isNotBlank() }
                      ?.let {
                        val r = InputSanitizers.validateFinal<Int>(FieldType.Price, it)
                        if (r.isValid) null else r.errorKey
                      }

              RoomSizeField(
                  value = ui.sizeSqm,
                  onValueChange = { addListingViewModel.setSizeSqm(it) },
                  externalErrorKey = sizeErrKey,
                  modifier = Modifier.testTag(C.AddListingScreenTags.SIZE_FIELD).fillMaxWidth())

              if (sizeInvalid) {
                Text(
                    text = stringResource(R.string.add_listing_invalid_size_text),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              PriceField(
                  value = ui.price,
                  onValueChange = { addListingViewModel.setPrice(it) },
                  externalErrorKey = priceErrKey,
                  modifier = Modifier.testTag(C.AddListingScreenTags.PRICE_FIELD).fillMaxWidth())

              if (priceInvalid) {
                Text(
                    text = stringResource(R.string.add_listing_invalid_price_text),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              // Start Date Field
              OutlinedButton(
                  onClick = { showDatePicker = true },
                  modifier =
                      Modifier.testTag(C.AddListingScreenTags.START_DATE_FIELD)
                          .fillMaxWidth()
                          .height(Dimens.ButtonHeightLarge),
                  shape = RoundedCornerShape(Dimens.CardCornerRadius),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                          Text(stringResource(R.string.start_date), color = TextColor)
                          Text(
                              formatDate(ui.startDate),
                              color = TextColor,
                              style = MaterialTheme.typography.bodyMedium)
                        }
                  }

              DescriptionField(
                  value = ui.description,
                  onValueChange = { addListingViewModel.setDescription(it) },
                  maxLines = 6,
                  modifier = Modifier.testTag(C.AddListingScreenTags.DESC_FIELD).fillMaxWidth())

              Text(stringResource(R.string.photos), style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingTopSmall)) {
                    DefaultAddPhotoButton(
                        onSelectPhoto = { addListingViewModel.addPhoto(it) }, multiplePick = true)
                    ImageGrid(
                        imageUris = ui.pickedImages.map { it.image }.toSet(),
                        isEditingMode = true,
                        onRemove = { addListingViewModel.removePhoto(it, true) },
                        onImageClick = { addListingViewModel.onClickImage(it) },
                        modifier = Modifier.testTag(C.AddReviewTags.PHOTOS))
                  }
              if (ui.requireAtLeastOnePhoto && ui.pickedImages.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_listing_photo_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }
            }

        // Date Picker Dialog
        CustomDatePickerDialog(
            showDialog = showDatePicker,
            initialDate = ui.startDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp -> addListingViewModel.setStartDate(timestamp) })

        // Custom Location Dialog
        if (ui.showCustomLocationDialog) {
          val onValueChange =
              remember<(String) -> Unit> {
                { query -> addListingViewModel.setCustomLocationQuery(query) }
              }
          val onDropDownLocationSelect =
              remember<(com.android.mySwissDorm.model.map.Location) -> Unit> {
                { location -> addListingViewModel.setCustomLocation(location) }
              }
          val onDismiss = remember { { addListingViewModel.dismissCustomLocationDialog() } }
          val onConfirm =
              remember<(com.android.mySwissDorm.model.map.Location) -> Unit> {
                { location ->
                  addListingViewModel.setCustomLocation(location)
                  addListingViewModel.dismissCustomLocationDialog()
                }
              }

          CustomLocationDialog(
              value = ui.customLocationQuery,
              currentLocation = ui.customLocation,
              locationSuggestions = ui.locationSuggestions,
              onValueChange = onValueChange,
              onDropDownLocationSelect = onDropDownLocationSelect,
              onDismiss = onDismiss,
              onConfirm = onConfirm,
              onUseCurrentLocationClick = onUseCurrentLocationClick)
        }
      }
}
