package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.RoundedBackgroundSpan

class ModLogAdapter(val mContext: Activity, var dataSet: ModLogPosts, private val listView: RecyclerView) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IFallibleAdapter {
    private val SPACER = 6

    override fun setError(b: Boolean) {
        listView.adapter = ErrorAdapter()
    }

    override fun undoSetError() {
        listView.adapter = this
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position == 0 && !dataSet.posts.isNullOrEmpty()) {
            return SPACER
        } else if (!dataSet.posts.isNullOrEmpty()) {
            position -= 1
        }
        return MESSAGE
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        return if (i == SPACER) {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.spacer, viewGroup, false)
            SpacerViewHolder(v)
        } else {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.mod_action, viewGroup, false)
            ModLogViewHolder(v)
        }
    }

    class ModLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var body: SpoilerRobotoTextView
        var icon: ImageView

        init {
            body = itemView.findViewById(R.id.body)
            icon = itemView.findViewById(R.id.action)
        }
    }

    override fun onBindViewHolder(firstHold: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if (firstHold is ModLogViewHolder) {
            val holder = firstHold
            val a = dataSet.posts!![i]
            val b = SpannableStringBuilder()
            val titleString = SpannableStringBuilder()
            val spacer = mContext.getString(R.string.submission_properties_seperator)
            val timeAgo = TimeUtils.getTimeAgo(a.created.time, mContext)
            val time = if (timeAgo == null || timeAgo.isEmpty()) "just now" else timeAgo //some users were crashing here
            titleString.append(time)
            titleString.append(spacer)
            if (a.subreddit != null) {
                val subname = a.subreddit
                val subreddit = SpannableStringBuilder("/c/$subname")
                if (SettingValues.colorSubName
                    && Palette.getColor(subname) != Palette.getDefaultColor()) {
                    subreddit.setSpan(ForegroundColorSpan(Palette.getColor(subname)), 0,
                        subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    subreddit.setSpan(StyleSpan(Typeface.BOLD), 0, subreddit.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                titleString.append(subreddit)
            }
            b.append(titleString)
            b.append(spacer)
            val author = SpannableStringBuilder(a.moderator)
            val authorcolor = Palette.getFontColorUser(a.moderator)
            author.setSpan(TypefaceSpan("sans-serif-condensed"), 0, author.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            author.setSpan(StyleSpan(Typeface.BOLD), 0, author.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (Authentication.name != null && (a.moderator
                    .lowercase()
                    == Authentication.name!!.lowercase())) {
                author.replace(0, author.length, " " + a.moderator + " ")
                author.setSpan(RoundedBackgroundSpan(mContext, android.R.color.white,
                    R.color.md_deep_orange_300, false), 0, author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (authorcolor != 0) {
                author.setSpan(ForegroundColorSpan(authorcolor), 0, author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            author.setSpan(RelativeSizeSpan(0.8f), 0, author.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            b.append(author)
            b.append("\n\n")
            b.append(a.action).append(" ").append(if (!a.dataNode["target_title"].isNull) ("\""
                + a.dataNode["target_title"].asText()
                + "\"") else "").append(if (a.targetAuthor != null) (" by /u/"
                + a.targetAuthor) else "")
            if (a.targetPermalink != null) {
                holder.itemView.setOnClickListener { OpenRedditLink.openUrl(mContext, a.targetPermalink, true) }
            }
            if (a.details != null) {
                val description = SpannableStringBuilder(" (" + a.details + ")")
                description.setSpan(StyleSpan(Typeface.ITALIC), 0, description.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                description.setSpan(RelativeSizeSpan(0.8f), 0, description.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                b.append(description)
            }
            holder.body.text = b
            val action = a.action
            when (action) {
                "removelink" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_close,
                        null))

                "approvecomment", "approvelink" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_thumb_up,
                        null))

                "removecomment" -> holder.icon.setImageDrawable(ResourcesCompat.getDrawable(mContext.resources,
                    R.drawable.ic_forum, null))

                "editflair" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_local_offer,
                        null))

                "distinguish" -> holder.icon.setImageDrawable(ResourcesCompat.getDrawable(mContext.resources,
                    R.drawable.ic_star, null))

                "sticky", "unsticky" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_lock,
                        null))

                "ignorereports" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_notifications_off,
                        null))

                "unignorereports" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_notifications_active,
                        null))

                "marknsfw", "unmarknsfw" -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_visibility_off,
                        null))

                else -> holder.icon.setImageDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_verified_user, null))
            }
        }
        if (firstHold is SpacerViewHolder) {
            firstHold.itemView.findViewById<View>(R.id.height).layoutParams = LinearLayout.LayoutParams(firstHold.itemView.width,
                mContext.findViewById<View>(R.id.header).height)
        }
    }

    override fun getItemCount(): Int {
        return if (dataSet.posts?.isNotEmpty() == true) {
            0
        } else {
            dataSet.posts!!.size + 1
        }
    }

    companion object {
        const val MESSAGE = 2
    }
}
