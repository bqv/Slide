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
import ltd.ucode.lemmy.data.type.CommentAggregates
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.SingleVote
import ltd.ucode.slide.data.IComment
import ltd.ucode.lemmy.data.type.Comment as LemmyComment

@Entity(tableName = "comment", indices = [
    Index(value = ["uri"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Comment(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") override val rowId: Int = -1,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // home instance
        @ColumnInfo(name = "group_rowid") val groupRowId: Int,
        @ColumnInfo(name = "post_rowid") val postRowId: Int,
        @ColumnInfo(name = "user_rowid") val userRowId: Int,
        @ColumnInfo(name = "language_rowid") val languageRowId: Int,
        @ColumnInfo(name = "comment_id") override val commentId: Int, // home instance

        @ColumnInfo(name = "uri") override val uri: String,

        @ColumnInfo(name = "content") override val content: String = "",
        @ColumnInfo(name = "parent_id") override val parentId: Int? = null,
        @ColumnInfo(name = "parent_rowid") override val parentRowId: Int? = null,

        @ColumnInfo(name = "is_removed") override val isRemoved: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
        @ColumnInfo(name = "is_distinguished") override val isDistinguished: Boolean = false,

        @ColumnInfo(name = "upvotes") override val upvotes: Int = 0, //     Isomorphic to (score,
        @ColumnInfo(name = "downvotes") override val downvotes: Int = 0, //  upvoteRatio) combo.
        @ColumnInfo(name = "child_count") val childCount: Int = 0,

        @ColumnInfo(name = "discovered") override val discovered: Instant = Clock.System.now(),
        @ColumnInfo(name = "updated") override val updated: Instant? = null,
) : IComment() {
    @Ignore lateinit var instance: Site
    @Ignore lateinit var group: Group
    @Ignore lateinit var post: Post
    @Ignore override lateinit var user: User
    @Ignore lateinit var language: Language

    @Ignore lateinit var parent: Comment
    @Ignore lateinit var votes: Map<out Account, out PostVote>
    @Ignore lateinit var children: List<out Comment>

    @Entity(tableName = "comment_images")
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
        @ColumnInfo(name = "post_rowid") val postRowId: Int, // imaged post
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
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

    companion object {
        fun from(other: CommentView,
                 instance: Site, group: Group, post: Post, user: User, language: Language): Comment {
            return Comment(
                instanceRowId = instance.rowId,
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
