package friendly.query

import kotlinx.coroutines.flow.StateFlow

public interface Query<T> {
    public val data: StateFlow<T>
}
