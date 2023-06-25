package me.ccrama.redditslide.Adapters

import ltd.ucode.lemmy.data.type.CommentView
import net.dean.jraw.models.MoreChildren

class MoreChildItem(node: CommentView, children: MoreChildren) : CommentObject() {
    @JvmField
    var children: MoreChildren
    override fun isComment(): Boolean {
        return false
    }

    init {
        comment = node
        this.children = children
        id = -node.comment.id.id
    }
}
