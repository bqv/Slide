package ltd.ucode.lemmy.data

import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.data.IPost
import net.dean.jraw.models.CommentNode

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

    override val groupName: String
        get() = data.community.name

    override val permalink: String
        get() = "https://${instance}/post/${data.post.id}"

    override val published: Instant
        get() = data.post.published.toInstant(UtcOffset.ZERO)

    override val comments: Iterable<CommentNode>
        get() = emptyList()
}
