package friendly.query

import kotlinx.coroutines.flow.StateFlow

public interface InfiniteQuery<TCursor, TItem> {
    public val state: StateFlow<InfiniteQueryState<TItem>>

    public fun refresh()

    public fun fetchNext()
}
