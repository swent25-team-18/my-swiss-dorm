package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewListingScreen(
    viewListingViewModel: ViewListingViewModel = viewModel(),
    listingUid: String,
    onGoBack: () -> Unit = {},
    onApply: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
  LaunchedEffect(listingUid) { viewListingViewModel.loadListing(listingUid) }

  val listingUIState by viewListingViewModel.uiState.collectAsState()
  val listing = listingUIState.listing
  val fullNameOfPoster = listingUIState.fullNameOfPoster
  val errorMsg = listingUIState.errorMsg
  val canApply = listingUIState.contactMessage.any { !it.isWhitespace() }
    val isOwner = FirebaseAuth.getInstance().currentUser?.uid == listing.ownerId


    val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      onGoBack()
      delay(500)
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
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  text = listing.title,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.SemiBold,
                  lineHeight = 32.sp)

              Text(
                  text = "Posted by $fullNameOfPoster ${formatRelative(listing.postedAt)}",
                  style =
                      MaterialTheme.typography.bodyMedium.copy(
                          color = MaterialTheme.colorScheme.onSurfaceVariant))

              // Bullet section
              SectionCard {
                BulletRow("${listing.roomType}")
                BulletRow("${listing.pricePerMonth}.-/month")
                BulletRow("${listing.areaInM2}m²")
                BulletRow("Starting ${formatDate(listing.startDate)}")
              }

              // Description
              SectionCard {
                Text("Description :", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(listing.description, style = MaterialTheme.typography.bodyLarge)
              }

              // Photos placeholder
              PlaceholderBlock(text = "PHOTOS (Not implemented yet)", height = 220.dp)

              // Location placeholder
              PlaceholderBlock(text = "LOCATION (Not implemented yet)", height = 180.dp)

              // Contact message
              OutlinedTextField(
                  value = listingUIState.contactMessage,
                  onValueChange = { viewListingViewModel.setContactMessage(it) },
                  placeholder = { Text("Contact the announcer") },
                  modifier = Modifier.fillMaxWidth(),
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

            if (isOwner) {
                // Owner sees an Edit button centered, same size as Apply
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // Contact message
                OutlinedTextField(
                    value = listingUIState.contactMessage,
                    onValueChange = { viewListingViewModel.setContactMessage(it) },
                    placeholder = { Text("Contact the announcer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    minLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        disabledContainerColor = Color(0xFFF0F0F0),
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Apply now button (centered, half width, rounded, red)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onApply,
                        enabled = listingUIState.contactMessage.any { !it.isWhitespace() },
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE66D66),
                            disabledContainerColor = Color(0xFFEBD0CE),
                            disabledContentColor = Color(0xFFFFFFFF)
                        )
                    ) {
                        Text("Apply now !", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
      }
  )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color(0xFFF0F0F0),
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
    Text(text, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun PlaceholderBlock(text: String, height: Dp) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(height)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFFF0F0F0)),
      contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium)
      }
}

// --- utilities

private fun formatDate(ts: Timestamp?): String =
    ts?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "—"

private fun formatRelative(ts: Timestamp?, nowMillis: Long = System.currentTimeMillis()): String {
  if (ts == null) return "—"

  val then = ts.toDate().time
  var diff = nowMillis - then
  if (diff < 0) diff = 0L // future-safe

  val seconds = diff / 1000
  return when {
    seconds < 60 -> "${seconds}s ago"
    seconds < 60 * 60 -> "${seconds / 60} min ago" // < 1 hour → minutes
    seconds < 24 * 60 * 60 -> "${seconds / 3600}h ago" // < 24h → hours
    seconds < 7 * 24 * 60 * 60 -> "${seconds / 86400}d ago" // < 7d → days
    else -> "on ${formatDate(ts)}" // ≥ 7d → date
  }
}

@Composable
@Preview
private fun ViewListingScreenPreview() {
  ViewListingScreen(listingUid = "preview")
}
