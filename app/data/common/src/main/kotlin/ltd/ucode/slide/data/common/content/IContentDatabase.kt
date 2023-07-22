package ltd.ucode.slide.data.common.content

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import ltd.ucode.slide.data.common.dao.CommentDao
import ltd.ucode.slide.data.common.dao.GroupDao
import ltd.ucode.slide.data.common.dao.LanguageDao
import ltd.ucode.slide.data.common.dao.PostDao
import ltd.ucode.slide.data.common.dao.SeenDao
import ltd.ucode.slide.data.common.dao.SiteDao
import ltd.ucode.slide.data.common.dao.TimestampDao
import ltd.ucode.slide.data.common.dao.UserDao

interface IContentDatabase {
    val sites: SiteDao
    val users: UserDao
    val groups: GroupDao
    val posts: PostDao
    val comments: CommentDao
    val languages: LanguageDao
    val timestamps: TimestampDao
    val seen: SeenDao
}

suspend fun <R : Any?> IContentDatabase.withTransaction(block: suspend () -> R): R =
    (this as? RoomDatabase)!!.withTransaction(block)
