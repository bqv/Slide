package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ltd.ucode.network.SingleVote
import ltd.ucode.network.data.IComment

@Entity(tableName = "_comment", indices = [
    Index(value = ["uri"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["site_rowid"])
])
data class Comment(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") override val rowId: Long = 0,
        @ColumnInfo(name = "site_rowid") val siteRowId: Long, // home instance
        @ColumnInfo(name = "group_rowid") val groupRowId: Long,
        @ColumnInfo(name = "post_rowid") val postRowId: Long,
        @ColumnInfo(name = "user_rowid") val userRowId: Long,
        @ColumnInfo(name = "language_rowid") val languageRowId: Long,
        @ColumnInfo(name = "comment_id") override val commentId: Int, // home instance

        @ColumnInfo(name = "uri") override val uri: String,

        @ColumnInfo(name = "content") override val content: String = "",
        @ColumnInfo(name = "parent_id") override val parentId: Int? = null,
        @ColumnInfo(name = "parent_rowid") override val parentRowId: Long? = null,

        @ColumnInfo(name = "is_removed") override val isRemoved: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
        @ColumnInfo(name = "is_distinguished") override val isDistinguished: Boolean = false,

        @ColumnInfo(name = "upvotes") override val upvotes: Int = 0, //     Isomorphic to (score,
        @ColumnInfo(name = "downvotes") override val downvotes: Int = 0, //  upvoteRatio) combo.
        @ColumnInfo(name = "child_count") val childCount: Int = 0,

        @ColumnInfo(name = "discovered") override val discovered: Instant = Clock.System.now(),
        @ColumnInfo(name = "created") override val created: Instant = Instant.DISTANT_PAST,
        @ColumnInfo(name = "updated") override val updated: Instant? = null,
) : IComment() {
    @Ignore lateinit var site: Site
    @Ignore lateinit var group: Group
    @Ignore lateinit var post: Post
    @Ignore override lateinit var user: User
    @Ignore lateinit var language: Language

    @Ignore lateinit var parent: Comment
    @Ignore lateinit var votes: Map<out Account, out PostVote>
    @Ignore lateinit var children: List<out Comment>

    @Entity(tableName = "_comment_image")
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
        @ColumnInfo(name = "post_rowid") val postRowId: Long, // imaged post
        @ColumnInfo(name = "site_rowid") val siteRowId: Long,
        @ColumnInfo(name = "post_id") val postId: Int,

        @ColumnInfo(name = "parent_id") val parentId: Int? = null,

        @ColumnInfo(name = "is_nsfw") val isNsfw: Boolean = false,
        @ColumnInfo(name = "is_locked") val isLocked: Boolean = false,

        @ColumnInfo(name = "upvotes") val upvotes: Int = 0, //     Isomorphic to (score,
        @ColumnInfo(name = "downvotes") val downvotes: Int = 0, //  upvoteRatio) combo.
        @ColumnInfo(name = "child_count") val childCount: Int = 0,

        val discovered: Instant = Clock.System.now(),
        val updated: Instant? = null, // imaged instance
    )

    /*
    companion object {
        fun from(other: CommentView,
                 site: Site, group: Group, post: Post, user: User, language: Language): Comment {
            return Comment(
                siteRowId = site.rowId,
                groupRowId = group.rowId,
                postRowId = post.rowId,
                userRowId = user.rowId,
                languageRowId = language.rowId,
                commentId = other.comment.id.id,
                uri = other.comment.apId)
                .copy(other.comment)
                .copy(other.counts)
                // TODO: also, a CommentVote
        }
    }

    fun copy(other: LemmyComment): Comment {
        return copy(
            commentId = other.id.id,

            uri = other.apId,

            content = other.content,
            parentId = other.pathIds.dropLast(1).last().id,
            parentRowId = parentRowId ?: null,

            isRemoved = other.isRemoved,
            isDeleted = other.isDeleted,
            isDistinguished = other.isDistinguished,

            updated = other.updated?.toInstant(TimeZone.UTC),
        )
    }

    fun copy(other: CommentAggregates): Comment {
        return copy(
            upvotes = other.upvotes,
            downvotes = other.downvotes,
            childCount = other.childCount,
        )
    }
     */

    override val groupName: String
        get() = group.name

    override val myVote: SingleVote
        get() = votes.values.firstOrNull()
                ?.vote?.let(::SingleVote)
                ?: SingleVote.NOVOTE

    override val score: Int
        get() = upvotes - downvotes
    override val scoreRatio: Double
        get() = (upvotes * 100.0) / downvotes
}
