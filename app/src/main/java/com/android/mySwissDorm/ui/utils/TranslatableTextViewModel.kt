package com.android.mySwissDorm.ui.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.R
import com.android.mySwissDorm.utils.Translator
import java.util.Locale
import kotlin.use
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class representing the state of the TranslatableText UI component.
 *
 * @property translated The translated version of the input text. Defaults to an empty string.
 */
data class TranslatableTextUIState(
    val translated: String = "",
)

/**
 * ViewModel responsible for handling the asynchronous text translation logic for the
 * [TranslatableText] Composable.
 *
 * It manages the state of the translated text, providing it to the UI via a [StateFlow].
 */
class TranslatableTextViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(TranslatableTextUIState())
  val uiState: StateFlow<TranslatableTextUIState> = _uiState.asStateFlow()

  /**
   * Translates the given text asynchronously and updates the [uiState] with the result.
   *
   * @param text The source string to be translated.
   * @param context The Android context, typically used here for resource access within the
   *   Translator utility.
   */
  fun translate(text: String, context: Context) {
    if (text.isNotBlank()) {
      viewModelScope.launch {
        _uiState.update { it.copy(translated = context.getString(R.string.translator_translating)) }
        val translator = Translator()
        val code = Locale.getDefault().language
        val translated = translator.use { translator.translateText(text, code, context) }
        _uiState.update { it.copy(translated = translated) }
      }
    }
  }
}
