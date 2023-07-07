package ltd.ucode.slide

import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.jakewharton.processphoenix.ProcessPhoenix
import com.lusfold.androidkeyvaluestore.KVStore
import com.nostra13.universalimageloader.core.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.ImageFlairs
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.Notifications.NotificationPiggyback
import me.ccrama.redditslide.PostMatch
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
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.acra.ACRAConstants
import org.acra.ReportField
import org.acra.config.limiter
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.apache.commons.lang3.tuple.Triple
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.impl.HandroidLoggerAdapter
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Locale

@HiltAndroidApp
class App : Application(), ActivityLifecycleCallbacks {
    private val logger: KLogger = KotlinLogging.logger {}

    init {
        if (BuildConfig.DEBUG) {
            HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
            HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
            HandroidLoggerAdapter.APP_NAME = ""
        }
        logger.error { "Booting" }
        logger.warn { "Booting" }
        logger.info { "Booting" }
        logger.debug { "Booting" }
        logger.trace { "Booting" }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            sendReportsInDevMode = true

            val logcatLines = 500
            logcatArguments = listOf(
                "-t", logcatLines.toString(), // limit to N most recent lines
                "-v", "time", // use "time" format (`logcat -h`)
                "*:D") // no verbose
            reportContent = ACRAConstants.DEFAULT_REPORT_FIELDS
                .plus(ReportField.STACK_TRACE_HASH)
                .plus(ReportField.THREAD_DETAILS)
                .plus(ReportField.EVENTSLOG)

            mailSender {
                enabled = false
                mailTo = "slide@fire.fundersclub.com"
            }

            notification {
                title = "Slide has had an error!"
                text = "Report the exception and log?"
                commentPrompt = "What led to this situation?"
                channelName = NotificationChannelCompat.DEFAULT_CHANNEL_ID
            }

            limiter {
                resetLimitsOnAppUpdate = true
            }
        }
    }

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
        if (authentication != null && Authentication.didOnline && SettingValues.authentication.getLong(
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
        AlbumUtils.albumRequests = SettingValues.albums
        TumblrUtils.tumblrRequests = SettingValues.tumblr
        cachedData = SettingValues.cachedData
        if (!cachedData!!.contains("hasReset")) {
            cachedData!!.edit().clear().putBoolean("hasReset", true).apply()
        }
        registerActivityLifecycleCallbacks(this)
        UserSubscriptions.subscriptions = SettingValues.subscriptions
        UserSubscriptions.multiNameToSubs = getSharedPreferences("MULTITONAME", 0)
        UserSubscriptions.newsNameToSubs = getSharedPreferences("NEWSMULTITONAME", 0)
        UserSubscriptions.news = getSharedPreferences("NEWS", 0)
        UserSubscriptions.newsNameToSubs?.run {
            edit()
                .putString("android", "android+androidapps+googlepixel")
                .putString("news", "worldnews+news+politics")
                .apply()
        }
        UserSubscriptions.pinned = getSharedPreferences("PINNED", 0)
        PostMatch.filters = SettingValues.filters
        ImageFlairs.flairs = getSharedPreferences("FLAIRS", 0)
        SettingValues.initialize()
        SortingUtil.defaultSorting = SettingValues.defaultSorting
        SortingUtil.timePeriod = SettingValues.timePeriod
        KVStore.init(this, "SEEN") // TODO: replace with room
        doLanguages()
        lastPosition = ArrayList()
        if (SettingValues.appRestart.contains("startScreen")) {
            SettingValues.appRestart.edit().remove("startScreen").apply()
        } else {
            Authentication.isLoggedIn = SettingValues.appRestart.getBoolean("loggedin", false)
            Authentication.name = SettingValues.appRestart.getString("name", "LOGGEDOUT")
            active = true
        }
        authentication = Authentication(this)
        AdBlocker.init(this)
        Authentication.mod = SettingValues.authentication.getBoolean(SHARED_PREF_IS_MOD, false)
        enter_animation_time = enter_animation_time_original * enter_animation_time_multiplier
        fabClear = SettingValues.colours.getBoolean(SettingValues.PREF_FAB_CLEAR, false)
        val widthDp = this.resources.configuration.screenWidthDp
        val heightDp = this.resources.configuration.screenHeightDp
        var fina = Math.max(widthDp, heightDp)
        fina += 99
        if (SettingValues.colours.contains("tabletOVERRIDE")) {
            dpWidth = SettingValues.colours.getInt("tabletOVERRIDE", fina / 300)
        } else {
            dpWidth = fina / 300
        }
        if (SettingValues.colours.contains("notificationOverride")) {
            notificationTime = SettingValues.colours.getInt("notificationOverride", 360)
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
        const val SHARED_PREF_IS_MOD = "is_mod"
        @JvmField
        var videoCache: Cache? = null
        var enter_animation_time = enter_animation_time_original
        const val enter_animation_time_multiplier = 1
        @JvmField
        var authentication: Authentication? = null
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
                SettingValues.appRestart.edit().putString("startScreen", "").apply()
                SettingValues.appRestart.edit().putBoolean("isRestarting", true).apply()
            }
            if (SettingValues.appRestart.contains("back")) {
                SettingValues.appRestart.edit().remove("back").apply()
            }
            SettingValues.appRestart.edit().putBoolean("isRestarting", true).apply()
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
            } catch (ignored: NameNotFoundException) {
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
                        Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TEST_URL)),
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
