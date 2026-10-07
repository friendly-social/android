package friendly.query

public interface Cache<T> {
    public fun store(key: String, value: T)

    public fun retrieve(key: String): T?
}
