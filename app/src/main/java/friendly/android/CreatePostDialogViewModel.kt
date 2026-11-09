package friendly.android

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.android.FriendlyNavGraph.Home.CreatePostDialog
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class CreatePostDialogVmState(
    val saved: Boolean,
    val isSending: Boolean,
) {
    fun toUiState(): CreatePostDialogUiState = CreatePostDialogUiState(
        saved = saved,
        isSending = isSending,
    )
}

class CreatePostDialogViewModel(
    savedStateHandle: SavedStateHandle,
    private val create: CreatePostUseCase,
    private val draft: PostDraftStorage,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CreatePostDialog>(
        typeMap = CreatePostDialogTypeMap,
    )

    val replyTo: CommunityPostDescriptor? = route.replyTo?.typed()

    val initialText: String = draft.get() ?: ""

    val postTextState = TextFieldState(initialText)

    private val _state = MutableStateFlow(
        CreatePostDialogVmState(
            saved = draft.get() != null,
            isSending = false,
        ),
    )

    val state: StateFlow<CreatePostDialogUiState> = _state
        .map(CreatePostDialogVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = Eagerly,
            CreatePostDialogUiState(
                saved = false,
                isSending = false,
            ),
        )

    fun onAttachMedia(
        // TODO
    ) {
    }

    fun onPostTextChange() {
        if (postTextState.text.toString().isBlank()) {
            draft.clear()
        }
        _state.update { it.copy(saved = false) }
    }

    fun saveDraft() {
        draft.store(postTextState.text.toString())
        _state.update { it.copy(saved = true) }
    }

    fun create(onCreate: () -> Unit) {
        if (CommunityPostText.validate(postTextState.string)) {
            val text = CommunityPostText.orThrow(postTextState.string)
            viewModelScope.launch {
                _state.update { it.copy(isSending = true) }
                create(text = text, replyTo = replyTo)
                draft.clear()
                // todo do some events here
                onCreate()
            }
        }
    }
}
