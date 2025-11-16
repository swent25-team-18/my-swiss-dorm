package com.android.mySwissDorm.ui.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mySwissDorm.model.residency.ResidenciesRepository
import com.android.mySwissDorm.model.residency.Residency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * An interface for UI state data classes used in "Add" forms. It ensures that the state contains a
 * list for 'residencies' and a property for 'errorMsg', which the abstract ViewModel will manage.
 */
interface AddFormUiState {
  val residencies: List<Residency>
  val errorMsg: String?
}

/**
 * Abstract ViewModel for "Add" forms (like AddListing and AddReview).
 *
 * It centralizes:
 * - UI state flow management (`_uiState`, `uiState`).
 * - The common `ResidenciesRepository` dependency.
 * - Loading residencies during `init`.
 * - Common error handling functions (`setErrorMsg`, `clearErrorMsg`).
 *
 * @param T The type of the UI state, which must implement [AddFormUiState].
 * @param initialState The initial value for the UI state.
 * @property residenciesRepository The repository to fetch residencies.
 */
abstract class AbstractAddFormViewModel<T : AddFormUiState>(
    protected val residenciesRepository: ResidenciesRepository,
    initialState: T
) : ViewModel() {

  // Protected mutable state for subclasses to update
  protected val _uiState = MutableStateFlow(initialState)

  // Public-facing immutable state for the UI to observe
  val uiState: StateFlow<T> = _uiState.asStateFlow()

  /**
   * Abstract function for child classes to implement. This is how the child tells the base class
   * how to update its specific state data class with an error message.
   */
  protected abstract fun updateStateWithError(errorMsg: String?)

  /**
   * Abstract function for child classes to implement. This is how the child tells the base class
   * how to update its specific state data class with the list of residencies.
   */
  protected abstract fun updateStateWithResidencies(residencies: List<Residency>)

  init {
    loadResidencies()
  }

  /**
   * Loads the list of residencies from the repository and updates the UI state using the abstract
   * helper functions.
   */
  private fun loadResidencies() {
    viewModelScope.launch {
      try {
        val residencies = residenciesRepository.getAllResidencies()
        updateStateWithResidencies(residencies)
      } catch (e: Exception) {
        updateStateWithError(e.message ?: "Failed to load residencies")
      }
    }
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    updateStateWithError(null)
  }

  /** Sets an error message in the UI state. */
  protected fun setErrorMsg(errorMsg: String) {
    updateStateWithError(errorMsg)
  }
}
