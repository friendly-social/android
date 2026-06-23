package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class SignInScreenVmState(
    val email: ValidatableField<String>,
    val isLoading: Boolean,
) {
    fun toUiState(): SignInScreenUiState {
        if (isLoading) return Loading
        return SignInScreenUiState.Idle(email = email)
    }
}

class SignInScreenViewModel(
    private val verify: SendEmailAuthVerificationCodeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SignInScreenVmState(
            email = ValidatableField(value = ""),
            isLoading = false,
        ),
    )

    val state = _state
        .map(SignInScreenVmState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = Eagerly,
            initialValue = SignInScreenUiState.Idle(ValidatableField("")),
        )

    fun onEmail(new: String) {
        _state.update { previous ->
            previous.copy(
                email = ValidatableField(
                    value = new,
                    isValid = Email.validate(new),
                ),
            )
        }
    }

    fun sendConfirmationCode() {
        if (_state.value.email.isValid) {
            viewModelScope.launch {
                verify(Email.orThrow(_state.value.email.value))
            }
        }
    }

    fun onSuccessfulCodeConfirmation() {

    }
}
