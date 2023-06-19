package ltd.ucode.slide.data

import ltd.ucode.reddit.data.RedditSubmission
import net.dean.jraw.models.Submission

abstract class IPost {
    abstract val title: String
    abstract val url: String?
    abstract val body: String?
    abstract val isLocked: Boolean
    abstract val groupName: String
    abstract val permalink: String

    val submission: Submission
        get() = (this as RedditSubmission).data // reddit-specific
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
