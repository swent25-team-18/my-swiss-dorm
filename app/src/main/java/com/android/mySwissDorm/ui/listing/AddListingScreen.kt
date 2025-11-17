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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.CustomDatePickerDialog
import com.android.mySwissDorm.ui.utils.CustomLocationDialog
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.ui.utils.onUserLocationClickFunc

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

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Add Listing") },
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
            val ui = listingUIState
            Button(
                onClick = { addListingViewModel.submitForm(onConfirm) },
                enabled = ui.isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = MainColor),
                modifier =
                    Modifier.fillMaxWidth()
                        .height(52.dp)
                        .testTag(C.AddListingScreenTags.CONFIRM_BUTTON),
                shape = RoundedCornerShape(16.dp)) {
                  Text("Confirm listing", color = Color.White)
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState)
                .testTag(C.AddListingScreenTags.ROOT),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              TitleField(
                  value = ui.title,
                  onValueChange = { addListingViewModel.setTitle(it) },
                  modifier = Modifier.testTag(C.AddListingScreenTags.TITLE_FIELD).fillMaxWidth())

              ResidencyDropdownResID(
                  selected = ui.residencyName,
                  onSelected = { addListingViewModel.setResidency(it) },
                  residencies = ui.residencies,
                  isListing = true,
                  accentColor = MainColor)

              HousingTypeDropdown(
                  selected = ui.housingType,
                  onSelected = { addListingViewModel.setHousingType(it) },
                  accentColor = MainColor)

              if (ui.residencyName == "Private Accommodation") {
                OutlinedButton(
                    onClick = { addListingViewModel.onCustomLocationClick(ui.customLocation) },
                    modifier =
                        Modifier.testTag(C.AddListingScreenTags.CUSTOM_LOCATION_BUTTON)
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor)) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically) {
                            Text("Location", color = TextColor)
                            Text(
                                ui.customLocation?.name ?: "Select location",
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
                    text = "Enter 1.0–1000.0 with one decimal (e.g., 18.5).",
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
                    text = "Enter an integer between 1 and 10000.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              // Start Date Field
              OutlinedButton(
                  onClick = { showDatePicker = true },
                  modifier =
                      Modifier.testTag(C.AddListingScreenTags.START_DATE_FIELD)
                          .fillMaxWidth()
                          .height(56.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                          Text("Start Date", color = TextColor)
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

              Text("Photos", style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultAddPhotoButton(onSelectPhoto = {}) // TODO display photo
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
