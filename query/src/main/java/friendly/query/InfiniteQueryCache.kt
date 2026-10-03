package friendly.query

public interface InfiniteQueryCache<C, T> {
    public suspend fun read(
        key: InfiniteQueryCacheKey,
    ): List<InfiniteQueryPage<C, T>>

    public suspend fun replace(
        key: InfiniteQueryCacheKey,
        pages: List<InfiniteQueryPage<C, T>>
    )

    public suspend fun append(
        key: InfiniteQueryCacheKey,
        index: Int,
        page: InfiniteQueryPage<C, T>,
    )

    public suspend fun clear(key: InfiniteQueryCacheKey)
}
