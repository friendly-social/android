package friendly.android

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import friendly.query.QueryClient
import friendly.query.QueryFetchResult
import friendly.query.QueryKey
import friendly.query.query
import friendly.sdk.CommunityPostDetails
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.Details2Result
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CommunityPostScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    private val db: FriendlyDatabase,
) : ViewModel() {
    private val route = savedStateHandle
        .toRoute<FriendlyNavGraph.Home.CommunityPost>(CommunityPostTypeMap)
    private val descriptor = route.descriptor.typed()

    private val queryClient = QueryClient<CommunityPost>(
        cache = roomQueryCache(
            dao = db.queryItemDao(),
            itemSerializable = { it.serializable() },
            itemTyped = { it.typed() },
        ),
        scope = viewModelScope,
    )

    val query = queryClient.query(
        key = QueryKey("community-post-${descriptor.id.long}"),
        retriesOnRefresh = 3,
        retryDelay = 300.milliseconds,
        fetch = {
            val detailsResult = client.community
                .details2(authStorage.getAuth(), descriptor)
            when (detailsResult) {
                is Details2Result.IOError,
                is Details2Result.ServerError,
                is Details2Result.Unauthorized,
                -> QueryFetchResult.Failure

                is Details2Result.Success -> {
                    val post = CommunityPost(
                        details = detailsResult.post,
                        upstream = detailsResult.upstream,
                        replies = detailsResult.replies,
                    )
                    QueryFetchResult.Success(post)
                }
            }
        },
    )

    fun runQueries() {
        viewModelScope.launch {
            // TODO: use other way of their prefetching
//            val jobs = mutableMapOf<CommunityPostDescriptor, Job>()
//
//            query.data
//                .filter { it.fetch is Idle }
//                .collect { state ->
//                    state.item?.let { post ->
//                        val replies = post.replies.data
//                        for (reply in replies) {
//                            if (jobs[reply.descriptor] == null) {
//                                launch {
//                                    queryClient.save(
//                                        key = QueryKey(
//                                            "community-post-${reply.id.long}",
//                                        ),
//                                        retries = 3,
//                                        retryDelay = 300.milliseconds,
//                                        fetch = {
//                                            fetchCommunityPostDetails(reply)
//                                        },
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
        }
    }

    // TODO: has to be refactored a bit
    private suspend fun fetchCommunityPostDetails(
        reply: CommunityPostDetails,
    ): QueryFetchResult<CommunityPost> {
        val detailsResult =
            client.community.details2(
                authorization =
                authStorage.getAuth(),
                descriptor =
                reply.descriptor,
            )
        return when (detailsResult) {
            is Details2Result.IOError,
            is Details2Result.ServerError,
            is Details2Result.Unauthorized,
            -> QueryFetchResult.Failure

            is Details2Result.Success -> {
                QueryFetchResult.Success(
                    CommunityPost(
                        details =
                        detailsResult.post,
                        replies =
                        detailsResult.replies,
                        upstream =
                        detailsResult.upstream,
                    ),
                )
            }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}
