package com.android.mySwissDorm.resources

import com.android.mySwissDorm.ui.navigation.Screen

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

    fun universityNameTestTag(universityName: String?): String {
      return "${universityName ?: "nullValUniversityName"}TestTag"
    }

    fun residencyNameTestTag(residencyName: String?): String {
      return "${residencyName ?: "nullValResidencyName"}TestTag"
    }

    const val PROFILE_SCREEN_TITLE = "profile_title"
    const val PROFILE_SCREEN_BACK_BUTTON = "profile_back_button"

    fun buttonNavBarTestTag(screen: Screen): String {
      return "bottom_nav_${screen.route}"
    }
  }

  object ViewListingTags {
    const val ROOT = "viewListingRoot"
    const val TITLE = "viewListingTitle"
    const val POSTED_BY = "viewListingPostedBy"
    const val BULLETS = "viewListingBullets"
    const val DESCRIPTION = "viewListingDescription"
    const val PHOTOS = "viewListingPhotos"
    const val LOCATION = "viewListingLocation"
    const val CONTACT_FIELD = "viewListingContactField"
    const val APPLY_BTN = "viewListingApplyBtn"
    const val EDIT_BTN = "viewListingEditBtn"
  }

  object ViewUserProfileTags {
    const val ROOT = "view_user_profile_root"
    const val TITLE = "view_user_profile_title"
    const val BACK_BTN = "view_user_profile_back"
    const val AVATAR_BOX = "view_user_profile_avatar"
    const val RESIDENCE_CHIP = "view_user_profile_residence_chip"
    const val SEND_MESSAGE = "view_user_profile_send_message"
    const val RETRY_BTN = "view_user_profile_retry"
    const val ERROR_TEXT = "view_user_profile_error"
    const val LOADING = "view_user_profile_loading"
  }

  object BrowseCityTags {
    const val ROOT = "browseCityRoot"
    const val BACK_BUTTON = "browseCityBackButton"
    const val LOCATION_BUTTON = "browseCityLocationButton"

    const val TAB_REVIEWS = "browseCityTabReviews"
    const val TAB_LISTINGS = "browseCityTabListings"

    const val LIST = "browseCityList"
    const val LOADING = "browseCityLoading"
    const val ERROR = "browseCityError"
    const val EMPTY = "browseCityEmpty"

    fun card(listingUid: String) = "browseCityCard_$listingUid"
  }

  object CameraButtonTag {
    const val TAG = "cameraButtonTag"
  }

  object GalleryButtonTag {
    const val TAG = "galleryButtonTag"
  }

  object CustomLocationDialogTags {
    const val DIALOG_TITLE = "customLocationDialogTitle"
    const val LOCATION_TEXT_FIELD = "customLocationDialogTextField"
    const val CONFIRM_BUTTON = "customLocationDialogConfirmButton"
    const val CLOSE_BUTTON = "customLocationDialogCloseButton"
    const val DROPDOWN_MENU = "customLocationDialogDropdownMenu"

    fun locationSuggestion(index: Int): String = "customLocationDialogSuggestion$index"

    const val MORE_OPTION = "customLocationDialogMoreOption"
  }
}
