@file:OptIn(ExperimentalPagingApi::class)

package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import friendly.query.InfiniteQueryCacheKey
import friendly.query.InfiniteQueryClient
import friendly.query.InfiniteQueryFetchResult
import friendly.query.QueryConfig
import friendly.query.infiniteQuery
import friendly.sdk.ActivityDetails
import friendly.sdk.CursorId
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
        config = QueryConfig(
            key = InfiniteQueryCacheKey("activity"),
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
}
