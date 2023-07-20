package me.ccrama.redditslide.Adapters

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ltd.ucode.network.data.IPost
import ltd.ucode.network.lemmy.api.ApiException
import ltd.ucode.network.lemmy.api.ApiResult
import ltd.ucode.network.lemmy.api.PagedData
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.getSubmissionSort
import ltd.ucode.slide.SettingValues.getSubmissionTimePeriod
import ltd.ucode.slide.data.entity.Post
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.shim.PeriodExtensions.from
import ltd.ucode.slide.shim.SortingExtensions.from
import ltd.ucode.slide.ui.BaseActivity
import ltd.ucode.slide.ui.main.MainActivity
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment
import ltd.ucode.slide.util.ContextExtensions.lifecycleScope
import me.ccrama.redditslide.Activities.SubredditView
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
class SubredditPosts constructor(
    var subreddit: String,
    var c: Context,
    private val postRepository: PostRepository,
    var force18: Boolean = false
) : PostLoader {
    override var posts: MutableList<IPost> = mutableListOf()
    lateinit var flow: Flow<PagingData<Post>>

    var subredditRandom: String? = null
    var nomore = false
    var offline = false
    var forced = false
    var loading = false
    var error = false
    private var paginator: PagedData<Post>? = null
    var cached: OfflineSubreddit? = null

    var all: ArrayList<String?>? = null
    var authedOnce = false
    var usedOffline = false
    var currentid: Long = 0
    var displayer: SubmissionDisplay? = null

    override fun loadMore(
        context: Context, display: SubmissionDisplay,
        reset: Boolean
    ) {
        context.lifecycleScope.launch {
            LoadData(context, display, reset)(subreddit)
        }
    }

    fun loadMore(
        context: Context, display: SubmissionDisplay?, reset: Boolean,
        subreddit: String
    ) {
        this.subreddit = subreddit
        loadMore(context, display!!, reset)
    }

    override fun hasMore(): Boolean {
        return !nomore
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

        suspend operator fun invoke(sub: String?) {
            onPreExecute()
            val result = withContext(Dispatchers.IO) {
                // runs in background thread without blocking the Main Thread
                doInBackground(sub)
            }
            onPostExecute(result)
        }

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
                if (displayer is SubmissionsViewFragment
                    && (displayer as SubmissionsViewFragment).adapter!!.hasError
                ) {
                    (displayer as SubmissionsViewFragment).adapter!!.undoSetError()
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
                if (sub == "random" || sub == "randnsfw" && MainActivity.randomoverride != null && MainActivity.randomoverride!!.isNotEmpty()) {
                    sub = MainActivity.randomoverride!!
                    MainActivity.randomoverride = ""
                }

                val postFlow = if (sub == "subscribed") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Subscribed,
                        pageSize = 50,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (sub == "local") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Local,
                        pageSize = 50,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (sub == "all") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.All,
                        pageSize = 50,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (!sub.contains(".")) {
                    //SubredditPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Group(sub),
                        pageSize = 50,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else {
                    //DomainPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Group(sub),
                        pageSize = 50,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                }

                flow = if (sub == "subscribed") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Subscribed,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (sub == "local") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Local,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (sub == "all") {
                    //SubredditPaginator(Authentication.reddit)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.All,
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else if (!sub.contains(".")) {
                    //SubredditPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Group(sub),
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                } else {
                    //DomainPaginator(Authentication.reddit, sub)
                    postRepository.getPosts(
                        AccountRepository.currentAccount,
                        feed = Feed.Group(sub),
                        period = Period.from(getSubmissionTimePeriod(subreddit)),
                        sort = Sorting.from(getSubmissionSort(subreddit)),
                    )
                }

                paginator = postFlow.let {
                    runBlocking {
                        it.singleOrNull().orEmpty()
                    }
                }.let { posts ->
                    PagedData {
                        { ApiResult.Success(
                            instance = AccountRepository.currentAccount ?: posts.first().domain!!,
                            data = posts) }
                    }
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
                val adding = ArrayList<Post>()
                try {
                    if (paginator != null && paginator!!.hasNext) {
                        //if (force18 && paginator is SubredditPaginator) {
                        //    (paginator as SubredditPaginator).setObeyOver18(false)
                        //}
                        val page = runBlocking { paginator!!.next() }
                            .mapSuccess { data.map { it } }
                        adding.addAll(page.success)
                    } else {
                        nomore = true
                    }
                    for (s in adding) {
                        if (!PostMatch.doesMatch(
                                s,
                                "",//if (paginator is SubredditPaginator) (paginator as SubredditPaginator).subreddit else (paginator!! as DomainPaginator).domain,
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
