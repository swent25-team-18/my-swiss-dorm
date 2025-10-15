package com.android.mySwissDorm.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.android.mySwissDorm.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SignUpScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SignUpScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.SIGN_UP_SCREEN) }) {
  val signUpNameField: KNode = child { hasTestTag(C.Tag.SIGN_UP_NAME_FIELD) }
  val signUpLastNameField: KNode = child { hasTestTag(C.Tag.SIGN_UP_LAST_NAME_FIELD) }
  val signUpPhoneNumberField: KNode = child { hasTestTag(C.Tag.SIGN_UP_PHONE_NUMBER_FIELD) }
  val signUpResidencyField: KNode = child { hasTestTag(C.Tag.SIGN_UP_RESIDENCY_FIELD) }
  val signUpResidencyDropDownBox: KNode = child {
    hasTestTag(C.Tag.SIGN_UP_RESIDENCY_DROP_DOWN_BOX)
  }
  val signUpUniversityField: KNode = child { hasTestTag(C.Tag.SIGN_UP_UNIVERSITY_FIELD) }
  val signUpUniversityDropDownBox: KNode = child {
    hasTestTag(C.Tag.SIGN_UP_UNIVERSITY_DROP_DOWN_BOX)
  }
  val signUpButton: KNode = child { hasTestTag(C.Tag.SIGN_UP_BUTTON) }
  val signUpHelpText: KNode = child { hasTestTag(C.Tag.SIGN_UP_HELP_TEXT) }
  val signUpBackButton: KNode = child { hasTestTag(C.Tag.SIGN_UP_BACK_BUTTON) }
}
