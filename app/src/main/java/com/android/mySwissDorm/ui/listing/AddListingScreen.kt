// ⬇️ NEW: central sanitizers
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.model.rental.RentalListing
import com.android.mySwissDorm.ui.DescriptionField
import com.android.mySwissDorm.ui.HousingTypeDropdown
import com.android.mySwissDorm.ui.InputSanitizers
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.android.mySwissDorm.ui.PriceField
import com.android.mySwissDorm.ui.ResidencyDropdown
import com.android.mySwissDorm.ui.RoomSizeField
import com.android.mySwissDorm.ui.TitleField
import com.android.mySwissDorm.ui.listing.AddListingViewModel
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor

@OptIn(ExperimentalMaterial3Api::class) val coralColor: Long = 0xFFFF6666

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    addListingViewModel: AddListingViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onOpenMap: () -> Unit,
    onConfirm: (RentalListing) -> Unit,
    onBack: () -> Unit
) {
  val listingUIState by addListingViewModel.uiState.collectAsState()
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              TitleField(
                  value = ui.title,
                  onValueChange = { addListingViewModel.setTitle(it) },
                  modifier = Modifier.testTag("titleField").fillMaxWidth())

              ResidencyDropdown(
                  selected = ui.residency.name,
                  onSelected = { addListingViewModel.setResidency(it) },
                  residencies = ui.residencies,
                  accentColor = MainColor)

              HousingTypeDropdown(
                  selected = ui.housingType,
                  onSelected = { addListingViewModel.setHousingType(it) },
                  accentColor = MainColor)

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
                  modifier = Modifier.testTag("sizeField").fillMaxWidth())

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
                  modifier = Modifier.testTag("priceField").fillMaxWidth())

              if (priceInvalid) {
                Text(
                    text = "Enter an integer between 1 and 10000.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              }

              DescriptionField(
                  value = ui.description,
                  onValueChange = { addListingViewModel.setDescription(it) },
                  maxLines = 6,
                  modifier = Modifier.testTag("descField").fillMaxWidth())

              Text("Photos", style = MaterialTheme.typography.titleMedium)
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { /* TODO: implement image selection */},
                        colors =
                            ButtonColors(
                                containerColor = TextBoxColor,
                                contentColor = MainColor,
                                disabledContentColor = TextBoxColor,
                                disabledContainerColor = TextBoxColor),
                        shape = RoundedCornerShape(14.dp)) {
                          Icon(Icons.Default.AddAPhoto, null, tint = MainColor)
                          Spacer(Modifier.width(8.dp))
                          Text("Add pictures")
                        }
                  }
            }
      }
}
