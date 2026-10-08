package friendly.query

public sealed interface QueryFetchResult<out T> {
    public data object Failure : QueryFetchResult<Nothing>

    public data class Success<T>(val value: T) : QueryFetchResult<T>
}
