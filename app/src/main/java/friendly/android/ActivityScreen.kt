package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import friendly.sdk.ActivityDetails
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.FileDescriptor
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun ActivityScreen(
    vm: ActivityScreenViewModel,
    contentPadding: PaddingValues,
    onActivityClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val activityState by vm.activity.state.collectAsState()

    val groupedActivity = remember(activityState.items) {
        groupActivityByDateSegment(
            activity = activityState.items,
            nowInstant = Clock.System.now(),
            zone = TimeZone.currentSystemDefault(),
        )
    }

    val lazyColumnState = rememberLazyListState()

    val isAppending = activityState.fetch is FetchingNext
    val isInitiallyLoading = activityState.fetch is Loading
    val isRefreshing = activityState.fetch is Refreshing

    vm.activity.setFetchNextOnScroll(activityState, lazyColumnState)

    PullToRefreshBox(
        isRefreshing = isRefreshing || isInitiallyLoading,
        state = pullToRefreshState,
        onRefresh = vm.activity::refresh,
        indicator = {
            LoadingIndicator(
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopCenter),
                isRefreshing = isRefreshing || isInitiallyLoading,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState,
            )
        },
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
        ) { innerPadding ->
            LazyColumn(
                state = lazyColumnState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                item(key = "header") {
                    Spacer(Modifier.height(12.dp))

                    if (activityState.error) {
                        Text("some error occurred")
                    }
                }

                groupedActivity.forEach { activityGroup ->
                    stickyHeader {
                        DateGroupStickyHeader(
                            groupKind = activityGroup.kind,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    items(
                        items = activityGroup.activity,
                        contentType = { "activity" },
                        key = { item -> item.id.long },
                    ) { item ->
                        ActivityDetails(
                            onActivityClick = { activity ->
                                vm.markRead(activity)
                                when (activity) {
                                    is IndirectReplies ->
                                        onActivityClick(
                                            activity.post.descriptor,
                                        )

                                    is NewReply ->
                                        onActivityClick(
                                            activity.post.descriptor,
                                        )

                                    is NewRequest -> {}
                                }
                            },
                            vm = vm,
                            details = item,
                            modifier = Modifier,
                        )
                    }
                }

                item("bottom") {
                    if (isAppending) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateGroupStickyHeader(
    groupKind: DateGroupKind,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = when (groupKind) {
                Today -> stringResource(R.string.today)
                Yesterday -> stringResource(R.string.yesterday)
                Older -> stringResource(R.string.older)
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun ActivityDetails(
    onActivityClick: (ActivityDetails) -> Unit,
    vm: ActivityScreenViewModel,
    details: ActivityDetails,
    modifier: Modifier = Modifier,
) {
    val itemContainerColor = if (details.isRead) {
        Color.Unspecified
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    when (details) {
        is ActivityDetails.NewReply -> {
            ReplyActivityListItem(
                onActivityClick = onActivityClick,
                details = details,
                vm = vm,
                itemContainerColor = itemContainerColor,
                modifier = modifier,
            )
        }

        is ActivityDetails.IndirectReplies ->
            IndirectReplyActivityListItem(
                onActivityClick = onActivityClick,
                details = details,
                vm = vm,
                itemContainerColor = itemContainerColor,
                modifier = modifier,
            )

        is ActivityDetails.NewRequest -> {
            NewFriendRequestListItem(
                onActivityClick = onActivityClick,
                details = details,
                vm = vm,
                itemContainerColor = itemContainerColor,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun NewFriendRequestListItem(
    onActivityClick: (ActivityDetails) -> Unit,
    details: ActivityDetails.NewRequest,
    vm: ActivityScreenViewModel,
    itemContainerColor: Color,
    modifier: Modifier,
) {
    ListItem(
        onClick = { onActivityClick(details) },
        leadingContent = {
            UserAvatar(
                nickname = details.from.nickname,
                userId = details.from.id,
                uri = details.from.avatar?.let(vm::avatarUri),
                style = Small,
            )
        },
        trailingContent = { RelativeTimestampText(instant = details.instant) },
        colors = ListItemDefaults.colors(
            containerColor = itemContainerColor,
        ),
        content = { FriendRuquestText(details) },
        modifier = modifier,
    )
}

@Composable
private fun FriendRuquestText(details: ActivityDetails.NewRequest) {
    Text(
        text = buildAnnotatedString {
            when (details.isMutual) {
                false -> {
                    append(
                        stringResource(R.string.new_friend_request_from),
                    )
                    append(" \"")
                    withStyle(
                        style = SpanStyle(fontWeight = Bold),
                    ) {
                        append(details.from.nickname.string)
                    }
                    append("\"")
                }

                true -> {
                    append("\"")
                    withStyle(
                        style = SpanStyle(fontWeight = Bold),
                    ) {
                        append(details.from.nickname.string)
                    }
                    append("\" ")
                    append(
                        stringResource(
                            R.string.accepted_your_friend_request,
                        ),
                    )
                }
            }
        },
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun IndirectReplyActivityListItem(
    onActivityClick: (ActivityDetails) -> Unit,
    details: ActivityDetails.IndirectReplies,
    vm: ActivityScreenViewModel,
    itemContainerColor: Color,
    modifier: Modifier,
) {
    when (val post = details.post) {
        is Deleted -> IndirectReplyOnDeletedPostActivityItem(
            details = details,
            onActivityClick = onActivityClick,
            itemContainerColor = itemContainerColor,
            modifier = modifier,
        )

        is Plain -> IndirectReplyOnPlainPostActivityItem(
            post = post,
            onActivityClick = { onActivityClick(details) },
            avatarUri = vm::avatarUri,
            itemContainerColor = itemContainerColor,
            modifier = modifier,
        )
    }
}

@Composable
fun IndirectReplyOnPlainPostActivityItem(
    post: CommunityPostDetails.Plain,
    onActivityClick: () -> Unit,
    itemContainerColor: Color,
    avatarUri: (FileDescriptor) -> Uri?,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = onActivityClick,
        leadingContent = {
            UserAvatar(
                nickname = post.owner.nickname,
                userId = post.owner.id,
                uri = post.owner.avatar?.let(avatarUri),
                style = Small,
            )
        },
        trailingContent = { RelativeTimestampText(instant = post.instant) },
        colors = ListItemDefaults.colors(
            containerColor = itemContainerColor,
        ),
        content = {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.new_activity_under))
                    append(" \"")
                    append(post.text.string)
                    append("\"")
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun IndirectReplyOnDeletedPostActivityItem(
    details: ActivityDetails.IndirectReplies,
    onActivityClick: (ActivityDetails) -> Unit,
    itemContainerColor: Color,
    modifier: Modifier = Modifier,
) {
    ListItem(
        onClick = { onActivityClick(details) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_construction_filled),
                contentDescription = null,
            )
        },
        trailingContent = { RelativeTimestampText(instant = details.instant) },
        colors = ListItemDefaults.colors(
            containerColor = itemContainerColor,
        ),
        content = {
            Text(
                text = stringResource(R.string.new_activity_under_deleted_post),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ReplyActivityListItem(
    onActivityClick: (ActivityDetails) -> Unit,
    details: ActivityDetails.NewReply,
    vm: ActivityScreenViewModel,
    itemContainerColor: Color,
    modifier: Modifier,
) {
    ListItem(
        onClick = { onActivityClick(details) },
        leadingContent = {
            UserAvatar(
                nickname = details.post.owner.nickname,
                userId = details.post.owner.id,
                uri = details.post.owner.avatar?.let(vm::avatarUri),
                style = Small,
            )
        },
        trailingContent = { RelativeTimestampText(instant = details.instant) },
        colors = ListItemDefaults.colors(
            containerColor = itemContainerColor,
        ),
        content = {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.reply_from))
                    append(" ")
                    withStyle(
                        style = SpanStyle(fontWeight = Bold),
                    ) {
                        append(details.post.owner.nickname.string)
                    }
                    append(": \"")
                    append(details.post.text.string)
                    append("\"")
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

private enum class DateGroupKind {
    Today,
    Yesterday,
    Older,
}

private data class ActivityGroup(
    val kind: DateGroupKind,
    val activity: List<ActivityDetails>,
)

private fun groupActivityByDateSegment(
    activity: List<ActivityDetails>,
    nowInstant: Instant,
    zone: TimeZone,
): List<ActivityGroup> {
    val today = nowInstant.toLocalDateTime(zone).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)
    return activity
        .groupBy { item ->
            val date = item.instant.toLocalDateTime(zone).date
            when {
                date >= today -> DateGroupKind.Today
                date == yesterday -> DateGroupKind.Yesterday
                else -> DateGroupKind.Older
            }
        }
        .map { (kind, items) -> ActivityGroup(kind, items) }
        .sortedBy { group -> group.kind }
}
