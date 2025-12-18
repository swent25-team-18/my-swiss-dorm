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
import java.util.concurrent.atomic.AtomicBoolean
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
   * Translates the given text to the specified language.
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
                Log.d(logTag, "Couldn't determine the language")
                if (continuation.isActive) continuation.resume(text)
                return@addOnSuccessListener
              }

              if (languageCode == targetLanguageCode) {
                if (continuation.isActive) continuation.resume(text)
                return@addOnSuccessListener
              }

              val options =
                  TranslatorOptions.Builder()
                      .setSourceLanguage(languageCode)
                      .setTargetLanguage(targetLanguageCode)
                      .build()

              val translator = getClient(options)

              // FIX 1: Use AtomicBoolean to safely track if we've closed the translator
              val isTranslatorClosed = AtomicBoolean(false)

              fun closeTranslatorSafely() {
                // compareAndSet ensures we only try to close it once
                if (isTranslatorClosed.compareAndSet(false, true)) {
                  try {
                    translator.close()
                  } catch (e: Exception) {
                    Log.w(logTag, "Error closing translator", e)
                  }
                }
              }

              continuation.invokeOnCancellation {
                Log.d(logTag, "Coroutine cancelled, closing translator.")
                closeTranslatorSafely()
              }

              translator
                  .downloadModelIfNeeded()
                  .addOnSuccessListener {
                    // FIX 2: Check if closed before attempting translation
                    if (!continuation.isActive || isTranslatorClosed.get()) {
                      closeTranslatorSafely()
                      return@addOnSuccessListener
                    }

                    // FIX 3: Wrap translate in try-catch. This is the specific fix for your crash.
                    try {
                      translator
                          .translate(text)
                          .addOnSuccessListener { translatedText ->
                            if (continuation.isActive && !isTranslatorClosed.get()) {
                              continuation.resume(translatedText)
                            }
                            closeTranslatorSafely()
                          }
                          .addOnFailureListener { exception ->
                            // Only report errors if we didn't intentionally close it
                            if (exception !is IllegalStateException || !isTranslatorClosed.get()) {
                              Log.w(logTag, "Error translating: $text", exception)
                              if (continuation.isActive) {
                                continuation.resume(
                                    context.getString(R.string.translator_error_translating))
                              }
                            }
                            closeTranslatorSafely()
                          }
                    } catch (e: Exception) {
                      // Catches "IllegalStateException: Translator has been closed" if race
                      // condition occurs
                      Log.w(logTag, "Translator closed right before translation started", e)
                      if (continuation.isActive) {
                        continuation.resume(
                            context.getString(R.string.translator_error_translating))
                      }
                      closeTranslatorSafely()
                    }
                  }
                  .addOnFailureListener { exception ->
                    Log.w(logTag, "Error downloading model", exception)
                    if (continuation.isActive) {
                      continuation.resume(context.getString(R.string.translator_error_translating))
                    }
                    closeTranslatorSafely()
                  }
            }
            .addOnFailureListener { exception ->
              Log.w(logTag, "Error detecting language: $text", exception)
              if (continuation.isActive) {
                continuation.resume(context.getString(R.string.translator_error_translating))
              }
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
