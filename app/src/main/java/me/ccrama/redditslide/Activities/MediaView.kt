package me.ccrama.redditslide.Activities

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.cocosw.bottomsheet.BottomSheet
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import com.davemorrissey.labs.subscaleview.DefaultOnStateChangedListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.ContentType.Companion.displayImage
import ltd.ucode.slide.ContentType.Companion.getContentType
import ltd.ucode.slide.ContentType.Companion.isGif
import ltd.ucode.slide.ContentType.Companion.isImage
import ltd.ucode.slide.ContentType.Companion.isImgurHash
import ltd.ucode.slide.ContentType.Companion.isImgurImage
import ltd.ucode.slide.ContentType.Companion.isImgurLink
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.imageDownloadButton
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.SecretConstants
import me.ccrama.redditslide.SubmissionViews.OpenVRedditTask
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.FileUtil
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.ShareUtil
import me.ccrama.redditslide.views.ExoVideoView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.Arrays
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class MediaView : FullScreenActivity() {
    var subreddit: String? = null
    private var submissionTitle: String? = null
    private var index = 0
    var previous = 0f
    var hidden = false
    var imageShown = false
    var actuallyLoaded: String? = null
    var isGif = false
    private var mNotifyManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var stopPosition: Long = 0
    private var gif: AsyncLoadGif? = null
    private var contentUrl: String? = null
    private var videoView: ExoVideoView? = null
    private var gson: Gson? = null
    private var mashapeKey: String? = null

    override fun onResume() {
        super.onResume()
        if (videoView != null) {
            videoView!!.seekTo(stopPosition)
            videoView!!.play()
        }
    }

    fun showBottomSheetImage() {
        val attrs = intArrayOf(R.attr.tintColor)
        val ta = obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val external = resources.getDrawable(R.drawable.ic_open_in_browser)
        val share = resources.getDrawable(R.drawable.ic_share)
        val image = resources.getDrawable(R.drawable.ic_image)
        val save = resources.getDrawable(R.drawable.ic_download)
        val collection = resources.getDrawable(R.drawable.ic_folder)
        val file = resources.getDrawable(R.drawable.ic_save)
        val thread = resources.getDrawable(R.drawable.ic_forum)
        val drawableSet = Arrays.asList(
            external, share, image, save, collection, file, thread
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        contentUrl = contentUrl!!.replace("/DASHPlaylist.mpd", "")
        val b = BottomSheet.Builder(this).title(contentUrl)
        b.sheet(2, external, getString(R.string.open_externally))
        b.sheet(5, share, getString(R.string.submission_link_share))
        if (!isGif) b.sheet(3, image, getString(R.string.share_image))
        b.sheet(4, save, "Save " + if (isGif) "MP4" else "image")
        b.sheet(16, collection, "Save " + (if (isGif) "MP4" else "image") + " to")
        if (isGif
            && !contentUrl!!.contains(".mp4")
            && !contentUrl!!.contains("streamable.com")
            && !contentUrl!!.contains("gfycat.com")
            && !contentUrl!!.contains("redgifs.com")
            && !contentUrl!!.contains("v.redd.it")
        ) {
            var type = contentUrl!!.substring(contentUrl!!.lastIndexOf(".") + 1)
                .uppercase(Locale.getDefault())
            try {
                if (type == "GIFV" && URL(contentUrl).host == "i.imgur.com") {
                    type = "GIF"
                    contentUrl = contentUrl!!.replace(".gifv", ".gif")
                    //todo possibly share gifs  b.sheet(9, ic_share, "Share GIF");
                }
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
            b.sheet(6, file, getString(R.string.mediaview_save, type))
        }
        if (contentUrl!!.contains("v.redd.it")) {
            b.sheet(15, thread, "View video thread")
        }
        b.listener { dialog, which ->
            when (which) {
                2 -> {
                    openExternally(contentUrl!!)
                }

                3 -> {
                    ShareUtil.shareImage(actuallyLoaded, this@MediaView)
                }

                5 -> {
                    defaultShareText(
                        "",
                        StringEscapeUtils.unescapeHtml4(contentUrl),
                        this@MediaView
                    )
                }

                6 -> {
                    saveFile(contentUrl!!)
                }

                15 -> {
                    OpenVRedditTask(this@MediaView, subreddit).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, contentUrl
                    )
                }

                9 -> {
                    shareGif(contentUrl!!)
                }

                4 -> {
                    doImageSave()
                }

                16 -> {
                    doImageSaveForLocation()
                }
            }
        }
        b.show()
    }

    fun doImageSave() {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                showFirstDialog()
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog()
            } else {
                val i = Intent(this, ImageDownloadNotificationService::class.java)
                //always download the original file, or use the cached original if that is currently displayed
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                if (submissionTitle != null) i.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    submissionTitle
                )
                i.putExtra("index", index)
                startService(i)
            }
        } else {
            doOnClick!!.run()
        }
    }

    fun doImageSaveForLocation() {
        if (!isGif) {
            MaterialDialog(this@MediaView).show {
                folderChooser(this@MediaView,
                    initialDirectory = Environment.getExternalStorageDirectory(),
                    allowFolderCreation = true) { _, folder ->
                    onFolderSelection(folder, true)
                }
            }
        }
    }

    fun saveFile(baseUrl: String) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                    showFirstDialog()
                } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                    showErrorDialog()
                } else {
                    val f = File(
                        appRestart.getString("imagelocation", "") + File.separator + UUID
                            .randomUUID()
                            .toString() + baseUrl.substring(baseUrl.lastIndexOf("."))
                    )
                    mNotifyManager = ContextCompat.getSystemService(
                        this@MediaView,
                        NotificationManager::class.java
                    )
                    mBuilder = NotificationCompat.Builder(this@MediaView, App.CHANNEL_IMG)
                    mBuilder!!.setContentTitle(getString(R.string.mediaview_saving, baseUrl))
                        .setSmallIcon(R.drawable.ic_download)
                    try {
                        val url =
                            URL(baseUrl) //wont exist on server yet, just load the full version
                        val ucon = url.openConnection()
                        ucon.readTimeout = 5000
                        ucon.connectTimeout = 10000
                        val `is` = ucon.getInputStream()
                        val inStream = BufferedInputStream(`is`, 1024 * 5)
                        val length = ucon.contentLength
                        f.createNewFile()
                        val outStream = FileOutputStream(f)
                        val buff = ByteArray(5 * 1024)
                        var len: Int
                        var last = 0
                        while (inStream.read(buff).also { len = it } != -1) {
                            outStream.write(buff, 0, len)
                            val percent = Math.round(100.0f * f.length() / length)
                            if (percent > last) {
                                last = percent
                                mBuilder!!.setProgress(length, f.length().toInt(), false)
                                mNotifyManager!!.notify(1, mBuilder!!.build())
                            }
                        }
                        outStream.flush()
                        outStream.close()
                        inStream.close()
                        MediaScannerConnection.scanFile(
                            this@MediaView, arrayOf(f.absolutePath), null
                        ) { path, uri ->
                            val mediaScanIntent = FileUtil.getFileIntent(
                                f,
                                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE),
                                this@MediaView
                            )
                            this@MediaView.sendBroadcast(mediaScanIntent)
                            val shareIntent = Intent(Intent.ACTION_VIEW)
                            val contentIntent = PendingIntent.getActivity(
                                this@MediaView, 0,
                                shareIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT!!
                            )
                            val notif = NotificationCompat.Builder(
                                this@MediaView, App.CHANNEL_IMG
                            )
                                .setContentTitle(getString(R.string.gif_saved))
                                .setSmallIcon(R.drawable.ic_save)
                                .setContentIntent(contentIntent)
                                .build()
                            val mNotificationManager = ContextCompat.getSystemService(
                                this@MediaView,
                                NotificationManager::class.java
                            )
                            mNotificationManager?.notify(1, notif)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun shareGif(baseUrl: String) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                    showFirstDialog()
                } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                    showErrorDialog()
                } else {
                    val f = File(
                        appRestart.getString("imagelocation", "") + File.separator + UUID
                            .randomUUID()
                            .toString() + baseUrl.substring(baseUrl.lastIndexOf("."))
                    )
                    mNotifyManager = ContextCompat.getSystemService(
                        this@MediaView,
                        NotificationManager::class.java
                    )
                    mBuilder = NotificationCompat.Builder(this@MediaView, App.CHANNEL_IMG)
                    mBuilder!!.setContentTitle(getString(R.string.mediaview_saving, baseUrl))
                        .setSmallIcon(R.drawable.ic_download)
                    try {
                        val url =
                            URL(baseUrl) //wont exist on server yet, just load the full version
                        val ucon = url.openConnection()
                        ucon.readTimeout = 5000
                        ucon.connectTimeout = 10000
                        val `is` = ucon.getInputStream()
                        val inStream = BufferedInputStream(`is`, 1024 * 5)
                        val length = ucon.contentLength
                        f.createNewFile()
                        val outStream = FileOutputStream(f)
                        val buff = ByteArray(5 * 1024)
                        var len: Int
                        var last = 0
                        while (inStream.read(buff).also { len = it } != -1) {
                            outStream.write(buff, 0, len)
                            val percent = Math.round(100.0f * f.length() / length)
                            if (percent > last) {
                                last = percent
                                mBuilder!!.setProgress(length, f.length().toInt(), false)
                                mNotifyManager!!.notify(1, mBuilder!!.build())
                            }
                        }
                        outStream.flush()
                        outStream.close()
                        inStream.close()
                        MediaScannerConnection.scanFile(
                            this@MediaView, arrayOf(f.absolutePath), null
                        ) { path, uri ->
                            val mediaScanIntent = FileUtil.getFileIntent(
                                f,
                                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE),
                                this@MediaView
                            )
                            this@MediaView.sendBroadcast(mediaScanIntent)
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            startActivity(
                                Intent.createChooser(shareIntent, "Share GIF")
                            )
                            val mNotificationManager = ContextCompat.getSystemService(
                                this@MediaView,
                                NotificationManager::class.java
                            )
                            mNotificationManager?.cancel(1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return null
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onDestroy() {
        super.onDestroy()
        (findViewById<View>(R.id.submission_image) as SubsamplingScaleImageView).recycle()
        if (gif != null) {
            gif!!.cancel()
            gif!!.cancel(true)
        }
        doOnClick = null
        if (!didLoadGif && fileLoc != null && !fileLoc!!.isEmpty()) {
            File(fileLoc).delete()
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (videoView != null) {
            stopPosition = videoView!!.currentPosition
            videoView!!.pause()
            outState.putLong("position", stopPosition)
        }
    }

    /* Possible drag to exit implementation in the future
     @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if(event.getF) {
            // peekView.doScroll(event);

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) base.getLayoutParams();

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    params.topMargin = (int) -((origY - event.getY()));
                    if (event.getY() != origY) {
                        params.leftMargin = twelve *2;
                        params.rightMargin = twelve * 2;
                    } else {
                        params.leftMargin = 0;
                        params.rightMargin = 0;
                    }

                    if (event.getY() != (origY)) {
                        shouldClose = true;
                    } else if (event.getY() == (origY)) {
                        shouldClose = false;
                    }
                    base.setLayoutParams(params);
                    break;
                case MotionEvent.ACTION_DOWN:
                    origY = event.getY();
                    break;
            }
        }
            // we don't want to pass along the touch event or else it will just scroll under the PeekView}

        if (event.getAction() == MotionEvent.ACTION_UP && shouldClose) {
            finish();
            return false;
        }

        return super.dispatchTouchEvent(event);
    }
     */
    fun hideOnLongClick() {
        findViewById<View>(R.id.gifheader).setOnClickListener {
            if (findViewById<View>(R.id.gifheader).visibility == View.GONE) {
                AnimatorUtil.animateIn(findViewById(R.id.gifheader), 56)
                AnimatorUtil.fadeOut(findViewById(R.id.black))
                window.decorView.systemUiVisibility = 0
            } else {
                AnimatorUtil.animateOut(findViewById(R.id.gifheader))
                AnimatorUtil.fadeIn(findViewById(R.id.black))
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
            }
        }
        findViewById<View>(R.id.submission_image).setOnClickListener {
            if (findViewById<View>(R.id.gifheader).visibility == View.GONE) {
                AnimatorUtil.animateIn(findViewById(R.id.gifheader), 56)
                AnimatorUtil.fadeOut(findViewById(R.id.black))
                window.decorView.systemUiVisibility = 0
            } else {
                finish()
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideRedditSwipeAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(ColorPreferences(this).getDarkThemeSubreddit(""), true)
        gson = Gson()
        mashapeKey = SecretConstants.getImgurApiKey(this)
        if (savedInstanceState != null && savedInstanceState.containsKey("position")) {
            stopPosition = savedInstanceState.getLong("position")
        }
        doOnClick = Runnable { }
        setContentView(R.layout.activity_media)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val firstUrl = intent.extras!!
            .getString(EXTRA_DISPLAY_URL, "")
        contentUrl = intent.extras!!.getString(EXTRA_URL)
        if (contentUrl == null || contentUrl!!.isEmpty()) {
            finish()
            return
        }
        shareUrl = contentUrl
        if (contentUrl!!.contains("reddituploads.com")) {
            contentUrl = CompatUtil.fromHtml(contentUrl!!).toString()
        }
        if (contentUrl != null && shouldTruncate(contentUrl!!)) {
            contentUrl = contentUrl!!.substring(0, contentUrl!!.lastIndexOf("."))
        }
        actuallyLoaded = contentUrl
        if (intent.hasExtra(SUBMISSION_URL)) {
            val commentUrl = intent.extras!!.getInt(ADAPTER_POSITION)
            findViewById<View>(R.id.comments).setOnClickListener {
                finish()
                datachanged(commentUrl)
            }
        } else {
            findViewById<View>(R.id.comments).visibility = View.GONE
        }
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.extras!!.getString(SUBREDDIT)
        }
        if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
            submissionTitle =
                intent.extras!!.getString(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
        }
        index = intent.getIntExtra("index", -1)
        findViewById<View>(R.id.mute).visibility = View.GONE
        if (intent.hasExtra(EXTRA_LQ)) {
            val lqUrl = intent.getStringExtra(EXTRA_DISPLAY_URL)
            displayImage(lqUrl)
            findViewById<View>(R.id.hq).setOnClickListener {
                imageShown = false
                doLoad(contentUrl!!)
                findViewById<View>(R.id.hq).visibility = View.GONE
            }
        } else if (isImgurImage(contentUrl) && SettingValues.loadImageLq && (SettingValues.lowResAlways || !NetworkUtil.isConnectedWifi(
                this
            )) && SettingValues.lowResMobile
        ) {
            var url: String = contentUrl!!
            url = url.substring(
                0,
                url.lastIndexOf(".")
            ) + (if (SettingValues.lqLow) "m" else if (SettingValues.lqMid) "l" else "h") + url.substring(
                url.lastIndexOf(".")
            )
            displayImage(url)
            findViewById<View>(R.id.hq).setOnClickListener {
                imageShown = false
                doLoad(contentUrl!!)
                findViewById<View>(R.id.hq).visibility = View.GONE
            }
        } else {
            if (!firstUrl.isEmpty() && contentUrl != null && displayImage(
                    getContentType(contentUrl!!)
                )
            ) {
                (findViewById<View>(R.id.progress) as ProgressBar).isIndeterminate = true
                if (isImgurHash(firstUrl)) {
                    displayImage("$firstUrl.png")
                } else {
                    displayImage(firstUrl)
                }
            } else if (firstUrl.isEmpty()) {
                imageShown = false
                (findViewById<View>(R.id.progress) as ProgressBar).isIndeterminate = true
            }
            findViewById<View>(R.id.hq).visibility = View.GONE
            doLoad(contentUrl!!)
        }
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
        findViewById<View>(R.id.more).setOnClickListener { showBottomSheetImage() }
        findViewById<View>(R.id.save).setOnClickListener { doImageSave() }
        if (!imageDownloadButton) {
            findViewById<View>(R.id.save).visibility = View.INVISIBLE
        }
        hideOnLongClick()
    }

    fun doLoad(contentUrl: String) {
        when (getContentType(contentUrl)) {
            ContentType.Type.DEVIANTART -> doLoadDeviantArt(contentUrl)
            ContentType.Type.IMAGE -> doLoadImage(contentUrl)
            ContentType.Type.IMGUR -> doLoadImgur(contentUrl)
            ContentType.Type.XKCD -> doLoadXKCD(contentUrl)
            ContentType.Type.STREAMABLE, ContentType.Type.VREDDIT_DIRECT, ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.GIF -> doLoadGif(
                contentUrl
            )

            else -> {}
        }
    }

    fun doLoadGif(dat: String?) {
        isGif = true
        videoView = findViewById<View>(R.id.gif) as ExoVideoView
        findViewById<View>(R.id.black).setOnClickListener {
            if (findViewById<View>(R.id.gifheader).visibility == View.GONE) {
                AnimatorUtil.animateIn(findViewById(R.id.gifheader), 56)
                AnimatorUtil.fadeOut(findViewById(R.id.black))
            }
        }
        videoView!!.clearFocus()
        findViewById<View>(R.id.gifarea).visibility = View.VISIBLE
        findViewById<View>(R.id.submission_image).visibility = View.GONE
        val loader = findViewById<View>(R.id.gifprogress) as ProgressBar
        findViewById<View>(R.id.progress).visibility = View.GONE
        gif = AsyncLoadGif(
            this, videoView!!, loader,
            findViewById<View>(R.id.placeholder), doOnClick, true, true,
            findViewById<View>(R.id.size) as TextView, subreddit!!, submissionTitle
        )
        videoView!!.attachMuteButton(findViewById<View>(R.id.mute) as ImageView)
        videoView!!.attachHqButton(findViewById<View>(R.id.hq) as ImageView)
        gif!!.execute(dat)
        findViewById<View>(R.id.more).setOnClickListener { showBottomSheetImage() }
    }

    fun doLoadImgur(url: String) {
        var url = url
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        val finalUrl = url
        var hash = url.substring(url.lastIndexOf("/"))
        if (NetworkUtil.isConnected(this)) {
            if (hash.startsWith("/")) hash = hash.substring(1)
            val apiUrl = "https://imgur-apiv3.p.mashape.com/3/image/$hash.json"
            LogUtil.v(apiUrl)
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getImgurMashapeJsonObject(
                        App.client, gson, apiUrl,
                        mashapeKey
                    )
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                        finish()
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
                                    doLoadGif(if (mp4 == null || mp4.isEmpty()) urls else mp4)
                                } else if (!imageShown) { //only load if there is no image
                                    displayImage(urls)
                                }
                            } else {
                                if (!imageShown) doLoadImage(finalUrl)
                            }
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                            val i = Intent(this@MediaView, Website::class.java)
                            i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                            this@MediaView.startActivity(i)
                            finish()
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun doLoadXKCD(url: String) {
        var url = url
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        if (NetworkUtil.isConnected(this)) {
            val apiUrl = url + "info.0.json"
            LogUtil.v(apiUrl)
            val finalUrl = url
            object : AsyncTask<Void?, Void?, JsonObject?>() {
                override fun doInBackground(vararg params: Void?): JsonObject? {
                    return HttpUtil.getJsonObject(App.client, gson, apiUrl)
                }

                override fun onPostExecute(result: JsonObject?) {
                    if (result != null && !result.isJsonNull && result.has("error")) {
                        LogUtil.v("Error loading content")
                        finish()
                    } else {
                        try {
                            if (result != null && !result.isJsonNull && result.has("img")) {
                                doLoadImage(result["img"].asString)
                                findViewById<View>(R.id.submission_image).setOnLongClickListener {
                                    try {
                                        AlertDialog.Builder(this@MediaView)
                                            .setTitle(result["safe_title"].asString)
                                            .setMessage(result["alt"].asString)
                                            .show()
                                    } catch (ignored: Exception) {
                                    }
                                    true
                                }
                            } else {
                                val i = Intent(this@MediaView, Website::class.java)
                                i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                                this@MediaView.startActivity(i)
                                finish()
                            }
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                            val i = Intent(this@MediaView, Website::class.java)
                            i.putExtra(LinkUtil.EXTRA_URL, finalUrl)
                            this@MediaView.startActivity(i)
                            finish()
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun doLoadDeviantArt(url: String) {
        val apiUrl = "http://backend.deviantart.com/oembed?url=$url"
        LogUtil.v(apiUrl)
        object : AsyncTask<Void?, Void?, JsonObject?>() {
            override fun doInBackground(vararg params: Void?): JsonObject? {
                return HttpUtil.getJsonObject(App.client, gson, apiUrl)
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
                    val i = Intent(this@MediaView, Website::class.java)
                    i.putExtra(LinkUtil.EXTRA_URL, contentUrl)
                    this@MediaView.startActivity(i)
                    finish()
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doLoadImage(contentUrl: String?) {
        var contentUrl = contentUrl
        if (contentUrl != null && contentUrl.contains("bildgur.de")) {
            contentUrl = contentUrl.replace("b.bildgur.de", "i.imgur.com")
        }
        if (contentUrl != null && isImgurLink(contentUrl)) {
            contentUrl = "$contentUrl.png"
        }
        findViewById<View>(R.id.gifprogress).visibility = View.GONE
        if (contentUrl != null && contentUrl.contains("m.imgur.com")) {
            contentUrl = contentUrl.replace("m.imgur.com", "i.imgur.com")
        }
        if (contentUrl == null) {
            finish()
            //todo maybe something better
        }
        if (contentUrl != null && !contentUrl.startsWith("https://i.redditmedia.com")
            && !contentUrl.startsWith("https://i.reddituploads.com")
            && !contentUrl.contains(
                "imgur.com"
            )
        ) { //we can assume redditmedia and imgur links are to direct images and not websites
            findViewById<View>(R.id.progress).visibility = View.VISIBLE
            (findViewById<View>(R.id.progress) as ProgressBar).isIndeterminate = true
            val finalUrl2: String = contentUrl
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    try {
                        val obj = URL(finalUrl2)
                        val conn = obj.openConnection()
                        val type = conn.getHeaderField("Content-Type")
                        runOnUiThread {
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
                                val i = Intent(this@MediaView, Website::class.java)
                                i.putExtra(LinkUtil.EXTRA_URL, finalUrl2)
                                this@MediaView.startActivity(i)
                                finish()
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    findViewById<View>(R.id.progress).visibility = View.GONE
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else {
            displayImage(contentUrl)
        }
        actuallyLoaded = contentUrl
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            appRestart.edit().putBoolean("tutorialSwipe", true).apply()
        }
    }

    fun displayImage(urlB: String?) {
        LogUtil.v("Displaying $urlB")
        val url = StringEscapeUtils.unescapeHtml4(urlB)
        if (!imageShown) {
            actuallyLoaded = url
            val i = findViewById<View>(R.id.submission_image) as SubsamplingScaleImageView
            i.setMinimumDpi(70)
            i.setMinimumTileDpi(240)
            val bar = findViewById<View>(R.id.progress) as ProgressBar
            bar.isIndeterminate = false
            bar.progress = 0
            val handler = Handler()
            val progressBarDelayRunner = Runnable { bar.visibility = View.VISIBLE }
            handler.postDelayed(progressBarDelayRunner, 500)
            val fakeImage = ImageView(this@MediaView)
            fakeImage.layoutParams = LinearLayout.LayoutParams(i.width, i.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            val f = (applicationContext as App).imageLoader!!.diskCache[url]
            if (f != null && f.exists()) {
                imageShown = true
                i.setOnImageEventListener(object : DefaultOnImageEventListener {
                    override fun onImageLoadError(e: Throwable) {
                        imageShown = false
                        LogUtil.v("No image displayed")
                    }
                })
                try {
                    i.setImage(ImageSource.Uri(f.absolutePath))
                } catch (e: Exception) {
                    imageShown = false
                    //todo  i.setImage(ImageSource.bitmap(loadedImage));
                }
                findViewById<View>(R.id.progress).visibility = View.GONE
                handler.removeCallbacks(progressBarDelayRunner)
                previous = i.scale
                val base = i.scale
                i.postDelayed({
                    i.onStateChangedListener = object : DefaultOnStateChangedListener {
                        override fun onScaleChanged(newScale: Float, origin: Int) {
                            if (newScale > previous && !hidden && newScale > base) {
                                hidden = true
                                val base = findViewById<View>(R.id.gifheader)
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
                                val base = findViewById<View>(R.id.gifheader)
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
                }, 2000)
            } else {
                val size = findViewById<View>(R.id.size) as TextView
                (application as App).imageLoader!!
                    .displayImage(url, ImageViewAware(fakeImage),
                        DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                            .cacheOnDisk(true)
                            .imageScaleType(ImageScaleType.NONE)
                            .cacheInMemory(false)
                            .build(), object : ImageLoadingListener {
                            override fun onLoadingStarted(imageUri: String, view: View) {
                                imageShown = true
                                size.visibility = View.VISIBLE
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
                                size.visibility = View.GONE
                                val f = (applicationContext as App).imageLoader!!
                                    .diskCache[url]
                                if (f != null && f.exists()) {
                                    i.setImage(ImageSource.Uri(f.absolutePath))
                                } else {
                                    i.setImage(ImageSource.Bitmap(loadedImage))
                                }
                                findViewById<View>(R.id.progress).visibility = View.GONE
                                handler.removeCallbacks(progressBarDelayRunner)
                                previous = i.scale
                                val base = i.scale
                                i.onStateChangedListener = object : DefaultOnStateChangedListener {
                                    override fun onScaleChanged(newScale: Float, origin: Int) {
                                        if (newScale > previous && !hidden && newScale > base) {
                                            hidden = true
                                            val base = findViewById<View>(R.id.gifheader)
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
                                            val base = findViewById<View>(R.id.gifheader)
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
                            size.text = FileUtil.readableFileSize(total.toLong())
                            (findViewById<View>(R.id.progress) as ProgressBar).progress =
                                (100.0f * current / total).roundToInt()
                        })
            }
        }
    }

    private fun showFirstDialog() {
        runOnUiThread { DialogUtil.showFirstDialog(this@MediaView) { _, folder -> onFolderSelection(folder) } }
    }

    private fun showErrorDialog() {
        runOnUiThread { DialogUtil.showErrorDialog(this@MediaView) { _, folder -> onFolderSelection(folder) } }
    }

    fun onFolderSelection(folder: File, isSaveToLocation: Boolean = false) {
        if (isSaveToLocation) {
            val i = Intent(this, ImageDownloadNotificationService::class.java)
            //always download the original file, or use the cached original if that is currently displayed
            i.putExtra("actuallyLoaded", contentUrl)
            i.putExtra("saveToLocation", folder.absolutePath)
            if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
            if (submissionTitle != null) i.putExtra(
                ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                submissionTitle
            )
            i.putExtra("index", index)
            startService(i)
        } else {
            appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
            Toast.makeText(
                this,
                getString(R.string.settings_set_image_location, folder.absolutePath),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    companion object {
        const val EXTRA_URL = "url"
        const val SUBREDDIT = "sub"
        const val ADAPTER_POSITION = "adapter_position"
        const val SUBMISSION_URL = "submission"
        const val EXTRA_DISPLAY_URL = "displayUrl"
        const val EXTRA_LQ = "lq"
        const val EXTRA_SHARE_URL = "urlShare"
        var fileLoc: String? = null
        @JvmField
        var doOnClick: Runnable? = null
        @JvmField
        var didLoadGif = false

        private fun shouldTruncate(url: String): Boolean {
            return try {
                val uri = URI(url)
                val path = uri.path
                !isGif(uri) && !isImage(uri) && path.contains(".")
            } catch (e: URISyntaxException) {
                false
            }
        }
    }
}
