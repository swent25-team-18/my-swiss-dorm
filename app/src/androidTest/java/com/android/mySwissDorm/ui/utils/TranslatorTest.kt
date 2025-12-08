package com.android.mySwissDorm.ui.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.mySwissDorm.R
import com.android.mySwissDorm.utils.Translator
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentifier
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

  @Test
  fun translateText_WithUndeterminedLanguage_ReturnsErrorMessage() = runBlocking {
    val translator = Translator()
    val result = translator.translateText("azbycxdwevfu", TranslateLanguage.ENGLISH, context)

    assertEquals(context.getString(R.string.translator_could_not_determine_language), result)
  }

  @Test
  fun translateText_WithSameLanguage_ReturnsOriginalMessage() = runBlocking {
    val translator = Translator()
    val result = translator.translateText("C'est un chien", TranslateLanguage.FRENCH, context)

    assertEquals("C'est un chien", result)
  }

  @Test
  fun translateText_WithFailingLanguageIdentifier_ReturnsErrorMessage() = runBlocking {
    class FailingLanguageIdentifier : LanguageIdentifier {
      override fun identifyLanguage(text: String): Task<String> {
        return Tasks.forException(RuntimeException("Language detection failed by API."))
      }

      override fun identifyPossibleLanguages(text: String): Task<List<IdentifiedLanguage?>?> =
          Tasks.forResult(emptyList())

      override fun close() {}
    }

    val translator = Translator(langIdentifier = FailingLanguageIdentifier())

    val result = translator.translateText("Hello", TranslateLanguage.FRENCH, context)
    assertEquals(context.getString(R.string.translator_error_translating), result)
  }

  @Test
  fun translateText_WithFailingModelDownload_ReturnsErrorMessage() = runBlocking {
    class FailingTranslator : com.google.mlkit.nl.translate.Translator {
      override fun downloadModelIfNeeded(): Task<Void> =
          Tasks.forException(RuntimeException("Model download error."))

      override fun downloadModelIfNeeded(p0: DownloadConditions): Task<Void?> =
          Tasks.forException(RuntimeException("Model download error."))

      override fun translate(text: String): Task<String> =
          Tasks.forException(RuntimeException("Translation service failed."))

      override fun close() {}
    }

    val translator = Translator(getClient = { options -> FailingTranslator() })

    val result = translator.translateText("Hello", TranslateLanguage.FRENCH, context)
    assertEquals(context.getString(R.string.translator_error_translating), result)
  }

  @Test
  fun translateText_WithFailingTranslation_ReturnsErrorMessage() = runBlocking {
    class FailingTranslator : com.google.mlkit.nl.translate.Translator {
      override fun downloadModelIfNeeded(): Task<Void> = Tasks.forResult(null as Void?)

      override fun downloadModelIfNeeded(p0: DownloadConditions): Task<Void?> =
          Tasks.forResult(null)

      override fun translate(text: String): Task<String> =
          Tasks.forException(RuntimeException("Translation service failed."))

      override fun close() {}
    }

    val translator = Translator(getClient = { options -> FailingTranslator() })

    val result = translator.translateText("Hello", TranslateLanguage.FRENCH, context)
    assertEquals(context.getString(R.string.translator_error_translating), result)
  }
}
