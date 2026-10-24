package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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

    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        vm.runQueries()

        lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            vm.events.collect { event ->
                when (event) {
                    is DataFetched ->
                        hapticFeedback
                            .performHapticFeedback(SegmentFrequentTick)
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = posts.fetch is Refreshing || posts.fetch is Loading,
        state = pullToRefreshState,
        onRefresh = vm.posts::refresh,
        indicator = {},
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom)
                .only(WindowInsetsSides.Horizontal),
            modifier = Modifier
                .fillMaxSize(),
        ) { innerPadding ->
            ScaffoldContent(
                vm = vm,
                posts = posts,
                onImageClick = onImageClick,
                onPostClick = onPostClick,
                pullToRefreshState = pullToRefreshState,
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
    pullToRefreshState: PullToRefreshState,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshDistanceFraction = pullToRefreshState.distanceFraction
    val extraHeight = 24.dp * pullToRefreshDistanceFraction

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (posts.items.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Spacer(Modifier.statusBarsPadding())
                    CommuintyFeedTopBar(
                        posts,
                        extraHeight,
                        pullToRefreshDistanceFraction,
                    )
                    HorizontalDivider()
                }

                itemsIndexed(
                    items = posts.items,
                    key = { _, item -> item.id.long },
                    contentType = { _, _ -> "post-preview" },
                ) { _, item ->
                    CommunityPostPreview(
                        vm = vm,
                        details = item,
                        avatarUri = when (item) {
                            is Plain -> item.owner.avatar?.let(vm::fileUri)
                            is Deleted -> null
                        },
                        onClick = onPostClick,
                        onImageClick = onImageClick,
                        modifier = Modifier,
                    )

                    HorizontalDivider()
                }

                item {
                    BottomSupportingText(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(Modifier.height(16.dp))
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
private fun CommuintyFeedTopBar(
    posts: InfiniteQueryState<CommunityPostDetails>,
    extraHeight: Dp,
    pullFraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Center,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val loading =
            posts.fetch is Refreshing || posts.fetch is Loading

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(
                    vertical = 8.dp + extraHeight,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.community),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(16.dp))
                when (loading) {
                    true -> {
                        LoadingIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val scale = 1f + 0.5f * pullFraction
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }

                    false -> {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_notification,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val scale = 1f + 0.5f * pullFraction
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.here_you_will_see_the_posts_from_your_network,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun BottomSupportingText(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(
                R.string.community_expand_network_to_fill_the_feed,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
