package me.ccrama.redditslide.Activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.R
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.repository.PostRepository
import me.ccrama.redditslide.Adapters.GalleryView
import me.ccrama.redditslide.Adapters.MultiredditPosts
import me.ccrama.redditslide.Adapters.SubmissionDisplay
import me.ccrama.redditslide.Adapters.SubredditPosts
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import javax.inject.Inject

@AndroidEntryPoint
class Gallery : FullScreenActivity(), SubmissionDisplay {
    @Inject
    lateinit var postRepository: PostRepository

    @JvmField
    var subredditPosts: PostLoader? = null
    var subreddit: String? = null
    var baseSubs: ArrayList<IPost>? = null

    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT)
        val multireddit = intent.extras!!.getString(EXTRA_MULTIREDDIT)
        val profile = intent.extras!!
            .getString(EXTRA_PROFILE, "")
        if (multireddit != null) {
            subredditPosts = MultiredditPosts(multireddit, profile)
        } else {
            subredditPosts = SubredditPosts(subreddit!!, this@Gallery, postRepository)
        }
        subreddit = if (multireddit == null) subreddit else "multi$multireddit"
        if (multireddit == null) {
            shareUrl = "https://reddit.com/r/$subreddit"
        }
        applyDarkColorTheme(subreddit)
        super.onCreate(savedInstance)
        setContentView(R.layout.gallery)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
        val offline = intent.getLongExtra("offline", 0L)
        val submissions: OfflineSubreddit =
            OfflineSubreddit.getSubreddit(subreddit, offline, !Authentication.didOnline, this)!!
        baseSubs = ArrayList<IPost>()
        for (s in submissions.submissions.orEmpty()) {
            if (s.thumbnails?.source != null) {
                baseSubs!!.add(s)
            } else if (s.contentType == ContentType.Type.IMAGE) {
                baseSubs!!.add(s)
            }
            subredditPosts!!.posts.add(s)
        }
        rv = findViewById<View>(R.id.content_view) as RecyclerView
        recyclerAdapter = GalleryView(this, baseSubs!!.map { it.submission!! }, subreddit!!)
        val layoutManager: RecyclerView.LayoutManager = createLayoutManager(
            LayoutUtils.getNumColumns(
                resources.configuration.orientation, this@Gallery
            )
        )
        rv!!.layoutManager = layoutManager
        rv!!.adapter = recyclerAdapter
        rv!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisibleItems: IntArray =
                    (rv!!.layoutManager as CatchStaggeredGridLayoutManager).findFirstVisibleItemPositions(
                        null
                    )
                if (firstVisibleItems.isNotEmpty()) {
                    for (firstVisibleItem in firstVisibleItems) {
                        pastVisiblesItems = firstVisibleItem
                    }
                }
                if (visibleItemCount + pastVisiblesItems + 5 >= totalItemCount) {
                    if (subredditPosts is SubredditPosts) {
                        if (!(subredditPosts as SubredditPosts?)!!.loading) {
                            (subredditPosts as SubredditPosts?)!!.loading = true
                            (subredditPosts as SubredditPosts)!!.loadMore(
                                this@Gallery, this@Gallery,
                                false, subreddit!!
                            )
                        }
                    } else if (subredditPosts is MultiredditPosts) {
                        if (!(subredditPosts as MultiredditPosts?)!!.loading) {
                            (subredditPosts as MultiredditPosts?)!!.loading = true
                            subredditPosts!!.loadMore(this@Gallery, this@Gallery, false)
                        }
                    }
                }
            }
        })
    }

    var recyclerAdapter: GalleryView? = null
    var pastVisiblesItems = 0
    var visibleItemCount = 0
    var totalItemCount = 0
    var rv: RecyclerView? = null
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val currentOrientation = newConfig.orientation
        val mLayoutManager: CatchStaggeredGridLayoutManager =
            rv!!.getLayoutManager() as CatchStaggeredGridLayoutManager
        mLayoutManager.setSpanCount(LayoutUtils.getNumColumns(currentOrientation, this@Gallery))
    }

    override fun updateSuccess(submissions: List<IPost>, startIndex: Int) {
        runOnUiThread {
            val startSize = baseSubs!!.size
            for (s in submissions) {
                if (!baseSubs!!.contains(s) && s.thumbnails?.source != null
                ) {
                    baseSubs!!.add(s)
                }
            }
            recyclerAdapter!!.notifyItemRangeInserted(startSize, baseSubs!!.size - startSize)
        }
    }

    override fun updateOffline(submissions: List<IPost>, cacheTime: Long) {
        runOnUiThread { recyclerAdapter!!.notifyDataSetChanged() }
    }

    override fun updateOfflineError() {}
    override fun updateError() {}
    override fun updateViews() {
        recyclerAdapter!!.notifyDataSetChanged()
    }

    override fun onAdapterUpdated() {
        recyclerAdapter!!.notifyDataSetChanged()
    }

    private fun createLayoutManager(numColumns: Int): RecyclerView.LayoutManager {
        return CatchStaggeredGridLayoutManager(
            numColumns,
            CatchStaggeredGridLayoutManager.VERTICAL
        )
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_PAGE = "page"
        const val EXTRA_SUBREDDIT = "subreddit"
        const val EXTRA_MULTIREDDIT = "multireddit"
    }
}
