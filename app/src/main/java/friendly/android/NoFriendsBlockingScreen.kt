package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.android.NoFriendsBlockingScreenUiEvent.SnackbarEvent
import friendly.android.NoFriendsBlockingScreenUiEvent.SnackbarEvent.SnackbarEventKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface NoFriendsBlockingScreenUiState {
    data class Idle(val friendLink: ValidatableField<String>) :
        NoFriendsBlockingScreenUiState

    data object Loading : NoFriendsBlockingScreenUiState
}

sealed interface NoFriendsBlockingScreenUiEvent {
    data object AddFriendSuccess : NoFriendsBlockingScreenUiEvent

    data class SnackbarEvent(val kind: SnackbarEventKind) :
        NoFriendsBlockingScreenUiEvent {
        enum class SnackbarEventKind {
            FriendLinkExpired,
            NetworkErrorOccurred,
            ServerErrorOccurred,
            UnauthorizedError,
            AddFriendSuccess,
            LinkIsInvalid,
        }
    }
}

@Composable
private fun snackbarStrings(): Map<SnackbarEventKind, String> =
    SnackbarEventKind.entries.associateWith { event ->
        when (event) {
            FriendLinkExpired ->
                stringResource(R.string.friend_link_expired)

            NetworkErrorOccurred ->
                stringResource(R.string.network_error_occurred)

            AddFriendSuccess ->
                stringResource(R.string.add_friend_by_token_success_text)

            LinkIsInvalid -> stringResource(R.string.link_is_invalid)

            ServerErrorOccurred -> stringResource(R.string.server_error)

            UnauthorizedError ->
                stringResource(R.string.unauthorized)
        }
    }

@Composable
fun NoFriendsBlockingScreen(
    onAddFriendSuccess: () -> Unit,
    contentPadding: PaddingValues,
    vm: NoFriendsBlockingScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = rememberLifecycleOwner()
    val state by vm.state.collectAsState()
    val snackbarStrings = snackbarStrings()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                launch {
                    when (event) {
                        is AddFriendSuccess -> onAddFriendSuccess()

                        is SnackbarEvent -> {
                            snackbarHostState.showSnackbar(
                                message = snackbarStrings.getValue(event.kind),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.padding(contentPadding),
    ) { innerPadding ->
        ScaffoldContent(
            state = state,
            onFriendLinkUpdate = vm::onFriendLink,
            onConfirm = vm::onConfirm,
            clipboard = clipboard,
            scope = scope,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ScaffoldContent(
    state: NoFriendsBlockingScreenUiState,
    onFriendLinkUpdate: (String) -> Unit,
    onConfirm: () -> Unit,
    scope: CoroutineScope,
    clipboard: Clipboard,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is Idle -> {
            IdleState(
                state = state,
                onFriendLinkUpdate = onFriendLinkUpdate,
                onConfirm = onConfirm,
                clipboard = clipboard,
                scope = scope,
                modifier = modifier,
            )
        }

        is Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingIndicator(modifier = Modifier.size(64.dp))
            }
        }
    }
}

@Composable
private fun IdleState(
    state: NoFriendsBlockingScreenUiState.Idle,
    onFriendLinkUpdate: (String) -> Unit,
    onConfirm: () -> Unit,
    scope: CoroutineScope,
    clipboard: Clipboard,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_frame_person),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_to_a_private_network),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_friends_screen_primary_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.friendLink.value,
            placeholder = { Text("https://getfriend.ly/…") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = null,
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = { onFriendLinkUpdate("") },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null,
                    )
                }
            },
            onValueChange = onFriendLinkUpdate,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val clipEntry = clipboard.getClipEntry()
                        val firstTextItem =
                            clipEntry?.clipData?.getItemAt(0)?.text

                        firstTextItem
                            ?.let(CharSequence::toString)
                            ?.let(onFriendLinkUpdate)

                        onConfirm()
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.paste))
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onConfirm,
                enabled = state.friendLink.validAndNotBlank,
                modifier = Modifier.weight(1f),
            ) {
                Text("Join")
            }
        }
    }
}
