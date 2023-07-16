package me.ccrama.redditslide

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.commentLastVisit
import ltd.ucode.slide.SettingValues.hidePostAwards
import ltd.ucode.slide.SettingValues.showDomain
import ltd.ucode.slide.SettingValues.typeInfoLine
import ltd.ucode.slide.SettingValues.votesInfoLine
import ltd.ucode.network.data.IPost
import me.ccrama.redditslide.Adapters.CommentAdapterHelper
import me.ccrama.redditslide.Toolbox.ToolboxUI.appendToolboxNote
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import net.dean.jraw.models.DistinguishedStatus
import java.net.URL
import java.util.Locale
import java.util.WeakHashMap

object SubmissionCache {
    private var titles: WeakHashMap<String, SpannableStringBuilder>? = null
    private var info: WeakHashMap<String, SpannableStringBuilder>? = null
    private var crosspost: WeakHashMap<String, SpannableStringBuilder?>? = null
    fun cacheSubmissions(
        submissions: List<IPost>, mContext: Context,
        baseSub: String
    ) {
        cacheInfo(submissions, mContext, baseSub)
    }

    fun getCrosspostLine(s: IPost, mContext: Context): SpannableStringBuilder? {
        if (crosspost == null) crosspost = WeakHashMap()
        return if (crosspost!!.containsKey(s.uri)) {
            crosspost!![s.uri]
        } else {
            getCrosspostSpannable(s, mContext)
        }
    }

    private fun cacheInfo(submissions: List<IPost>, mContext: Context, baseSub: String) {
        if (titles == null) titles = WeakHashMap()
        if (info == null) info = WeakHashMap()
        if (crosspost == null) crosspost = WeakHashMap()
        for (submission in submissions) {
            titles!![submission.uri] = getTitleSpannable(submission, mContext)
            info!![submission.uri] = getInfoSpannable(submission, mContext, baseSub)
            crosspost!![submission.uri] = getCrosspostLine(submission, mContext)
        }
    }

    fun updateInfoSpannable(changed: IPost, mContext: Context, baseSub: String) {
        info!![changed.uri] =
            getInfoSpannable(changed, mContext, baseSub)
    }

    fun updateTitleFlair(s: IPost, flair: String?, c: Context) {
        titles!![s.uri] = getTitleSpannable(s, flair, c)
    }

    fun getTitleLine(s: IPost, mContext: Context): SpannableStringBuilder? {
        if (titles == null) titles = WeakHashMap()
        return if (titles!!.containsKey(s.uri)) {
            titles!![s.uri]
        } else {
            getTitleSpannable(s, mContext)
        }
    }

    fun getInfoLine(
        s: IPost, mContext: Context,
        baseSub: String
    ): SpannableStringBuilder? {
        if (info == null) info = WeakHashMap()
        return if (info!!.containsKey(s.uri)) {
            info!![s.uri]
        } else {
            getInfoSpannable(s, mContext, baseSub)
        }
    }

    private fun getCrosspostSpannable(s: IPost, mContext: Context): SpannableStringBuilder? {
        val spacer = mContext.getString(R.string.submission_properties_seperator)
        val titleString = SpannableStringBuilder("/kbin$spacer")
        if (!URL(s.uri).path.startsWith("/m/")) { //is not a crosspost
            return null
        }
        s.groupName.let {
            val subname = it.lowercase()
            val subreddit = SpannableStringBuilder("/c/$subname$spacer")
            if (SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor()
                || (SettingValues.colorSubName
                        && Palette.getColor(subname) != Palette.getDefaultColor())
            ) {
                if (!SettingValues.colorEverywhere) {
                    subreddit.setSpan(
                        ForegroundColorSpan(Palette.getColor(subname)), 0,
                        subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    subreddit.setSpan(
                        StyleSpan(Typeface.BOLD), 0, subreddit.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            titleString.append(subreddit)
        }
        val author = SpannableStringBuilder(s.user.name + " ")
        val authorcolor = Palette.getFontColorUser(s.user.name)
        if (authorcolor != 0) {
            author.setSpan(
                ForegroundColorSpan(authorcolor), 0, author.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        titleString.append(author)
        if (UserTags.isUserTagged(s.user.name)) {
            val pinned = SpannableStringBuilder(
                " " + UserTags.getUserTag(s.user.name) + " "
            )
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_500, false),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(pinned)
        }
        if (UserSubscriptions.friends!!.contains(s.user.name)) {
            val pinned = SpannableStringBuilder(
                " " + mContext.getString(R.string.profile_friend) + " "
            )
            pinned.setSpan(
                RoundedBackgroundSpan(
                    mContext, android.R.color.white, R.color.md_deep_orange_500,
                    false
                ), 0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(pinned)
        }
        return titleString
    }

    private fun getInfoSpannable(
        submission: IPost, mContext: Context,
        baseSub: String
    ): SpannableStringBuilder {
        var baseSub: String? = baseSub
        val spacer = mContext.getString(R.string.submission_properties_seperator)
        val titleString = SpannableStringBuilder()
        var subreddit = SpannableStringBuilder(" /c/" + submission.groupName + " ")
        if (submission.groupName == null) {
            subreddit = SpannableStringBuilder("Promoted ")
        }
        val subname: String
        subname = if (submission.groupName != null) {
            submission.groupName.lowercase()
        } else {
            ""
        }
        if (baseSub == null || baseSub.isEmpty()) baseSub = subname
        if (SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor() || baseSub == "nomatching" && (SettingValues.colorSubName
                    && Palette.getColor(subname) != Palette.getDefaultColor())
        ) {
            val secondary = (baseSub.equals("frontpage", ignoreCase = true)
                    || baseSub.equals("all", ignoreCase = true)
                    || baseSub.equals("popular", ignoreCase = true)
                    || baseSub.equals("friends", ignoreCase = true)
                    || baseSub.equals("mod", ignoreCase = true)
                    || baseSub.contains(".")
                    || baseSub.contains("+"))
            if (secondary || !SettingValues.colorEverywhere) {
                subreddit.setSpan(
                    ForegroundColorSpan(Palette.getColor(subname)), 0,
                    subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                subreddit.setSpan(
                    StyleSpan(Typeface.BOLD), 0, subreddit.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        titleString.append(subreddit)
        titleString.append(spacer)
        try {
            val time = TimeUtils.getTimeAgo(submission.created.toEpochMilliseconds(), mContext)
            titleString.append(time)
        } catch (e: Exception) {
            titleString.append("just now")
        }
        titleString.append(
            if (submission.updated != null) " (edit " + TimeUtils.getTimeAgo(
                submission.updated!!.toEpochMilliseconds(), mContext
            ) + ")" else ""
        )
        titleString.append(spacer)
        val author = SpannableStringBuilder(" " + submission.user.name + " ")
        val authorcolor = Palette.getFontColorUser(submission.user.name)
        if (submission.user.name != null) {
            if (Authentication.name != null && (submission.user.name
                    .lowercase()
                        == Authentication.name!!.lowercase())
            ) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext, android.R.color.white,
                        R.color.md_deep_orange_300, false
                    ), 0, author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (submission.regalia == DistinguishedStatus.ADMIN) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext, android.R.color.white, R.color.md_red_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (submission.regalia == DistinguishedStatus.SPECIAL) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext, android.R.color.white, R.color.md_purple_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (submission.regalia == DistinguishedStatus.MODERATOR) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext, android.R.color.white, R.color.md_green_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (authorcolor != 0) {
                author.setSpan(
                    ForegroundColorSpan(authorcolor), 0, author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            titleString.append(author)
        }


        /*todo maybe?  titleString.append(((comment.hasBeenEdited() && comment.getEditDate() != null) ? " *" + TimeUtils.getTimeAgo(comment.getEditDate().getTime(), mContext) : ""));
        titleString.append("  ");*/if (UserTags.isUserTagged(submission.user.name)) {
            val pinned = SpannableStringBuilder(
                " " + UserTags.getUserTag(submission.user.name) + " "
            )
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_500, false),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        if (UserSubscriptions.friends!!.contains(submission.user.name)) {
            val pinned = SpannableStringBuilder(
                " " + mContext.getString(R.string.profile_friend) + " "
            )
            pinned.setSpan(
                RoundedBackgroundSpan(
                    mContext, android.R.color.white, R.color.md_deep_orange_500,
                    false
                ), 0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        appendToolboxNote(mContext, titleString, submission.groupName, submission.user.name)

        /* too big, might add later todo
        if (submission.getAuthorFlair() != null && submission.getAuthorFlair().getText() != null && !submission.getAuthorFlair().getText().isEmpty()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = mContext.getTheme();
            theme.resolveAttribute(R.attr.activity_background, typedValue, true);
            int color = typedValue.data;
            SpannableStringBuilder pinned = new SpannableStringBuilder(" " + submission.getAuthorFlair().getText() + " ");
            pinned.setSpan(new RoundedBackgroundSpan(holder.title.getCurrentTextColor(), color, false, mContext), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleString.append(pinned);
            titleString.append(" ");
        }



        if (holder.leadImage.getVisibility() == View.GONE && !full) {
            String text = "";

            switch (ContentType.getContentType(submission)) {
                case NSFW_IMAGE:
                    text = mContext.getString(R.string.type_nsfw_img);
                    break;

                case NSFW_GIF:
                case NSFW_GFY:
                    text = mContext.getString(R.string.type_nsfw_gif);
                    break;

                case REDDIT:
                    text = mContext.getString(R.string.type_reddit);
                    break;

                case LINK:
                case IMAGE_LINK:
                    text = mContext.getString(R.string.type_link);
                    break;

                case NSFW_LINK:
                    text = mContext.getString(R.string.type_nsfw_link);

                    break;
                case STREAMABLE:
                    text = ("Streamable");
                    break;
                case SELF:
                    text = ("Selftext");
                    break;

                case ALBUM:
                    text = mContext.getString(R.string.type_album);
                    break;

                case IMAGE:
                    text = mContext.getString(R.string.type_img);
                    break;
                case IMGUR:
                    text = mContext.getString(R.string.type_imgur);
                    break;
                case GFY:
                case GIF:
                case NONE_GFY:
                case NONE_GIF:
                    text = mContext.getString(R.string.type_gif);
                    break;

                case NONE:
                    text = mContext.getString(R.string.type_title_only);
                    break;

                case NONE_IMAGE:
                    text = mContext.getString(R.string.type_img);
                    break;

                case VIDEO:
                    text = mContext.getString(R.string.type_vid);
                    break;

                case EMBEDDED:
                    text = mContext.getString(R.string.type_emb);
                    break;

                case NONE_URL:
                    text = mContext.getString(R.string.type_link);
                    break;
            }
            if(!text.isEmpty()) {
                titleString.append(" \n");
                text = text.toUpperCase();
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = mContext.getTheme();
                theme.resolveAttribute(R.attr.activity_background, typedValue, true);
                int color = typedValue.data;
                SpannableStringBuilder pinned = new SpannableStringBuilder(" " + text + " ");
                pinned.setSpan(new RoundedBackgroundSpan(holder.title.getCurrentTextColor(), color, false, mContext), 0, pinned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleString.append(pinned);
            }
        }*/if (showDomain) {
            titleString.append(spacer)
            titleString.append(submission.domain.orEmpty())
        }
        if (typeInfoLine) {
            titleString.append(spacer)
            val s = SpannableStringBuilder(
                submission.contentDescription
            )
            s.setSpan(
                StyleSpan(Typeface.BOLD), 0, s.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(s)
        }
        if (votesInfoLine) {
            titleString.append("\n ")
            val s = SpannableStringBuilder(
                submission.score
                    .toString() + String.format(
                    Locale.getDefault(), " %s", mContext.resources
                        .getQuantityString(R.plurals.points, submission.score)
                ) + spacer
                        + submission.commentCount
                        + String.format(
                    Locale.getDefault(), " %s", mContext.resources
                        .getQuantityString(R.plurals.comments, submission.commentCount)
                )
            )
            s.setSpan(
                StyleSpan(Typeface.BOLD), 0, s.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (commentLastVisit) {
                val more = LastComments.commentsSince(submission)
                s.append(if (more > 0) "(+$more)" else "")
            }
            titleString.append(s)
        }
        if (removed.contains(submission.uri) || (submission.bannedBy != null
                    && !approved.contains(submission.uri))
        ) {
            titleString.append(
                CommentAdapterHelper.createRemovedLine(
                    if (submission.bannedBy == null) Authentication.name else submission.bannedBy,
                    mContext
                )
            )
        } else if (approved.contains(submission.uri) || (submission.approvedBy
                    != null && !removed.contains(submission.uri))
        ) {
            titleString.append(
                CommentAdapterHelper.createApprovedLine(
                    if (submission.approvedBy == null) Authentication.name else submission.approvedBy,
                    mContext
                )
            )
        }
        return titleString
    }

    private fun getTitleSpannable(
        submission: IPost,
        flairOverride: String?, mContext: Context
    ): SpannableStringBuilder {
        val titleString = SpannableStringBuilder()
        titleString.append(CompatUtil.fromHtml(submission.title))
        if (submission.isFeatured) {
            val pinned = SpannableStringBuilder(
                "\u00A0"
                        + mContext.getString(R.string.submission_stickied)
                    .uppercase(Locale.getDefault())
                        + "\u00A0"
            )
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_green_300, true),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        if (!hidePostAwards && (submission.timesSilvered > 0 || submission.timesGilded > 0 || submission.timesPlatinized > 0)) {
            val a = mContext.obtainStyledAttributes(
                FontPreferences(mContext).postFontStyle.resId,
                R.styleable.FontStyle
            )
            val fontsize =
                (a.getDimensionPixelSize(R.styleable.FontStyle_font_cardtitle, -1) * .75).toInt()
            a.recycle()
            // Add silver, gold, platinum icons and counts in that order
            MiscUtil.addSubmissionAwards(
                mContext,
                fontsize,
                titleString,
                submission.timesSilvered,
                R.drawable.silver
            )
            MiscUtil.addSubmissionAwards(
                mContext,
                fontsize,
                titleString,
                submission.timesGilded,
                R.drawable.gold
            )
            MiscUtil.addSubmissionAwards(
                mContext,
                fontsize,
                titleString,
                submission.timesPlatinized,
                R.drawable.platinum
            )
        }
        if (submission.isNsfw) {
            val pinned = SpannableStringBuilder("\u00A0NSFW\u00A0")
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_red_300, true), 0,
                pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        if (submission.isSpoiler) {
            val pinned = SpannableStringBuilder("\u00A0SPOILER\u00A0")
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_grey_600, true),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        if (submission.isOC) {
            val pinned = SpannableStringBuilder("\u00A0OC\u00A0")
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_500, true),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        if (submission.flair.text != null && !submission.flair
                .text
                .isEmpty() || flairOverride != null || submission.flair
                .cssClass != null
        ) {
            val typedValue = TypedValue()
            val theme = mContext.theme
            theme.resolveAttribute(R.attr.activity_background, typedValue, false)
            val color = typedValue.data
            theme.resolveAttribute(R.attr.fontColor, typedValue, false)
            val font = typedValue.data
            val flairString: String
            flairString = flairOverride
                ?: if ((submission.flair.text == null
                            || submission.flair.text.isEmpty())
                    && submission.flair.cssClass != null
                ) {
                    submission.flair.cssClass
                } else {
                    submission.flair.text
                }
            val pinned =
                SpannableStringBuilder("\u00A0" + CompatUtil.fromHtml(flairString) + "\u00A0")
            pinned.setSpan(
                RoundedBackgroundSpan(font, color, true, mContext), 0,
                pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(" ")
            titleString.append(pinned)
        }
        return titleString
    }

    var removed = ArrayList<String>()
    var approved = ArrayList<String>()
    private fun getTitleSpannable(
        submission: IPost,
        mContext: Context
    ): SpannableStringBuilder {
        return getTitleSpannable(submission, null, mContext)
    }

    fun evictAll() {
        info = WeakHashMap()
    }
}
