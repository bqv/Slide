package me.ccrama.redditslide.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Adapters.ContributionAdapter
import me.ccrama.redditslide.Adapters.ContributionPosts
import me.ccrama.redditslide.Adapters.ContributionPostsSaved
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.PreCachingLayoutManager

class ContributionsView : Fragment() {
    private var totalItemCount = 0
    private var visibleItemCount = 0
    private var pastVisiblesItems = 0
    private var adapter: ContributionAdapter? = null
    private var posts: ContributionPosts? = null
    private var id: String? = null
    private var where: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_verticalcontent, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.vertical_content)
        val mLayoutManager = PreCachingLayoutManager(context)
        rv.layoutManager = mLayoutManager
        rv.setItemViewCacheSize(2)
        v.findViewById<View>(R.id.post_floating_action_button).visibility = View.GONE
        val mSwipeRefreshLayout = v.findViewById<SwipeRefreshLayout>(R.id.activity_main_swipe_refresh_layout)
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors(id, activity))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp
        mSwipeRefreshLayout.setProgressViewOffset(false,
            Constants.TAB_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.TAB_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM)
        mSwipeRefreshLayout.post { mSwipeRefreshLayout.isRefreshing = true }
        posts = if (where == "saved" && activity is Profile) ContributionPostsSaved(id, where, (activity as Profile?)!!.category) else ContributionPosts(id!!, where!!)
        adapter = if (where === "hidden") ContributionAdapter(requireActivity(), posts!!, rv, true) else ContributionAdapter(requireActivity(), posts!!, rv)
        rv.adapter = adapter
        posts!!.bindAdapter(adapter, mSwipeRefreshLayout)
        //TODO catch errors
        mSwipeRefreshLayout.setOnRefreshListener {
            posts!!.loadMore(adapter, id, true)
            //TODO catch errors
        }
        rv.addOnScrollListener(object : ToolbarScrollHideHandler(requireActivity().findViewById(R.id.toolbar), requireActivity().findViewById(R.id.header)) {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleItemCount = rv.layoutManager!!.childCount
                totalItemCount = rv.layoutManager!!.itemCount
                if (rv.layoutManager is PreCachingLayoutManager) {
                    pastVisiblesItems = (rv.layoutManager as PreCachingLayoutManager?)!!.findFirstVisibleItemPosition()
                } else {
                    var firstVisibleItems: IntArray? = null
                    firstVisibleItems = (rv.layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(firstVisibleItems)
                    if (firstVisibleItems != null && firstVisibleItems.isNotEmpty()) {
                        pastVisiblesItems = firstVisibleItems[0]
                    }
                }
                if (!posts!!.loading) {
                    if (visibleItemCount + pastVisiblesItems + 5 >= totalItemCount && !posts!!.nomore) {
                        posts!!.loading = true
                        posts!!.loadMore(adapter, id, false)
                    }
                }
            }
        })
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        id = bundle!!.getString("id", "")
        where = bundle.getString("where", "")
    }
}
