package friendly.android

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostReply

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
            CircularProgressIndicator()
        }

        else -> {
            Content(
                post = item,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    post: CommunityPost,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val topAppBarBehavior = TopAppBarDefaults
        .enterAlwaysScrollBehavior()

    val lazyColumnState = rememberLazyListState(
        initialFirstVisibleItemIndex = post.upstream.size,
    )

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
        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(topAppBarBehavior.nestedScrollConnection),
        ) {
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

            item { Spacer(Modifier.fillParentMaxHeight(0.8f)) }
        }
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
