package friendly.android

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import friendly.android.RegisterScreenUiEvent.SnackbarEvent.SnackbarEventKind
import friendly.sdk.Interest
import kotlinx.coroutines.launch

@Composable
private fun snackbarStrings(): Map<SnackbarEventKind, String> =
    SnackbarEventKind.entries.associateWith { kind ->
        when (kind) {
            SnackbarEventKind.NetworkError ->
                stringResource(R.string.network_error_occurred)

            SnackbarEventKind.ServerError ->
                stringResource(R.string.server_error_occurred)

            SnackbarEventKind.CompressionFailure ->
                stringResource(R.string.compression_failure)
        }
    }

@Composable
fun RegisterScreen(
    vm: RegisterScreenViewModel,
    onRegistration: () -> Unit,
    onEditInterests: (initialInterests: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val state by vm.state.collectAsState()

    val lifecycleOwner = rememberLifecycleOwner()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarStrings = snackbarStrings()

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    is SnackbarEvent -> {
                        launch {
                            snackbarHostState.showSnackbar(
                                message = snackbarStrings.getValue(event.kind),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }

                    is SuccessfulRegistration -> onRegistration()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.padding(contentPadding),
    ) { innerPadding ->
        ScreenContent(
            state = state,
            vm = vm,
            onEditInterests = onEditInterests,
            modifier = Modifier
                .padding(innerPadding)
                .imePadding(),
        )
    }
}

@Composable
fun ScreenContent(
    state: RegisterScreenUiState,
    vm: RegisterScreenViewModel,
    onEditInterests: (initialInterests: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val state = state) {
        is RegisterScreenUiState.Generating -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.generating))

                    Spacer(Modifier.height(16.dp))

                    CircularProgressIndicator()
                }
            }
        }

        is RegisterScreenUiState.Editing -> {
            EditingState(
                state = state,
                onNickname = vm::updateNickname,
                onDescription = vm::updateDescription,
                onSocialLink = vm::updateSocialLink,
                onAvatarResult = vm::pickAvatar,
                onRegister = vm::register,
                onEditInterests = {
                    // TODO: need to pass an interest list here
                    onEditInterests(state.pickedInterests)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EditingState(
    onRegister: () -> Unit,
    onEditInterests: () -> Unit,
    modifier: Modifier = Modifier,
    onNickname: (String) -> Unit,
    onSocialLink: (String) -> Unit,
    onDescription: (String) -> Unit,
    onAvatarResult: (Uri?) -> Unit,
    state: RegisterScreenUiState.Editing,
) {
    val pickMedia = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = onAvatarResult,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.sign_up_to_friendly),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier,
        )

        Spacer(Modifier.height(8.dp))

        AvatarPicker(
            uiState = state,
            onPick = {
                val pickRequest = PickVisualMediaRequest(
                    mediaType = PickVisualMedia.ImageOnly,
                )
                pickMedia.launch(pickRequest)
            },
            modifier = Modifier,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            maxItemsInEachRow = 4,
        ) {
            state.pickedInterests.forEach { interest ->
                InterestChip(Interest.orThrow(interest))
            }
        }

        Button(
            onClick = onEditInterests,
            shape = ButtonDefaults.textShape,
        ) {
            Text(stringResource(R.string.edit_interests))
        }

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
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = state.socialLink,
                onValueChange = onSocialLink,
                placeholder = {
                    Text(stringResource(R.string.your_social_link))
                },
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

        if (state.isFirstPageValid) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Button(
                    onClick = onRegister,
                ) {
                    Text(text = stringResource(R.string.sign_up))
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

@Composable
private fun AvatarPicker(
    uiState: RegisterScreenUiState.Editing,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .size(128.dp)
            .clickable { onPick() },
    ) {
        when (uiState.avatar) {
            is RegisterScreenUiState.AvatarState.None -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme
                                .secondaryContainer,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_photo_camera),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is RegisterScreenUiState.AvatarState.Uploaded -> {
                AsyncImage(
                    model = uiState.avatar.uriOrNull,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is RegisterScreenUiState.AvatarState.Uploading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AsyncImage(
                        model = uiState.avatar.uriOrNull,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            }
        }
    }
}
