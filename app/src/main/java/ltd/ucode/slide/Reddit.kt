package ltd.ucode.slide

import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager.BadTokenException
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.multidex.MultiDexApplication
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.jakewharton.processphoenix.ProcessPhoenix
import com.lusfold.androidkeyvaluestore.KVStore
import com.nostra13.universalimageloader.core.ImageLoader
import ltd.ucode.slide.Activities.MainActivity
import me.ccrama.redditslide.Authentication
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.ContentType
import me.ccrama.redditslide.ImageFlairs
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.Notifications.NotificationPiggyback
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SecretConstants
import me.ccrama.redditslide.SettingValues
import me.ccrama.redditslide.Tumblr.TumblrUtils
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AdBlocker
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.GifCache
import me.ccrama.redditslide.util.ImageLoaderUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.UpgradeUtil
import me.ccrama.redditslide.util.billing.IabHelper
import net.dean.jraw.http.NetworkException
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.apache.commons.lang3.tuple.Triple
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.lang.ref.WeakReference
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Locale

/**
 * Created by ccrama on 9/17/2015.
 */
class Reddit : MultiDexApplication(), ActivityLifecycleCallbacks {
    var active = false
    @JvmField
    var defaultImageLoader: ImageLoader? = null
    override fun onLowMemory() {
        super.onLowMemory()
        imageLoader!!.clearMemoryCache()
    }

    val imageLoader: ImageLoader?
        get() {
            if (defaultImageLoader == null || !defaultImageLoader!!.isInited) {
                ImageLoaderUtils.initImageLoader(applicationContext)
                defaultImageLoader = ImageLoaderUtils.imageLoader
            }
            return defaultImageLoader
        }

    override fun onActivityResumed(activity: Activity) {
        doLanguages()
        if (client == null) {
            val builder = OkHttpClient.Builder()
            builder.dns(GfycatIpv4Dns())
            client = builder.build()
        }
        if (authentication != null && Authentication.didOnline && Authentication.authentication.getLong(
                "expires",
                0
            ) <= Calendar.getInstance()
                .timeInMillis
        ) {
            authentication!!.updateToken(activity)
        } else if (NetworkUtil.isConnected(activity) && authentication == null) {
            authentication = Authentication(this)
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        doLanguages()
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onCreate() {
        super.onCreate()
        mApplication = this
        //  LeakCanary.install(this);
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return
        }
        val dir: File
        dir =
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && externalCacheDir != null) {
                File(externalCacheDir.toString() + File.separator + "video-cache")
            } else {
                File(cacheDir.toString() + File.separator + "video-cache")
            }
        val evictor = LeastRecentlyUsedCacheEvictor((256 * 1024 * 1024).toLong())
        val databaseProvider: DatabaseProvider = ExoDatabaseProvider(appContext)
        videoCache = SimpleCache(dir, evictor, databaseProvider) // 256MB
        UpgradeUtil.upgrade(applicationContext)
        doMainStuff()
    }

    fun doMainStuff() {
        Log.v(LogUtil.getTag(), "ON CREATED AGAIN")
        if (client == null) {
            client = OkHttpClient()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setCanUseNightModeAuto()
        }
        overrideLanguage = getSharedPreferences("SETTINGS", 0).getBoolean(
            SettingValues.PREF_OVERRIDE_LANGUAGE,
            false
        )
        appRestart = getSharedPreferences("appRestart", 0)
        AlbumUtils.albumRequests = getSharedPreferences("albums", 0)
        TumblrUtils.tumblrRequests = getSharedPreferences("tumblr", 0)
        cachedData = getSharedPreferences("cache", 0)
        if (!cachedData!!.contains("hasReset")) {
            cachedData!!.edit().clear().putBoolean("hasReset", true).apply()
        }
        registerActivityLifecycleCallbacks(this)
        Authentication.authentication = getSharedPreferences("AUTH", 0)
        UserSubscriptions.subscriptions = getSharedPreferences("SUBSNEW", 0)
        UserSubscriptions.multiNameToSubs = getSharedPreferences("MULTITONAME", 0)
        UserSubscriptions.newsNameToSubs = getSharedPreferences("NEWSMULTITONAME", 0)
        UserSubscriptions.news = getSharedPreferences("NEWS", 0)
        UserSubscriptions.newsNameToSubs.edit()
            .putString("android", "android+androidapps+googlepixel")
            .putString("news", "worldnews+news+politics")
            .apply()
        UserSubscriptions.pinned = getSharedPreferences("PINNED", 0)
        PostMatch.filters = getSharedPreferences("FILTERS", 0)
        ImageFlairs.flairs = getSharedPreferences("FLAIRS", 0)
        SettingValues.setAllValues(getSharedPreferences("SETTINGS", 0))
        SortingUtil.defaultSorting = SettingValues.defaultSorting
        SortingUtil.timePeriod = SettingValues.timePeriod
        colours = getSharedPreferences("COLOR", 0)
        tags = getSharedPreferences("TAGS", 0)
        KVStore.init(this, "SEEN")
        doLanguages()
        lastPosition = ArrayList()
        if (!appRestart!!.contains("startScreen")) {
            Authentication.isLoggedIn = appRestart!!.getBoolean("loggedin", false)
            Authentication.name = appRestart!!.getString("name", "LOGGEDOUT")
            active = true
        } else {
            appRestart!!.edit().remove("startScreen").apply()
        }
        authentication = Authentication(this)
        AdBlocker.init(this)
        Authentication.mod = Authentication.authentication.getBoolean(SHARED_PREF_IS_MOD, false)
        enter_animation_time = enter_animation_time_original * enter_animation_time_multiplier
        fabClear = colours!!.getBoolean(SettingValues.PREF_FAB_CLEAR, false)
        val widthDp = this.resources.configuration.screenWidthDp
        val heightDp = this.resources.configuration.screenHeightDp
        var fina = Math.max(widthDp, heightDp)
        fina += 99
        if (colours!!.contains("tabletOVERRIDE")) {
            dpWidth = colours!!.getInt("tabletOVERRIDE", fina / 300)
        } else {
            dpWidth = fina / 300
        }
        if (colours!!.contains("notificationOverride")) {
            notificationTime = colours!!.getInt("notificationOverride", 360)
        } else {
            notificationTime = 360
        }
        SettingValues.isPro = isProPackageInstalled || BuildConfig.isFDroid
        videoPlugin = isVideoPluginInstalled
        GifCache.init(this)
        setupNotificationChannels()
    }

    fun doLanguages() {
        if (SettingValues.overrideLanguage) {
            val locale = Locale("en_US")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.locale = locale
            resources.updateConfiguration(config, null)
        }
    }

    val isNotificationAccessEnabled: Boolean
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        get() {
            val manager = ContextCompat.getSystemService(this, ActivityManager::class.java)
            if (manager != null) {
                for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                    if (NotificationPiggyback::class.java.name == service.service.className) {
                        return true
                    }
                }
            }
            return false
        }

    fun setupNotificationChannels() {
        // Each triple contains the channel ID, name, and importance level
        val notificationTripleList: List<Triple<String, String?, Int>> =
            object : ArrayList<Triple<String, String?, Int>>() {
                init {
                    add(
                        Triple.of(
                            CHANNEL_IMG, "Image downloads",
                            NotificationManagerCompat.IMPORTANCE_LOW
                        )
                    )
                    add(
                        Triple.of(
                            CHANNEL_COMMENT_CACHE, "Comment caching",
                            NotificationManagerCompat.IMPORTANCE_LOW
                        )
                    )
                    add(
                        Triple.of(
                            CHANNEL_MAIL, "Reddit mail",
                            NotificationManagerCompat.IMPORTANCE_HIGH
                        )
                    )
                    add(
                        Triple.of(
                            CHANNEL_MODMAIL, "Reddit modmail",
                            NotificationManagerCompat.IMPORTANCE_HIGH
                        )
                    )
                    add(
                        Triple.of(
                            CHANNEL_SUBCHECKING, "Submission post checking",
                            NotificationManagerCompat.IMPORTANCE_LOW
                        )
                    )
                }
            }
        val notificationManager = NotificationManagerCompat.from(this)
        for (notificationTriple in notificationTripleList) {
            val notificationChannel = NotificationChannelCompat.Builder(
                notificationTriple.left, notificationTriple.right
            )
                .setName(notificationTriple.middle)
                .setLightsEnabled(true)
                .setShowBadge(notificationTriple.right == NotificationManagerCompat.IMPORTANCE_HIGH)
                .setLightColor(
                    if (notificationTriple.left.contains("MODMAIL")) ResourcesCompat.getColor(
                        this.resources,
                        R.color.md_red_500,
                        null
                    ) else Palette.getColor("")
                )
                .build()
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    //IPV6 workaround by /u/talklittle
    class GfycatIpv4Dns : Dns {
        @Throws(UnknownHostException::class)
        override fun lookup(hostname: String): List<InetAddress> {
            return if (ContentType.hostContains(hostname, "gfycat.com", "redgifs.com")) {
                val addresses = InetAddress.getAllByName(hostname)
                if (addresses == null || addresses.size == 0) {
                    throw UnknownHostException("Bad host: $hostname")
                }

                // prefer IPv4; list IPv4 first
                val result = ArrayList<InetAddress>()
                for (address in addresses) {
                    if (address is Inet4Address) {
                        result.add(address)
                    }
                }
                for (address in addresses) {
                    if (address !is Inet4Address) {
                        result.add(address)
                    }
                }
                result
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    companion object {
        private var mApplication: Application? = null
        const val EMPTY_STRING = "NOTHING"
        const val enter_animation_time_original: Long = 600
        const val PREF_LAYOUT = "PRESET"
        const val SHARED_PREF_IS_MOD = "is_mod"
        @JvmField
        var videoCache: Cache? = null
        @JvmField
        var mHelper: IabHelper? = null
        var enter_animation_time = enter_animation_time_original
        const val enter_animation_time_multiplier = 1
        @JvmField
        var authentication: Authentication? = null
        @JvmField
        var colours: SharedPreferences? = null
        @JvmField
        var appRestart: SharedPreferences? = null
        @JvmField
        var tags: SharedPreferences? = null
        @JvmField
        var dpWidth = 0
        @JvmField
        var notificationTime = 0
        @JvmField
        var videoPlugin = false
        @JvmField
        var notifications: NotificationJobScheduler? = null
        @JvmField
        var isLoading = false
        val time = System.currentTimeMillis()
        @JvmField
        var fabClear = false
        var lastPosition: ArrayList<Int>? = null
        @JvmField
        var currentPosition = 0
        @JvmField
        var cachedData: SharedPreferences? = null
        const val noGapps = true //for testing
        var overrideLanguage = false
        var isRestarting = false
        @JvmField
        var autoCache: AutoCacheScheduler? = null
        @JvmField
        var peek = false
        @JvmField
        var client: OkHttpClient? = null
        @JvmField
        var canUseNightModeAuto = false
        @JvmStatic
        fun forceRestart(context: Context?, forceLoadScreen: Boolean) {
            if (forceLoadScreen) {
                appRestart!!.edit().putString("startScreen", "").apply()
                appRestart!!.edit().putBoolean("isRestarting", true).apply()
            }
            if (appRestart!!.contains("back")) {
                appRestart!!.edit().remove("back").apply()
            }
            appRestart!!.edit().putBoolean("isRestarting", true).apply()
            isRestarting = true
            ProcessPhoenix.triggerRebirth(context, Intent(context, MainActivity::class.java))
        }

        @JvmStatic
        fun defaultShareText(title: String?, url: String?, c: Context) {
            var title = title
            var url = url
            url = StringEscapeUtils.unescapeHtml4(CompatUtil.fromHtml(url!!).toString())
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            /* Decode html entities */title = StringEscapeUtils.unescapeHtml4(title)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title)
            sharingIntent.putExtra(Intent.EXTRA_TEXT, url)
            c.startActivity(Intent.createChooser(sharingIntent, c.getString(R.string.title_share)))
        }

        @JvmStatic
        fun isPackageInstalled(s: String?): Boolean {
            try {
                val pi = appContext.packageManager.getPackageInfo(
                    s!!, 0
                )
                if (pi != null && pi.applicationInfo.enabled) return true
            } catch (ignored: Throwable) {
            }
            return false
        }

        private val isProPackageInstalled: Boolean
            private get() = isPackageInstalled(appContext.getString(R.string.ui_unlock_package))
        private val isVideoPluginInstalled: Boolean
            private get() = isPackageInstalled(appContext.getString(R.string.youtube_plugin_package))
        @JvmStatic
        val installedBrowsers: HashMap<String, String>
            get() {
                val packageMatcher =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PackageManager.MATCH_ALL else PackageManager.GET_DISABLED_COMPONENTS
                val browserMap = HashMap<String, String>()
                val resolveInfoList = appContext.packageManager
                    .queryIntentActivities(
                        Intent(Intent.ACTION_VIEW, Uri.parse("http://ccrama.me")),
                        packageMatcher
                    )
                for (resolveInfo in resolveInfoList) {
                    if (resolveInfo.activityInfo.enabled) {
                        browserMap[resolveInfo.activityInfo.applicationInfo.packageName] =
                            appContext
                                .packageManager
                                .getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
                                .toString()
                    }
                }
                return browserMap
            }
        @JvmField
        var notFirst = false
        @JvmStatic
        fun setDefaultErrorHandler(base: Context?) {
            //START code adapted from https://github.com/QuantumBadger/RedReader/
            val androidHandler = Thread.getDefaultUncaughtExceptionHandler()
            val cont = WeakReference(base)
            Thread.setDefaultUncaughtExceptionHandler { thread, t ->
                if (cont.get() != null) {
                    val c = cont.get()
                    val writer: Writer = StringWriter()
                    val printWriter = PrintWriter(writer)
                    t.printStackTrace(printWriter)
                    val stacktrace = writer.toString().replace(";", ",")
                    if (stacktrace.contains("UnknownHostException") || stacktrace.contains(
                            "SocketTimeoutException"
                        ) || stacktrace.contains("ConnectException")
                    ) {
                        //is offline
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(c!!)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_connection_failed_msg)
                                    .setNegativeButton(R.string.btn_close) { dialog: DialogInterface?, which: Int ->
                                        if (c !is MainActivity) {
                                            (c as Activity?)!!.finish()
                                        }
                                    }
                                    .setPositiveButton(R.string.btn_offline) { dialog: DialogInterface?, which: Int ->
                                        appRestart!!.edit()
                                            .putBoolean("forceoffline", true)
                                            .apply()
                                        forceRestart(c, false)
                                    }
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    } else if (stacktrace.contains("403 Forbidden") || stacktrace.contains(
                            "401 Unauthorized"
                        )
                    ) {
                        //Un-authenticated
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(c!!)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_refused_request_msg)
                                    .setNegativeButton("No") { dialog: DialogInterface?, which: Int ->
                                        if (c !is MainActivity) {
                                            (c as Activity?)!!.finish()
                                        }
                                    }
                                    .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
                                        authentication!!.updateToken(
                                            c
                                        )
                                    }
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    } else if (stacktrace.contains("404 Not Found") || stacktrace.contains(
                            "400 Bad Request"
                        )
                    ) {
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(c!!)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_could_not_find_content_msg)
                                    .setNegativeButton("Close") { dialog: DialogInterface?, which: Int ->
                                        if (c !is MainActivity) {
                                            (c as Activity?)!!.finish()
                                        }
                                    }
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    } else if (t is NetworkException) {
                        Toast.makeText(
                            c, "Error "
                                    + t.response.statusMessage
                                    + ": "
                                    + t.message, Toast.LENGTH_LONG
                        ).show()
                    } else if (t is NullPointerException && t.message!!
                            .contains(
                                "Attempt to invoke virtual method 'android.content.Context android.view.ViewGroup.getContext()' on a null object reference"
                            )
                    ) {
                        t.printStackTrace()
                    } else if (t is BadTokenException) {
                        t.printStackTrace()
                    } else if (t is IllegalArgumentException && t.message!!
                            .contains("pointerIndex out of range")
                    ) {
                        t.printStackTrace()
                    } else {
                        appRestart!!.edit()
                            .putString("startScreen", "a")
                            .apply() //Force reload of data after crash incase state was not saved
                        try {
                            val prefs = c!!.getSharedPreferences("STACKTRACE", MODE_PRIVATE)
                            prefs.edit().putString("stacktrace", stacktrace).apply()
                        } catch (ignored: Throwable) {
                        }
                        androidHandler.uncaughtException(thread, t)
                    }
                } else {
                    androidHandler.uncaughtException(thread, t)
                }
            }
            //END adaptation
        }

        const val CHANNEL_IMG = "IMG_DOWNLOADS"
        const val CHANNEL_COMMENT_CACHE = "POST_SYNC"
        const val CHANNEL_MAIL = "MAIL_NOTIFY"
        const val CHANNEL_MODMAIL = "MODMAIL_NOTIFY"
        const val CHANNEL_SUBCHECKING = "SUB_CHECK_NOTIFY"
        @JvmStatic
        val appContext: Context
            get() = mApplication!!.applicationContext

        @TargetApi(Build.VERSION_CODES.M)
        private fun setCanUseNightModeAuto() {
            val uiModeManager = appContext.getSystemService(
                UiModeManager::class.java
            )
            canUseNightModeAuto = uiModeManager != null
        }
    }
}
