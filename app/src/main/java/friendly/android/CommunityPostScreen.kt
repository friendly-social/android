package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostReply
import friendly.sdk.UserDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.take
import kotlin.time.Instant

private const val REPLY_PREVIEWS_LIMIT = 8

@Composable
fun CommunityPostScreen(
    vm: CommunityPostScreenViewModel,
    descriptor: CommunityPostDescriptor,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val postDetails = vm.query.data.collectAsState()

    // TODO: display here a bit more rich state after tests

    LaunchedEffect(Unit) { vm.runQueries() }

    when (val item = postDetails.value.item) {
        null -> {
            CircularProgressIndicator()
        }

        else -> {
            Content(
                post = item,
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
    post: CommunityPost,
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(topAppBarBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            UpstreamPosts(
                upstream = post.upstream,
                onClick = onPostClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )

            when (val details = post.details) {
                is CommunityPostDetails.Deleted -> TODO()

                is CommunityPostDetails.Plain -> {
                    MainPostContent(
                        isRoot = post.upstream.isEmpty(),
                        post = details,
                        vm = vm,
                        modifier = Modifier,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.replies),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(16.dp))

            Replies(
                replies = post.replies.data,
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
                SegmentedListItem(
                    shapes = endingListItemShape(),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor =
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                horizontal = 4.dp,
                                vertical = 4.dp,
                            )
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
                            Column {
                                Text(
                                    text = post.owner.nickname.string,
                                    style = MaterialTheme.typography.labelLarge,
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
                            modifier = Modifier.fillMaxSize(),
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
    replies: List<CommunityPostReply>,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
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
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = upstream.owner.nickname,
                            userId = upstream.owner.id,
                            uri = upstream.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(
                                text = upstream.owner.nickname.string,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatDateTime(upstream.instant),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Markdown(
                        content = upstream.text.string,
                        imageTransformer = Coil3ImageTransformerImpl,
                        modifier = modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun Reply(
    reply: CommunityPostReply,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    when (reply) {
        is CommunityPostReply.Single -> {
            SingleReply(
                onClick = onClick,
                reply = reply,
                vm = vm,
                modifier = modifier,
            )
        }

        is CommunityPostReply.Thread -> {
            ReplyThread(
                reply = reply,
                onClick = onClick,
                vm = vm,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SingleReply(
    onClick: (CommunityPostDescriptor) -> Unit,
    reply: CommunityPostReply.Single,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier,
) {
    OutlinedCard(
        onClick = { onClick(reply.post.descriptor) },
        modifier = modifier,
    ) {
        when (val replyPost = reply.post) {
            is Deleted -> {
                Text(
                    "This reply has been deleted, but the ui is work in progress",
                )
            }

            is Plain -> {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = replyPost.owner.nickname,
                            userId = replyPost.owner.id,
                            uri = replyPost.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = replyPost.owner.nickname.string,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatDateTime(replyPost.instant),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Markdown(
                        content = replyPost.text.string,
                        imageTransformer = Coil3ImageTransformerImpl,
                        modifier = modifier
                            .fillMaxSize(),
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ReplyPreviews(
                            vm = vm,
                            replyPreviews = replyPost.replyPreviews,
                            modifier = Modifier,
                        )

                        Spacer(Modifier.width(8.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_reply),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyThread(
    reply: CommunityPostReply.Thread,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        reply.thread.forEachIndexed { index, replyPost ->
            when (replyPost) {
                is Deleted -> {
                    Text(
                        "This reply has been deleted, but the ui is work in progress",
                    )
                }

                is Plain -> {
                    Column(
                        modifier = Modifier
                            .clickable(
                                onClick = { onClick(replyPost.descriptor) },
                            )
                            .padding(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            UserAvatar(
                                nickname = replyPost.owner.nickname,
                                userId = replyPost.owner.id,
                                uri = replyPost.owner.avatar?.let(vm::fileUri),
                                style = UserAvatarStyle.Small,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = replyPost.owner.nickname.string,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatDateTime(replyPost.instant),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Markdown(
                            content = replyPost.text.string,
                            imageTransformer = Coil3ImageTransformerImpl,
                            modifier = modifier
                                .fillMaxSize(),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (index == reply.thread.size - 1) {
                                ReplyPreviews(
                                    vm = vm,
                                    replyPreviews = replyPost.replyPreviews,
                                    modifier = Modifier,
                                )
                                Spacer(Modifier.width(8.dp))
                            }

                            Icon(
                                painter = painterResource(R.drawable.ic_reply),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
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

@Composable
private fun ReplyPreviews(
    vm: CommunityPostScreenViewModel,
    replyPreviews: List<UserDetails>,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = (-8).dp,
            alignment = Alignment.CenterHorizontally,
        ),
        modifier = modifier,
    ) {
        val moreThanLimit = replyPreviews.size > REPLY_PREVIEWS_LIMIT

        for (commonFriend in replyPreviews.take(REPLY_PREVIEWS_LIMIT)) {
            UserAvatar(
                userId = commonFriend.id,
                nickname = commonFriend.nickname,
                uri = commonFriend.avatar?.let(vm::fileUri),
                style = UserAvatarStyle(24.dp, noAvatarSize = 14.dp),
            )
        }

        if (moreThanLimit) {
            MorePreviewsStub(replyPreviews.size - REPLY_PREVIEWS_LIMIT)
        }
    }
}

@Composable
private fun MorePreviewsStub(
    previewsLeft: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Center,
        modifier = modifier
            .size(32.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
    ) {
        Text(
            text = "$previewsLeft+",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
