package friendly.query

// TODO:
//  this state management looks very dumb…
//  possible solution:
//      Loading
//      Error
//      Present(isRefreshing, isError, isCached)

public data class QueryState<T>(
    val item: T?,
    val fetch: FetchStatus,
    val source: Source,
    val error: Boolean,
) {
    public sealed interface FetchStatus {
        public data object Idle : FetchStatus
        public data object Loading : FetchStatus
        public data object Refreshing : FetchStatus
    }

    public sealed interface Source {
        public data object None : Source
        public data object Cached : Source
        public data object Fetched : Source
    }
}
