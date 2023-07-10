package me.ccrama.redditslide.Adapters

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import kotlinx.coroutines.runBlocking
import ltd.ucode.lemmy.api.ApiException
import ltd.ucode.lemmy.api.PagedData
import ltd.ucode.lemmy.data.LemmyPost
import ltd.ucode.lemmy.data.type.PostListingType
import ltd.ucode.lemmy.data.type.PostSortType
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.getSubmissionSort
import ltd.ucode.slide.SettingValues.getSubmissionTimePeriod
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.ui.BaseActivity
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Fragments.SubmissionsView
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.PhotoLoader
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.http.NetworkException
import java.util.Collections

/**
 * This class is reponsible for loading subreddit specific submissions [] is implemented asynchronously.
 */
class SubredditPosts @JvmOverloads constructor(
    subreddit: String,
    c: Context,
    private val postRepository: PostRepository,
    force18: Boolean = false
) : PostLoader {
    override var posts: MutableList<IPost> = mutableListOf()

    @JvmField
    var subreddit: String
    var subredditRandom: String? = null
    @JvmField
    var nomore = false
    @JvmField
    var offline = false
    @JvmField
    var forced = false
    @JvmField
    var loading = false
    @JvmField
    var error = false
    private var paginator: PagedData<PostView>? = null
    @JvmField
    var cached: OfflineSubreddit? = null
    var c: Context
    var force18: Boolean

    override fun loadMore(
        context: Context, display: SubmissionDisplay,
        reset: Boolean
    ) {
        LoadData(context, display, reset).execute(subreddit)
    }

    fun loadMore(
        context: Context, display: SubmissionDisplay?, reset: Boolean,
        subreddit: String
    ) {
        this.subreddit = subreddit
        loadMore(context, display!!, reset)
    }

    var all: ArrayList<String?>? = null

    override fun hasMore(): Boolean {
        return !nomore
    }

    var authedOnce = false
    var usedOffline = false
    var currentid: Long = 0
    var displayer: SubmissionDisplay? = null

    init {
        posts = ArrayList()
        this.subreddit = subreddit
        this.c = c
        this.force18 = force18
    }

    /**
     * Asynchronous task for loading data
     */
    private inner class LoadData(
        var context: Context,
        display: SubmissionDisplay?,
        reset: Boolean
    ) : AsyncTask<String?, Void?, List<IPost?>?>() {
        val reset: Boolean
        var start = 0
        public override fun onPreExecute() {
            if (reset) {
                posts.clear()
                displayer!!.onAdapterUpdated()
            }
        }

        public override fun onPostExecute(submissions: List<IPost?>?) {
            var success = true
            loading = false
            if (error != null) {
                if (error is NetworkException) {
                    val e = error as NetworkException
                    if (e.response.statusCode == 403 && !authedOnce) {
                        if (App.authentication != null && Authentication.didOnline) {
                            App.authentication!!.updateToken(context)
                        } else if (NetworkUtil.isConnected(context)
                            && App.authentication == null
                        ) {
                            App.authentication = Authentication(context)
                        }
                        authedOnce = true
                        loadMore(context, displayer, reset, subreddit)
                        return
                    } else {
                        Toast.makeText(
                            context,
                            "A server error occurred, " + e.response.statusCode + if (e.response.statusMessage.isEmpty()) "" else ": " + e.response.statusMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                success = false
            } else if (!submissions.isNullOrEmpty()) {
                if (displayer is SubmissionsView
                    && (displayer as SubmissionsView).adapter!!.isError
                ) {
                    (displayer as SubmissionsView).adapter!!.undoSetError()
                }
                val ids = arrayOfNulls<String>(submissions.size)
                for ((i, s) in submissions.withIndex()) {
                    ids[i] = s!!.rowId.toString()
                }

                // update online
                displayer!!.updateSuccess(posts, start)
                currentid = 0
                OfflineSubreddit.currentid = currentid
                (c as? BaseActivity)?.let { it.shareUrl = "https://reddit.com/c/$subreddit" }
                if (subreddit == "random" || subreddit == "myrandom" || subreddit == "randnsfw") {
                    subredditRandom = submissions[0]!!.groupName
                }
                MainActivity.randomoverride = subredditRandom
                if (context is SubredditView && (subreddit == "random" || subreddit == "myrandom" || subreddit == "randnsfw")) {
                    (context as SubredditView).subreddit = subredditRandom
                    (context as SubredditView).executeAsyncSubreddit(subredditRandom)
                }
            } else if (submissions != null) {
                // end of submissions
                nomore = true
                displayer!!.updateSuccess(posts, posts.size + 1)
            } else if (MainActivity.isRestart) {
                posts = ArrayList()
                cached = OfflineSubreddit.getSubreddit(subreddit, 0L, true, c)
                for (s in cached!!.submissions!!) {
                    if (!PostMatch.doesMatch(s, subreddit, force18)) {
                        posts.add(s)
                    }
                }
                offline = false
                usedOffline = false
                displayer!!.updateSuccess(posts, start)
            } else {
                if (!all!!.isEmpty() && !nomore && SettingValues.cache) {
                    if (context is MainActivity) {
                        doMainActivityOffline(context, displayer)
                    }
                } else if (!nomore) {
                    // error
                    LogUtil.v("Setting error")
                    success = false
                }
            }
            this@SubredditPosts.error = !success
        }

        override fun doInBackground(vararg subredditPaginators: String?): List<IPost?>? {
            if (BuildConfig.DEBUG) LogUtil.v("Loading data")
            if (!NetworkUtil.isConnected(context) && !Authentication.didOnline
                || MainActivity.isRestart
            ) {
                Log.v(LogUtil.getTag(), "Using offline data")
                offline = true
                usedOffline = true
                all = OfflineSubreddit.getAll(subreddit)
                return null
            } else {
                offline = false
                usedOffline = false
            }
            if (reset || paginator == null) {
                offline = false
                nomore = false
                var sub = subredditPaginators[0]!!.lowercase()
                if (sub == "random" || sub == "randnsfw" && MainActivity.randomoverride != null && !MainActivity.randomoverride!!.isEmpty()) {
                    sub = MainActivity.randomoverride!!
                    MainActivity.randomoverride = ""
                }
                paginator = if (sub == "subscribed") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        type = PostListingType.Subscribed,
                        sort = PostSortType.from(getSubmissionSort(subreddit), getSubmissionTimePeriod(subreddit)),
                        limit = Constants.PAGINATOR_POST_LIMIT
                    )
                } else if (sub == "local") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        type = PostListingType.Local,
                        sort = PostSortType.from(getSubmissionSort(subreddit), getSubmissionTimePeriod(subreddit)),
                        limit = Constants.PAGINATOR_POST_LIMIT
                    )
                } else if (sub == "all") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        type = PostListingType.All,
                        sort = PostSortType.from(getSubmissionSort(subreddit), getSubmissionTimePeriod(subreddit)),
                        limit = Constants.PAGINATOR_POST_LIMIT
                    )
                } else if (!sub.contains(".")) {
                    //SubredditPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        communityName = sub,
                        sort = PostSortType.from(getSubmissionSort(subreddit), getSubmissionTimePeriod(subreddit)),
                        limit = Constants.PAGINATOR_POST_LIMIT
                    )
                } else {
                    //DomainPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        communityName = sub,
                        sort = PostSortType.from(getSubmissionSort(subreddit), getSubmissionTimePeriod(subreddit)),
                        limit = Constants.PAGINATOR_POST_LIMIT
                    )
                }
            }
            val filteredSubmissions: MutableList<IPost> = nextFiltered
            if (!(SettingValues.noImages && ((!NetworkUtil.isConnectedWifi(c)
                        && SettingValues.lowResMobile) || SettingValues.lowResAlways))
            ) {
                PhotoLoader.loadPhotos(c, filteredSubmissions)
            }
            if (SettingValues.storeHistory) {
                //HasSeen.setHasSeenSubmission(filteredSubmissions.map { it.submission })
                //LastComments.setCommentsSince(filteredSubmissions.map { it.submission })
            }
            //SubmissionCache.cacheSubmissions(filteredSubmissions.map { it.submission }, context, subreddit)
            if (reset || offline || posts == null) {
                posts = filteredSubmissions.distinct().toMutableList()
                start = -1
            } else {
                posts.addAll(filteredSubmissions)
                posts = posts.distinct().toMutableList()
                offline = false
            }
            if (!usedOffline) {
                /*
                OfflineSubreddit.getSubNoLoad(subreddit.lowercase())
                    .overwriteSubmissions(posts.map { it.submission })
                    .writeToMemory(context)
                 */
            }
            start = 0
            if (posts != null) {
                start = posts.size + 1
            }
            return filteredSubmissions
        }

        val nextFiltered: ArrayList<IPost>
            get() {
                val filteredSubmissions = ArrayList<IPost>()
                val adding = ArrayList<LemmyPost>()
                try {
                    if (paginator != null && paginator!!.hasNext) {
                        //if (force18 && paginator is SubredditPaginator) {
                        //    (paginator as SubredditPaginator).setObeyOver18(false)
                        //}
                        val page = runBlocking { paginator!!.next() }
                            .mapSuccess { data.map { LemmyPost(instance, it) } }
                        adding.addAll(page.success)
                    } else {
                        nomore = true
                    }
                    for (s in adding) {
                        if (!PostMatch.doesMatch(
                                s,
                                "",//if (paginator is SubredditPaginator) (paginator as SubredditPaginator).subreddit else (paginator as DomainPaginator?)!!.domain,
                                force18
                            )
                        ) {
                            filteredSubmissions.add(s)
                        }
                    }
                    if (paginator != null && paginator!!.hasNext && filteredSubmissions.isEmpty()) {
                        filteredSubmissions.addAll(nextFiltered)
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                    error = e
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    error = e
                    if (e.message != null && e.message!!.contains("Forbidden")) {
                        App.authentication!!.updateToken(context)
                    }
                }
                return filteredSubmissions
            }
        var error: Exception? = null

        init {
            displayer = display
            this.reset = reset
        }
    }

    fun doMainActivityOffline(c: Context, displayer: SubmissionDisplay?) {
        LogUtil.v(subreddit)
        if (all == null) {
            all = OfflineSubreddit.getAll(subreddit)
        }
        Collections.rotate(all, -1) //Move 0, or "submission only", to the end
        offline = true
        val titles = arrayOfNulls<String>(all!!.size)
        val base = arrayOfNulls<String>(all!!.size)
        for ((i, s) in all!!.withIndex()) {
            val split = s!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            titles[i] = if (split[1].toLong() == 0L) c.getString(
                R.string.settings_backup_submission_only
            ) else TimeUtils.getTimeAgo(
                split[1].toLong(), c
            ) + c.getString(
                R.string.settings_backup_comments
            )
            base[i] = s
        }
        (c as MainActivity).supportActionBar!!.navigationMode = ActionBar.NAVIGATION_MODE_LIST
        c.supportActionBar!!
            .setListNavigationCallbacks(
                OfflineSubAdapter(c, android.R.layout.simple_list_item_1, titles),
                ActionBar.OnNavigationListener { itemPosition, itemId ->
                    val s2 =
                        base[itemPosition]!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    OfflineSubreddit.currentid = java.lang.Long.valueOf(s2[1])
                    currentid = OfflineSubreddit.currentid
                    object : AsyncTask<Void?, Void?, Void?>() {
                        var cached: OfflineSubreddit? = null
                        override fun doInBackground(vararg params: Void?): Void? {
                            cached = OfflineSubreddit.getSubreddit(
                                subreddit,
                                java.lang.Long.valueOf(s2[1]), true, c
                            )
                            val finalSubs: MutableList<IPost> = ArrayList()
                            for (s in cached!!.submissions!!) {
                                if (!PostMatch.doesMatch(s, subreddit, force18)) {
                                    finalSubs.add(s)
                                }
                            }
                            posts = finalSubs
                            return null
                        }

                        override fun onPostExecute(aVoid: Void?) {
                            if (cached!!.submissions.isNullOrEmpty()) {
                                displayer!!.updateOfflineError()
                            }
                            // update offline
                            displayer!!.updateOffline(posts, s2[1].toLong())
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    true
                })
    }
}
