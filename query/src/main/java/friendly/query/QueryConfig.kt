package friendly.query

import kotlin.time.Duration

public data class QueryConfig(
    val key: InfiniteQueryCacheKey,
    val retriesOnRefresh: Int = 3,
    val retriesOnFetchNext: Int = 3,
    val retryDelay: Duration,
)
