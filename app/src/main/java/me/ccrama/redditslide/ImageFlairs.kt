package me.ccrama.redditslide

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.nostra13.universalimageloader.cache.disc.DiskCache
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.assist.ImageSize
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.SendMessage
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.OkHttpImageDownloader
import net.dean.jraw.http.HttpRequest
import net.dean.jraw.http.MediaTypes
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

object ImageFlairs {
    fun syncFlairs(context: Context, subreddit: String) {
        object : StylesheetFetchTask(subreddit, context) {
            override fun onPostExecute(flairStylesheet: FlairStylesheet?) {
                super.onPostExecute(flairStylesheet)
                d!!.dismiss()
                d = if (flairStylesheet != null) {
                    flairs!!.edit().putBoolean(subreddit.lowercase(), true).commit()
                    AlertDialog.Builder(context)
                        .setTitle("Subreddit flairs synced")
                        .setMessage("Slide found and synced " + flairStylesheet.count + " image flairs")
                        .setPositiveButton(R.string.btn_ok, null)
                        .show()
                } else {
                    val b = AlertDialog.Builder(context)
                        .setTitle("Error syncing subreddit flairs")
                        .setMessage(
                            "Slide could not find any subreddit flairs to sync from /c/"
                                    + subreddit
                                    + "'s stylesheet."
                        )
                        .setPositiveButton(R.string.btn_ok, null)
                    if (Authentication.isLoggedIn) {
                        b.setNeutralButton("Report no flairs") { dialog: DialogInterface?, which: Int ->
                            Toast.makeText(
                                context,
                                "Not all subreddits can be parsed, but send a message to SlideBot and hopefully we can add support for this subreddit :)\n\nPlease, only send one report.",
                                Toast.LENGTH_LONG
                            )
                            val i = Intent(context, SendMessage::class.java)
                            i.putExtra(SendMessage.EXTRA_NAME, "slidebot")
                            i.putExtra(SendMessage.EXTRA_MESSAGE, "/c/$subreddit")
                            i.putExtra(SendMessage.EXTRA_REPLY, "Subreddit flair")
                            context.startActivity(i)
                        }
                    }
                    b.show()
                }
            }

            override fun onPreExecute() {
                d = MaterialDialog(context)
                    //.progress(true, 100)
                    .message(R.string.misc_please_wait)
                    .title(text = "Syncing flairs...")
                    .cancelable(false)
                    .also { it.show() }
            }
        }.execute()
    }

    var flairs: SharedPreferences? = null
    fun isSynced(subreddit: String): Boolean {
        return flairs!!.contains(subreddit.lowercase())
    }

    fun getFlairImageLoader(context: Context): FlairImageLoader? {
        return if (imageLoader == null) {
            initFlairImageLoader(context)
        } else {
            imageLoader
        }
    }

    var imageLoader: FlairImageLoader? = null
    fun getCacheDirectory(context: Context): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && context.externalCacheDir != null) {
            File(context.externalCacheDir, "flairs")
        } else File(context.cacheDir, "flairs")
    }

    fun initFlairImageLoader(context: Context): FlairImageLoader? {
        var discCacheSize = (1024 * 1024 * 100).toLong() //100 MB limit
        val discCache: DiskCache
        val dir = getCacheDirectory(context)
        discCacheSize *= 100
        val threadPoolSize = 7
        discCache = if (discCacheSize > 0) {
            try {
                dir.mkdir()
                LruDiskCache(dir, Md5FileNameGenerator(), discCacheSize)
            } catch (e: IOException) {
                UnlimitedDiskCache(dir)
            }
        } else {
            UnlimitedDiskCache(dir)
        }
        options = DisplayImageOptions.Builder().cacheOnDisk(true)
            .imageScaleType(ImageScaleType.NONE)
            .cacheInMemory(false)
            .resetViewBeforeLoading(false)
            .build()
        val config = ImageLoaderConfiguration.Builder(context).threadPoolSize(threadPoolSize)
            .denyCacheImageMultipleSizesInMemory()
            .diskCache(discCache)
            .threadPoolSize(4)
            .imageDownloader(OkHttpImageDownloader(context))
            .defaultDisplayImageOptions(options)
            .build()
        if (FlairImageLoader.instance!!.isInited) {
            FlairImageLoader.instance!!.destroy()
        }
        imageLoader = FlairImageLoader.instance
        imageLoader!!.init(config)
        return imageLoader
    }

    var options: DisplayImageOptions? = null

    internal open class StylesheetFetchTask(var subreddit: String, var context: Context) :
        AsyncTask<Void?, Void?, FlairStylesheet?>() {
        var d: Dialog? = null
        override fun doInBackground(vararg params: Void?): FlairStylesheet? {
            return try {
                val r = HttpRequest.Builder().host("old.reddit.com")
                    .path("/c/$subreddit/stylesheet")
                    .expected(MediaTypes.CSS.type())
                    .build()
                val response = Authentication.reddit!!.execute(r)
                val stylesheet = response.raw
                val allImages = ArrayList<String?>()
                val flairStylesheet = FlairStylesheet(stylesheet)
                for (s in flairStylesheet.listOfFlairIds) {
                    val classDef = flairStylesheet.getClass(
                        flairStylesheet.stylesheetString,
                        "flair-$s"
                    )
                    try {
                        var backgroundURL = flairStylesheet.getBackgroundURL(classDef)
                        if (backgroundURL == null) backgroundURL = flairStylesheet.defaultURL
                        if (!allImages.contains(backgroundURL)) allImages.add(backgroundURL)
                    } catch (e: Exception) {
                        //  e.printStackTrace();
                    }
                }
                if (flairStylesheet.defaultURL != null) {
                    LogUtil.v("Default url is " + flairStylesheet.defaultURL)
                    allImages.add(flairStylesheet.defaultURL)
                }
                for (backgroundURL in allImages) {
                    flairStylesheet.cacheFlairsByFile(subreddit, backgroundURL, context)
                }
                flairStylesheet
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    class CropTransformation(
        private val id: String,
        private val width: Int,
        private val height: Int,
        private val x: Int,
        private val y: Int
    ) {
        @Throws(Exception::class)
        fun transform(bitmap: Bitmap, isPercentage: Boolean): Bitmap {
            val nX: Int
            val nY: Int
            if (isPercentage) {
                nX = Math.max(0, Math.min(bitmap.width - 1, bitmap.width * x / 100))
                nY = Math.max(0, Math.min(bitmap.height - 1, bitmap.height * y / 100))
            } else {
                nX = Math.max(0, Math.min(bitmap.width - 1, x))
                nY = Math.max(0, Math.min(bitmap.height - 1, y))
            }
            val nWidth = Math.max(1, Math.min(bitmap.width - nX - 1, width))
            val nHeight = Math.max(1, Math.min(bitmap.height - nY - 1, height))
            LogUtil.v(
                "Flair loaded: "
                        + id
                        + " size: "
                        + nWidth
                        + "x"
                        + nHeight
                        + " location: "
                        + nX
                        + ":"
                        + nY + " and bit is " + bitmap.width + ":" + bitmap.height
            )
            return Bitmap.createBitmap(bitmap, nX, nY, nWidth, nHeight)
        }
    }

    internal class FlairStylesheet(stylesheetString: String) {
        var stylesheetString: String
        var defaultDimension = Dimensions()
        var defaultLocation = Location()
        var defaultURL: String? = ""
        var count: Int = 0
        var prevDimension: Dimensions? = null

        internal class Dimensions {
            var width = 0
            var height = 0
            var scale = false
            var missing = true

            constructor(width: Int, height: Int) {
                this.width = width
                this.height = height
                if (height == -1) {
                    scale = true
                }
                missing = false
            }

            constructor() {}
        }

        internal class Location {
            var x = 0
            var y = 0
            var isPercentage = false
            var missing = true

            constructor(x: Int, y: Int) {
                this.x = x
                this.y = y
                missing = false
            }

            constructor(x: Int, y: Int, isPercentage: Boolean) {
                this.x = x
                this.y = y
                this.isPercentage = isPercentage
                missing = false
            }

            constructor() {}
        }

        init {
            var stylesheetString = stylesheetString
            stylesheetString =
                stylesheetString.replace("@media[^{]+\\{([\\s\\S]+?\\})\\s*\\}".toRegex(), "")
            stylesheetString = stylesheetString.replace("~.".toRegex(), " .")
            this.stylesheetString = stylesheetString
            val baseFlairDef = getClass(stylesheetString, "flair")
            if (baseFlairDef != null) {
                LogUtil.v("Base is $baseFlairDef")
                // Attempts to find default dimension, offset and image URL
                defaultDimension = getBackgroundSize(baseFlairDef)
                LogUtil.v("Default dimens are " + defaultDimension.width + ":" + defaultDimension.height)
                defaultLocation = getBackgroundPosition(baseFlairDef)
                defaultURL = getBackgroundURL(baseFlairDef)
                count = 0
            }
        }

        /**
         * Get class definition string by class name.
         *
         * @param cssDefinitionString
         * @param className
         * @return
         */
        fun getClass(cssDefinitionString: String?, className: String): String? {
            val propertyDefinition = Pattern.compile(
                "(?<! )\\.$className(?!-|\\[|[A-Za-z0-9_.])([^\\{]*)*\\{(.+?)\\}"
            )
            val matches = propertyDefinition.matcher(cssDefinitionString)
            var properties: StringBuilder? = null
            while (matches.find()) {
                if (properties == null) properties = StringBuilder()
                properties.insert(
                    0, matches.group(2)
                            + ";"
                ) // append properties to simulate property overriding
            }
            return properties?.toString()
        }

        /**
         * Get property value inside a class definition by property name.
         *
         * @param classDefinitionsString
         * @param property
         * @return
         */
        fun getProperty(classDefinitionsString: String?, property: String): String? {
            val propertyDefinition = Pattern.compile("(?<!-)$property\\s*:\\s*(.+?)(;|$)")
            val matches = propertyDefinition.matcher(classDefinitionsString)
            return if (matches.find()) {
                matches.group(1)
            } else {
                null
            }
        }

        //Attempts to get a real integer value instead of "auto", if possible
        fun getPropertyTryNoAuto(classDefinitionsString: String?, property: String): String? {
            val propertyDefinition = Pattern.compile("(?<!-)$property\\s*:\\s*(.+?)(;|$)")
            val matches = propertyDefinition.matcher(classDefinitionsString)
            var defaultString: String
            defaultString = if (matches.find()) {
                matches.group(1)
            } else {
                return null
            }
            LogUtil.v("Has auto")
            while ((defaultString.contains("auto") || !defaultString.contains("%") || !defaultString.contains(
                    "px"
                )) && matches.find()
            ) {
                defaultString = matches.group(1)
            }
            LogUtil.v("Returning $defaultString")
            return defaultString
        }

        fun getPropertyBackgroundUrl(classDefinitionsString: String?): String? {
            val propertyDefinition = Pattern.compile("background:url\\([\"'](.+?)[\"']\\)")
            val matches = propertyDefinition.matcher(classDefinitionsString)
            return if (matches.find()) {
                matches.group(1)
            } else {
                null
            }
        }

        /**
         * Get flair background url in class definition.
         *
         * @param classDefinitionString
         * @return
         */
        fun getBackgroundURL(classDefinitionString: String?): String? {
            val urlDefinition = Pattern.compile("url\\([\"\'](.+?)[\"\']\\)")
            val backgroundProperty = getPropertyBackgroundUrl(classDefinitionString)
            if (backgroundProperty != null) {
                // check "background"
                var url: String = backgroundProperty
                if (url.startsWith("//")) url = "https:$url"
                return url
            }
            // either backgroundProperty is null or url cannot be found
            val backgroundImageProperty = getProperty(classDefinitionString, "background-image")
            if (backgroundImageProperty != null) {
                // check "background-image"
                val matches = urlDefinition.matcher(backgroundImageProperty)
                if (matches.find()) {
                    var url = matches.group(1)
                    if (url.startsWith("//")) url = "https:$url"
                    return url
                }
            }
            // could not find any background url
            return null
        }

        /**
         * Get background dimension in class definition.
         *
         * @param classDefinitionString
         * @return
         */
        fun getBackgroundSize(classDefinitionString: String?): Dimensions {
            val numberDefinition = Pattern.compile("(\\d+)\\s*px")
            var autoWidth = false
            var autoHeight = false
            // check common properties used to define width
            var widthProperty = getPropertyTryNoAuto(classDefinitionString, "width")
            if (widthProperty == null) {
                widthProperty = getPropertyTryNoAuto(classDefinitionString, "min-width")
            } else if (widthProperty == "auto") {
                autoWidth = true
            }
            if (widthProperty == null) {
                widthProperty = getProperty(classDefinitionString, "text-indent")
            }
            if (widthProperty == null) return Dimensions()

            // check common properties used to define height
            var heightProperty = getPropertyTryNoAuto(classDefinitionString, "height")
            if (heightProperty == null) {
                heightProperty = getPropertyTryNoAuto(classDefinitionString, "min-height")
            } else if (heightProperty == "auto") {
                autoHeight = true
            }
            if (heightProperty == null) return Dimensions()
            var width = 0
            var height = 0
            var matches: Matcher
            if (!autoWidth) {
                matches = numberDefinition.matcher(widthProperty)
                width = if (matches.find()) {
                    matches.group(1).toInt()
                } else {
                    return Dimensions()
                }
            }
            if (!autoHeight) {
                matches = numberDefinition.matcher(heightProperty)
                height = if (matches.find()) {
                    matches.group(1).toInt()
                } else {
                    return Dimensions()
                }
            }
            if (autoWidth) {
                width = height
            }
            if (autoHeight) {
                height = width
            }
            return Dimensions(width, height)
        }

        /**
         * Get background scaling in class definition.
         *
         * @param classDefinitionString
         * @return
         */
        fun getBackgroundScaling(classDefinitionString: String?): Dimensions {
            val positionDefinitionPx =
                Pattern.compile("([+-]?\\d+|0)(px\\s|\\s)+(|([+-]?\\d+|0)(px|))")
            val backgroundPositionProperty = getProperty(classDefinitionString, "background-size")
            val backgroundPositionPropertySecondary =
                getProperty(classDefinitionString, "background-size")
            if (backgroundPositionProperty == null && backgroundPositionPropertySecondary == null
                || (backgroundPositionProperty == null && !backgroundPositionPropertySecondary!!.contains(
                    "px "
                )
                        && !backgroundPositionPropertySecondary.contains("px;"))
            ) {
                return Dimensions()
            }
            val matches = positionDefinitionPx.matcher(backgroundPositionProperty)
            return if (matches.find()) {
                Dimensions(
                    matches.group(1).toInt(),
                    if (matches.groupCount() < 2) matches.group(3).toInt() else -1
                )
            } else {
                Dimensions()
            }
        }

        /**
         * Get background offset in class definition.
         *
         * @param classDefinitionString
         * @return
         */
        fun getBackgroundPosition(classDefinitionString: String?): Location {
            val positionDefinitionPx =
                Pattern.compile("([+-]?\\d+|0)(px\\s|\\s)+([+-]?\\d+|0)(px|)")
            val positionDefinitionPercentage =
                Pattern.compile("([+-]?\\d+|0)(%\\s|\\s)+([+-]?\\d+|0)(%|)")
            var backgroundPositionProperty =
                getProperty(classDefinitionString, "background-position")
            if (backgroundPositionProperty == null) {
                backgroundPositionProperty = getProperty(classDefinitionString, "background")
                if (backgroundPositionProperty == null) {
                    return Location()
                }
            }
            var matches = positionDefinitionPx.matcher(backgroundPositionProperty)
            try {
                if (matches.find()) {
                    return Location(
                        -matches.group(1).toInt(),
                        -matches.group(3).toInt()
                    )
                } else {
                    matches = positionDefinitionPercentage.matcher(backgroundPositionProperty)
                    if (matches.find()) {
                        return Location(matches.group(1).toInt(), matches.group(3).toInt(), true)
                    }
                }
            } catch (ignored: NumberFormatException) {
            }
            return Location()
        }

        fun getBackgroundOffset(classDefinitionString: String?): Dimensions {
            val positionDefinitionPx = Pattern.compile("([+-]?\\d+|0)\\/+([+-]?\\d+|0)(px|)")
            val backgroundPositionProperty = getProperty(classDefinitionString, "background")
                ?: return Dimensions()
            val matches = positionDefinitionPx.matcher(backgroundPositionProperty)
            try {
                if (matches.find()) {
                    return Dimensions(matches.group(2).toInt(), matches.group(2).toInt())
                }
            } catch (ignored: NumberFormatException) {
            }
            return Dimensions()
        }

        /**
         * Request a flair by flair id. `.into` can be chained onto this method call.
         *
         * @param sub
         * @param context
         * @return
         */
        fun cacheFlairsByFile(sub: String, filename: String?, context: Context) {
            val flairsToGet = ArrayList<String>()
            LogUtil.v("Doing sheet $filename")
            for (s in listOfFlairIds) {
                val classDef = getClass(stylesheetString, "flair-$s")
                if (classDef != null && !classDef.isEmpty()) {
                    var backgroundURL = getBackgroundURL(classDef)
                    if (backgroundURL == null) backgroundURL = defaultURL
                    if (backgroundURL != null && backgroundURL.equals(
                            filename,
                            ignoreCase = true
                        )
                    ) {
                        flairsToGet.add(s)
                    }
                }
            }
            val scaling = getClass(stylesheetString, "flair")
            val backScaling: Dimensions
            val offset: Dimensions
            if (scaling != null) {
                backScaling = getBackgroundScaling(scaling)
                offset = getBackgroundOffset(scaling)
                LogUtil.v("Offset is " + offset.width)
            } else {
                backScaling = Dimensions()
                offset = Dimensions()
            }
            if (!backScaling.missing && !backScaling.scale || !offset.missing && !offset.scale) {
                val loaded = getFlairImageLoader(context)!!.loadImageSync(
                    filename,
                    ImageSize(backScaling.width, backScaling.height)
                )
                if (loaded != null) {
                    val b: Bitmap
                    b = if (backScaling.missing || backScaling.width < offset.width) {
                        Bitmap.createScaledBitmap(
                            loaded, offset.width, offset.height,
                            false
                        )
                    } else {
                        Bitmap.createScaledBitmap(
                            loaded, backScaling.width, backScaling.height,
                            false
                        )
                    }
                    loadingComplete(b, sub, context, filename, flairsToGet)
                    loaded.recycle()
                }
            } else {
                val loadedB = getFlairImageLoader(context)!!
                    .loadImageSync(filename)
                if (loadedB != null) {
                    if (backScaling.scale) {
                        val width = backScaling.width
                        val height = loadedB.height
                        val scaledHeight = height * width / loadedB.width
                        loadingComplete(
                            Bitmap.createScaledBitmap(loadedB, width, scaledHeight, false), sub,
                            context, filename, flairsToGet
                        )
                        loadedB.recycle()
                    } else {
                        loadingComplete(loadedB, sub, context, filename, flairsToGet)
                    }
                }
            }
        }

        private fun loadingComplete(
            loadedImage: Bitmap?, sub: String, context: Context,
            filename: String?, flairsToGet: ArrayList<String>
        ) {
            if (loadedImage != null) {
                for (id in flairsToGet) {
                    var newBit: Bitmap? = null
                    val classDef = getClass(stylesheetString, "flair-$id") ?: break
                    var flairDimensions = getBackgroundSize(classDef)
                    if (flairDimensions.missing) {
                        flairDimensions = defaultDimension
                    }
                    prevDimension = flairDimensions
                    var flairLocation = getBackgroundPosition(classDef)
                    if (flairLocation.missing) flairLocation = defaultLocation
                    LogUtil.v(
                        "Flair: "
                                + id
                                + " size: "
                                + flairDimensions.width
                                + "x"
                                + flairDimensions.height
                                + " location: "
                                + flairLocation.x
                                + ":"
                                + flairLocation.y
                    )
                    try {
                        newBit = CropTransformation(
                            id, flairDimensions.width,
                            flairDimensions.height, flairLocation.x, flairLocation.y
                        ).transform(
                            loadedImage, flairLocation.isPercentage
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        getFlairImageLoader(context)!!.diskCache
                            .save(sub.lowercase() + ":" + id.lowercase(), newBit)
                        count += 1
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                loadedImage.recycle()
            } else {
                LogUtil.v("Loaded image is null for $filename")
            }
        }

        /**
         * Util function
         *
         * @return
         */
        val listOfFlairIds: List<String>
            get() {
                val flairId = Pattern.compile("\\.flair-(\\w+)\\s*(\\{|\\,|\\:|)")
                val matches = flairId.matcher(stylesheetString)
                val flairIds: MutableList<String> = ArrayList()
                while (matches.find()) {
                    if (!flairIds.contains(matches.group(1))) flairIds.add(matches.group(1))
                }
                flairIds.sort()
                return flairIds
            }
    }

    object FlairImageLoader : ImageLoader() {
        /** Returns singleton class instance  */
        @Volatile
        var instance: FlairImageLoader? = null
            get() {
                if (field == null) {
                    synchronized(ImageLoader::class.java) {
                        if (field == null) {
                            field = FlairImageLoader
                        }
                    }
                }
                return field
            }
            private set
    }
}
