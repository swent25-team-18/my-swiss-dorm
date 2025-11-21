package com.android.mySwissDorm.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.android.mySwissDorm.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SignInScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SignInScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.SIGN_IN_SCREEN) }) {
  val appLogo: KNode = child { hasTestTag(C.Tag.SIGN_IN_APP_LOGO) }
  val logInButton: KNode = child { hasTestTag(C.Tag.SIGN_IN_LOG_IN_BUTTON) }
  val signUpButton: KNode = child { hasTestTag(C.Tag.SIGN_IN_SIGN_UP_BUTTON) }
  val guestButton = onNode { hasTestTag(C.Tag.SIGN_IN_GUEST_BUTTON) }
}
