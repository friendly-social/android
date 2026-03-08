package friendly.android

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import friendly.android.ProfileScreenViewModel.UserProfile
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface ProfileScreenUiState {
    data class Present(val profile: UserProfile) : ProfileScreenUiState

    data object Loading : ProfileScreenUiState

    data object Error : ProfileScreenUiState
}

val ProfileScreenUiState.socialLink: SocialLink?
    get() = when (val state = this) {
        is ProfileScreenUiState.Error -> null
        is ProfileScreenUiState.Loading -> null
        is ProfileScreenUiState.Present -> state.profile.socialLink
    }

data class ProfileScreenSource(
    val nickname: Nickname,
    val userId: UserId,
    val avatar: Uri?,
    val accessHash: UserAccessHash,
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ProfileScreen(
    source: ProfileScreenSource,
    onHome: () -> Unit,
    vm: ProfileScreenViewModel,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val state by vm.state.collectAsState()
    var removeFriendDialogVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val socialLink = state.socialLink

    LaunchedEffect(Unit) { vm.load(source) }

    Scaffold(
        topBar = {
            TopBar(
                onHome = onHome,
                socialLink = socialLink,
                context = context,
                onRemoveFriend = { removeFriendDialogVisible = true },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        ScaffoldContent(
            removeFriendDialogVisible = removeFriendDialogVisible,
            onDismissRemoveFriendDialog = { removeFriendDialogVisible = false },
            vm = vm,
            onHome = onHome,
            profileSource = source,
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            modifier = Modifier.consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun ScaffoldContent(
    removeFriendDialogVisible: Boolean,
    vm: ProfileScreenViewModel,
    onHome: () -> Unit,
    profileSource: ProfileScreenSource,
    state: ProfileScreenUiState,
    onDismissRemoveFriendDialog: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        RemoveFriendAlertDialog(
            visible = removeFriendDialogVisible,
            onDismiss = onDismissRemoveFriendDialog,
            vm = vm,
            onRemoveSuccess = onHome,
        )

        SharedAvatarNicknameContent(
            nickname = profileSource.nickname,
            userId = profileSource.userId,
            avatar = profileSource.avatar,
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            modifier = Modifier,
            /**/
        )
    }
}

@Composable
private fun TopBar(
    onHome: () -> Unit,
    socialLink: SocialLink?,
    context: Context,
    onRemoveFriend: () -> Unit,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onHome) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            if (socialLink != null) {
                SocialLinkIcon(
                    socialLink = socialLink,
                    context = context,
                )
            }

            FriendProfileDropdownMenu(
                socialLink = socialLink,
                onRemoveFriendDialogShow = onRemoveFriend,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
fun SharedAvatarNicknameContent(
    nickname: Nickname,
    userId: UserId,
    avatar: Uri?,
    state: ProfileScreenUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
): Unit = with(sharedTransitionScope) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(128.dp))

        PreloadedContent(
            nickname = nickname,
            userId = userId,
            avatar = avatar,
            animatedContentScope = animatedContentScope,
        )

        Spacer(Modifier.height(16.dp))

        AnimatedStateContent(state = state)
    }
}

@Composable
private fun AnimatedStateContent(
    state: ProfileScreenUiState,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(targetState = state) { state ->
        when (val state = state) {
            is ProfileScreenUiState.Loading -> LoadingState(modifier)

            is ProfileScreenUiState.Present -> PresentState(
                state = state,
                modifier = modifier,
            )

            is ProfileScreenUiState.Error -> ErrorState(modifier)
        }
    }
}

@Composable
private fun PresentState(
    state: ProfileScreenUiState.Present,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(top = 16.dp, bottom = 32.dp)
            .fillMaxWidth(),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.profile.interests.raw.forEachIndexed { _, interest ->
                InterestChip(interest)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.profile.description.string,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        LoadingIndicator(Modifier.size(48.dp))
    }
}

@Composable
private fun ErrorState(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.profile_screen_error_text),
        modifier = modifier,
    )
}

@Composable
private fun SharedTransitionScope.PreloadedContent(
    nickname: Nickname,
    userId: UserId,
    avatar: Uri?,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        UserAvatar(
            nickname = nickname,
            userId = userId,
            uri = avatar,
            style = Large,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = sharedProfileAvatarKey(userId),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
                .skipToLookaheadSize(),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = nickname.string,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = sharedProfileNicknameKey(userId),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
                .skipToLookaheadSize(),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SocialLinkIcon(socialLink: SocialLink, context: Context) {
    IconButton(
        onClick = {
            openSocialLink(
                context = context,
                socialLink = socialLink,
            )
        },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_link),
            contentDescription = null,
        )
    }
}

private fun copyToClipboardIn(
    scope: CoroutineScope,
    socialLink: SocialLink,
    clipboardManager: Clipboard,
) {
    scope.launch {
        val entry = ClipEntry(
            clipData = ClipData.newPlainText("social link", socialLink.string),
        )
        clipboardManager.setClipEntry(entry)
    }
}

private fun openSocialLink(context: Context, socialLink: SocialLink) {
    val intent = Intent(Intent.ACTION_VIEW, socialLink.string.toUri())
    context.startActivity(intent)
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FriendProfileDropdownMenu(
    socialLink: SocialLink?,
    onRemoveFriendDialogShow: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = null,
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (socialLink != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_social_link)) },
                    onClick = {
                        copyToClipboardIn(
                            scope = scope,
                            socialLink = socialLink,
                            clipboardManager = clipboardManager,
                        )
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_friend)) },
                onClick = {
                    onRemoveFriendDialogShow()
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun RemoveFriendAlertDialog(
    visible: Boolean,
    vm: ProfileScreenViewModel,
    onDismiss: () -> Unit,
    onRemoveSuccess: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandIn(),
        exit = ExitTransition.None,
    ) {
        AlertDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_person_remove),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.remove_friend)) },
            text = { Text(stringResource(R.string.remove_friend_text)) },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        vm.removeFriend(onRemoveSuccess)
                    },
                ) {
                    Text(stringResource(R.string.remove_friend))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
