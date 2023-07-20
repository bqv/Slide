package me.ccrama.redditslide.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.R
import me.ccrama.redditslide.Adapters.InboxAdapter
import me.ccrama.redditslide.Adapters.InboxMessages
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.PreCachingLayoutManager

class InboxPage : Fragment() {
    private var totalItemCount = 0
    private var visibleItemCount = 0
    private var pastVisiblesItems = 0
    private var adapter: InboxAdapter? = null
    private var posts: InboxMessages? = null
    private var id: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_verticalcontent, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.vertical_content)
        val mLayoutManager = PreCachingLayoutManager(activity)
        rv.layoutManager = mLayoutManager
        val mSwipeRefreshLayout = v.findViewById<SwipeRefreshLayout>(R.id.activity_main_swipe_refresh_layout)
        v.findViewById<View>(R.id.post_floating_action_button).visibility = View.GONE
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors(id, activity))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp
        mSwipeRefreshLayout.setProgressViewOffset(false,
            Constants.TAB_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.TAB_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM)
        mSwipeRefreshLayout.post { mSwipeRefreshLayout.isRefreshing = true }
        posts = InboxMessages(id!!)
        adapter = InboxAdapter(requireContext(), posts!!, rv)
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
                    if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                        pastVisiblesItems = firstVisibleItems[0]
                    }
                }
                if (!posts!!.loading && !posts!!.nomore) {
                    if (visibleItemCount + pastVisiblesItems + 5 >= totalItemCount) {
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
    }
}
