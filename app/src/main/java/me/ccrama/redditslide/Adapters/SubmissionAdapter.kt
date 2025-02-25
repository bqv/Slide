package me.ccrama.redditslide.Adapters

import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.network.data.IPost
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.getLayoutSettings
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.ui.commentsScreen.CommentsScreen
import ltd.ucode.slide.ui.main.MainActivity
import ltd.ucode.slide.ui.main.MainPagerAdapterComment
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment.Companion.createLayoutManager
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.SubredditView.SubredditPagerAdapterComment
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.CreateCardView.CreateView
import me.ccrama.redditslide.views.CreateCardView.colorCard

class SubmissionAdapter(
    var context: ComponentActivity, var dataSet: SubredditPosts, private val listView: RecyclerView?,
    subreddit: String, var displayer: SubmissionDisplay
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IFallibleAdapter {
    val postRepository: PostRepository get() = when {
        displayer is SubmissionsViewFragment -> { (displayer as SubmissionsViewFragment).postRepository }
        context is MainActivity -> { (context as MainActivity).postRepository }
        else -> { throw IllegalArgumentException() }
    }

    val commentRepository: CommentRepository get() = when {
        displayer is SubmissionsViewFragment -> { (displayer as SubmissionsViewFragment).commentRepository }
        context is MainActivity -> { (context as MainActivity).commentRepository }
        else -> { throw IllegalArgumentException() }
    }

    val subreddit = subreddit.lowercase()
    private val custom: Boolean = getLayoutSettings(subreddit)!!
    var seen: List<IPost> = ArrayList()
    private val LOADING_SPINNER = 5
    private val NO_MORE = 3
    private val SPACER = 6
    private val ERROR = 7
    var clicked = 0

    init {
        MainActivity.randomoverride = ""
    }

    var hasError = false

    override fun setError(b: Boolean) {
        listView!!.adapter = ErrorAdapter()
        hasError = true
        listView.layoutManager = createLayoutManager(
            LayoutUtils.getNumColumns(context.resources.configuration.orientation, context)
        )
    }

    override fun undoSetError() {
        listView!!.adapter = this
        hasError = false
        listView.layoutManager = createLayoutManager(
            LayoutUtils.getNumColumns(context.resources.configuration.orientation, context)
        )
    }

    override fun getItemId(position: Int): Long {
        var position = position
        if (position <= 0 && dataSet.posts.isNotEmpty()) {
            return SPACER.toLong()
        } else if (dataSet.posts.isNotEmpty()) {
            position -= 1
        }
        if (position == dataSet.posts.size && dataSet.posts.isNotEmpty()
            && !dataSet.offline
            && !dataSet.nomore
        ) {
            return LOADING_SPINNER.toLong()
        } else if (position == dataSet.posts.size && (dataSet.offline || dataSet.nomore)) {
            return NO_MORE.toLong()
        }
        return dataSet.posts[position].created.toEpochMilliseconds()
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position <= 0 && dataSet.posts.isNotEmpty()) {
            return SPACER
        } else if (dataSet.posts.isNotEmpty()) {
            position -= 1
        }
        if (position == dataSet.posts.size) {
            if (dataSet.error) {
                return ERROR
            } else if (dataSet.posts.isNotEmpty() && !dataSet.offline && !dataSet.nomore) {
                return LOADING_SPINNER
            } else if (dataSet.offline || dataSet.nomore) {
                return NO_MORE
            }
        }
        return 1
    }

    var tag = 1
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        tag++
        return when (i) {
            SPACER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.spacer, viewGroup, false)
                SpacerViewHolder(v)
            }
            LOADING_SPINNER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.loadingmore, viewGroup, false)
                SubmissionFooterViewHolder(v)
            }
            NO_MORE -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.nomoreposts, viewGroup, false)
                SubmissionFooterViewHolder(v)
            }
            ERROR -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.errorloadingcontent, viewGroup, false)
                v.findViewById<View>(R.id.retry).setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        dataSet.loadMore(
                            v.context,
                            displayer, false, dataSet.subreddit
                        )
                    }
                })
                SubmissionFooterViewHolder(v)
            }
            else -> {
                val v = CreateView(viewGroup)
                SubmissionViewHolder(v)
            }
        }
    }

    fun refreshView() {
        val a = listView!!.itemAnimator
        listView.itemAnimator = null
        notifyItemChanged(clicked)
        listView.postDelayed({ listView.itemAnimator = a }, 500)
    }

    fun refreshView(seen: ArrayList<Int>) {
        listView!!.itemAnimator = null
        val a = listView.itemAnimator
        for (i in seen) {
            notifyItemChanged(i + 1)
        }
        listView.postDelayed({ listView.itemAnimator = a }, 500)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val i = (pos - 1).coerceAtLeast(0)
        when (holder) {
            is SubmissionViewHolder -> {
                val submission = dataSet.posts[i]
                colorCard(
                    submission.groupName.lowercase(), holder.itemView,
                    subreddit,
                    subreddit == "frontpage" || subreddit == "mod" || subreddit == "friends" || subreddit == "all"
                        || subreddit.contains(".")
                        || subreddit.contains("+")
                )
                holder.itemView.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        if (Authentication.didOnline || submission.commentNodes != null) {
                            holder.title.alpha = 0.54f
                            holder.body.alpha = 0.54f
                            if (context is MainActivity) {
                                val a = context as MainActivity
                                if (a.singleMode
                                    && a.commentPager
                                    && a.adapter is MainPagerAdapterComment
                                ) {
                                    if (true || a.openingComments == submission) {
                                        clicked = holder.getBindingAdapterPosition()
                                        a.openingComments = submission
                                        a.toOpenComments = a.pager!!.currentItem + 1
                                        a.currentComment = holder.bindingAdapterPosition - 1
                                        (a.adapter as MainPagerAdapterComment?)!!.storedFragment =
                                            a.adapter!!.currentFragment
                                        (a.adapter as MainPagerAdapterComment?)!!.size =
                                            a.toOpenComments + 1
                                        try {
                                            a.adapter!!.notifyDataSetChanged()
                                        } catch (ignored: Exception) {
                                        }
                                    }
                                    a.pager!!.postDelayed({
                                        a.pager!!.setCurrentItem(
                                            a.pager!!.currentItem + 1,
                                            true
                                        )
                                    }, 400)
                                } else {
                                    val i2 = Intent(context, CommentsScreen::class.java)
                                    i2.putExtra(
                                        CommentsScreen.EXTRA_PAGE,
                                        holder.getBindingAdapterPosition() - 1
                                    )
                                    i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, subreddit)
                                    i2.putExtra(CommentsScreen.EXTRA_FULLNAME, submission.uri)
                                    i2.putExtra(CommentsScreen.EXTRA_POSTID, submission.rowId)
                                    context.startActivityForResult(i2, MainActivity.OPEN_POST_RESULT)
                                    clicked = holder.getBindingAdapterPosition()
                                }
                            } else if (context is SubredditView) {
                                val a = context as SubredditView
                                if (a.singleMode && a.commentPager) {
                                    if (a.openingComments !== submission.submission) {
                                        clicked = holder.getBindingAdapterPosition()
                                        a.openingComments = submission.submission
                                        a.currentComment = holder.bindingAdapterPosition - 1
                                        (a.adapter as SubredditPagerAdapterComment).storedFragment =
                                            a.adapter!!.currentFragment
                                        (a.adapter as SubredditPagerAdapterComment).size = 3
                                        a.adapter!!.notifyDataSetChanged()
                                    }
                                    a.pager!!.postDelayed({
                                        a.pager!!.setCurrentItem(
                                            a.pager!!.currentItem + 1,
                                            true
                                        )
                                    }, 400)
                                } else {
                                    val i2 = Intent(context, CommentsScreen::class.java)
                                    i2.putExtra(
                                        CommentsScreen.EXTRA_PAGE,
                                        holder.getBindingAdapterPosition() - 1
                                    )
                                    i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, subreddit)
                                    i2.putExtra(CommentsScreen.EXTRA_FULLNAME, submission.uri)
                                    i2.putExtra(CommentsScreen.EXTRA_POSTID, submission.rowId)
                                    i2.putParcelableArrayListExtra(
                                        CommentsScreen.EXTRA_POSTS,
                                        ArrayList(dataSet.posts.mapNotNull { it as Parcelable }))
                                    context.startActivityForResult(i2, MainActivity.OPEN_POST_RESULT)
                                    clicked = holder.getBindingAdapterPosition()
                                }
                            }
                        } else {
                            if (!appRestart.contains("offlinepopup")) {
                                AlertDialog.Builder(context)
                                    .setTitle(R.string.cache_no_comments_found)
                                    .setMessage(R.string.cache_no_comments_found_message)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int ->
                                        appRestart.edit()
                                            .putString("offlinepopup", "")
                                            .apply()
                                    }
                                    .show()
                            } else {
                                val s = Snackbar.make(
                                    holder.itemView,
                                    R.string.cache_no_comments_found_snackbar,
                                    Snackbar.LENGTH_SHORT
                                )
                                s.setAction(R.string.misc_more_info) {
                                    AlertDialog.Builder(context)
                                        .setTitle(R.string.cache_no_comments_found)
                                        .setMessage(R.string.cache_no_comments_found_message)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int ->
                                            appRestart.edit()
                                                .putString("offlinepopup", "")
                                                .apply()
                                        }
                                        .show()
                                }
                                LayoutUtils.showSnackbar(s)
                            }
                        }
                    }
                }
                )
                PopulateSubmissionViewHolder(postRepository, commentRepository).populateSubmissionViewHolder(
                    holder, submission,
                    context, false, false, dataSet.posts, listView!!, custom, dataSet.offline,
                    dataSet.subreddit.lowercase(), null
                )
            }
            is SubmissionFooterViewHolder -> {
                val handler = Handler()
                val r = Runnable {
                    notifyItemChanged(
                        dataSet.posts.size
                            + 1
                    ) // the loading spinner to replaced by nomoreposts.xml
                }
                handler.post(r)
                if (holder.itemView.findViewById<View?>(R.id.reload) != null) {
                    holder.itemView.findViewById<View>(R.id.reload)
                        .setOnClickListener { (displayer as SubmissionsViewFragment).forceRefresh() }
                }
            }
            is SpacerViewHolder -> {
                val header = context.findViewById<View>(R.id.header)
                var height = header.height
                if (height == 0) {
                    header.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    height = header.measuredHeight
                    holder.itemView.findViewById<View>(R.id.height).layoutParams =
                        LinearLayout.LayoutParams(holder.itemView.width, height)
                    if (listView!!.layoutManager is CatchStaggeredGridLayoutManager) {
                        val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, height
                        )
                        layoutParams.isFullSpan = true
                        holder.itemView.layoutParams = layoutParams
                    }
                } else {
                    holder.itemView.findViewById<View>(R.id.height).layoutParams =
                        LinearLayout.LayoutParams(holder.itemView.width, height)
                    if (listView!!.layoutManager is CatchStaggeredGridLayoutManager) {
                        val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, height
                        )
                        layoutParams.isFullSpan = true
                        holder.itemView.layoutParams = layoutParams
                    }
                }
            }
        }
    }

    class SubmissionFooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SpacerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 2 // Always account for footer
        }
    }

    fun performClick(adapterPosition: Int) {
        if (listView != null) {
            val holder = listView.findViewHolderForLayoutPosition(adapterPosition)
            if (holder != null) {
                val view = holder.itemView
                if (view != null) {
                    view.performClick()
                }
            }
        }
    }
}
