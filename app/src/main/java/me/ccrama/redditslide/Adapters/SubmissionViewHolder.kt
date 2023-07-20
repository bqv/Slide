package me.ccrama.redditslide.Adapters

import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ltd.ucode.slide.R
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.SubmissionViews.HeaderImageLinkView
import me.ccrama.redditslide.views.CommentOverflow

class SubmissionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val title: SpoilerRobotoTextView
    val contentTitle: TextView
    val contentURL: TextView
    val score: TextView
    val comments: TextView
    val info: TextView
    val menu: View
    val mod: View
    val hide: View
    val upvote: View
    val thumbimage: View
    val secondMenu: View
    val downvote: View
    val edit: View
    val leadImage: HeaderImageLinkView
    val firstTextView: SpoilerRobotoTextView
    val commentOverflow: CommentOverflow
    val save: View
    val flairText: TextView
    val body: SpoilerRobotoTextView
    val innerRelative: RelativeLayout

    init {
        title = v.findViewById(R.id.title)
        info = v.findViewById(R.id.information)
        hide = v.findViewById(R.id.hide)
        menu = v.findViewById(R.id.menu)
        mod = v.findViewById(R.id.mod)
        downvote = v.findViewById(R.id.downvote)
        upvote = v.findViewById(R.id.upvote)
        leadImage = v.findViewById(R.id.headerimage)
        contentTitle = v.findViewById(R.id.contenttitle)
        secondMenu = v.findViewById(R.id.secondMenu)
        flairText = v.findViewById(R.id.text)
        thumbimage = v.findViewById(R.id.thumbimage2)
        contentURL = v.findViewById(R.id.contenturl)
        save = v.findViewById(R.id.save)
        edit = v.findViewById(R.id.edit)
        body = v.findViewById(R.id.body)
        score = v.findViewById(R.id.score)
        comments = v.findViewById(R.id.comments)
        firstTextView = v.findViewById(R.id.firstTextView)
        commentOverflow = v.findViewById(R.id.commentOverflow)
        innerRelative = v.findViewById(R.id.innerrelative)
    }
}
