package ltd.ucode.reddit.data

import kotlinx.datetime.Instant
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.data.IGroup
import ltd.ucode.slide.data.IIdentifier
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails
import net.dean.jraw.models.VoteDirection

class RedditSubmission(val data: Submission) : IPost() {
    override val id: String
        get() = data.id

    override val title: String
        get() = data.title

    override val url: String?
        get() = data.url

    override val body: String?
        get() = data.selftext

    override val bodyHtml: String?
        get() = data.dataNode["body_html"]?.asText()

    override val isLocked: Boolean
        get() = data.isLocked

    override val groupName: String
        get() = data.subredditName
    override val groupId: IIdentifier<IGroup>?
        get() = TODO("Not yet implemented")

    override val link: String
        get() = data.fullName // on reddit: Kind + UniqueId

    override val permalink: String
        get() = data.fullName // on reddit: Kind + UniqueId

    override val isArchived: Boolean
        get() = data.isArchived

    override val isContest: Boolean
        get() = data.dataNode["contest_mode"].asBoolean()

    override val isHidden: Boolean
        get() = data.isHidden

    override val isNsfw: Boolean
        get() = data.isNsfw

    override val published: Instant
        get() = data.created.time.let(Instant::fromEpochMilliseconds)

    override val updated: Instant?
        get() = data.edited?.time?.let(Instant::fromEpochMilliseconds)

    override val comments: Iterable<CommentNode>
        get() = data.comments

    override val thumbnails: Thumbnails?
        get() = data.thumbnails

    override val thumbnailType: ThumbnailType
        get() = data.thumbnailType

    override val contentType: ContentType.Type?
        get() = ContentType.getContentType(data)

    override val flair: Flair
        get() = data.submissionFlair

    override val creator: IUser
        get() = RedditUser(data.author)

    override val score: Int
        get() = data.score

    override val myVote: VoteDirection
        get() = data.vote

    override val hasPreview: Boolean
        get() = data.dataNode.has("preview") &&
                data.dataNode["preview"]["images"][0]["source"].has("height")

    override val preview: String?
        get() = data.dataNode["preview"]["images"][0]["source"]["url"].asText()

    override val upvoteRatio: Double
        get() = data.upvoteRatio

    override val commentCount: Int
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
