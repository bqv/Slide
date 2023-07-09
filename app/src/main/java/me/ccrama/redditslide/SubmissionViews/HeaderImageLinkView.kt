package me.ccrama.redditslide.SubmissionViews

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.cocosw.bottomsheet.BottomSheet
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.isNSFWEnabled
import ltd.ucode.slide.SettingValues.isPicsEnabled
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.ContentType
import me.ccrama.redditslide.ForceTouch.PeekView
import me.ccrama.redditslide.ForceTouch.PeekViewActivity
import me.ccrama.redditslide.ForceTouch.builder.Peek
import me.ccrama.redditslide.ForceTouch.builder.PeekViewOptions
import me.ccrama.redditslide.ForceTouch.callback.SimpleOnPeek
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.views.PeekMediaView
import me.ccrama.redditslide.views.TransparentTagTextView
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.NetworkUtil
import net.dean.jraw.models.Submission.ThumbnailType
import java.util.Arrays

class HeaderImageLinkView : RelativeLayout {
    var loadedUrl: String? = null
    var lq = false
    var thumbImage2: ImageView? = null
    var secondTitle: TextView? = null
    var secondSubTitle: TextView? = null
    var wrapArea: View? = null
        set(v: View?) {
            field = v
            secondTitle = v!!.findViewById(R.id.contenttitle)
            secondSubTitle = v!!.findViewById(R.id.contenturl)
        }
    var lastDone = ""
    var type: ContentType.Type? = null
    var bigOptions = DisplayImageOptions.Builder().resetViewBeforeLoading(false)
        .cacheOnDisk(true)
        .imageScaleType(ImageScaleType.EXACTLY)
        .cacheInMemory(false)
        .displayer(FadeInBitmapDisplayer(250))
        .build()
    var clickHandled = false
    @JvmField var handler: Handler? = null
    var event: MotionEvent? = null
    var longClicked: Runnable? = null
    var position = 0f
    private var title: TextView? = null
    private var info: TextView? = null
    var backdrop: ImageView? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    var thumbUsed = false
    fun doImageAndText(submission: IPost, full: Boolean, baseSub: String?, news: Boolean) {
        val fullImage = ContentType.fullImage(type)
        thumbUsed = false
        visibility = VISIBLE
        var url = ""
        var forceThumb = false
        thumbImage2!!.setImageResource(android.R.color.transparent)
        val loadLq = (!NetworkUtil.isConnectedWifi(context) && SettingValues.lowResMobile
                || SettingValues.lowResAlways)

        /* todo, maybe if(thumbImage2 != null && thumbImage2 instanceof RoundImageTriangleView)
            switch (ContentType.getContentType(submission)) {
            case ALBUM:
                ((RoundImageTriangleView)(thumbImage2)).setFlagColor(R.color.md_blue_300);
                break;
            case EXTERNAL:
            case LINK:
            case REDDIT:
                ((RoundImageTriangleView)(thumbImage2)).setFlagColor(R.color.md_red_300);
                break;
            case SELF:
                ((RoundImageTriangleView)(thumbImage2)).setFlagColor(R.color.md_grey_300);
                break;
            case EMBEDDED:
            case GIF:
            case STREAMABLE:
            case VIDEO:
                ((RoundImageTriangleView)(thumbImage2)).setFlagColor(R.color.md_green_300);
                break;
            default:
                ((RoundImageTriangleView)(thumbImage2)).setFlagColor(Color.TRANSPARENT);
                break;
        }*/if (type == ContentType.Type.SELF && SettingValues.hideSelftextLeadImage
            || SettingValues.noImages && submission.link == null
        ) {
            visibility = GONE
            if (wrapArea != null) wrapArea!!.visibility = GONE
            thumbImage2!!.visibility = GONE
        } else {
            if (submission.thumbnails != null) {
                val height = submission.thumbnails!!.source.height
                val width = submission.thumbnails!!.source.width
                if (full) {
                    if (!fullImage && height < dpToPx(50) && type != ContentType.Type.SELF) {
                        forceThumb = true
                    } else if (SettingValues.cropImage) {
                        backdrop!!.layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            dpToPx(200)
                        )
                    } else {
                        val h = getHeightFromAspectRatio(height, width)
                        if (h != 0.0) {
                            if (h > 3200) {
                                backdrop!!.layoutParams = LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    3200
                                )
                            } else {
                                backdrop!!.layoutParams =
                                    LayoutParams(LayoutParams.MATCH_PARENT, h.toInt())
                            }
                        } else {
                            backdrop!!.layoutParams = LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT
                            )
                        }
                    }
                } else if (SettingValues.bigPicCropped) {
                    if (!fullImage && height < dpToPx(50)) {
                        forceThumb = true
                    } else {
                        backdrop!!.layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            dpToPx(200)
                        )
                    }
                } else if (fullImage || height >= dpToPx(50)) {
                    val h = getHeightFromAspectRatio(height, width)
                    if (h != 0.0) {
                        if (h > 3200) {
                            backdrop!!.layoutParams = LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                3200
                            )
                        } else {
                            backdrop!!.layoutParams =
                                LayoutParams(LayoutParams.MATCH_PARENT, h.toInt())
                        }
                    } else {
                        backdrop!!.layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT
                        )
                    }
                } else {
                    forceThumb = true
                }
            }
            val thumbnail = submission.thumbnailUrl
            val thumbnailType: ThumbnailType = if (submission.thumbnailUrl != null) {
                submission.thumbnailType
            } else {
                ThumbnailType.NONE
            }
            /*
            val node = submission.dataNode
            if (!SettingValues.ignoreSubSetting && node != null && node.has("sr_detail") && node["sr_detail"].has(
                    "show_media"
                ) && !node["sr_detail"]["show_media"].asBoolean()
            ) {
                thumbnailType = ThumbnailType.NONE
            }
             */
            if (SettingValues.noImages && loadLq) {
                visibility = GONE
                if (!full && submission.link != null) {
                    thumbImage2!!.visibility = VISIBLE
                } else {
                    if (full && submission.link != null) wrapArea!!.visibility = VISIBLE
                }
                thumbImage2!!.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.web)
                )
                thumbUsed = true
            } else if (((submission.isNsfw && isNSFWEnabled)
                        || baseSub != null && submission.isNsfw && SettingValues.hideNSFWCollection)
                && (baseSub == "frontpage" || baseSub == "all" || baseSub!!.contains("+") || baseSub == "popular")
            ) {
                visibility = GONE
                if (!full || forceThumb) {
                    thumbImage2!!.visibility = VISIBLE
                } else {
                    wrapArea!!.visibility = VISIBLE
                }
                if (submission.link == null && full) {
                    wrapArea!!.visibility = GONE
                } else {
                    thumbImage2!!.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.nsfw)
                    )
                    thumbUsed = true
                }
                loadedUrl = submission.link
            } else if (submission.isSpoiler) {
                visibility = GONE
                if (!full || forceThumb) {
                    thumbImage2!!.visibility = VISIBLE
                } else {
                    wrapArea!!.visibility = VISIBLE
                }
                if (submission.link == null && full) {
                    wrapArea!!.visibility = GONE
                } else {
                    thumbImage2!!.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.spoiler)
                    )
                    thumbUsed = true
                }
                loadedUrl = submission.link
            } else if (type != ContentType.Type.IMAGE && type != ContentType.Type.SELF && thumbnail != null && thumbnailType != ThumbnailType.URL
                || thumbnail.isNullOrEmpty() && submission.link != null
            ) {
                visibility = GONE
                if (!full) {
                    thumbImage2!!.visibility = VISIBLE
                } else {
                    wrapArea!!.visibility = VISIBLE
                }
                thumbImage2!!.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.web)
                )
                thumbUsed = true
                loadedUrl = submission.link
            } else if (type == ContentType.Type.IMAGE && thumbnail?.isNotEmpty() == true) {
                if (loadLq && submission.thumbnails != null && submission.thumbnails!!.variations != null && submission.thumbnails!!.variations.isNotEmpty()) {
                    if (ContentType.isImgurImage(submission.link)) {
                        url = submission.link!!
                        url = url.substring(
                            0,
                            url.lastIndexOf(".")
                        ) + (if (SettingValues.lqLow) "m" else if (SettingValues.lqMid) "l" else "h") + url.substring(
                            url.lastIndexOf(".")
                        )
                    } else {
                        val length = submission.thumbnails!!.variations.size
                        url = if (SettingValues.lqLow && length >= 3) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[2].url
                            )
                                .toString() //unescape url characters
                        } else if (SettingValues.lqMid && length >= 4) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[3].url
                            )
                                .toString() //unescape url characters
                        } else if (length >= 5) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[length - 1].url
                            )
                                .toString() //unescape url characters
                        } else {
                            CompatUtil.fromHtml(submission.thumbnails!!.source.url)
                                .toString() //unescape url characters
                        }
                    }
                    lq = true
                } else {
                    url =
                        if (submission.hasPreview) { //Load the preview image which has probably already been cached in memory instead of the direct link
                            submission.preview!!
                        } else {
                            submission.link!!
                        }
                }
                if (!full && !isPicsEnabled(baseSub) || forceThumb) {
                    if (submission.link != null || full) {
                        if (!full) {
                            thumbImage2!!.visibility = VISIBLE
                        } else {
                            wrapArea!!.visibility = VISIBLE
                        }
                        loadedUrl = url
                        if (!full) {
                            (context.applicationContext as App).imageLoader!!
                                .displayImage(url, thumbImage2)
                        } else {
                            (context.applicationContext as App).imageLoader!!
                                .displayImage(url, thumbImage2, bigOptions)
                        }
                    } else {
                        thumbImage2!!.visibility = GONE
                    }
                    visibility = GONE
                } else {
                    loadedUrl = url
                    if (!full) {
                        (context.applicationContext as App).imageLoader!!
                            .displayImage(url, backdrop)
                    } else {
                        (context.applicationContext as App).imageLoader!!
                            .displayImage(url, backdrop, bigOptions)
                    }
                    visibility = VISIBLE
                    if (!full) {
                        thumbImage2!!.visibility = GONE
                    } else {
                        wrapArea!!.visibility = GONE
                    }
                }
            } else if (submission.thumbnails != null) {
                if (loadLq && submission.thumbnails!!.variations.isNotEmpty()) {
                    if (ContentType.isImgurImage(submission.link)) {
                        url = submission.link!!
                        url = url.substring(
                            0,
                            url.lastIndexOf(".")
                        ) + (if (SettingValues.lqLow) "m" else if (SettingValues.lqMid) "l" else "h") + url.substring(
                            url.lastIndexOf(".")
                        )
                    } else {
                        val length = submission.thumbnails!!.variations.size
                        url = if (SettingValues.lqLow && length >= 3) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[2].url
                            )
                                .toString() //unescape url characters
                        } else if (SettingValues.lqMid && length >= 4) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[3].url
                            )
                                .toString() //unescape url characters
                        } else if (length >= 5) {
                            CompatUtil.fromHtml(
                                submission.thumbnails!!.variations[length - 1].url
                            )
                                .toString() //unescape url characters
                        } else {
                            CompatUtil.fromHtml(submission.thumbnails!!.source.url)
                                .toString() //unescape url characters
                        }
                    }
                    lq = true
                } else {
                    url =
                        (if (submission.thumbnails!!.source.url.isEmpty()) submission.thumbnailUrl else submission.thumbnails!!.source.url)?.let {
                            CompatUtil.fromHtml(it)
                                .toString()
                        }!! //unescape url characters
                }
                if (!isPicsEnabled(baseSub) && !full || forceThumb || news && submission.score < 5000) {
                    if (!full) {
                        thumbImage2!!.visibility = VISIBLE
                    } else {
                        wrapArea!!.visibility = VISIBLE
                    }
                    loadedUrl = url
                    (context.applicationContext as App).imageLoader!!
                        .displayImage(url, thumbImage2)
                    visibility = GONE
                } else {
                    loadedUrl = url
                    if (!full) {
                        (context.applicationContext as App).imageLoader!!
                            .displayImage(url, backdrop)
                    } else {
                        (context.applicationContext as App).imageLoader!!
                            .displayImage(url, backdrop, bigOptions)
                    }
                    visibility = VISIBLE
                    if (!full) {
                        thumbImage2!!.visibility = GONE
                    } else {
                        wrapArea!!.visibility = GONE
                    }
                }
            } else if (thumbnail != null && submission.thumbnailUrl != null && (submission.thumbnailType == ThumbnailType.URL || thumbnail != null) && submission.isNsfw && isNSFWEnabled) {
                url = submission.thumbnailUrl!!
                if (!full) {
                    thumbImage2!!.visibility = VISIBLE
                } else {
                    wrapArea!!.visibility = VISIBLE
                }
                loadedUrl = url
                (context.applicationContext as App).imageLoader!!
                    .displayImage(url, thumbImage2)
                visibility = GONE
            } else {
                if (!full) {
                    thumbImage2!!.visibility = GONE
                } else {
                    wrapArea!!.visibility = GONE
                }
                visibility = GONE
            }
            if (full) {
                if (wrapArea!!.visibility == VISIBLE) {
                    title = secondTitle
                    info = secondSubTitle
                    setBottomSheet(wrapArea, submission, full)
                } else {
                    title = findViewById(R.id.textimage)
                    info = findViewById(R.id.subtextimage)
                    if ((forceThumb || (submission.isNsfw && submission.thumbnailType == ThumbnailType.NSFW)
                                || type != ContentType.Type.IMAGE&& type != ContentType.Type.SELF
                                && submission.thumbnailUrl != null
                                && submission.thumbnailType != ThumbnailType.URL)
                    ) {
                        setBottomSheet(thumbImage2, submission, full)
                    } else {
                        setBottomSheet(this, submission, full)
                    }
                }
            } else {
                title = findViewById(R.id.textimage)
                info = findViewById(R.id.subtextimage)
                setBottomSheet(thumbImage2, submission, full)
                setBottomSheet(this, submission, full)
            }
            if (SettingValues.smallTag && !full && !news) {
                title = findViewById(R.id.tag)
                findViewById<View>(R.id.tag).visibility = VISIBLE
                info = null
            } else {
                findViewById<View>(R.id.tag).visibility = GONE
                title!!.visibility = VISIBLE
                info!!.visibility = VISIBLE
            }
            if (SettingValues.smallTag && !full && !news) {
                (title as TransparentTagTextView?)!!.init(context)
            }
            title!!.text = submission.contentDescription
            if (info != null) info!!.text = submission.domain
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    var popped = false
    fun getHeightFromAspectRatio(imageHeight: Int, imageWidth: Int): Double {
        val ratio = imageHeight.toDouble() / imageWidth.toDouble()
        val width = width.toDouble()
        return width * ratio
    }

    fun onLinkLongClick(url: String?, event: MotionEvent?) {
        popped = false
        if (url == null) {
            return
        }
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        var activity: Activity? = null
        val context = context
        if (context is Activity) {
            activity = context
        } else if (context is ContextThemeWrapper) {
            activity = context.baseContext as Activity
        } else if (context is ContextWrapper) {
            val context1 = context.baseContext
            if (context1 is Activity) {
                activity = context1
            } else if (context1 is ContextWrapper) {
                val context2 = context1.baseContext
                if (context2 is Activity) {
                    activity = context2
                } else if (context2 is ContextWrapper) {
                    activity = (context2 as ContextThemeWrapper).baseContext as Activity
                }
            }
        } else {
            throw RuntimeException("Could not find activity from context:$context")
        }
        if (activity != null && !activity.isFinishing) {
            if (SettingValues.peek) {
                Peek.into(R.layout.peek_view_submission, object : SimpleOnPeek() {
                    override fun onInflated(peekView: PeekView, rootView: View) {
                        //do stuff
                        val text = rootView.findViewById<TextView>(R.id.title)
                        text.text = url
                        text.setTextColor(Color.WHITE)
                        (rootView.findViewById<View>(R.id.peek) as PeekMediaView).setUrl(url)
                        peekView.addButton(R.id.share) {
                            defaultShareText(
                                "",
                                url,
                                rootView.context
                            )
                        }
                        peekView.addButton(R.id.upvoteb) {
                            (parent as View).findViewById<View>(R.id.upvote).callOnClick()
                        }
                        peekView.setOnRemoveListener { (rootView.findViewById<View>(R.id.peek) as PeekMediaView).doClose() }
                        peekView.addButton(R.id.comments) { (parent.parent as View).callOnClick() }
                        peekView.setOnPop {
                            popped = true
                            callOnClick()
                        }
                    }
                })
                    .with(PeekViewOptions().setFullScreenPeek(true))
                    .show(activity as PeekViewActivity?, event)
            } else {
                val b = BottomSheet.Builder(activity).title(url).grid()
                val attrs = intArrayOf(R.attr.tintColor)
                val ta = getContext().obtainStyledAttributes(attrs)
                val color = ta.getColor(0, Color.WHITE)
                val open = resources.getDrawable(R.drawable.ic_open_in_new)
                val share = resources.getDrawable(R.drawable.ic_share)
                val copy = resources.getDrawable(R.drawable.ic_content_copy)
                val drawableSet = Arrays.asList(open, share, copy)
                BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
                ta.recycle()
                b.sheet(
                    R.id.open_link, open,
                    resources.getString(R.string.open_externally)
                )
                b.sheet(R.id.share_link, share, resources.getString(R.string.share_link))
                b.sheet(
                    R.id.copy_link, copy,
                    resources.getString(R.string.submission_link_copy)
                )
                val finalActivity: Activity = activity
                b.listener { dialog, which ->
                    when (which) {
                        R.id.open_link -> LinkUtil.openExternally(url)
                        R.id.share_link -> defaultShareText("", url, finalActivity)
                        R.id.copy_link -> LinkUtil.copyUrl(url, finalActivity)
                    }
                }.show()
            }
        }
    }

    fun setBottomSheet(v: View?, submission: IPost, full: Boolean) {
        handler = Handler()
        v!!.setOnTouchListener(OnTouchListener { v, event ->
            var x = event.x.toInt()
            var y = event.y.toInt()
            x += scrollX
            y += scrollY
            this@HeaderImageLinkView.event = event
            if (event.action == MotionEvent.ACTION_DOWN) {
                position = event.y //used to see if the user scrolled or not
            }
            if (!(event.action == MotionEvent.ACTION_UP
                        || event.action == MotionEvent.ACTION_DOWN)
            ) {
                if (Math.abs(position - event.y) > 25) {
                    handler!!.removeCallbacksAndMessages(null)
                }
                return@OnTouchListener false
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    clickHandled = false
                    if (SettingValues.peek) {
                        handler!!.postDelayed(
                            longClicked!!,
                            (
                                    ViewConfiguration.getTapTimeout() + 50).toLong()
                        )
                    } else {
                        handler!!.postDelayed(
                            longClicked!!,
                            ViewConfiguration.getLongPressTimeout().toLong()
                        )
                    }
                }

                MotionEvent.ACTION_UP -> {
                    handler!!.removeCallbacksAndMessages(null)
                    if (!clickHandled) {
                        // regular click
                        callOnClick()
                    }
                }
            }
            true
        })
        longClicked = Runnable { // long click
            clickHandled = true
            handler!!.removeCallbacksAndMessages(null)
            if (SettingValues.storeHistory && !full) {
                if (!submission.isNsfw || SettingValues.storeNSFWHistory) {
                    HasSeen.addSeen(submission.uri)
                    (parent as View).findViewById<View>(R.id.title).alpha = 0.54f
                    (parent as View).findViewById<View>(R.id.body).alpha = 0.54f
                }
            }
            onLinkLongClick(submission.link, event)
        }
    }

    fun setSubmission(
        submission: IPost, full: Boolean, baseSub: String?,
        type: ContentType.Type?
    ) {
        this.type = type
        if (lastDone != submission.uri) {
            lq = false
            lastDone = submission.uri
            backdrop!!.setImageResource(
                android.R.color.transparent
            ) //reset the image view in case the placeholder is still visible
            thumbImage2!!.setImageResource(android.R.color.transparent)
            doImageAndText(submission, full, baseSub, false)
        }
    }

    fun setSubmissionNews(
        submission: IPost, full: Boolean, baseSub: String?,
        type: ContentType.Type?
    ) {
        this.type = type
        if (lastDone != submission.uri) {
            lq = false
            lastDone = submission.uri
            backdrop!!.setImageResource(
                android.R.color.transparent
            ) //reset the image view in case the placeholder is still visible
            thumbImage2!!.setImageResource(android.R.color.transparent)
            doImageAndText(submission, full, baseSub, true)
        }
    }

    fun setThumbnail(v: ImageView?) {
        thumbImage2 = v
    }

    fun setUrl(url: String?) {}

    private fun init() {
        inflate(context, R.layout.header_image_title_view, this)
        title = findViewById(R.id.textimage)
        info = findViewById(R.id.subtextimage)
        backdrop = findViewById(R.id.leadimage)
    }
}
