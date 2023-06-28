package me.ccrama.redditslide.util

import android.content.Context
import ltd.ucode.slide.App
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.ContentType
import net.dean.jraw.models.Submission.ThumbnailType
import net.dean.jraw.models.Thumbnails

object PhotoLoader {
    fun loadPhoto(c: Context, submission: IPost) {
        val url: String
        val type = submission.contentType
        val thumbnails = submission.thumbnails
        val thumbnailType = submission.thumbnailType
        if (thumbnails != null) {
            if (type == ContentType.Type.IMAGE || type == ContentType.Type.SELF || thumbnailType == ThumbnailType.URL) {
                url = if (type == ContentType.Type.IMAGE) {
                    if ((!NetworkUtil.isConnectedWifi(c) && SettingValues.lowResMobile
                                || SettingValues.lowResAlways) && thumbnails.variations != null && thumbnails.variations.isNotEmpty()
                    ) {
                        val length = thumbnails.variations.size
                        if (SettingValues.lqLow && length >= 3) {
                            getThumbnailUrl(thumbnails.variations[2])
                        } else if (SettingValues.lqMid && length >= 4) {
                            getThumbnailUrl(thumbnails.variations[3])
                        } else if (length >= 5) {
                            getThumbnailUrl(thumbnails.variations[length - 1])
                        } else {
                            getThumbnailUrl(thumbnails.source)
                        }
                    } else {
                        if (submission.hasPreview) { //Load the preview image which has probably already been cached in memory instead of the direct link
                            submission.preview!!
                        } else {
                            submission.url!!
                        }
                    }
                } else {
                    if ((!NetworkUtil.isConnectedWifi(c) && SettingValues.lowResMobile
                                || SettingValues.lowResAlways)
                        && thumbnails.variations.isNotEmpty()
                    ) {
                        val length = thumbnails.variations.size
                        if (SettingValues.lqLow && length >= 3) {
                            getThumbnailUrl(thumbnails.variations[2])
                        } else if (SettingValues.lqMid && length >= 4) {
                            getThumbnailUrl(thumbnails.variations[3])
                        } else if (length >= 5) {
                            getThumbnailUrl(thumbnails.variations[length - 1])
                        } else {
                            getThumbnailUrl(thumbnails.source)
                        }
                    } else {
                        getThumbnailUrl(thumbnails.source)
                    }
                }
                loadImage(c, url)
            }
        }
    }

    private fun getThumbnailUrl(thumbnail: Thumbnails.Image): String {
        return CompatUtil.fromHtml(thumbnail.url).toString() //unescape url characters
    }

    private fun loadImage(context: Context, url: String) {
        val appContext = context.applicationContext as App
        appContext.imageLoader!!.loadImage(url, null)
    }

    fun loadPhotos(c: Context, submissions: List<IPost>) {
        for (submission in submissions) {
            loadPhoto(c, submission)
        }
    }
}
