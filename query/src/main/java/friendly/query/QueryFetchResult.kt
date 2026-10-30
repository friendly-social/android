package friendly.query

public sealed interface QueryFetchResult<out T> {
    // TODO: introduce two failure types: retriable and unretriable flavors
    public data object Failure : QueryFetchResult<Nothing>

    public data class Success<T>(val value: T) : QueryFetchResult<T>
}
