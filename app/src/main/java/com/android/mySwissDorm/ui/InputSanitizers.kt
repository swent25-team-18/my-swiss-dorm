@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.android.mySwissDorm.ui

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/**
 * Centralized normalization + validation for MySwissDorm inputs.
 *
 * Usage in VM (typing): val clean = InputSanitizers.normalizeWhileTyping(FieldType.FirstName, raw)
 *
 * Usage in VM (submit): val res = InputSanitizers.validateFinal<String>(FieldType.FirstName, clean)
 * if (!res.isValid) showError(res.errorKey)
 */
object InputSanitizers {

  // ---------- Public API ----------

  sealed class FieldType {
    // Auth / profile
    data object FirstName : FieldType()

    data object LastName : FieldType()

    data object Phone : FieldType()

    data object Email : FieldType()

    // Search
    data object SearchQuery : FieldType()

    // Listing
    data object Title : FieldType() // Listing title

    data object RoomSize : FieldType() // Double ∈ [1.0, 1000.0], exactly one decimal

    data object Description : FieldType() // Multiline free text (≤ 5000)

    // Kept for reuse elsewhere (you used it before)
    data object Price : FieldType() // Int ∈ [1, 10000], no leading zeros
  }

  data class ValidationResult<T>(val value: T?, val errorKey: String? = null) {
    val isValid: Boolean
      get() = value != null && errorKey == null
  }

  /** Normalize user text while typing (for UX friendliness). */
  fun normalizeWhileTyping(type: FieldType, raw: String): String =
      when (type) {
        FieldType.FirstName -> normalizeNameTyping(raw, NAME_MAX)
        FieldType.LastName -> normalizeNameTyping(raw, NAME_MAX)
        FieldType.Phone -> normalizePhoneTyping(raw)
        FieldType.Email -> normalizeEmailTyping(raw)
        FieldType.SearchQuery -> normalizeSearchTyping(raw)
        FieldType.Title -> normalizeTitleTyping(raw)
        FieldType.RoomSize -> normalizeDecimalTyping(raw) // one decimal, range enforced at submit
        FieldType.Description -> normalizeDescriptionTyping(raw)
        FieldType.Price -> normalizePriceTyping(raw)
      }

  /** Strict submit-time validation → returns typed values (String / Int / Double). */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> validateFinal(type: FieldType, raw: String): ValidationResult<T> =
      when (type) {
        FieldType.FirstName -> validateNameFinal(raw, "error.firstname") as ValidationResult<T>
        FieldType.LastName -> validateNameFinal(raw, "error.lastname") as ValidationResult<T>
        FieldType.Phone -> validatePhoneFinal(raw) as ValidationResult<T>
        FieldType.Email -> validateEmailFinal(raw) as ValidationResult<T>
        FieldType.SearchQuery -> validateSearchFinal(raw) as ValidationResult<T>
        FieldType.Title -> validateTitleFinal(raw) as ValidationResult<T>
        FieldType.RoomSize -> validateRoomSizeFinal(raw) as ValidationResult<T>
        FieldType.Description -> validateDescriptionFinal(raw) as ValidationResult<T>
        FieldType.Price -> validatePriceFinal(raw) as ValidationResult<T>
      }

  // Dropdowns / non-text helpers
  fun <T> validateSelection(
      value: T?,
      allowed: Set<T>,
      errorKey: String = "error.selection.invalid"
  ): ValidationResult<T> {
    return if (value != null && value in allowed) ValidationResult(value)
    else ValidationResult(null, errorKey)
  }

  fun validateLatLng(lat: Double?, lng: Double?): ValidationResult<Pair<Double, Double>> {
    if (lat == null || lng == null) return ValidationResult(null, "error.location.required")
    if (lat !in -90.0..90.0) return ValidationResult(null, "error.location.latRange")
    if (lng !in -180.0..180.0) return ValidationResult(null, "error.location.lngRange")
    return ValidationResult(lat to lng)
  }

  // ---------- Constants & regex ----------

  private const val NAME_MAX = 20
  private const val TITLE_MAX = 100
  private const val RESIDENCY_MAX = 100
  private const val DESCRIPTION_MAX = 5000
  private const val SEARCH_MAX = 100
  private const val EMAIL_MAX = 254

  private const val PRICE_MIN = 1
  private const val PRICE_MAX = 10_000

  private const val DEC_MIN = 1.0
  private const val DEC_MAX = 1000.0

  private const val PHONE_LEN = 9

  private val CONTROL_REGEX = Regex("\\p{C}+")
  private val WHITESPACE_REGEX = Regex("\\s+")
  private val NAME_FINAL_REGEX =
      Regex("^[\\p{L}](?:[\\p{L}\\s\\-']*[\\p{L}])?$") // starts & ends with letter
  private val RES_ALLOWED_CHARS = Regex("[^\\p{L}\\p{N}\\s.,'()\\-/&]") // remove disallowed
  private val TITLE_ALLOWED_CHARS =
      Regex("[^\\p{L}\\p{N}\\s.,'()\\-\\/&:+]") // broader than residency
  private val DECIMAL_ONE_PLACE = Regex("^\\d{1,4}\\.\\d$") // up to 4 int digits + '.' + 1 frac

  private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  // ---------- Name (First/Last) ----------

  private fun normalizeNameTyping(raw: String, max: Int): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    // keep letters, spaces, hyphen, apostrophe
    val filtered =
        buildString(nfc.length) {
          for (ch in nfc) if (ch.isLetter() || ch == ' ' || ch == '-' || ch == '\'') append(ch)
        }
    val collapsed = WHITESPACE_REGEX.replace(filtered, " ").trim()
    return collapsed.take(max)
  }

  private fun validateNameFinal(raw: String, keyPrefix: String): ValidationResult<String> {
    val s = normalizeNameTyping(raw, NAME_MAX).trim()
    if (s.isEmpty()) return ValidationResult(null, "$keyPrefix.required")
    if (s.length > NAME_MAX) return ValidationResult(null, "$keyPrefix.length")
    if (!NAME_FINAL_REGEX.matches(s)) return ValidationResult(null, "$keyPrefix.format")
    return ValidationResult(s)
  }

  // ---------- Phone (exactly 9 digits, no leading zero) ----------

  fun normalizePhoneTyping(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    val noLeading = digits.dropWhile { it == '0' }
    val trimmed = if (noLeading.isEmpty()) "" else noLeading
    return trimmed.take(PHONE_LEN)
  }

  private fun validatePhoneFinal(raw: String): ValidationResult<String> {
    val s = normalizePhoneTyping(raw)
    if (s.isEmpty()) return ValidationResult(null, "error.phone.required")
    if (s.length != PHONE_LEN) return ValidationResult(null, "error.phone.length")
    if (s.first() == '0') return ValidationResult(null, "error.phone.leadingZero")
    if (!s.all(Char::isDigit)) return ValidationResult(null, "error.phone.format")
    return ValidationResult(s)
  }

  // ---------- Email ----------

  fun normalizeEmailTyping(raw: String): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    val noCtrl = CONTROL_REGEX.replace(nfc, "")
    val trimmed = noCtrl.trim()
    val oneLine = trimmed.replace('\n', ' ').replace('\r', ' ')
    val collapsed = WHITESPACE_REGEX.replace(oneLine, " ")
    val atIdx = collapsed.indexOf('@')
    if (atIdx <= 0 || atIdx == collapsed.lastIndex) return collapsed.take(EMAIL_MAX)
    val local = collapsed.substring(0, atIdx)
    val domain = collapsed.substring(atIdx + 1).lowercase(Locale.ROOT)
    return "$local@$domain".take(EMAIL_MAX)
  }

  private fun validateEmailFinal(raw: String): ValidationResult<String> {
    val s = normalizeEmailTyping(raw).replace(" ", "")
    if (s.isEmpty()) return ValidationResult(null, "error.email.required")
    if (s.length > EMAIL_MAX) return ValidationResult(null, "error.email.length")
    if (!EMAIL_REGEX.matches(s)) return ValidationResult(null, "error.email.format")
    return ValidationResult(s)
  }

  // ---------- Search query ----------

  private fun normalizeSearchTyping(raw: String): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    val noCtrl = CONTROL_REGEX.replace(nfc, "")
    val collapsed = WHITESPACE_REGEX.replace(noCtrl, " ").trim()
    return collapsed.take(SEARCH_MAX)
  }

  private fun validateSearchFinal(raw: String): ValidationResult<String> {
    val s = normalizeSearchTyping(raw)
    if (s.isEmpty()) return ValidationResult(null, "error.search.required")
    return ValidationResult(s)
  }

  /** Accent-insensitive token for search (store alongside the raw if needed). */
  fun toSearchKey(input: String): String {
    val base = normalizeSearchTyping(input).lowercase(Locale.ROOT)
    val nfd = Normalizer.normalize(base, Normalizer.Form.NFD)
    val noDiacritics = nfd.replace(Regex("\\p{M}+"), "")
    return WHITESPACE_REGEX.replace(noDiacritics, " ").trim()
  }

  // ---------- Title (Listing title) ----------

  private fun normalizeTitleTyping(raw: String): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    val noCtrl = CONTROL_REGEX.replace(nfc, "")
    val filtered = TITLE_ALLOWED_CHARS.replace(noCtrl, "") // keep only allowed chars

    // Replace any newlines with a space (title is single-line)
    val singleLine = filtered.replace('\n', ' ')

    // Collapse horizontal whitespace runs of length >= 2 to a single space,
    // but keep single spaces (incl. trailing) while typing.
    val collapsed = singleLine.replace(Regex("[ \\t\\x0B\\f\\r]{2,}"), " ")

    // Avoid leading spaces while typing (optional, nicer UX), but DO NOT trim end.
    val noLeading = collapsed.trimStart()

    // Do NOT .trim() here; let the user keep a trailing space while typing.
    return noLeading.take(TITLE_MAX)
  }

  private fun validateTitleFinal(raw: String): ValidationResult<String> {
    // Final cleanup: trim edges, then enforce limits.
    val s = normalizeTitleTyping(raw).trim()
    if (s.isEmpty()) return ValidationResult(null, "error.title.required")
    if (s.length > TITLE_MAX) return ValidationResult(null, "error.title.length")
    return ValidationResult(s)
  }

  // ---------- Description (multiline free text) ----------
  private val CONTROL_EXCEPT_NL =
      Regex("\\p{Cc}&&[^\\n]") // Java-style character class intersection

  private fun normalizeDescriptionTyping(raw: String): String {
    val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
    val noCtrl = CONTROL_EXCEPT_NL.replace(nfc, "")
    // Cap 3+ newlines to 2
    val twoNewlinesMax = noCtrl.replace(Regex("\n{3,}"), "\n\n")
    // Collapse horizontal whitespace runs of length >= 2 to a single space,
    // but keep single spaces (esp. trailing) while typing.
    val collapsedSpaces = twoNewlinesMax.replace(Regex("[ \\t\\x0B\\f\\r]{2,}"), " ")
    // IMPORTANT: no trim() here during typing
    return collapsedSpaces.take(DESCRIPTION_MAX)
  }

  private fun validateDescriptionFinal(raw: String): ValidationResult<String> {
    // Final pass may trim
    val s = normalizeDescriptionTyping(raw).trim()
    if (s.length > DESCRIPTION_MAX) {
      return ValidationResult(null, "error.description.length")
    }
    // empty allowed; enforce "required" at screen/submit level if you want
    return ValidationResult(s)
  }

  // ---------- Room size (Double ∈ [1.0, 1000.0], exactly one decimal) ----------

  private fun normalizeDecimalTyping(raw: String): String {
    if (raw.isEmpty()) return ""
    val filtered =
        buildString(raw.length) {
          var dotSeen = false
          for (ch in raw) {
            when {
              ch.isDigit() -> append(ch)
              ch == '.' && !dotSeen -> {
                append('.')
                dotSeen = true
              }
            }
          }
        }
    if (filtered.isEmpty()) return ""

    val dotIdx = filtered.indexOf('.')
    if (dotIdx == -1) {
      val intPart = filtered.dropWhile { it == '0' }
      return intPart
    }

    val intPart = filtered.substring(0, dotIdx).dropWhile { it == '0' }
    val fracPart = filtered.substring(dotIdx + 1)
    val boundedInt = intPart.take(4) // up to 1000
    val oneDigitFrac = fracPart.take(1)
    val composed = (if (boundedInt.isEmpty()) "0" else boundedInt) + "." + oneDigitFrac

    val asDouble = composed.toDoubleOrNull()
    return when {
      asDouble == null -> composed
      asDouble > DEC_MAX -> "1000.0"
      else -> composed
    }
  }

  private fun validateRoomSizeFinal(raw: String): ValidationResult<Double> {
    val s = normalizeDecimalTyping(raw)
    if (!DECIMAL_ONE_PLACE.matches(s)) {
      val digits = raw.filter(Char::isDigit)
      val coerced =
          if (digits.isNotEmpty()) {
            val intPart = digits.dropWhile { it == '0' }.ifEmpty { "0" }.take(4)
            "$intPart.0"
          } else ""
      if (!DECIMAL_ONE_PLACE.matches(coerced))
          return ValidationResult(null, "error.roomsize.format")
      val v = coerced.toDouble()
      if (v < DEC_MIN || v > DEC_MAX) return ValidationResult(null, "error.roomsize.range")
      return ValidationResult(v)
    }
    val v = s.toDouble()
    if (v < DEC_MIN || v > DEC_MAX) return ValidationResult(null, "error.roomsize.range")
    return ValidationResult(v)
  }

  // ---------- Price (Int ∈ [1, 10000], no leading zeros) ----------

  private fun normalizePriceTyping(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    val noLeading = digits.dropWhile { it == '0' }
    val trimmed = if (noLeading.isEmpty()) "" else noLeading
    val cut = trimmed.take(5) // up to 10000
    val num = cut.toIntOrNull() ?: return ""
    return min(num, PRICE_MAX).toString()
  }

  private fun validatePriceFinal(raw: String): ValidationResult<Int> {
    val normalized = normalizePriceTyping(raw)
    val num = normalized.toIntOrNull() ?: return ValidationResult(null, "error.price.required")
    if (normalized.startsWith("0")) return ValidationResult(null, "error.price.leadingZero")
    if (num < PRICE_MIN || num > PRICE_MAX) return ValidationResult(null, "error.price.range")
    return ValidationResult(num)
  }
}
