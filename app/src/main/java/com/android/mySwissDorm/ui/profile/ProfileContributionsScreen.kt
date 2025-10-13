package com.android.mySwissDorm.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mySwissDorm.ui.theme.AccentRed
import com.android.mySwissDorm.ui.theme.CardBorder
import com.android.mySwissDorm.ui.theme.ScreenBg
import androidx.compose.ui.tooling.preview.Preview
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContributionsScreen(
    onBackClick: () -> Unit,
    onContributionClick: (Contribution) -> Unit,
    vm: ProfileContributionsViewModel = viewModel()
) {
  LaunchedEffect(Unit) { vm.load() }
  val ui by vm.ui.collectAsState()

  Scaffold(
      containerColor = ScreenBg,
      topBar = {
        CenterAlignedTopAppBar(
            title = { Text("My contributions") },
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AccentRed)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White, titleContentColor = Color.Black))
      }) { inner ->
        if (ui.isLoading) {
          LinearProgressIndicator(
              modifier = Modifier.padding(inner).fillMaxWidth().padding(horizontal = 16.dp))
        }

        ui.error?.let {
          Text(
              it,
              modifier = Modifier.padding(inner).padding(16.dp),
              color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              itemsIndexed(ui.items) { index, c ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onContributionClick(c) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, CardBorder)) {
                      Column(Modifier.padding(16.dp)) {
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = c.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A7A7A))
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onContributionClick(c) },
                            modifier = Modifier.testTag("btn_contrib_details_$index"),
                            border = BorderStroke(1.dp, AccentRed),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent, contentColor = AccentRed),
                            shape = MaterialTheme.shapes.medium) {
                              Text("View details")
                            }
                      }
                    }
              }
            }
      }
}

/**
 * Backward-compatible overload so existing call-sites that pass a list don’t break immediately. It
 * seeds the ViewModel with that list.
 */
@Composable
fun ProfileContributionsScreen(
    contributions: List<Contribution>,
    onBackClick: () -> Unit,
    onContributionClick: (Contribution) -> Unit,
    vm: ProfileContributionsViewModel = viewModel()
) {
  LaunchedEffect(contributions) { vm.setFromExternal(contributions) }
  ProfileContributionsScreen(
      onBackClick = onBackClick, onContributionClick = onContributionClick, vm = vm)
}


@Preview(showBackground = true)
@Composable
private fun ProfileContributionsScreenPreview() {
    MySwissDormAppTheme {
        val items = listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room")
        )
        ProfileContributionsScreen(
            contributions = items,
            onBackClick = {},
            onContributionClick = {}
        )
    }
}