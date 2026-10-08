package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import friendly.query.InfiniteQueryClient
import friendly.query.InfiniteQueryConfig
import friendly.query.InfiniteQueryFetchResult
import friendly.query.QueryCache
import friendly.query.QueryClient
import friendly.query.QueryFetchResult
import friendly.query.QueryKey
import friendly.query.infiniteQuery
import friendly.query.save
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostDetailsSerializable
import friendly.sdk.Cursor
import friendly.sdk.CursorId
import friendly.sdk.CursorSerializable
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.DetailsResult
import friendly.sdk.FriendlyCommunityClient.ListResult.IOError
import friendly.sdk.FriendlyCommunityClient.ListResult.ServerError
import friendly.sdk.FriendlyCommunityClient.ListResult.Success
import friendly.sdk.FriendlyCommunityClient.ListResult.Unauthorized
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.milliseconds

data class CommunityPost(
    val details: CommunityPostDetails,
    val upstream: List<CommunityPostDetails>,
    val replies: Cursor<CommunityPostDetails>,
) {
    fun serializable(): CommunityPostSerializable = CommunityPostSerializable(
        details = details.serializable(),
        upstream = upstream.map { upstreamPost ->
            upstreamPost.serializable()
        },
        replies = replies.serializable { reply -> reply.serializable() },
    )
}

@Serializable
data class CommunityPostSerializable(
    val details: CommunityPostDetailsSerializable,
    val upstream: List<CommunityPostDetailsSerializable>,
    val replies: CursorSerializable<CommunityPostDetailsSerializable>,
) {
    fun typed(): CommunityPost = CommunityPost(
        details = details.typed(),
        upstream = upstream.map { upstreamPost -> upstreamPost.typed() },
        replies = replies.typed { reply -> reply.typed() },
    )
}

inline fun <T, reified TS> roomQueryCache(
    dao: QueryItemDao,
    noinline itemSerializable: (T) -> TS,
    noinline itemTyped: (TS) -> T,
    json: Json = Json,
): QueryCache<T> = RoomSerializableQueryCache(
    itemSerializer = serializer<TS>().map(itemSerializable, itemTyped),
    dao = dao,
    json = json,
)

class RoomSerializableQueryCache<T>(
    private val itemSerializer: KSerializer<T>,
    private val json: Json = Json,
    private val dao: QueryItemDao,
) : QueryCache<T> {
    override suspend fun store(key: QueryKey, item: T) {
        val queryItem = QueryItem(
            queryKey = key.string,
            payload = json.encodeToString(itemSerializer, item),
        )
        dao.set(queryItem)
    }

    override suspend fun get(key: QueryKey): T? {
        val queryItem = dao.read(key.string)
        return queryItem?.payload?.let { payload ->
            json.decodeFromString(itemSerializer, payload)
        }
    }
}

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
        config = InfiniteQueryConfig(
            key = QueryKey("community"),
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

    val queryClient = QueryClient<CommunityPost>(
        cache = roomQueryCache(
            dao = db.queryItemDao(),
            itemSerializable = { it.serializable() },
            itemTyped = { it.typed() },
        ),
        scope = viewModelScope,
    )

    fun runQueries() {
        viewModelScope.launch {
            val jobs = mutableMapOf<CommunityPostDescriptor, Job>()

            posts.state
                .filter { it.fetch is Idle }
                .collect { state ->
                    for (item in state.items) {
                        if (jobs.containsKey(item.descriptor)) continue

                        jobs[item.descriptor] = launch {
                            queryClient.save(
                                retries = 3,
                                retryDelay = 300.milliseconds,
                                fetch = {
                                    fetchPostDetails(item.descriptor)
                                },
                                key = QueryKey(
                                    string = "community-post-${item.id.long}",
                                ),
                            )
                        }
                    }
                }
        }
    }

    private suspend fun fetchPostDetails(
        descriptor: CommunityPostDescriptor,
    ): QueryFetchResult<CommunityPost> {
        val detailsResult = client.community
            .details(authStorage.getAuth(), descriptor)
        return when (detailsResult) {
            is DetailsResult.IOError,
            is DetailsResult.ServerError,
            is DetailsResult.Unauthorized,
            -> QueryFetchResult.Failure

            is DetailsResult.Success -> {
                val post = CommunityPost(
                    details = detailsResult.post,
                    upstream = detailsResult.upstream,
                    replies = detailsResult.replies,
                )
                QueryFetchResult.Success(post)
            }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}
