package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import friendly.query.InfiniteQueryState
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostDetails.Deleted
import friendly.sdk.CommunityPostDetails.Plain

@Composable
fun CommunityScreen(
    vm: CommunityScreenViewModel,
    contentPadding: PaddingValues,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by vm.posts.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) { vm.runQueries() }

    PullToRefreshBox(
        isRefreshing = posts.fetch is Refreshing || posts.fetch is Loading,
        state = pullToRefreshState,
        onRefresh = vm.posts::refresh,
        indicator = {
            LoadingIndicator(
                isRefreshing =
                posts.fetch is Refreshing || posts.fetch is Loading,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState,
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
            )
        },
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ScaffoldContent(
                vm = vm,
                posts = posts,
                onImageClick = onImageClick,
                onPostClick = onPostClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
fun ScaffoldContent(
    vm: CommunityScreenViewModel,
    posts: InfiniteQueryState<CommunityPostDetails>,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (posts.items.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item { HeadingSupportingText() }

                itemsIndexed(
                    items = posts.items,
                    key = { _, item -> item.id.long },
                    contentType = { _, _ -> "post-preview" },
                ) { _, item ->
                    CommunityPostPreview(
                        vm = vm,
                        details = item,
                        avatarUri = when (item) {
                            is Plain ->
                                item.owner.avatar
                                    ?.let(vm::fileUri)

                            is Deleted -> null
                        },
                        onClick = onPostClick,
                        onImageClick = onImageClick,
                        modifier = Modifier,
                    )
                }

                item { BottomSupportingText() }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = stringResource(R.string.the_feed_is_empty),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.community_expand_network_to_fill_the_feed,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun HeadingSupportingText() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.feed),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.here_you_will_see_the_posts_from_your_network,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun BottomSupportingText() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                R.string.community_expand_network_to_fill_the_feed,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
    }
}
