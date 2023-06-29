package me.ccrama.redditslide.Adapters

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Activities.NewsActivity
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Fragments.SubmissionsView
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.LastComments
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SubmissionCache
import me.ccrama.redditslide.Synccit.MySynccitReadTask
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.PhotoLoader
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.http.NetworkException
import net.dean.jraw.models.Submission
import net.dean.jraw.paginators.DomainPaginator
import net.dean.jraw.paginators.Paginator
import net.dean.jraw.paginators.SubredditPaginator
import java.net.UnknownHostException
import java.util.Collections

/**
 * This class is reponsible for loading subreddit specific submissions [] is implemented asynchronously.
 */
class SubredditPostsRealm : PostLoader {
    override var posts: MutableList<IPost>
    @JvmField
    var subreddit: String
    var subredditRandom: String? = null
    @JvmField
    var nomore = false
    @JvmField
    var offline = false
    var forced = false
    var loading = false
    private var paginator: Paginator<Submission>? = null
    var cached: OfflineSubreddit? = null
    var c: Context
    var force18 = false

    constructor(subreddit: String, c: Context) {
        posts = ArrayList()
        this.subreddit = subreddit
        this.c = c
    }

    constructor(subreddit: String, c: Context, force18: Boolean) {
        posts = ArrayList()
        this.subreddit = subreddit
        this.c = c
        this.force18 = force18
    }

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
    @JvmField
    var displayer: SubmissionDisplay? = null

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
        public override fun onPostExecute(submissions: List<IPost?>?) {
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
                if (error!!.cause is UnknownHostException) {
                    Toast.makeText(
                        context, "Loading failed, please check your internet connection",
                        Toast.LENGTH_LONG
                    ).show()
                }
                displayer!!.updateError()
            } else if (submissions != null && !submissions.isEmpty()) {
                if (displayer is SubmissionsView
                    && (displayer as SubmissionsView).adapter!!.isError
                ) {
                    (displayer as SubmissionsView).adapter!!.undoSetError()
                }
                val ids = arrayOfNulls<String>(submissions.size)
                var i = 0
                for (s in submissions) {
                    ids[i] = s!!.id
                    i++
                }

                // update online
                displayer!!.updateSuccess(posts, start)
                currentid = 0
                OfflineSubreddit.currentid = currentid
                if (c is BaseActivity) {
                    (c as BaseActivity).shareUrl = "https://reddit.com/c/$subreddit"
                }
                if (subreddit == "random" || subreddit == "myrandom" || subreddit == "randnsfw") {
                    subredditRandom = submissions[0]!!.groupName
                }
                if (context is SubredditView && (subreddit == "random" || subreddit == "myrandom" || subreddit == "randnsfw")) {
                    (context as SubredditView).subreddit = subredditRandom
                    (context as SubredditView).executeAsyncSubreddit(subredditRandom)
                }
                if (!SettingValues.synccitName!!.isEmpty() && !offline) {
                    MySynccitReadTask(displayer).execute(*ids)
                }
            } else if (submissions != null) {
                // end of submissions
                nomore = true
                displayer!!.updateSuccess(posts, posts!!.size + 1)
            } else {
                if (!all!!.isEmpty() && !nomore && SettingValues.cache) {
                    if (context is NewsActivity) {
                        doNewsActivityOffline(context, displayer)
                    }
                } else if (!nomore) {
                    // error
                    LogUtil.v("Setting error")
                    displayer!!.updateError()
                }
            }
        }

        override fun doInBackground(vararg subredditPaginators: String?): List<IPost?>? {
            if (BuildConfig.DEBUG) LogUtil.v("Loading data")
            if (!NetworkUtil.isConnected(context) && !Authentication.didOnline) {
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
                val sub = subredditPaginators[0]!!.lowercase()
                LogUtil.v(sub)
                paginator = if (sub == "frontpage") {
                    SubredditPaginator(Authentication.reddit)
                } else if (!sub.contains(".")) {
                    SubredditPaginator(Authentication.reddit, sub)
                } else {
                    DomainPaginator(Authentication.reddit, sub)
                }
                //paginator!!.sorting = getSubmissionSort(subreddit)
                //paginator!!.timePeriod = getSubmissionTimePeriod(subreddit)
                paginator!!.setLimit(Constants.PAGINATOR_POST_LIMIT)
            }
            val filteredSubmissions: List<IPost?> = nextFiltered
            if (!(SettingValues.noImages && ((!NetworkUtil.isConnectedWifi(c)
                        && SettingValues.lowResMobile) || SettingValues.lowResAlways))
            ) {
                PhotoLoader.loadPhotos(c, filteredSubmissions.filterNotNull())
            }
            if (SettingValues.storeHistory) {
                HasSeen.setHasSeenSubmission(filteredSubmissions.mapNotNull { it?.submission })
                LastComments.setCommentsSince(filteredSubmissions.filterNotNull())
            }
            SubmissionCache.cacheSubmissions(filteredSubmissions.filterNotNull(), context, subreddit)
            if (reset || offline || posts == null) {
                posts = filteredSubmissions.filterNotNull().toHashSet().toMutableList()
                start = -1
            } else {
                posts!!.addAll(filteredSubmissions.filterNotNull())
                posts = posts.toHashSet().toMutableList()
                offline = false
            }
            if (!usedOffline) {
                OfflineSubreddit.getSubNoLoad(subreddit.lowercase())
                    .overwriteSubmissions(posts)
                    .writeToMemory(context)
            }
            start = 0
            if (posts != null) {
                start = posts.size + 1
            }
            return filteredSubmissions
        }

        val nextFiltered: ArrayList<IPost?>
            get() {
                val filteredSubmissions = ArrayList<IPost?>()
                val adding = ArrayList<IPost>()
                try {
                    if (paginator != null && paginator!!.hasNext()) {
                        if (force18 && paginator is SubredditPaginator) {
                            (paginator as SubredditPaginator).setObeyOver18(false)
                        }
                        adding.addAll(paginator!!.next().map(::RedditSubmission))
                    } else {
                        nomore = true
                    }
                    for (s in adding) {
                        if (!PostMatch.doesMatch(
                                s,
                                if (paginator is SubredditPaginator) (paginator as SubredditPaginator).subreddit else (paginator as DomainPaginator?)!!.domain,
                                force18
                            )
                        ) {
                            filteredSubmissions.add(s)
                        }
                    }
                    if (paginator != null && paginator!!.hasNext() && filteredSubmissions.isEmpty()) {
                        filteredSubmissions.addAll(nextFiltered)
                    }
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

    fun doNewsActivityOffline(c: Context, displayer: SubmissionDisplay?) {
        LogUtil.v(subreddit)
        if (all == null) {
            all = OfflineSubreddit.getAll(subreddit)
        }
        Collections.rotate(all, -1) //Move 0, or "submission only", to the end
        offline = true
        val titles = arrayOfNulls<String>(all!!.size)
        val base = arrayOfNulls<String>(all!!.size)
        var i = 0
        for (s in all!!) {
            val split = s!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            titles[i] = if (split[1].toLong() == 0L) c.getString(
                R.string.settings_backup_submission_only
            ) else TimeUtils.getTimeAgo(
                split[1].toLong(), c
            ) + c.getString(
                R.string.settings_backup_comments
            )
            base[i] = s
            i++
        }
        (c as NewsActivity).supportActionBar!!.navigationMode = ActionBar.NAVIGATION_MODE_LIST
        c.supportActionBar!!
            .setListNavigationCallbacks(
                OfflineSubAdapter(c, android.R.layout.simple_list_item_1, titles)
            ) { itemPosition, itemId ->
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
            }
    }
}
