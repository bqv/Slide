package me.ccrama.redditslide.Fragments

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
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
import com.google.common.base.Strings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.ContentType.Companion.getContentType
import ltd.ucode.slide.ContentType.Companion.isImgurLink
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.isNSFWEnabled
import ltd.ucode.slide.ui.commentsScreen.CommentsScreen
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Activities.AlbumPager
import me.ccrama.redditslide.Activities.FullscreenVideo
import me.ccrama.redditslide.Activities.GalleryImage
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.RedditGallery
import me.ccrama.redditslide.Activities.RedditGalleryPager
import me.ccrama.redditslide.Activities.Shadowbox
import me.ccrama.redditslide.Activities.Tumblr
import me.ccrama.redditslide.Activities.TumblrPager
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.SecretConstants
import me.ccrama.redditslide.SubmissionViews.PopulateShadowboxInfo.doActionbar
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openGif
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openImage
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openRedditContent
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.JsonUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LinkUtil.openUrl
import me.ccrama.redditslide.util.LinkUtil.tryOpenWithVideoPlugin
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.views.ExoVideoView
import net.dean.jraw.models.Submission
import okhttp3.OkHttpClient
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.math.roundToInt

class MediaFragment : Fragment() {
    var firstUrl: String? = null
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
    private var gif: AsyncLoadGif? = null
    private val s: Submission? = null
    private var client: OkHttpClient? = null
    private var gson: Gson? = null
    private var mashapeKey: String? = null

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.v("Destroying")
        if (rootView!!.findViewById<View>(R.id.submission_image) != null) {
            (rootView!!.findViewById<View>(R.id.submission_image) as SubsamplingScaleImageView).recycle()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (videoView != null) {
            if (isVisibleToUser) {
                videoView!!.seekTo(0)
                videoView!!.play()
            } else {
                videoView!!.pause()
            }
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
        doActionbar(RedditSubmission((s)!!), (rootView)!!, requireActivity(), true)
        val thumbnailView = (rootView!!.findViewById<View>(R.id.thumbimage2))
        thumbnailView.visibility = View.GONE
        val typeImage = rootView!!.findViewById<ImageView>(R.id.type)
        typeImage.visibility = View.VISIBLE
        val img = rootView!!.findViewById<SubsamplingScaleImageView>(R.id.submission_image)
        val slideLayout = rootView!!.findViewById<SlidingUpPanelLayout>(R.id.sliding_layout)
        var type = getContentType(s)
        if (type === ContentType.Type.VREDDIT_REDIRECT || type === ContentType.Type.VREDDIT_DIRECT) {
            if (((!s.dataNode.has("media") || !s.dataNode["media"].has("reddit_video"))
                        && !s.dataNode.has("crosspost_parent_list"))
            ) {
                type = ContentType.Type.LINK
            }
        }
        img.alpha = 1f
        if ((Strings.isNullOrEmpty(s.thumbnail)
                    || Strings.isNullOrEmpty(firstUrl)
                    || (s.isNsfw && isNSFWEnabled))
        ) {
            thumbnailView.visibility = View.VISIBLE
            (thumbnailView as ImageView).setImageResource(R.drawable.web)
            addClickFunctions(
                thumbnailView, slideLayout, rootView,
                type, activity, s
            )
            addClickFunctions(
                typeImage, slideLayout, rootView,
                type, activity, s
            )
            (rootView!!.findViewById<View>(R.id.progress)).visibility = View.GONE
            if ((s.isNsfw && isNSFWEnabled)) {
                thumbnailView.setImageResource(
                    R.drawable.nsfw
                )
            } else {
                if (Strings.isNullOrEmpty(firstUrl) && !Strings.isNullOrEmpty(
                        s.thumbnail
                    )
                ) {
                    (requireContext().applicationContext as App).imageLoader!!
                        .displayImage(s.thumbnail, thumbnailView)
                }
            }
        } else {
            thumbnailView.visibility = View.GONE
            addClickFunctions(img, slideLayout, rootView, type, activity, s)
        }
        if (!s.isNsfw || !isNSFWEnabled) {
            if ((type === ContentType.Type.EXTERNAL
                        ) || (type === ContentType.Type.LINK
                        ) || (type === ContentType.Type.VIDEO)
            ) {
                doLoad(firstUrl, type)
            } else {
                doLoad(contentUrl, type)
            }
        }
        when (type) {
            ContentType.Type.ALBUM, ContentType.Type.REDDIT_GALLERY -> typeImage.setImageResource(R.drawable.ic_photo_library)
            ContentType.Type.EXTERNAL, ContentType.Type.LINK, ContentType.Type.REDDIT -> {
                typeImage.setImageResource(R.drawable.ic_public)
                rootView!!.findViewById<View>(R.id.submission_image).alpha = 0.5f
            }

            ContentType.Type.SELF -> typeImage.setImageResource(R.drawable.ic_text_fields)
            ContentType.Type.EMBEDDED, ContentType.Type.VIDEO -> {
                typeImage.setImageResource(R.drawable.ic_play_arrow)
                rootView!!.findViewById<View>(R.id.submission_image).alpha = 0.5f
            }

            else -> typeImage.visibility = View.GONE
        }
        rootView!!.findViewById<View>(R.id.base).setOnClickListener {
            val i2 = Intent(activity, CommentsScreen::class.java)
            i2.putExtra(CommentsScreen.EXTRA_PAGE, i)
            i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, sub)
            requireActivity().startActivity(i2)
        }
        val openClick: View.OnClickListener = View.OnClickListener {
            (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                PanelState.EXPANDED
        }
        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
        val title = rootView!!.findViewById<View>(R.id.title)
        title.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    slideLayout.panelHeight = title.measuredHeight
                    title.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        slideLayout.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(view: View, v: Float) {}
            override fun onPanelStateChanged(
                panel: View,
                previousState: PanelState,
                newState: PanelState
            ) {
                if (newState == PanelState.EXPANDED) {
                    rootView!!.findViewById<View>(R.id.base).setOnClickListener { v: View? ->
                        val i2 = Intent(activity, CommentsScreen::class.java)
                        i2.putExtra(CommentsScreen.EXTRA_PAGE, i)
                        i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, sub)
                        requireActivity().startActivity(i2)
                    }
                } else {
                    rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
                }
            }
        })
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        firstUrl = bundle!!.getString("firstUrl")
        sub = (activity as Shadowbox?)!!.subreddit
        i = bundle.getInt("page")
        if ((activity as Shadowbox?)!!.subredditPosts!!.posts.size != 0) {
            //s = ((Shadowbox) getActivity()).subredditPosts.getPosts().get(i).getSubmission();
        } else {
            requireActivity().finish()
        }
        contentUrl = bundle.getString("contentUrl")
        client = App.client
        gson = Gson()
        mashapeKey = SecretConstants.getImgurApiKey(context)
    }

    fun doLoad(contentUrl: String?, type: ContentType.Type?) {
        when (type) {
            ContentType.Type.DEVIANTART -> doLoadDeviantArt(contentUrl)
            ContentType.Type.IMAGE, ContentType.Type.LINK, ContentType.Type.REDDIT -> doLoadImage(
                contentUrl
            )

            ContentType.Type.IMGUR -> doLoadImgur(contentUrl)
            ContentType.Type.XKCD -> doLoadXKCD(contentUrl)
            ContentType.Type.STREAMABLE, ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.VREDDIT_DIRECT, ContentType.Type.GIF -> doLoadGif(
                s
            )

            else -> {}
        }
    }

    fun doLoadGif(s: Submission?) {
        isGif = true
        videoView = rootView!!.findViewById(R.id.gif)
        videoView!!.clearFocus()
        rootView!!.findViewById<View>(R.id.gifarea).visibility = View.VISIBLE
        rootView!!.findViewById<View>(R.id.submission_image).visibility = View.GONE
        val loader = rootView!!.findViewById<ProgressBar>(R.id.gifprogress)
        gif = AsyncLoadGif(
            requireActivity(),
            videoView!!, loader,
            rootView!!.findViewById(R.id.placeholder), false, (
                    activity !is Shadowbox
                            || ((activity) as Shadowbox?)!!.pager!!.currentItem == i), sub!!
        )
        val t = AsyncLoadGif.getVideoType(s!!.url)
        val toLoadURL: String
        if (t == AsyncLoadGif.VideoType.VREDDIT) {
            if (s.dataNode.has("media") && s.dataNode["media"].has("reddit_video")) {
                toLoadURL = StringEscapeUtils.unescapeJson(
                    s.dataNode["media"]["reddit_video"]["dash_url"]
                        .asText()
                ).replace("&amp;", "&")
            } else if (s.dataNode.has("crosspost_parent_list")) {
                toLoadURL = StringEscapeUtils.unescapeJson(
                    s.dataNode["crosspost_parent_list"][0]["media"]["reddit_video"]["dash_url"]
                        .asText()
                ).replace("&amp;", "&")
            } else {
                //We shouldn't get here, will be caught in initializer
                return
            }
        } else if (((t.shouldLoadPreview() && s.dataNode.has("preview")
                    && s.dataNode["preview"]["images"][0].has("variants")
                    && s.dataNode["preview"]["images"][0]["variants"]
                .has("mp4")))
        ) {
            toLoadURL = StringEscapeUtils.unescapeJson(
                s.dataNode["preview"]["images"][0]["variants"]["mp4"]["source"]["url"]
                    .asText()
            ).replace("&amp;", "&")
        } else if (((t.shouldLoadPreview() && s.dataNode.has("preview")
                    && s.dataNode["preview"].has("reddit_video_preview")))
        ) {
            toLoadURL = StringEscapeUtils.unescapeJson(
                s.dataNode["preview"]["reddit_video_preview"]["dash_url"]
                    .asText()
            )
        } else if (((t == AsyncLoadGif.VideoType.DIRECT
                    ) && s.dataNode.has("media")
                    && s.dataNode["media"].has("reddit_video")
                    && s.dataNode["media"]["reddit_video"].has("fallback_url"))
        ) {
            toLoadURL = StringEscapeUtils.unescapeJson(
                s.dataNode["media"]["reddit_video"]["fallback_url"].asText()
            )
                .replace("&amp;", "&")
        } else if (t != AsyncLoadGif.VideoType.OTHER) {
            toLoadURL = s.url
        } else {
            doLoadImage(firstUrl)
            return
        }
        gif!!.execute(toLoadURL)
        rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
    }

    fun doLoadGifDirect(s: String?) {
        isGif = true
        videoView = rootView!!.findViewById(R.id.gif)
        videoView!!.clearFocus()
        rootView!!.findViewById<View>(R.id.gifarea).visibility = View.VISIBLE
        rootView!!.findViewById<View>(R.id.submission_image).visibility = View.GONE
        val loader = rootView!!.findViewById<ProgressBar>(R.id.gifprogress)
        gif = AsyncLoadGif(
            requireActivity(), videoView!!, loader,
            rootView!!.findViewById(R.id.placeholder), false, (
                    activity !is Shadowbox
                            || ((activity) as Shadowbox?)!!.pager!!.currentItem == i), sub!!
        )
        gif!!.execute(s)
        rootView!!.findViewById<View>(R.id.progress).visibility = View.GONE
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
                if ((result != null) && !result.isJsonNull && ((result.has("fullsize_url")
                            || result.has("url")))
                ) {
                    val url: String
                    if (result.has("fullsize_url")) {
                        url = result["fullsize_url"].asString
                    } else {
                        url = result["url"].asString
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
                    if ((result != null) && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                        (activity)!!.finish()
                    } else {
                        try {
                            if ((result != null) && !result.isJsonNull && result.has("image")) {
                                val type = result["image"]
                                    .asJsonObject["image"]
                                    .asJsonObject["type"]
                                    .asString
                                val urls = result["image"]
                                    .asJsonObject["links"]
                                    .asJsonObject["original"]
                                    .asString
                                if (type.contains("gif")) {
                                    doLoadGifDirect(urls)
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
                                    doLoadGifDirect((if (Strings.isNullOrEmpty(mp4)) urls else mp4))
                                } else if (!imageShown) { //only load if there is no image
                                    doLoadImage(urls)
                                }
                            } else {
                                if (!imageShown) doLoadImage(finalUrl)
                            }
                        } catch (e: Exception) {
                            LogUtil.e(
                                e, ("Error loading Imgur image finalUrl = ["
                                        + finalUrl
                                        + "], apiUrl = ["
                                        + apiUrl
                                        + "]")
                            )
                            if (context != null) {
                                val i = Intent(context, Website::class.java)
                                i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                                context!!.startActivity(i)
                            }
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
                    if ((result != null) && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                    } else {
                        try {
                            if ((result != null) && !result.isJsonNull && result.has("img")) {
                                doLoadImage(result["img"].asString)
                                rootView!!.findViewById<View>(R.id.submission_image)
                                    .setOnLongClickListener {
                                        try {
                                            AlertDialog.Builder((context)!!)
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
        contentUrl = StringEscapeUtils.unescapeHtml4(contentUrl)
        if ((((contentUrl != null
                    ) && !contentUrl.startsWith("https://i.redditmedia.com")
                    && !contentUrl.startsWith("https://i.reddituploads.com")
                    && !contentUrl.contains(
                "imgur.com"
            )))
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
                                if ((!imageShown
                                            && !Strings.isNullOrEmpty(type)
                                            && type.startsWith("image/"))
                                ) {
                                    //is image
                                    if (type.contains("gif")) {
                                        doLoadGifDirect(
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

    fun displayImage(urlB: String?) {
        val url = StringEscapeUtils.unescapeHtml4(urlB)
        if (!imageShown) {
            actuallyLoaded = url
            val i = rootView!!.findViewById<SubsamplingScaleImageView>(R.id.submission_image)
            i.setMinimumDpi(70)
            i.setMinimumTileDpi(240)
            val bar = rootView!!.findViewById<ProgressBar>(R.id.progress)
            bar.isIndeterminate = false
            LogUtil.v("Displaying image $url")
            bar.progress = 0
            val handler = Handler()
            val progressBarDelayRunner: Runnable = object : Runnable {
                override fun run() {
                    bar.visibility = View.VISIBLE
                }
            }
            handler.postDelayed(progressBarDelayRunner, 500)
            val fakeImage = ImageView(activity)
            fakeImage.layoutParams = LinearLayout.LayoutParams(i.width, i.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            val f = (requireActivity().applicationContext as App).imageLoader!!.diskCache[url]
            if (f != null && f.exists()) {
                imageShown = true
                try {
                    i.setImage(ImageSource.Uri(f.absolutePath))
                } catch (e: Exception) {
                    //todo  i.setImage(ImageSource.bitmap(loadedImage));
                }
                (rootView!!.findViewById<View>(R.id.progress)).visibility = View.GONE
                handler.removeCallbacks(progressBarDelayRunner)
                previous = i.scale
                val base = i.scale
                i.onStateChangedListener = object : DefaultOnStateChangedListener {
                    override fun onScaleChanged(
                        subsamplingScaleImageView: SubsamplingScaleImageView,
                        v: Float,
                        i: Int
                    ) {
                    }

                    override fun onCenterChanged(
                        subsamplingScaleImageView: SubsamplingScaleImageView,
                        pointF: PointF,
                        i: Int
                    ) {
                    }

                    override fun onCenterChanged(pointF: PointF, i: Int) {}
                    override fun onScaleChanged(newScale: Float, origin: Int) {
                        if ((newScale > previous) && !hidden && (newScale > base)) {
                            hidden = true
                            val base = rootView!!.findViewById<View>(R.id.base)
                            val va = ValueAnimator.ofFloat(1.0f, 0.2f)
                            val mDuration = 250 //in millis
                            va.duration = mDuration.toLong()
                            va.addUpdateListener { animation: ValueAnimator ->
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
                            va.addUpdateListener { animation: ValueAnimator ->
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
                    .displayImage(url,
                        ImageViewAware(fakeImage),
                        DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                            .cacheOnDisk(true)
                            .imageScaleType(ImageScaleType.NONE)
                            .cacheInMemory(false)
                            .build(),
                        object : ImageLoadingListener {
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
                                        .imageLoader!!.diskCache[url]
                                }
                                if (f != null && f.exists()) {
                                    i.setImage(ImageSource.Uri(f.absolutePath))
                                } else {
                                    i.setImage(ImageSource.Bitmap(loadedImage))
                                }
                                (rootView!!.findViewById<View>(R.id.progress)).visibility =
                                    View.GONE
                                handler.removeCallbacks(progressBarDelayRunner)
                                previous = i.scale
                                val base = i.scale
                                i.onStateChangedListener = object : DefaultOnStateChangedListener {
                                    override fun onScaleChanged(
                                        subsamplingScaleImageView: SubsamplingScaleImageView,
                                        v: Float,
                                        i: Int
                                    ) {
                                    }

                                    override fun onCenterChanged(
                                        subsamplingScaleImageView: SubsamplingScaleImageView,
                                        pointF: PointF,
                                        i: Int
                                    ) {
                                    }

                                    override fun onCenterChanged(pointF: PointF, i: Int) {}
                                    override fun onScaleChanged(newScale: Float, origin: Int) {
                                        if (((newScale > previous
                                                    ) && !hidden
                                                    && (newScale > base))
                                        ) {
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
                                            va.addUpdateListener { animation: ValueAnimator ->
                                                val value = animation.animatedValue as Float
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
                                            va.addUpdateListener { animation: ValueAnimator ->
                                                val value = animation.animatedValue as Float
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
                        },
                        ImageLoadingProgressListener { imageUri: String?, view: View?, current: Int, total: Int ->
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
            submission: Submission?
        ) {
            base.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (slidingPanel.panelState == PanelState.EXPANDED) {
                        slidingPanel.panelState = PanelState.COLLAPSED
                    } else {
                        when (type) {
                            ContentType.Type.STREAMABLE -> {
                                if (SettingValues.video) {
                                    val myIntent = Intent(contextActivity, MediaView::class.java)
                                    myIntent.putExtra(MediaView.EXTRA_URL, submission!!.url)
                                    myIntent.putExtra(
                                        MediaView.SUBREDDIT,
                                        submission.subredditName
                                    )
                                    contextActivity!!.startActivity(myIntent)
                                } else {
                                    openExternally(submission!!.url)
                                }
                                if (SettingValues.video) {
                                    openExternally(submission.url)
                                    val data = submission.dataNode["media_embed"]["content"]
                                        .asText()
                                    run {
                                        val i = Intent(contextActivity, FullscreenVideo::class.java)
                                        i.putExtra(FullscreenVideo.EXTRA_HTML, data)
                                        contextActivity!!.startActivity(i)
                                    }
                                } else {
                                    openExternally(submission.url)
                                }
                            }

                            ContentType.Type.EMBEDDED -> if (SettingValues.video) {
                                openExternally(submission!!.url)
                                val data = submission.dataNode["media_embed"]["content"]
                                    .asText()
                                run {
                                    val i = Intent(contextActivity, FullscreenVideo::class.java)
                                    i.putExtra(FullscreenVideo.EXTRA_HTML, data)
                                    contextActivity!!.startActivity(i)
                                }
                            } else {
                                openExternally(submission!!.url)
                            }

                            ContentType.Type.REDDIT -> openRedditContent(
                                submission!!.url,
                                contextActivity
                            )

                            ContentType.Type.LINK -> openUrl(
                                submission!!.url,
                                Palette.getColor(submission.subredditName),
                                (contextActivity)!!
                            )

                            ContentType.Type.SELF, ContentType.Type.NONE -> {}
                            ContentType.Type.ALBUM -> if (SettingValues.album) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(contextActivity, AlbumPager::class.java)
                                    i.putExtra(Album.EXTRA_URL, submission!!.url)
                                    i.putExtra(AlbumPager.SUBREDDIT, submission.subredditName)
                                } else {
                                    i = Intent(contextActivity, Album::class.java)
                                    i.putExtra(Album.EXTRA_URL, submission!!.url)
                                    i.putExtra(Album.SUBREDDIT, submission.subredditName)
                                }
                                i.putExtra(
                                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                    submission.title
                                )
                                contextActivity!!.startActivity(i)
                            } else {
                                openExternally(submission!!.url)
                            }

                            ContentType.Type.REDDIT_GALLERY -> if (SettingValues.album) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(contextActivity, RedditGalleryPager::class.java)
                                    i.putExtra(
                                        AlbumPager.SUBREDDIT,
                                        submission!!.subredditName
                                    )
                                } else {
                                    i = Intent(contextActivity, RedditGallery::class.java)
                                    i.putExtra(
                                        Album.SUBREDDIT,
                                        submission!!.subredditName
                                    )
                                }
                                i.putExtra(
                                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                    submission.title
                                )
                                i.putExtra(
                                    RedditGallery.SUBREDDIT,
                                    submission.subredditName
                                )
                                val urls = ArrayList<GalleryImage>()
                                val dataNode = submission.dataNode
                                if (dataNode.has("gallery_data")) {
                                    JsonUtil.getGalleryData(dataNode, urls)
                                }
                                val urlsBundle = Bundle()
                                urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, urls)
                                LogUtil.v("Opening gallery with " + urls.size)
                                i.putExtras(urlsBundle)
                                contextActivity!!.startActivity(i)
                            } else {
                                openExternally(submission!!.url)
                            }

                            ContentType.Type.TUMBLR -> if (SettingValues.image) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(contextActivity, TumblrPager::class.java)
                                    i.putExtra(Album.EXTRA_URL, submission!!.url)
                                    i.putExtra(
                                        TumblrPager.SUBREDDIT,
                                        submission.subredditName
                                    )
                                } else {
                                    i = Intent(contextActivity, Tumblr::class.java)
                                    i.putExtra(Album.EXTRA_URL, submission!!.url)
                                    i.putExtra(Tumblr.SUBREDDIT, submission.subredditName)
                                }
                                contextActivity!!.startActivity(i)
                            } else {
                                openExternally(submission!!.url)
                            }

                            ContentType.Type.DEVIANTART, ContentType.Type.XKCD, ContentType.Type.IMAGE -> openImage(
                                type, (contextActivity)!!,
                                RedditSubmission((submission)!!), null, -1
                            )

                            ContentType.Type.GIF -> openGif(
                                (contextActivity)!!, RedditSubmission((submission)!!), -1
                            )

                            ContentType.Type.VIDEO -> if (!tryOpenWithVideoPlugin(
                                    submission!!.url
                                )
                            ) {
                                openUrl(
                                    submission.url, Palette.getStatusBarColor(),
                                    (contextActivity)!!
                                )
                            }

                            else -> {}
                        }
                    }
                }
            })
        }
    }
}
