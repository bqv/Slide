package me.ccrama.redditslide

import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.data.IPost
import net.dean.jraw.models.Submission

val IPost.submission: Submission?
    get() = (this as? RedditSubmission)?.data
