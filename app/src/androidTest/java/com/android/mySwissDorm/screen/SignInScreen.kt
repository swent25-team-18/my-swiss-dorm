package com.android.mySwissDorm.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.android.mySwissDorm.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SignInScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SignInScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.SIGN_IN_SCREEN) }) {
  val app_logo: KNode = child { hasTestTag(C.Tag.SIGN_IN_APP_LOGO) }
  val log_in_button: KNode = child { hasTestTag(C.Tag.SIGN_IN_LOG_IN_BUTTON) }
  val sign_up_button: KNode = child { hasTestTag(C.Tag.SIGN_IN_SIGN_UP_BUTTON) }
}
