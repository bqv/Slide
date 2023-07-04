package ltd.ucode.slide.data.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.IGroup
import ltd.ucode.slide.data.IIdentifier
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.VoteDirection

@Entity(tableName = "posts", indices = [
    Index(value = ["name"], unique = true)
], primaryKeys = ["permalink"])
class Post (
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
    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }
}
