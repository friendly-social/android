@file:OptIn(ExperimentalPagingApi::class)

package friendly.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import friendly.sdk.ActivityDetailsSerializable
import friendly.sdk.CursorId
import friendly.sdk.FriendlyClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private data class ActivityScreenVmState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isNetworkError: Boolean,
)

// TODO: think
//
// here we need to think about making this bro both reusable and less bloated
// for the future

fun <T : Any> FriendlyDatabase.cachedPager(
    pagesKey: String,
    decode: (PagingItem) -> T,
    fetch: suspend (cursor: String?, start: Long) -> FetchResult,
): Flow<PagingData<T>> {
    val pager = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        remoteMediator = CachedListMediator(pagesKey, this, fetch),
        pagingSourceFactory = { pagingCacheDao().pagingSource(pagesKey) },
    )
    return pager.flow
        .map { paging ->
            paging
                .map { pagingItem ->
                    decode(pagingItem)
                } // TODO T?
                .filter { it != null } // TODO: items failed to decode
        }
}

class ActivityScreenViewModel(
    private val authStorage: AuthStorage,
    private val client: FriendlyClient,
    private val db: FriendlyDatabase, // TODO use sep abstraction for that later
) : ViewModel() {
    private val pagesKey = "activity"

    private val json = Json { ignoreUnknownKeys = true }

    val items = db.cachedPager(
        pagesKey = pagesKey,
        decode = { row ->
            // TODO: what if we will get a failure here?
            json
                .decodeFromString<ActivityDetailsSerializable>(row.payload)
                .typed()
        },
        fetch = { cursor, start ->
            println("CURSOR TO FETCH: $cursor")
            val activityListResult = client.activity.list(
                authorization = authStorage.getAuth(),
                cursorId = cursor?.let(::CursorId),
            )
            when (val activityListResult = activityListResult) {
                is IOError -> FetchResult.Failure.Io
                is ServerError -> FetchResult.Failure.Server
                is Unauthorized -> FetchResult.Failure.Unauthorized

                is Success -> {
                    val pageResult = PageResult(
                        items = activityListResult.cursor.data
                            .mapIndexed { index, item ->
                                PagingItem(
                                    pagesKey = pagesKey,
                                    position = start + index,
                                    itemId = item.id.toString(),
                                    payload = json.encodeToString(
                                        value = item.serializable(),
                                    ),
                                )
                            },
                        nextCursor = activityListResult.cursor.nextId?.string,
                    )
                    FetchResult.Success(pageResult)
                }
            }
        },
    ).cachedIn(viewModelScope)
}
