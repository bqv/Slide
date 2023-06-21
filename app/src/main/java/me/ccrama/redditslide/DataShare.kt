package me.ccrama.redditslide

import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Adapters.CommentObject
import net.dean.jraw.models.PrivateMessage

object DataShare {
    var sharedSubmission: IPost? = null

    //   public static Submission notifs;
    @JvmField
    var sharedMessage: PrivateMessage? = null
    @JvmField
    var sharedComments: ArrayList<CommentObject>? = null
    @JvmField
    var subAuthor: String? = null
}
