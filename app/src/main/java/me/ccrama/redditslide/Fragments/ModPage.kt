package me.ccrama.redditslide.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.ModQueue
import me.ccrama.redditslide.Adapters.ModeratorAdapter
import me.ccrama.redditslide.Adapters.ModeratorPosts
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.views.PreCachingLayoutManager

class ModPage : Fragment() {
    var adapter: ModeratorAdapter? = null
    private var posts: ModeratorPosts? = null
    private var id: String? = null
    private var sub: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_verticalcontent, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.vertical_content)
        val mLayoutManager = PreCachingLayoutManager(activity)
        rv.layoutManager = mLayoutManager
        v.findViewById<View>(R.id.post_floating_action_button).visibility = View.GONE
        val mSwipeRefreshLayout = v.findViewById<SwipeRefreshLayout>(R.id.activity_main_swipe_refresh_layout)
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors(id, activity))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp
        mSwipeRefreshLayout.setProgressViewOffset(false,
            Constants.TAB_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.TAB_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM)
        mSwipeRefreshLayout.post { mSwipeRefreshLayout.isRefreshing = true }
        posts = ModeratorPosts(id, sub)
        adapter = ModeratorAdapter(requireActivity(), posts!!, rv)
        rv.adapter = adapter
        rv.addOnScrollListener(ToolbarScrollHideHandler((requireActivity() as ModQueue).mToolbar, requireActivity().findViewById(R.id.header)))
        posts!!.bindAdapter(adapter, mSwipeRefreshLayout)
        mSwipeRefreshLayout.setOnRefreshListener { posts!!.loadMore(adapter, id, sub) }
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        id = bundle!!.getString("id", "")
        sub = bundle.getString("subreddit", "")
    }
}
