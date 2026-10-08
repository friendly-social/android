package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.query.InfiniteQueryCacheKey
import friendly.query.InfiniteQueryClient
import friendly.query.InfiniteQueryFetchResult
import friendly.query.QueryConfig
import friendly.query.infiniteQuery
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CursorId
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import kotlin.time.Duration.Companion.milliseconds

class CommunityScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    private val db: FriendlyDatabase,
) : ViewModel() {
    val infiniteQueryClient = InfiniteQueryClient(
        cache = roomInfiniteQueryCache(
            itemSerializable = { item: CommunityPostDetails ->
                item.serializable()
            },
            cursorSerializable = { cursor: CursorId ->
                cursor.serializable()
            },
            itemTyped = { it.typed() },
            cursorTyped = { it.typed() },
            db = db.pagingCacheDao(),
        ),
        queryScope = viewModelScope,
    )

    val posts = infiniteQueryClient.infiniteQuery(
        config = QueryConfig(
            key = InfiniteQueryCacheKey("community"),
            retryDelay = 100.milliseconds,
        ),
        fetch = { cursor: CursorId? ->
            val result = client.community.list(
                authorization = authStorage.getAuth(),
                cursorId = cursor,
            )
            when (result) {
                is IOError,
                is ServerError,
                is Unauthorized,
                -> InfiniteQueryFetchResult.Failure

                is Success -> InfiniteQueryFetchResult.Success(
                    value = result.cursor.data,
                    nextCursor = result.cursor.nextId,
                )
            }
        },
    )

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}

@Composable
fun CommunityScreen(
    vm: CommunityScreenViewModel,
    contentPadding: PaddingValues,
    onImageClick: (url: String) -> Unit,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val posts by vm.posts.state.collectAsState()

    Scaffold(
        modifier = modifier.padding(contentPadding),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (posts.items.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    item { HeadingSupportingText() }

                    itemsIndexed(
                        items = posts.items,
                        key = { _, item -> item.id.long },
                        contentType = { _, _ -> "post-preview" },
                    ) { index, item ->
                        Column {
                            CommunityPostPreview(
                                details = item,
                                avatarUri = when (item) {
                                    is Plain -> item.owner.avatar?.let(
                                        vm::fileUri,
                                    )
                                    is Deleted -> null
                                },
                                onClick = onPostClick,
                                onImageClick = onImageClick,
                                modifier = Modifier,
                            )
                        }
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
