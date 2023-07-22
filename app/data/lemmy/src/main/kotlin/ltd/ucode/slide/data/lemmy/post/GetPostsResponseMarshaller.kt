package ltd.ucode.slide.data.lemmy.post

import ltd.ucode.network.lemmy.api.response.GetPostsResponse
import ltd.ucode.network.lemmy.data.type.PostView
import ltd.ucode.slide.data.common.entity.Group
import ltd.ucode.slide.data.common.entity.Language
import ltd.ucode.slide.data.common.entity.Post
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.User
import ltd.ucode.slide.data.lemmy.post.GetPostResponseMarshaller.copy

object GetPostsResponseMarshaller {
    fun GetPostsResponse.toPosts(site: Site, group: Group, user: User, language: Language): List<Post> {
        return posts.map {
            Post(
                siteRowId = site.rowId,
                groupRowId = group.rowId,
                userRowId = user.rowId,
                languageRowId = language.rowId,
                postId = it.post.id.id,
                uri = it.post.apId,
            )
                .copy(it.post)
                .copy(it.counts)
        }
    }

    fun PostView.toPost(site: Site, group: Group, user: User, language: Language): Post {
        return Post(
            siteRowId = site.rowId,
            groupRowId = group.rowId,
            userRowId = user.rowId,
            languageRowId = language.rowId,
            postId = this.post.id.id,
            uri = this.post.apId,
        )
            .copy(this.post)
            .copy(this.counts)
    }
}
