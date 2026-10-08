package friendly.query

public interface QueryCache<T> {
    public suspend fun store(key: QueryKey, item: T)

    public suspend fun get(key: QueryKey): T?
}
