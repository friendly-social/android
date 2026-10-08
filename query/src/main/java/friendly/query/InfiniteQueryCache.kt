package friendly.query

public interface InfiniteQueryCache<C, T> {
    public suspend fun read(
        key: QueryKey,
        pageIndex: Int,
    ): List<InfiniteQueryPage<C, T>>

    public suspend fun replace(
        key: QueryKey,
        pages: List<InfiniteQueryPage<C, T>>,
    )

    public suspend fun append(
        key: QueryKey,
        pageIndex: Int,
        page: InfiniteQueryPage<C, T>,
    )

    public suspend fun clear(key: QueryKey)
}
