package me.ccrama.redditslide

import android.content.Context
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ltd.ucode.lemmy.data.LemmyPostWithComments
import ltd.ucode.slide.App
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.CommentCacheAsync.CommentStore
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Collections

class OfflineSubreddit {
    var time: Long = 0
    var submissions: ArrayList<IPost>? = null
    var subreddit: String? = null
    var base = false
    fun overwriteSubmissions(data: List<IPost>?): OfflineSubreddit {
        submissions = ArrayList(data)
        return this
    }

    fun isStored(name: String, c: Context?): Boolean {
        return File(getCacheDirectory(c).toString() + File.separator + name).exists()
    }

    fun writeToMemory(c: Context?) {
        if (cache == null) cache = HashMap()
        if (subreddit != null) {
            val title = subreddit!!.lowercase() + "," + if (base) 0 else time
            val permalinks = StringBuilder()
            cache!![title] = this
            for (sub in ArrayList<IPost>(submissions)) {
                permalinks.append(sub.permalink).append(",")
                if (!isStored(sub.permalink, c)) {
                    //writeSubmissionToStorage(sub, Json.encodeToJsonElement(sub), c)
                }
            }
            if (permalinks.isNotEmpty()) {
                App.cachedData!!.edit()
                    .putString(title, permalinks.substring(0, permalinks.length - 1))
                    .apply()
            }
            cache!![title] = this
        }
    }

    fun writeToMemoryNoStorage() {
        if (cache == null) cache = HashMap()
        if (subreddit != null) {
            val title = subreddit!!.lowercase() + "," + if (base) 0 else time
            val permalinks = StringBuilder()
            for (sub in submissions!!) {
                permalinks.append(sub.permalink).append(",")
            }
            if (permalinks.isNotEmpty()) {
                App.cachedData!!.edit()
                    .putString(title, permalinks.substring(0, permalinks.length - 1))
                    .apply()
            }
            cache!![title] = this
        }
    }

    fun writeToMemory(names: List<String?>) {
        if (subreddit != null && names.isNotEmpty()) {
            val title = subreddit!!.lowercase() + "," + time
            val permalinksBuilder = StringBuilder()
            for (sub in names) {
                permalinksBuilder.append(sub).append(",")
            }
            val permalinks = permalinksBuilder.toString()
            if (subreddit == CommentCacheAsync.SAVED_SUBMISSIONS) {
                val offlineSubs = App.cachedData!!.all
                for (offlineSub in offlineSubs.keys) {
                    if (offlineSub.contains(CommentCacheAsync.SAVED_SUBMISSIONS)) {
                        savedSubmissionsSubreddit = offlineSub
                        break
                    }
                }
                var savedSubmissions = App.cachedData!!.getString(
                    savedSubmissionsSubreddit,
                    permalinks
                )
                App.cachedData!!.edit().remove(savedSubmissionsSubreddit).apply()
                if (savedSubmissions != permalinks) {
                    savedSubmissions = permalinks + savedSubmissions
                }
                saveToCache(title, savedSubmissions)
            } else {
                saveToCache(title, permalinks)
            }
        }
    }

    fun deleteFromMemory(name: String) {
        if (subreddit != null) {
            val title = subreddit!!.lowercase() + "," + time
            if (subreddit == CommentCacheAsync.SAVED_SUBMISSIONS) {
                val offlineSubs = App.cachedData!!.all
                for (offlineSub in offlineSubs.keys) {
                    if (offlineSub.contains(CommentCacheAsync.SAVED_SUBMISSIONS)) {
                        savedSubmissionsSubreddit = offlineSub
                        break
                    }
                }
                val savedSubmissions = App.cachedData!!.getString(
                    savedSubmissionsSubreddit,
                    name
                )
                if (savedSubmissions != name) {
                    App.cachedData!!.edit().remove(savedSubmissionsSubreddit).apply()
                    val modifiedSavedSubmissions = savedSubmissions!!.replace("$name,", "")
                    saveToCache(title, modifiedSavedSubmissions)
                }
            }
        }
    }

    private fun saveToCache(title: String, submissions: String?) {
        App.cachedData!!.edit().putString(title, submissions).apply()
    }

    fun clearPost(s: IPost) {
        if (submissions != null) {
            var toRemove: IPost? = null
            for (s2 in submissions!!) {
                if (s.permalink == s2.permalink) {
                    toRemove = s2
                }
            }
            if (toRemove != null) {
                submissions!!.remove(toRemove)
            }
        }
    }

    var savedIndex = 0
    var savedSubmission: IPost? = null

    fun hideMulti(index: Int) {
        if (submissions != null) {
            submissions!!.removeAt(index)
        }
    }

    @JvmOverloads
    fun hide(index: Int, save: Boolean = true) {
        if (submissions != null) {
            savedSubmission = submissions!![index]
            submissions!!.removeAt(index)
            savedIndex = index
            writeToMemoryNoStorage()
        }
    }

    fun unhideLast() {
        if (submissions != null && savedSubmission != null) {
            submissions!!.add(savedIndex, savedSubmission!!)
            writeToMemoryNoStorage()
        }
    }

    class MultiComparator<T> : Comparator<T> {
        override fun compare(o1: T, o2: T): Int {
            val first = (o1 as String).split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1].toDouble()
            val second = (o2 as String).split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1].toDouble()
            return if (first >= second) if (first == second) 0 else -1 else 1
        }
    }

    companion object {
        var currentid = 0L
        private var savedSubmissionsSubreddit = ""
        fun writeSubmission(node: CommentStore, s: IPost, c: Context?) {
            writeSubmissionToStorage(s, node, c)
        }

        var cacheDirectory: File? = null
        fun getCacheDirectory(context: Context?): File? {
            if (cacheDirectory == null && context != null) {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && context.externalCacheDir != null) {
                    cacheDirectory = context.externalCacheDir
                }
                cacheDirectory = context.cacheDir
            }
            return cacheDirectory
        }

        fun writeSubmissionToStorage(s: IPost, comments: CommentStore, c: Context?) {
            val toStore = File(getCacheDirectory(c).toString() + File.separator + s.permalink)
            try {
                val writer = FileWriter(toStore)
                writer.append(Json.encodeToString(s.withComments(comments)))
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getSubreddit(subreddit: String?, offline: Boolean, c: Context?): OfflineSubreddit? {
            return getSubreddit(subreddit, 0L, offline, c)
        }

        fun getSubNoLoad(s: String): OfflineSubreddit {
            var s = s
            s = s.lowercase()
            val o = OfflineSubreddit()
            o.subreddit = s.lowercase()
            o.base = true
            o.time = 0
            o.submissions = ArrayList()
            return o
        }

        private var cache: HashMap<String, OfflineSubreddit>? = null
        fun getSubreddit(
            subreddit: String?, time: Long, offline: Boolean,
            c: Context?
        ): OfflineSubreddit? {
            var subreddit = subreddit
            if (cache == null) cache = HashMap()
            val title: String
            if (subreddit != null) {
                title = subreddit.lowercase() + "," + time
            } else {
                title = ""
                subreddit = ""
            }
            return if (cache!!.containsKey(title)) {
                cache!![title]
            } else {
                subreddit = subreddit.lowercase()
                val o = OfflineSubreddit()
                o.subreddit = subreddit.lowercase()
                if (time == 0L) {
                    o.base = true
                }
                o.time = time
                val split = App.cachedData!!.getString(subreddit.lowercase() + "," + time, "")
                    ?.run { split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() }.orEmpty()
                if (split.isNotEmpty()) {
                    o.time = time
                    o.submissions = ArrayList()
                    val mapperBase = ObjectMapper()
                    val reader = mapperBase.reader()
                    for (s in split) {
                        if (s.isNotEmpty()) {
                            try {
                                val sub = getSubmissionFromStorage(
                                    if (!s.contains("_")) "t3_$s" else s,
                                    c, offline, reader)
                                if (sub != null) {
                                    o.submissions!!.add(sub)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    o.submissions = ArrayList()
                }
                cache!![title] = o
                o
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getSubmissionFromStorage(
            permalink: String, c: Context?, offline: Boolean,
            reader: ObjectReader
        ): IPost? {
            val gotten = getStringFromFile(permalink, c)
            if (gotten.isEmpty()) return null
            return Json.decodeFromString<LemmyPostWithComments>(gotten)
        }

        fun getStringFromFile(name: String, c: Context?): String {
            val f = File(getCacheDirectory(c).toString() + File.separator + name)
            if (f.exists()) {
                try {
                    val reader = BufferedReader(FileReader(f))
                    val chars = CharArray(f.length().toInt())
                    reader.read(chars)
                    reader.close()
                    return String(chars)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                return ""
            }
            return ""
        }

        fun newSubreddit(subreddit: String): OfflineSubreddit {
            var subreddit = subreddit
            subreddit = subreddit.lowercase()
            val o = OfflineSubreddit()
            o.subreddit = subreddit.lowercase()
            o.base = false
            o.time = System.currentTimeMillis()
            o.submissions = ArrayList()
            return o
        }

        fun getAll(subreddit: String): ArrayList<String?> {
            var subreddit = subreddit
            subreddit = subreddit.lowercase()
            val base = ArrayList<String?>()
            for (s in App.cachedData!!.all.keys) {
                if (s.startsWith(subreddit) && s.contains(",")) {
                    base.add(s)
                }
            }
            Collections.sort(base, MultiComparator<Any?>())
            return base
        }

        val all: ArrayList<String>
            get() {
                val keys = ArrayList<String>()
                for (s in App.cachedData!!.all.keys) {
                    if (s.contains(",") && !s.startsWith("multi")) {
                        keys.add(s)
                    }
                }
                return keys
            }
        val allFormatted: ArrayList<String>
            get() {
                val keys = ArrayList<String>()
                for (s in App.cachedData!!.all.keys) {
                    if (s.contains(",") && !keys.contains(
                            s.substring(
                                0,
                                s.indexOf(",")
                            )
                        ) && !s.startsWith(
                            "multi"
                        )
                    ) {
                        keys.add(s.substring(0, s.indexOf(",")))
                    }
                }
                return keys
            }
    }
}
