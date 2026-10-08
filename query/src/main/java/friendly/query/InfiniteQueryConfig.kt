package friendly.query

import kotlin.time.Duration

public data class InfiniteQueryConfig(
    val key: QueryKey,
    val retriesOnRefresh: Int = 3,
    val retriesOnFetchNext: Int = 3,
    val retryDelay: Duration,
)
