package friendly.android

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
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
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostReply
import friendly.sdk.CommunityPostText
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient
import friendly.sdk.FriendlyCommunityClient.Details2Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CommunityPostScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    private val db: FriendlyDatabase,
    private val createPostUseCase: CreatePostUseCase,
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

    private data class CommunityPostScreenVmState(
        val isReplySending: Boolean = false,
    ) {
        fun toUiState(): CommunityPostScreenUiState =
            CommunityPostScreenUiState(
                isReplySending = isReplySending,
            )
    }

    private val _state = MutableStateFlow(CommunityPostScreenVmState())

    val state: StateFlow<CommunityPostScreenUiState> = _state
        .map(CommunityPostScreenVmState::toUiState)
        .stateIn(viewModelScope, Eagerly, CommunityPostScreenUiState())

    val replyPostTextState = TextFieldState()

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
                is Details2Result.NotFound,
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

    init {
        viewModelScope.launch {
            query.data
                .filter { it.fetch is Idle }
                .collect { state ->
                    state.item?.let { post ->
                        val replies = post.replies.data
                        for (reply in replies) {
                            val postsToFetch = when (reply) {
                                is CommunityPostReply.Single ->
                                    listOf(reply.post)

                                is CommunityPostReply.Thread ->
                                    reply.thread
                            }

                            for (postToFetch in postsToFetch) {
                                queryClient.save(
                                    key = QueryKey(
                                        "community-post-${postToFetch.id.long}",
                                    ),
                                    retries = 3,
                                    retryDelay = 300.milliseconds,
                                    fetch = {
                                        fetchCommunityPostDetails(
                                            postToFetch,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
        }
    }

    private suspend fun fetchCommunityPostDetails(
        reply: CommunityPostDetails,
    ): QueryFetchResult<CommunityPost> {
        val detailsResult = client.community.details2(
            authorization = authStorage.getAuth(),
            descriptor = reply.descriptor,
        )
        return when (detailsResult) {
            is Details2Result.IOError,
            is Details2Result.ServerError,
            is Details2Result.Unauthorized,
            is Details2Result.NotFound,
            -> QueryFetchResult.Failure

            is Details2Result.Success -> {
                QueryFetchResult.Success(
                    CommunityPost(
                        details = detailsResult.post,
                        replies = detailsResult.replies,
                        upstream = detailsResult.upstream,
                    ),
                )
            }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()

    fun sendReply() {
        val isPostTextValid =
            CommunityPostText.validate(replyPostTextState.string)
        if (!isPostTextValid) return

        viewModelScope.launch {
            _state.update { it.copy(isReplySending = true) }
            val result = createPostUseCase(
                text = CommunityPostText.orThrow(replyPostTextState.string),
                replyTo = route.descriptor.typed(),
            )
            when (result) {
                is FriendlyCommunityClient.PostResult.IOError,
                is NotFound,
                is FriendlyCommunityClient.PostResult.ServerError,
                is FriendlyCommunityClient.PostResult.Unauthorized,
                -> TODO()

                is FriendlyCommunityClient.PostResult.Success -> {
                    replyPostTextState.clearText()
                    // todo error handling must be here
                    query.refresh()
                }
            }
            _state.update { it.copy(isReplySending = false) }
        }
    }

    fun onUserReplied() {
        query.refresh()
    }
}
