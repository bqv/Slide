package ltd.ucode.lemmy.data

import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.data.IPost

class LemmyPost(val instance: String, val data: PostView) : IPost() {
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
}
