package com.android.mySwissDorm.ui.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.utils.Translator
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TranslatorTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun getLanguageCodeFromLocaleWorks() {
    assertEquals(TranslateLanguage.FRENCH, Translator.getLanguageCodeFromLocale("fr"))
    assertEquals(TranslateLanguage.ENGLISH, Translator.getLanguageCodeFromLocale("en-US"))
    assertEquals(TranslateLanguage.ENGLISH, Translator.getLanguageCodeFromLocale("not a code"))
  }

  @Test
  fun translateText_EnglishToFrench_ReturnsCorrectTranslation() = runBlocking {
    val translator = Translator()
    val translatedText =
        translator.translateText("This is a cat", TranslateLanguage.FRENCH, context)

    assertEquals("Ceci est un chat", translatedText)
  }

  @Test
  fun translateText_FrenchToEnglish_ReturnsCorrectTranslation() = runBlocking {
    val translator = Translator()
    val translatedText =
        translator.translateText("C'est un chien", TranslateLanguage.ENGLISH, context)

    assertEquals("It's a dog", translatedText)
  }
}
