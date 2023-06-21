package ltd.ucode.lemmy.data

import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.VoteDirection

class LemmyPost(val instance: String, val data: PostView) : IPost() {
    override val id: String
        get() = data.post.id.toString() // TODO: drop string repr

    override val title: String
        get() = data.post.name

    override val url: String?
        get() = data.post.url

    override val body: String?
        get() = data.post.body

    override val isLocked: Boolean
        get() = data.post.isLocked

    override val isNsfw: Boolean
        get() = data.post.isNsfw

    override val groupName: String
        get() = data.community.name

    override val permalink: String
        get() = "https://${instance}/post/${data.post.id}"

    override val thumbnail: String?
        get() = data.post.thumbnailUrl

    override val thumbnailType: ThumbnailType
        get() = (data.post.thumbnailUrl?.let { ThumbnailType.URL }) ?: ThumbnailType.NONE

    override val published: Instant
        get() = data.post.published.toInstant(UtcOffset.ZERO)

    override val updated: Instant?
        get() = data.post.updated?.toInstant(UtcOffset.ZERO)

  //override val creator: IUser by lazy {
  //    val user = runBlocking {
  //        Authentication.api!!.getPersonDetails(personId = data.post.creatorId)
  //    }
  //    LemmyUser(instance, user.personView)
  //}
    override val creator: IUser
      get() = LemmyUser(instance, data.creator)

    override val score: Int
        get() = data.counts.score

    override val myVote: VoteDirection
        get() = when (data.myVote?.let { it > 0 }) {
            null -> { VoteDirection.NO_VOTE }
            true -> { VoteDirection.UPVOTE }
            false -> { VoteDirection.DOWNVOTE }
        }

    override val upvoteRatio: Double
        get() = data.counts.run { upvotes.toDouble() / (upvotes + downvotes) }

    override val commentCount: Int
        get() = data.counts.comments
}
