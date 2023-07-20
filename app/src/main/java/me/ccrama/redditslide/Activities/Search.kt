package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Adapters.ContributionAdapter
import me.ccrama.redditslide.Adapters.SubredditSearchPosts
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.PreCachingLayoutManager
import net.dean.jraw.paginators.SubmissionSearchPaginator
import net.dean.jraw.paginators.TimePeriod
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

class Search : BaseActivityAnim() {
    private var totalItemCount: Int = 0
    private var visibleItemCount: Int = 0
    private var pastVisiblesItems: Int = 0
    private var adapter: ContributionAdapter? = null
    private var where: String? = null
    private var subreddit: String? = null

    //    private String site;
    //    private String url;
    //    private boolean self;
    //    private boolean nsfw;
    //    private String author;
    private var posts: SubredditSearchPosts? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        //   if (mShowInfoButton) menu.findItem(R.id.action_info).setVisible(true);
        //   else menu.findItem(R.id.action_info).setVisible(false);
        return true
    }

    fun reloadSubs() {
        posts!!.refreshLayout!!.isRefreshing = true
        posts!!.reset(time!!)
    }

    fun openTimeFramePopup() {
        val l2: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { dialogInterface, i ->
                when (i) {
                    0 -> time = TimePeriod.HOUR
                    1 -> time = TimePeriod.DAY
                    2 -> time = TimePeriod.WEEK
                    3 -> time = TimePeriod.MONTH
                    4 -> time = TimePeriod.YEAR
                    5 -> time = TimePeriod.ALL
                }
                reloadSubs()

                //When the .name() is returned for both of the ENUMs, it will be in all caps.
                //So, make it lowercase, then capitalize the first letter of each.
                supportActionBar!!.setSubtitle(
                    StringUtils.capitalize(
                        SortingUtil.search.name.lowercase()
                    ) + " › " + StringUtils
                        .capitalize(time!!.name.lowercase())
                )
            }
        AlertDialog.Builder(this@Search)
            .setTitle(R.string.sorting_time_choose)
            .setSingleChoiceItems(
                SortingUtil.getSortingTimesStrings(),
                SortingUtil.getSortingSearchId(this),
                l2
            )
            .show()
    }

    fun openSearchTypePopup() {
        val l2: DialogInterface.OnClickListener =
            DialogInterface.OnClickListener { dialogInterface, i ->
                when (i) {
                    0 -> SortingUtil.search = SubmissionSearchPaginator.SearchSort.RELEVANCE
                    1 -> SortingUtil.search = SubmissionSearchPaginator.SearchSort.TOP
                    2 -> SortingUtil.search = SubmissionSearchPaginator.SearchSort.NEW
                    3 -> SortingUtil.search = SubmissionSearchPaginator.SearchSort.COMMENTS
                }
                reloadSubs()

                //When the .name() is returned for both of the ENUMs, it will be in all caps.
                //So, make it lowercase, then capitalize the first letter of each.
                supportActionBar!!.setSubtitle(
                    StringUtils.capitalize(
                        SortingUtil.search.name.lowercase()
                    ) + " › " + StringUtils
                        .capitalize(time!!.name.lowercase())
                )
            }
        AlertDialog.Builder(this@Search)
            .setTitle(R.string.sorting_choose)
            .setSingleChoiceItems(
                SortingUtil.getSearch(),
                SortingUtil.getSearchType(),
                l2
            )
            .show()
    }

    @JvmField
    var time: TimePeriod? = null
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            me.zhanghai.android.materialprogressbar.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.time -> {
                openTimeFramePopup()
                return true
            }

            R.id.edit -> {
                val builder = MaterialDialog(this)
                    .title(R.string.search_title)
                    .input(hintRes = R.string.search_msg, prefill = where,
                        waitForPositiveButton = false) { materialDialog: MaterialDialog?, charSequence: CharSequence ->
                        where = charSequence.toString()
                    }

                //Add "search current sub" if it is not frontpage/all/random
                builder.positiveButton(text = "Search") { materialDialog: MaterialDialog ->
                    val i: Intent = Intent(this@Search, Search::class.java)
                    i.putExtra(EXTRA_TERM, where)
                    if (multireddit) {
                        i.putExtra(EXTRA_MULTIREDDIT, subreddit)
                    } else {
                        i.putExtra(EXTRA_SUBREDDIT, subreddit)
                    }
                    startActivity(i)
                    overridePendingTransition(0, 0)
                    finish()
                    overridePendingTransition(0, 0)
                }
                builder.show()
                return true
            }

            R.id.sort -> {
                openSearchTypePopup()
                return true
            }
        }
        return false
    }

    var multireddit: Boolean = false
    var rv: RecyclerView? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        applyColorTheme("")
        setContentView(R.layout.activity_search)
        where = intent.extras!!.getString(EXTRA_TERM, "")
        time = TimePeriod.ALL
        if (intent.hasExtra(EXTRA_MULTIREDDIT)) {
            multireddit = true
            subreddit = intent.extras!!.getString(EXTRA_MULTIREDDIT)
        } else {
            if (intent.hasExtra(EXTRA_AUTHOR)) {
                where = where + "&author=" + intent.extras!!.getString(EXTRA_AUTHOR)
            }
            if (intent.hasExtra(EXTRA_NSFW)) {
                where = where + "&nsfw=" + (if (intent.extras!!.getBoolean(EXTRA_NSFW)) "yes" else "no")
            }
            if (intent.hasExtra(EXTRA_SELF)) {
                where = where + "&selftext=" + (if (intent.extras!!.getBoolean(EXTRA_SELF)) "yes" else "no")
            }
            if (intent.hasExtra(EXTRA_SITE)) {
                where = where + "&site=" + intent.extras!!.getString(EXTRA_SITE)
            }
            if (intent.hasExtra(EXTRA_URL)) {
                where = where + "&url=" + intent.extras!!.getString(EXTRA_URL)
            }
            if (intent.hasExtra(EXTRA_TIME)) {
                val timePeriod: TimePeriod? = TimeUtils.stringToTimePeriod(
                    intent.extras!!.getString(EXTRA_TIME)
                )
                if (timePeriod != null) {
                    time = timePeriod
                }
            }
            subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT, "")
        }
        where = StringEscapeUtils.unescapeHtml4(where)
        setupSubredditAppBar(R.id.toolbar, "Search", true, subreddit!!.lowercase())
        supportActionBar!!.setTitle(CompatUtil.fromHtml(where!!))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        assert(
            mToolbar != null //it won't be, trust me
        )
        mToolbar!!.setNavigationOnClickListener {
            onBackPressed() //Simulate a system's "Back" button functionality.
        }
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId

        //When the .name() is returned for both of the ENUMs, it will be in all caps.
        //So, make it lowercase, then capitalize the first letter of each.
        supportActionBar!!.setSubtitle(
            (StringUtils.capitalize(SortingUtil.search.name.lowercase())
                    + " › "
                    + StringUtils.capitalize(time!!.name.lowercase()))
        )
        rv = (findViewById<View>(R.id.vertical_content) as RecyclerView?)
        val mLayoutManager: RecyclerView.LayoutManager = createLayoutManager(
            LayoutUtils.getNumColumns(
                resources.configuration.orientation,
                this@Search
            )
        )
        rv!!.layoutManager = mLayoutManager
        rv!!.addOnScrollListener(object :
            ToolbarScrollHideHandler(mToolbar, findViewById<View>(R.id.header)) {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleItemCount = rv!!.layoutManager!!.childCount
                totalItemCount = rv!!.layoutManager!!.itemCount
                if (rv!!.layoutManager is PreCachingLayoutManager) {
                    pastVisiblesItems =
                        (rv!!.layoutManager as PreCachingLayoutManager?)!!.findFirstVisibleItemPosition()
                } else {
                    var firstVisibleItems: IntArray? = null
                    firstVisibleItems =
                        (rv!!.layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(
                            firstVisibleItems
                        )
                    if (firstVisibleItems != null && firstVisibleItems.isNotEmpty()) {
                        pastVisiblesItems = firstVisibleItems.get(0)
                    }
                }
                if (!posts!!.loading && ((visibleItemCount + pastVisiblesItems) + 5 >= totalItemCount) && !posts!!.nomore) {
                    posts!!.loading = true
                    posts!!.loadMore(adapter, subreddit!!, where!!, false, multireddit, time!!)
                }
            }
        })
        val mSwipeRefreshLayout: SwipeRefreshLayout =
            findViewById<View>(R.id.activity_main_swipe_refresh_layout) as SwipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors(subreddit, this))

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp.
        mSwipeRefreshLayout.setProgressViewOffset(
            false,
            Constants.SINGLE_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.SINGLE_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM
        )
        mSwipeRefreshLayout.post(object : Runnable {
            override fun run() {
                mSwipeRefreshLayout.isRefreshing = true
            }
        })
        posts = SubredditSearchPosts(subreddit, where!!.lowercase(), this, multireddit)
        adapter = ContributionAdapter(this, posts!!, rv!!)
        rv!!.adapter = adapter
        posts!!.bindAdapter(adapter, mSwipeRefreshLayout)
        //TODO catch errors
        mSwipeRefreshLayout.setOnRefreshListener {
            posts!!.loadMore(adapter, subreddit!!, where!!, true, multireddit, time!!)
            //TODO catch errors
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val currentOrientation: Int = newConfig.orientation
        val mLayoutManager: CatchStaggeredGridLayoutManager? =
            rv!!.layoutManager as CatchStaggeredGridLayoutManager?
        mLayoutManager!!.setSpanCount(LayoutUtils.getNumColumns(currentOrientation, this@Search))
    }

    private fun createLayoutManager(numColumns: Int): RecyclerView.LayoutManager {
        return CatchStaggeredGridLayoutManager(
            numColumns,
            CatchStaggeredGridLayoutManager.VERTICAL
        )
    }

    companion object {
        //todo NFC support
        @JvmField
        val EXTRA_TERM: String = "term"
        @JvmField
        val EXTRA_SUBREDDIT: String = "subreddit"
        val EXTRA_MULTIREDDIT: String = "multi"
        @JvmField
        val EXTRA_SITE: String = "site"
        @JvmField
        val EXTRA_URL: String = "url"
        @JvmField
        val EXTRA_SELF: String = "self"
        @JvmField
        val EXTRA_NSFW: String = "nsfw"
        @JvmField
        val EXTRA_AUTHOR: String = "author"
        @JvmField
        val EXTRA_TIME: String = "t"
    }
}
