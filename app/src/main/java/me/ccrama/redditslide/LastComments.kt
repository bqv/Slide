package me.ccrama.redditslide

import com.lusfold.androidkeyvaluestore.KVStore
import com.lusfold.androidkeyvaluestore.core.KVManagerImpl
import com.lusfold.androidkeyvaluestore.utils.CursorUtils
import ltd.ucode.network.data.IPost

object LastComments {
    var commentsSince: HashMap<String, Int>? = null
    fun setCommentsSince(submissions: List<IPost>) {
        if (commentsSince == null) {
            commentsSince = HashMap()
        }
        val m = KVStore.getInstance()
        try {
            for (s in submissions) {
                val fullname = s.uri

                // Check if KVStore has a key containing comments + the fullname
                // This is necessary because the KVStore library is limited and Carlos didn't realize the performance impact
                val cur = m.execQuery(
                    "SELECT * FROM ? WHERE ? LIKE '%?%' LIMIT 1",
                    arrayOf(KVManagerImpl.TABLE_NAME, KVManagerImpl.COLUMN_KEY, "comments$fullname")
                )
                val contains = cur != null && cur.count > 0
                CursorUtils.closeCursorQuietly(cur)
                if (contains) {
                    commentsSince!![fullname] = Integer.valueOf(m["comments$fullname"])
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun commentsSince(s: IPost): Int {
        return if (commentsSince != null && commentsSince!!.containsKey(s.uri)) s.comments - commentsSince!![s.uri]!! else 0
    }

    @JvmStatic
    fun setComments(s: IPost) {
        if (commentsSince == null) {
            commentsSince = HashMap()
        }
        KVStore.getInstance().insertOrUpdate("comments" + s.uri, s.comments.toString())
        commentsSince!![s.uri] = s.comments
    }
}
