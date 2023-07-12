package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.data.type.PostAggregates
import ltd.ucode.network.lemmy.data.type.PostView
import ltd.ucode.network.SingleVote
import ltd.ucode.network.data.IPost
import ltd.ucode.network.lemmy.data.type.Post as LemmyPost

@Entity(tableName = "posts", indices = [
    Index(value = ["uri"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Post(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") override val rowId: Int = -1,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // home instance
        @ColumnInfo(name = "group_rowid") override val groupRowId: Int,
        @ColumnInfo(name = "user_rowid") val userRowId: Int,
        @ColumnInfo(name = "language_rowid") val languageRowId: Int,
        @ColumnInfo(name = "post_id") override val postId: Int, // home instance

        @ColumnInfo(name = "uri") override val uri: String,

        @ColumnInfo(name = "title") override val title: String = "",
        @ColumnInfo(name = "link") override val link: String = "",
        @ColumnInfo(name = "body") override val body: String = "",
        @ColumnInfo(name = "thumbnail_url") override val thumbnailUrl: String = "",

        @ColumnInfo(name = "is_nsfw") override val isNsfw: Boolean = false,
        @ColumnInfo(name = "is_locked") override val isLocked: Boolean = false,
        @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,

        @ColumnInfo(name = "upvotes") override val upvotes: Int = 0, //     Isomorphic to (score,
        @ColumnInfo(name = "downvotes") override val downvotes: Int = 0, //  upvoteRatio) combo.
        @ColumnInfo(name = "comments") override val comments: Int = 0,

        @ColumnInfo(name = "discovered") override val discovered: Instant = Clock.System.now(),
        @ColumnInfo(name = "created") override val created: Instant = Instant.DISTANT_PAST,
        @ColumnInfo(name = "updated") override val updated: Instant? = null,
) : IPost() {
    @Ignore lateinit var site: Site
    @Ignore lateinit var group: Group
    @Ignore override lateinit var user: User
    @Ignore lateinit var language: Language

    @Ignore lateinit var votes: Map<out Account, out PostVote>

    @Entity(tableName = "post_images")
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
        @ColumnInfo(name = "post_rowid") val postRowId: Int, // imaged post
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
        @ColumnInfo(name = "post_id") val postId: Int,

        @ColumnInfo(name = "is_nsfw") val isNsfw: Boolean = false,
        @ColumnInfo(name = "is_locked") val isLocked: Boolean = false,

        @ColumnInfo(name = "upvotes") val upvotes: Int = 0, //     Isomorphic to (score,
        @ColumnInfo(name = "downvotes") val downvotes: Int = 0, //  upvoteRatio) combo.
        @ColumnInfo(name = "comments") val comments: Int = 0,

        val discovered: Instant = Clock.System.now(),
        val updated: Instant? = null, // imaged instance
    )

    companion object {
        fun from(other: PostView,
                 site: Site, group: Group, user: User, language: Language): Post {
            return Post(
                instanceRowId = site.rowId,
                groupRowId = group.rowId,
                userRowId = user.rowId,
                languageRowId = language.rowId,
                postId = other.post.id.id,
                uri = other.post.apId)
                .copy(other.post)
                .copy(other.counts)
                // TODO: also, a PostVote
        }
    }

    fun copy(other: LemmyPost): Post {
        return copy(
            postId = other.id.id,

            uri = other.apId,

            title = other.name,
            link = other.url.orEmpty(),
            body = other.body.orEmpty(),
            thumbnailUrl = other.thumbnailUrl.orEmpty(),

            isNsfw = other.isNsfw,
            isLocked = other.isLocked,
            isDeleted = other.isDeleted,

            created = other.published.toInstant(TimeZone.UTC),
            updated = other.updated?.toInstant(TimeZone.UTC),
        )
    }

    fun copy(other: PostAggregates): Post {
        return copy(
            upvotes = other.upvotes,
            downvotes = other.downvotes,
            comments = other.comments,
        )
    }

    override val groupName: String
        get() = group.name

    override val myVote: SingleVote
        get() = votes.values.firstOrNull()
                ?.vote?.let(::SingleVote)
                ?: SingleVote.NOVOTE

    override val score: Int
        get() = upvotes - downvotes
    override val upvoteRatio: Double
        get() = (upvotes * 100.0) / downvotes
}
