package ltd.ucode.slide.data.common.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.common.entity.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM _post " +
            "WHERE rowid = :rowId ")
    fun flow(rowId: Long): Flow<Post>

    @Query("SELECT * FROM _post " +
            "WHERE rowid = :rowId ")
    fun get(rowId: Long): Post?

    @Query("SELECT * FROM _post " +
            "WHERE rowid = :rowid ")
    suspend fun query(rowid: Long): Post?

    @Query("SELECT * FROM _post_image AS pi " +
            "INNER JOIN _post AS p ON p.rowid = pi.post_rowid " +
            "INNER JOIN _site AS s ON s.rowid = p.site_rowid " +
            "WHERE pi.post_id = :postId AND s.name LIKE :siteName ")
    fun flow(postId: Int, siteName: String): Flow<Post>

    @Query("SELECT * FROM _post AS p " +
            "INNER JOIN _site AS s ON s.rowid = p.site_rowid " +
            "WHERE p.rowid = :postId AND s.name LIKE :siteName ")
    fun get(postId: Int, siteName: String): List<Post>

    @Query("SELECT * FROM _post AS p " +
            "INNER JOIN _site AS s ON s.rowid = p.site_rowid " +
            "WHERE p.rowid = :postId AND s.name LIKE :siteName ")
    suspend fun query(postId: Int, siteName: String): List<Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.created DESC ")
    fun pagingSourceNew(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.created ASC ")
    fun pagingSourceOld(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "INNER JOIN _comment AS c ON c.post_rowid = p.rowid " + // TODO: add lastComment timestamp
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "GROUP BY p.rowid " +
            "ORDER BY max(c.created) DESC ")
    fun pagingSourceNewComments(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY CASE " +
            "   WHEN p.upvotes = 0 OR p.upvotes = 0 THEN 0 " +
            "   ELSE (p.upvotes + p.downvotes) * (min(p.upvotes, p.downvotes)/max(p.upvotes, p.downvotes)) " +
            "END ASC ")
    fun pagingSourceControversial(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.comment_count DESC ")
    fun pagingSourceMostComments(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>


    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.score DESC ")
    fun pagingSourceTop(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.hot_rank DESC ")
    fun pagingSourceHot(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
            "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
            "WHERE s.name LIKE :siteName " +
            "AND (:before IS NULL OR p.created <= :before) " +
            "AND (:after IS NULL OR p.created >= :after) " +
            "ORDER BY p.active_rank DESC ")
    fun pagingSourceActive(siteName: String, before: Instant? = null, after: Instant? = null): PagingSource<Int, Post>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.created DESC " +
        "LIMIT :limit ")
    fun flowNew(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.created ASC " +
        "LIMIT :limit ")
    fun flowOld(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "INNER JOIN _comment AS c ON c.post_rowid = p.rowid " + // TODO: add lastComment timestamp
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "GROUP BY p.rowid " +
        "ORDER BY max(c.created) DESC " +
        "LIMIT :limit ")
    fun flowNewComments(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY CASE " +
        "   WHEN p.upvotes = 0 OR p.upvotes = 0 THEN 0 " +
        "   ELSE (p.upvotes + p.downvotes) * (min(p.upvotes, p.downvotes)/max(p.upvotes, p.downvotes)) " +
        "END ASC " +
        "LIMIT :limit ")
    fun flowControversial(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.comment_count DESC " +
        "LIMIT :limit ")
    fun flowMostComments(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>


    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.score DESC " +
        "LIMIT :limit ")
    fun flowTop(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.hot_rank DESC " +
        "LIMIT :limit ")
    fun flowHot(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Query("SELECT * FROM _site AS s " +
        "INNER JOIN _post AS p ON p.site_rowid = s.rowid " +
        "WHERE s.name LIKE :siteName " +
        "AND (:before IS NULL OR p.created <= :before) " +
        "AND (:after IS NULL OR p.created >= :after) " +
        "ORDER BY p.active_rank DESC " +
        "LIMIT :limit ")
    fun flowActive(limit: Int, siteName: String, before: Instant? = null, after: Instant? = null): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(post: Post)

    @Insert
    suspend fun addAll(posts: List<Post>)

    @Upsert
    suspend fun upsert(post: Post): Long

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("DELETE FROM _post")
    suspend fun deleteAll(): Int
}
