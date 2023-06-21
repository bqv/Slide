package me.ccrama.redditslide.Adapters

import android.content.Context
import android.os.AsyncTask
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.getSubmissionSort
import ltd.ucode.slide.SettingValues.getSubmissionTimePeriod
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.LastComments
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SubmissionCache
import me.ccrama.redditslide.Synccit.MySynccitReadTask
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.PhotoLoader
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.paginators.MultiRedditPaginator

/**
 * This class is reponsible for loading subreddit specific submissions
 * [] is implemented
 * asynchronously.
 */
class MultiredditPosts(multireddit: String, profile: String) : PostLoader {
    override var posts: MutableList<IPost> = mutableListOf()

    @JvmField
    var nomore = false
    var stillShow = false
    var offline = false
    @JvmField
    var loading = false
    var profile: String
    private var paginator: MultiRedditPaginator? = null
    var c: Context? = null
    var adapter: MultiredditAdapter? = null
    override fun loadMore(context: Context, displayer: SubmissionDisplay, reset: Boolean) {
        c = context
        LoadData(context, displayer, reset).execute(multiReddit)
    }

    fun loadMore(
        context: Context,
        displayer: SubmissionDisplay,
        reset: Boolean,
        adapter: MultiredditAdapter?
    ) {
        this.adapter = adapter
        c = context
        loadMore(context, displayer, reset)
    }

    @JvmField
    var multiReddit: MultiReddit? = null
    override fun hasMore(): Boolean {
        return !nomore
    }

    var usedOffline = false

    init {
        posts = ArrayList()
        LogUtil.e("MJWHITTA: Profile is $profile.")
        LogUtil.e("MJWHITTA: Multireddit is $multireddit.")
        if (profile.isEmpty()) {
            multiReddit = UserSubscriptions.getMultiredditByDisplayName(multireddit)
        } else {
            multiReddit = UserSubscriptions.getPublicMultiredditByDisplayName(profile, multireddit)
        }
        this.profile = profile
    }

    /**
     * Asynchronous task for loading data
     */
    private inner class LoadData(
        var context: Context,
        val displayer: SubmissionDisplay,
        val reset: Boolean
    ) : AsyncTask<MultiReddit?, Void?, List<IPost?>?>() {
        public override fun onPostExecute(submissions: List<IPost?>?) {
            loading = false
            if (submissions != null && !submissions.isEmpty()) {
                // new submissions found
                var start = 0
                if (posts != null) {
                    start = posts!!.size + 1
                }
                if (reset || offline || posts == null) {
                    posts = submissions.filterNotNull().toHashSet().toMutableList()
                    start = -1
                } else {
                    posts!!.addAll(submissions.filterNotNull())
                    posts = posts.toHashSet().toMutableList()
                    offline = false
                }
                if (!usedOffline) OfflineSubreddit.getSubreddit(
                    "multi" + multiReddit!!.displayName.lowercase(),
                    false,
                    context
                ).overwriteSubmissions(posts.map { it.submission }).writeToMemory(c)
                val ids = arrayOfNulls<String>(submissions.size)
                var i = 0
                for (s in submissions) {
                    ids[i] = s!!.id
                    i++
                }
                if (!SettingValues.synccitName!!.isEmpty() && !offline) {
                    MySynccitReadTask().execute(*ids)
                }
                val finalStart = start

                // update online
                displayer.updateSuccess(posts, finalStart)
            } else if (submissions != null) {
                // end of submissions
                nomore = true
            } else if (!OfflineSubreddit.getSubreddit(
                    "multi" + multiReddit!!.displayName.lowercase(),
                    false,
                    context
                ).submissions.isEmpty() && !nomore && SettingValues.cache
            ) {
                offline = true
                val cached = OfflineSubreddit.getSubreddit(
                    "multi" + multiReddit!!.displayName.lowercase(),
                    true,
                    context
                )
                val finalSubs: MutableList<IPost> = ArrayList()
                for (s in cached.submissions) {
                    if (!PostMatch.doesMatch(
                            RedditSubmission(s),
                            "multi" + multiReddit!!.displayName.lowercase(),
                            false
                        )
                    ) {
                        finalSubs.add(RedditSubmission(s))
                    }
                }
                posts = finalSubs
                if (!cached.submissions.isEmpty()) {
                    stillShow = true
                } else {
                    displayer.updateOfflineError()
                }
                // update offline
                displayer.updateOffline(submissions!!, cached.time)
            } else if (!nomore) {
                // error
                displayer.updateError()
            }
        }

        override fun doInBackground(vararg subredditPaginators: MultiReddit?): List<IPost?>? {
            if (!NetworkUtil.isConnected(context)) {
                offline = true
                return null
            } else {
                offline = false
            }
            stillShow = true
            if (reset || paginator == null) {
                offline = false
                paginator = MultiRedditPaginator(Authentication.reddit, subredditPaginators[0])
                paginator!!.sorting = getSubmissionSort(
                    "multi" + subredditPaginators[0]!!.displayName.lowercase()
                )
                paginator!!.timePeriod =
                    getSubmissionTimePeriod("multi" + subredditPaginators[0]!!.displayName.lowercase())
                paginator!!.setLimit(Constants.PAGINATOR_POST_LIMIT)
            }
            val things: MutableList<IPost> = ArrayList()
            try {
                if (paginator?.hasNext() == true) {
                    things.addAll(paginator!!.next().map(::RedditSubmission))
                } else {
                    nomore = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.message!!.contains("Forbidden")) {
                    App.authentication!!.updateToken(context)
                }
            }
            val filteredSubmissions: MutableList<IPost?> = ArrayList()
            for (s in things) {
                if (!PostMatch.doesMatch(s, paginator!!.multiReddit.displayName, false)) {
                    filteredSubmissions.add(s)
                }
            }
            HasSeen.setHasSeenSubmission(filteredSubmissions.mapNotNull { it?.submission })
            SubmissionCache.cacheSubmissions(
                filteredSubmissions.filterNotNull(),
                context,
                paginator!!.multiReddit.displayName
            )
            if (!(SettingValues.noImages && (!NetworkUtil.isConnectedWifi(c) && SettingValues.lowResMobile || SettingValues.lowResAlways))) PhotoLoader.loadPhotos(
                c!!,
                filteredSubmissions.filterNotNull()
            )
            if (SettingValues.storeHistory) LastComments.setCommentsSince(filteredSubmissions.filterNotNull())
            return filteredSubmissions
        }
    }
}
