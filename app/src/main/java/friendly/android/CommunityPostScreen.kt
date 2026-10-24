package friendly.android

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import friendly.query.QueryState
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostReply
import kotlinx.coroutines.launch

@Composable
fun CommunityPostScreen(
    vm: CommunityPostScreenViewModel,
    descriptor: CommunityPostDescriptor,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val postDetails = vm.query.data.collectAsState()

    LaunchedEffect(Unit) { vm.runQueries() }

    when (val item = postDetails.value.item) {
        null -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                ContainedLoadingIndicator()
            }
        }

        else -> {
            Content(
                post = item,
                state = postDetails.value,
                onPostClick = onPostClick,
                onBack = onBack,
                onImageClick = onImageClick,
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
    state: QueryState<CommunityPost>,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val topAppBarBehavior = TopAppBarDefaults
        .enterAlwaysScrollBehavior()

    val lazyColumnState = rememberLazyListState(
        initialFirstVisibleItemIndex = post.upstream.size + 1,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                post = post,
                onBack = onBack,
                modifier = Modifier,
                scrollBehavior = topAppBarBehavior,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding),
        ) {
            ScaffoldContent(
                lazyColumnState = lazyColumnState,
                post = post,
                onPostClick = onPostClick,
                onImageClick = onImageClick,
                vm = vm,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .nestedScroll(topAppBarBehavior.nestedScrollConnection),
            )

            AnimatedVisibility(
                visible = state.fetch !is QueryState.FetchStatus.Idle,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ScaffoldContent(
    lazyColumnState: LazyListState,
    post: CommunityPost,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (String) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyColumnState,
        modifier = modifier,
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        itemsIndexed(
            items = post.upstream,
            key = { _, item -> item.id.long },
        ) { index, upstream ->
            UpstreamPost(
                index = index,
                count = post.upstream.size,
                upstream = upstream,
                onClick = onPostClick,
                onImageClick = onImageClick,
                vm = vm,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
        }

        item {
            when (val details = post.details) {
                is CommunityPostDetails.Deleted -> {
                    CommunityPostScreenMainPostDeleted(
                        post = details,
                        modifier = Modifier,
                    )
                }

                is CommunityPostDetails.Plain -> {
                    CommunityPostScreenMainPostContent(
                        isRoot = post.upstream.isEmpty(),
                        post = details,
                        onImageClick = onImageClick,
                        vm = vm,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.replies),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(16.dp))
        }

        itemsIndexed(
            items = post.replies.data,
            key = { _, item -> item.listKey() },
        ) { _, reply ->
            ReplyPost(
                reply = reply,
                onClick = onPostClick,
                vm = vm,
                onImageClick = onImageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        item { Spacer(Modifier.fillParentMaxHeight(0.3f)) }
    }
}

private fun CommunityPostReply.listKey(): Long {
    val reply = this
    return when (reply) {
        is CommunityPostReply.Single -> reply.post.id.long
        is CommunityPostReply.Thread -> reply.thread.last().id.long
    }
}

@Composable
private fun TopAppBar(
    post: CommunityPost,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
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
        actions = {
            Box {
                IconButton(
                    onClick = { dropdownMenuExpanded = true },
                    shape = IconButtonDefaults.mediumSquareShape,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = null,
                    )
                }
                PostActionsDropdownMenu(
                    post = post,
                    expanded = dropdownMenuExpanded,
                    onDismissRequest = { dropdownMenuExpanded = false },
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

@Composable
private fun PostActionsDropdownMenu(
    post: CommunityPost,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = MenuDefaults.groupStandardContainerColor,
        shape = MenuDefaults.standaloneGroupShape,
    ) {
        DropdownMenuItem(
            text = { Text("Copy raw markdown") },
            shape = MenuDefaults.itemShape(1, 4).shape,
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                )
            },
            onClick = {
                scope.launch {
                    if (post.details is CommunityPostDetails.Plain) {
                        copyRawMarkdown(clipboardManager, post.details)
                    }
                }
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text("Open in web") },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in),
                    contentDescription = null,
                )
            },
            shape = MenuDefaults.itemShape(2, 4).shape,
            onClick = {
                openInBrowser(post, context)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text("Copy link") },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null,
                )
            },
            shape = MenuDefaults.itemShape(2, 4).shape,
            onClick = {
                scope.launch { copyWebLink(clipboardManager, post) }
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text("Share (WIP)") },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                )
            },
            shape = MenuDefaults.itemShape(3, 4).shape,
            onClick = onDismissRequest,
        )
    }
}

private suspend fun copyRawMarkdown(
    clipboardManager: Clipboard,
    details: CommunityPostDetails.Plain,
) {
    clipboardManager.setClipEntry(
        label = "post raw markdown",
        content = details.text.string,
    )
}

private fun openInBrowser(post: CommunityPost, context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        formWebLink(post).toUri(),
    )
    context.startActivity(intent)
}

private suspend fun copyWebLink(
    clipboardManager: Clipboard,
    post: CommunityPost,
) {
    clipboardManager.setClipEntry(
        label = "post web link",
        content = formWebLink(post),
    )
}

private suspend fun Clipboard.setClipEntry(
    label: String,
    content: String,
) {
    val entry = ClipEntry(
        ClipData.newPlainText(label, content),
    )
    this.setClipEntry(entry)
}

private fun formWebLink(post: CommunityPost): String {
    return "https://web.getfriend.ly/community/${post.details.id.long}/replies"
}
