package friendly.android

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.Home.EditInterestsDialog
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

class EditInterestsDialogViewModel(savedStateHandle: SavedStateHandle) :
    ViewModel() {
    private val route = savedStateHandle.toRoute<EditInterestsDialog>()
    private val initialInterests = route.interests

    data class EditInterestsDialogVmState(val pickedInterests: List<String>) {
        fun toUiState(
            initialInterests: List<String>,
        ): EditInterestsDialogUiState {
            val refinedInterests = pickedInterests.filterNonBlank()
            val isSavable = refinedInterests != initialInterests
            return EditInterestsDialogUiState(
                pickedInterests = pickedInterests,
                isSavable = isSavable,
            )
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
            initialValue = EditInterestsDialogUiState(
                pickedInterests = initialInterests,
                isSavable = false,
            ),
        )

    private val _events = MutableSharedFlow<EditInterestsDialogUiEvents>()

    val events: SharedFlow<EditInterestsDialogUiEvents> = _events
        .shareIn(viewModelScope, Eagerly)

    fun onAddNewInterest() {
        _state.update { old ->
            old.copy(
                pickedInterests = old.pickedInterests.plus(""),
            )
        }
    }

    fun onNewInterestValue(index: Int, new: String) {
        _state.update { old ->
            val newPickedInterests = old.pickedInterests
                .toMutableList()
                .apply { this[index] = new }
            old.copy(pickedInterests = newPickedInterests)
        }
    }

    fun onRemoveInterest(index: Int) {
        _state.update { old ->
            val newPickedInterest = old.pickedInterests
                .filterIndexed { currentIndex, _ -> currentIndex != index }
            old.copy(pickedInterests = newPickedInterest)
        }
    }

    fun onConfirmInterests() {
        val refinedInterests = _state.value.pickedInterests.filterNonBlank()
        val isSavable = refinedInterests != initialInterests
        if (!isSavable) return

        viewModelScope.launch {
            _events.emit(
                EditInterestsDialogUiEvents.ConfirmSuccess(refinedInterests),
            )
        }
    }
}

private fun List<String>.filterNonBlank(): List<String> =
    this.filterNot(String::isBlank)
