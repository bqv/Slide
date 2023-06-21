package ltd.ucode.slide.data

import kotlinx.datetime.Instant
import ltd.ucode.reddit.data.RedditSubmission
import me.ccrama.redditslide.ContentType
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails
import net.dean.jraw.models.VoteDirection
import java.net.URI

abstract class IPost : IItem {
    abstract val id: String
    abstract val title: String
    abstract val url: String?
    abstract val body: String?
    abstract val isLocked: Boolean
    abstract val isNsfw: Boolean
    abstract val groupName: String
    abstract override val permalink: String
    abstract override val published: Instant
    abstract override val updated: Instant?
    abstract override val creator: IUser
    abstract override val score: Int
    abstract override val myVote: VoteDirection
    abstract override val upvoteRatio: Double
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
    override val isSaved: Boolean
        get() = false // reddit-specific
    open val isSpoiler: Boolean
        get() = false // TODO: hook up
    open val isFeatured: Boolean
        get() = false // TODO: hook up
    open val isOC: Boolean
        get() = true // TODO: reddit-specific
    open val bannedBy: String?
        get() = null // TODO: reddit-specific
    open val approvedBy: String?
        get() = null // TODO: reddit-specific
    open val timesSilvered: Int
        get() = 0 // TODO: reddit-specific
    open val timesGilded: Int
        get() = 0 // TODO: reddit-specific
    open val timesPlatinized: Int
        get() = 0 // TODO: reddit-specific
    open val moderatorReports: Map<String, String>
        get() = emptyMap() // TODO: reddit-specific
    open val userReports: Map<String, Int>
        get() = emptyMap() // TODO: reddit-specific
    open val regalia: DistinguishedStatus
        get() = DistinguishedStatus.NORMAL // TODO: reddit-specific
    open val comments: Iterable<CommentNode> // TODO: reddit-specific
        get() = emptyList()
    open val thumbnail: String? // TODO: reddit-specific
        get() = null
    open val thumbnails: Thumbnails? // TODO: reddit-specific
        get() = null
    open val thumbnailType: ThumbnailType // TODO: reddit-specific
        get() = ThumbnailType.NONE
    open val contentType: ContentType.Type? // TODO: reddit-specific
        get() = if (url != null) ContentType.Type.LINK else ContentType.Type.SELF
    open val contentDescription: String // TODO: reddit-specific
        get() = "lemmy"
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
