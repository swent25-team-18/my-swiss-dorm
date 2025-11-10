package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
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
      containerColor = BackGroundColor,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("My contributions") },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackGroundColor, titleContentColor = TextColor))
      }) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize().background(White).padding(16.dp)) {
          ProfileContributionsList(
              contributions = contributions,
              onContributionClick = onContributionClick,
              modifier = Modifier.fillMaxWidth().testTag("contributions_list"))
        }
      }
}

@Composable
fun ProfileContributionsList(
    contributions: List<Contribution>,
    onContributionClick: (Contribution) -> Unit,
    modifier: Modifier = Modifier
) {
  if (contributions.isEmpty()) {
    Text(
        text = "No contributions yet",
        style = MaterialTheme.typography.bodyMedium,
        color = TextColor.copy(alpha = 0.6f),
        modifier = modifier)
    return
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    contributions.forEachIndexed { index, contribution ->
      ContributionCard(
          contribution = contribution,
          onClick = { onContributionClick(contribution) },
          index = index)
    }
  }
}

@Composable
private fun ContributionCard(contribution: Contribution, onClick: () -> Unit, index: Int) {
  Card(
      modifier =
          Modifier.fillMaxWidth().testTag("card_contrib_$index").clickable(onClick = onClick),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = BackGroundColor),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border = BorderStroke(1.dp, LightGray)) {
        Column(Modifier.padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = contribution.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = TextColor,
                    modifier = Modifier.weight(1f, fill = true))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = contribution.type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MainColor)
              }
          Spacer(Modifier.height(6.dp))
          Text(
              text = contribution.description,
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF7A7A7A))
          Spacer(Modifier.height(12.dp))
          OutlinedButton(
              onClick = onClick,
              modifier = Modifier.testTag("btn_contrib_details_$index"),
              border = BorderStroke(1.dp, MainColor),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      containerColor = BackGroundColor, contentColor = MainColor),
              shape = MaterialTheme.shapes.medium) {
                Text("View details")
              }
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun ProfileContributionsScreenPreview() {
  MySwissDormAppTheme {
    val items =
        listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room"))
    ProfileContributionsScreen(contributions = items, onBackClick = {}, onContributionClick = {})
  }
}
