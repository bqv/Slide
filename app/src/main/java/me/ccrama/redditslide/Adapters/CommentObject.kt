package me.ccrama.redditslide.Adapters

import ltd.ucode.network.lemmy.data.type.CommentView

open class CommentObject {
    @JvmField
    var id: Int = 0
    open fun isComment(): Boolean {
        return false
    }

    @JvmField
    var comment: CommentView? = null
}
