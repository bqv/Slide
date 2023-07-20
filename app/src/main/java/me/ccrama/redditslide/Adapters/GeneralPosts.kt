package me.ccrama.redditslide.Adapters

import net.dean.jraw.models.Contribution

sealed class GeneralPosts<out T : Contribution> {
    var posts: MutableList<@UnsafeVariance T> = mutableListOf()
    var nomore = false
}
