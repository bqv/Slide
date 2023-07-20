package me.ccrama.redditslide.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.itemanimators.SlideUpAlphaAnimator
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Adapters.SubredditAdapter
import me.ccrama.redditslide.Adapters.SubredditNames
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.views.PreCachingLayoutManager
import net.dean.jraw.models.Subreddit

class SubredditListView : Fragment() {
    var posts: SubredditNames? = null
    lateinit var rv: RecyclerView
    private var visibleItemCount = 0
    private var pastVisiblesItems = 0
    private var totalItemCount = 0
    var adapter: SubredditAdapter? = null
    var where: String? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper: Context = ContextThemeWrapper(activity, ColorPreferences(inflater.context).getThemeSubreddit(where))
        val v = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.fragment_verticalcontent, container, false)
        rv = v.findViewById(R.id.vertical_content)
        val mLayoutManager: RecyclerView.LayoutManager = PreCachingLayoutManager(activity)
        rv.layoutManager = mLayoutManager
        rv.itemAnimator = SlideUpAlphaAnimator().withInterpolator(LinearOutSlowInInterpolator())
        mSwipeRefreshLayout = v.findViewById(R.id.activity_main_swipe_refresh_layout)
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors("no sub", context))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp
        mSwipeRefreshLayout.setProgressViewOffset(false,
            Constants.TAB_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.TAB_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM)
        v.findViewById<View>(R.id.post_floating_action_button).visibility = View.GONE
        doAdapter()
        return v
    }

    var main = false
    fun doAdapter() {
        mSwipeRefreshLayout.post { mSwipeRefreshLayout.isRefreshing = true }
        posts = SubredditNames(where, context, this@SubredditListView)
        adapter = SubredditAdapter(activity!!, posts!!, rv, where!!, this)
        rv.adapter = adapter
        posts!!.loadMore(mSwipeRefreshLayout.context, true, where)
        mSwipeRefreshLayout.setOnRefreshListener { refresh() }
        rv.addOnScrollListener(object : ToolbarScrollHideHandler((requireActivity() as BaseActivity).mToolbar, requireActivity().findViewById(R.id.header)) {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!posts!!.loading && !posts!!.nomore) {
                    visibleItemCount = rv.layoutManager!!.childCount
                    totalItemCount = rv.layoutManager!!.itemCount
                    pastVisiblesItems = (rv.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
                    if (visibleItemCount + pastVisiblesItems >= totalItemCount) {
                        posts!!.loading = true
                        LogUtil.v("Loading more")
                        posts!!.loadMore(mSwipeRefreshLayout.context, false, where)
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        where = bundle!!.getString("id", "")
    }

    private fun refresh() {
        posts!!.loadMore(mSwipeRefreshLayout.context, true, where)
    }

    fun updateSuccess(submissions: List<Subreddit?>?, startIndex: Int) {
        if (activity != null) {
            requireActivity().runOnUiThread {
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.isRefreshing = false
                }
                if (startIndex > 0) {
                    adapter!!.notifyItemRangeInserted(startIndex + 1, posts!!.posts.size)
                } else {
                    adapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    fun updateError() {
        mSwipeRefreshLayout.isRefreshing = false
        adapter!!.setError(true)
    }
}
