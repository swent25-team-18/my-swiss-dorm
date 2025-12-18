package com.android.mySwissDorm.utils

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.R
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.lang.AutoCloseable
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Utility class used to translate text. It automatically detects the source language of the text
 * and translates it into the given language.
 *
 * @param langIdentifier The language identifier, identifying the language of a text
 * @param getClient The function creating the Translator.
 *
 * Note: This class implements AutoCloseable. It must be used with a 'use' block or have its close()
 * method called explicitly after use to prevent memory leaks.
 */
class Translator(
    private val langIdentifier: LanguageIdentifier = LanguageIdentification.getClient(),
    private val getClient: (TranslatorOptions) -> Translator = { options ->
      Translation.getClient(options)
    }
) : AutoCloseable {
  private val logTag = "Translator"

  /**
   * Translates the given text to the specified language. This function was written with the help of
   * AI.
   *
   * @param text The string to be translated
   * @param targetLanguageCode The target language code (e.g. TranslateLanguage.FRENCH).
   * @param context The context of the app, used to get strings from different languages.
   * @return The translated text, or a status/error message if an error occurs.
   */
  suspend fun translateText(text: String, targetLanguageCode: String, context: Context): String =
      suspendCancellableCoroutine { continuation ->
        langIdentifier
            .identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
              if (languageCode == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG) {
                continuation.resume(
                    context.getString(R.string.translator_could_not_determine_language))
                return@addOnSuccessListener
              }

              if (languageCode == targetLanguageCode) {
                continuation.resume(text)
                return@addOnSuccessListener
              }

              val options =
                  TranslatorOptions.Builder()
                      .setSourceLanguage(languageCode)
                      .setTargetLanguage(targetLanguageCode)
                      .build()

              val translator = getClient(options)

              continuation.invokeOnCancellation {
                Log.d(logTag, "Coroutine cancelled, closing translator.")
                translator.close()
              }

              translator
                  .downloadModelIfNeeded()
                  .addOnSuccessListener {
                    translator
                        .translate(text)
                        .addOnSuccessListener { translatedText ->
                          continuation.resume(translatedText)
                          translator.close()
                        }
                        .addOnFailureListener { exception ->
                          Log.w(logTag, "Error translating the string: $text", exception)
                          continuation.resume(
                              context.getString(R.string.translator_error_translating))
                          translator.close()
                        }
                  }
                  .addOnFailureListener { exception ->
                    Log.w(logTag, "Error downloading the model", exception)
                    continuation.resume(context.getString(R.string.translator_error_translating))
                    translator.close()
                  }
            }
            .addOnFailureListener { exception ->
              Log.w(logTag, "Error detecting the language of the string: $text", exception)
              continuation.resume(context.getString(R.string.translator_error_translating))
            }
      }

  override fun close() {
    langIdentifier.close()
  }

  companion object {
    /**
     * Utility function used to get the code of a language from the Locale code
     *
     * @param localeCode The locale code (e.g. "fr", "en-US") obtained from
     *   Locale.getDefault().language.
     * @return The language code understandable for TranslateLanguage.
     */
    fun getLanguageCodeFromLocale(localeCode: String): String {
      val simpleCode = localeCode.split("-").first()
      return TranslateLanguage.fromLanguageTag(simpleCode) ?: TranslateLanguage.ENGLISH
    }
  }
}
