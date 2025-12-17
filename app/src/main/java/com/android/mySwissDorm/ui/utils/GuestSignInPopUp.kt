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
import androidx.compose.ui.res.stringResource
import com.android.mySwissDorm.R
import com.android.mySwissDorm.ui.theme.BackGroundColor
import com.android.mySwissDorm.ui.theme.Dimens
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MainColor)
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Surface(
                  color = BackGroundColor,
                  contentColor = TextColor,
                  shape = RoundedCornerShape(Dimens.CardCornerRadius),
                  modifier = Modifier.fillMaxWidth(0.8f).padding(Dimens.PaddingDefault)) {
                    Column(
                        modifier = Modifier.padding(Dimens.PaddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXLarge)) {
                          Text(
                              text =
                                  stringResource(
                                      R.string.guest_sign_in_pop_up_sign_in_to_create_profile),
                              style = MaterialTheme.typography.titleMedium,
                              color = TextColor)
                          Button(
                              onClick = onSignInClick,
                              colors = ButtonDefaults.buttonColors(containerColor = MainColor)) {
                                Text(
                                    stringResource(R.string.guest_sign_in_pop_up_sign_in),
                                    color = BackGroundColor)
                              }
                        }
                  }
            }
      }
}
