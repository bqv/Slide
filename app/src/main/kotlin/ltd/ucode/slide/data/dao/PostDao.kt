package ltd.ucode.slide.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ltd.ucode.slide.data.entity.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM posts " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Int): Post

    @Query("SELECT * FROM posts " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Int): Post

    @Query("SELECT * FROM posts AS p " +
            "INNER JOIN sites AS s ON s.rowid = p.instance_rowid " +
            "WHERE p.rowid = :postId AND s.name LIKE :instanceName ")
    fun get(postId: Int, instanceName: String): List<Post>

    @Query("SELECT * FROM posts AS p " +
            "INNER JOIN sites AS s ON s.rowid = p.instance_rowid " +
            "WHERE p.rowid = :postId AND s.name LIKE :instanceName ")
    suspend fun query(postId: Int, instanceName: String): List<Post>

    @Query("SELECT * FROM posts AS p " +
            "INNER JOIN sites AS s ON s.rowid = p.instance_rowid " +
            "WHERE s.name LIKE :instanceName ") // TODO: flip query
    fun pagingSource(instanceName: String): PagingSource<Int, Post>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(post: Post)

    @Insert
    suspend fun addAll(posts: List<Post>)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("DELETE FROM posts")
    suspend fun deleteAll(): Int
}
