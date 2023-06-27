package me.ccrama.redditslide.views

import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.switchThumb
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.DisplayUtil

object CreateCardView {
    @JvmStatic
    fun CreateViewNews(viewGroup: ViewGroup): View {
        return LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.submission_news, viewGroup, false)
    }

    @JvmStatic
    fun CreateView(viewGroup: ViewGroup): View? {
        val cardEnum = SettingValues.defaultCardView
        var v: View? = null
        when (cardEnum) {
            CardEnum.LARGE -> v = if (SettingValues.middleImage) {
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.submission_largecard_middle, viewGroup, false)
            } else {
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.submission_largecard, viewGroup, false)
            }

            CardEnum.LIST -> {
                v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.submission_list, viewGroup, false)

                //if the radius is set to 0 on KitKat--it crashes.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (v.findViewById<View>(R.id.card) as CardView).radius = 0f
                }
            }

            CardEnum.DESKTOP -> {
                v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.submission_list_desktop, viewGroup, false)

                //if the radius is set to 0 on KitKat--it crashes.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (v.findViewById<View>(R.id.card) as CardView).radius = 0f
                }
            }
        }
        val thumbImage = v.findViewById<View>(R.id.thumbimage2)
        /**
         * If the user wants small thumbnails, revert the list style to the "old" list view.
         * The "old" thumbnails were (70dp x 70dp).
         * Adjusts the paddingTop of the innerrelative, and adjusts the margins on the thumbnail.
         */
        if (!SettingValues.bigThumbnails) {
            if (SettingValues.defaultCardView == CardEnum.DESKTOP) {
                val SQUARE_THUMBNAIL_SIZE = 48
                thumbImage.layoutParams.height = DisplayUtil.dpToPxVertical(SQUARE_THUMBNAIL_SIZE)
                thumbImage.layoutParams.width = DisplayUtil.dpToPxHorizontal(SQUARE_THUMBNAIL_SIZE)
            } else {
                val SQUARE_THUMBNAIL_SIZE = 70
                thumbImage.layoutParams.height = DisplayUtil.dpToPxVertical(SQUARE_THUMBNAIL_SIZE)
                thumbImage.layoutParams.width = DisplayUtil.dpToPxHorizontal(SQUARE_THUMBNAIL_SIZE)
                val EIGHT_DP_Y = DisplayUtil.dpToPxVertical(8)
                val EIGHT_DP_X = DisplayUtil.dpToPxHorizontal(8)
                (thumbImage.layoutParams as RelativeLayout.LayoutParams)
                    .setMargins(EIGHT_DP_X * 2, EIGHT_DP_Y, EIGHT_DP_X, EIGHT_DP_Y)
                v.findViewById<View>(R.id.innerrelative).setPadding(0, EIGHT_DP_Y, 0, 0)
            }
        }
        if (SettingValues.noThumbnails) {
            val SQUARE_THUMBNAIL_SIZE = 0
            thumbImage.layoutParams.height = DisplayUtil.dpToPxVertical(SQUARE_THUMBNAIL_SIZE)
            thumbImage.layoutParams.width = DisplayUtil.dpToPxHorizontal(SQUARE_THUMBNAIL_SIZE)
        }
        doHideObjects(v)
        return v
    }

    @JvmStatic
    fun resetColorCard(v: View) {
        v.setTag(v.id, "none")
        val background = TypedValue()
        v.context.theme.resolveAttribute(R.attr.card_background, background, true)
        (v.findViewById<View>(R.id.card) as CardView).setCardBackgroundColor(background.data)
        if (!SettingValues.actionbarVisible) {
            for (v2 in getViewsByTag(v as ViewGroup, "tintactionbar")) {
                v2.visibility = View.GONE
            }
        }
        doColor(getViewsByTag(v as ViewGroup, "tint"))
        doColorSecond(getViewsByTag(v, "tintsecond"))
        doColorSecond(getViewsByTag(v, "tintactionbar"))
    }

    fun doColor(v: ArrayList<View>) {
        for (v2 in v) {
            if (v2 is TextView) {
                v2.setTextColor(Palette.getCurrentFontColor(v2.getContext()))
            } else if (v2 is ImageView) {
                BlendModeUtil.tintImageViewAsSrcAtop(
                    v2,
                    Palette.getCurrentTintColor(v2.getContext())
                )
            }
        }
    }

    fun doColorSecond(v: ArrayList<View>) {
        for (v2 in v) {
            if (v2 is TextView) {
                v2.setTextColor(Palette.getCurrentTintColor(v2.getContext()))
            } else if (v2 is ImageView) {
                BlendModeUtil.tintImageViewAsSrcAtop(
                    v2,
                    Palette.getCurrentTintColor(v2.getContext())
                )
            }
        }
    }

    fun resetColor(v: ArrayList<View>) {
        for (v2 in v) {
            if (v2 is TextView) {
                v2.setTextColor(Palette.getWhiteFontColor())
            } else if (v2 is ImageView) {
                BlendModeUtil.tintImageViewAsSrcAtop(v2, Palette.getWhiteTintColor())
            }
        }
    }

    private fun getViewsByTag(root: ViewGroup?, tag: String): ArrayList<View> {
        val views = ArrayList<View>()
        val childCount = root!!.childCount
        for (i in 0 until childCount) {
            val child = root.getChildAt(i)
            if (child is ViewGroup) {
                views.addAll(getViewsByTag(child, tag))
            }
            val tagObj = child.tag
            if (tagObj != null && tagObj == tag) {
                views.add(child)
            }
        }
        return views
    }

    @JvmStatic fun colorCard(sec: String?, v: View, subToMatch: String, secondary: Boolean) {
        resetColorCard(v)
        if (SettingValues.colorBack && !SettingValues.colorSubName && Palette.getColor(
                sec
            ) != Palette.getDefaultColor() || subToMatch == "nomatching" && SettingValues.colorBack && !SettingValues.colorSubName && Palette.getColor(
                sec
            ) != Palette.getDefaultColor()
        ) {
            if (secondary || !SettingValues.colorEverywhere) {
                (v.findViewById<View>(R.id.card) as CardView).setCardBackgroundColor(
                    Palette.getColor(
                        sec
                    )
                )
                v.setTag(v.id, "color")
                resetColor(getViewsByTag(v as ViewGroup, "tint"))
                resetColor(getViewsByTag(v, "tintsecond"))
                resetColor(getViewsByTag(v, "tintactionbar"))
            }
        }
    }

    @JvmStatic fun setActionbarVisible(isChecked: Boolean, parent: ViewGroup): View? {
        SettingValues.actionbarVisible = isChecked
        return CreateView(parent)
    }

    @JvmStatic fun setSmallTag(isChecked: Boolean, parent: ViewGroup): View? {
        SettingValues.smallTag = isChecked
        return CreateView(parent)
    }

    @JvmStatic fun setCardViewType(cardEnum: CardEnum, parent: ViewGroup): View? {
        SettingValues.middleImage = false
        SettingValues.defaultCardView = cardEnum
        return CreateView(parent)
    }

    @JvmStatic fun setBigPicEnabled(b: Boolean?, parent: ViewGroup): View? {
        SettingValues.noThumbnails = false
        SettingValues.bigPicEnabled = b!!
        SettingValues.bigPicCropped = false
        return CreateView(parent)
    }

    @JvmStatic fun setBigPicCropped(b: Boolean?, parent: ViewGroup): View? {
        SettingValues.noThumbnails = false
        SettingValues.bigPicEnabled = b!!
        SettingValues.bigPicCropped = b!!
        return CreateView(parent)
    }

    @JvmStatic fun setNoThumbnails(b: Boolean?, parent: ViewGroup): View? {
        SettingValues.noThumbnails = b!!
        SettingValues.bigPicEnabled = false
        SettingValues.bigPicCropped = false
        return CreateView(parent)
    }

    @JvmStatic fun setMiddleCard(b: Boolean, parent: ViewGroup): View? {
        SettingValues.defaultCardView = CardEnum.LARGE
        SettingValues.middleImage = b
        return CreateView(parent)
    }

    @JvmStatic fun setSwitchThumb(b: Boolean, parent: ViewGroup): View? {
        switchThumb = b
        return CreateView(parent)
    }

    fun toggleActionbar(v: View?) {
        if (!SettingValues.actionbarVisible) {
            val a = AnimatorUtil.flipAnimatorIfNonNull(
                v!!.findViewById<View>(R.id.upvote).visibility == View.VISIBLE,
                v.findViewById(R.id.secondMenu)
            )
            a?.start()
            for (v2 in getViewsByTag(v as ViewGroup?, "tintactionbar")) {
                if (v2.id != R.id.mod && v2.id != R.id.edit) {
                    if (v2.id == R.id.save) {
                        if (SettingValues.saveButton) {
                            if (v2.visibility == View.VISIBLE) {
                                AnimatorUtil.animateOut(v2)
                            } else {
                                AnimatorUtil.animateIn(v2, 36)
                            }
                        }
                    } else if (v2.id == R.id.hide) {
                        if (SettingValues.hideButton) {
                            if (v2.visibility == View.VISIBLE) {
                                AnimatorUtil.animateOut(v2)
                            } else {
                                AnimatorUtil.animateIn(v2, 36)
                            }
                        }
                    } else {
                        if (v2.visibility == View.VISIBLE) {
                            AnimatorUtil.animateOut(v2)
                        } else {
                            AnimatorUtil.animateIn(v2, 36)
                        }
                    }
                }
            }
        }
    }

    private fun doHideObjects(v: View?) {
        if (SettingValues.smallTag) {
            v!!.findViewById<View>(R.id.base).visibility = View.GONE
            v.findViewById<View>(R.id.tag).visibility = View.VISIBLE
        } else {
            v!!.findViewById<View>(R.id.tag).visibility = View.GONE
        }
        if (SettingValues.bigPicCropped) {
            (v.findViewById<View>(R.id.leadimage) as ImageView).maxHeight = 900
            (v.findViewById<View>(R.id.leadimage) as ImageView).scaleType =
                ImageView.ScaleType.CENTER_CROP
        }
        if (!SettingValues.actionbarVisible && !SettingValues.actionbarTap) {
            for (v2 in getViewsByTag(v as ViewGroup?, "tintactionbar")) {
                v2.visibility = View.GONE
            }
            v.findViewById<View>(R.id.secondMenu).setOnClickListener { toggleActionbar(v) }
        } else {
            v.findViewById<View>(R.id.secondMenu).visibility = View.GONE
            if (SettingValues.actionbarTap) {
                for (v2 in getViewsByTag(v as ViewGroup?, "tintactionbar")) {
                    v2.visibility = View.GONE
                }
                v.setOnLongClickListener { v ->
                    toggleActionbar(v)
                    true
                }
            }
        }
        if (switchThumb) {
            val picParams =
                v.findViewById<View>(R.id.thumbimage2).layoutParams as RelativeLayout.LayoutParams
            val layoutParams =
                v.findViewById<View>(R.id.inside).layoutParams as RelativeLayout.LayoutParams
            if (!SettingValues.actionbarVisible && !SettingValues.actionbarTap) {
                picParams.addRule(RelativeLayout.LEFT_OF, R.id.secondMenu)
            } else {
                picParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)
            }
            picParams.setMargins(
                picParams.rightMargin,
                picParams.topMargin,
                picParams.leftMargin,
                picParams.bottomMargin
            )
            layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.thumbimage2)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                layoutParams.removeRule(RelativeLayout.RIGHT_OF)
            } else {
                layoutParams.addRule(RelativeLayout.RIGHT_OF, 0)
            }
        }
        if (!SettingValues.bigPicEnabled) {
            v.findViewById<View>(R.id.thumbimage2).visibility = View.VISIBLE
            v.findViewById<View>(R.id.headerimage).visibility = View.GONE
        } else if (SettingValues.bigPicEnabled) {
            v.findViewById<View>(R.id.thumbimage2).visibility = View.GONE
        }
    }

    val isCard: Boolean
        get() = SettingValues.defaultCardView == CardEnum.LARGE
    val isMiddle: Boolean
        get() = SettingValues.middleImage
    val isDesktop: Boolean
        get() = SettingValues.defaultCardView == CardEnum.DESKTOP

    enum class CardEnum(val displayName: String) {
        LARGE("Big Card"), LIST("List"), DESKTOP("Desktop");

    }
}
