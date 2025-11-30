package com.android.mySwissDorm.resources

import android.net.Uri
import com.android.mySwissDorm.ui.navigation.Screen

// Like R, but C
object C {
  object Tag {
    const val main_screen_container = "main_screen_container"
    const val SIGN_IN_SCREEN = "signInScreen"
    const val SIGN_IN_APP_LOGO = "signInAppLogo"
    const val SIGN_IN_LOG_IN_BUTTON = "signInLogInButton"

    const val SIGN_IN_GUEST_BUTTON = "signInGuestButton"
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
    const val BLOCKED_NOTICE = "viewListingBlockedNotice"
    const val BLOCKED_BACK_BTN = "viewListingBlockedBackBtn"
    const val BACK_BTN = "viewListingBackBtn"
    const val BOOKMARK_BTN = "viewListingBookmarkBtn"
  }

  object ViewReviewTags {
    const val ROOT = "viewReviewRoot"
    const val TITLE = "viewReviewTitle"
    const val POSTED_BY = "viewReviewPostedBy"
    const val BULLETS = "viewReviewBullets"
    const val FILLED_STAR = "viewReviewFilledStar"
    const val EMPTY_STAR = "viewReviewEmptyStar"
    const val HALF_STAR = "viewReviewHalfStar"
    const val REVIEW_TEXT = "viewReviewText"
    const val PHOTOS = "viewReviewPhotos"
    const val LOCATION = "viewReviewLocation"
    const val EDIT_BTN = "viewReviewEditBtn"
    const val VOTE_BUTTONS = "viewReviewVoteButtons"
    const val VOTE_UPVOTE_BUTTON = "viewReviewVoteUpvoteButton"
    const val VOTE_DOWNVOTE_BUTTON = "viewReviewVoteDownvoteButton"
    const val VOTE_SCORE = "viewReviewVoteScore"
  }

  object ViewUserProfileTags {
    const val ROOT = "view_user_profile_root"
    const val TITLE = "view_user_profile_title"
    const val BACK_BTN = "view_user_profile_back"
    const val AVATAR_BOX = "view_user_profile_avatar"
    const val RESIDENCE_CHIP = "view_user_profile_residence_chip"
    const val SEND_MESSAGE = "view_user_profile_send_message"
    const val BLOCK_BUTTON = "view_user_profile_block_button"
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

    const val LISTING_LIST = "browseCityListingList"
    const val RESIDENCY_LIST = "browseCityResidencyList"
    const val LOADING = "browseCityLoading"
    const val ERROR = "browseCityError"
    const val EMPTY = "browseCityEmpty"
    const val BOOKMARKED_LISTINGS_ROOT = "bookmarkedListingsRoot"

    const val FABMENU = "fab_main"

    const val FABMENUREVIEW = "fab_menu_review"

    const val FABMENULISTING = "fab_menu_listing"

    const val FABSCRIM = "fab_scrim"

    fun listingCard(listingUid: String) = "browseCityListingCard_$listingUid"

    fun residencyCard(residencyName: String) = "browseCityResidencyCard_$residencyName"

    fun residencyCardEmptyReview(residencyName: String) =
        "browseCityResidencyCardEmptyReview_$residencyName"

    fun reviewText(reviewUid: String) = "browseCityReviewText_$reviewUid"

    fun reviewPosterName(residencyName: String) = "browseCityReviewPosterName_$residencyName"

    // Filter tags
    const val FILTER_CHIP_ROW = "browseCityFilterChipRow"
    const val FILTER_CHIP_ROOM_TYPE = "browseCityFilterChipRoomType"
    const val FILTER_CHIP_PRICE = "browseCityFilterChipPrice"
    const val FILTER_CHIP_SIZE = "browseCityFilterChipSize"
    const val FILTER_CHIP_START_DATE = "browseCityFilterChipStartDate"
    const val FILTER_CHIP_MOST_RECENT = "browseCityFilterChipMostRecent"
    const val FILTER_CLEAR_ALL_BUTTON = "browseCityFilterClearAllButton"
    const val FILTER_BOTTOM_SHEET = "browseCityFilterBottomSheet"
    const val FILTER_BOTTOM_SHEET_APPLY_BUTTON = "browseCityFilterBottomSheetApplyButton"
    const val FILTER_BOTTOM_SHEET_CLEAR_BUTTON = "browseCityFilterBottomSheetClearButton"
    const val FILTER_BOTTOM_SHEET_TITLE = "browseCityFilterBottomSheetTitle"

    fun roomTypeCheckbox(roomType: String) = "browseCityRoomTypeCheckbox_$roomType"
  }

  object ReviewsByResidencyTag {
    const val ROOT = "reviewsByResidencyRoot"
    const val BACK_BUTTON = "reviewsByResidencyBackButton"
    const val TOP_BAR_TITLE = "reviewsByResidencyTopBarTitle"
    const val LOADING = "reviewsByResidencyLoading"
    const val ERROR = "reviewsByResidencyError"
    const val EMPTY = "reviewsByResidencyEmpty"
    const val REVIEW_LIST = "reviewsByResidencyReviewList"

    fun reviewCard(reviewUid: String) = "reviewsByResidencyReviewCard_$reviewUid"

    fun reviewImagePlaceholder(reviewUid: String) = "reviewsByResidencyImagePlaceholder_$reviewUid"

    fun reviewTitle(reviewUid: String) = "reviewsByResidencyReviewTitle_$reviewUid"

    fun reviewDescription(reviewUid: String) = "reviewsByResidencyReviewDescription_$reviewUid"

    fun reviewPosterName(reviewUid: String) = "reviewsByResidencyReviewPosterName_$reviewUid"

    fun reviewPostDate(reviewUid: String) = "reviewsByResidencyReviewPostDate_$reviewUid"

    fun reviewGrade(reviewUid: String) = "reviewsByResidencyGrade_$reviewUid"

    fun reviewVoteButtons(reviewUid: String) = "reviewsByResidencyReviewVoteButtons_$reviewUid"

    fun reviewPhoto(reviewUid: String) = "reviewByResidencyPhoto$reviewUid"

    const val COMPACT_VOTE_UPVOTE_BUTTON = "reviewsByResidencyCompactVoteUpvoteButton"
    const val COMPACT_VOTE_DOWNVOTE_BUTTON = "reviewsByResidencyCompactVoteDownvoteButton"
    const val COMPACT_VOTE_SCORE = "reviewsByResidencyCompactVoteScore"
  }

  object CameraButtonTag {
    const val TAG = "cameraButtonTag"
  }

  object GalleryButtonTag {
    const val SINGLE_TAG = "singleGalleryButtonTag"
    const val MULTIPLE_TAG = "multipleGalleryButtonTag"
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

  object AddReviewTags {
    const val TITLE_FIELD = "reviewTitleField"
    const val RESIDENCY_DROPDOWN = "residencyDropdown"
    const val SIZE_FIELD = "sizeField"
    const val PRICE_FIELD = "priceField"
    const val DESC_FIELD = "descField"
    const val GRADE_FIELD = "gradeField"
    const val SUBMIT_BUTTON = "submitButton"
    const val PHOTOS = "addReviewPhotos"
  }

  object StarRatingBarTags {
    fun getStarTag(starIndex: Int) = "starTag_$starIndex"
  }

  object EditReviewTags {
    const val TEXT_FIELD = "reviewTitleField"

    const val RESIDENCY_DROPDOWN = "residencyDropdown"

    const val SIZE_FIELD = "sizeField"

    const val PRICE_FIELD = "priceField"

    const val DESCRIPTION_FIELD = "descField"

    const val GRADE_FIELD = "gradeField"

    const val DELETE_BUTTON = "deleteButton"

    const val SAVE_BUTTON = "saveButton"

    const val REVIEW_TITLE = "reviewTitleField"
    const val PHOTOS = "editReviewPhotos"
  }

  object CustomDatePickerDialogTags {
    const val OK_BUTTON = "customDatePickerDialogOkButton"
    const val CANCEL_BUTTON = "customDatePickerDialogCancelButton"
  }

  object AddListingScreenTags {
    const val ROOT = "addListingRoot"
    const val TITLE_FIELD = "titleField"
    const val SIZE_FIELD = "sizeField"
    const val PRICE_FIELD = "priceField"
    const val START_DATE_FIELD = "startDateField"
    const val DESC_FIELD = "descField"
    const val CONFIRM_BUTTON = "confirmButton"
    const val CUSTOM_LOCATION_BUTTON = "customLocationButton"
  }

  object EditListingScreenTags {
    const val TITLE_FIELD = "titleField"
    const val SIZE_FIELD = "sizeField"
    const val PRICE_FIELD = "priceField"
    const val START_DATE_FIELD = "startDateField"
    const val DESC_FIELD = "descField"
    const val DELETE_BUTTON = "deleteButton"
    const val SAVE_BUTTON = "saveButton"
    const val CUSTOM_LOCATION_BUTTON = "customLocationButton"
  }

  object AdminPageTags {
    const val CHIP_CITY = "adminPageChipCity"
    const val CHIP_RESIDENCY = "adminPageChipResidency"
    const val CHIP_UNIVERSITY = "adminPageChipUniversity"
    const val CHIP_ADMIN = "adminPageChipAdmin"
    const val LOCATION_BUTTON = "adminPageLocationButton"
    const val SAVE_BUTTON = "adminPageSaveButton"
  }

  object AddPhotoButtonTags {
    const val BUTTON = "addPhotoButton"
    const val DIALOG = "addPhotoDialog"
    const val CAMERA_BUTTON_TEXT = "addPhotoCameraButtonText"
    const val GALLERY_BUTTON_TEXT = "addPhotoGalleryButtonText"
  }

  object SettingsTags {
    const val SETTINGS_SCROLL = "SettingsScroll"
    const val PROFILE_BUTTON = "ProfileButton"
    const val EMAIL_FIELD = "EmailField"
    const val DELETE_ACCOUNT_BUTTON = "DeleteAccountButton"
    const val BLOCKED_CONTACTS_TOGGLE = "BlockedContactsToggle"
    const val BLOCKED_CONTACTS_LIST = "BlockedContactsList"
    const val BOTTOM_BAR = "bottom_nav"
    const val CONTRIBUTIONS_BUTTON = "ContributionsButton"

    fun switch(label: String): String = "SettingSwitch_$label"
  }

  object ImageGridTags {
    fun imageTag(uri: Uri): String = "ImageGridImage$uri"

    fun deleteButtonTag(uri: Uri): String = "ImageGridDeleteButton$uri"

    const val ICON_DELETE_CONTENT_DESC: String = "Remove image"
  }

  object FullScreenImageViewerTags {
    fun imageTag(uri: Uri): String = "FullScreenImageViewerImage$uri"

    const val DELETE_BUTTON = "FullScreenImageViewerDeleteButton"
    const val LEFT_ARROW_BUTTON = "FullScreenImageViewerLeftArrowButton"
    const val RIGHT_ARROW_BUTTON = "FullScreenImageViewerRightArrowButton"
  }

  object ChannelsScreenTestTags {
    const val ROOT = "channelsScreenRoot"
    const val SEARCH_BAR = "channelsSearchBar"
    const val REQUESTED_MESSAGES_BUTTON = "requestedMessagesButton"
    const val CHANNELS_LIST = "channelsList"
    const val EMPTY_STATE = "channelsEmptyState"
    const val LOADING_INDICATOR = "channelsLoadingIndicator"
  }

  object SanitizedResidencyDropdownTags {
    const val RESIDENCY_DROPDOWN_BOX = "sanitizedResidencyDropdownBox"
    const val PRIVATE_ACCOMMODATION = "privateAccommodation"

    fun getResidencyTag(residencyName: String) = "sanitizedResidencyDropdown_$residencyName"
  }

  object ShareLinkDialogTags {
    const val DIALOG_TITLE = "shareLinkDialogTitle"
    const val QR_CODE = "shareLinkDialogQrCode"
    const val QR_ERROR = "shareLinkDialogQrError"
    const val COPY_LINK_BUTTON = "shareLinkDialogCopyLinkButton"
    const val CANCEL_TEXT = "shareLinkDialogCancelText"
  }
}
