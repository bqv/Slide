package ltd.ucode.network.lemmy.data

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.ContentType
import ltd.ucode.network.SingleVote
import ltd.ucode.network.data.IPost
import ltd.ucode.network.data.IUser
import ltd.ucode.network.lemmy.data.type.PostView
import ltd.ucode.util.text.Markdown
import net.dean.jraw.models.Submission.ThumbnailType
import java.net.URL

open class LemmyPost(val instance: String, val data: PostView) : IPost() {
    override val postId: Int
        get() = data.post.id.id

    override val rowId: Int
        get() = data.post.id.id

    override val title: String
        get() = data.post.name

    override val link: String
        get() = data.post.url.orEmpty()

    override val body: String
        get() = data.post.body.orEmpty()

    override val bodyHtml: String by lazy {
        Markdown.parseToHtml(data.post.body)
    }

    override val isLocked: Boolean
        get() = data.post.isLocked

    override val isNsfw: Boolean
        get() = data.post.isNsfw

    override val groupName: String
        get() = data.community.name
    override val groupRowId: Int
        get() = data.community.id.id

    override val uri: String
        get() = data.post.apId

    override val thumbnailUrl: String?
        get() = data.post.thumbnailUrl

    override val thumbnailType: ThumbnailType
        get() = (data.post.thumbnailUrl?.let { ThumbnailType.URL }) ?: ThumbnailType.NONE

    override val contentType: ContentType.Type?
        get() = when { // ContentType.getContentType(String)?
            this.link == null -> {
                if (body.isNullOrBlank()) ContentType.Type.NONE
                else ContentType.Type.SELF
            }
            isSpoiler -> { ContentType.Type.SPOILER }
            listOf("gif")
                .contains(extension?.lowercase()) -> { ContentType.Type.GIF }
            listOf("mp4", "webm")
                .contains(extension?.lowercase()) -> { ContentType.Type.VIDEO }
            else -> domain.orEmpty().lowercase().run {
                when {
                    listOf("v.redd.it")
                        .any(::endsWith) -> { ContentType.Type.VREDDIT_DIRECT }
                    listOf("redd.it", "reddit.com")
                        .any(::endsWith) -> { ContentType.Type.REDDIT }
                    listOf("imgur.com")
                        .any(::endsWith) -> { ContentType.Type.IMGUR }
                    listOf("gfycat.com")
                        .any(::endsWith) -> { ContentType.Type.GIF } // ?
                    listOf("tumblr.com")
                        .any(::endsWith) -> { ContentType.Type.TUMBLR }
                    listOf("deviantart.com")
                        .any(::endsWith) -> { ContentType.Type.DEVIANTART }
                    listOf("xkcd.org")
                        .any(::endsWith) -> { ContentType.Type.XKCD }

                    // ALBUM, EMBEDDED, EXTERNAL, VREDDIT_REDIRECT, STREAMABLE

                    listOf("jpg", "jpeg", "png", "bmp", "webp", "tiff", "tif")
                        .contains(extension?.lowercase()) -> { ContentType.Type.IMAGE }
                    listOf("svg")
                        .contains(extension?.lowercase()) -> { ContentType.Type.IMAGE }
                    else -> ContentType.Type.LINK
                }
            }
        }

    override val discovered: Instant
        get() = TODO("Not yet implemented")

    override val created: Instant
        get() = data.post.published.toInstant(TimeZone.UTC)

    override val updated: Instant?
        get() = data.post.updated?.toInstant(TimeZone.UTC)

    override val contentDescription: String
        get() = URL(data.community.actorId).host

    //override val creator: IUser by lazy {
  //    val user = runBlocking {
  //        Authentication.api!!.getPersonDetails(personId = data.post.creatorId)
  //    }
  //    LemmyUser(instance, user.personView)
  //}
    override val user: IUser
      get() = LemmyUser(instance, data.creator)

    override val score: Int
        get() = data.counts.score

    override val myVote: SingleVote
        get() = SingleVote.of(data.myVote)

    override val upvoteRatio: Double
        get() = data.counts.run { upvotes.toDouble() / (upvotes + downvotes) }

    override val upvotes: Int
        get() = TODO("Not yet implemented")
    override val downvotes: Int
        get() = TODO("Not yet implemented")

    override val comments: Int
        get() = data.counts.comments

    companion object {
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is IPost) return false
        if (other !is LemmyPost) return false
        return uri == other.uri
    }

    override fun hashCode(): Int = uri.hashCode()
}
