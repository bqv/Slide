package ltd.ucode.reddit.data

import ltd.ucode.slide.data.IPost
import net.dean.jraw.models.Submission

class RedditSubmission(val data: Submission) : IPost() {
    override val title: String
        get() = data.title

    override val url: String?
        get() = data.url

    override val body: String?
        get() = data.selftext

    override val isLocked: Boolean
        get() = data.isLocked

    override val groupName: String
        get() = data.subredditName

    override val permalink: String
        get() = data.fullName // on reddit: Kind + UniqueId

    override val isArchived: Boolean
        get() = data.isArchived

    override val isContest: Boolean
        get() = data.dataNode["contest_mode"].asBoolean()
}
