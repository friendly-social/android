package friendly.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

sealed interface EditInterestsDialogUiEvents {
    data class ConfirmSuccess(val interests: List<String>) :
        EditInterestsDialogUiEvents
}

data class EditInterestsDialogUiState(
    val pickedInterests: List<String>,
    val isSavable: Boolean,
)

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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { onDismiss() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                title = { Text("Edit interests") },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isSavable,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                FloatingActionButton(onClick = vm::onConfirmInterests) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save_outlined),
                        contentDescription = null,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
        ) {
            itemsIndexed(state.pickedInterests) { index, item ->
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                ListItem(
                    headlineContent = {
                        BasicTextField(
                            value = item,
                            onValueChange = { new ->
                                vm.onNewInterestValue(index, new)
                            },
                            textStyle = MaterialTheme.typography.bodyLarge
                                .copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            cursorBrush = SolidColor(
                                MaterialTheme.colorScheme.onSurface,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = Done),
                            keyboardActions = KeyboardActions(
                                onDone = { vm.onAddNewInterest() },
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxWidth(),
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { vm.onRemoveInterest(index) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                )
            }

            item {
                ListItem(
                    onClick = vm::onAddNewInterest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.add_new_interest),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier,
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_interests_outlined,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
