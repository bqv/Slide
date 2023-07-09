package ltd.ucode.reddit.data

import kotlinx.datetime.Instant
import ltd.ucode.reddit.VoteDirectionExtensions.asSingleVote
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.SingleVote
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails

class RedditSubmission(val data: Submission) : IPost() {
    override val postId: Int
        get() = data.id.hashCode() // lol

    override val title: String
        get() = data.title

    override val link: String
        get() = data.url

    override val body: String?
        get() = data.selftext

    override val bodyHtml: String?
        get() = data.dataNode["body_html"]?.asText()

    override val isLocked: Boolean
        get() = data.isLocked

    override val groupName: String
        get() = data.subredditName
    override val groupId: Int
        get() = TODO("Not yet implemented")

    override val uri: String
        get() = data.fullName // on reddit: Kind + UniqueId

    override val isArchived: Boolean
        get() = data.isArchived

    override val isContest: Boolean
        get() = data.dataNode["contest_mode"].asBoolean()

    override val isHidden: Boolean
        get() = data.isHidden

    override val isNsfw: Boolean
        get() = data.isNsfw

    override val discovered: Instant
        get() = data.created.time.let(Instant::fromEpochMilliseconds)

    override val updated: Instant?
        get() = data.edited?.time?.let(Instant::fromEpochMilliseconds)

    override val commentNodes: Iterable<CommentNode>
        get() = data.comments

    override val thumbnails: Thumbnails?
        get() = data.thumbnails

    override val thumbnailType: ThumbnailType
        get() = data.thumbnailType

    override val contentType: ContentType.Type?
        get() = ContentType.getContentType(data)

    override val flair: Flair
        get() = data.submissionFlair

    override val user: IUser
        get() = RedditUser(data.author)

    override val score: Int
        get() = data.score

    override val myVote: SingleVote
        get() = data.vote.asSingleVote()

    override val hasPreview: Boolean
        get() = data.dataNode.has("preview") &&
                data.dataNode["preview"]["images"][0]["source"].has("height")

    override val preview: String?
        get() = data.dataNode["preview"]["images"][0]["source"]["url"].asText()

    override val upvoteRatio: Double
        get() = data.upvoteRatio

    override val upvotes: Int
        get() = TODO("Not yet implemented")

    override val downvotes: Int
        get() = TODO("Not yet implemented")

    override val comments: Int
        get() = data.commentCount

    companion object {
        @Deprecated("TODO")
        fun IPost.getSubmission(): Submission? {
            return (this as? RedditSubmission)?.data // reddit-specific
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is IPost) return false
        if (other !is RedditSubmission) return false
        return data == other.data
    }

    override fun hashCode(): Int = data.hashCode()
}
