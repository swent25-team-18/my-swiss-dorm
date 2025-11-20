@file:Suppress("unused")

package com.android.mySwissDorm.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.android.mySwissDorm.R
import com.android.mySwissDorm.model.rental.RoomType
import com.android.mySwissDorm.model.residency.Residency
import com.android.mySwissDorm.resources.C
import com.android.mySwissDorm.ui.InputSanitizers.FieldType
import com.android.mySwissDorm.ui.theme.*

/**
 * Uniform, sanitized text fields for MySwissDorm.
 *
 * Usage: var name by remember { mutableStateOf("") } FirstNameField(value = name, onValueChange = {
 * name = it })
 *
 * For submit-time validation, call: val res =
 * InputSanitizers.validateFinal<String>(FieldType.FirstName, name) if (!res.isValid) { /* show
 * error */ }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitizedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    fieldType: InputSanitizers.FieldType,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    // If you want live error display after first blur:
    showErrorOnBlur: Boolean = true,
    // External error (e.g., from submit) overrides blur error if provided:
    externalErrorKey: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    keyboardOptions: KeyboardOptions = defaultKeyboardOptionsFor(fieldType, imeAction),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    testTag: String = defaultTestTag(fieldType),
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {

  val textFieldColors =
      OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MainColor,
          unfocusedBorderColor = OutlineColor,
          focusedLabelColor = MainColor,
          unfocusedLabelColor = OutlineColor,
          cursorColor = TextColor)

  var hasBlurred by remember { mutableStateOf(false) }
  var selection by remember { mutableStateOf(TextRange(value.length)) }

  // Sanitize while typing (cursor kept best-effort at end for simplicity)
  val onSanitizedChange: (String) -> Unit = { raw ->
    val sanitized = InputSanitizers.normalizeWhileTyping(fieldType, raw)
    onValueChange(sanitized)
    selection = TextRange(sanitized.length)
  }

  // Build error state
  val blurErrorKey by
      remember(value, hasBlurred) {
        mutableStateOf(
            if (showErrorOnBlur && hasBlurred) {
              InputSanitizers.validateFinal<Any>(fieldType, value).errorKey
            } else null)
      }
  val errorKey = externalErrorKey ?: blurErrorKey
  val isError = errorKey != null
  val supportingText: @Composable (() -> Unit)? =
      if (isError) {
        { Text(text = mapErrorKeyToText(errorKey!!)) }
      } else null

  OutlinedTextField(
      value = value,
      onValueChange = onSanitizedChange,
      modifier = modifier.fillMaxWidth().testTag(testTag),
      enabled = enabled,
      readOnly = readOnly,
      singleLine = singleLine,
      maxLines = maxLines,
      isError = isError,
      label = label?.let { { Text(it) } },
      placeholder = placeholder?.let { { Text(it) } },
      supportingText = supportingText,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = visualTransformation,
      shape = RoundedCornerShape(16.dp),
      colors = textFieldColors,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon
      // Keep the caret at end after sanitization changes (simple approach)
      // If you use BasicTextField for finer control, port this logic there.
      // textSelectionColors uses theme defaults unless you override.
      )

  // Detect blur by validating when IME moves focus via "Next" or when consumer calls validate
  // If you have a FocusRequester/focus events in your app, you can toggle hasBlurred there.
  LaunchedEffect(imeAction) {
    // no-op placeholder; hook your focus events to set hasBlurred = true
  }

  // Public helper to be called from your focus change listener:
  // fun markBlur() { hasBlurred = true }
}

/* ---------- Field-specific wrappers (consistent look & behavior) ---------- */

@Composable
fun FirstNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.first_name),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.FirstName,
        modifier,
        label = label,
        placeholder = "John",
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next)

@Composable
fun LastNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.last_name),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.LastName,
        modifier,
        label = label,
        placeholder = "Doe",
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next)

@Composable
fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.email_address),
    externalErrorKey: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.Email,
        modifier,
        label = label,
        placeholder = stringResource(R.string.sanitized_text_fields_email_placeholder),
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = imeAction,
        keyboardOptions = defaultKeyboardOptionsFor(FieldType.Email, imeAction))

@Composable
fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.phone),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.Phone,
        modifier,
        label = label,
        placeholder = "791234567",
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next,
        keyboardOptions = defaultKeyboardOptionsFor(FieldType.Phone, ImeAction.Next))

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.search),
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.SearchQuery,
        modifier,
        label = label,
        placeholder = stringResource(R.string.sanitized_text_fields_search_placeholder),
        singleLine = true,
        imeAction = ImeAction.Search,
        leadingIcon = {
          Icon(
              Icons.Default.Search,
              modifier = Modifier.size(30.dp),
              contentDescription = "Search",
              tint = PalePink)
        },
    )

@Composable
fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.title),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.Title,
        modifier,
        label = label,
        placeholder = stringResource(R.string.sanitized_text_fields_title_placeholder),
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next)

@Composable
fun RoomSizeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.sanitized_text_fields_room_size_label),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.RoomSize,
        modifier,
        label = label,
        placeholder = "18.5",
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next,
        keyboardOptions = defaultKeyboardOptionsFor(FieldType.RoomSize, ImeAction.Next))

@Composable
fun PriceField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.sanitized_text_fields_price_label),
    externalErrorKey: String? = null,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.Price,
        modifier,
        label = label,
        placeholder = "1200",
        externalErrorKey = externalErrorKey,
        singleLine = true,
        imeAction = ImeAction.Next,
        keyboardOptions = defaultKeyboardOptionsFor(FieldType.Price, ImeAction.Next))

@Composable
fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.description),
    externalErrorKey: String? = null,
    maxLines: Int = 8,
) =
    SanitizedOutlinedTextField(
        value,
        onValueChange,
        FieldType.Description,
        modifier = modifier.fillMaxWidth().heightIn(min = 140.dp),
        label = label,
        placeholder = stringResource(R.string.sanitized_text_fields_description_placeholder),
        externalErrorKey = externalErrorKey,
        singleLine = false,
        maxLines = maxLines,
        imeAction = ImeAction.Default,
        keyboardOptions = defaultKeyboardOptionsForMultiline())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingTypeDropdown(selected: RoomType?, onSelected: (RoomType) -> Unit, accentColor: Color) {
  var expanded by remember { mutableStateOf(false) }
  val label = selected.toString()

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.sanitized_text_fields_housing_type_label)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        leadingIcon = { Icon(Icons.Default.Apartment, null, tint = MainColor) },
        colors =
            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MainColor, // Focused outline color (Red)
                unfocusedBorderColor = MainColor, // Remove the default border when not focused
                focusedLabelColor = MainColor, // Optional: Change label color when focused
                unfocusedLabelColor = Color.Gray // Optional: Change label color when not focused
                ),
        modifier = Modifier.menuAnchor().fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      RoomType.entries.forEach { type ->
        DropdownMenuItem(
            text = { Text(type.toString()) },
            onClick = {
              onSelected(type)
              expanded = false
            })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidencyDropdownResID(
    selected: String?,
    onSelected: (String) -> Unit,
    accentColor: Color,
    residencies: List<Residency>,
    isListing: Boolean,
    modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  val label =
      selected?.toString() ?: stringResource(R.string.sanitized_text_fields_select_residency_label)

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = Modifier.testTag(C.SanitizedResidencyDropdownTags.RESIDENCY_DROPDOWN_BOX)) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.sanitized_text_fields_residency_name_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = { Icon(Icons.Default.Home, null, tint = MainColor) },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MainColor,
                    unfocusedBorderColor = MainColor,
                    focusedLabelColor = MainColor,
                    unfocusedLabelColor = Color.Gray),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          residencies.forEach { residency ->
            DropdownMenuItem(
                modifier =
                    Modifier.testTag(
                        C.SanitizedResidencyDropdownTags.getResidencyTag(residency.name)),
                text = { Text(residency.name) },
                onClick = {
                  onSelected(residency.name)
                  expanded = false
                })
          }
          if (isListing) {
            DropdownMenuItem(
                modifier = Modifier.testTag(C.SanitizedResidencyDropdownTags.PRIVATE_ACCOMMODATION),
                text = { Text("Private Accommodation") },
                onClick = {
                  onSelected("Private Accommodation")
                  expanded = false
                })
          }
        }
      }
}

/* ---------- Helpers ---------- */

private fun defaultTestTag(type: FieldType): String =
    when (type) {
      FieldType.FirstName -> "MSD_Text_FirstName"
      FieldType.LastName -> "MSD_Text_LastName"
      FieldType.Email -> "MSD_Text_Email"
      FieldType.Phone -> "MSD_Text_Phone"
      FieldType.SearchQuery -> "MSD_Text_Search"
      FieldType.Title -> "MSD_Text_Title"
      FieldType.RoomSize -> "MSD_Text_RoomSize"
      FieldType.Description -> "MSD_Text_Description"
      FieldType.Price -> "MSD_Text_Price"
      FieldType.City -> "MSD_Text_City"
      FieldType.Website -> "MSD_Text_Website"
    }

/** Map your error keys to human text (or delegate to string resources). */
@Composable
private fun mapErrorKeyToText(key: String): String {
  // Example; wire to stringResource(...) if you have i18n.
  return when (key) {
    "error.firstname.required" -> stringResource(R.string.sanitized_text_fields_first_name_required)
    "error.firstname.length" -> stringResource(R.string.sanitized_text_fields_first_name_length)
    "error.firstname.format" -> stringResource(R.string.sanitized_text_fields_first_name_format)
    "error.lastname.required" -> stringResource(R.string.sanitized_text_fields_last_name_required)
    "error.lastname.length" -> stringResource(R.string.sanitized_text_fields_last_name_length)
    "error.lastname.format" -> stringResource(R.string.sanitized_text_fields_last_name_format)
    "error.phone.required" -> stringResource(R.string.sanitized_text_fields_phone_required)
    "error.phone.length" -> stringResource(R.string.sanitized_text_fields_phone_length)
    "error.phone.leadingZero" -> stringResource(R.string.sanitized_text_fields_phone_leading_zero)
    "error.phone.format" -> stringResource(R.string.sanitized_text_fields_phone_format)
    "error.email.required" -> stringResource(R.string.sanitized_text_fields_email_required)
    "error.email.length" -> stringResource(R.string.sanitized_text_fields_email_length)
    "error.email.format" -> stringResource(R.string.sanitized_text_fields_email_format)
    "error.search.required" -> stringResource(R.string.sanitized_text_fields_search_required)
    "error.title.required" -> stringResource(R.string.sanitized_text_fields_title_required)
    "error.title.length" -> stringResource(R.string.sanitized_text_fields_title_length)
    "error.roomsize.format" -> stringResource(R.string.sanitized_text_fields_room_size_format)
    "error.roomsize.range" -> stringResource(R.string.sanitized_text_fields_room_size_range)
    "error.description.length" -> stringResource(R.string.sanitized_text_fields_description_length)
    "error.price.required" -> stringResource(R.string.sanitized_text_fields_price_required)
    "error.price.leadingZero" -> stringResource(R.string.sanitized_text_fields_price_leading_zero)
    "error.price.range" -> stringResource(R.string.sanitized_text_fields_price_range)
    else -> key // fallback
  }
}

private fun defaultKeyboardOptionsFor(type: FieldType, ime: ImeAction): KeyboardOptions =
    when (type) {
      FieldType.Email -> KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ime)
      FieldType.Phone,
      FieldType.Price -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ime)
      FieldType.RoomSize -> KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ime)
      else -> KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ime)
    }

private fun defaultKeyboardOptionsForMultiline(): KeyboardOptions =
    KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
