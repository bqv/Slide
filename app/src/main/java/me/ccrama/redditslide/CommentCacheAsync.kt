package me.ccrama.redditslide

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import ltd.ucode.lemmy.api.ApiException
import ltd.ucode.lemmy.data.LemmyPost
import ltd.ucode.lemmy.data.type.CommentSortType
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.lemmy.data.type.PostId
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.getCommentSorting
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.util.GifUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.PhotoLoader

class CommentCacheAsync : AsyncTask<Any?, Any?, Any?> {
    var alreadyReceived: List<IPost>? = null
    var mNotifyManager: NotificationManager? = null

    constructor(
        submissions: List<IPost>?, c: Context, subreddit: String,
        otherChoices: BooleanArray
    ) {
        alreadyReceived = submissions
        context = c
        subs = arrayOf(subreddit)
        this.otherChoices = otherChoices
    }

    constructor(
        submissions: List<IPost>?, mContext: Activity, baseSub: String,
        alternateSubName: String?
    ) : this(submissions, mContext, baseSub, booleanArrayOf(true, true)) {
    }

    constructor(c: Context, subreddits: Array<String>) {
        context = c
        subs = subreddits
    }

    var subs: Array<String>
    var context: Context
    var mBuilder: NotificationCompat.Builder? = null
    var otherChoices: BooleanArray = emptyArray<Boolean>().toBooleanArray()
    public override fun doInBackground(params: Array<Any?>): Void? {
        if (Authentication.isLoggedIn && Authentication.me == null || Authentication.reddit == null) {
            if (Authentication.reddit == null) {
                Authentication(context)
            }
            if (Authentication.reddit != null) {
                try {
                    Authentication.me = Authentication.reddit!!.me()
                    Authentication.mod = Authentication.me!!.isMod()
                    SettingValues.authentication.edit()
                        .putBoolean(App.SHARED_PREF_IS_MOD, Authentication.mod)
                        .apply()
                    val name = Authentication.me!!.getFullName()
                    Authentication.name = name
                    LogUtil.v("AUTHENTICATED")
                    if (Authentication.reddit!!.isAuthenticated) {
                        val accounts = SettingValues.authentication.getStringSet("accounts", HashSet())
                        if (accounts!!.contains(name)) { //convert to new system
                            accounts.remove(name)
                            accounts.add(name + ":" + Authentication.refresh)
                            SettingValues.authentication.edit().putStringSet("accounts", accounts)
                                .apply() //force commit
                        }
                        Authentication.isLoggedIn = true
                        App.notFirst = true
                    }
                } catch (e: Exception) {
                    Authentication(context)
                }
            }
        }
        val multiNameToSubsMap = UserSubscriptions.getMultiNameToSubs(true)
        if (Authentication.reddit == null) App.authentication = Authentication(context)
        val success = ArrayList<String?>()
        for (fSub in subs) {
            val sortType = getCommentSorting(fSub)
            val sub: String? = if (multiNameToSubsMap.containsKey(fSub)) {
                multiNameToSubsMap[fSub]
            } else {
                fSub
            }
            if (sub!!.isNotEmpty()) {
                if (sub != SAVED_SUBMISSIONS) {
                    mNotifyManager =
                        ContextCompat.getSystemService(context, NotificationManager::class.java)
                    mBuilder = NotificationCompat.Builder(context, App.CHANNEL_COMMENT_CACHE)
                    mBuilder!!.setOngoing(true)
                    mBuilder!!.setContentTitle(
                        context.getString(
                            R.string.offline_caching_title,
                            if (sub.equals(
                                    "frontpage",
                                    ignoreCase = true
                                )
                            ) fSub else if (fSub.contains("/m/")) fSub else "/r/$fSub"
                        )
                    )
                        .setSmallIcon(R.drawable.ic_save)
                }
                val submissions: MutableList<IPost> = ArrayList()
                val newFullnames = ArrayList<String>()
                var count = 0
                if (alreadyReceived != null) {
                    submissions.addAll(alreadyReceived!!)
                } else {
                    var p = if (fSub.equals("frontpage", ignoreCase = true)) {
                        //SubredditPaginator(Authentication.reddit)
                        Authentication.api!!.getPosts()
                    } else {
                        //SubredditPaginator(Authentication.reddit, sub)
                        Authentication.api!!.getPosts(communityName = sub)
                    }
                    //p.setLimit(Constants.PAGINATOR_POST_LIMIT)
                    try {
                        val page = runBlocking { p!!.next() }
                        submissions.addAll(page.map { LemmyPost(Authentication.api!!.instance, it) })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val commentDepth = SettingValues.commentDepth ?: 5
                val commentCount = SettingValues.commentCount ?: 50
                Log.v("CommentCacheAsync", "comment count $commentCount")
                val random = (Math.random() * 100).toInt()
                for (s in submissions) {
                    try {
                        val commentStore = getSubmission(
                            id = s.postId,
                            limit = commentCount,
                            depth = commentDepth,
                            sort = CommentSortType.from(sortType)
                        )
                        OfflineSubreddit.writeSubmission(commentStore, s, context)
                        newFullnames.add(s.permalink)
                        if (!SettingValues.noImages) PhotoLoader.loadPhoto(context, s)
                        when (s.contentType) {
                            ContentType.Type.VREDDIT_DIRECT, ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.GIF -> if (otherChoices[0]) {
                                if (context is Activity) {
                                    (context as Activity).runOnUiThread {
                                        GifUtils.cacheSaveGif(
                                            Uri.parse(GifUtils.AsyncLoadGif.formatUrl(s.url)),
                                            context as Activity,
                                            s.groupName,
                                            null,
                                            false
                                        )
                                    }
                                }
                            }

                            ContentType.Type.ALBUM -> if (otherChoices[1]) //todo this AlbumUtils.saveAlbumToCache(context, s.getUrl());
                            {
                                break
                            }

                            else -> {}
                        }
                    } catch (ignored: Exception) {
                    }
                    count += 1
                    if (mBuilder != null) {
                        mBuilder!!.setProgress(submissions.size, count, false)
                        mNotifyManager!!.notify(random, mBuilder!!.build())
                    }
                }
                OfflineSubreddit.newSubreddit(sub).writeToMemory(newFullnames)
                if (mBuilder != null) {
                    mNotifyManager!!.cancel(random)
                }
                if (!submissions.isEmpty()) success.add(sub)
            }
        }
        if (mBuilder != null) {
            mBuilder!!.setContentText(context.getString(R.string.offline_caching_complete)) // Removes the progress bar
                .setSubText(success.size.toString() + " subreddits cached").setProgress(0, 0, false)
            mBuilder!!.setOngoing(false)
            mNotifyManager!!.notify(2001, mBuilder!!.build())
        }
        return null
    }

    data class CommentStore(val sort: CommentSortType?, val comments: List<CommentView>)

    @Throws(ApiException::class)
    fun getSubmission(id: PostId,
                      depth: Int? = null,
                      limit: Int? = null,
                      sort: CommentSortType? = null): CommentStore {
        val paginator = Authentication.api!!.getComments(
            postId = id,
            maxDepth = depth,
            limit = limit,
            sort = sort
        )
        val comments: MutableList<CommentView> = mutableListOf()
        while (paginator.hasNext) {
            val page = runBlocking { paginator.next() }
            comments.addAll(page)
        }
        return CommentStore(sort, comments)
    }

    companion object {
        const val SAVED_SUBMISSIONS = "read later"
    }
}
