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

@Entity(
    tableName = "paging_item",
    primaryKeys = ["queryKey", "pageIndex"],
)
data class PagingItem(
    val queryKey: String,
    val pageIndex: Int,
    val payload: String,
)

@Dao
interface PagingCacheDao {
    @Query(
        "SELECT * FROM paging_item WHERE queryKey = :key ORDER BY pageIndex",
    )
    suspend fun read(key: String): List<PagingItem>

    @Query("DELETE FROM paging_item WHERE queryKey = :key")
    suspend fun clear(key: String)

    @Upsert
    suspend fun upsert(entity: PagingItem)

    @Transaction
    suspend fun replace(key: String, pages: List<PagingItem>) {
        clear(key)
        pages.forEach { page -> upsert(page) }
    }
}
