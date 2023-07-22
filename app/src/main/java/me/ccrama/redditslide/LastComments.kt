package me.ccrama.redditslide

import ltd.ucode.network.data.IPost
import ltd.ucode.slide.App

object LastComments {
    var commentsSince: HashMap<String, Int>? = null
    fun setCommentsSince(submissions: List<IPost>) {
        if (commentsSince == null) {
            commentsSince = HashMap()
        }
        val m = App.contentDatabase.seen
        try {
            for (s in submissions) {
                val fullname = s.uri

                val contains = App.contentDatabase.seen.has("comments$fullname")
                if (contains) {
                    commentsSince!![fullname] = Integer.valueOf(m["comments$fullname"])
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun commentsSince(s: IPost): Int {
        return if (commentsSince != null && commentsSince!!.containsKey(s.uri)) s.commentCount - commentsSince!![s.uri]!! else 0
    }

    @JvmStatic
    fun setComments(s: IPost) {
        if (commentsSince == null) {
            commentsSince = HashMap()
        }
        App.contentDatabase.seen.insertOrUpdate("comments" + s.uri, s.commentCount.toString())
        commentsSince!![s.uri] = s.commentCount
    }
}
