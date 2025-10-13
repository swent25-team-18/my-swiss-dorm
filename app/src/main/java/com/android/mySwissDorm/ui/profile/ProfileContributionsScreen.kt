package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mySwissDorm.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContributionsScreen(
    contributions: List<Contribution>,
    onBackClick: () -> Unit,
    onContributionClick: (Contribution) -> Unit
) {
  Scaffold(
      containerColor = LightGray,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("My contributions") },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Red0)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White, titleContentColor = Color.Black))
      }) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              itemsIndexed(contributions) { index, c ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onContributionClick(c) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, LightGray0)) {
                      Column(Modifier.padding(16.dp)) {
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightGray0)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onContributionClick(c) },
                            modifier = Modifier.testTag("btn_contrib_details_$index"),
                            border = BorderStroke(1.dp, Red0),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent, contentColor = Red0),
                            shape = MaterialTheme.shapes.medium) {
                              Text("View details")
                            }
                      }
                    }
              }
            }
      }
}
