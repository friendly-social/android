@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package friendly.android

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import friendly.sdk.ActivityDetails
import friendly.sdk.CommunityPostDescriptor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
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
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "activity")
                    },
                    navigationIcon = { },
                )
            },
            modifier = Modifier
                .fillMaxSize(),
        ) { innerPadding ->
            LazyColumn(
                state = lazyColumnState,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                item {
                    Spacer(Modifier.height(12.dp))

                    if (activityState.error) {
                        Text("some error occurred")
                    }
                }

                items(
                    items = activityState.items,
                    contentType = { "feed" },
                    key = { item -> item.id.long },
                ) { item ->
                    ActivityDetails(
                        onActivityClick = onActivityClick,
                        vm = vm,
                        details = item,
                        modifier = Modifier,
                    )
                }

                item {
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
fun ActivityDetails(
    onActivityClick: (CommunityPostDescriptor) -> Unit,
    vm: ActivityScreenViewModel,
    details: ActivityDetails,
    modifier: Modifier = Modifier,
) {
    when (details) {
        is ActivityDetails.Reply -> {
            ListItem(
                onClick = { onActivityClick(details.post.descriptor) },
                leadingContent = {
                    UserAvatar(
                        nickname = details.post.owner.nickname,
                        userId = details.post.owner.id,
                        uri = details.post.owner.avatar?.let(vm::avatarUri),
                        style = UserAvatarStyle.Small,
                    )
                },
                trailingContent = {
                    Text(
                        text = formatDateTime(details.instant),
                        maxLines = 1,
                    )
                },
                modifier = modifier,
                content = {
                    Text(
                        text = buildAnnotatedString {
                            // TODO: string resources
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
            )
        }
    }
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
