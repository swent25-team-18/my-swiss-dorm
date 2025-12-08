# Add Residency Details Navigation and Image Support

## Summary
This PR adds navigation to residency detail cards from listings and reviews screens, implements image support for residency cards using Unsplash placeholders, and improves UI spacing consistency.

## Features Added

### 1. Residency Details Navigation
- **Clickable residency name in listings**: Users can now tap on the residency name in a listing to navigate directly to the residency details card
- **"See residency details" button**: Added a button on the "Reviews by Residency" screen that navigates to the residency details card
- Both navigation paths use the same `ViewResidencyScreen` with proper routing

### 2. Image Support for Residency Cards
- **Unsplash image integration**: Residency cards now display images from Unsplash as placeholders
- **Priority system**: Images are loaded in the following order:
  1. Unsplash images (via `ResidencyImageProvider`)
  2. Images from associated rental listings (fallback)
- **Note**: The current Unsplash images are placeholders. Real photos of the residencies will replace these in a future update.

### 3. UI Improvements
- **Reduced spacing**: Adjusted spacing between "Posted by" and "Residency" text in listings to match the spacing used in "Nearby Points of Interest" section (0.dp)
- **Better visual hierarchy**: Residency name is now positioned directly below "Posted by" for better readability

## Technical Changes

### New Files
- `app/src/main/java/com/android/mySwissDorm/ui/residency/ResidencyImageProvider.kt`
  - Provides default Unsplash image URLs for residencies
  - Maps residency names to publicly available image URLs

### Modified Files

#### Navigation
- `app/src/main/java/com/android/mySwissDorm/ui/navigation/Screen.kt`
  - Added `ResidencyDetails` screen route
  
- `app/src/main/java/com/android/mySwissDorm/ui/navigation/AppNavHost.kt`
  - Added composable route for `Screen.ResidencyDetails`
  - Updated `ViewListingScreen` to handle residency name clicks
  - Updated `ReviewsByResidencyScreen` to handle "See residency details" button clicks

#### ViewModels
- `app/src/main/java/com/android/mySwissDorm/ui/residency/ViewResidencyViewModel.kt`
  - Added `imageUrls` to `ViewResidencyUIState`
  - Implemented `loadResidencyImages()` method with Unsplash priority
  - Updated `loadResidency()` to load images in parallel with residency data

#### UI Screens
- `app/src/main/java/com/android/mySwissDorm/ui/residency/ViewResidencyScreen.kt`
  - Replaced placeholder image with `AsyncImage` component
  - Added placeholder and error handling for image loading
  - Reduced spacing in contact information section

- `app/src/main/java/com/android/mySwissDorm/ui/listing/ViewListingScreen.kt`
  - Made residency name clickable with navigation callback
  - Removed padding between "Posted by" and "Residency" rows
  - Added test tag for clickable residency name

- `app/src/main/java/com/android/mySwissDorm/ui/review/ReviewsByResidencyScreen.kt`
  - Moved "See residency details" button below the title (instead of in TopAppBar actions)
  - Centered the button horizontally
  - Added navigation callback

#### Repositories
- `app/src/main/java/com/android/mySwissDorm/model/rental/RentalListingRepository.kt`
  - Added `getAllRentalListingsByResidency(residencyName: String)` method

- `app/src/main/java/com/android/mySwissDorm/model/rental/RentalListingRepositoryFirestore.kt`
  - Implemented `getAllRentalListingsByResidency()` with Firestore query

- `app/src/main/java/com/android/mySwissDorm/model/rental/RentalListingRepositoryLocal.kt`
  - Implemented `getAllRentalListingsByResidency()` for local Room database

- `app/src/main/java/com/android/mySwissDorm/model/rental/RentalListingRepositoryHybrid.kt`
  - Implemented `getAllRentalListingsByResidency()` for hybrid repository

#### Resources
- `app/src/main/res/values/strings.xml`
  - Added `screen_residency_details` string
  - Added `see_residency_details` string
  - Added `view_residency_failed_to_load` string

- `app/src/main/java/com/android/mySwissDorm/resources/C.kt`
  - Added `RESIDENCY_NAME_CLICKABLE` test tag

### Test Files

#### New Test Files
- `app/src/androidTest/java/com/android/mySwissDorm/ui/listing/ViewListingScreenResidencyTest.kt`
  - Tests for residency name display and click navigation in listings
  
- `app/src/androidTest/java/com/android/mySwissDorm/ui/residency/ViewResidencyScreenTest.kt`
  - Tests for residency details screen display and navigation

- `app/src/test/java/com/android/mySwissDorm/ui/residency/ResidencyImageProviderTest.kt`
  - Unit tests for image provider functionality

#### Modified Test Files
- `app/src/androidTest/java/com/android/mySwissDorm/ui/review/ReviewsByResidencyScreenTest.kt`
  - Added tests for "See residency details" button display and click

- `app/src/androidTest/java/com/android/mySwissDorm/ui/profile/ProfileContributionsViewModelTest.kt`
  - Added `@Before` method to initialize repository providers with mocks
  - Fixed `IllegalStateException` by properly initializing `RentalListingRepositoryProvider` and `ReviewsRepositoryProvider`

- `app/src/androidTest/java/com/android/mySwissDorm/ui/overview/BrowseCityScreenTest.kt`
  - Added `getAllRentalListingsByResidency()` to mock repository

- `app/src/androidTest/java/com/android/mySwissDorm/ui/profile/ProfileContributionsViewModelTest.kt`
  - Added `getAllRentalListingsByResidency()` to mock repository

## Testing
- ✅ All new tests pass
- ✅ Existing tests updated to support new repository method
- ✅ No compilation errors
- ✅ Navigation flows tested

## Notes
- **Image Placeholders**: The current Unsplash images are temporary placeholders. Real photos of each residency will be integrated in a future update to replace these placeholders.
- The image loading prioritizes Unsplash images, but falls back to listing images if available
- All navigation uses proper routing with screen parameters

## Screenshots
(Add screenshots showing:)
- Clickable residency name in listing
- "See residency details" button on reviews screen
- Residency card with image displayed
- Improved spacing in listing view

