package friendly.query

import friendly.query.InfiniteQueryFetchResult.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

// TODO:
// - [ ] right now it doesn care about caching only the first page (which is big, though)
// - [ ] introduce here a request deduplication
// - [ ] when we have an invalid cache - we must clear all that cache and
//       make a new schema with no migration. also this
//       isn't cache-fault-tolerant

public fun <TCursor, TItem> InfiniteQueryClient<TCursor, TItem>.infiniteQuery(
    config: InfiniteQueryConfig,
    fetch: suspend (
        cursor: TCursor?,
    ) -> InfiniteQueryFetchResult<TCursor, TItem>,
): InfiniteQuery<TCursor, TItem> {
    val queryClient = this

    val infiniteQuery = DefaultInfiniteQuery(
        config = config,
        queryClient = queryClient,
        fetch = fetch,
    )

    queryScope.launch { infiniteQuery.run() }

    return infiniteQuery
}

private data class InternalInfiniteQueryState<C, T>(
    val pages: List<InfiniteQueryPage<C, T>>,
    val source: InfiniteQueryState.Source,
    val error: Boolean,
    val fetch: InfiniteQueryState.FetchStatus,
) {
    fun toUserState(): InfiniteQueryState<T> = InfiniteQueryState(
        items = pages.flatten(),
        source = source,
        error = error,
        fetch = fetch,
        hasNext = pages.hasNext,
    )
}

private class DefaultInfiniteQuery<TCursor, TItem>(
    queryClient: InfiniteQueryClient<TCursor, TItem>,
    private val config: InfiniteQueryConfig,
    private val fetch: suspend (
        cursor: TCursor?,
    ) -> InfiniteQueryFetchResult<TCursor, TItem>,
) : InfiniteQuery<TCursor, TItem> {
    private val cache = queryClient.cache
    private val queryScope = queryClient.queryScope

    var refreshJob: Job? = null
    var fetchNextJob: Job? = null

    private val _state =
        MutableStateFlow<InternalInfiniteQueryState<TCursor, TItem>>(
            InternalInfiniteQueryState(
                pages = emptyList(),
                source = None,
                error = false,
                fetch = Idle,
            ),
        )

    override val state: StateFlow<InfiniteQueryState<TItem>> =
        _state
            .map(InternalInfiniteQueryState<TCursor, TItem>::toUserState)
            .stateIn(
                scope = queryScope,
                started = SharingStarted.Eagerly,
                initialValue = InfiniteQueryState(
                    items = emptyList(),
                    source = None,
                    error = false,
                    fetch = Idle,
                    hasNext = false,
                ),
            )

    override fun refresh() {
        refreshJob?.cancel()
        refreshJob = queryScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    fetch = if (it.pages.isEmpty()) Loading else Refreshing,
                    error = false,
                )
            }
            val fetchResult = fetchWithRetries(
                retries = config.retriesOnRefresh,
                retryDelay = config.retryDelay,
            ) {
                fetch(null)
            }
            when (fetchResult) {
                is Failure -> _state.update {
                    it.copy(fetch = Idle, error = true)
                }

                is Success -> {
                    val page = InfiniteQueryPage(
                        fetchResult.value,
                        fetchResult.nextCursor,
                    )
                    _state.update {
                        InternalInfiniteQueryState(
                            pages = listOf(page),
                            source = Fetched,
                            error = false,
                            fetch = Idle,
                        )
                    }
                    cache.replace(config.key, listOf(page))
                }
            }
        }
    }

    override fun fetchNext() {
        if (fetchNextJob?.isActive == true || refreshJob?.isActive == true) {
            return
        }

        val nextCursor = _state.value.pages.lastOrNull()?.nextCursor ?: return

        fetchNextJob = queryScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    fetch = FetchingNext,
                    error = false,
                )
            }

            val fetchResult = fetchWithRetries(
                retries = config.retriesOnFetchNext,
                retryDelay = config.retryDelay,
            ) {
                fetch(nextCursor)
            }

            when (fetchResult) {
                is Failure -> _state.update {
                    it.copy(fetch = Idle, error = true)
                }

                is Success -> {
                    val page = InfiniteQueryPage(
                        items = fetchResult.value,
                        nextCursor = fetchResult.nextCursor,
                    )
                    _state.update { old ->
                        val newPages = old.pages + page
                        old.copy(
                            pages = newPages,
                            fetch = Idle,
                        )
                    }
                    cache.append(
                        key = config.key,
                        pageIndex = _state.value.pages.size - 1,
                        page = page,
                    )
                }
            }
        }
    }

    suspend fun run() {
        // TODO: we need to read all pages
        val cache = cache.read(
            key = config.key,
            pageIndex = 0,
        )

        if (cache.isNotEmpty()) {
            _state.update { it.copy(pages = cache, source = Cached) }
        }

        refresh()
    }
}

private val List<InfiniteQueryPage<*, *>>.hasNext: Boolean
    get() = lastOrNull()?.nextCursor != null

private fun <C, T> List<InfiniteQueryPage<C, T>>.flatten(): List<T> =
    this.flatMap { page -> page.items }

private suspend inline fun <C, T> fetchWithRetries(
    retries: Int,
    retryDelay: Duration,
    block: () -> InfiniteQueryFetchResult<C, T>,
): InfiniteQueryFetchResult<C, T> {
    require(retries >= 0)

    repeat(retries) {
        when (val result = block()) {
            is Failure -> delay(retryDelay)

            is Success -> return result
        }
    }

    return block()
}
