package friendly.android

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.android.AvatarUploadUseCase.UploadingPercentage
import friendly.android.EditProfileScreen.EditProfileScreenUiState
import friendly.android.EditProfileScreen.EditProfileScreenUiState.CurrentProfileUiState
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserId

data class ValidatableField<T>(val value: T, val isValid: Boolean = true)

object EditProfileScreen {
    data class OnEdit(
        val onNickname: (String) -> Unit,
        val onSocialLink: (String) -> Unit,
        val onDescription: (String) -> Unit,
        val toggleInterest: (Interest) -> Unit,
    )

    sealed interface SnackbarEvent {
        data object SavingError : SnackbarEvent
        data object AvatarUploadingError : SnackbarEvent
    }

    sealed interface EditProfileScreenUiState {
        val userId: UserId
        val initialNickname: Nickname

        data class Edit(
            override val userId: UserId,
            override val initialNickname: Nickname,
            val profile: CurrentProfileUiState,
            val availableInterests: List<Interest>,
            val isSavable: Boolean,
            val hasChanges: Boolean,
        ) : EditProfileScreenUiState

        data class CurrentProfileUiState(
            val nickname: ValidatableField<String>,
            val description: ValidatableField<String>,
            val avatar: AvatarUiState,
            val interests: List<Interest>,
            val socialLink: ValidatableField<String?>,
        )

        sealed interface AvatarUiState {
            data class Present(val uri: Uri?) : AvatarUiState

            data class Uploading(val percentage: UploadingPercentage) :
                AvatarUiState
        }

        data class Saving(
            override val userId: UserId,
            override val initialNickname: Nickname,
        ) : EditProfileScreenUiState
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    vm: EditProfileScreenViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val onEdit = remember {
        EditProfileScreen.OnEdit(
            onNickname = vm::onNickname,
            onSocialLink = vm::onSocialLink,
            onDescription = vm::onDescription,
            toggleInterest = vm::toggleInterest,
        )
    }

    val state by vm.state.collectAsState()

    var unsavedChangesConfirmationDialogVisible by remember {
        mutableStateOf(false)
    }
    var editAvatarDialogVisible by remember {
        mutableStateOf(false)
    }
    val pickMedia = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri: Uri? ->
            vm.pickAvatar(uri)
        },
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = rememberLifecycleOwner()
    val profileSavingErrorText = stringResource(R.string.profile_saving_error)
    val avatarUploadingErrorText = stringResource(
        id = R.string.avatar_uploading_error,
    )

    val saveButtonEnabled = when (val state = state) {
        is EditProfileScreenUiState.Edit -> state.isSavable
        is EditProfileScreenUiState.Saving -> false
    }
    val shouldShowExitDialog = when (val state = state) {
        is EditProfileScreenUiState.Edit -> state.hasChanges
        is EditProfileScreenUiState.Saving -> true
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    SavingError -> {
                        snackbarHostState.showSnackbar(profileSavingErrorText)
                    }

                    AvatarUploadingError -> {
                        snackbarHostState.showSnackbar(avatarUploadingErrorText)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                isSavable = state.isSavable(),
                isSaving = state is Saving,
                onBack = {
                    if (shouldShowExitDialog) {
                        unsavedChangesConfirmationDialogVisible = true
                    } else {
                        onBack()
                    }
                },
                onDone = {
                    vm.save(onSuccess = onBack)
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ExitWithUnsavedChangesDialog(
                visible = unsavedChangesConfirmationDialogVisible,
                saveButtonEnabled = saveButtonEnabled,
                onDismiss = {
                    unsavedChangesConfirmationDialogVisible = false
                },
                onDiscard = {
                    unsavedChangesConfirmationDialogVisible = false
                    onBack()
                },
                onSave = {
                    unsavedChangesConfirmationDialogVisible = false
                    vm.save(onSuccess = onBack)
                },
            )
            EditAvatarDialog(
                visible = editAvatarDialogVisible,
                onDismiss = {
                    editAvatarDialogVisible = false
                },
                onRemoveAvatar = {
                    vm.removeAvatar()
                    editAvatarDialogVisible = false
                },
                onPickAvatar = {
                    editAvatarDialogVisible = false
                    val pickRequest = PickVisualMediaRequest(
                        mediaType = PickVisualMedia.ImageOnly,
                    )
                    pickMedia.launch(pickRequest)
                },
            )
            when (val state = state) {
                is Saving -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }

                is Edit -> {
                    EditProfileState(
                        state = state,
                        onEdit = onEdit,
                        onEditAvatarClick = {
                            editAvatarDialogVisible = true
                        },
                        modifier = modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding(),
                    )
                }
            }
        }
    }
}

private fun EditProfileScreenUiState.isSavable(): Boolean = when (this) {
    is EditProfileScreenUiState.Saving -> false
    is EditProfileScreenUiState.Edit -> this.isSavable
}

@Composable
fun EditAvatarDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    ListItem(onRemoveAvatar) {
                        Text(stringResource(R.string.remove_avatar))
                    }
                    ListItem(onPickAvatar) {
                        Text(stringResource(R.string.pick_new_avatar))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitWithUnsavedChangesDialog(
    visible: Boolean,
    saveButtonEnabled: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            text = {
                Text(stringResource(R.string.unsaved_changes_alert_text))
            },
            confirmButton = {
                TextButton(
                    onClick = onSave,
                    enabled = saveButtonEnabled,
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onDiscard) {
                    Text(stringResource(R.string.discard))
                }
            },
            onDismissRequest = onDismiss,
        )
    }
}

@Composable
private fun TopBar(
    isSavable: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    TopAppBar(
        navigationIcon = { CancelButton(enabled = !isSaving, onBack) },
        title = { Text(stringResource(R.string.edit_profile)) },
        actions = { SaveButton(isSavable, onDone) },
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor =
            MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
private fun CancelButton(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onBack,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
        )
    }
}

@Composable
private fun SaveButton(
    isSavable: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onDone,
        enabled = isSavable,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun EditProfileState(
    state: EditProfileScreenUiState.Edit,
    onEdit: EditProfileScreen.OnEdit,
    onEditAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editInterestsVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        if (editInterestsVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    editInterestsVisible = false
                },
            ) {
                InterestsEditorContent(
                    state = state,
                    onToggle = onEdit.toggleInterest,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
        ) {
            EditAvatar(
                onClick = onEditAvatarClick,
                userId = state.userId,
                initialNickname = state.initialNickname,
                profile = state.profile,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.profile.interests.forEachIndexed { _, interest ->
                    InterestChip(interest)
                }
                SuggestionChip(
                    onClick = { editInterestsVisible = true },
                    label = { Text(stringResource(R.string.edit_interests)) },
                    modifier = Modifier.height(32.dp),
                )
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
                    value = state.profile.nickname.value,
                    onValueChange = onEdit.onNickname,
                    placeholder = {
                        Text(stringResource(R.string.your_nickname))
                    },
                    isError = !state.profile.nickname.isValid,
                    supportingText = {
                        if (!state.profile.nickname.isValid) {
                            Text(
                                text = stringResource(
                                    id = R.string.nickname_validation_text,
                                ),
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                    value = state.profile.socialLink.value ?: "",
                    onValueChange = onEdit.onSocialLink,
                    placeholder = {
                        Text(stringResource(R.string.your_social_link))
                    },
                    isError = !state.profile.socialLink.isValid,
                    supportingText = {
                        if (!state.profile.socialLink.isValid) {
                            Text(
                                text = stringResource(
                                    id = R.string.social_link_validation_text,
                                ),
                            )
                        }
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
                    value = state.profile.description.value,
                    onValueChange = onEdit.onDescription,
                    placeholder = {
                        Text(
                            stringResource(R.string.a_few_words_about_yourself),
                        )
                    },
                    isError = !state.profile.description.isValid,
                    supportingText = {
                        if (!state.profile.description.isValid) {
                            Text(
                                text = stringResource(
                                    id = R.string.description_validation_text,
                                ),
                            )
                        }
                    },
                    minLines = 3,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EditAvatar(
    onClick: () -> Unit,
    userId: UserId,
    initialNickname: Nickname,
    profile: CurrentProfileUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(128.dp),
    ) {
        when (val avatar = profile.avatar) {
            is Uploading -> {
                Box(
                    contentAlignment = Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EmptyAvatar(
                        nickname = null,
                        userId = userId,
                        style = Large,
                        modifier = Modifier,
                    )
                    if (avatar.percentage.float < 95f) {
                        LoadingIndicator(
                            progress = { avatar.percentage.float },
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }
            }

            is Present -> {
                UserAvatar(
                    nickname = initialNickname,
                    userId = userId,
                    uri = avatar.uri,
                    style = Large,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onClick,
                        ),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_edit_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape,
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(4.dp)
                        .size(24.dp)
                        .align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun InterestsEditorContent(
    state: EditProfileScreenUiState.Edit,
    onToggle: (Interest) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.edit_interests),
            textAlign = Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            maxItemsInEachRow = 4,
        ) {
            state.availableInterests.forEach { interest ->
                ToggleableInterestChip(
                    interest = interest,
                    selected = interest in state.profile.interests,
                    onToggle = onToggle,
                )
            }
        }
    }
}
