package friendly.query

import kotlinx.coroutines.CoroutineScope

public class QueryClient<T>(
    public val cache: QueryCache<T>,
    public val scope: CoroutineScope,
)
