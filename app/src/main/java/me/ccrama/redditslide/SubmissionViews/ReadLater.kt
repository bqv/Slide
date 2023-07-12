package me.ccrama.redditslide.SubmissionViews

import com.lusfold.androidkeyvaluestore.KVStore
import ltd.ucode.network.data.IPost

object ReadLater {
    fun setReadLater(s: IPost, readLater: Boolean) {
        if (readLater) {
            KVStore.getInstance()
                .insert("readLater" + s.uri, System.currentTimeMillis().toString())
        } else {
            if (isToBeReadLater(s)) {
                KVStore.getInstance().delete("readLater" + s.uri)
            }
        }
    }

    fun isToBeReadLater(s: IPost): Boolean {
        return KVStore.getInstance().getByContains("readLater" + s.uri).isNotEmpty()
    }
}
