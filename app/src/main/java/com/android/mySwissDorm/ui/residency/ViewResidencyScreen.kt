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
import com.android.mySwissDorm.ui.theme.*

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
            onViewMap = onViewMap)
      })
}

@Composable
private fun ViewResidencyContent(
    uiState: ViewResidencyUIState,
    residency: com.android.mySwissDorm.model.residency.Residency?,
    errorMsg: String?,
    paddingValues: PaddingValues,
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
          paddingValues = paddingValues,
          onViewMap = onViewMap)
    }
  }
}

@Composable
private fun LoadingView(paddingValues: PaddingValues) {
  Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(modifier = Modifier.size(64.dp).testTag(C.ViewResidencyTags.LOADING))
  }
}

@Composable
private fun ErrorView(errorMsg: String?, paddingValues: PaddingValues) {
  Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
    Text(
        text = errorMsg ?: stringResource(R.string.view_residency_not_found),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(C.ViewResidencyTags.ERROR))
  }
}

@Composable
private fun ResidencyDetailsContent(
    residency: com.android.mySwissDorm.model.residency.Residency,
    poiDistances: List<POIDistance>,
    paddingValues: PaddingValues,
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
        DescriptionSection(residency.description)
        POIDistancesSection(poiDistances)
        Spacer(Modifier.height(8.dp))
        ContactInformationSection(residency)
        LocationSection(residency, onViewMap)
      }
}

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

@Composable
private fun DescriptionSection(description: String) {
  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.DESCRIPTION)) {
    Text("${stringResource(R.string.description)} :", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(3.dp))
    Text(description, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun POIDistancesSection(poiDistances: List<POIDistance>) {
  Text(
      stringResource(R.string.view_listing_nearby_points_of_interest),
      style =
          MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold),
      modifier = Modifier.testTag(C.ViewResidencyTags.POI_DISTANCES))
  if (poiDistances.isNotEmpty()) {
    POIDistancesList(poiDistances)
  } else {
    Text(
        stringResource(R.string.view_listing_no_points_of_interest),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.padding(start = 16.dp, top = 2.dp))
  }
}

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
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = MaterialTheme.typography.bodySmall.lineHeight))
}

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

@Composable
private fun determinePOITypeLabel(pois: List<POIDistance>): String {
  return when {
    pois.all { it.poiType == POIDistance.TYPE_UNIVERSITY } -> stringResource(R.string.university)
    pois.all { it.poiType == POIDistance.TYPE_SUPERMARKET } -> ""
    else -> ""
  }
}

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
          style = MaterialTheme.typography.bodyMedium,
          color = Gray)
    }
  }
}

private fun hasContactInfo(residency: com.android.mySwissDorm.model.residency.Residency): Boolean {
  return residency.email != null || residency.phone != null || residency.website != null
}

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

@Composable
private fun ContactInfoItem(label: String, value: String) {
  Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
  Spacer(Modifier.height(4.dp))
}

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
