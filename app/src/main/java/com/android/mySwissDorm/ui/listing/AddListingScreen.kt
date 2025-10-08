@file:OptIn(ExperimentalMaterial3Api::class)
package com.android.mySwissDorm.ui.listing

import androidx.compose.material3.ExperimentalMaterial3Api

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.TextFieldDefaults

/**
 * Domain types
 */
enum class HousingType(val label: String) {
    ROOM_IN_SHARED_APT("Room in a shared apartment"),
    STUDIO("Studio"),
    INDEPENDENT_ROOM("Independent room"),
    APARTMENT("Apartment"),
    HOUSE("House");
}

data class ListingForm(
    val title: String,
    val residencyName: String,
    val housingType: HousingType?,
    val roommates: Int?,             // only when shared apartment
    val roomSizeSqm: Double?,        // m²
    val mapLat: Double?,             // fill after map screen
    val mapLng: Double?,
    val description: String,
    val imageUris: List<Uri>
)

/**
 * Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF0004), // Red color for buttons and arrow
    onOpenMap: () -> Unit,                  // navigate to "drop a pin" screen
    onConfirm: (ListingForm) -> Unit        // called when form valid
) {
    val focus = LocalFocusManager.current
    val scroll = rememberScrollState()

    var title by rememberSaveable { mutableStateOf("") }
    var residency by rememberSaveable { mutableStateOf("") }
    var housingType by rememberSaveable { mutableStateOf<HousingType?>(null) }
    var roommates by rememberSaveable { mutableStateOf("") }
    var sizeSqm by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var pickedImages by rememberSaveable(stateSaver = uriListSaver()) { mutableStateOf(emptyList()) }

    // Map coords would be filled when you return from the map page
    var mapLat by rememberSaveable { mutableDoubleStateOf(Double.NaN) }
    var mapLng by rememberSaveable { mutableDoubleStateOf(Double.NaN) }

    val isShared = housingType == HousingType.ROOM_IN_SHARED_APT

    // Validation
    val sizeOk = sizeSqm.toDoubleOrNull()?.let { it in 1.0..1_000.0 } == true
    val roommatesOk = if (isShared) roommates.toIntOrNull()?.let { it in 1..20 } == true else true
    val canSubmit = title.isNotBlank() &&
            residency.isNotBlank() &&
            housingType != null &&
            sizeOk &&
            roommatesOk

    // Image picker (multiple)
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) pickedImages = uris
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Listing") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle Back Navigation */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFFF0004)) // red color
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (canSubmit) {
                                onConfirm(
                                    ListingForm(
                                        title = title.trim(),
                                        residencyName = residency.trim(),
                                        housingType = housingType,
                                        roommates = roommates.toIntOrNull(),
                                        roomSizeSqm = sizeSqm.toDoubleOrNull(),
                                        mapLat = mapLat.takeIf { it.isFinite() },
                                        mapLng = mapLng.takeIf { it.isFinite() },
                                        description = description.trim(),
                                        imageUris = pickedImages
                                    )
                                )
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6666)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp) // More round edges
                    ) {
                        Text("Confirm listing", color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Tiny helper text for validation
                    if (!canSubmit) {
                        Text(
                            "Please complete all required fields (valid size and roommates if shared).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Listing title") },
                leadingIcon = { Icon(Icons.Default.Title, null, tint = Color(0xFFFF6666)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)), // Gray with rounded edges
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                    unfocusedBorderColor = Color.Transparent, // Remove the default border when not focused
                    focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                    unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
                )
            )

            OutlinedTextField(
                value = residency,
                onValueChange = { residency = it },
                label = { Text("Location / Residency name") },
                leadingIcon = { Icon(Icons.Default.Apartment, null, tint = Color(0xFFFF6666)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)), // Gray with rounded edges
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                    unfocusedBorderColor = Color.Transparent, // Remove the default border when not focused
                    focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                    unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
                )
            )

            // Housing type dropdown
            HousingTypeDropdown(
                selected = housingType,
                onSelected = { housingType = it },
                accentColor = accentColor
            )

            if (isShared) {
                OutlinedTextField(
                    value = roommates,
                    onValueChange = { if (it.length <= 2) roommates = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Number of roommates") },
                    singleLine = true,
                    supportingText = { Text("Between 1 and 20") },
                    isError = !roommatesOk,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)), // Gray with rounded edges
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                        unfocusedBorderColor = Color.Transparent, // Remove the default border when not focused
                        focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                        unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
                    )
                )
            }

            OutlinedTextField(
                value = sizeSqm,
                onValueChange = { input ->
                    sizeSqm = input.filter { it.isDigit() || it == '.' }.take(7)
                },
                label = { Text("Room size (m²)") },
                trailingIcon = { Text("m²") },
                singleLine = true,
                isError = !sizeOk && sizeSqm.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)), // Gray with rounded edges
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                    unfocusedBorderColor = Color.Transparent, // Remove the default border when not focused
                    focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                    unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
                )
            )

            // Map selector
            ElevatedButton(
                onClick = onOpenMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0F0F0), // Background color (optional)
                    contentColor = Color(0xFFFF6666)   // Text color (your desired purple shade)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Map, null, tint = Color(0xFFFF6666))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (mapLat.isFinite() && mapLng.isFinite())
                        "Location selected (tap to change)"
                    else
                        "Add location on the map"
                )
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = {
                    Text("Add a description of your listing (e.g. subletting, lease transfer or other information you find relevant)")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)), // Gray with rounded edges
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                    unfocusedBorderColor = Color.Transparent, // Remove the default border when not focused
                    focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                    unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
                ),
                maxLines = 6
            )

            // Images
            Text("Photos", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        pickImages.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = androidx.compose.material3.ButtonColors(
                        containerColor = Color(0xFFF0F0F0), // Focused outline color (Red)
                        contentColor = Color(0xFFFF6666), // Remove the default border when not focused
                        disabledContentColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                        disabledContainerColor = Color.Gray        // Optional: Change label color when not focused
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Color(0xFFFF6666))
                    Spacer(Modifier.width(8.dp))
                    Text("Add pictures")
                }
                AssistChip(
                    onClick = { pickedImages = emptyList() },
                    label = { Text("Clear") }
                )
            }

            if (pickedImages.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(pickedImages) { uri ->
                        UriImage(
                            uri = uri,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}


/**
 * Housing type dropdown
 */
@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun HousingTypeDropdown(
    selected: HousingType?,
    onSelected: (HousingType) -> Unit,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.label ?: "Select housing type"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Housing type") },
            leadingIcon = { Icon(Icons.Default.Home, null, tint = Color(0xFFFF6666)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF6666), // Focused outline color (Red)
                unfocusedBorderColor = Color(0xFFFF6666), // Remove the default border when not focused
                focusedLabelColor = Color(0xFFFF6666),  // Optional: Change label color when focused
                unfocusedLabelColor = Color.Gray        // Optional: Change label color when not focused
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),

            )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HousingType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Helpers
 */

@Composable
fun UriImage(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    // Load the bitmap in a background thread
    LaunchedEffect(uri) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    // For API level 28 and above, use ImageDecoder
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    // For lower API levels, use deprecated MediaStore
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Throwable) { null }
        }
        bitmap = bmp
    }

    // Display the image if it has been loaded
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
private fun rememberSaveableUriList(initial: List<Uri> = emptyList()) =
    rememberSaveable(stateSaver = uriListSaver()) { mutableStateOf(initial) }

private fun uriListSaver(): Saver<List<Uri>, Any> =
    listSaver(
        save = { list -> list.map { it.toString() } },
        restore = { list -> list.map { Uri.parse(it) } }
    )

// Tiny badge for coordinates (optional use)
@Composable
private fun MapBadge(lat: Double?, lng: Double?) {
    if (lat == null || lng == null) return
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFF6666))
        Spacer(Modifier.width(6.dp))
        Text("${"%.5f".format(lat)}, ${"%.5f".format(lng)}")
    }
}
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun AddListingPreview() {
    val exampleListing = ListingForm(
        title = "Cozy Studio in Downtown",
        residencyName = "MySwissDorm Residence",
        housingType = HousingType.STUDIO,
        roommates = null,
        roomSizeSqm = 25.0,
        mapLat = 46.5197, // Some location (Lausanne)
        mapLng = 6.6323,
        description = "A great little studio near the university.",
        imageUris = emptyList()
    )

    AddListingScreen(
        modifier = Modifier.fillMaxSize(),
        onOpenMap = { /* TODO: handle map navigation */ },
        onConfirm = { /* Handle confirmed listing submission */ }
    )
}

