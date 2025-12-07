package com.android.mySwissDorm.ui.residency

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      // Error is already shown in UI, no need to navigate back
    }
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
        when {
          uiState.loading -> {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.size(64.dp).testTag(C.ViewResidencyTags.LOADING))
                }
          }
          errorMsg != null || residency == null -> {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  Text(
                      text = errorMsg ?: stringResource(R.string.view_residency_not_found),
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag(C.ViewResidencyTags.ERROR))
                }
          }
          else -> {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag(C.ViewResidencyTags.ROOT),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  // Residency name (already in top bar, but can show here too if needed)
                  Text(
                      text = residency.name,
                      fontSize = 28.sp,
                      fontWeight = FontWeight.SemiBold,
                      lineHeight = 32.sp,
                      modifier = Modifier.testTag(C.ViewResidencyTags.NAME),
                      color = TextColor)

                  // Photos
                  val imageUrls = uiState.imageUrls
                  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.PHOTOS)) {
                    Text("${stringResource(R.string.photos)} :", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (imageUrls.isNotEmpty()) {
                      // Show first image as main image
                      AsyncImage(
                          model = imageUrls.first(),
                          contentDescription = null,
                          modifier =
                              Modifier.fillMaxWidth()
                                  .height(200.dp)
                                  .clip(RoundedCornerShape(12.dp)),
                          contentScale = ContentScale.Crop,
                          placeholder =
                              androidx.compose.ui.graphics.painter.ColorPainter(LightGray),
                          error = androidx.compose.ui.graphics.painter.ColorPainter(LightGray))
                    } else {
                      // Placeholder if no images
                      Box(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .height(200.dp)
                                  .clip(RoundedCornerShape(12.dp))
                                  .background(LightGray),
                          contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.image),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray)
                          }
                    }
                  }

                  // Description
                  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.DESCRIPTION)) {
                    Text(
                        "${stringResource(R.string.description)} :",
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(residency.description, style = MaterialTheme.typography.bodyLarge)
                  }

                  // Nearby Points of Interest
                  val poiDistances = uiState.poiDistances
                  Text(
                      stringResource(R.string.view_listing_nearby_points_of_interest),
                      style =
                          MaterialTheme.typography.bodyMedium.copy(
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              fontWeight = FontWeight.SemiBold),
                      modifier = Modifier.testTag(C.ViewResidencyTags.POI_DISTANCES))
                  if (poiDistances.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)) {
                          // Group POIs by walking time
                          val groupedByTime = poiDistances.groupBy { it.walkingTimeMinutes }

                          groupedByTime
                              .toList()
                              .sortedBy { it.first }
                              .forEach { (timeMinutes, pois) ->
                                if (pois.size == 1) {
                                  // Single POI at this time
                                  val poiDistance = pois.first()
                                  val timeText =
                                      when (poiDistance.poiType) {
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
                                        else -> "" // Should not happen
                                      }
                                  Text(
                                      timeText,
                                      style =
                                          MaterialTheme.typography.bodyMedium.copy(
                                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                                              lineHeight =
                                                  MaterialTheme.typography.bodySmall.lineHeight))
                                } else {
                                  // Multiple POIs at the same time - combine them
                                  val poiNames = pois.map { it.poiName }
                                  val andString = stringResource(R.string.and)
                                  val combinedNames =
                                      when (poiNames.size) {
                                        2 -> poiNames.joinToString(" $andString ")
                                        else ->
                                            poiNames.dropLast(1).joinToString(", ") +
                                                " $andString " +
                                                poiNames.last()
                                      }

                                  // Determine the type label (university or supermarket)
                                  val typeLabel =
                                      when {
                                        pois.all { it.poiType == POIDistance.TYPE_UNIVERSITY } ->
                                            stringResource(R.string.university)
                                        pois.all { it.poiType == POIDistance.TYPE_SUPERMARKET } ->
                                            "" // Supermarkets don't need a type label
                                        else -> "" // Mixed types, no label
                                      }

                                  val timeText =
                                      if (typeLabel.isNotEmpty()) {
                                        stringResource(
                                            R.string.view_listing_walking_time_minutes_of_multiple,
                                            timeMinutes,
                                            combinedNames,
                                            typeLabel)
                                      } else {
                                        stringResource(
                                            R.string.view_listing_walking_time_minutes_of_no_type,
                                            timeMinutes,
                                            combinedNames)
                                      }

                                  Text(
                                      timeText,
                                      style =
                                          MaterialTheme.typography.bodyMedium.copy(
                                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                                              lineHeight =
                                                  MaterialTheme.typography.bodySmall.lineHeight))
                                }
                              }
                        }
                  } else {
                    Text(
                        stringResource(R.string.view_listing_no_points_of_interest),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                  }
                  Spacer(Modifier.height(8.dp))

                  // Contact Information
                  SectionCard(modifier = Modifier.testTag(C.ViewResidencyTags.CONTACT_INFO)) {
                    Text(
                        "${stringResource(R.string.contact_information)} :",
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (residency.email != null) {
                      Text(
                          "${stringResource(R.string.email)}: ${residency.email}",
                          style = MaterialTheme.typography.bodyMedium)
                      Spacer(Modifier.height(4.dp))
                    }
                    if (residency.phone != null) {
                      Text(
                          "${stringResource(R.string.phone)}: ${residency.phone}",
                          style = MaterialTheme.typography.bodyMedium)
                      Spacer(Modifier.height(4.dp))
                    }
                    if (residency.website != null) {
                      Text(
                          "${stringResource(R.string.website)}: ${residency.website}",
                          style = MaterialTheme.typography.bodyMedium)
                    }
                    if (residency.email == null &&
                        residency.phone == null &&
                        residency.website == null) {
                      Text(
                          stringResource(R.string.view_residency_no_contact_info),
                          style = MaterialTheme.typography.bodyMedium,
                          color = Gray)
                    }
                  }

                  // Location
                  MapPreview(
                      location = residency.location,
                      title = residency.name,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(180.dp)
                              .testTag(C.ViewResidencyTags.LOCATION),
                      onMapClick = {
                        onViewMap(
                            residency.location.latitude,
                            residency.location.longitude,
                            residency.name,
                            R.string.view_residency_location)
                      })
                }
          }
        }
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
