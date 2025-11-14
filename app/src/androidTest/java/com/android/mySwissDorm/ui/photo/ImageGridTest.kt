package com.android.mySwissDorm.ui.photo

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testUris = setOf(
        "android.resource://com.android.mySwissDorm/${R.drawable.geneve}".toUri(),
        "android.resource://com.android.mySwissDorm/${R.drawable.zurich}".toUri(),
        "android.resource://com.android.mySwissDorm/${R.drawable.fribourg}".toUri(),
    )

    @Test
    fun imageGridDisplaysAllImages() {
        composeTestRule.setContent {
            ImageGrid(
                imageUris = testUris,
                isEditingMode = false,
                onRemove = {}
            )
        }
        testUris.forEach { uri ->
            composeTestRule.onNodeWithTag(C.ImageGridTags.imageTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
                .performScrollTo()
        }
    }

    @Test
    fun imageGridDisplaysDeleteButton() {
        composeTestRule.setContent {
            ImageGrid(
                imageUris = testUris,
                isEditingMode = true,
                onRemove = {}
            )
        }
        testUris.forEach { uri ->
            composeTestRule.onNodeWithTag(C.ImageGridTags.imageTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
                .performScrollTo()
            composeTestRule.onNodeWithTag(C.ImageGridTags.deleteButtonTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun imageGridDoesNotDisplaysDeleteButtonCorrectly() {
        composeTestRule.setContent {
            ImageGrid(
                imageUris = testUris,
                isEditingMode = false,
                onRemove = {}
            )
        }
        testUris.forEach { uri ->
            composeTestRule.onNodeWithTag(C.ImageGridTags.imageTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
                .performScrollTo()
            composeTestRule.onNodeWithTag(C.ImageGridTags.deleteButtonTag(uri), useUnmergedTree = true)
                .assertIsNotDisplayed()
        }
    }

    @Test
    fun imageGridDisplaysNothingOnEmptyList() {
        composeTestRule.setContent {
            ImageGrid(
                imageUris = setOf(),
                isEditingMode = true,
                onRemove = {}
            )
        }

        composeTestRule.onNodeWithContentDescription( C.ImageGridTags.ICON_DELETE_CONTENT_DESC, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun imageGridOnRemoveWorks() {
        val removedUris = mutableListOf<Uri>()

        composeTestRule.setContent {
            ImageGrid(
                imageUris = testUris,
                isEditingMode = true,
                onRemove = { removedUris.add(it) }
            )
        }

        testUris.forEach { uri ->
            composeTestRule.onNodeWithTag(C.ImageGridTags.imageTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
                .performScrollTo()
            composeTestRule.onNodeWithTag(C.ImageGridTags.deleteButtonTag(uri), useUnmergedTree = true)
                .assertIsDisplayed()
                .performClick()
        }
        composeTestRule.waitUntil(5_000) {
            removedUris.containsAll(testUris)
        }
    }
}