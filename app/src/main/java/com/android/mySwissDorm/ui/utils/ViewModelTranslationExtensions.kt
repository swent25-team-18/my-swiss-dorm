package com.android.mySwissDorm.ui.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.utils.Translator
import java.util.Locale
import kotlinx.coroutines.launch

// Documentation written with the help of AI

/**
 * Extension function on [ViewModel] to handle the translation of a single text field or string.
 *
 * @param text The source string to be translated.
 * @param context The Android context, used for accessing string resources (e.g., loading messages)
 * @param onUpdateTranslating A lambda invoked immediately before translation starts, typically used
 *   to update the UI state with a loading message (e.g., "Translating...").
 * @param onUpdateTranslated A lambda invoked upon successful completion of the translation,
 *   delivering the final translated string back to the ViewModel's state.
 */
fun ViewModel.translateTextField(
    text: String,
    context: Context,
    onUpdateTranslating: (String) -> Unit,
    onUpdateTranslated: (String) -> Unit
) {
  if (text.isNotBlank()) {
    viewModelScope.launch {
      onUpdateTranslating(context.getString(R.string.translator_translating))
      val translated = translateSingleText(text, context)
      onUpdateTranslated(translated)
    }
  }
}

/**
 * Suspended utility function to perform the translation of a single piece of text.
 *
 * @param text The source string to be translated.
 * @param context The Android context required by the [Translator] for internal resource access.
 * @return The translated string.
 */
private suspend fun translateSingleText(text: String, context: Context): String {
  val translator = Translator()
  val code = Locale.getDefault().language
  return translator.use { it.translateText(text, code, context) }
}
