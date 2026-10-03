package friendly.query

import kotlinx.coroutines.CoroutineScope

public class InfiniteQueryClient<TCursor, TItem>(
    public val cache: InfiniteQueryCache<TCursor, TItem>,
    public val queryScope: CoroutineScope,
)
