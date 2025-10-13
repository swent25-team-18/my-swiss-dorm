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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.ui.listing.AddListingViewModel

@OptIn(ExperimentalMaterial3Api::class) val coralColor: Long = 0xFFFF6666

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF0004), // Red color for buttons and arrow
    onOpenMap: () -> Unit, // navigate to "drop a pin" screen
    onConfirm: (RentalListing) -> Unit, // called when form valid
    onBack: () -> Unit //navigate back
) {
  val viewModel: AddListingViewModel = viewModel()

  // Validation and form submission moved to the ViewModel
  val isFormValid = viewModel.isFormValid
  val mapLat = viewModel.mapLat.value
  val mapLng = viewModel.mapLng.value

  // Remember scroll state
  val scrollState = rememberScrollState()

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Add Listing") },
            navigationIcon = {
              IconButton(onClick = { onBack }) {
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
            Button(
                onClick = { viewModel.submitForm(onConfirm) },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(coralColor)),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                  Text("Confirm listing", color = Color.White)
                }
            Spacer(Modifier.height(8.dp))
            if (!isFormValid) {
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
                  value = viewModel.title.value,
                  leadingIcon = { Icon(Icons.Default.Title, null, tint = Color(coralColor)) },
                  onValueChange = { viewModel.title.value = it },
                  label = { Text("Listing title") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              Color(0xFFF0F0F0),
                              RoundedCornerShape(10.dp)), // Gray with rounded edges
                  colors =
                      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor), // Focused outline color (Red)
                          unfocusedBorderColor =
                              Color.Transparent, // Remove the default border when not focused
                          focusedLabelColor =
                              Color(coralColor), // Optional: Change label color when focused
                          unfocusedLabelColor =
                              Color.Gray // Optional: Change label color when not focused
                          ))

              ResidencyDropdown(
                  selected = viewModel.residency.value,
                  onSelected = { viewModel.residency.value = it},
                  accentColor = accentColor
              )

              HousingTypeDropdown(
                  selected = viewModel.housingType.value,
                  onSelected = { viewModel.housingType.value = it },
                  accentColor = accentColor)


              val isSizeValid = viewModel.sizeSqm.value.toDoubleOrNull() != null
              OutlinedTextField(
                  value = viewModel.sizeSqm.value,
                  onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^(?:0|[1-9]\d*)(?:[.,]\d*)?$"""))) {
                      viewModel.sizeSqm.value = input
                    }
                  },
                  label = { Text("Room size (m²)") },
                  isError = !isSizeValid, // Show error state if invalid
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

              if (!isSizeValid) {
                Text(
                    text = "Please enter a valid number.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }


            val isPriceValid = viewModel.price.value.toDoubleOrNull() != null
            OutlinedTextField(
                value = viewModel.price.value,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^(?:0|[1-9]\\d*)\$"))) {
                        viewModel.price.value = input
                    }
                },
                label = { Text("Monthly rent (CHF)") },
                isError = !isPriceValid, // Show error state if invalid
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

            if (!isPriceValid) {
                Text(
                    text = "Please enter a valid number.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

              // Map button
//              ElevatedButton(
//                  onClick = onOpenMap,
//                  modifier = Modifier.fillMaxWidth(),
//                  colors =
//                      ButtonDefaults.buttonColors(
//                          containerColor = Color(0xFFF0F0F0), // Background color (optional)
//                          contentColor = Color(coralColor) // Text color (your desired purple shade)
//                          ),
//                  shape = RoundedCornerShape(14.dp)) {
//                    Icon(Icons.Default.Map, null, tint = Color(coralColor))
//                    Spacer(Modifier.width(8.dp))
//                    Text(
//                        if (mapLat != null && mapLng != null) "Location selected (tap to change)"
//                        else "Add location on the map")
//                  }



              OutlinedTextField(
                  value = viewModel.description.value,
                  onValueChange = { viewModel.description.value = it },
                  label = { Text("Description") },
                  placeholder = {
                    Text(
                        "Add a description of your listing (e.g. subletting, lease transfer or other information you find relevant)")
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .heightIn(min = 140.dp)
                          .background(
                              Color(0xFFF0F0F0),
                              RoundedCornerShape(10.dp)), // Gray with rounded edges
                  colors =
                      androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color(coralColor), // Focused outline color (Red)
                          unfocusedBorderColor =
                              Color.Transparent, // Remove the default border when not focused
                          focusedLabelColor =
                              Color(coralColor), // Optional: Change label color when focused
                          unfocusedLabelColor =
                              Color.Gray // Optional: Change label color when not focused
                          ),
                  maxLines = 6)

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
                            androidx.compose.material3.ButtonColors(
                                containerColor = Color(0xFFF0F0F0), // Focused outline color (Red)
                                contentColor =
                                    Color(coralColor), // Remove the default border when not focused
                                disabledContentColor =
                                    Color(coralColor), // Optional: Change label color when focused
                                disabledContainerColor =
                                    Color.Gray // Optional: Change label color when not focused
                                ),
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
  val label = selected?.toString() ?: "Select housing type"

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
fun ResidencyDropdown(selected: ResidencyName?, onSelected: (ResidencyName) -> Unit, accentColor: Color) {
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
            ResidencyName.entries.forEach { type ->
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

@Preview
@Composable
fun AddListingScreenPreview(){
    AddListingScreen(
        onBack = {},
        onConfirm = {},
        onOpenMap = {}
    )
}


