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
import friendly.query.save
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.DetailsResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
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
                .details(authStorage.getAuth(), descriptor)
            when (detailsResult) {
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
        },
    )

    fun runQueries() {
        viewModelScope.launch {
            val jobs = mutableMapOf<CommunityPostDescriptor, Job>()

            query.data
                .filter { it.fetch is Idle }
                .collect { state ->
                    state.item?.let { post ->
                        val replies = post.replies.data
                        for (reply in replies) {
                            if (jobs[reply.descriptor] == null) {
                                launch {
                                    queryClient.save(
                                        key = QueryKey("community-post-${reply.id.long}"),
                                        retries = 3,
                                        retryDelay = 300.milliseconds,
                                        fetch = {
                                            val detailsResult =
                                                client.community.details(
                                                    authorization = authStorage.getAuth(),
                                                    descriptor = reply.descriptor,
                                                )
                                            when (detailsResult) {
                                                is DetailsResult.IOError,
                                                is DetailsResult.ServerError,
                                                is DetailsResult.Unauthorized,
                                                    -> QueryFetchResult.Failure

                                                is DetailsResult.Success -> {
                                                    QueryFetchResult.Success(
                                                        CommunityPost(
                                                            details = detailsResult.post,
                                                            replies = detailsResult.replies,
                                                            upstream = detailsResult.upstream,
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}
