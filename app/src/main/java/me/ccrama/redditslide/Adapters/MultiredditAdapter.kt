package me.ccrama.redditslide.Adapters

import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.commentsScreen.CommentsScreen
import me.ccrama.redditslide.Fragments.MultiredditView
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.CreateCardView.CreateView
import me.ccrama.redditslide.views.CreateCardView.colorCard
import net.dean.jraw.models.Submission

class MultiredditAdapter(
    var context: ComponentActivity,
    var dataSet: MultiredditPosts,
    private val listView: RecyclerView,
    refreshLayout: SwipeRefreshLayout,
    baseView: MultiredditView
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter {
    private val postRepository get() = baseView.postRepository
    private val commentRepository get() = baseView.commentRepository

    var seen: List<Submission>
    private val LOADING_SPINNER = 5
    private val NO_MORE = 3
    private val SPACER = 6
    var refreshLayout: SwipeRefreshLayout
    private var baseView: MultiredditView
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
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        tag++
        return if (i == SPACER) {
            val v =
                LayoutInflater.from(viewGroup.context).inflate(R.layout.spacer, viewGroup, false)
            SpacerViewHolder(v)
        } else if (i == LOADING_SPINNER) {
            val v =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.loadingmore, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else if (i == NO_MORE) {
            val v =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.nomoreposts, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else {
            val v = CreateView(viewGroup)
            SubmissionViewHolder(v)
        }
    }

    var clicked = 0

    init {
        seen = ArrayList()
        this.refreshLayout = refreshLayout
        this.baseView = baseView
    }

    fun refreshView() {
        val a = listView.itemAnimator
        listView.itemAnimator = null
        notifyItemChanged(clicked)
        listView.postDelayed({ listView.itemAnimator = a }, 500)
    }

    fun refreshView(seen: ArrayList<Int>) {
        listView.itemAnimator = null
        val a = listView.itemAnimator
        for (i in seen) {
            notifyItemChanged(i + 1)
        }
        listView.postDelayed({ listView.itemAnimator = a }, 500)
    }

    override fun onBindViewHolder(holder2: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if (holder2 is SubmissionViewHolder) {
            val holder = holder2
            val submission: Submission = dataSet.posts[i].submission!!
            colorCard(
                submission.subredditName.lowercase(),
                holder.itemView,
                "multi" + dataSet.multiReddit!!.displayName,
                true
            )
            holder.itemView.setOnClickListener {
                if (Authentication.didOnline || submission.comments != null) {
                    holder.title.alpha = 0.65f
                    holder.leadImage.alpha = 0.65f
                    holder.thumbimage.alpha = 0.65f
                    val i2 = Intent(context, CommentsScreen::class.java)
                    i2.putExtra(CommentsScreen.EXTRA_PAGE, holder2.getBindingAdapterPosition() - 1)
                    i2.putExtra(CommentsScreen.EXTRA_MULTIREDDIT, dataSet.multiReddit!!.displayName)
                    context.startActivityForResult(i2, 940)
                    i2.putExtra("fullname", submission.fullName)
                    clicked = holder2.getBindingAdapterPosition()
                } else {
                    val s = Snackbar.make(
                        holder.itemView,
                        R.string.offline_comments_not_loaded,
                        Snackbar.LENGTH_SHORT
                    )
                    LayoutUtils.showSnackbar(s)
                }
            }
            PopulateSubmissionViewHolder(postRepository, commentRepository).populateSubmissionViewHolder(
                holder,
                RedditSubmission(submission),
                context,
                false,
                false,
                dataSet.posts,
                listView,
                true,
                false,
                "multi" + dataSet.multiReddit!!.displayName.lowercase(),
                null
            )
        }
        if (holder2 is SubmissionFooterViewHolder) {
            val handler = Handler()
            val r = Runnable {
                notifyItemChanged(dataSet.posts.size + 1) // the loading spinner to replaced by nomoreposts.xml
            }
            handler.post(r)
            if (holder2.itemView.findViewById<View?>(R.id.reload) != null) {
                holder2.itemView.findViewById<View>(R.id.reload).setOnClickListener {
                    dataSet.loadMore(
                        context,
                        baseView,
                        true,
                        this@MultiredditAdapter
                    )
                }
            }
        }
        if (holder2 is SpacerViewHolder) {
            val height = context.findViewById<View>(R.id.header).height
            holder2.itemView.findViewById<View>(R.id.height).layoutParams =
                LinearLayout.LayoutParams(holder2.itemView.width, height)
            if (listView.layoutManager is CatchStaggeredGridLayoutManager) {
                val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height
                )
                layoutParams.isFullSpan = true
                holder2.itemView.layoutParams = layoutParams
            }
        }
    }

    class SubmissionFooterViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 2 // Always account for footer
        }
    }
}
