package friendly.android

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import friendly.sdk.CommunityPostText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class CreatePostDialogUiState(val saved: Boolean, val isSending: Boolean)

@Composable
fun CreatePostDialog(
    onGoBack: () -> Unit,
    vm: CreatePostDialogViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150.milliseconds)
        focusRequester.requestFocus()
    }

    var unsavedChangesDialog by remember { mutableStateOf(false) }
    var formattingDialog by remember { mutableStateOf(false) }

    BackHandler(
        enabled = !state.saved &&
            CommunityPostText
                .validate(vm.postTextState.text.toString()),
        onBack = { unsavedChangesDialog = true },
    )

    AnimatedVisibility(
        visible = formattingDialog,
        exit = ExitTransition.None,
    ) {
        FormattingDialog(
            onVisibility = { formattingDialog = it },
        )
    }

    AnimatedVisibility(
        visible = unsavedChangesDialog,
        exit = ExitTransition.None,
    ) {
        UnsavedChangesDialog(
            vm = vm,
            onVisibility = { unsavedChangesDialog = it },
            onSave = vm::saveDraft,
            onGoBack = onGoBack,
        )
    }

    with(sharedTransitionScope) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        "create_post_shared_fab",
                    ),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = 0.8f, stiffness = 380f)
                    },
                    resizeMode = RemeasureToBounds,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(16.dp),
                    ),
                )
                .padding(contentPadding)
                .imePadding()
                .statusBarsPadding(),
        ) {
            ScreenContent(
                onCreate = onGoBack,
                onFormatting = { formattingDialog = true },
                onTryGoBack = {
                    if (state.saved || vm.postTextState.text.isBlank()) {
                        onGoBack()
                    } else {
                        unsavedChangesDialog = true
                    }
                },
                state = state,
                vm = vm,
                fieldState = vm.postTextState,
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FormattingDialog(
    onVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { onVisibility(false) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_md),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.formatting)) },
        text = {
            Text(
                stringResource(R.string.formatting_helper_dialog_text),
            )
        },
        confirmButton = {
            TextButton(onClick = { onVisibility(false) }) {
                Text(stringResource(R.string.close_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = { openGfmSpecInBrowser(context) }) {
                Text(stringResource(R.string.open_gfm_spec))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun UnsavedChangesDialog(
    vm: CreatePostDialogViewModel,
    onVisibility: (Boolean) -> Unit,
    onGoBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { onVisibility(false) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_save_outlined),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.unsaved_post_title)) },
        text = { Text(stringResource(R.string.unsaved_post_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onVisibility(false)
                    onSave()
                    onGoBack()
                },
            ) {
                Text(stringResource(R.string.save_draft))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onVisibility(false)
                    onGoBack()
                },
            ) {
                if (vm.initialText.isNotBlank()) {
                    Text(stringResource(R.string.keep_saved_draft))
                } else {
                    Text(stringResource(R.string.delete))
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ScreenContent(
    onTryGoBack: () -> Unit,
    onCreate: () -> Unit,
    onFormatting: () -> Unit,
    state: CreatePostDialogUiState,
    vm: CreatePostDialogViewModel,
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TopBar(
                state = state,
                onTryGoBack = onTryGoBack,
                onCreate = onCreate,
                vm = vm,
            )

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                PostTextField(
                    vm = vm,
                    state = fieldState,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(48.dp))
        }

        Toolbar(
            onFormatting = onFormatting,
            textFieldState = fieldState,
            modifier = Modifier.align(BottomCenter),
        )
    }
}

@Composable
private fun TopBar(
    onTryGoBack: () -> Unit,
    onCreate: () -> Unit,
    state: CreatePostDialogUiState,
    vm: CreatePostDialogViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp),
    ) {
        IconButton(
            onClick = onTryGoBack,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = vm::saveDraft,
            enabled = !state.saved &&
                CommunityPostText
                    .validate(vm.postTextState.text.toString()),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_save_outlined),
                contentDescription = null,
                modifier = Modifier.size(
                    ButtonDefaults.IconSize,
                ),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.save_draft))
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { vm.create(onCreate) },
            enabled = CommunityPostText
                .validate(vm.postTextState.text.toString()),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            if (state.isSending) {
                LoadingIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = null,
                    modifier = Modifier.size(
                        ButtonDefaults.IconSize,
                    ),
                )
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.post))
        }
    }
}

@Composable
private fun PostTextField(
    vm: CreatePostDialogViewModel,
    state: TextFieldState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collect {
                println("meow")
                vm.onPostTextChange()
            }
    }

    BasicTextField(
        state = state,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        inputTransformation = InputTransformation {
        },
        decorator = { innerTextField ->
            if (state.text.isEmpty()) {
                Text(
                    text = stringResource(
                        R.string.create_post_text_field_placeholder,
                    ),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            innerTextField()
        },
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun Toolbar(
    textFieldState: TextFieldState,
    onFormatting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    Column(
        modifier = modifier,
    ) {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp,
                )
                .horizontalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onFormatting,
                colors = ButtonDefaults.elevatedButtonColors(),
                contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                modifier = Modifier
                    .height(
                        ButtonDefaults.ExtraSmallContainerHeight,
                    ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_help),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                Text(stringResource(R.string.formatting))
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = {
                    copyToClipboardIn(
                        scope = scope,
                        text = textFieldState.text.toString(),
                        clipboardManager = clipboard,
                    )
                },
                colors = ButtonDefaults.elevatedButtonColors(),
                contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                modifier = Modifier
                    .height(
                        ButtonDefaults.ExtraSmallContainerHeight,
                    ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                Text(stringResource(R.string.copy_all))
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = {
                    // TODO
                },
                colors = ButtonDefaults.elevatedButtonColors(),
                enabled = false,
                contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                modifier = Modifier
                    .height(
                        ButtonDefaults.ExtraSmallContainerHeight,
                    ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_photo_camera),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                Text(stringResource(R.string.attach_media) + " (WIP)")
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = {
                    // TODO
                },
                colors = ButtonDefaults.elevatedButtonColors(),
                enabled = false,
                contentPadding = ButtonDefaults.ExtraSmallContentPadding,
                modifier = Modifier
                    .height(
                        ButtonDefaults.ExtraSmallContainerHeight,
                    ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_md),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
                Text(stringResource(R.string.preview) + " (WIP)")
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

private fun copyToClipboardIn(
    scope: CoroutineScope,
    text: String,
    clipboardManager: Clipboard,
) {
    scope.launch {
        val entry = ClipEntry(
            clipData = ClipData.newPlainText("draft text", text),
        )
        clipboardManager.setClipEntry(entry)
    }
}

private fun openGfmSpecInBrowser(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        "https://github.github.com/gfm/".toUri(),
    )
    context.startActivity(intent)
}
