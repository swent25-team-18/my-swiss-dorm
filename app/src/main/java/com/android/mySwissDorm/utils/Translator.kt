package com.android.mySwissDorm.utils

import android.content.Context
import android.util.Log
import com.android.mySwissDorm.R
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Utility class used to translate text. It automatically detects the source language of the text
 * and translates it into the given language.
 */
class Translator {
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
        val langIdentifier = LanguageIdentification.getClient()
        continuation.invokeOnCancellation {
          Log.d(logTag, "Coroutine cancelled, closing language identifier.")
          langIdentifier.close()
        }

        langIdentifier
            .identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
              if (languageCode == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG) {
                continuation.resume(
                    context.getString(R.string.translator_could_not_determine_language))
                langIdentifier.close()
                return@addOnSuccessListener
              }

              if (languageCode == targetLanguageCode) {
                continuation.resume(text)
                langIdentifier.close()
                return@addOnSuccessListener
              }

              val options =
                  TranslatorOptions.Builder()
                      .setSourceLanguage(languageCode)
                      .setTargetLanguage(targetLanguageCode)
                      .build()

              val translator = Translation.getClient(options)

              langIdentifier.close()

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
              langIdentifier.close()
            }
      }

  /**
   * Utility function used to get the code of a language from the Locale code
   *
   * @param localeCode The locale code (e.g. "fr", "en-US") obtained from
   *   Locale.getDefault().language.
   * @return The language code understandable for TranslateLanguage.
   */
  companion object {
    fun getLanguageCodeFromLocale(localeCode: String): String {
      val simpleCode = localeCode.split("-").first()
      return TranslateLanguage.fromLanguageTag(simpleCode) ?: TranslateLanguage.ENGLISH
    }
  }
}
