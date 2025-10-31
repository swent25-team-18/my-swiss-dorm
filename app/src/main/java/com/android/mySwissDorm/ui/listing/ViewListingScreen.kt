package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextBoxColor
import com.android.mySwissDorm.ui.theme.TextColor
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatDate
import com.android.mySwissDorm.ui.utils.DateTimeUi.formatRelative

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewListingScreen(
    viewListingViewModel: ViewListingViewModel = viewModel(),
    listingUid: String,
    onGoBack: () -> Unit = {},
    onApply: () -> Unit = {},
    onEdit: () -> Unit = {},
    onViewProfile: (ownerId: String) -> Unit = {}
) {
  LaunchedEffect(listingUid) { viewListingViewModel.loadListing(listingUid) }

  val listingUIState by viewListingViewModel.uiState.collectAsState()
  val listing = listingUIState.listing
  val fullNameOfPoster = listingUIState.fullNameOfPoster
  val errorMsg = listingUIState.errorMsg
  val canApply = listingUIState.contactMessage.any { !it.isWhitespace() }
  val isOwner = listingUIState.isOwner

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      onGoBack()
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewListingViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Listing Details") },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      },
      content = { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .testTag(C.ViewListingTags.ROOT),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  text = listing.title,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.SemiBold,
                  lineHeight = 32.sp,
                  modifier = Modifier.testTag(C.ViewListingTags.TITLE),
                  color = TextColor)

              // tag we'll look for
              val tagProfile = "PROFILE_ID"

              // build the AnnotatedString tagging the name
              val annotatedPostedByString = buildAnnotatedString {
                append("Posted by ")

                // pushStringAnnotation to "tag" this part of the string
                pushStringAnnotation(tag = tagProfile, annotation = listing.ownerId)
                // apply the style to the name
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MainColor)) {
                  append(fullNameOfPoster)
                  if (isOwner) append(" (You)")
                }
                // stop tagging
                pop()

                append(" ${formatRelative(listing.postedAt)}")
              }

              // remember the TextLayoutResult
              var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

              Text(
                  text = annotatedPostedByString,
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant),
                  onTextLayout = { textLayoutResult = it },
                  modifier =
                      Modifier.testTag(C.ViewListingTags.POSTED_BY).pointerInput(Unit) {
                        detectTapGestures { pos ->
                          val l = textLayoutResult ?: return@detectTapGestures
                          val offset = l.getOffsetForPosition(pos)

                          // find any annotations at that exact offset
                          annotatedPostedByString
                              .getStringAnnotations(start = offset, end = offset)
                              .firstOrNull { it.tag == tagProfile } // Check if it's our tag
                              ?.let { annotation ->
                                // trigger the callback with the stored ownerId
                                onViewProfile(annotation.item)
                              }
                        }
                      })

              // Bullet section
              SectionCard(modifier = Modifier.testTag(C.ViewListingTags.BULLETS)) {
                BulletRow("${listing.roomType}")
                BulletRow("${listing.pricePerMonth}.-/month")
                BulletRow("${listing.areaInM2}m²")
                BulletRow("Starting ${formatDate(listing.startDate)}")
              }

              // Description
              SectionCard(modifier = Modifier.testTag(C.ViewListingTags.DESCRIPTION)) {
                Text("Description :", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(listing.description, style = MaterialTheme.typography.bodyLarge)
              }

              // Photos placeholder
              PlaceholderBlock(
                  text = "PHOTOS (Not implemented yet)",
                  height = 220.dp,
                  modifier = Modifier.testTag(C.ViewListingTags.PHOTOS))

              // Location placeholder
              PlaceholderBlock(
                  text = "LOCATION (Not implemented yet)",
                  height = 180.dp,
                  modifier = Modifier.testTag(C.ViewListingTags.LOCATION))

              if (isOwner) {
                // Owner sees an Edit button centered, same size as Apply
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Button(
                      onClick = onEdit,
                      modifier =
                          Modifier.fillMaxWidth(0.55f)
                              .height(52.dp)
                              .testTag(C.ViewListingTags.EDIT_BTN),
                      shape = RoundedCornerShape(16.dp)) {
                        Text("Edit", style = MaterialTheme.typography.titleMedium)
                      }
                }
              } else {
                // Contact message
                OutlinedTextField(
                    value = listingUIState.contactMessage,
                    onValueChange = { viewListingViewModel.setContactMessage(it) },
                    placeholder = { Text("Contact the announcer") },
                    modifier = Modifier.fillMaxWidth().testTag(C.ViewListingTags.CONTACT_FIELD),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    minLines = 1,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F0F0),
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            disabledContainerColor = Color(0xFFF0F0F0),
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline))

                // Apply now button (centered, half width, rounded, red)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Button(
                      onClick = onApply,
                      enabled = canApply,
                      modifier =
                          Modifier.fillMaxWidth(0.55f)
                              .height(52.dp)
                              .testTag(C.ViewListingTags.APPLY_BTN),
                      shape = RoundedCornerShape(16.dp),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MainColor,
                              disabledContainerColor = Color(0xFFEBD0CE),
                              disabledContentColor = Color(0xFFFFFFFF))) {
                        Text(
                            "Apply now !",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium)
                      }
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

@Composable
private fun BulletRow(text: String) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    Text("•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = TextColor)
  }
}

@Composable
private fun PlaceholderBlock(text: String, height: Dp, modifier: Modifier) {
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .height(height)
              .clip(RoundedCornerShape(16.dp))
              .background(TextBoxColor),
      contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextColor)
      }
}

@Composable
@Preview
private fun ViewListingScreenPreview() {
  ViewListingScreen(listingUid = "preview")
}
