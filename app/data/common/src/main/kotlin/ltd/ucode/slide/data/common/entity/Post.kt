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
import ltd.ucode.network.data.IPost

@Entity(tableName = "posts", indices = [
    Index(value = ["uri"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["site_rowid"])
])
data class Post(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") override val rowId: Int = -1,
    @ColumnInfo(name = "site_rowid") val siteRowId: Int, // home instance
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
    @ColumnInfo(name = "comment_count") override val commentCount: Int = 0,
    @ColumnInfo(name = "hot_rank") val hotRank: Int = 0,
    @ColumnInfo(name = "active_rank") val activeRank: Int = 0,

    @ColumnInfo(name = "discovered") override val discovered: Instant = Clock.System.now(),
    @ColumnInfo(name = "created") override val created: Instant = Instant.DISTANT_PAST,
    @ColumnInfo(name = "updated") override val updated: Instant? = null,
) : IPost() {
    @ColumnInfo(name = "score") override var score: Int = upvotes - downvotes
    @ColumnInfo(name = "score_ratio") override var scoreRatio: Double = (upvotes * 100.0) / downvotes

    @Ignore lateinit var site: Site
    @Ignore lateinit var group: Group
    @Ignore override lateinit var user: User
    @Ignore lateinit var language: Language

    @Ignore lateinit var votes: Map<out Account, out PostVote>

    @Entity(tableName = "post_images", indices = [
        Index(value = ["post_rowid", "site_rowid"], unique = true)
    ])
    data class Image(
            @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
            @ColumnInfo(name = "post_rowid") val postRowId: Int, // imaged post
            @ColumnInfo(name = "site_rowid") val siteRowId: Int,
            @ColumnInfo(name = "post_id") val postId: Int,

            @ColumnInfo(name = "is_nsfw") val isNsfw: Boolean = false,
            @ColumnInfo(name = "is_locked") val isLocked: Boolean = false,

            @ColumnInfo(name = "upvotes") val upvotes: Int = 0, //     Isomorphic to (score,
            @ColumnInfo(name = "downvotes") val downvotes: Int = 0, //  upvoteRatio) combo.
            @ColumnInfo(name = "comment_count") val commentCount: Int = 0,
            @ColumnInfo(name = "hot_rank") val hotRank: Int = 0,
            @ColumnInfo(name = "active_rank") val activeRank: Int = 0,

            val discovered: Instant = Clock.System.now(),
            val updated: Instant? = null, // imaged instance
    ) {
        @ColumnInfo(name = "score") var score: Int = upvotes - downvotes
        @ColumnInfo(name = "score_ratio") var scoreRatio: Double = (upvotes * 100.0) / downvotes
    }

    override val groupName: String
        get() = group.name

    override val myVote: SingleVote
        get() = votes.values.firstOrNull()
                ?.vote?.let(::SingleVote)
                ?: SingleVote.NOVOTE
}
