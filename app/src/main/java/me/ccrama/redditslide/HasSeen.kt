package me.ccrama.redditslide

import ltd.ucode.network.SingleVote
import ltd.ucode.network.data.IPost
import ltd.ucode.slide.App
import ltd.ucode.slide.data.common.dao.SeenDao
import me.ccrama.redditslide.OpenRedditLink.RedditLinkType
import me.ccrama.redditslide.Synccit.SynccitRead
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection

object HasSeen {
    var hasSeen: HashSet<String>? = null
    var seenTimes: HashMap<String, Long>? = null

    @JvmStatic fun setHasSeenContrib(submissions: List<Contribution?>) {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        val m = App.contentDatabase.seen
        for (s in submissions) {
            if (s is Submission) {
                historyContains(s, m)
            }
        }
    }

    @JvmStatic fun setHasSeenSubmission(submissions: List<Submission>) {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        val m = App.contentDatabase.seen
        for (s in submissions) {
            historyContains(s, m)
        }
    }

    @JvmStatic private fun historyContains(s: Contribution, m: SeenDao) {
        var fullname = s.fullName
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }

        val contains = App.contentDatabase.seen.has(fullname)
        if (contains) {
            hasSeen!!.add(fullname)
            val value = m[fullname]
            try {
                if (value != null) seenTimes!![fullname] = java.lang.Long.valueOf(value)
            } catch (ignored: Exception) {
            }
        }
    }

    @JvmStatic fun getSeen(s: IPost): Boolean {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        var fullname = s.uri
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        return (hasSeen!!.contains(fullname)
                || s.myVote != SingleVote.NOVOTE)
    }

    @JvmStatic fun getSeen(s: Submission): Boolean {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        var fullname = s.fullName
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        return (hasSeen!!.contains(fullname)
                || SynccitRead.visitedIds.contains(fullname)) || s.dataNode.has("visited")
                && s.dataNode["visited"].asBoolean() || s.vote != VoteDirection.NO_VOTE
    }

    @JvmStatic fun getSeen(s: String): Boolean {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        var uri = OpenRedditLink.formatRedditUrl(s)
        var fullname = s
        if (uri != null) {
            val host = uri.host
            if (host!!.startsWith("np")) {
                uri = uri.buildUpon().authority(host.substring(2)).build()
            }
            val type = OpenRedditLink.getRedditLinkType(uri!!)
            val parts = uri.pathSegments
            when (type) {
                RedditLinkType.SHORTENED -> {
                    fullname = parts[0]
                }

                RedditLinkType.COMMENT_PERMALINK, RedditLinkType.SUBMISSION -> {
                    fullname = parts[3]
                }

                RedditLinkType.SUBMISSION_WITHOUT_SUB -> {
                    fullname = parts[1]
                }

                else -> {}
            }
        }
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        hasSeen!!.add(fullname)
        return hasSeen!!.contains(fullname) || SynccitRead.visitedIds.contains(fullname)
    }

    @JvmStatic fun getSeenTime(s: Submission): Long {
        if (hasSeen == null) {
            hasSeen = HashSet()
            seenTimes = HashMap()
        }
        var fullname = s.fullName
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        return if (seenTimes!!.containsKey(fullname)) {
            seenTimes!![fullname]!!
        } else {
            try {
                App.contentDatabase.seen[fullname].toLong()
            } catch (e: NumberFormatException) {
                0
            }
        }
    }

    @JvmStatic fun addSeen(fullname: String) {
        var fullname = fullname
        if (hasSeen == null) {
            hasSeen = HashSet()
        }
        if (seenTimes == null) {
            seenTimes = HashMap()
        }
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        hasSeen!!.add(fullname)
        seenTimes!![fullname] = System.currentTimeMillis()
        val result = App.contentDatabase.seen.insert(fullname, System.currentTimeMillis().toString())
        if (result == -1L) {
            App.contentDatabase.seen.update(fullname, System.currentTimeMillis().toString())
        }
        if (!fullname.contains("t1_")) {
            SynccitRead.newVisited.add(fullname)
            SynccitRead.visitedIds.add(fullname)
        }
    }

    @JvmStatic fun addSeenScrolling(fullname: String) {
        var fullname = fullname
        if (hasSeen == null) {
            hasSeen = HashSet()
        }
        if (seenTimes == null) {
            seenTimes = HashMap()
        }
        if (fullname.contains("t3_")) {
            fullname = fullname.substring(3)
        }
        hasSeen!!.add(fullname)
        seenTimes!![fullname] = System.currentTimeMillis()
        App.contentDatabase.seen.insert(fullname, System.currentTimeMillis().toString())
        if (!fullname.contains("t1_")) {
            SynccitRead.newVisited.add(fullname)
            SynccitRead.visitedIds.add(fullname)
        }
    }
}
