package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Adapters.ContributionAdapter
import me.ccrama.redditslide.Adapters.SubredditSearchPosts
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.PreCachingLayoutManager
import me.zhanghai.android.materialprogressbar.R

class Related : BaseActivityAnim() {
    private var totalItemCount = 0
    private var visibleItemCount = 0
    private var pastVisiblesItems = 0
    private var adapter: ContributionAdapter? = null
    private var posts: SubredditSearchPosts? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    var url: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        applyColorTheme("")
        setContentView(ltd.ucode.slide.R.layout.activity_search)
        val intent = intent
        if (intent.hasExtra(Intent.EXTRA_TEXT) && !intent.extras!!.getString(Intent.EXTRA_TEXT, "").isEmpty()) {
            url = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        if (intent.hasExtra(EXTRA_URL)) {
            url = intent.getStringExtra(EXTRA_URL)
        }
        if (url == null || url!!.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("URL is empty")
                .setMessage("Try again with a different link!")
                .setCancelable(false)
                .setPositiveButton(ltd.ucode.slide.R.string.btn_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }
                .show()
        }
        setupAppBar(ltd.ucode.slide.R.id.toolbar, "Related links", true, true)
        assert(mToolbar != null //it won't be, trust me
        )
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        val rv = findViewById<View>(ltd.ucode.slide.R.id.vertical_content) as RecyclerView
        val mLayoutManager = PreCachingLayoutManager(this)
        rv.layoutManager = mLayoutManager
        rv.addOnScrollListener(object : ToolbarScrollHideHandler(mToolbar, findViewById(ltd.ucode.slide.R.id.header)) {
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
                if (!posts!!.loading && visibleItemCount + pastVisiblesItems + 5 >= totalItemCount) {
                    posts!!.loading = true
                    posts!!.loadMore(adapter, "", "url:$url", false)
                }
            }
        })
        val mSwipeRefreshLayout = findViewById<View>(ltd.ucode.slide.R.id.activity_main_swipe_refresh_layout) as SwipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors("", this))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp.
        mSwipeRefreshLayout.setProgressViewOffset(false,
            Constants.SINGLE_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.SINGLE_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM)
        mSwipeRefreshLayout.post { mSwipeRefreshLayout.isRefreshing = true }
        posts = SubredditSearchPosts("", "url:$url", this, false)
        adapter = ContributionAdapter(this, posts!!, rv)
        rv.adapter = adapter
        posts!!.bindAdapter(adapter, mSwipeRefreshLayout)
        //TODO catch errors
        mSwipeRefreshLayout.setOnRefreshListener { posts!!.loadMore(adapter, "", "url:$url", true) }
    }

    companion object {
        //todo NFC support
        const val EXTRA_URL = "url"
    }
}
