package friendly.query

public data class InfiniteQueryPage<TCursor, TItem>(
    public val items: List<TItem>,
    public val nextCursor: TCursor?,
)
