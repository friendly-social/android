@file:OptIn(ExperimentalPagingApi::class)

package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import friendly.query.InfiniteQueryClient
import friendly.query.InfiniteQueryConfig
import friendly.query.InfiniteQueryFetchResult
import friendly.query.QueryKey
import friendly.query.infiniteQuery
import friendly.sdk.ActivityDetails
import friendly.sdk.CursorId
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import kotlin.time.Duration.Companion.milliseconds

class ActivityScreenViewModel(
    private val authStorage: AuthStorage,
    private val client: FriendlyClient,
    private val db: FriendlyDatabase,
) : ViewModel() {
    private val infiniteQueryClient =
        InfiniteQueryClient(
            cache = roomInfiniteQueryCache(
                itemSerializable = { item: ActivityDetails ->
                    item.serializable()
                },
                itemTyped = {
                    it.typed()
                },
                cursorSerializable = { cursor: CursorId ->
                    cursor.serializable()
                },
                cursorTyped = {
                    it.typed()
                },
                db = db.pagingCacheDao(),
            ),
            queryScope = viewModelScope,
        )

    val activity = infiniteQueryClient.infiniteQuery(
        config = InfiniteQueryConfig(
            key = QueryKey("activity"),
            retryDelay = 100.milliseconds,
        ),
        fetch = { cursor ->
            val result = client.activity.list(
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

    fun avatarUri(descriptor: FileDescriptor): Uri =
        descriptor.let(client.files::getEndpoint).string.toUri()
}
