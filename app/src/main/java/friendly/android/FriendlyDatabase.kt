package friendly.android

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Database(entities = [NetworkDao.Friend::class], version = 1)
abstract class FriendlyDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}

@Dao
interface NetworkDao {
    @Insert
    fun insertAll(network: List<Friend>)

    @Query("SELECT * FROM friend")
    fun getAll(): List<Friend>

    @Entity
    data class Friend(@PrimaryKey val userId: Long, val nickname: String)
}
