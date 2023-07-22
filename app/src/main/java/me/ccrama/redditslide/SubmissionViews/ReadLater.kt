package me.ccrama.redditslide.SubmissionViews

import ltd.ucode.network.data.IPost
import ltd.ucode.slide.App

object ReadLater {
    fun setReadLater(s: IPost, readLater: Boolean) {
        if (readLater) {
            App.contentDatabase.seen
                .insert("readLater" + s.uri, System.currentTimeMillis().toString())
        } else {
            if (isToBeReadLater(s)) {
                App.contentDatabase.seen.delete("readLater" + s.uri)
            }
        }
    }

    fun isToBeReadLater(s: IPost): Boolean {
        return App.contentDatabase.seen.getByContains("readLater" + s.uri).isNotEmpty()
    }
}
