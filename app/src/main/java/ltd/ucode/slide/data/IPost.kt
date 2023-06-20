package ltd.ucode.slide.data

import kotlinx.datetime.Instant
import ltd.ucode.reddit.data.RedditSubmission
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Submission

abstract class IPost {
    abstract val id: String
    abstract val title: String
    abstract val url: String?
    abstract val body: String?
    abstract val isLocked: Boolean
    abstract val groupName: String
    abstract val permalink: String
    abstract val published: Instant
    abstract val comments: Iterable<CommentNode> // TODO: reddit-specific

    val submission: Submission by lazy {
        (this as RedditSubmission).data // reddit-specific
    }

    open val isArchived: Boolean
        get() = false // reddit-specific
    open val isContest: Boolean
        get() = false // reddit-specific

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Submission -> submission == other
            is IPost -> permalink == other.permalink
            else -> false
        }
    }
}
