package me.ccrama.redditslide.Adapters

import ltd.ucode.slide.data.IPost

/**
 * Interface to provide methods for updating an object when new submissions
 * have been loaded.
 */
interface SubmissionDisplay {
    /**
     * Called when the update was done online.
     * @param submissions   the updated list of submissions
     * @param startIndex    the index of the first new submission
     */
    fun updateSuccess(submissions: List<IPost>, startIndex: Int)

    /**
     * Called when the update was offline.
     * @param submissions   the updated list of submissions
     * @param cacheTime     the last time updated (unix time?)
     */
    fun updateOffline(submissions: List<IPost>, cacheTime: Long)

    /**
     * Called when the update was offline but failed (e.g. no subreddit was cached).
     */
    fun updateOfflineError()

    /**
     * Called when the update was done online but failed (e.g. network connection).
     */
    fun updateError()
    fun updateViews()
    fun onAdapterUpdated()
}
