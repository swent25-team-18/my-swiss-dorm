package com.android.mySwissDorm.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.model.photo.Photo
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.utils.FakeRequestPermissionContract
import com.android.mySwissDorm.utils.FakeTakePictureContract
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraButtonTest {
  val simpleTestTag = "simpleTestTag"

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testEveryThingIsDisplayed() {
    composeTestRule.setContent {
      CameraButton(onSave = {}) {
        Text(text = "HelloWorld!", modifier = Modifier.testTag(tag = simpleTestTag))
      }
    }

    composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(simpleTestTag, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testClickTakePictureSucceed() {
    val clicked = mutableStateOf(false)
    composeTestRule.setContent {
      CameraButton(
          onSave = { clicked.value = true },
          permissionContract = FakeRequestPermissionContract.success(),
          takePictureContract = FakeTakePictureContract.success())
    }
    val cameraButtonNode = composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG)
    cameraButtonNode.assertIsDisplayed()
    cameraButtonNode.performClick()

    composeTestRule.waitUntil(5_000) { clicked.value }
  }

  @Test
  fun testClickedTakePictureFail() {
    val notClicked = mutableStateOf(true)
    composeTestRule.setContent {
      DefaultCameraButton(
          onSave = { notClicked.value = false },
          permissionContract = FakeRequestPermissionContract.success(),
          takePictureContract = FakeTakePictureContract.failure())
    }
    val cameraButtonNode = composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG)
    cameraButtonNode.assertIsDisplayed()
    cameraButtonNode.performClick()

    composeTestRule.waitForIdle()

    assertTrue(notClicked.value)
  }

  @Test
  fun testCameraUseContentSchemeForPhoto() {
    val photoCaptured = mutableStateOf<Photo?>(null)
    composeTestRule.setContent {
      CameraButton(
          onSave = { photoCaptured.value = it },
          permissionContract = FakeRequestPermissionContract.success(),
          takePictureContract = FakeTakePictureContract.success())
    }
    val cameraButtonNode = composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG)
    cameraButtonNode.assertIsDisplayed()
    cameraButtonNode.performClick()
    composeTestRule.waitForIdle()
    assertNotNull(photoCaptured.value)
    composeTestRule.waitUntil("CameraButton should pass a Photo with a content Uri scheme", 5_000) {
      photoCaptured.value?.image?.scheme == "content"
    }
  }

  @Test
  fun testCameraDoesNotReturnPhotoDeniedCameraAccess() {
    val captured = mutableStateOf(false)
    composeTestRule.setContent {
      DefaultCameraButton(
          onSave = { captured.value = true },
          permissionContract = FakeRequestPermissionContract.failure(),
          takePictureContract = FakeTakePictureContract.success())
    }
    composeTestRule.onNodeWithTag(C.CameraButtonTag.TAG).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertTrue("No photos should have been captured", !captured.value)
  }
}
