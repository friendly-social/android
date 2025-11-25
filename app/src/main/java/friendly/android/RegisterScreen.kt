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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import friendly.sdk.Interest
import kotlinx.coroutines.launch

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
        is RegisterScreenUiState.Generating -> {
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

        is RegisterScreenUiState.Editing -> {
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
                        onAvatarResult = vm::pickAvatar,
                        onProceed = {
                            if (state.isFirstPageValid) {
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
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

        if (state.isFirstPageValid) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
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

@Composable
private fun InterestsPage(
    state: RegisterScreenUiState.Editing,
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
                Text(stringResource(R.string.back))
            }

            Spacer(Modifier.weight(1f))

            if (state.pickedInterests.isNotEmpty()) {
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
}

@Composable
private fun InterestChip(
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
