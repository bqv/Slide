package me.ccrama.redditslide

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import ltd.ucode.network.ContentType
import ltd.ucode.slide.R
import me.ccrama.redditslide.Adapters.SubredditPosts
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.models.Submission

class StackRemoteViewsFactory(private val mContext: Context, intent: Intent) : RemoteViewsFactory {
    private var submissions: MutableList<Submission> = ArrayList()
    private val posts: SubredditPosts? = null

    init {
        val mAppWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    override fun onCreate() {}
    override fun onDestroy() {
        submissions.clear()
    }

    override fun getCount(): Int {
        return submissions.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(mContext.packageName, R.layout.submission_widget)
        if (position <= count) {
            val submission = submissions[position]
            var url: String? = ""
            val type = ContentType.getContentType(submission)
            if (type == ContentType.Type.IMAGE) {
                url = submission.url
            } else if (submission.dataNode.has("preview") && submission.dataNode["preview"]["images"][0]["source"].has(
                    "height"
                ) && submission.dataNode["preview"]["images"][0]["source"]["height"].asInt() > 200
            ) {
                url = submission.dataNode["preview"]["images"][0]["source"]["url"].asText()
            } else if (submission.thumbnail != null && (submission.thumbnailType == Submission.ThumbnailType.URL || submission.thumbnailType == Submission.ThumbnailType.NSFW)) {
                url = submission.thumbnail
            }
            try {

                //todo rv.setImageViewBitmap(R.id.thumbnail, Glide.with(mContext).load(url).asBitmap().);
                rv.setTextViewText(R.id.title, CompatUtil.fromHtml(submission.title))
            } catch (e: Exception) {
                Log.v(LogUtil.getTag(), e.toString())
            }
            rv.setTextViewText(R.id.title, CompatUtil.fromHtml(submission.title))
            rv.setTextViewText(R.id.subreddit, submission.subredditName)
            rv.setTextViewText(
                R.id.info,
                submission.author + " " + TimeUtils.getTimeAgo(submission.created.time, mContext)
            )
            val extras = Bundle()
            extras.putString("url", submission.url)
            val fillInIntent = Intent()
            fillInIntent.putExtras(extras)
            rv.setOnClickFillInIntent(R.id.card, fillInIntent)
        }
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        // if (posts == null) {
        //    posts = new SubredditPosts("all", StackWidgetService.this);
        Log.v(LogUtil.getTag(), "MAKING POSTS")
        // }
        // posts.loadMore(mContext, null, true);
        //TODO
        submissions = posts!!.posts.map { it.submission!! }.toMutableList()
        Log.v(LogUtil.getTag(), "POSTS IS SIZE " + submissions.size)
    }
}
