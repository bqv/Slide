package me.ccrama.redditslide

import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.network.data.IPost
import net.dean.jraw.models.Submission

val IPost.submission: Submission?
    get() = (this as? RedditSubmission)?.data
