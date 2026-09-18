package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.ActivityDetails
import friendly.sdk.FriendlyClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class ActivityScreenVmState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isNetworkError: Boolean,
    val activity: List<ActivityDetails>?,
) {
    fun toUiState(): ActivityScreenUiState {
        if (isLoading) return Loading
        if (isNetworkError) {
            return ActivityScreenUiState.NetworkError(isRefreshing)
        }
        if (activity != null) {
            return ActivityScreenUiState
                .Idle(
                    activity = activity,
                    isRefreshing = isRefreshing,
                )
        }
        error("undefined state? idk") // todo idk how should i failfast here...
    }
}

class ActivityScreenViewModel(
    private val authStorage: AuthStorage,
    private val client: FriendlyClient,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ActivityScreenVmState(
            isLoading = true,
            isNetworkError = false,
            activity = null,
            isRefreshing = false,
        ),
    )

    val state = _state
        .map(ActivityScreenVmState::toUiState)
        .stateIn(
            viewModelScope, Eagerly,
            ActivityScreenUiState.Idle(
                listOf(), false,
            ),
        )

    fun load() {
        viewModelScope.launch {
            val auth = authStorage.getAuth()
            val list = client.activity.list(auth, null)
            val activity = list.orThrow()
            activity.nextId.let(::println) // todo
            _state.update { old ->
                old.copy(
                    isLoading = false,
                    activity = activity.data,
                )
            }
        }
    }

    fun refresh() {
        _state.update {
            it.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            val auth = authStorage.getAuth()
            val list = client.activity.list(auth, null)
            val activity = list.orThrow()
            activity.nextId.let(::println) // todo
            _state.update { old ->
                old.copy(
                    isRefreshing = false,
                    activity = activity.data,
                )
            }
        }
    }
}
