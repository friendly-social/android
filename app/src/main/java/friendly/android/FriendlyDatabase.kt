package friendly.android

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
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
    primaryKeys = ["pagesKey", "itemId"],
)
data class PagingItem(
    val pagesKey: String,
    val position: Long,
    val itemId: String,
    val payload: String,
)

@Dao
interface PagingCacheDao {
    @Query(
        "SELECT * FROM paging_item WHERE pagesKey = :key ORDER BY position ASC",
    )
    fun pagingSource(key: String): PagingSource<Int, PagingItem>

    @Query(
        "SELECT COALESCE(MAX(position), -1) FROM paging_item WHERE pagesKey = :key",
    )
    suspend fun maxPosition(key: String): Long

    @Upsert
    suspend fun upsert(items: List<PagingItem>)

    @Query("DELETE FROM paging_item WHERE pagesKey = :key")
    suspend fun clear(key: String)
}
