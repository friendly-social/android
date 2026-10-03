package friendly.query

public sealed interface InfiniteQueryFetchResult<out TCursor, out TItem> {
    public data object Failure : InfiniteQueryFetchResult<Nothing, Nothing>

    public data class Success<TCursor, TResult>(
        val value: List<TResult>,
        val nextCursor: TCursor?,
    ) : InfiniteQueryFetchResult<TCursor, TResult>
}
