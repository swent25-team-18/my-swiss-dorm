package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.ui.theme.MainColor
import com.android.mySwissDorm.ui.theme.TextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInPopUp(onSignInClick: () -> Unit, onBack: () -> Unit, title: String) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Surface(
                  color = MainColor,
                  contentColor = TextColor,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier.fillMaxWidth(0.8f).padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                          Text(
                              text = "Sign in to create a profile",
                              style = MaterialTheme.typography.titleMedium,
                              color = TextColor // Text color TextColor
                              )
                          Button(
                              onClick = onSignInClick,
                              colors = ButtonDefaults.buttonColors(containerColor = TextColor)) {
                                Text("Sign In", color = MainColor)
                              }
                        }
                  }
            }
      }
}
