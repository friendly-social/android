package friendly.cache

public interface Cache {
    public fun <T> store(
        key: String,
        value: T,
    )

    public fun <T> retrieve(key: String): T?
}
