package ltd.ucode.slide.data

import kotlinx.datetime.Instant
import ltd.ucode.reddit.data.RedditSubmission
import me.ccrama.redditslide.ContentType
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails
import net.dean.jraw.models.VoteDirection
import java.net.URI

abstract class IPost {
    abstract val id: String
    abstract val title: String
    abstract val url: String?
    abstract val body: String?
    abstract val isLocked: Boolean
    abstract val isNsfw: Boolean
    abstract val groupName: String
    abstract val permalink: String
    abstract val published: Instant
    abstract val creator: IUser
    abstract val score: Int
    abstract val myVote: VoteDirection
    abstract val upvoteRatio: Double
    abstract val commentCount: Int

    val submission: Submission? by lazy {
        (this as? RedditSubmission)?.data // reddit-specific
    }

    val domain: String?
        get() = url?.let { URI(it).host }
    open val isArchived: Boolean
        get() = false // reddit-specific
    open val isContest: Boolean
        get() = false // reddit-specific
    open val isHidden: Boolean
        get() = false // reddit-specific
    open val comments: Iterable<CommentNode> // TODO: reddit-specific
        get() = emptyList()
    open val thumbnails: Thumbnails? // TODO: reddit-specific
        get() = null
    open val thumbnailType: ThumbnailType // TODO: reddit-specific
        get() = ThumbnailType.NONE
    open val contentType: ContentType.Type? // TODO: reddit-specific
        get() = null
    open val flair: Flair // TODO: reddit-specific
        get() = Flair(null, null)
    open val hasPreview: Boolean // TODO: reddit-specific
        get() = false
    open val preview: String? // TODO: reddit-specific
        get() = null

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Submission -> submission == other
            is IPost -> permalink == other.permalink
            else -> false
        }
    }
}
