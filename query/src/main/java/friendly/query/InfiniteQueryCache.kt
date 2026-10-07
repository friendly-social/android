package friendly.query

public interface InfiniteQueryCache<C, T> {
    public suspend fun read(
        key: InfiniteQueryCacheKey,
        pageIndex: Int,
    ): List<InfiniteQueryPage<C, T>>

    public suspend fun replace(
        key: InfiniteQueryCacheKey,
        pages: List<InfiniteQueryPage<C, T>>,
    )

    public suspend fun append(
        key: InfiniteQueryCacheKey,
        pageIndex: Int,
        page: InfiniteQueryPage<C, T>,
    )

    public suspend fun clear(key: InfiniteQueryCacheKey)
}
