package friendly.android

import friendly.query.InfiniteQueryCache
import friendly.query.InfiniteQueryCacheKey
import friendly.query.InfiniteQueryPage
import kotlinx.serialization.KSerializer
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

inline fun <C, T, reified CS, reified TS> roomInfiniteQueryCache(
    db: PagingCacheDao,
    noinline itemSerializable: (T) -> TS,
    noinline itemTyped: (TS) -> T,
    noinline cursorSerializable: (C) -> CS,
    noinline cursorTyped: (CS) -> C,
    json: Json = Json,
): RoomSerializableInfiniteQueryCache<C, T> =
    RoomSerializableInfiniteQueryCache(
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

class RoomSerializableInfiniteQueryCache<C, T>(
    private val cursorSerializer: KSerializer<C>,
    private val itemSerializer: KSerializer<T>,
    private val db: PagingCacheDao,
    private val json: Json = Json,
) : InfiniteQueryCache<C, T> {
    override suspend fun append(
        key: InfiniteQueryCacheKey,
        pageIndex: Int,
        page: InfiniteQueryPage<C, T>,
    ) {
        val cacheItems = page.items
            .mapIndexed { index, pageItem ->
                PagingItem(
                    queryKey = key.string,
                    pageIndex = pageIndex,
                    indexInPage = index,
                    payload = json.encodeToString(itemSerializer, pageItem),
                    nextCursorPayload = page.nextCursor?.let { nextCursor ->
                        json.encodeToString(cursorSerializer, nextCursor)
                    },
                )
            }
        db.upsert(cacheItems)
    }

    override suspend fun read(
        key: InfiniteQueryCacheKey,
        pageIndex: Int,
    ): List<InfiniteQueryPage<C, T>> {
        val cachedPagingItems = db.read(
            key = key.string,
            page = pageIndex,
        )

        val cachedPages = cachedPagingItems
            .groupBy { pagingItem -> pagingItem.pageIndex }

        val pages = cachedPages
            .map { (_, page) ->
                val pageItems = page.map { cachedPageItem ->
                    json.decodeFromString(
                        deserializer = itemSerializer,
                        string = cachedPageItem.payload,
                    )
                }
                val nextCursorPayload = page
                    .firstOrNull()
                    ?.nextCursorPayload
                val nextCursor: C? = nextCursorPayload?.let { payload ->
                    json.decodeFromString(
                        deserializer = cursorSerializer,
                        string = payload,
                    )
                }
                InfiniteQueryPage(
                    items = pageItems,
                    nextCursor = nextCursor,
                )
            }

        return pages
    }

    override suspend fun clear(key: InfiniteQueryCacheKey) {
        db.clear(key.string)
    }

    override suspend fun replace(
        key: InfiniteQueryCacheKey,
        pages: List<InfiniteQueryPage<C, T>>,
    ) {
        val cachedItems = pages
            .flatMapIndexed { pageIndex, page ->
                page.items.mapIndexed { indexInPage, item ->
                    val itemPayload = json.encodeToString(
                        serializer = itemSerializer,
                        value = item,
                    )
                    val nextCursorPayload =
                        page.nextCursor?.let { nextCursor ->
                            json.encodeToString(
                                serializer = cursorSerializer,
                                value = nextCursor,
                            )
                        }
                    PagingItem(
                        queryKey = key.string,
                        pageIndex = pageIndex,
                        payload = itemPayload,
                        indexInPage = indexInPage,
                        nextCursorPayload = nextCursorPayload,
                    )
                }
            }

        db.replace(
            key = key.string,
            items = cachedItems,
        )
    }
}
