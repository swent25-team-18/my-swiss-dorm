package com.android.mySwissDorm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.profile.Contribution
import com.android.mySwissDorm.ui.profile.ListingDetailScreen
import com.android.mySwissDorm.ui.profile.ProfileContributionsScreen
import com.android.mySwissDorm.ui.profile.RequestDetailScreen
import com.android.mySwissDorm.ui.theme.MySwissDormAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Light system bars
    window.statusBarColor = Color.WHITE
    window.navigationBarColor = Color.WHITE
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = true
      isAppearanceLightNavigationBars = true
    }

    setContent {
      MySwissDormAppTheme {
        val navController = rememberNavController()

        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
              NavHost(navController = navController, startDestination = "profileContributions") {
                composable("profileContributions") {
                  // Démo : une annonce (ouvre Listing) + une demande (ouvre Request)
                  val contributions =
                      listOf(
                          Contribution("Listing l1", "Nice room near EPFL"),
                          Contribution("Request r1", "Student interested in a room"))

                  ProfileContributionsScreen(
                      contributions = contributions,
                      onBackClick = { onBackPressedDispatcher.onBackPressed() },
                      onContributionClick = { c ->
                        // extrait l'ID après l'espace → "l1" / "r1"
                        val id = c.title.substringAfter(' ').trim()

                        if (c.title.startsWith("Request", ignoreCase = true)) {
                          navController.navigate("requestDetail/$id")
                        } else {
                          navController.navigate("listingDetail/$id")
                        }
                      })
                }

                // Détail Annonce
                composable("listingDetail/{id}") { backStackEntry ->
                  val id = backStackEntry.arguments?.getString("id") ?: "unknown"
                  ListingDetailScreen(
                      id = id,
                      onBack = {
                        navController.popBackStack()
                      } // retour à ProfileContributionsScreen
                      )
                }

                // Détail Demande
                composable("requestDetail/{id}") { backStackEntry ->
                  val id = backStackEntry.arguments?.getString("id") ?: "unknown"
                  RequestDetailScreen(
                      id = id,
                      onBack = {
                        navController.popBackStack()
                      } // retour à ProfileContributionsScreen
                      )
                }
              }
            }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier.semantics { testTag = C.Tag.greeting })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MySwissDormAppTheme { Greeting("Android") }
}
