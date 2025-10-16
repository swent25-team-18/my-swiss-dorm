import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.city.CityName
import com.android.mySwissDorm.model.map.Location
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.ui.listing.AddListingViewModel

@OptIn(ExperimentalMaterial3Api::class) val coralColor: Long = 0xFFFF6666

private val SIZE_REGEX = Regex("""^(?:0|[1-9]\d*)(?:\.\d*)?$""")
private val PRICE_INT_REGEX = Regex("""^(?:0|[1-9]\d*)$""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    addListingViewModel: AddListingViewModel = viewModel(),
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF0004),
    // onOpenMap: () -> Unit, // navigate to "drop a pin" screen
    onConfirm: (RentalListing) -> Unit, // called when form valid
    onBack: () -> Unit // navigate back
) {
  val listingUIState by addListingViewModel.uiState.collectAsState()
  // Validation and form submission moved to the ViewModel
  // val isFormValid = viewModel.isFormValid
  // val mapLat = viewModel.mapLat.value
  //  val mapLng = viewModel.mapLng.value

  // Remember scroll state
  val scrollState = rememberScrollState()

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Add Listing") },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = accentColor)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(coralColor)),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                  Text("Confirm listing", color = Color.White)
                }
            Spacer(Modifier.height(8.dp))
            if (!(listingUIState.isFormValid)) {
              Text(
                  "Please complete all required fields (valid size, price, and starting date).",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }) { padding ->
        // Wrap the content in a vertical scrollable column
        Column(
            modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState), // Make the column scrollable
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              OutlinedTextField(
                  value = listingUIState.title.take(30), // Limit to 30 characters
                  leadingIcon = { Icon(Icons.Default.Title, null, tint = Color(coralColor)) },
                  onValueChange = { addListingViewModel.setTitle(it) },
                  label = { Text("Listing title") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor),
                          unfocusedBorderColor = Color.Transparent,
                          focusedLabelColor = Color(coralColor),
                          unfocusedLabelColor = Color.Gray))

              ResidencyDropdown(
                  selected = listingUIState.residency.name,
                  onSelected = { addListingViewModel.setResidency(it) },
                  accentColor = accentColor)

              HousingTypeDropdown(
                  selected = listingUIState.housingType,
                  onSelected = { addListingViewModel.setHousingType(it) },
                  accentColor = accentColor)
              val isSizeInvalid =
                  listingUIState.sizeSqm.isNotBlank() && listingUIState.sizeSqm.toInt() >= 1000
              OutlinedTextField(
                  value = listingUIState.sizeSqm,
                  onValueChange = { raw ->
                    if (raw.isEmpty() || SIZE_REGEX.matches(raw)) {
                      addListingViewModel.setSizeSqm(raw)
                    }
                  },
                  label = { Text("Room size (m²)") },
                  isError = isSizeInvalid, // Show error state if invalid
                  singleLine = true,
                  keyboardOptions =
                      KeyboardOptions(
                          imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)),
                  colors =
                      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor),
                          unfocusedBorderColor = Color.Transparent,
                          focusedLabelColor = Color(coralColor),
                          unfocusedLabelColor = Color.Gray))

              if (isSizeInvalid) {
                Text(
                    text = "Please enter a valid number under 1000.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              val isPriceInvalid =
                  listingUIState.price.isNotBlank() &&
                      listingUIState.price.toDouble() >= 10000.0 &&
                      PRICE_INT_REGEX.matches(listingUIState.price)
              OutlinedTextField(
                  value = listingUIState.price.take(7),
                  onValueChange = { raw ->
                    if (raw.isEmpty() || PRICE_INT_REGEX.matches(raw)) {
                      addListingViewModel.setPrice(raw)
                    }
                  },
                  label = { Text("Monthly rent (CHF)") },
                  isError = isPriceInvalid, // Show error state if invalid
                  singleLine = true,
                  keyboardOptions =
                      KeyboardOptions(
                          imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)),
                  colors =
                      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor),
                          unfocusedBorderColor = Color.Transparent,
                          focusedLabelColor = Color(coralColor),
                          unfocusedLabelColor = Color.Gray))

              if (isPriceInvalid) {
                Text(
                    text = "Please enter a valid number under 10000.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              // Map button
              //              ElevatedButton(
              //                  onClick = onOpenMap,
              //                  modifier = Modifier.fillMaxWidth(),
              //                  colors =
              //                      ButtonDefaults.buttonColors(
              //                          containerColor = Color(0xFFF0F0F0), // Background color
              // (optional)
              //                          contentColor = Color(coralColor) // Text color (your
              // desired purple shade)
              //                          ),
              //                  shape = RoundedCornerShape(14.dp)) {
              //                    Icon(Icons.Default.Map, null, tint = Color(coralColor))
              //                    Spacer(Modifier.width(8.dp))
              //                    Text(
              //                        if (mapLat != null && mapLng != null) "Location selected
              // (tap to change)"
              //                        else "Add location on the map")
              //                  }

              OutlinedTextField(
                  maxLines = 6,
                  minLines = 6,
                  value = listingUIState.description.take(500),
                  onValueChange = { addListingViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = {
                    Text(
                        "Add a description of your listing (e.g. subletting, lease transfer or other information you find relevant)")
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .heightIn(min = 140.dp)
                          .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)),
                  colors =
                      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor),
                          unfocusedBorderColor = Color.Transparent,
                          focusedLabelColor = Color(coralColor),
                          unfocusedLabelColor = Color.Gray),
              )

              // Image picker button
              Text("Photos", style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                          // TODO: implement image selection
                        },
                        colors =
                            ButtonColors(
                                containerColor = Color(0xFFF0F0F0),
                                contentColor = Color(coralColor),
                                disabledContentColor = Color(coralColor),
                                disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(14.dp)) {
                          Icon(Icons.Default.AddAPhoto, null, tint = Color(coralColor))
                          Spacer(Modifier.width(8.dp))
                          Text("Add pictures")
                        }
                  }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingTypeDropdown(selected: RoomType?, onSelected: (RoomType) -> Unit, accentColor: Color) {
  var expanded by remember { mutableStateOf(false) }
  val label = selected.toString()

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        readOnly = true,
        label = { Text("Housing type") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        leadingIcon = { Icon(Icons.Default.Apartment, null, tint = Color(coralColor)) },
        colors =
            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(coralColor), // Focused outline color (Red)
                unfocusedBorderColor =
                    Color(coralColor), // Remove the default border when not focused
                focusedLabelColor = Color(coralColor), // Optional: Change label color when focused
                unfocusedLabelColor = Color.Gray // Optional: Change label color when not focused
                ),
        modifier = Modifier.menuAnchor().fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      RoomType.entries.forEach { type ->
        DropdownMenuItem(
            text = { Text(type.toString()) },
            onClick = {
              onSelected(type)
              expanded = false
            })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidencyDropdown(
    selected: ResidencyName?,
    onSelected: (Residency) -> Unit,
    accentColor: Color
) {
  var expanded by remember { mutableStateOf(false) }
  val label = selected?.toString() ?: "Select residency"

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        readOnly = true,
        label = { Text("Residency Name") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        leadingIcon = { Icon(Icons.Default.Home, null, tint = Color(coralColor)) },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(coralColor),
                unfocusedBorderColor = Color(coralColor),
                focusedLabelColor = Color(coralColor),
                unfocusedLabelColor = Color.Gray),
        modifier = Modifier.menuAnchor().fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ResidencyName.entries.forEach { type ->
        DropdownMenuItem(
            text = { Text(type.toString()) },
            onClick = {
              onSelected(
                  Residency(
                      name = type,
                      description = "",
                      location = Location(name = "", latitude = 0.0, longitude = 0.0),
                      city = CityName.LAUSANNE,
                      email = "",
                      phone = "",
                      website = null))
              expanded = false
            })
      }
    }
  }
}
