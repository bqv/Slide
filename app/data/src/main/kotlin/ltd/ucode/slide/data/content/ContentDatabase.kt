package ltd.ucode.slide.data.content

import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ltd.ucode.slide.data.Converters
import ltd.ucode.slide.data.common.content.IContentDatabase
import ltd.ucode.slide.data.common.dao.CommentDao
import ltd.ucode.slide.data.common.dao.GroupDao
import ltd.ucode.slide.data.common.dao.LanguageDao
import ltd.ucode.slide.data.common.dao.PostDao
import ltd.ucode.slide.data.common.dao.SeenDao
import ltd.ucode.slide.data.common.dao.SiteDao
import ltd.ucode.slide.data.common.dao.TimestampDao
import ltd.ucode.slide.data.common.dao.UserDao
import ltd.ucode.slide.data.common.entity.Account
import ltd.ucode.slide.data.common.entity.Comment
import ltd.ucode.slide.data.common.entity.CommentVote
import ltd.ucode.slide.data.common.entity.Group
import ltd.ucode.slide.data.common.entity.GroupSubscription
import ltd.ucode.slide.data.common.entity.Language
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.common.entity.PostSearch
import ltd.ucode.slide.data.common.entity.PostVote
import ltd.ucode.slide.data.common.entity.Seen
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.Tagline
import ltd.ucode.slide.data.common.entity.Timestamp
import ltd.ucode.slide.data.common.entity.User
import ltd.ucode.slide.data.common.view.One

@Database(version = 1,
    entities = [
        Site::class,
        Site.Image::class,
        Tagline::class,
        User::class,
        User.Image::class,
        Account::class,
        PostVote::class,
        CommentVote::class,
        Group::class,
        Group.Image::class,
        GroupSubscription::class,
        Post::class,
        Post.Image::class,
        PostSearch::class,
        Comment::class,
        Comment.Image::class,
        Language::class,
        Language.Image::class,
        Timestamp::class,
        Seen::class,
    ],
    views = [
        One::class,
    ],
    autoMigrations = [
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase(), IContentDatabase {
    val database: SupportSQLiteDatabase? get() = mDatabase

    abstract override val sites: SiteDao
    abstract override val users: UserDao
    abstract override val groups: GroupDao
    abstract override val posts: PostDao
    abstract override val comments: CommentDao
    abstract override val languages: LanguageDao
    abstract override val timestamps: TimestampDao
    abstract override val seen: SeenDao

    companion object {
        const val filename: String = "content.db"

        val initScripts: List<Query> = listOf(
            Query("""SELECT 1"""), // sanity check
            Query("""
                CREATE TRIGGER IF NOT EXISTS software_case AFTER INSERT ON sites
                BEGIN
                    UPDATE sites
                    SET software = lower(software) ;
                END
            """.trimIndent()),
            Query("""INSERT INTO post_search(post_search) VALUES ('rebuild')"""),
        )
    }
}
