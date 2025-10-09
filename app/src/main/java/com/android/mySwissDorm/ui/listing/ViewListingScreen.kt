package com.android.mySwissDorm.ui.listing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewListingScreen(
    viewListingViewModel: ViewListingViewModel = viewModel(),
    listingUid: String,
    onGoBack: () -> Unit = {},
    onClick: () -> Unit = {}
) {
  LaunchedEffect(listingUid) { viewListingViewModel.loadListing(listingUid) }

  val listingUIState by viewListingViewModel.uiState.collectAsState()
  val listing = listingUIState.listing
  val errorMsg = listingUIState.errorMsg

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
        TopAppBar(
            title = { Text("Listing Details") },
            navigationIcon = {
              IconButton(onClick = { onGoBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              // Title
              Text(text = listing.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)

              // Posted by and Time
              Text(
                  text =
                      "Posted by ${listing.ownerId} • ${formatTimestamp(listing.postedAt)}", // TODO
                  // Replace ownerId with actual name
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Normal)

              // Room Type and Details
              Column(
                  modifier = Modifier.fillMaxWidth().background(Color.Gray).padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Add other UI components here
                    BulletPointText(text = listing.roomType.toString)
                    BulletPointText(text = "${listing.pricePerMonth} .-/month")
                    BulletPointText(text = "${listing.areaInM2} m²")
                    BulletPointText(text = "Starting ${formatTimestamp(listing.startDate)}")
                  }

              // Description in grey box
              Box(modifier = Modifier.fillMaxWidth().background(Color.Gray).padding(8.dp)) {
                Text(text = "Description: \n${listing.description}")
              }

              // Photos Placeholder
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(200.dp)
                          .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center) {
                    Text("PHOTOS HERE", color = Color.White) // TODO Replace with actual photos
              }

              // Location Placeholder
              Box(
                  modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray),
                  contentAlignment = Alignment.Center) {
                    Text("LOCATION HERE", color = Color.Black) // TODO Replace with actual map
              }

              // Contact TextField
              OutlinedTextField(
                  value = listingUIState.contactMessage,
                  onValueChange = { viewListingViewModel.setContactMessage(it) },
                  placeholder = { Text("Contact the announcer") },
                  modifier = Modifier.fillMaxWidth())

              // Apply Button
              OutlinedButton(
                  onClick = { onClick() },
                  modifier =
                      Modifier.fillMaxWidth(0.5f) // Fill half the width of the parent
                          .align(Alignment.CenterHorizontally) // Center the button horizontally
                  ) {
                    Text(text = "Apply now!", color = Color.White)
                  }
            }
      })
}

// Helper function to format Timestamp into a readable date string
fun formatTimestamp(timestamp: Timestamp?): String {
  return timestamp?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
      ?: "Unknown Date"
}

// BulletPointText Composable to add bullet points
@Composable
fun BulletPointText(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = "•", // Bullet character
        fontSize = 16.sp,
        modifier = Modifier.padding(end = 8.dp))
    Text(text = text, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
  }
}
