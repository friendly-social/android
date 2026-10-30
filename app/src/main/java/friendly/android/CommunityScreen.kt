package friendly.android

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import friendly.query.InfiniteQueryState
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostDetails.Deleted
import friendly.sdk.CommunityPostDetails.Plain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.ignoreLayoutPadding(amount: Dp) =
    layout { measurable, constraints ->
        val extra = (amount * 2).roundToPx()
        val placeable = measurable.measure(constraints.offset(extra, extra))
        layout(placeable.width - extra, placeable.height - extra) {
            placeable.place(-amount.roundToPx(), -amount.roundToPx())
        }
    }

@OptIn(FlowPreview::class)
@Composable
fun CommunityScreen(
    vm: CommunityScreenViewModel,
    contentPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by vm.posts.state.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lazyListState = rememberLazyListState()

    var fabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState.lastScrolledBackward) {
        snapshotFlow { lazyListState.lastScrolledBackward }
            .debounce(300.milliseconds)
            .collect {
                fabExpanded = it
            }
    }

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
            floatingActionButton = {
                with(sharedTransitionScope) {
                    CreatePostFab(
                        animatedContentScope = animatedContentScope,
                        onCreatePostClick = onCreatePostClick,
                        fabExpanded = fabExpanded,
                    )
                }
            },
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
                lazyListState = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.CreatePostFab(
    animatedContentScope: AnimatedContentScope,
    onCreatePostClick: () -> Unit,
    fabExpanded: Boolean,
) {
    Box(
        modifier = Modifier
            .ignoreLayoutPadding(24.dp)
            .sharedBounds(
                sharedContentState =
                rememberSharedContentState("create_post_shared_fab"),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = 0.8f,
                        stiffness = 380f,
                    )
                },
                resizeMode = RemeasureToBounds,
                enter = fadeIn(),
                exit = fadeOut(),
                clipInOverlayDuringTransition =
                OverlayClip(RoundedCornerShape(16.dp)),
            ),
    ) {
        ExtendedFloatingActionButton(
            onClick = { onCreatePostClick() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_filled),
                    contentDescription = null,
                )
            },
            expanded = fabExpanded,
            text = { Text(stringResource(R.string.create_post)) },
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
fun ScaffoldContent(
    vm: CommunityScreenViewModel,
    posts: InfiniteQueryState<CommunityPostDetails>,
    pullToRefreshState: PullToRefreshState,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshDistanceFraction = pullToRefreshState.distanceFraction
    val extraHeight = 24.dp * pullToRefreshDistanceFraction
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier,
    ) {
        if (posts.items.isNotEmpty()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
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

        ScrollToTopButton(
            scope = scope,
            lazyListState = lazyListState,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .align(Alignment.TopCenter),
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ScrollToTopButton(
    scope: CoroutineScope,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.lastScrolledBackward
        }
            .debounce(300.milliseconds)
            .collect { lastScrolledBackward ->
                val scrolledBeyondThreshold =
                    lazyListState.firstVisibleItemIndex > 10
                showButton = scrolledBeyondThreshold && lastScrolledBackward
            }
    }

    AnimatedVisibility(
        visible = showButton,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FilledTonalButton(
            shape = ButtonDefaults.squareShape,
            onClick = {
                scope.launch { lazyListState.scrollToItem(0) }
                showButton = false
            },
        ) {
            Text(stringResource(R.string.to_the_top))
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
