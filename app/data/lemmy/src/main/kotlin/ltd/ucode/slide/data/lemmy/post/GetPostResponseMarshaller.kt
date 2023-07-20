package ltd.ucode.slide.data.lemmy.post

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.api.response.GetPostResponse
import ltd.ucode.network.lemmy.data.type.PostAggregates
import ltd.ucode.slide.data.common.content.IContentDatabase
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.network.lemmy.data.type.Post as LemmyPost

object GetPostResponseMarshaller {
    fun GetPostResponse.toPost(contentDatabase: IContentDatabase, domain: String): Post {
        return toPost(
            siteRowId = contentDatabase.sites.get(this.postView.community.instanceId.id, domain),
            groupRowId = contentDatabase.groups.get(this.communityView.community.id.id, domain),
            userRowId = contentDatabase.users.get(this.postView.creator.id.id, domain),
            languageRowId = contentDatabase.languages.get(this.postView.post.languageId.id, domain),
        )
    }

    fun GetPostResponse.toPost(siteRowId: Int, groupRowId: Int, userRowId: Int, languageRowId: Int): Post {
        return Post(
            siteRowId = siteRowId,
            groupRowId = groupRowId,
            userRowId = userRowId,
            languageRowId = languageRowId,
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
