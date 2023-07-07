package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.IGroup
import ltd.ucode.slide.data.IIdentifier
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.VoteDirection

@Entity(tableName = "posts", indices = [
    Index(value = ["name"], unique = true)
], primaryKeys = ["permalink"], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Post(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,

    override val id: String,
    override val title: String,
    override val url: String?,
    override val body: String?,
    override val bodyHtml: String?,
    override val isLocked: Boolean,
    override val isNsfw: Boolean,
    override val groupName: String,
    override val groupId: IIdentifier<IGroup>?,
    override val link: String,
    override val permalink: String,
    override val published: Instant,
    override val updated: Instant?,
    override val creator: IUser,
    override val score: Int,
    override val myVote: VoteDirection,
    override val upvoteRatio: Double,
    override val commentCount: Int
) : IPost() {
}
