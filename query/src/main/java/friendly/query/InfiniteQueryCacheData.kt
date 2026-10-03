package friendly.query

public data class InfiniteQueryCacheData<TCursor, TItem>(
    val pages: List<InfiniteQueryPage<TCursor, TItem>>
)
