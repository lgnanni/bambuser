package com.lgnanni.bambuser

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lgnanni.bambuser.viewmodel.MainViewModel
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test

@ExperimentalAnimationApi
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSearchText() {
        val mockedViewModel: MainViewModel = mock()
        // Perform UI actions using Compose UI Test library
        composeTestRule.onNodeWithText("Filter title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Filter title").performTextInput("Test Text")

        // Add assertions or verifications based on your test scenario
        // For example, you might want to verify that the ViewModel's setSearchText method is called
        verify(mockedViewModel).setSearchText("Test Text")
    }

    @Test
    fun testSaveImage() {
        // Mock the necessary dependencies, e.g., Bitmap
        // Perform UI actions using Compose UI Test library
        composeTestRule.onNodeWithText("Save Image").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Image").performClick()

        // Add assertions or verifications based on your test scenario
        // For example, you might want to verify that the saveToStorage method is called with the correct parameters
        // Mockito.verify(viewModel).saveToStorage(bitmap)
    }

    // Add more tests as needed
}
