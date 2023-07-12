package me.ccrama.redditslide

import android.content.Context
import ltd.ucode.network.data.IPost
import me.ccrama.redditslide.Adapters.SubmissionDisplay

/**
 * This interface provides methods for loading and retrieving submissions (such
 * as subreddit or multireddit submissions) to be called by views which require
 * a minimal amount of functionality.
 */
interface PostLoader {
    /**
     * Load more submissions, which will be available in the [.getPosts]
     * method.
     *
     * @param context   context to get connectivity information
     * @param display   the object that is displaying the view
     * @param reset     whether to reset the posts or add onto the existing set
     */
    fun loadMore(context: Context, display: SubmissionDisplay, reset: Boolean)

    /**
     * Get all currently loaded posts
     * @return
     */
    val posts: MutableList<IPost>

    /**
     * Returns whether there are more posts to load.
     * @return
     */
    fun hasMore(): Boolean
}
