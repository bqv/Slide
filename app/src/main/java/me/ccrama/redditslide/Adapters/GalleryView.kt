package me.ccrama.redditslide.Adapters

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.cocosw.bottomsheet.BottomSheet
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.ContentType.Companion.getContentType
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.ui.commentsScreen.CommentsScreen
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Activities.AlbumPager
import me.ccrama.redditslide.Activities.FullscreenVideo
import me.ccrama.redditslide.Activities.Gallery
import me.ccrama.redditslide.Activities.GalleryImage
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.RedditGallery
import me.ccrama.redditslide.Activities.RedditGalleryPager
import me.ccrama.redditslide.Activities.Tumblr
import me.ccrama.redditslide.Activities.TumblrPager
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.PostMatch.openExternal
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openGif
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openImage
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openRedditContent
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.JsonUtil
import me.ccrama.redditslide.util.LinkUtil.copyUrl
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LinkUtil.openUrl
import me.ccrama.redditslide.util.LinkUtil.tryOpenWithVideoPlugin
import net.dean.jraw.models.Submission

class GalleryView(private val main: Gallery?, displayer: List<Submission>?, subreddit: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var paddingBottom = false
    var posts: ArrayList<Submission>?
    var subreddit: String

    init {
        posts = ArrayList(displayer)
        this.subreddit = subreddit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.gallery_image, parent, false)
        return AlbumViewHolder(v)
    }

    private fun getHeightFromAspectRatio(imageHeight: Int, imageWidth: Int, viewWidth: Int): Double {
        val ratio = imageHeight.toDouble() / imageWidth.toDouble()
        return viewWidth * ratio
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        if (holder is AlbumViewHolder) {
            val submission = posts!![i]
            if (submission.thumbnails != null && submission.thumbnails.source != null) {
                (main!!.applicationContext as App).imageLoader!!.displayImage(
                    submission.thumbnails.source.url,
                    holder.image,
                    ImageGridAdapter.options
                )
            } else {
                (main!!.applicationContext as App).imageLoader!!.displayImage(
                    submission.url,
                    holder.image,
                    ImageGridAdapter.options
                )
            }
            var h = 0.0
            var height = 0
            if (submission.thumbnails != null) {
                val source = submission.thumbnails.source
                if (source != null) {
                    h = getHeightFromAspectRatio(source.height, source.width, holder.image.width)
                    height = source.height
                }
            }
            holder.type.visibility = View.VISIBLE
            when (getContentType(submission)) {
                ContentType.Type.REDDIT_GALLERY, ContentType.Type.ALBUM -> holder.type.setImageResource(
                    R.drawable.ic_photo_library
                )

                ContentType.Type.EXTERNAL, ContentType.Type.LINK, ContentType.Type.REDDIT -> holder.type.setImageResource(
                    R.drawable.ic_public
                )

                ContentType.Type.SELF -> holder.type.setImageResource(R.drawable.ic_text_fields)
                ContentType.Type.EMBEDDED, ContentType.Type.GIF, ContentType.Type.STREAMABLE, ContentType.Type.VIDEO -> holder.type.setImageResource(
                    R.drawable.ic_play_arrow
                )

                else -> holder.type.visibility = View.GONE
            }
            if (h != 0.0) {
                if (h > 3200) {
                    holder.image.layoutParams =
                        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 3200)
                } else {
                    holder.image.layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        h.toInt()
                    )
                }
            } else {
                if (height > 3200) {
                    holder.image.layoutParams =
                        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 3200)
                } else {
                    holder.image.layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
            holder.comments.setOnClickListener { v: View ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                val i2 = Intent(main, CommentsScreen::class.java)
                i2.putExtra(
                    CommentsScreen.EXTRA_PAGE,
                    main.subredditPosts!!.posts.indexOf<Any>(submission)
                )
                i2.putExtra(CommentsScreen.EXTRA_SUBREDDIT, subreddit)
                i2.putExtra("fullname", submission.fullName)
                main.startActivity(i2)
            }
            holder.image.setOnLongClickListener {
                val b = BottomSheet.Builder(main)
                    .title(submission.url)
                    .grid()
                val attrs = intArrayOf(R.attr.tintColor)
                val ta = main.obtainStyledAttributes(attrs)
                val color = ta.getColor(0, Color.WHITE)
                val open = main.resources.getDrawable(R.drawable.ic_open_in_new)
                val share = main.resources.getDrawable(R.drawable.ic_share)
                val copy = main.resources.getDrawable(R.drawable.ic_content_copy)
                val drawableSet = listOf(open, share, copy)
                BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
                ta.recycle()
                b.sheet(
                    R.id.open_link,
                    open,
                    main.resources.getString(R.string.open_externally)
                )
                b.sheet(R.id.share_link, share, main.resources.getString(R.string.share_link))
                b.sheet(
                    R.id.copy_link,
                    copy,
                    main.resources.getString(R.string.submission_link_copy)
                )
                b.listener { _, which ->
                    when (which) {
                        R.id.open_link -> openExternally(submission.url)
                        R.id.share_link -> defaultShareText("", submission.url, main)
                        R.id.copy_link -> copyUrl(submission.url, main)
                    }
                }.show()
                return@setOnLongClickListener true
            }
            holder.image.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    val type = getContentType(submission)
                    if (!openExternal(submission.url) || type === ContentType.Type.VIDEO) {
                        when (type) {
                            ContentType.Type.STREAMABLE -> if (SettingValues.video) {
                                val myIntent = Intent(main, MediaView::class.java)
                                myIntent.putExtra(MediaView.SUBREDDIT, subreddit)
                                myIntent.putExtra(MediaView.EXTRA_URL, submission.url)
                                myIntent.putExtra(
                                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                    submission.title
                                )
                                main.startActivity(myIntent)
                            } else {
                                openExternally(submission.url)
                            }

                            ContentType.Type.IMGUR, ContentType.Type.DEVIANTART, ContentType.Type.XKCD, ContentType.Type.IMAGE -> openImage(
                                type,
                                main,
                                RedditSubmission(submission),
                                null,
                                holder.bindingAdapterPosition
                            )

                            ContentType.Type.EMBEDDED -> if (SettingValues.video) {
                                val data = CompatUtil.fromHtml(
                                    submission.dataNode["media_embed"]["content"].asText()
                                )
                                    .toString()
                                run {
                                    val i = Intent(main, FullscreenVideo::class.java)
                                    i.putExtra(FullscreenVideo.EXTRA_HTML, data)
                                    main.startActivity(i)
                                }
                            } else {
                                openExternally(submission.url)
                            }

                            ContentType.Type.REDDIT -> openRedditContent(submission.url, main)
                            ContentType.Type.LINK -> openUrl(
                                submission.url,
                                Palette.getColor(submission.subredditName),
                                main
                            )

                            ContentType.Type.ALBUM -> if (SettingValues.album) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(main, AlbumPager::class.java)
                                    i.putExtra(AlbumPager.SUBREDDIT, subreddit)
                                    i.putExtra(
                                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                        submission.title
                                    )
                                    i.putExtra(Album.EXTRA_URL, submission.url)
                                } else {
                                    i = Intent(main, Album::class.java)
                                    i.putExtra(Album.SUBREDDIT, subreddit)
                                    i.putExtra(Album.EXTRA_URL, submission.url)
                                    i.putExtra(
                                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                        submission.title
                                    )
                                }
                                main.startActivity(i)
                            } else {
                                openExternally(submission.url)
                            }

                            ContentType.Type.REDDIT_GALLERY -> if (SettingValues.album) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(main, RedditGalleryPager::class.java)
                                    i.putExtra(
                                        AlbumPager.SUBREDDIT,
                                        submission.subredditName
                                    )
                                } else {
                                    i = Intent(main, RedditGallery::class.java)
                                    i.putExtra(
                                        Album.SUBREDDIT,
                                        submission.subredditName
                                    )
                                }
                                i.putExtra(
                                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                    submission.title
                                )
                                i.putExtra(
                                    RedditGallery.SUBREDDIT,
                                    submission.subredditName
                                )
                                val urls = ArrayList<GalleryImage>()
                                val dataNode = submission.dataNode
                                if (dataNode.has("gallery_data")) {
                                    JsonUtil.getGalleryData(dataNode, urls)
                                }
                                val urlsBundle = Bundle()
                                urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, urls)
                                i.putExtras(urlsBundle)
                                main.startActivity(i)
                            } else {
                                openExternally(submission.url)
                            }

                            ContentType.Type.TUMBLR -> if (SettingValues.image) {
                                val i: Intent
                                if (albumSwipe) {
                                    i = Intent(main, TumblrPager::class.java)
                                    i.putExtra(TumblrPager.SUBREDDIT, subreddit)
                                } else {
                                    i = Intent(main, Tumblr::class.java)
                                    i.putExtra(Tumblr.SUBREDDIT, subreddit)
                                }
                                i.putExtra(Album.EXTRA_URL, submission.url)
                                main.startActivity(i)
                            } else {
                                openExternally(submission.url)
                            }

                            ContentType.Type.GIF -> openGif(
                                main, RedditSubmission(submission), holder.bindingAdapterPosition
                            )

                            ContentType.Type.NONE, ContentType.Type.SELF -> holder.comments.callOnClick()
                            ContentType.Type.VIDEO -> if (!tryOpenWithVideoPlugin(submission.url)) {
                                openUrl(
                                    submission.url,
                                    Palette.getStatusBarColor(), main
                                )
                            }

                            else -> {}
                        }
                    } else {
                        openExternally(submission.url)
                    }
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return if (posts == null) 0 else posts!!.size
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView
        val type: ImageView
        val comments: View

        init {
            comments = itemView.findViewById(R.id.comments)
            image = itemView.findViewById(R.id.image)
            type = itemView.findViewById(R.id.type)
        }
    }
}
