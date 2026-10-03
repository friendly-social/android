package friendly.query

public data class InfiniteQueryState<T>(
    val items: List<T>,
    val source: Source,
    val error: Boolean,
    val fetch: FetchStatus,
    val hasNext: Boolean,
) {
    public sealed interface FetchStatus {
        public data object Idle : FetchStatus
        public data object Loading : FetchStatus
        public data object Refreshing : FetchStatus
        public data object FetchingNext : FetchStatus
    }

    public sealed interface Source {
        public data object None : Source
        public data object Cached : Source
        public data object Fetched : Source
    }
}
