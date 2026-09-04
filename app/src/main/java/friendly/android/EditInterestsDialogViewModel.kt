package friendly.android

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.Home.EditInterestsDialog
import friendly.sdk.Interest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditInterestsDialogViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<EditInterestsDialog>()
    private val initialInterests = route.interests

    data class EditInterestsDialogVmState(
        val pickedInterests: List<String>,
        val currentInterestValue: String? = null,
        val currentInterestIndex: Int? = null,
        val editingInterest: Boolean = false,
    ) {
        fun toUiState(
            initialInterests: List<String>,
        ): EditInterestsDialogUiState {
            val refinedInterests = pickedInterests.filterNonBlank()
            val isSavable = refinedInterests != initialInterests
            return when (editingInterest) {
                false -> {
                    EditInterestsDialogUiState.Idle(
                        pickedInterests = pickedInterests,
                        isSavable = isSavable,
                    )
                }

                true -> {
                    EditInterestsDialogUiState.EditingInterest(
                        pickedInterests = pickedInterests,
                        isSavable = isSavable,
                        interestValue = currentInterestValue ?: "",
                        interestValid = currentInterestValue
                            ?.let(Interest::validate)
                            ?: false,
                    )
                }
            }
        }
    }

    private val _state = MutableStateFlow(
        EditInterestsDialogVmState(
            pickedInterests = initialInterests,
        ),
    )

    val state: StateFlow<EditInterestsDialogUiState> = _state
        .map { vmState -> vmState.toUiState(initialInterests) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EditInterestsDialogUiState.Idle(
                pickedInterests = initialInterests,
                isSavable = false,
            ),
        )

    private val _events = MutableSharedFlow<EditInterestsDialogUiEvents>()

    val events: SharedFlow<EditInterestsDialogUiEvents> = _events
        .shareIn(viewModelScope, Eagerly)

    fun onNewInterestValue(new: String) {
        _state.update { old -> old.copy(currentInterestValue = new) }
    }

    fun onDiscardInterest() {
        _state.update { old ->
            old.copy(
                currentInterestIndex = null,
                currentInterestValue = null,
                editingInterest = false,
            )
        }
    }

    fun onRemoveInterest(index: Int) {
        _state.update { old ->
            val newPickedInterest = old.pickedInterests
                .filterIndexed { currentIndex, _ -> currentIndex != index }
            old.copy(pickedInterests = newPickedInterest)
        }
    }

    fun onSaveInterests() {
        val refinedInterests = _state.value.pickedInterests.filterNonBlank()
        val isSavable = refinedInterests != initialInterests
        if (!isSavable) return

        viewModelScope.launch {
            _events.emit(
                EditInterestsDialogUiEvents.ConfirmSuccess(refinedInterests),
            )
        }
    }

    fun editInterest(index: Int) {
        _state.update { old ->
            old.copy(
                currentInterestIndex = index,
                currentInterestValue = old.pickedInterests[index],
                editingInterest = true,
            )
        }
    }

    fun createNewInterest() {
        _state.update { old ->
            old.copy(
                currentInterestIndex = old.pickedInterests.lastIndex + 1,
                currentInterestValue = "",
                editingInterest = true,
            )
        }
    }

    fun onConfirmInterest() {
        val index = _state.value.currentInterestIndex
        val value = _state.value.currentInterestValue

        require(index != null)
        require(value != null)

        _state.update { old ->
            old.copy(
                pickedInterests = old.pickedInterests.setOrAppend(index, value),
                currentInterestIndex = null,
                currentInterestValue = null,
                editingInterest = false,
            )
        }
    }
}

private fun <T> List<T>.setOrAppend(
    index: Int,
    value: T,
): List<T> {
    require(index in 0..size)
    return toMutableList().apply {
        if (index >= size) {
            add(value)
        } else {
            this[index] = value
        }
    }
}

private fun List<String>.filterNonBlank(): List<String> =
    this.filterNot(String::isBlank)
