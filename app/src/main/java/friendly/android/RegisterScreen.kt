package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RegisterState(
    val nickname: String,
    val description: String,
    val availableInterests: List<Interest>,
    val pickedInterests: List<Interest>,
    val isGenerating: Boolean,
) {
    fun toUiState(): RegisterUiState {
        if (isGenerating) return RegisterUiState.Generating
        return RegisterUiState.Editing(
            nickname = nickname,
            description = description,
            isValid = nickname.isNotBlank() &&
                description.isNotBlank(), // todo add validators
            availableInterests = availableInterests,
            pickedInterests = pickedInterests,
        )
    }
}

sealed interface RegisterUiState {
    data class Editing(
        val availableInterests: List<Interest>,
        val pickedInterests: List<Interest>,
        val nickname: String,
        val description: String,
        val isValid: Boolean,
    ) : RegisterUiState

    data object Generating : RegisterUiState
}

class RegisterScreenViewModel(private val register: RegisterUseCase) :
    ViewModel() {
    private val _state = MutableStateFlow(
        value = RegisterState(
            nickname = "",
            description = "",
            availableInterests = interests,
            pickedInterests = listOf(),
            isGenerating = false,
        ),
    )
    val state = _state
        .map(RegisterState::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RegisterUiState.Editing(
                nickname = "",
                description = "",
                isValid = false,
                availableInterests = interests,
                pickedInterests = listOf(),
            ),
        )

    fun updateNickname(new: String) {
        _state.update {
            it.copy(
                nickname = new,
            )
        }
    }

    fun updateDescription(new: String) {
        _state.update {
            it.copy(
                description = new,
            )
        }
    }

    fun toggleInterest(interest: Interest) {
        val picked = interest in _state.value.pickedInterests
        if (picked) {
            _state.update {
                it.copy(
                    pickedInterests = it.pickedInterests.minus(interest),
                )
            }
        } else {
            _state.update {
                it.copy(
                    pickedInterests = it.pickedInterests.plus(interest),
                )
            }
        }
    }

    // todo add validators
    fun stateIsValid(): Boolean = _state.value.nickname.isNotBlank() &&
        _state.value.description.isNotBlank() &&
        _state.value.pickedInterests.isNotEmpty()

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (!stateIsValid()) return@launch

            _state.update { it.copy(isGenerating = true) }

            val nickname = Nickname.orThrow(_state.value.nickname)
            val description = UserDescription.orThrow(_state.value.description)
            val interests = _state.value.pickedInterests
            register(nickname, description, interests)
            onSuccess()
        }
    }
}

@Composable
fun RegisterScreen(
    vm: RegisterScreenViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()

    when (val state = state) {
        is RegisterUiState.Generating -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.generating))

                    Spacer(Modifier.height(16.dp))

                    CircularProgressIndicator()
                }
            }
        }

        is RegisterUiState.Editing -> {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = modifier,
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> NicknameAndDescriptionPage(
                        state = state,
                        onNickname = vm::updateNickname,
                        onDescription = vm::updateDescription,
                        onProceed = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    1 -> InterestsPage(
                        state = state,
                        onToggle = vm::toggleInterest,
                        onProceed = {
                            vm.register(onSuccess = onHome)
                        },
                        onBack = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NicknameAndDescriptionPage(
    onProceed: () -> Unit,
    modifier: Modifier = Modifier,
    onNickname: (String) -> Unit,
    onDescription: (String) -> Unit,
    state: RegisterUiState.Editing,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.sign_up_to_friendly),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier,
        )

        Spacer(Modifier.height(8.dp))

        Row {
            Icon(
                painter = painterResource(R.drawable.ic_nickname_field),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNickname,
                placeholder = { Text(stringResource(R.string.your_nickname)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }

        Row {
            Icon(
                painter = painterResource(R.drawable.ic_description_field),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescription,
                placeholder = {
                    Text(stringResource(R.string.a_few_words_about_yourself))
                },
                minLines = 3,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    onProceed()
                },
            ) {
                Text(
                    text = stringResource(R.string.proceed),
                )
            }
        }
    }
}

@Composable
private fun InterestsPage(
    state: RegisterUiState.Editing,
    onToggle: (Interest) -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_interests),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(128.dp),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.pick_your_interests),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(32.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            maxItemsInEachRow = 4,
        ) {
            interests.forEach { interest ->
                InterestChip(
                    interest = interest,
                    // todo wrap in remember maybe
                    selected = interest in state.pickedInterests,
                    onToggle = onToggle,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
            ) {
                Text(
                    text = "Back",
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onProceed()
                },
            ) {
                Text(
                    text = stringResource(R.string.proceed),
                )
            }
        }
    }
}

@Composable
fun InterestChip(
    interest: Interest,
    selected: Boolean,
    onToggle: (Interest) -> Unit,
) {
    FilterChip(
        onClick = { onToggle(interest) },
        label = {
            Text(text = interest.string)
        },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
