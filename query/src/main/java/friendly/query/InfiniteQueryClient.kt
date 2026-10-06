package friendly.query

import kotlinx.coroutines.CoroutineScope

public class InfiniteQueryClient<C, T>(
    public val cache: InfiniteQueryCache<C, T>,
    public val queryScope: CoroutineScope,
)
