package me.ccrama.redditslide.views

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
import ltd.ucode.slide.App
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.ContentType.Companion.getContentType
import ltd.ucode.slide.ContentType.Companion.isImgurLink
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.Adapters.ImageGridAdapter
import me.ccrama.redditslide.ForceTouch.PeekViewActivity
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils.GetAlbumWithCallback
import me.ccrama.redditslide.ImgurAlbum.Image
import me.ccrama.redditslide.SecretConstants
import me.ccrama.redditslide.Tumblr.Photo
import me.ccrama.redditslide.Tumblr.TumblrUtils.GetTumblrPostWithCallback
import me.ccrama.redditslide.util.AdBlocker
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import org.apache.commons.text.StringEscapeUtils
import java.io.IOException
import java.net.URL
import kotlin.math.abs
import kotlin.math.roundToInt

class PeekMediaView : RelativeLayout {
    var contentType: ContentType.Type? = null
    private var gif: AsyncLoadGif? = null
    private var videoView: ExoVideoView? = null
    var website: WebView? = null
    private var progress: ProgressBar? = null
    private var image: SubsamplingScaleImageView? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    var web = false
    var origY = 0f
    fun doClose() {
        website!!.visibility = GONE
        website!!.loadUrl("about:blank")
        videoView!!.stop()
        if (gif != null) gif!!.cancel(true)
    }

    fun doScroll(event: MotionEvent) {
        if (origY == 0f) {
            origY = event.y
        }
        if ((web && website!!.canScrollVertically(if (origY - event.y > 0) 0 else 1))
            && abs(origY - event.y) > website!!.height / 4.0f
        ) {
            website!!.scrollBy(0, -(origY - event.y).toInt() / 5)
        }
    }

    fun setUrl(url: String) {
        contentType = getContentType(url)
        when (contentType) {
            ContentType.Type.ALBUM -> {
                doLoadAlbum(url)
                progress!!.isIndeterminate = true
            }

            ContentType.Type.TUMBLR -> {
                doLoadTumblr(url)
                progress!!.isIndeterminate = true
            }

            ContentType.Type.EMBEDDED, ContentType.Type.EXTERNAL, ContentType.Type.LINK, ContentType.Type.VIDEO, ContentType.Type.SELF, ContentType.Type.REDDIT_GALLERY, ContentType.Type.SPOILER, ContentType.Type.NONE -> {
                doLoadLink(url)
                progress!!.isIndeterminate = false
            }

            ContentType.Type.REDDIT -> {
                progress!!.isIndeterminate = true
                doLoadReddit(url)
            }

            ContentType.Type.DEVIANTART -> {
                doLoadDeviantArt(url)
                progress!!.isIndeterminate = false
            }

            ContentType.Type.IMAGE -> {
                doLoadImage(url)
                progress!!.isIndeterminate = false
            }

            ContentType.Type.XKCD -> {
                doLoadXKCD(url)
                progress!!.isIndeterminate = false
            }

            ContentType.Type.IMGUR -> {
                doLoadImgur(url)
                progress!!.isIndeterminate = false
            }

            ContentType.Type.GIF, ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.VREDDIT_DIRECT, ContentType.Type.STREAMABLE -> {
                doLoadGif(url)
                progress!!.isIndeterminate = false
            }

            else -> {}
        }
    }

    private fun doLoadAlbum(url: String) {
        object : GetAlbumWithCallback(url, (context as PeekViewActivity)) {
            override fun onError() {
                (context as PeekViewActivity).runOnUiThread { doLoadLink(url) }
            }

            override fun doWithData(jsonElements: List<Image>) {
                super.doWithData(jsonElements)
                progress!!.visibility = GONE
                images = ArrayList(jsonElements)
                displayImage(images!!.get(0).imageUrl)
                if (images!!.size > 1) {
                    val grid = findViewById<GridView>(R.id.grid_area)
                    grid.numColumns = 5
                    grid.visibility = VISIBLE
                    grid.adapter = ImageGridAdapter(context, images)
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun doLoadTumblr(url: String) {
        object : GetTumblrPostWithCallback(url, (context as PeekViewActivity)) {
            override fun onError() {
                (context as PeekViewActivity).runOnUiThread { doLoadLink(url) }
            }

            override fun doWithData(jsonElements: List<Photo>) {
                super.doWithData(jsonElements)
                progress!!.visibility = GONE
                tumblrImages = ArrayList(jsonElements)
                displayImage(tumblrImages!!.get(0).originalSize.url)
                if (tumblrImages!!.size > 1) {
                    val grid = findViewById<GridView>(R.id.grid_area)
                    grid.numColumns = 5
                    grid.visibility = VISIBLE
                    grid.adapter = ImageGridAdapter(context, tumblrImages, true)
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    var images: List<Image>? = null
    var tumblrImages: List<Photo>? = null
    var client: WebChromeClient? = null
    var webClient: WebViewClient? = null
    fun setValue(newProgress: Int) {
        progress!!.progress = newProgress
        if (newProgress == 100) {
            progress!!.visibility = GONE
        } else if (progress!!.visibility == GONE) {
            progress!!.visibility = VISIBLE
        }
    }

    private inner class MyWebViewClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            setValue(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    }

    fun doLoadXKCD(url: String) {
        if (NetworkUtil.isConnected(context)) {
            val apiUrl = (if (url.endsWith("/")) url else "$url/") + "info.0.json"
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getJsonObject(App.client, Gson(), apiUrl)
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        doLoadLink(url)
                    } else {
                        try {
                            if (result != null && !result.isJsonNull && result.has("img")) {
                                doLoadImage(result["img"].asString)
                            } else {
                                doLoadLink(url)
                            }
                        } catch (e2: Exception) {
                            doLoadLink(url)
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun doLoadLink(url: String?) {
        client = MyWebViewClient()
        web = true
        webClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                website!!.loadUrl(
                    "javascript:(function() { document.getElementsByTagName('video')[0].play(); })()"
                )
            }

            private val loadedUrls: MutableMap<String, Boolean> = HashMap()
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                val ad: Boolean
                if (!loadedUrls.containsKey(url)) {
                    ad = AdBlocker.isAd(url, context)
                    loadedUrls[url] = ad
                } else {
                    ad = loadedUrls[url]!!
                }
                return if (ad && SettingValues.isPro) AdBlocker.createEmptyResource() else super.shouldInterceptRequest(
                    view,
                    url
                )
            }
        }
        website!!.visibility = VISIBLE
        website!!.webChromeClient = client
        website!!.webViewClient = webClient!!
        website!!.settings.builtInZoomControls = true
        website!!.settings.displayZoomControls = false
        website!!.settings.javaScriptEnabled = true
        website!!.settings.loadWithOverviewMode = true
        website!!.settings.useWideViewPort = true
        website!!.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength -> //Downloads using download manager on default browser
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            context.startActivity(i)
        }
        website!!.loadUrl(url!!)
    }

    private fun doLoadReddit(url: String) {
        val v = findViewById<RedditItemView>(R.id.reddit_item)
        v.loadUrl(this, url, progress)
    }

    fun doLoadDeviantArt(url: String) {
        val apiUrl = "http://backend.deviantart.com/oembed?url=$url"
        LogUtil.v(apiUrl)
        object : AsyncTask<Void?, Void?, JsonObject?>() {
            override fun doInBackground(vararg params: Void?): JsonObject? {
                return HttpUtil.getJsonObject(App.client, Gson(), apiUrl)
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
                    //todo error out
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doLoadImgur(url: String) {
        var url = url
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        val finalUrl = url
        var hash = url.substring(url.lastIndexOf("/"))
        if (NetworkUtil.isConnected(context)) {
            if (hash.startsWith("/")) hash = hash.substring(1)
            val apiUrl = "https://imgur-apiv3.p.mashape.com/3/image/$hash.json"
            LogUtil.v(apiUrl)
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getImgurMashapeJsonObject(
                        App.client, Gson(), apiUrl,
                        SecretConstants.getImgurApiKey(context)
                    )
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        ///todo error out
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
                                    displayImage(urls)
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
                                    displayImage(urls)
                                }
                            } else {
                                if (!imageShown) doLoadImage(finalUrl)
                            }
                        } catch (e2: Exception) {
                            //todo error out
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    var imageShown = false
    fun doLoadImage(contentUrl: String?) {
        var contentUrl = contentUrl
        if (contentUrl != null && contentUrl.contains("bildgur.de")) {
            contentUrl = contentUrl.replace("b.bildgur.de", "i.imgur.com")
        }
        if (contentUrl != null && isImgurLink(contentUrl)) {
            contentUrl = "$contentUrl.png"
        }
        if (contentUrl != null && contentUrl.contains("m.imgur.com")) {
            contentUrl = contentUrl.replace("m.imgur.com", "i.imgur.com")
        }
        if (contentUrl == null) {
            //todo error out
        }
        if (contentUrl != null && !contentUrl.startsWith("https://i.redditmedia.com")
            && !contentUrl.startsWith("https://i.reddituploads.com")
            && !contentUrl.contains(
                "imgur.com"
            )
        ) { //we can assume redditmedia and imgur links are to direct images and not websites
            progress!!.visibility = VISIBLE
            progress!!.isIndeterminate = true
            val finalUrl2: String = contentUrl
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    try {
                        val obj = URL(finalUrl2)
                        val conn = obj.openConnection()
                        val type = conn.getHeaderField("Content-Type")
                        (context as PeekViewActivity).runOnUiThread {
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
                                //todo error out
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    progress!!.visibility = GONE
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayImage(contentUrl)
        }
    }

    var actuallyLoaded: String? = null
    fun doLoadGif(dat: String?) {
        videoView = findViewById(R.id.gif)
        videoView!!.clearFocus()
        findViewById<View>(R.id.gifarea).visibility = VISIBLE
        findViewById<View>(R.id.submission_image).visibility = GONE
        progress!!.visibility = VISIBLE
        gif = object : AsyncLoadGif(
            (context as PeekViewActivity),
            videoView!!, progress, null, false, true, ""
        ) {
            override fun onError() {
                doLoadLink(dat)
            }
        }
        gif!!.execute(dat)
    }

    fun displayImage(urlB: String?) {
        LogUtil.v("Displaying $urlB")
        val url = StringEscapeUtils.unescapeHtml4(urlB)
        if (!imageShown) {
            actuallyLoaded = url
            val i = findViewById<SubsamplingScaleImageView>(R.id.submission_image)
            i.setMinimumDpi(70)
            i.setMinimumTileDpi(240)
            progress!!.isIndeterminate = false
            progress!!.progress = 0
            val handler = Handler()
            val progressBarDelayRunner = Runnable { progress!!.visibility = VISIBLE }
            handler.postDelayed(progressBarDelayRunner, 500)
            val fakeImage = ImageView(context)
            fakeImage.layoutParams = LinearLayout.LayoutParams(i.width, i.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            val f = (context.applicationContext as App).imageLoader!!
                .getDiskCache()[url]
            if (f != null && f.exists()) {
                imageShown = true
                i.setOnImageEventListener(object : DefaultOnImageEventListener {
                    fun onImageLoadError(e: Exception?) {
                        imageShown = false
                        LogUtil.v("No image displayed")
                    }
                })
                try {
                    i.setImage(ImageSource.Uri(f.absolutePath))
                    i.isZoomEnabled = false
                } catch (e: Exception) {
                    imageShown = false
                    //todo  i.setImage(ImageSource.bitmap(loadedImage));
                }
                progress!!.visibility = GONE
                handler.removeCallbacks(progressBarDelayRunner)
            } else {
                (context.applicationContext as App).imageLoader!!
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
                                imageShown = false
                            }

                            override fun onLoadingComplete(
                                imageUri: String, view: View,
                                loadedImage: Bitmap
                            ) {
                                imageShown = true
                                val f = (context.applicationContext as App).imageLoader!!
                                    .diskCache[url]
                                if (f != null && f.exists()) {
                                    i.setImage(ImageSource.Uri(f.absolutePath))
                                } else {
                                    i.setImage(ImageSource.Bitmap(loadedImage))
                                }
                                progress!!.visibility = GONE
                                handler.removeCallbacks(progressBarDelayRunner)
                            }

                            override fun onLoadingCancelled(imageUri: String, view: View) {
                                Log.v(LogUtil.getTag(), "LOADING CANCELLED")
                            }
                        },
                        { imageUri, view, current, total ->
                            progress!!.progress = (100.0f * current / total).roundToInt()
                        })
            }
        }
    }

    private fun init() {
        inflate(context, R.layout.peek_media_view, this)
        image = findViewById(R.id.submission_image)
        videoView = findViewById(R.id.gif)
        website = findViewById(R.id.website)
        progress = findViewById(R.id.progress)
    }
}
