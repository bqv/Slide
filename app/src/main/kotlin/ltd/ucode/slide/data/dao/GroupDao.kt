package ltd.ucode.slide.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.entity.Group

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Int): Group

    @Query("SELECT * FROM groups " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Int): Group

    @Query("SELECT * FROM group_images AS gi " +
            "INNER JOIN groups AS g ON g.rowid = gi.group_rowid " +
            "INNER JOIN sites AS s ON s.rowid = gi.instance_rowid " +
            "WHERE gi.group_id = :groupId AND s.rowid = :siteRowId ")
    fun flow(groupId: Int, siteRowId: Int): Flow<Group>

    @Query("SELECT * FROM group_images AS gi " +
            "INNER JOIN groups AS g ON g.rowid = gi.group_rowid " +
            "INNER JOIN sites AS s ON s.rowid = gi.instance_rowid " +
            "WHERE gi.group_id = :groupId AND s.rowid = :siteRowId ")
    fun get(groupId: Int, siteRowId: Int): List<Group>

    @Query("SELECT * FROM groups AS g " +
            "INNER JOIN sites AS s ON s.rowid = g.instance_rowid " +
            "WHERE g.rowid = :groupId AND s.name LIKE :instanceName ")
    suspend fun query(groupId: Int, instanceName: String): List<Group>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(group: Group)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(group: Group)

    @Insert
    suspend fun addAll(groups: List<Group>)

    @Update
    suspend fun update(group: Group)

    @Delete
    suspend fun delete(group: Group)
}
