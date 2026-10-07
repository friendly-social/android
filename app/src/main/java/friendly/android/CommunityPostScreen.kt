package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.DetailsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// TODO:
//  show reply previews below each post

class CommunityPostScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsResult.Success?>(null)

    val state: StateFlow<DetailsResult.Success?> = _state.asStateFlow()

    fun fetch(descriptor: CommunityPostDescriptor) {
        viewModelScope.launch {
            val authorization = authStorage.getAuth()
            val postDetails = client.community.details(
                authorization = authorization,
                descriptor = descriptor,
            )
            _state.update { postDetails.orThrow() }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}

// TODO: this is a completely WIP screen that has to changed very much

@Composable
fun CommunityPostScreen(
    vm: CommunityPostScreenViewModel,
    descriptor: CommunityPostDescriptor,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: this bro looks strange
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.fetch(descriptor) }

    when (val state = state) {
        null -> {
            CircularProgressIndicator()
        }

        else -> {
            Content(
                postDetailsResult = state,
                onPostClick = onPostClick,
                onBack = onBack,
                vm = vm,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun Content(
    postDetailsResult: DetailsResult.Success,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val topAppBarBehavior = TopAppBarDefaults
        .enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                onBack = onBack,
                modifier = Modifier,
                scrollBehavior = topAppBarBehavior,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .nestedScroll(topAppBarBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            UpstreamPosts(
                upstream = postDetailsResult.upstream,
                onClick = onPostClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )

            when (val post = postDetailsResult.post) {
                is CommunityPostDetails.Deleted -> TODO()

                is CommunityPostDetails.Plain -> {
                    MainPostContent(
                        isRoot = postDetailsResult.upstream.isEmpty(),
                        post = post,
                        vm = vm,
                        modifier = Modifier,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Replies(
                // todo use an infinite query for that later !!!
                replies = postDetailsResult.replies.data,
                onClick = onPostClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MainPostContent(
    isRoot: Boolean,
    post: CommunityPostDetails.Plain,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        when (isRoot) {
            true -> {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier,
                ) {
                    UserAvatar(
                        nickname = post.owner.nickname,
                        userId = post.owner.id,
                        uri = post.owner.avatar?.let(vm::fileUri),
                        style = UserAvatarStyle.Medium,
                    )

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            text = post.owner.nickname.string,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = formatDateTime(post.instant),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Markdown(
                    content = post.text.string,
                    imageTransformer = Coil3ImageTransformerImpl,
                    modifier = Modifier
                        .fillMaxSize(),
                )

                Spacer(Modifier.height(8.dp))
            }

            false -> {
                HorizontalDivider()
                SegmentedListItem(
                    shapes = endingListItemShape(),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor =
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            UserAvatar(
                                nickname = post.owner.nickname,
                                userId = post.owner.id,
                                uri = post.owner.avatar?.let(vm::fileUri),
                                style = UserAvatarStyle.Small,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = post.owner.nickname.string,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Markdown(
                            content = post.text.string,
                            imageTransformer = Coil3ImageTransformerImpl,
                            modifier = Modifier
                                .fillMaxSize(),
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun endingListItemShape(): ListItemShapes =
    ListItemDefaults.segmentedShapes(1, 2)

@Composable
fun Replies(
    replies: List<CommunityPostDetails>,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        for (reply in replies) {
            Reply(
                reply = reply,
                onClick = onClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun UpstreamPosts(
    upstream: List<CommunityPostDetails>,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        upstream.forEachIndexed { index, upstreamPost ->
            Upstream(
                index = index,
                count = upstream.count() + 1,
                upstream = upstreamPost,
                onClick = onClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun Upstream(
    index: Int,
    count: Int,
    upstream: CommunityPostDetails,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    when (upstream) {
        is CommunityPostDetails.Deleted -> {
            // TODO
            Text("This reply has been deleted, but the ui is work in progress")
        }

        is CommunityPostDetails.Plain -> {
            SegmentedListItem(
                onClick = { onClick(upstream.descriptor) },
                shapes = ListItemDefaults.segmentedShapes(index, count),
                colors = ListItemDefaults.segmentedColors(
                    containerColor =
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = upstream.owner.nickname,
                            userId = upstream.owner.id,
                            uri = upstream.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = upstream.owner.nickname.string,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatDateTime(upstream.instant),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Markdown(
                        content = upstream.text.string,
                        imageTransformer = Coil3ImageTransformerImpl,
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun Reply(
    reply: CommunityPostDetails,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    when (reply) {
        is CommunityPostDetails.Deleted -> {
            Text("This reply has been deleted, but the ui is work in progress")
        }

        is CommunityPostDetails.Plain -> {
            OutlinedCard(
                onClick = { onClick(reply.descriptor) },
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = reply.owner.nickname,
                            userId = reply.owner.id,
                            uri = reply.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = reply.owner.nickname.string,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatDateTime(reply.instant),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Markdown(
                        content = reply.text.string,
                        imageTransformer = Coil3ImageTransformerImpl,
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopAppBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onBack,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        title = { Text(stringResource(R.string.post)) },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

// TODO: make normal global date-time formatting utils
private fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val format = LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        day()

        chars(" ")

        hour()
        chars(":")
        minute()
    }
    val formattedDateTime = localDateTime.format(format)
    return formattedDateTime
}
