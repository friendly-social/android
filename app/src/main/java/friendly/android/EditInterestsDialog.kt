package friendly.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

sealed interface EditInterestsDialogUiEvents {
    data class ConfirmSuccess(val interests: List<String>) :
        EditInterestsDialogUiEvents
}

sealed interface EditInterestsDialogUiState {
    val pickedInterests: List<String>
    val isSavable: Boolean

    data class Idle(
        override val pickedInterests: List<String>,
        override val isSavable: Boolean,
    ) : EditInterestsDialogUiState

    data class EditingInterest(
        override val pickedInterests: List<String>,
        override val isSavable: Boolean,
        val interestValue: String,
        val interestValid: Boolean,
    ) : EditInterestsDialogUiState
}

@Composable
fun EditInterestsDialog(
    vm: EditInterestsDialogViewModel,
    onDismiss: () -> Unit,
    onConfirm: (interests: List<String>) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = rememberLifecycleOwner()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    is ConfirmSuccess -> onConfirm(event.interests)
                }
            }
        }
    }

    val lazyColumnState = rememberLazyListState()

    val editingInterest = state as? EditingInterest

    AnimatedVisibility(visible = editingInterest != null) {
        if (editingInterest != null) {
            EditInterestDialog(
                interestValue = editingInterest.interestValue,
                isInterestValid = editingInterest.interestValid,
                onValueChange = vm::onNewInterestValue,
                onDismiss = vm::onDiscardInterest,
                onConfirm = vm::onConfirmInterest,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = state.isSavable,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        TextButton(
                            onClick = vm::onSaveInterests,
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                            )
                        }
                    }
                },
                title = { Text(stringResource(R.string.edit_interests)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::createNewInterest) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
    ) { innerPadding ->
        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier.padding(innerPadding),
        ) {
            itemsIndexed(state.pickedInterests) { index, interestText ->
                ListItem(
                    onClick = { vm.editInterest(index) },
                    trailingContent = {
                        IconButton(onClick = { vm.onRemoveInterest(index) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = interestText,
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun EditInterestDialog(
    interestValue: String,
    onValueChange: (String) -> Unit,
    isInterestValid: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_interests_filled),
                contentDescription = null,
            )
        },
        title = { Text(text = stringResource(R.string.edit_interest)) },
        text = {
            OutlinedTextField(
                value = interestValue,
                onValueChange = onValueChange,
                singleLine = true,
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isInterestValid,
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard))
            }
        },
    )
}
