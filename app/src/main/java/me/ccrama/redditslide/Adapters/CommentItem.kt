package me.ccrama.redditslide.Adapters

import ltd.ucode.network.lemmy.data.type.CommentView

class CommentItem(node: CommentView) : CommentObject() {
    init {
        comment = node
        id = comment!!.comment.id.id
    }

    override fun isComment(): Boolean {
        return true
    }
}
