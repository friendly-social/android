package friendly.query

public sealed interface CacheResult<out T> {
    public data class Success<T>(val data: T) : CacheResult<T>

    public data object Corrupted : CacheResult<Nothing>
}
