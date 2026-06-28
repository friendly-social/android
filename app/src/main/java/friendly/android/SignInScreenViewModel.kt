package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.android.SignInScreenEvent.SnackbarEvent
import friendly.sdk.Email
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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
    private val signIn: SignInUseCase,
    private val sendCode: SendEmailAuthVerificationCodeUseCase,
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

    private val _events = MutableSharedFlow<SignInScreenEvent>()
    val events = _events.shareIn(viewModelScope, Eagerly)

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
                val result = sendCode(Email.orThrow(_state.value.email.value))
                if (result is Failure) {
                    _events.emit(SnackbarEvent.CodeSendingFailure)
                }
            }
        }
    }

    fun onCodeConfirmationResult(result: EmailCodeLoginState) {
        viewModelScope.launch {
            when (result.success) {
                true -> {
                    _state.update { old -> old.copy(isLoading = true) }
                    val authorization = result.authorization
                        ?: error("Authorization can not be empty")
                    val signInResult = signIn(authorization)

                    when (signInResult) {
                        is SignInUseCase.Result.IOError,
                        is SignInUseCase.Result.ServerError,
                        is SignInUseCase.Result.Unauthorized,
                        -> {
                            _events.emit(SnackbarEvent.SignInFailure)
                        }
                        is SignInUseCase.Result.Success -> {
                            _state.update { old -> old.copy(isLoading = false) }
                            _events.emit(SnackbarEvent.SignInSuccess)
                            _events.emit(SignInScreenEvent.SignInSuccess)
                        }
                    }
                }

                false -> {
                    _events.emit(SnackbarEvent.CodeConfirmationFailure)
                }
            }
        }
    }
}
