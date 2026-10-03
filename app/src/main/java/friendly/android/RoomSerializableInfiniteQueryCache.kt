package friendly.android

import friendly.query.InfiniteQueryCache
import friendly.query.InfiniteQueryCacheKey
import friendly.query.InfiniteQueryPage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// TODO: introduce support for fetching individual items by an id

@PublishedApi
internal fun <T, S> KSerializer<S>.map(
    toSerializable: (T) -> S,
    fromSerializable: (S) -> T,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor: SerialDescriptor = this@map.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(this@map, toSerializable(value))
    }

    override fun deserialize(decoder: Decoder): T =
        fromSerializable(decoder.decodeSerializableValue(this@map))
}

public inline fun <C, T, reified CS, reified TS> roomInfiniteQueryCache(
    db: PagingCacheDao,
    noinline itemSerializable: (T) -> TS,
    noinline itemTyped: (TS) -> T,
    noinline cursorSerializable: (C) -> CS,
    noinline cursorTyped: (CS) -> C,
    json: Json = Json,
): RoomSerializableInfiniteQueryCache<C, T> =
    RoomSerializableInfiniteQueryCache<C, T>(
        cursorSerializer = serializer<CS>().map(
            cursorSerializable,
            cursorTyped,
        ),
        itemSerializer = serializer<TS>().map(
            itemSerializable,
            itemTyped,
        ),
        db = db,
        json = json,
    )

public class RoomSerializableInfiniteQueryCache<C, T>(
    private val cursorSerializer: KSerializer<C>,
    private val itemSerializer: KSerializer<T>,
    private val db: PagingCacheDao,
    private val json: Json = Json,
) : InfiniteQueryCache<C, T> {
    private val pageSerializer: KSerializer<CachedPage<C, T>> =
        CachedPage.serializer(cursorSerializer, itemSerializer)

    override suspend fun append(
        key: InfiniteQueryCacheKey,
        index: Int,
        page: InfiniteQueryPage<C, T>,
    ) {
        val dto = CachedPage(
            items = page.items,
            nextCursor = page.nextCursor,
        )
        db.upsert(
            PagingItem(
                queryKey = key.string,
                pageIndex = index,
                payload = json.encodeToString(pageSerializer, dto),
            ),
        )
    }

    override suspend fun read(
        key: InfiniteQueryCacheKey,
    ): List<InfiniteQueryPage<C, T>> = db.read(key.string).map { entity ->
        val dto: CachedPage<C, T> = json.decodeFromString(
            deserializer = pageSerializer,
            string = entity.payload,
        )
        InfiniteQueryPage(
            items = dto.items,
            nextCursor = dto.nextCursor,
        )
    }

    override suspend fun clear(key: InfiniteQueryCacheKey) {
        db.clear(key.string)
    }

    override suspend fun replace(
        key: InfiniteQueryCacheKey,
        pages: List<InfiniteQueryPage<C, T>>,
    ) {
        db.replace(
            key = key.string,
            pages = pages.mapIndexed { index, page ->
                val dto = CachedPage(
                    items = page.items,
                    nextCursor = page.nextCursor,
                )
                PagingItem(
                    queryKey = key.string,
                    pageIndex = index,
                    payload = json.encodeToString(pageSerializer, dto),
                )
            },
        )
    }
}

@Serializable
private data class CachedPage<C, T>(val items: List<T>, val nextCursor: C?)
