package me.ccrama.redditslide.Fragments

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.DefaultOnStateChangedListener
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import ltd.ucode.slide.App
import ltd.ucode.network.ContentType
import ltd.ucode.network.ContentType.Companion.fullImage
import ltd.ucode.network.ContentType.Companion.getContentType
import ltd.ucode.network.ContentType.Companion.isImgurLink
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.ShadowboxComments
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.Adapters.CommentUrlObject
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SecretConstants
import me.ccrama.redditslide.SubmissionViews.PopulateShadowboxInfo.doActionbar
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.views.ExoVideoView
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.math.roundToInt

class MediaFragmentComment : Fragment() {
    var contentUrl: String? = null
    var sub: String? = null
    var actuallyLoaded: String? = null
    var i = 0
    private var rootView: ViewGroup? = null
    private var videoView: ExoVideoView? = null
    private var imageShown = false
    private var previous = 0f
    private var hidden = false
    private var stopPosition: Long = 0
    var isGif = false
    private var s: CommentUrlObject? = null
    private var client: OkHttpClient? = null
    private var gson: Gson? = null
    private var mashapeKey: String? = null

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.v("Destroying")
        (rootView!!.findViewById<View>(R.id.submission_image) as SubsamplingScaleImageView).recycle()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && videoView != null) {
            videoView!!.seekTo(0)
            videoView!!.play()
        }
    }

    override fun onResume() {
        super.onResume()
        if (videoView != null) {
            videoView!!.seekTo(stopPosition.toInt().toLong())
            videoView!!.play()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (videoView != null) {
            stopPosition = videoView!!.currentPosition
            videoView!!.pause()
            (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                PanelState.COLLAPSED
            outState.putLong("position", stopPosition)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.submission_mediacard, container, false) as ViewGroup
        if (savedInstanceState != null && savedInstanceState.containsKey("position")) {
            stopPosition = savedInstanceState.getLong("position")
        }
        val slideLayout = rootView!!.findViewById<SlidingUpPanelLayout>(R.id.sliding_layout)
        doActionbar(s!!.comment, rootView!!, requireActivity(), true)
        rootView!!.findViewById<View>(R.id.thumbimage2).visibility = View.GONE
        val type = getContentType(contentUrl!!)
        if (fullImage(type)) {
            rootView!!.findViewById<View>(R.id.thumbimage2).visibility = View.GONE
        }
        addClickFunctions(
            rootView!!.findViewById(R.id.submission_image), slideLayout, rootView,
            type, activity, s
        )
        doLoad(contentUrl)
        val openClick = View.OnClickListener {
            (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                PanelState.EXPANDED
        }
        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
        val title = rootView!!.findViewById<View>(R.id.title)
        title.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    (rootView!!.findViewById<View>(
                        R.id.sliding_layout
                    ) as SlidingUpPanelLayout).panelHeight = title.measuredHeight
                    title.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).addPanelSlideListener(
            object : PanelSlideListener {
                override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
                    if (newState === PanelState.EXPANDED) {
                        val c = s!!.comment.comment
                        rootView!!.findViewById<View>(R.id.base)
                            .setOnClickListener { v: View? ->
                                val url = ("https://reddit.com/r/${c.subredditName}/comments/${
                                    c.dataNode["link_id"].asText().substring(3)
                                }/nothing/${c.id}?context=3")
                                OpenRedditLink.openUrl(activity, url, true)
                            }
                    } else {
                        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
                    }
                }

                override fun onPanelSlide(panel: View, slideOffset: Float) {
                }
            })
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        i = bundle!!.getInt("page")
        s = ShadowboxComments.comments[i]
        sub = s!!.comment.comment.subredditName
        contentUrl = bundle.getString("contentUrl")
        client = App.client
        gson = Gson()
        mashapeKey = SecretConstants.getImgurApiKey(context)
    }

    fun doLoad(contentUrl: String?) {
        when (getContentType(contentUrl!!)) {
            ContentType.Type.DEVIANTART -> doLoadDeviantArt(contentUrl)
            ContentType.Type.IMAGE -> doLoadImage(contentUrl)
            ContentType.Type.IMGUR -> doLoadImgur(contentUrl)
            ContentType.Type.XKCD -> doLoadXKCD(contentUrl)
            ContentType.Type.STREAMABLE, ContentType.Type.GIF -> doLoadGif(contentUrl)
            else -> {}
        }
    }

    fun doLoadXKCD(url: String?) {
        var url = url
        if (!url!!.endsWith("/")) {
            url = "$url/"
        }
        if (NetworkUtil.isConnected(context)) {
            val apiUrl = url + "info.0.json"
            LogUtil.v(apiUrl)
            val finalUrl = url
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getJsonObject(client, gson, apiUrl)
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                    } else {
                        try {
                            if (result != null && !result.isJsonNull && result.has("img")) {
                                doLoadImage(result["img"].asString)
                                rootView!!.findViewById<View>(R.id.submission_image)
                                    .setOnLongClickListener {
                                        try {
                                            AlertDialog.Builder(context!!)
                                                .setTitle(result["safe_title"].asString)
                                                .setMessage(result["alt"].asString)
                                                .show()
                                        } catch (ignored: Exception) {
                                        }
                                        true
                                    }
                            } else {
                                val i = Intent(context, Website::class.java)
                                i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                                context!!.startActivity(i)
                            }
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                            val i = Intent(context, Website::class.java)
                            i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                            context!!.startActivity(i)
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun doLoadGif(dat: String?) {
        isGif = true
        videoView = rootView!!.findViewById(R.id.gif)
        videoView!!.clearFocus()
        rootView!!.findViewById<View>(R.id.gifarea).visibility = View.VISIBLE
        rootView!!.findViewById<View>(R.id.submission_image).visibility =
            View.GONE
        val loader = rootView!!.findViewById<ProgressBar>(R.id.gifprogress)
        rootView!!.findViewById<View>(R.id.progress).visibility =
            View.GONE
        val gif = AsyncLoadGif(
            requireActivity(), videoView!!, loader,
            rootView!!.findViewById(R.id.placeholder),
            closeIfNull = false,
            autostart = true,
            subreddit = sub!!
        )
        gif.execute(dat)
    }

    fun doLoadDeviantArt(url: String?) {
        val apiUrl = "http://backend.deviantart.com/oembed?url=$url"
        LogUtil.v(apiUrl)
        object : AsyncTask<Void?, Void?, JsonObject?>() {
            override fun doInBackground(vararg params: Void?): JsonObject? {
                return HttpUtil.getJsonObject(client, gson, apiUrl)
            }

            override fun onPostExecute(result: JsonObject?) {
                LogUtil.v("doLoad onPostExecute() called with: result = [$result]")
                if (result != null && !result.isJsonNull && (result.has("fullsize_url")
                            || result.has("url"))
                ) {
                    val url: String = if (result.has("fullsize_url")) {
                        result["fullsize_url"].asString
                    } else {
                        result["url"].asString
                    }
                    doLoadImage(url)
                } else {
                    val i = Intent(activity, Website::class.java)
                    i.putExtra(LinkUtil.EXTRA_URL, contentUrl)
                    activity!!.startActivity(i)
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doLoadImgur(url: String?) {
        var url = url
        if (url!!.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        val finalUrl = url
        var hash = url.substring(url.lastIndexOf("/"))
        if (NetworkUtil.isConnected(activity)) {
            if (hash.startsWith("/")) hash = hash.substring(1)
            val apiUrl = "https://imgur-apiv3.p.mashape.com/3/image/$hash.json"
            LogUtil.v(apiUrl)
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getImgurMashapeJsonObject(client, gson, apiUrl, mashapeKey)
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                        activity!!.finish()
                    } else {
                        try {
                            if (result != null && !result.isJsonNull && result.has("image")) {
                                val type = result["image"]
                                    .asJsonObject["image"]
                                    .asJsonObject["type"]
                                    .asString
                                val urls = result["image"]
                                    .asJsonObject["links"]
                                    .asJsonObject["original"]
                                    .asString
                                if (type.contains("gif")) {
                                    doLoadGif(urls)
                                } else if (!imageShown) { //only load if there is no image
                                    doLoadImage(urls)
                                }
                            } else if (result != null && result.has("data")) {
                                val type = result["data"]
                                    .asJsonObject["type"]
                                    .asString
                                val urls = result["data"]
                                    .asJsonObject["link"]
                                    .asString
                                var mp4: String? = ""
                                if (result["data"].asJsonObject.has("mp4")) {
                                    mp4 = result["data"]
                                        .asJsonObject["mp4"]
                                        .asString
                                }
                                if (type.contains("gif")) {
                                    doLoadGif(if (mp4.isNullOrEmpty()) urls else mp4)
                                } else if (!imageShown) { //only load if there is no image
                                    doLoadImage(urls)
                                }
                            } else {
                                if (!imageShown) doLoadImage(finalUrl)
                            }
                        } catch (e: Exception) {
                            LogUtil.e(
                                e, "Error loading Imgur image finalUrl = ["
                                        + finalUrl
                                        + "], apiUrl = ["
                                        + apiUrl
                                        + "]"
                            )
                            //todo open it?
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun doLoadImage(contentUrl: String?) {
        var contentUrl = contentUrl
        if (contentUrl != null && contentUrl.contains("bildgur.de")) {
            contentUrl = contentUrl.replace("b.bildgur.de", "i.imgur.com")
        }
        if (contentUrl != null && isImgurLink(contentUrl)) {
            contentUrl = "$contentUrl.png"
        }
        rootView!!.findViewById<View>(R.id.gifprogress).visibility = View.GONE
        if (contentUrl != null && contentUrl.contains("m.imgur.com")) {
            contentUrl = contentUrl.replace("m.imgur.com", "i.imgur.com")
        }
        if (contentUrl != null && !contentUrl.startsWith("https://i.redditmedia.com")
            && !contentUrl.startsWith("https://i.reddituploads.com")
            && !contentUrl.contains(
                "imgur.com"
            )
        ) { //we can assume redditmedia and imgur links are to direct images and not websites
            rootView!!.findViewById<View>(R.id.progress).visibility = View.VISIBLE
            (rootView!!.findViewById<View>(R.id.progress) as ProgressBar).isIndeterminate = true
            val finalUrl2: String = contentUrl
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    try {
                        val obj = URL(finalUrl2)
                        val conn = obj.openConnection()
                        val type = conn.getHeaderField("Content-Type")
                        if (activity != null) {
                            activity!!.runOnUiThread {
                                if (!imageShown && type != null && !type.isEmpty()
                                    && type.startsWith("image/")
                                ) {
                                    //is image
                                    if (type.contains("gif")) {
                                        doLoadGif(
                                            finalUrl2.replace(".jpg", ".gif")
                                                .replace(".png", ".gif")
                                        )
                                    } else if (!imageShown) {
                                        displayImage(finalUrl2)
                                    }
                                    actuallyLoaded = finalUrl2
                                } else if (!imageShown) {
                                    val i = Intent(activity, Website::class.java)
                                    i.putExtra(LinkUtil.EXTRA_URL, finalUrl2)
                                    activity!!.startActivity(i)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        LogUtil.e(e, "Error loading image finalUrl2 = [$finalUrl2]")
                    }
                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayImage(contentUrl)
        }
        actuallyLoaded = contentUrl
    }

    fun displayImage(url: String?) {
        if (!imageShown) {
            actuallyLoaded = url
            val i = rootView!!.findViewById<SubsamplingScaleImageView>(R.id.submission_image)
            i.setMinimumDpi(70)
            i.setMinimumTileDpi(240)
            val bar = rootView!!.findViewById<ProgressBar>(R.id.progress)
            bar.isIndeterminate = false
            bar.progress = 0
            val handler = Handler()
            val progressBarDelayRunner = Runnable { bar.visibility = View.VISIBLE }
            handler.postDelayed(progressBarDelayRunner, 500)
            val fakeImage = ImageView(activity)
            fakeImage.layoutParams = LinearLayout.LayoutParams(i.width, i.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            val f = (requireActivity().applicationContext as App).imageLoader!!
                .getDiskCache()[url]
            if (f != null && f.exists()) {
                imageShown = true
                try {
                    i.setImage(ImageSource.Uri(f.absolutePath))
                } catch (e: Exception) {
                    //todo  i.setImage(ImageSource.bitmap(loadedImage));
                }
                rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
                handler.removeCallbacks(progressBarDelayRunner)
                previous = i.scale
                val base = i.scale
                i.onStateChangedListener = object : DefaultOnStateChangedListener {
                    override fun onScaleChanged(newScale: Float, origin: Int) {
                        if (newScale > previous && !hidden && newScale > base) {
                            hidden = true
                            val base = rootView!!.findViewById<View>(R.id.base)
                            val va = ValueAnimator.ofFloat(1.0f, 0.2f)
                            val mDuration = 250 //in millis
                            va.duration = mDuration.toLong()
                            va.addUpdateListener { animation ->
                                val value = animation.animatedValue as Float
                                base.alpha = value
                            }
                            va.start()
                            //hide
                        } else if (newScale <= previous && hidden) {
                            hidden = false
                            val base = rootView!!.findViewById<View>(R.id.base)
                            val va = ValueAnimator.ofFloat(0.2f, 1.0f)
                            val mDuration = 250 //in millis
                            va.duration = mDuration.toLong()
                            va.addUpdateListener { animation ->
                                val value = animation.animatedValue as Float
                                base.alpha = value
                            }
                            va.start()
                            //unhide
                        }
                        previous = newScale
                    }
                }
            } else {
                (requireActivity().applicationContext as App).imageLoader!!
                    .displayImage(url, ImageViewAware(fakeImage),
                        DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                            .cacheOnDisk(true)
                            .imageScaleType(ImageScaleType.NONE)
                            .cacheInMemory(false)
                            .build(), object : ImageLoadingListener {
                            override fun onLoadingStarted(imageUri: String, view: View) {
                                imageShown = true
                            }

                            override fun onLoadingFailed(
                                imageUri: String, view: View,
                                failReason: FailReason
                            ) {
                                Log.v(LogUtil.getTag(), "LOADING FAILED")
                            }

                            override fun onLoadingComplete(
                                imageUri: String, view: View,
                                loadedImage: Bitmap
                            ) {
                                imageShown = true
                                var f: File? = null
                                if (activity != null) {
                                    f = (activity!!.applicationContext as App)
                                        .imageLoader!!
                                        .diskCache[url]
                                }
                                if (f != null && f.exists()) {
                                    i.setImage(ImageSource.Uri(f.absolutePath))
                                } else {
                                    i.setImage(ImageSource.Bitmap(loadedImage))
                                }
                                rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
                                handler.removeCallbacks(progressBarDelayRunner)
                                previous = i.scale
                                val base = i.scale
                                i.onStateChangedListener =
                                    object : DefaultOnStateChangedListener {
                                        override fun onScaleChanged(newScale: Float, origin: Int) {
                                            if (newScale > previous && !hidden && newScale > base) {
                                                hidden = true
                                                val base = rootView!!.findViewById<View>(
                                                    R.id.base
                                                )
                                                val va = ValueAnimator.ofFloat(
                                                    1.0f,
                                                    0.2f
                                                )
                                                val mDuration = 250 //in millis
                                                va.duration = mDuration.toLong()
                                                va.addUpdateListener { animation ->
                                                    val value = animation
                                                        .animatedValue as Float
                                                    base.alpha = value
                                                }
                                                va.start()
                                                //hide
                                            } else if (newScale <= previous && hidden) {
                                                hidden = false
                                                val base = rootView!!.findViewById<View>(
                                                    R.id.base
                                                )
                                                val va = ValueAnimator.ofFloat(
                                                    0.2f,
                                                    1.0f
                                                )
                                                val mDuration = 250 //in millis
                                                va.duration = mDuration.toLong()
                                                va.addUpdateListener { animation ->
                                                    val value = animation
                                                        .animatedValue as Float
                                                    base.alpha = value
                                                }
                                                va.start()
                                                //unhide
                                            }
                                            previous = newScale
                                        }
                                    }
                            }

                            override fun onLoadingCancelled(imageUri: String, view: View) {
                                Log.v(LogUtil.getTag(), "LOADING CANCELLED")
                            }
                        }, { imageUri, view, current, total ->
                            (rootView!!.findViewById<View>(
                                R.id.progress
                            ) as ProgressBar).progress = (100.0f * current / total).roundToInt()
                        })
            }
        }
    }

    companion object {
        private fun addClickFunctions(
            base: View, slidingPanel: SlidingUpPanelLayout,
            clickingArea: View?, type: ContentType.Type, contextActivity: Activity?,
            submission: CommentUrlObject?
        ) {
            base.setOnClickListener {
                if (slidingPanel.panelState == PanelState.EXPANDED) {
                    slidingPanel.panelState = PanelState.COLLAPSED
                } else {
                    if (type === ContentType.Type.IMAGE) {
                        if (SettingValues.image) {
                            val myIntent = Intent(contextActivity, MediaView::class.java)
                            val url = submission!!.getUrl()
                            myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, submission.getUrl())
                            myIntent.putExtra(MediaView.EXTRA_URL, url)
                            myIntent.putExtra(MediaView.SUBREDDIT, submission.subredditName)
                            //May be a bug with downloading multiple comment albums off the same submission
                            myIntent.putExtra(
                                ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                submission.comment.comment.submissionTitle
                            )
                            myIntent.putExtra(MediaView.EXTRA_SHARE_URL, submission.getUrl())
                            contextActivity!!.startActivity(myIntent)
                        } else {
                            openExternally(submission!!.getUrl())
                        }
                    }
                }
            }
        }
    }
}
