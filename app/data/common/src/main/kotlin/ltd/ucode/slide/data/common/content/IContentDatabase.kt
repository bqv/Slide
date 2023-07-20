package ltd.ucode.slide.data.common.content

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import ltd.ucode.slide.data.common.dao.*

interface IContentDatabase {
    val sites: SiteDao
    val users: UserDao
    val groups: GroupDao
    val posts: PostDao
    val comments: CommentDao
    val languages: LanguageDao
    val timestamps: TimestampDao

}

suspend fun <R : Any?> IContentDatabase.withTransaction(block: suspend () -> R): R =
    (this as? RoomDatabase)!!.withTransaction(block)
