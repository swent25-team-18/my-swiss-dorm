package com.android.mySwissDorm.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.android.mySwissDorm.R
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.theme.Dimens
import com.android.mySwissDorm.ui.theme.MainColor

/**
 * A Text composable that can be translated, by clicking on the "Translate" text below it.
 *
 * @param text The string to be displayed and translated.
 * @param viewModel The ViewModel that takes care of the translation.
 *
 * The other parameters are just transitive to the wrapped [Text]
 */
@Composable
fun TranslatableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    viewModel: TranslatableTextViewModel = remember { TranslatableTextViewModel() }
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsState()
  var isTranslated by remember { mutableStateOf(false) }

  // Translate the text each time the input 'text' changes
  LaunchedEffect(text) { viewModel.translate(text, context) }

  // Determines which version to display : original or translated
  val textToDisplay = if (isTranslated) state.translated else text
  Column {
    // Text to display, original or translated
    Text(
        text = textToDisplay,
        modifier = modifier.testTag(C.TranslatableTextTestTags.TRANSLATABLE_TEXT),
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style)

    // Translation toggle button, visible only if the text input is not blank
    if (text.isNotBlank()) {
      Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))

      // Either displays "Translate" or "See original"
      val clickableText =
          if (isTranslated) {
            context.getString(R.string.translatable_text_see_original_button)
          } else {
            context.getString(R.string.translatable_text_translate_button)
          }
      Text(
          text = clickableText,
          modifier =
              Modifier.clickable(onClick = { isTranslated = !isTranslated })
                  .testTag(C.TranslatableTextTestTags.TRANSLATE_BUTTON),
          color = MainColor)
    }
  }
}
