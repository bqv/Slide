package me.ccrama.redditslide.util

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSourceInputStream
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.gson.Gson
import com.google.gson.JsonObject
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.imageSubfolders
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.views.ExoVideoView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.NullOutputStream
import org.mp4parser.muxer.Movie
import org.mp4parser.muxer.builder.DefaultMp4Builder
import org.mp4parser.muxer.container.mp4.MovieCreator
import org.mp4parser.muxer.tracks.ClippedTrack
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.util.Locale

/**
 * GIF handling utilities
 */
object GifUtils {
    /**
     * Create a notification that opens a newly-saved GIF
     *
     * @param f File referencing the GIF
     * @param c
     */
    fun doNotifGif(f: File?, c: Activity) {
        MediaScannerConnection.scanFile(
            c, arrayOf(f!!.absolutePath), null
        ) { path, uri ->
            val shareIntent = FileUtil.getFileIntent(f, Intent(Intent.ACTION_VIEW), c)
            val contentIntent =
                PendingIntent.getActivity(
                    c,
                    0,
                    shareIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            val notif = NotificationCompat.Builder(c, App.CHANNEL_IMG).setContentTitle(
                c.getString(
                    R.string.gif_saved
                )
            )
                .setSmallIcon(R.drawable.ic_save)
                .setContentIntent(contentIntent)
                .build()
            val mNotificationManager =
                ContextCompat.getSystemService(c, NotificationManager::class.java)
            mNotificationManager?.notify(System.currentTimeMillis().toInt(), notif)
        }
    }

    private fun showErrorDialog(a: Activity) {
        DialogUtil.showErrorDialog(a as MediaView) { _, folder -> a.onFolderSelection(folder) }
    }

    private fun showFirstDialog(a: Activity) {
        DialogUtil.showFirstDialog(a as MediaView) { _, folder -> a.onFolderSelection(folder) }
    }

    /**
     * Temporarily cache or permanently save a GIF
     *
     * @param uri       URL of the GIF
     * @param a
     * @param subreddit Subreddit for saving in sub-specific folders
     * @param save      Whether to permanently save the GIF of just temporarily cache it
     */
    fun cacheSaveGif(
        uri: Uri,
        a: Activity,
        subreddit: String,
        submissionTitle: String?,
        save: Boolean
    ) {
        if (save) {
            try {
                Toast.makeText(a, a.getString(R.string.mediaview_notif_title), Toast.LENGTH_SHORT)
                    .show()
            } catch (ignored: Exception) {
            }
        }
        if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
            showFirstDialog(a)
        } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
            showErrorDialog(a)
        } else {
            object : AsyncTask<Void?, Int?, Boolean>() {
                var outFile: File? = null
                var notifMgr = ContextCompat.getSystemService(a, NotificationManager::class.java)

                override fun onPreExecute() {
                    super.onPreExecute()
                    if (save) {
                        val notif = NotificationCompat.Builder(a, App.CHANNEL_IMG)
                            .setContentTitle(
                                a.getString(
                                    R.string.mediaview_saving,
                                    uri.toString().replace("/DASHPlaylist.mpd", "")
                                )
                            )
                            .setSmallIcon(R.drawable.ic_download)
                            .setProgress(0, 0, true)
                            .setOngoing(true)
                            .build()
                        notifMgr!!.notify(1, notif)
                    }
                }

                override fun doInBackground(vararg voids: Void?): Boolean {
                    val folderPath = appRestart.getString("imagelocation", "")
                    var subFolderPath = ""
                    if (imageSubfolders && !subreddit.isEmpty()) {
                        subFolderPath = File.separator + subreddit
                    }
                    val extension = ".mp4"
                    outFile = FileUtil.getValidFile(
                        folderPath,
                        subFolderPath,
                        submissionTitle,
                        "",
                        extension
                    )
                    var out: OutputStream? = null
                    var `in`: InputStream? = null
                    try {
                        val downloader: DataSource.Factory = OkHttpDataSource.Factory(
                            App.client!!
                        )
                            .setUserAgent(a.getString(R.string.app_name))
                        val cacheDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
                            .setCache(App.videoCache!!)
                            .setUpstreamDataSourceFactory(downloader)
                        if (uri.lastPathSegment!!.endsWith("DASHPlaylist.mpd")) {
                            val dashManifestStream: InputStream = DataSourceInputStream(
                                cacheDataSourceFactory.createDataSource(),
                                DataSpec(uri)
                            )
                            val dashManifest = DashManifestParser().parse(uri, dashManifestStream)
                            dashManifestStream.close()
                            var audioUri: Uri? = null
                            var videoUri: Uri? = null
                            for (i in 0 until dashManifest.periodCount) {
                                for (`as` in dashManifest.getPeriod(i).adaptationSets) {
                                    var isAudio = false
                                    var bitrate = 0
                                    var hqUri: String? = null
                                    for (r in `as`.representations) {
                                        if (r.format.bitrate > bitrate) {
                                            bitrate = r.format.bitrate
                                            hqUri = r.baseUrls[0].url
                                        }
                                        if (MimeTypes.isAudio(r.format.sampleMimeType)) {
                                            isAudio = true
                                        }
                                    }
                                    if (isAudio) {
                                        audioUri = Uri.parse(hqUri)
                                    } else {
                                        videoUri = Uri.parse(hqUri)
                                    }
                                }
                            }
                            if (audioUri != null) {
                                LogUtil.v("Downloading DASH audio from: $audioUri")
                                val audioInputStream = DataSourceInputStream(
                                    cacheDataSourceFactory.createDataSource(), DataSpec(audioUri)
                                )
                                if (save) {
                                    FileUtils.copyInputStreamToFile(
                                        audioInputStream,
                                        File(a.cacheDir.absolutePath, "audio.mp4")
                                    )
                                } else {
                                    IOUtils.copy(
                                        audioInputStream,
                                        NullOutputStream.NULL_OUTPUT_STREAM
                                    )
                                }
                                audioInputStream.close()
                            }
                            if (videoUri != null) {
                                LogUtil.v("Downloading DASH video from: $videoUri")
                                val videoInputStream = DataSourceInputStream(
                                    cacheDataSourceFactory.createDataSource(), DataSpec(videoUri)
                                )
                                if (save) {
                                    FileUtils.copyInputStreamToFile(
                                        videoInputStream,
                                        File(a.cacheDir.absolutePath, "video.mp4")
                                    )
                                } else {
                                    IOUtils.copy(
                                        videoInputStream,
                                        NullOutputStream.NULL_OUTPUT_STREAM
                                    )
                                }
                                videoInputStream.close()
                            }
                            `in` = if (!save) {
                                return true
                            } else if (audioUri != null && videoUri != null) {
                                if (mux(
                                        File(a.cacheDir.absolutePath, "video.mp4").absolutePath,
                                        File(a.cacheDir.absolutePath, "audio.mp4").absolutePath,
                                        File(a.cacheDir.absolutePath, "muxed.mp4").absolutePath
                                    )
                                ) {
                                    FileInputStream(File(a.cacheDir.absolutePath, "muxed.mp4"))
                                } else {
                                    throw IOException("Muxing failed!")
                                }
                            } else {
                                FileInputStream(File(a.cacheDir.absolutePath, "video.mp4"))
                            }
                        } else {
                            `in` = DataSourceInputStream(
                                cacheDataSourceFactory.createDataSource(),
                                DataSpec(uri)
                            )
                        }
                        out =
                            if (save) FileOutputStream(outFile) else NullOutputStream.NULL_OUTPUT_STREAM
                        IOUtils.copy(`in`, out)
                        out!!.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LogUtil.e(
                            "Error saving GIF called with: "
                                    + "from = ["
                                    + uri
                                    + "], in = ["
                                    + `in`
                                    + "]"
                        )
                        return false
                    } finally {
                        try {
                            out?.close()
                            `in`?.close()
                        } catch (e: IOException) {
                            LogUtil.e(
                                "Error closing GIF called with: "
                                        + "from = ["
                                        + uri
                                        + "], out = ["
                                        + out
                                        + "]"
                            )
                            return false
                        }
                    }
                    return true
                }

                override fun onPostExecute(success: Boolean) {
                    super.onPostExecute(success)
                    if (save) {
                        notifMgr!!.cancel(1)
                        if (success) {
                            doNotifGif(outFile, a)
                        } else {
                            showErrorDialog(a)
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    /**
     * Shows a ProgressBar in the UI. If this method is called from a non-main thread, it will run
     * the UI code on the main thread
     *
     * @param activity        The activity context to use to display the ProgressBar
     * @param progressBar     The ProgressBar to display
     * @param isIndeterminate True to show an indeterminate ProgressBar, false otherwise
     */
    private fun showProgressBar(
        activity: Activity?, progressBar: ProgressBar?,
        isIndeterminate: Boolean
    ) {
        if (activity == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Current Thread is Main Thread.
            if (progressBar != null) progressBar.isIndeterminate = isIndeterminate
        } else {
            activity.runOnUiThread(Runnable {
                if (progressBar != null) progressBar.isIndeterminate = isIndeterminate
            })
        }
    }

    /**
     * Mux a video and audio file (e.g. from DASH) together into a single video
     *
     * @param videoFile  Video file
     * @param audioFile  Audio file
     * @param outputFile File to output muxed video to
     * @return Whether the muxing completed successfully
     */
    private fun mux(videoFile: String, audioFile: String, outputFile: String): Boolean {
        val rawVideo: Movie
        rawVideo = try {
            MovieCreator.build(videoFile)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        val audio: Movie = try {
            MovieCreator.build(audioFile)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return false
        }
        val audioTrack = audio.tracks[0]
        val videoTrack = rawVideo.tracks[0]
        val video = Movie()
        val croppedTrackAudio = ClippedTrack(audioTrack, 0, audioTrack.samples.size.toLong())
        video.addTrack(croppedTrackAudio)
        val croppedTrackVideo = ClippedTrack(videoTrack, 0, videoTrack.samples.size.toLong())
        video.addTrack(croppedTrackVideo)
        val out = DefaultMp4Builder().build(video)
        val fos: FileOutputStream = try {
            FileOutputStream(outputFile)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }
        val byteBufferByteChannel = BufferedWritableFileByteChannel(fos)
        try {
            out.writeContainer(byteBufferByteChannel)
            byteBufferByteChannel.close()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    open class AsyncLoadGif : AsyncTask<String?, Void?, Uri?> {
        private var c: Activity
        private var video: ExoVideoView
        private var progressBar: ProgressBar?
        private var placeholder: View?
        private val gifSave: View? = null
        private var closeIfNull: Boolean
        private var doOnClick: Runnable? = null
        private var autostart: Boolean
        var subreddit: String
        var submissionTitle: String? = null
        private var size: TextView? = null

        constructor(
            c: Activity, video: ExoVideoView,
            p: ProgressBar?, placeholder: View?, gifSave: Runnable?,
            closeIfNull: Boolean, autostart: Boolean, subreddit: String
        ) {
            this.c = c
            this.subreddit = subreddit
            this.video = video
            progressBar = p
            this.closeIfNull = closeIfNull
            this.placeholder = placeholder
            doOnClick = gifSave
            this.autostart = autostart
        }

        constructor(
            c: Activity,
            video: ExoVideoView,
            p: ProgressBar?,
            placeholder: View?,
            gifSave: Runnable?,
            closeIfNull: Boolean,
            autostart: Boolean,
            size: TextView?,
            subreddit: String,
            submissionTitle: String?
        ) {
            this.c = c
            this.video = video
            this.subreddit = subreddit
            progressBar = p
            this.closeIfNull = closeIfNull
            this.placeholder = placeholder
            doOnClick = gifSave
            this.autostart = autostart
            this.size = size
            this.submissionTitle = submissionTitle
        }

        open fun onError() {}

        constructor(
            c: Activity, video: ExoVideoView,
            p: ProgressBar?, placeholder: View?, closeIfNull: Boolean,
            autostart: Boolean, subreddit: String
        ) {
            this.c = c
            this.video = video
            this.subreddit = subreddit
            progressBar = p
            this.closeIfNull = closeIfNull
            this.placeholder = placeholder
            this.autostart = autostart
        }

        fun cancel() {
            LogUtil.v("cancelling")
            video.stop()
        }

        override fun onPreExecute() {
            super.onPreExecute()
            gson = Gson()
        }

        var gson: Gson? = null

        enum class VideoType {
            IMGUR, STREAMABLE, GFYCAT, DIRECT, OTHER, VREDDIT;

            fun shouldLoadPreview(): Boolean {
                return this == OTHER
            }
        }

        /**
         * Get an API response for a given host and gfy name
         *
         * @param host the host to send the req to
         * @param name the name of the gfy
         * @return the result
         */
        fun getApiResponse(host: String, name: String): JsonObject {
            val domain = "api.$host.com"
            val gfycatUrl = "https://$domain/v1/gfycats$name"
            return HttpUtil.getJsonObject(client, gson, gfycatUrl, makeHeaderMap(domain))
        }

        /**
         * Get the correct mp4/mobile url from a given result JsonObject
         *
         * @param result the result to check
         * @return the video url
         */
        fun getUrlFromApi(result: JsonObject): String {
            return if (!SettingValues.hqgif && result.getAsJsonObject("gfyItem").has("mobileUrl")) {
                result.getAsJsonObject("gfyItem")["mobileUrl"].asString
            } else {
                result.getAsJsonObject("gfyItem")["mp4Url"].asString
            }
        }

        var client = App.client

        /**
         * Load the correct URL for a gfycat gif
         *
         * @param name    Name of the gfycat gif
         * @param fullUrl full URL to the gfycat
         * @param gson
         * @return Correct URL
         */
        fun loadGfycat(name: String, fullUrl: String, gson: Gson?): Uri? {
            var name = name
            showProgressBar(c, progressBar, true)
            var host = "gfycat"
            if (fullUrl.contains("redgifs")) {
                host = "redgifs"
            }
            if (!name.startsWith("/")) name = "/$name"
            if (name.contains("-")) {
                name = name.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            }
            val result = getApiResponse(host, name)
            if (result == null || result["gfyItem"] == null || result.getAsJsonObject("gfyItem")["mp4Url"]
                    .isJsonNull
            ) {
                //If the result null, the gfycat link may be redirecting to gifdeliverynetwork which is powered by redgifs.
                //Try getting the redirected url from gfycat and check if redirected url is gifdeliverynetwork and if it is,
                // we fetch the actual .mp4/.webm url from the redgifs api
                if (result == null) {
                    try {
                        val newUrl = URL(fullUrl)
                        val ucon = newUrl.openConnection() as HttpURLConnection
                        ucon.instanceFollowRedirects = false
                        val secondURL = URL(ucon.getHeaderField("location")).toString()
                        if (secondURL.contains("gifdeliverynetwork")) {
                            return Uri.parse(
                                getUrlFromApi(
                                    getApiResponse(
                                        "redgifs", name.lowercase(
                                            Locale.getDefault()
                                        )
                                    )
                                )
                            )
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                onError()
                if (closeIfNull) {
                    c.runOnUiThread {
                        try {
                            AlertDialog.Builder(c)
                                .setTitle(R.string.gif_err_title)
                                .setMessage(R.string.gif_err_msg)
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> c.finish() }
                                .setNeutralButton(R.string.open_externally) { dialog: DialogInterface?, which: Int ->
                                    openExternally(fullUrl)
                                    c.finish()
                                }
                                .create()
                                .show()
                        } catch (ignored: Exception) {
                        }
                    }
                }
                return null
            }
            return Uri.parse(getUrlFromApi(result))
        }

        /*Loads a direct MP4, used for DASH mp4 or direct/imgur videos, currently unused
        private void loadDirect(String url) {
            try {
                writeGif(new URL(url), progressBar, c, subreddit);
            } catch (Exception e) {
                LogUtil.e(e,
                        "Error loading URL " + url); //Most likely is an image, not a gif!
                if (c instanceof MediaView && url.contains("imgur.com") && url.endsWith(
                        ".mp4")) {
                    c.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            (c).startActivity(new Intent(c, MediaView.class).putExtra(
                                    MediaView.EXTRA_URL, url.replace(".mp4",
                                            ".png"))); //Link is likely an image and not a gif
                            (c).finish();
                        }
                    });
                } else {
                    if (closeIfNull) {
                        Intent web = new Intent(c, Website.class);
                        web.putExtra(LinkUtil.EXTRA_URL, url);
                        web.putExtra(LinkUtil.EXTRA_COLOR, Color.BLACK);
                        c.startActivity(web);
                        c.finish();
                    }
                }
            }
        }*/
        //Handles failures of loading a DASH mp4 or muxing a Reddit video
        private fun catchVRedditFailure(e: Exception, url: String) {
            LogUtil.e(
                e,
                "Error loading URL $url"
            ) //Most likely is an image, not a gif!
            if (c is MediaView && url.contains("imgur.com") && url.endsWith(
                    ".mp4"
                )
            ) {
                c.runOnUiThread {
                    c.startActivity(
                        Intent(c, MediaView::class.java).putExtra(
                            MediaView.EXTRA_URL, url.replace(
                                ".mp4",
                                ".png"
                            )
                        )
                    ) //Link is likely an image and not a gif
                    c.finish()
                }
            } else {
                openWebsite(url)
            }
        }

        override fun doInBackground(vararg sub: String?): Uri? {
            MediaView.didLoadGif = false
            val gson = Gson()
            val url = formatUrl(sub[0]!!)
            val videoType = getVideoType(url)
            LogUtil.v("$url, VideoType: $videoType")
            if (size != null) {
                getRemoteFileSize(url, client, size!!, c)
            }
            when (videoType) {
                VideoType.VREDDIT ->                     /* We may not need this after all, but keeping the code here in case we run into more DASH issues. This is implemented in the iOS app
                    try {
                        //If it's an HLSPlaylist, there is a good chance we can find a DASH mp4 url
                        if (url.contains("HLSPlaylist")) {
                            //Test these qualities
                            getQualityURL(url, new String[]{"1080", "720", "480", "360", "240", "96"},
                                    (didFindVideo, videoUrl) -> {
                                        if (didFindVideo) {
                                            //Load the MP4 directly
                                            loadDirect(videoUrl);
                                        } else {
                                            try {
                                                //Fall back to muxing code
                                                WriteGifMuxed(new URL(url), progressBar, c, subreddit);
                                            } catch (Exception e) {
                                                catchVRedditFailure(e, url);
                                            }
                                        }
                                    });
                        } else {
                            WriteGifMuxed(new URL(url), progressBar, c, subreddit);
                        }
                    } catch (Exception e) {
                        catchVRedditFailure(e, url);
                    }
                    break;*/return Uri.parse(url)

                VideoType.GFYCAT -> {
                    val name = url.substring(url.lastIndexOf("/"))
                    val gfycatUrl = "https://api.gfycat.com/v1/gfycats$name"

                    //Check if resolved gfycat link is gifdeliverynetwork. If it is gifdeliverynetwork, open the link externally
                    try {
                        val uri = loadGfycat(name, url, gson)
                        return if (uri.toString().contains("gifdeliverynetwork")) {
                            openWebsite(url)
                            null
                        } else uri
                    } catch (e: Exception) {
                        LogUtil.e(
                            e, "Error loading gfycat video url = ["
                                    + url
                                    + "] gfycatUrl = ["
                                    + gfycatUrl
                                    + "]"
                        )
                    }
                }

                VideoType.DIRECT, VideoType.IMGUR -> try {
                    return Uri.parse(url)
                } catch (e: Exception) {
                    LogUtil.e(
                        e,
                        "Error loading URL $url"
                    ) //Most likely is an image, not a gif!
                    if (c is MediaView && url.contains("imgur.com") && url.endsWith(
                            ".mp4"
                        )
                    ) {
                        c.runOnUiThread {
                            c.startActivity(
                                Intent(c, MediaView::class.java).putExtra(
                                    MediaView.EXTRA_URL, url.replace(
                                        ".mp4",
                                        ".png"
                                    )
                                )
                            ) //Link is likely an image and not a gif
                            c.finish()
                        }
                    } else {
                        openWebsite(url)
                    }
                }

                VideoType.STREAMABLE -> {
                    val hash = url.substring(url.lastIndexOf("/") + 1)
                    val streamableUrl = "https://api.streamable.com/videos/$hash"
                    LogUtil.v(streamableUrl)
                    try {
                        val result = HttpUtil.getJsonObject(client, gson, streamableUrl)
                        val obj: String
                        if (result == null || result["files"] == null || !(result.getAsJsonObject("files")
                                .has("mp4")
                                    || result.getAsJsonObject("files").has("mp4-mobile"))
                        ) {
                            onError()
                            if (closeIfNull) {
                                c.runOnUiThread {
                                    AlertDialog.Builder(c)
                                        .setTitle(R.string.error_video_not_found)
                                        .setMessage(R.string.error_video_message)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> c.finish() }
                                        .create()
                                        .show()
                                }
                            }
                        } else {
                            obj = if (result.asJsonObject["files"]
                                    .asJsonObject
                                    .has("mp4-mobile") && !result.asJsonObject["files"]
                                    .asJsonObject["mp4-mobile"]
                                    .asJsonObject["url"]
                                    .asString
                                    .isEmpty()
                            ) {
                                result.asJsonObject["files"]
                                    .asJsonObject["mp4-mobile"]
                                    .asJsonObject["url"]
                                    .asString
                            } else {
                                result.asJsonObject["files"]
                                    .asJsonObject["mp4"]
                                    .asJsonObject["url"]
                                    .asString
                            }
                            return Uri.parse(obj)
                        }
                    } catch (e: Exception) {
                        LogUtil.e(
                            e, "Error loading streamable video url = ["
                                    + url
                                    + "] streamableUrl = ["
                                    + streamableUrl
                                    + "]"
                        )
                        c.runOnUiThread { onError() }
                        if (closeIfNull) {
                            c.runOnUiThread {
                                try {
                                    AlertDialog.Builder(c)
                                        .setTitle(R.string.error_video_not_found)
                                        .setMessage(R.string.error_video_message)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> c.finish() }
                                        .create()
                                        .show()
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                    }
                }

                VideoType.OTHER -> {
                    LogUtil.e("We shouldn't be here!")
                    // unless it's a .gif that reddit didn't generate a preview vid for, then we should be here
                    // e.g. https://www.reddit.com/r/testslideforreddit/comments/hpht5o/stinky/
                    openWebsite(url)
                }
            }
            return null
        }

        override fun onPostExecute(uri: Uri?) {
            if (uri == null) {
                cancel()
                return
            }
            progressBar!!.isIndeterminate = true
            if (gifSave != null) {
                gifSave.setOnClickListener(View.OnClickListener {
                    cacheSaveGif(
                        uri,
                        c,
                        subreddit,
                        submissionTitle,
                        true
                    )
                })
            } else if (doOnClick != null) {
                MediaView.doOnClick =
                    Runnable { cacheSaveGif(uri, c, subreddit, submissionTitle, true) }
            }
            val type =
                if (uri.host == "v.redd.it") ExoVideoView.VideoType.DASH else ExoVideoView.VideoType.STANDARD
            video.setVideoURI(uri, type, object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        progressBar!!.visibility = View.GONE
                        if (size != null) {
                            size!!.visibility = View.GONE
                        }
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        progressBar!!.visibility = View.VISIBLE
                        if (size != null) {
                            size!!.visibility = View.VISIBLE
                        }
                    }
                }
            })
            if (autostart) {
                video.play()
            }
        }

        private fun openWebsite(url: String) {
            if (closeIfNull) {
                val web = Intent(c, Website::class.java)
                web.putExtra(LinkUtil.EXTRA_URL, url)
                web.putExtra(LinkUtil.EXTRA_COLOR, Color.BLACK)
                c.startActivity(web)
                c.finish()
            }
        }

        companion object {
            /**
             * Format a video URL correctly and strip unnecessary parts
             *
             * @param s URL to format
             * @return Formatted URL
             */
            fun formatUrl(s: String): String {
                var s = s
                if (s.endsWith("v") && !s.contains("streamable.com")) {
                    s = s.substring(0, s.length - 1)
                } else if (s.contains("gfycat") && !s.contains("mp4") && !s.contains("webm")) {
                    s = s.replace("-size_restricted", "")
                    s = s.replace(".gif", "")
                }
                if ((s.contains(".webm") || s.contains(".gif")) && !s.contains(".gifv") && s.contains(
                        "imgur.com"
                    )
                ) {
                    s = s.replace(".gif", ".mp4")
                    s = s.replace(".webm", ".mp4")
                }
                if (s.endsWith("/")) s = s.substring(0, s.length - 1)
                if (s.endsWith("?r")) s = s.substring(0, s.length - 2)
                if (s.contains("v.redd.it") && !s.contains("DASHPlaylist")) {
                    if (s.contains("DASH")) {
                        s = s.substring(0, s.indexOf("DASH"))
                    }
                    if (s.endsWith("/")) {
                        s = s.substring(0, s.length - 1)
                    }
                    s += "/DASHPlaylist.mpd"
                }
                return s
            }

            /**
             * Identifies the type of a video URL
             *
             * @param url URL to identify the type of
             * @return The type of video
             */
            @JvmStatic
            fun getVideoType(url: String): VideoType {
                val realURL = url.lowercase()
                if (realURL.contains("v.redd.it")) {
                    return VideoType.VREDDIT
                }
                if (realURL.contains(".mp4") || realURL.contains("webm") || realURL.contains("redditmedia.com")
                    || realURL.contains("preview.redd.it")
                ) {
                    return VideoType.DIRECT
                }
                if (realURL.contains("gfycat") && !realURL.contains("mp4")) return VideoType.GFYCAT
                if (realURL.contains("redgifs") && !realURL.contains("mp4")) return VideoType.GFYCAT
                if (realURL.contains("imgur.com")) return VideoType.IMGUR
                return if (realURL.contains("streamable.com")) VideoType.STREAMABLE else VideoType.OTHER
            }

            fun makeHeaderMap(domain: String): Map<String, String> {
                val map: MutableMap<String, String> = HashMap()
                map["Host"] = domain
                map["Sec-Fetch-Dest"] = "empty"
                map["Sec-Fetch-Mode"] = "cors"
                map["Sec-Fetch-Site"] = "same-origin"
                map["User-Agent"] =
                    "Mozilla/5.0 (Windows NT 10.0; rv:107.0) Gecko/20100101 Firefox/107.0"
                return map
            }
            /* Code currently unused, but could be used for future DASH issues
                public interface VideoSuccessCallback {
            void onVideoFound(Boolean didFindVideo, String videoUrl);
        }

        public interface VideoTestCallback {
            void onTestComplete(Boolean testSuccess, String videoUrl);
        }

        //Find a Reddit video MP4 URL by replacing HLSPlaylist.m3u8 with tests of different qualities
        public static void getQualityURL(String urlToLoad, String[] qualityList, VideoSuccessCallback callback) {
            if (qualityList.length == 0) {
                //Will fall back to muxing code if no URL was found
                callback.onVideoFound(false, "");
            } else {
                //Test current first link in qualityList
                VideoTestCallback testCallback = (testSuccess, videoUrl) -> {
                    if (testSuccess) {
                        //Success, load this video
                        callback.onVideoFound(true, videoUrl);
                    } else {
                        //Failed, check next video URL
                        String[] newList = Arrays.copyOfRange(qualityList, 1, qualityList.length);
                        getQualityURL(urlToLoad, newList, callback);
                    }
                };
                testQuality(urlToLoad, qualityList[0], testCallback);

            }
        }

        //Test URL headers to see if this quality URL exists
        private static void testQuality(String urlToLoad, String quality, AsyncLoadGif.VideoTestCallback callback) {
            String newURL = urlToLoad.replace("HLSPlaylist.m3u8", "DASH_" + quality + ".mp4");
            try {
                URL url = new URL(newURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("HEAD");
                con.connect();
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    //Success, load this MP4
                    callback.onTestComplete(true, newURL);
                } else {
                    //Failed, this callback will test a new URL
                    callback.onTestComplete(false, newURL);
                }
            } catch (Exception e) {
                e.printStackTrace();
                //Failed, this callback will test a new URL
                callback.onTestComplete(false, newURL);
            }
        }
         */
            /**
             * Get a remote video's file size
             *
             * @param url      URL of video (or v.redd.it DASH manifest) to get
             * @param client   OkHttpClient
             * @param sizeText TextView to put size into
             * @param c        Activity
             */
            fun getRemoteFileSize(
                url: String, client: OkHttpClient?,
                sizeText: TextView, c: Activity
            ) {
                if (!url.contains("v.redd.it")) {
                    val request: Request = Request.Builder().url(url).head().build()
                    val response: Response
                    try {
                        response = client!!.newCall(request).execute()
                        val size = response.body!!.contentLength()
                        response.close()
                        c.runOnUiThread { sizeText.text = FileUtil.readableFileSize(size) }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    val downloader: DataSource.Factory = OkHttpDataSource.Factory(
                        App.client!!
                    )
                        .setUserAgent(c.getString(R.string.app_name))
                    val cacheDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
                        .setCache(App.videoCache!!)
                        .setUpstreamDataSourceFactory(downloader)
                    val dashManifestStream: InputStream = DataSourceInputStream(
                        cacheDataSourceFactory.createDataSource(),
                        DataSpec(Uri.parse(url))
                    )
                    try {
                        val dashManifest =
                            DashManifestParser().parse(Uri.parse(url), dashManifestStream)
                        dashManifestStream.close()
                        var videoSize: Long = 0
                        var audioSize: Long = 0
                        for (i in 0 until dashManifest.periodCount) {
                            for (`as` in dashManifest.getPeriod(i).adaptationSets) {
                                var isAudio = false
                                var bitrate = 0
                                var hqUri: String? = null
                                for (r in `as`.representations) {
                                    if (r.format.bitrate > bitrate) {
                                        bitrate = r.format.bitrate
                                        hqUri = r.baseUrls[0].url
                                    }
                                    if (MimeTypes.isAudio(r.format.sampleMimeType)) {
                                        isAudio = true
                                    }
                                }
                                val request: Request = Request.Builder().url(hqUri!!).head().build()
                                var response: Response? = null
                                try {
                                    response = client!!.newCall(request).execute()
                                    if (isAudio) {
                                        audioSize = response.body!!.contentLength()
                                    } else {
                                        videoSize = response.body!!.contentLength()
                                    }
                                    response.close()
                                } catch (e: IOException) {
                                    response?.close()
                                }
                            }
                        }
                        val totalSize = videoSize + audioSize
                        c.runOnUiThread { // We can't know which quality will be selected, so we display <= the highest quality size
                            if (totalSize > 0) sizeText.text =
                                "≤ " + FileUtil.readableFileSize(totalSize)
                        }
                    } catch (ignored: IOException) {
                    }
                }
            }
        }
    }

    private class BufferedWritableFileByteChannel(private val outputStream: OutputStream) :
        WritableByteChannel {
        private var isOpen = true
        private val byteBuffer: ByteBuffer
        private val rawBuffer = ByteArray(BUFFER_CAPACITY)

        init {
            byteBuffer = ByteBuffer.wrap(rawBuffer)
        }

        override fun write(inputBuffer: ByteBuffer): Int {
            val inputBytes = inputBuffer.remaining()
            if (inputBytes > byteBuffer.remaining()) {
                dumpToFile()
                byteBuffer.clear()
                if (inputBytes > byteBuffer.remaining()) {
                    throw BufferOverflowException()
                }
            }
            byteBuffer.put(inputBuffer)
            return inputBytes
        }

        override fun isOpen(): Boolean {
            return isOpen
        }

        override fun close() {
            dumpToFile()
            isOpen = false
        }

        private fun dumpToFile() {
            try {
                outputStream.write(rawBuffer, 0, byteBuffer.position())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        companion object {
            private const val BUFFER_CAPACITY = 1000000
        }
    }
}
