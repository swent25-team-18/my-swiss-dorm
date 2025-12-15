package com.android.mySwissDorm.ui.residency

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.poi.POIDistance
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.map.MapPreview
import com.android.mySwissDorm.ui.photo.FullScreenImageViewer
import com.android.mySwissDorm.ui.photo.ImageGrid
import com.android.mySwissDorm.ui.theme.*

/**
 * Screen that displays detailed information about a residency, including its description, contact
 * information, location, and nearby points of interest (POIs).
 *
 * The screen shows:
 * - Residency name and description
 * - Nearby points of interest (universities and supermarkets) with walking distances
 * - Contact information (email, phone, website)
 * - Interactive map preview
 *
 * POIs are calculated using the [DistanceService], which fetches universities from the
 * [UniversitiesRepository] and supermarkets via the [WalkingRouteService]. This matches the
 * implementation used in the main branch for consistency.
 *
 * @param viewResidencyViewModel The [ViewResidencyViewModel] providing residency data and POI
 *   distances.
 * @param residencyName The unique identifier of the residency to display.
 * @param onGoBack Callback invoked when the user taps the back button.
 * @param onViewMap Callback invoked when the user taps the map preview to open the full map screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewResidencyScreen(
    viewResidencyViewModel: ViewResidencyViewModel = viewModel(),
    residencyName: String,
    onGoBack: () -> Unit = {},
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit =
        { _, _, _, _ ->
        }
) {
  val context = LocalContext.current
  LaunchedEffect(residencyName) { viewResidencyViewModel.loadResidency(residencyName, context) }

  val uiState by viewResidencyViewModel.uiState.collectAsState()
  val residency = uiState.residency
  val errorMsg = uiState.errorMsg

  // Handle full screen images
  if (uiState.showFullScreenImages && uiState.images.isNotEmpty()) {
    FullScreenImageViewer(
        imageUris = uiState.images.map { it.image },
        onDismiss = { viewResidencyViewModel.dismissFullScreenImages() },
        initialIndex = uiState.fullScreenImagesIndex)
    return
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  residency?.name ?: residencyName,
                  color = MainColor,
                  modifier = Modifier.testTag(C.ViewResidencyTags.TOP_BAR_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = { onGoBack() },
                  modifier = Modifier.testTag(C.ViewResidencyTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MainColor)
                  }
            })
      },
      content = { paddingValues ->
        ViewResidencyContent(
            uiState = uiState,
            residency = residency,
            errorMsg = errorMsg,
            paddingValues = paddingValues,
            onImageClick = { index -> viewResidencyViewModel.onClickImage(index) },
            onViewMap = onViewMap)
      })
}

/**
 * Content composable that handles different UI states (loading, error, success) for the residency
 * screen.
 *
 * @param uiState The current UI state containing loading status, residency data, and error
 *   messages.
 * @param residency The residency data to display, or null if not loaded.
 * @param errorMsg Error message to display if loading failed.
 * @param paddingValues Padding values from the Scaffold.
 * @param onViewMap Callback invoked when the map preview is tapped.
 */
@Composable
private fun ViewResidencyContent(
    uiState: ViewResidencyUIState,
    residency: com.android.mySwissDorm.model.residency.Residency?,
    errorMsg: String?,
    paddingValues: PaddingValues,
    onImageClick: (Int) -> Unit,
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit
) {
  when {
    uiState.loading -> {
      LoadingView(paddingValues)
    }
    errorMsg != null || residency == null -> {
      ErrorView(errorMsg, paddingValues)
    }
    else -> {
      ResidencyDetailsContent(
          residency = residency,
          poiDistances = uiState.poiDistances,
          isLoadingPOIs = uiState.isLoadingPOIs,
          paddingValues = paddingValues,
          images = uiState.images,
          onImageClick = onImageClick,
          onViewMap = onViewMap)
    }
  }
}

/**
 * Displays a loading indicator while the residency data is being fetched.
 *
 * @param paddingValues Padding values from the Scaffold.
 */
@Composable
private fun LoadingView(paddingValues: PaddingValues) {
  Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(modifier = Modifier.size(64.dp).testTag(C.ViewResidencyTags.LOADING))
  }
}

/**
 * Displays an error message when the residency fails to load.
 *
 * @param errorMsg The error message to display, or null to show a default "not found" message.
 * @param paddingValues Padding values from the Scaffold.
 */
@Composable
private fun ErrorView(errorMsg: String?, paddingValues: PaddingValues) {
  Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
    Text(
        text = errorMsg ?: stringResource(R.string.view_residency_not_found),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(C.ViewResidencyTags.ERROR))
  }
}

/**
 * Displays the main content of the residency screen, including all sections.
 *
 * @param residency The residency data to display.
 * @param poiDistances List of nearby points of interest with their walking distances.
 * @param paddingValues Padding values from the Scaffold.
 * @param onViewMap Callback invoked when the map preview is tapped.
 */
@Composable
private fun ResidencyDetailsContent(
    residency: com.android.mySwissDorm.model.residency.Residency,
    poiDistances: List<POIDistance>,
    isLoadingPOIs: Boolean,
    paddingValues: PaddingValues,
    images: List<com.android.mySwissDorm.model.photo.Photo>,
    onImageClick: (Int) -> Unit,
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .verticalScroll(rememberScrollState())
              .testTag(C.ViewResidencyTags.ROOT),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResidencyNameHeader(residency.name)
        if (images.isNotEmpty()) {
          ImagesSection(images, onImageClick)
        }
        DescriptionSection(residency.description)
        POIDistancesSection(poiDistances, isLoadingPOIs)
        Spacer(Modifier.height(8.dp))
        ContactInformationSection(residency)
        LocationSection(residency, onViewMap)
      }
}

/**
 * Displays the residency images in a grid.
 *
 * @param images List of photos to display.
 * @param onImageClick Callback invoked when an image is clicked.
 */
@Composable
private fun ImagesSection(
    images: List<com.android.mySwissDorm.model.photo.Photo>,
    onImageClick: (Int) -> Unit
) {
  ImageGrid(
      imageUris = images.map { it.image }.toSet(),
      isEditingMode = false,
      onRemove = {},
      onImageClick = { uri ->
        val index = images.map { it.image }.indexOf(uri)
        if (index >= 0) {
          onImageClick(index)
        }
      },
      modifier = Modifier.fillMaxWidth().testTag(C.ViewResidencyTags.PHOTOS))
}

/**
 * Displays the residency name as a header.
 *
 * @param name The name of the residency.
 */
@Composable
private fun ResidencyNameHeader(name: String) {
  Text(
      text = name,
      fontSize = 28.sp,
      fontWeight = FontWeight.SemiBold,
      lineHeight = 32.sp,
      modifier = Modifier.testTag(C.ViewResidencyTags.NAME),
      color = TextColor)
}

/**
 * Displays the residency description in a card.
 *
 * @param description The description text to display.
 */
@Composable
private fun DescriptionSection(description: String) {
  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.DESCRIPTION)) {
    Text("${stringResource(R.string.description)} :", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(3.dp))
    Text(description, style = MaterialTheme.typography.bodyLarge)
  }
}

/**
 * Displays the nearby points of interest section with walking distances.
 *
 * Shows universities and supermarkets within walking distance, calculated using the same
 * [DistanceService] implementation as the main branch. Shows a loading indicator while POIs are
 * being calculated, or a message if no POIs are found.
 *
 * @param poiDistances List of nearby points of interest with their walking distances.
 * @param isLoadingPOIs Whether POI distances are currently being calculated.
 */
@Composable
private fun POIDistancesSection(poiDistances: List<POIDistance>, isLoadingPOIs: Boolean) {
  Text(
      stringResource(R.string.view_listing_nearby_points_of_interest),
      style =
          MaterialTheme.typography.bodyMedium.copy(color = Gray, fontWeight = FontWeight.SemiBold),
      modifier = Modifier.testTag(C.ViewResidencyTags.POI_DISTANCES))
  if (isLoadingPOIs) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CircularProgressIndicator(
              modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MainColor)
          Text(
              stringResource(R.string.poi_loading_message),
              style = MaterialTheme.typography.bodyMedium.copy(color = Gray.copy(alpha = 0.6f)))
        }
  } else if (poiDistances.isNotEmpty()) {
    POIDistancesList(poiDistances)
  } else {
    Text(
        stringResource(R.string.view_listing_no_points_of_interest),
        style = MaterialTheme.typography.bodyMedium.copy(color = Gray.copy(alpha = 0.6f)),
        modifier = Modifier.padding(start = 16.dp, top = 2.dp))
  }
}

/**
 * Displays a list of POI distances, grouped by walking time.
 *
 * @param poiDistances List of POI distances to display.
 */
@Composable
private fun POIDistancesList(poiDistances: List<POIDistance>) {
  Column(
      modifier = Modifier.padding(start = 16.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp)) {
        val groupedByTime = poiDistances.groupBy { it.walkingTimeMinutes }
        groupedByTime
            .toList()
            .sortedBy { it.first }
            .forEach { (timeMinutes, pois) -> POIDistanceItem(timeMinutes, pois) }
      }
}

/**
 * Displays a single POI distance item, handling both single and multiple POIs at the same time.
 *
 * @param timeMinutes The walking time in minutes.
 * @param pois List of POIs at this walking time (can be multiple if they have the same time).
 */
@Composable
private fun POIDistanceItem(timeMinutes: Int, pois: List<POIDistance>) {
  val timeText =
      if (pois.size == 1) {
        formatSinglePOITimeText(pois.first())
      } else {
        formatMultiplePOIsTimeText(timeMinutes, pois)
      }
  Text(
      timeText,
      style =
          MaterialTheme.typography.bodyMedium.copy(
              color = Gray, lineHeight = MaterialTheme.typography.bodySmall.lineHeight))
}

/**
 * Formats the walking time text for a single POI.
 *
 * @param poiDistance The POI distance to format.
 * @return Formatted string resource for the walking time.
 */
@Composable
private fun formatSinglePOITimeText(poiDistance: POIDistance): String {
  return when (poiDistance.poiType) {
    POIDistance.TYPE_UNIVERSITY ->
        stringResource(
            R.string.view_listing_walking_time_university,
            poiDistance.walkingTimeMinutes,
            poiDistance.poiName)
    POIDistance.TYPE_SUPERMARKET ->
        stringResource(
            R.string.view_listing_walking_time_supermarket,
            poiDistance.walkingTimeMinutes,
            poiDistance.poiName)
    else -> ""
  }
}

/**
 * Formats the walking time text for multiple POIs at the same time.
 *
 * @param timeMinutes The walking time in minutes.
 * @param pois List of POIs at this walking time.
 * @return Formatted string resource for the walking time of multiple POIs.
 */
@Composable
private fun formatMultiplePOIsTimeText(timeMinutes: Int, pois: List<POIDistance>): String {
  val poiNames = pois.map { it.poiName }
  val andString = stringResource(R.string.and)
  val combinedNames =
      when (poiNames.size) {
        2 -> poiNames.joinToString(" $andString ")
        else -> poiNames.dropLast(1).joinToString(", ") + " $andString " + poiNames.last()
      }
  val typeLabel = determinePOITypeLabel(pois)
  return if (typeLabel.isNotEmpty()) {
    stringResource(
        R.string.view_listing_walking_time_minutes_of_multiple,
        timeMinutes,
        combinedNames,
        typeLabel)
  } else {
    stringResource(
        R.string.view_listing_walking_time_minutes_of_no_type, timeMinutes, combinedNames)
  }
}

/**
 * Determines the type label for a group of POIs (e.g., "university").
 *
 * @param pois List of POIs to determine the type for.
 * @return The type label string resource, or empty string if mixed types or supermarkets.
 */
@Composable
private fun determinePOITypeLabel(pois: List<POIDistance>): String {
  return when {
    pois.all { it.poiType == POIDistance.TYPE_UNIVERSITY } -> stringResource(R.string.university)
    pois.all { it.poiType == POIDistance.TYPE_SUPERMARKET } -> ""
    else -> ""
  }
}

/**
 * Displays the contact information section in a card.
 *
 * Shows email, phone, and website if available, or a message indicating no contact information.
 *
 * @param residency The residency data containing contact information.
 */
@Composable
private fun ContactInformationSection(
    residency: com.android.mySwissDorm.model.residency.Residency
) {
  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.CONTACT_INFO)) {
    Text("${stringResource(R.string.contact_information)} :", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    if (hasContactInfo(residency)) {
      ContactInfoItems(residency)
    } else {
      Text(
          stringResource(R.string.view_residency_no_contact_info),
          style = MaterialTheme.typography.bodyMedium.copy(color = Gray.copy(alpha = 0.6f)))
    }
  }
}

/**
 * Checks if the residency has any contact information available.
 *
 * @param residency The residency to check.
 * @return True if the residency has at least one contact method (email, phone, or website).
 */
private fun hasContactInfo(residency: com.android.mySwissDorm.model.residency.Residency): Boolean {
  return residency.email != null || residency.phone != null || residency.website != null
}

/**
 * Displays all available contact information items for the residency.
 *
 * @param residency The residency data containing contact information.
 */
@Composable
private fun ContactInfoItems(residency: com.android.mySwissDorm.model.residency.Residency) {
  if (residency.email != null) {
    ContactInfoItem(stringResource(R.string.email), residency.email)
  }
  if (residency.phone != null) {
    ContactInfoItem(stringResource(R.string.phone), residency.phone)
  }
  if (residency.website != null) {
    ContactInfoItem(stringResource(R.string.website), residency.website.toString())
  }
}

/**
 * Displays a single contact information item (e.g., "Email: example@email.com").
 *
 * @param label The label for the contact information (e.g., "Email").
 * @param value The contact information value.
 */
@Composable
private fun ContactInfoItem(label: String, value: String) {
  Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
  Spacer(Modifier.height(4.dp))
}

/**
 * Displays an interactive map preview showing the residency location.
 *
 * @param residency The residency data containing location information.
 * @param onViewMap Callback invoked when the map preview is tapped.
 */
@Composable
private fun LocationSection(
    residency: com.android.mySwissDorm.model.residency.Residency,
    onViewMap: (latitude: Double, longitude: Double, title: String, nameId: Int) -> Unit
) {
  MapPreview(
      location = residency.location,
      title = residency.name,
      modifier = Modifier.fillMaxWidth().height(180.dp).testTag(C.ViewResidencyTags.LOCATION),
      onMapClick = {
        onViewMap(
            residency.location.latitude,
            residency.location.longitude,
            residency.name,
            R.string.view_residency_location)
      })
}

/**
 * Reusable card component for grouping related content sections.
 *
 * @param modifier Modifier applied to the card.
 * @param content Composable content displayed inside the card.
 */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      color = TextBoxColor,
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 0.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content)
      }
}
