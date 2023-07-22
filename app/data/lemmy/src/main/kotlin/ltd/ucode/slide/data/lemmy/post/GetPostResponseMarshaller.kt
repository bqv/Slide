package ltd.ucode.slide.data.lemmy.post

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.api.response.GetPostResponse
import ltd.ucode.network.lemmy.data.type.PostAggregates
import ltd.ucode.slide.data.common.entity.Group
import ltd.ucode.slide.data.common.entity.Language
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.User
import ltd.ucode.network.lemmy.data.type.Post as LemmyPost

object GetPostResponseMarshaller {
    fun GetPostResponse.toPost(site: Site, group: Group, user: User, language: Language): Post {
        return Post(
            siteRowId = site.rowId,
            groupRowId = group.rowId,
            userRowId = user.rowId,
            languageRowId = language.rowId,
            postId = this.postView.post.id.id,
            uri = this.postView.post.apId,
        )
            .copy(this.postView.post)
            .copy(this.postView.counts)
    }

    internal fun Post.copy(other: LemmyPost): Post {
        return copy(
            postId = other.id.id,

            uri = other.apId,

            title = other.name,
            link = other.url.orEmpty(),
            body = other.body.orEmpty(),
            thumbnailUrl = other.thumbnailUrl.orEmpty(),

            isNsfw = other.isNsfw,
            isLocked = other.isLocked,
            isDeleted = other.isDeleted,

            created = other.published.toInstant(TimeZone.UTC),
            updated = other.updated?.toInstant(TimeZone.UTC),
        )
    }

    internal fun Post.copy(other: PostAggregates): Post {
        return copy(
            upvotes = other.upvotes,
            downvotes = other.downvotes,
            commentCount = other.comments,
            hotRank = other.hotRank,
            activeRank = other.activeRank
        )
    }
}
