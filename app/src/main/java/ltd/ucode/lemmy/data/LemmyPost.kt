package ltd.ucode.lemmy.data

import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.data.type.CommunityId
import ltd.ucode.lemmy.data.type.PostId
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.data.IUser
import me.ccrama.redditslide.CommentCacheAsync.CommentStore
import me.ccrama.redditslide.ContentType
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.VoteDirection
import java.net.URL

open class LemmyPost(val instance: String, val data: PostView) : IPost() {
    override val id: String
        get() = data.post.id.toString() // TODO: drop string repr

    override val postId: PostId
        get() = data.post.id

    override val title: String
        get() = data.post.name

    override val url: String?
        get() = data.post.url

    override val body: String?
        get() = data.post.body

    override val isLocked: Boolean
        get() = data.post.isLocked

    override val isNsfw: Boolean
        get() = data.post.isNsfw

    override val groupName: String
        get() = data.community.name
    override val groupId: CommunityId?
        get() = data.community.id

    override val link: String
        get() = "https://${instance}/post/${data.post.id}"

    override val permalink: String
        get() = data.post.apId

    override val thumbnail: String?
        get() = data.post.thumbnailUrl

    override val thumbnailType: ThumbnailType
        get() = (data.post.thumbnailUrl?.let { ThumbnailType.URL }) ?: ThumbnailType.NONE

    override val contentType: ContentType.Type?
        get() = when { // ContentType.getContentType(String)?
            url == null -> {
                if (body.isNullOrBlank()) ContentType.Type.NONE
                else ContentType.Type.SELF
            }
            isSpoiler -> { ContentType.Type.SPOILER }
            listOf("gif")
                .contains(extension?.lowercase()) -> { ContentType.Type.GIF }
            listOf("mp4", "webm")
                .contains(extension?.lowercase()) -> { ContentType.Type.VIDEO }
            else -> domain!!.lowercase().run {
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

    override val published: Instant
        get() = data.post.published.toInstant(UtcOffset.ZERO)

    override val updated: Instant?
        get() = data.post.updated?.toInstant(UtcOffset.ZERO)

    override val contentDescription: String
        get() = URL(data.community.actorId).host

  //override val creator: IUser by lazy {
  //    val user = runBlocking {
  //        Authentication.api!!.getPersonDetails(personId = data.post.creatorId)
  //    }
  //    LemmyUser(instance, user.personView)
  //}
    override val creator: IUser
      get() = LemmyUser(instance, data.creator)

    override val score: Int
        get() = data.counts.score

    override val myVote: VoteDirection
        get() = when (data.myVote?.let { it > 0 }) {
            null -> { VoteDirection.NO_VOTE }
            true -> { VoteDirection.UPVOTE }
            false -> { VoteDirection.DOWNVOTE }
        }

    override val upvoteRatio: Double
        get() = data.counts.run { upvotes.toDouble() / (upvotes + downvotes) }

    override val commentCount: Int
        get() = data.counts.comments

    override fun withComments(comments: CommentStore): IPost {
        return LemmyPostWithComments(instance, data, comments)
    }
}
