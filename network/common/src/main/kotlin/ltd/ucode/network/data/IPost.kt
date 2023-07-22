package ltd.ucode.network.data

import kotlinx.datetime.Instant
import ltd.ucode.network.ContentType
import ltd.ucode.network.SingleVote
import ltd.ucode.network.trait.IVotable
import ltd.ucode.util.text.Markdown
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.Flair
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.extension

abstract class IPost : IVotable {

    abstract val postId: Int
    abstract val title: String
    abstract val link: String
    abstract val body: String
    abstract val isLocked: Boolean
    abstract val isNsfw: Boolean
    abstract val groupName: String
    abstract val groupRowId: Long
    abstract override val uri: String
    abstract override val discovered: Instant
    abstract override val created: Instant
    abstract override val updated: Instant?
    abstract override val user: IUser
    abstract override val score: Int
    abstract override val myVote: SingleVote
    abstract override val scoreRatio: Double
    abstract override val upvotes: Int
    abstract override val downvotes: Int
    abstract val commentCount: Int

    val domain: String?
        get() = link.ifBlank{null}?.let { URL(it).host }
    val extension: String?
        get() = link.ifBlank{null}?.let { Path(URL(it).path).extension }
    open val rowId: Long
        get() = postId.toLong()
    @delegate:Transient open val bodyHtml: String by lazy {
        Markdown.parseToHtml(body)
    }
    open val isArchived: Boolean
        get() = false // reddit-specific
    open val isContest: Boolean
        get() = false // reddit-specific
    open val isHidden: Boolean
        get() = false // reddit-specific
    open val isSaved: Boolean
        get() = false // reddit-specific
    open val isSpoiler: Boolean
        get() = false // TODO: hook up
    open val isFeatured: Boolean
        get() = false // TODO: hook up
    open val isOC: Boolean
        get() = false // TODO: reddit-specific
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
    open val commentNodes: Iterable<CommentNode> // TODO: reddit-specific
        get() = emptyList()
    open val thumbnailUrl: String? // TODO: reddit-specific
        get() = null
    open val thumbnails: Thumbnails? // TODO: reddit-specific
        get() = null
    open val thumbnailType: ThumbnailType // TODO: reddit-specific
        get() = ThumbnailType.NONE
    open val contentType: ContentType.Type? // TODO: reddit-specific
        get() = if (link.isNotBlank()) ContentType.Type.LINK else ContentType.Type.SELF
    open val contentDescription: String // TODO: reddit-specific
        get() = ""
    open val flair: Flair // TODO: reddit-specific
        get() = Flair(null, null)
    open val hasPreview: Boolean // TODO: reddit-specific
        get() = false
    open val preview: String? // TODO: reddit-specific
        get() = null

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is IPost) return false
        return uri == other.uri
    }

    override fun hashCode(): Int = uri.hashCode()
}
