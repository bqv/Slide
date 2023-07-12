package me.ccrama.redditslide

import android.content.SharedPreferences
import ltd.ucode.network.ContentType
import ltd.ucode.slide.SettingValues.alwaysExternal
import ltd.ucode.slide.SettingValues.domainFilters
import ltd.ucode.slide.SettingValues.flairFilters
import ltd.ucode.slide.SettingValues.showNSFWContent
import ltd.ucode.slide.SettingValues.subredditFilters
import ltd.ucode.slide.SettingValues.textFilters
import ltd.ucode.slide.SettingValues.titleFilters
import ltd.ucode.slide.SettingValues.userFilters
import ltd.ucode.network.data.IPost
import java.net.MalformedURLException
import java.net.URL

object PostMatch {
    /**
     * Checks if a string is totally or partially contained in a set of strings
     *
     * @param target     string to check
     * @param strings    set of strings to check in
     * @param totalMatch only allow total match, no partial matches
     * @return if the string is contained in the set of strings
     */
    @JvmStatic
    fun contains(target: String, strings: Set<String?>, totalMatch: Boolean): Boolean {
        // filters are always stored lowercase
        return if (totalMatch) {
            strings.contains(target.lowercase().trim { it <= ' ' })
        } else if (strings.contains(target.lowercase().trim { it <= ' ' })) {
            true
        } else {
            for (s in strings) {
                if (target.lowercase().trim { it <= ' ' }.contains(s!!)) {
                    return true
                }
            }
            false
        }
    }

    /**
     * Checks if a domain should be filtered or not: returns true if the target domain ends with the
     * comparison domain and if supplied, target path begins with the comparison path
     *
     * @param target  URL to check
     * @param strings The URLs to check against
     * @return If the target is covered by any strings
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun isDomain(target: String?, strings: Set<String>): Boolean {
        val domain = URL(target)
        for (s in strings) {
            if (!s.contains("/")) {
                return if (ContentType.hostContains(domain.host, s)) {
                    true
                } else {
                    continue
                }
            }
            try {
                val url = if (!s.contains("://")) {
                    "http://$s"
                } else {
                    s
                }
                val comparison = URL(url.lowercase())
                if (ContentType.hostContains(domain.host, comparison.host)
                    && domain.path.startsWith(comparison.path)
                ) {
                    return true
                }
            } catch (ignored: MalformedURLException) {
            }
        }
        return false
    }

    @JvmStatic
    fun openExternal(url: String): Boolean {
        return try {
            isDomain(url.lowercase(), alwaysExternal)
        } catch (e: MalformedURLException) {
            false
        }
    }

    var filters: SharedPreferences? = null
    fun doesMatch(s: IPost, baseSubreddit: String?, ignore18: Boolean): Boolean {
        var baseSubreddit = baseSubreddit
        if (Hidden.id.contains(s.uri)) return true // if it's hidden we're not going to show it regardless
        val title = s.title
        val body = s.body
        val domain = s.link
        val subreddit = s.groupName
        val flair = if (s.flair.text != null) s.flair.text else ""
        if (contains(title, titleFilters, false)) return true
        if (contains(body.orEmpty(), textFilters, false)) return true
        if (contains(s.user.name, userFilters, false)) return true
        try {
            if (isDomain(domain.orEmpty().lowercase(), domainFilters)) return true
        } catch (ignored: MalformedURLException) {
        }
        if (!subreddit.equals(baseSubreddit, ignoreCase = true) && contains(
                subreddit,
                subredditFilters,
                true
            )
        ) {
            return true
        }
        var contentMatch = false
        if (baseSubreddit == null || baseSubreddit.isEmpty()) {
            baseSubreddit = "frontpage"
        }
        baseSubreddit = baseSubreddit.lowercase()
        val gifs = isGif(baseSubreddit)
        val images = isImage(baseSubreddit)
        val nsfw = isNsfw(baseSubreddit)
        val albums = isAlbums(baseSubreddit)
        val urls = isUrls(baseSubreddit)
        val selftext = isSelftext(baseSubreddit)
        val videos = isVideo(baseSubreddit)
        if (s.isNsfw) {
            if (!showNSFWContent) {
                contentMatch = true
            }
            if (ignore18) {
                contentMatch = false
            }
            if (nsfw) {
                contentMatch = true
            }
        }
        when (s.contentType) {
            ContentType.Type.REDDIT, ContentType.Type.EMBEDDED, ContentType.Type.LINK -> if (urls) {
                contentMatch = true
            }

            ContentType.Type.SELF, ContentType.Type.NONE -> if (selftext) {
                contentMatch = true
            }

            ContentType.Type.REDDIT_GALLERY, ContentType.Type.ALBUM -> if (albums) {
                contentMatch = true
            }

            ContentType.Type.IMAGE, ContentType.Type.DEVIANTART, ContentType.Type.IMGUR, ContentType.Type.XKCD -> if (images) {
                contentMatch = true
            }

            ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.GIF -> if (gifs) {
                contentMatch = true
            }

            ContentType.Type.STREAMABLE, ContentType.Type.VIDEO -> if (videos) {
                contentMatch = true
            }

            else -> {}
        }
        if (flair.isNotEmpty()) for (flairText in flairFilters) {
            if (flairText.lowercase().startsWith(baseSubreddit)) {
                val split =
                    flairText.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (split[0].equals(baseSubreddit, ignoreCase = true)) {
                    if (flair.equals(split[1].trim { it <= ' ' }, ignoreCase = true)) {
                        contentMatch = true
                        break
                    }
                }
            }
        }
        return contentMatch
    }

    @JvmStatic
    fun doesMatch(s: IPost): Boolean {
        val title = s.title
        val body = s.body
        val domain = s.link
        val subreddit = s.groupName
        var domainc = false
        val titlec = contains(title, titleFilters, false)
        val bodyc = contains(body.orEmpty(), textFilters, false)
        try {
            domainc = isDomain(domain.orEmpty().lowercase(), domainFilters)
        } catch (ignored: MalformedURLException) {
        }
        val subredditc =
            subreddit.isNotEmpty() && contains(subreddit, subredditFilters, true)
        return titlec || bodyc || domainc || subredditc
    }

    fun setChosen(values: BooleanArray, subreddit: String) {
        val subreddit = subreddit.lowercase()
        val e = filters!!.edit()
        e.putBoolean(subreddit + "_gifsFilter", values[2])
        e.putBoolean(subreddit + "_albumsFilter", values[1])
        e.putBoolean(subreddit + "_imagesFilter", values[0])
        e.putBoolean(subreddit + "_nsfwFilter", values[6])
        e.putBoolean(subreddit + "_selftextFilter", values[5])
        e.putBoolean(subreddit + "_urlsFilter", values[4])
        e.putBoolean(subreddit + "_videoFilter", values[3])
        e.apply()
    }

    fun isGif(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_gifsFilter", false)
    }

    fun isImage(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_imagesFilter", false)
    }

    fun isAlbums(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_albumsFilter", false)
    }

    fun isNsfw(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_nsfwFilter", false)
    }

    fun isSelftext(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_selftextFilter", false)
    }

    fun isUrls(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_urlsFilter", false)
    }

    fun isVideo(baseSubreddit: String): Boolean {
        return filters!!.getBoolean(baseSubreddit + "_videoFilter", false)
    }
}
