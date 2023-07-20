package ltd.ucode.slide.data.lemmy.post

import ltd.ucode.network.lemmy.api.response.GetPostsResponse
import ltd.ucode.network.lemmy.data.type.PostView
import ltd.ucode.slide.data.common.content.IContentDatabase
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.lemmy.post.GetPostResponseMarshaller.copy

object GetPostsResponseMarshaller {
    fun GetPostsResponse.toPosts(contentDatabase: IContentDatabase, domain: String): List<Post> {
        return posts.map {
            it.toPost(
                siteRowId = contentDatabase.sites.get(it.community.instanceId.id, domain),
                groupRowId = contentDatabase.groups.get(it.community.id.id, domain),
                userRowId = contentDatabase.users.get(it.creator.id.id, domain),
                languageRowId = contentDatabase.languages.get(it.post.languageId.id, domain),
            )
        }
    }

    fun GetPostsResponse.toPosts(siteRowId: Int, groupRowId: Int, userRowId: Int, languageRowId: Int): List<Post> {
        return posts.map {
            Post(
                siteRowId = siteRowId,
                groupRowId = groupRowId,
                userRowId = userRowId,
                languageRowId = languageRowId,
                postId = it.post.id.id,
                uri = it.post.apId,
            )
                .copy(it.post)
                .copy(it.counts)
        }
    }

    fun PostView.toPost(siteRowId: Int, groupRowId: Int, userRowId: Int, languageRowId: Int): Post {
        return Post(
            siteRowId = siteRowId,
            groupRowId = groupRowId,
            userRowId = userRowId,
            languageRowId = languageRowId,
            postId = this.post.id.id,
            uri = this.post.apId,
        )
            .copy(this.post)
            .copy(this.counts)
    }
}
