package me.ccrama.redditslide.SubmissionViews

import com.lusfold.androidkeyvaluestore.KVStore
import ltd.ucode.slide.data.IPost

object ReadLater {
    fun setReadLater(s: IPost, readLater: Boolean) {
        if (readLater) {
            KVStore.getInstance()
                .insert("readLater" + s.permalink, System.currentTimeMillis().toString())
        } else {
            if (isToBeReadLater(s)) {
                KVStore.getInstance().delete("readLater" + s.permalink)
            }
        }
    }

    fun isToBeReadLater(s: IPost): Boolean {
        return KVStore.getInstance().getByContains("readLater" + s.permalink).isNotEmpty()
    }
}
