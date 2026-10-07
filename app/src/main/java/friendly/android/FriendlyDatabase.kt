package friendly.android

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert

@Database(
    entities = [PagingItem::class],
    version = 1,
)
abstract class FriendlyDatabase : RoomDatabase() {
    abstract fun pagingCacheDao(): PagingCacheDao
}

/**
 * Represents a single item in a page [pageIndex] of some
 * query with id [queryKey]. [indexInPage] represents an item index in the page.
 */
@Entity(
    tableName = "paging_item",
    primaryKeys = ["queryKey", "pageIndex", "indexInPage"],
)
data class PagingItem(
    val queryKey: String,
    val pageIndex: Int,
    val indexInPage: Int,
    val payload: String,
    val nextCursorPayload: String?,
)

@Dao
interface PagingCacheDao {
    @Query(
        """
        SELECT * FROM paging_item
        WHERE queryKey = :key AND pageIndex = :page
        ORDER BY indexInPage
        """,
    )
    suspend fun read(key: String, page: Int): List<PagingItem>

    @Query("DELETE FROM paging_item WHERE queryKey = :key")
    suspend fun clear(key: String)

    @Upsert
    suspend fun upsert(items: List<PagingItem>)

    @Transaction
    suspend fun replace(key: String, items: List<PagingItem>) {
        clear(key)
        upsert(items)
    }
}
