package com.android.mySwissDorm.resources

import com.android.mySwissDorm.model.residency.ResidencyName
import com.android.mySwissDorm.model.university.UniversityName

// Like R, but C
object C {
  object Tag {
    const val greeting = "main_screen_greeting"
    const val greeting_robo = "second_screen_greeting"

    const val main_screen_container = "main_screen_container"
    const val second_screen_container = "second_screen_container"
    const val SIGN_IN_SCREEN = "signInScreen"
    const val SIGN_IN_APP_LOGO = "signInAppLogo"
    const val SIGN_IN_LOG_IN_BUTTON = "signInLogInButton"
    const val SIGN_IN_SIGN_UP_BUTTON = "signInSignUpButton"
    const val SIGN_UP_SCREEN = "signUpScreen"
    const val SIGN_UP_NAME_FIELD = "signUpNameField"
    const val SIGN_UP_NAME_HELP_TEXT = "signUpNameHelpText"
    const val SIGN_UP_LAST_NAME_FIELD = "signUpLastNameField"
    const val SIGN_UP_LAST_NAME_HELP_TEXT = "signUpLastNameHelpText"
    const val SIGN_UP_PHONE_NUMBER_FIELD = "signUpPhoneNumberField"
    const val SIGN_UP_PHONE_NUMBER_HELP_TEXT = "signUpPhoneNumberHelpText"
    const val SIGN_UP_RESIDENCY_FIELD = "signUpResidencyField"
    const val SIGN_UP_RESIDENCY_DROP_DOWN_BOX = "signupResidencyDropDownBox"
    const val SIGN_UP_RESIDENCY_DROP_DOWN_MENU = "signUpResidencyDropDownMenu"
    const val SIGN_UP_UNIVERSITY_FIELD = "signUpUniversityField"
    const val SIGN_UP_UNIVERSITY_DROP_DOWN_BOX = "signUpUniversityDropDownBox"
    const val SIGN_UP_UNIVERSITY_DROP_DOWN_MENU = "signUpUniversityDropDownMenu"
    const val SIGN_UP_BUTTON = "signUpButton"
    const val SIGN_UP_HELP_TEXT = "signUpHelpText"
    const val SIGN_UP_BACK_BUTTON = "signUpBackButton"

    fun universityNameTestTag(universityName: UniversityName?): String {
      return "${universityName?.name ?: "nullValUniversityName"}TestTag"
    }

    fun residencyNameTestTag(residencyName: ResidencyName?): String {
      return "${residencyName?.name ?: "nullValResidencyName"}TestTag"
    }
  }
}
