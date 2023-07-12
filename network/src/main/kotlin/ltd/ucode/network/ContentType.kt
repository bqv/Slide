package ltd.ucode.network

import net.dean.jraw.models.Submission
import java.net.URI
import java.net.URISyntaxException

open class ContentType {
    companion object {
        /**
         * Checks if `host` is contains by any of the provided `bases`
         *
         *
         * For example "www.youtube.com" contains "youtube.com" but not "notyoutube.com" or
         * "youtube.co.uk"
         *
         * @param host  A hostname from e.g. [URI.getHost]
         * @param bases Any number of hostnames to compare against `host`
         * @return If `host` contains any of `bases`
         */
        @JvmStatic
        fun hostContains(host: String?, vararg bases: String?): Boolean {
            if (host == null || host.isEmpty()) return false
            for (base in bases) {
                if (base == null || base.isEmpty()) continue
                val index = host.lastIndexOf(base)
                if (index < 0 || index + base.length != host.length) continue
                if (base.length == host.length || host[index - 1] == '.') return true
            }
            return false
        }

        @JvmStatic
        fun isGif(uri: URI): Boolean {
            return try {
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                (hostContains(host, "gfycat.com")
                        || hostContains(host, "v.redd.it")
                        || hostContains(host, "redgifs.com")
                        || path.endsWith(".gif")
                        || path.endsWith(".gifv")
                        || path.endsWith(".webm")
                        || path.endsWith(".mp4"))
            } catch (e: NullPointerException) {
                false
            }
        }

        @JvmStatic
        fun isImage(uri: URI): Boolean {
            return try {
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                host == "i.reddituploads.com"
                        || path.endsWith(".jpg")
                        || path.endsWith(".jpeg")
                        || path.endsWith(".png")
                        || path.endsWith(".bmp")
                        || path.endsWith(".webp")
                        || path.endsWith(".tiff")
                        || path.endsWith(".tif")
                        || path.endsWith(".svg") // unsure
            } catch (e: NullPointerException) {
                false
            }
        }

        @JvmStatic
        fun isAlbum(uri: URI): Boolean {
            return try {
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                hostContains(host, "imgur.com", "bildgur.de") && (path.startsWith("/a/")
                        || path.startsWith("/gallery/")
                        || path.startsWith("/g/")
                        || path.contains(","))
            } catch (e: NullPointerException) {
                false
            }
        }

        @JvmStatic
        fun isVideo(uri: URI): Boolean {
            return try {
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                hostContains(
                    host, "youtu.be", "youtube.com",
                    "youtube.co.uk"
                ) && !path.contains("/user/") && !path.contains("/channel/")
            } catch (e: NullPointerException) {
                false
            }
        }

        @JvmStatic
        fun isImgurLink(url: String?): Boolean {
            return try {
                val uri = URI(url)
                val host = uri.host.lowercase()
                (hostContains(host, "imgur.com", "bildgur.de")
                        && !isAlbum(uri)
                        && !isGif(uri)
                        && !isImage(uri))
            } catch (e: URISyntaxException) {
                false
            } catch (e: NullPointerException) {
                false
            }
        }

        @JvmStatic
        fun isRedditGallery(url: String): Boolean {
            return try {
                val uri = URI(url)
                val host = uri.host.lowercase()
                hostContains(host, "reddit.com", "redd.it") && url.contains("/gallery/")
            } catch (e: URISyntaxException) {
                false
            } catch (e: NullPointerException) {
                false
            }
        }

        /**
         * Attempt to determine the content type of a link from the URL
         *
         * @param url URL to get ContentType from
         * @return ContentType of the URL
         */
        @JvmStatic
        fun getContentType(url: String): Type {
            @Suppress("NAME_SHADOWING") var url = url
            if (!url.startsWith("//") && (url.startsWith("/") && url.length < 4
                        || url.startsWith("#spoiler") || url.startsWith("/spoiler")
                        || url.startsWith("#s-")
                        || url == "#s" || url == "#ln" || url == "#b" || url == "#sp")
            ) {
                return Type.SPOILER
            }
            if (url.startsWith("mailto:")) {
                return Type.EXTERNAL
            }
            if (url.startsWith("//")) url = "https:$url"
            if (url.startsWith("/")) url = "reddit.com$url"
            if (!url.contains("://")) url = "http://$url"
            return try {
                val uri = URI(url)
                val host = uri.host.lowercase()
                val scheme = uri.scheme.lowercase()
                if (hostContains(
                        host,
                        "v.redd.it"
                    ) || host == "reddit.com" && url.contains("reddit.com/video/")
                ) {
                    return if (url.contains("DASH_")) {
                        Type.VREDDIT_DIRECT
                    } else {
                        Type.VREDDIT_REDIRECT
                    }
                }
                if (isRedditGallery(url)) {
                    return Type.REDDIT_GALLERY
                }
                if (scheme != "http" && scheme != "https") {
                    return Type.EXTERNAL
                }
                if (isVideo(uri)) {
                    return Type.VIDEO
                }
                /*if (PostMatch.openExternal(url)) {
                    return Type.EXTERNAL
                }*/// TODO: reimplement in a less layer-violation-y way
                if (isGif(uri)) {
                    return Type.GIF
                }
                if (isImage(uri)) {
                    return Type.IMAGE
                }
                if (isAlbum(uri)) {
                    return Type.ALBUM
                }
                if (hostContains(host, "imgur.com", "bildgur.de")) {
                    return Type.IMGUR
                }
                if (hostContains(
                        host,
                        "xkcd.com"
                    ) && !hostContains("imgs.xkcd.com") && !hostContains(
                        "what-if.xkcd.com"
                    )
                ) {
                    return Type.XKCD
                }
                if (hostContains(host, "tumblr.com") && uri.path.contains("post")) {
                    return Type.TUMBLR
                }
                if (hostContains(host, "reddit.com", "redd.it")) {
                    return Type.REDDIT
                }
                if (hostContains(host, "deviantart.com")) {
                    return Type.DEVIANTART
                }
                if (hostContains(host, "streamable.com")) {
                    Type.STREAMABLE
                } else Type.LINK
            } catch (e: URISyntaxException) {
                if (e.message != null && (e.message!!.contains("Illegal character in fragment")
                            || e.message!!.contains("Illegal character in query")
                            || e.message!!
                        .contains(
                            "Illegal character in path"
                        ))
                ) {
                    Type.LINK
                } else Type.NONE
            } catch (e: NullPointerException) {
                if (e.message != null && (e.message!!.contains("Illegal character in fragment")
                            || e.message!!.contains("Illegal character in query")
                            || e.message!!
                        .contains(
                            "Illegal character in path"
                        ))
                ) {
                    Type.LINK
                } else Type.NONE
            }
        }

        /**
         * Attempts to determine the content of a submission, mostly based on the URL
         *
         * @param submission Submission to get the content type from
         * @return Content type of the Submission
         * @see .getContentType
         */
        @JvmStatic
        fun getContentType(submission: Submission?): Type {
            if (submission == null) {
                return Type.SELF //hopefully shouldn't be null, but catch it in case
            }
            if (submission.isSelfPost()) {
                return Type.SELF
            }
            val url: String = submission.getUrl()

            // TODO: Decide whether internal youtube links should be EMBEDDED or LINK
            /* Disable this for nowif (basicType.equals(Type.LINK) && submission.getDataNode().has("media_embed") && submission
                .getDataNode()
                .get("media_embed")
                .has("content")) {
            return Type.EMBEDDED;
        }*/return getContentType(url)
        }

        @JvmStatic
        fun displayImage(t: Type?): Boolean {
            return when (t) {
                Type.ALBUM, Type.REDDIT_GALLERY, Type.DEVIANTART, Type.IMAGE, Type.XKCD, Type.TUMBLR, Type.IMGUR, Type.SELF -> true
                else -> false
            }
        }

        @JvmStatic
        fun fullImage(t: Type?): Boolean {
            return when (t) {
                Type.ALBUM, Type.REDDIT_GALLERY, Type.DEVIANTART, Type.GIF, Type.IMAGE, Type.IMGUR, Type.STREAMABLE, Type.TUMBLR, Type.XKCD, Type.VIDEO, Type.SELF, Type.VREDDIT_DIRECT, Type.VREDDIT_REDIRECT -> true
                Type.EMBEDDED, Type.EXTERNAL, Type.LINK, Type.NONE, Type.REDDIT, Type.SPOILER -> false
                else -> false
            }
        }

        @JvmStatic
        fun mediaType(t: Type?): Boolean {
            return when (t) {
                Type.ALBUM, Type.REDDIT_GALLERY, Type.DEVIANTART, Type.GIF, Type.IMAGE, Type.TUMBLR, Type.XKCD, Type.IMGUR, Type.VREDDIT_DIRECT, Type.VREDDIT_REDIRECT, Type.STREAMABLE -> true
                else -> false
            }
        }

        @JvmStatic
        fun isImgurImage(lqUrl: String?): Boolean {
            return try {
                val uri = URI(lqUrl)
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                (host.contains("imgur.com") || host.contains("bildgur.de")) && (path.endsWith(
                    ".png"
                ) || path.endsWith(".jpg") || path.endsWith(".jpeg"))
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun isImgurHash(lqUrl: String?): Boolean {
            return try {
                val uri = URI(lqUrl)
                val host = uri.host.lowercase()
                val path = uri.path.lowercase()
                host.contains("imgur.com") && !(path.endsWith(".png") && !path.endsWith(
                    ".jpg"
                ) && !path.endsWith(".jpeg"))
            } catch (e: Exception) {
                false
            }
        }
    }

    enum class Type {
        ALBUM, REDDIT_GALLERY, DEVIANTART, EMBEDDED, EXTERNAL, GIF,
        VREDDIT_DIRECT, VREDDIT_REDIRECT, IMAGE, IMGUR, LINK, NONE,
        REDDIT, SELF, SPOILER, STREAMABLE, VIDEO, XKCD, TUMBLR
    }
}
