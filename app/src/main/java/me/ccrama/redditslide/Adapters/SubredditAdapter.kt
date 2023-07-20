package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Fragments.SubredditListView
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.CommentOverflow

class SubredditAdapter(context: Activity, dataSet: SubredditNames, listView: RecyclerView, where: String, displayer: SubredditListView) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IFallibleAdapter {
    private val listView: RecyclerView
    var context: Activity
    var dataSet: SubredditNames
    private val LOADING_SPINNER = 5
    private val NO_MORE = 3
    private val SPACER = 6
    var displayer: SubredditListView

    override fun setError(b: Boolean) {
        listView.adapter = ErrorAdapter()
    }

    override fun undoSetError() {
        listView.adapter = this
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position <= 0 && !dataSet.posts.isEmpty()) {
            return SPACER
        } else if (!dataSet.posts.isEmpty()) {
            position -= 1
        }
        if (position == dataSet.posts.size && !dataSet.posts.isEmpty() && !dataSet.nomore) {
            return LOADING_SPINNER
        } else if (position == dataSet.posts.size && dataSet.nomore) {
            return NO_MORE
        }
        return 1
    }

    var tag = 1

    init {
        val where1 = where.lowercase()
        this.listView = listView
        this.dataSet = dataSet
        this.context = context
        this.displayer = displayer
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        tag++
        return if (i == SPACER) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.spacer, viewGroup, false)
            SpacerViewHolder(v)
        } else if (i == LOADING_SPINNER) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.loadingmore, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else if (i == NO_MORE) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.nomoreposts, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.subfordiscover, viewGroup, false)
            SubredditViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder2: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if (holder2 is SubredditViewHolder) {
            val holder = holder2
            val sub = dataSet.posts[i]
            holder.name.text = sub.displayName
            if (sub.localizedSubscriberCount != null) {
                holder.subscribers.text = context.getString(R.string.subreddit_subscribers_string,
                    sub.localizedSubscriberCount)
            } else {
                holder.subscribers.visibility = View.GONE
            }
            holder.color.setBackgroundResource(R.drawable.circle)
            BlendModeUtil.tintDrawableAsModulate(
                holder.color.background,
                Palette.getColor(sub.displayName.lowercase()))
            holder.itemView.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    val inte = Intent(context, SubredditView::class.java)
                    inte.putExtra(SubredditView.EXTRA_SUBREDDIT, sub.displayName)
                    context.startActivityForResult(inte, 4)
                }
            })
            holder.overflow.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    val inte = Intent(context, SubredditView::class.java)
                    inte.putExtra(SubredditView.EXTRA_SUBREDDIT, sub.displayName)
                    context.startActivityForResult(inte, 4)
                }
            })
            holder.body.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    val inte = Intent(context, SubredditView::class.java)
                    inte.putExtra(SubredditView.EXTRA_SUBREDDIT, sub.displayName)
                    context.startActivityForResult(inte, 4)
                }
            })
            if (sub.dataNode["public_description_html"].asText() == "null") {
                holder.body.visibility = View.GONE
                holder.overflow.visibility = View.GONE
            } else {
                holder.body.visibility = View.VISIBLE
                holder.overflow.visibility = View.VISIBLE
                setViews(sub.dataNode["public_description_html"].asText().trim { it <= ' ' }, sub.displayName.lowercase(), holder.body, holder.overflow)
            }
            try {
                val state = if (sub.isUserSubscriber) View.VISIBLE else View.INVISIBLE
                holder.subbed.visibility = state
            } catch (e: Exception) {
                holder.subbed.visibility = View.INVISIBLE
            }
        } else if (holder2 is SubmissionFooterViewHolder) {
            val handler = Handler()
            val r = Runnable {
                notifyItemChanged(dataSet.posts.size + 1) // the loading spinner to replaced by nomoreposts.xml
            }
            handler.post(r)
            if (holder2.itemView.findViewById<View?>(R.id.reload) != null) {
                holder2.itemView.visibility = View.INVISIBLE
            }
        }
        if (holder2 is SpacerViewHolder) {
            val height = context.findViewById<View>(R.id.header).height
            holder2.itemView.findViewById<View>(R.id.height).layoutParams = LinearLayout.LayoutParams(holder2.itemView.width, height)
            if (listView.layoutManager is CatchStaggeredGridLayoutManager) {
                val layoutParams = StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                layoutParams.isFullSpan = true
                holder2.itemView.layoutParams = layoutParams
            }
        }
    }

    class SubmissionFooterViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 2 // Always account for footer
        }
    }

    private fun setViews(rawHTML: String, subredditName: String, firstTextView: SpoilerRobotoTextView, commentOverflow: CommentOverflow) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subredditName)
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subredditName)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }
}
