package friendly.query

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

private class DefaultQuery<T>(
    queryClient: QueryClient<T>,
    private val key: QueryKey,
    private val retriesOnRefresh: Int,
    private val retryDelay: Duration,
    private val fetch: suspend () -> QueryFetchResult<T>,
) : Query<T> {
    val cache = queryClient.cache
    val scope = queryClient.scope

    private val _data = MutableStateFlow<QueryState<T>>(
        QueryState(
            item = null,
            fetch = QueryState.FetchStatus.Loading,
            source = QueryState.Source.None,
            error = false,
        ),
    )
    override val data: StateFlow<QueryState<T>> = _data.asStateFlow()

    override fun refresh() {
        _data.update { it.copy(fetch = Refreshing) }

        scope.launch {
            val fetchResult = fetchWithRetries(
                retriesOnRefresh,
                retryDelay,
            ) { fetch() }

            when (fetchResult) {
                is QueryFetchResult.Failure -> {
                    _data.update {
                        it.copy(
                            fetch = Idle,
                            error = true,
                        )
                    }
                }

                is QueryFetchResult.Success -> {
                    val fetched = fetchResult.value
                    _data.update {
                        QueryState(
                            item = fetched,
                            fetch = Idle,
                            source = Fetched,
                            error = false,
                        )
                    }
                }
            }
        }
    }

    fun run() {
        scope.launch(Dispatchers.IO) {
            val cached = cache.get(key)

            if (cached != null) {
                _data.update {
                    QueryState(
                        item = cached,
                        fetch = Idle,
                        source = Cached,
                        error = false,
                    )
                }
            }

            _data.update { it.copy(fetch = Loading) }

            val fetchResult = fetchWithRetries(
                retriesOnRefresh,
                retryDelay,
            ) { fetch() }

            when (fetchResult) {
                is QueryFetchResult.Failure -> {
                    _data.update {
                        it.copy(
                            fetch = Idle,
                            error = true,
                        )
                    }
                }

                is QueryFetchResult.Success -> {
                    val fetched = fetchResult.value
                    _data.update {
                        QueryState(
                            item = fetched,
                            fetch = Idle,
                            source = Fetched,
                            error = false,
                        )
                    }
                }
            }
        }
    }
}

// TODO: make it fetch new value only if there is no any cache
public fun <T> QueryClient<T>.save(
    key: QueryKey,
    retries: Int,
    retryDelay: Duration,
    fetch: suspend () -> QueryFetchResult<T>,
) {
    val client = this
    val cache = client.cache
    client.scope.launch(Dispatchers.IO) {
        if (cache.get(key) != null) return@launch

        val fetchResult = fetchWithRetries(retries, retryDelay) { fetch() }
        when (fetchResult) {
            is Failure -> {}
            is Success -> cache.store(key, fetchResult.value)
        }
    }
}

public fun <T> QueryClient<T>.query(
    key: QueryKey,
    retriesOnRefresh: Int,
    retryDelay: Duration,
    fetch: suspend () -> QueryFetchResult<T>,
): Query<T> {
    val queryClient = this
    val defaultQuery = DefaultQuery(
        key = key,
        retryDelay = retryDelay,
        retriesOnRefresh = retriesOnRefresh,
        queryClient = queryClient,
        fetch = fetch,
    )
    defaultQuery.run()
    return defaultQuery
}

private suspend inline fun <T> fetchWithRetries(
    retries: Int,
    retryDelay: Duration,
    block: () -> QueryFetchResult<T>,
): QueryFetchResult<T> {
    require(retries >= 0)

    repeat(retries) {
        when (val result = block()) {
            is Failure -> delay(retryDelay)

            is Success -> return result
        }
    }

    return block()
}
