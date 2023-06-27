package ltd.ucode.lemmy.data

import ltd.ucode.lemmy.data.type.PostView
import me.ccrama.redditslide.CommentCacheAsync.CommentStore

class LemmyPostWithComments(instance: String, data: PostView,
                            val commentStore: CommentStore) : LemmyPost(instance, data) {
}
