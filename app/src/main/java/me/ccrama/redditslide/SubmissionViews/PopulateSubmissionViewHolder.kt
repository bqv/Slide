package me.ccrama.redditslide.SubmissionViews

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.ListCallback
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.cocosw.bottomsheet.BottomSheet
import com.devspark.robototextview.RobotoTypefaces
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.isSelftextEnabled
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.ActionStates
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Activities.AlbumPager
import me.ccrama.redditslide.Activities.FullscreenVideo
import me.ccrama.redditslide.Activities.GalleryImage
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.ModQueue
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.Activities.PostReadLater
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.Reauthenticate
import me.ccrama.redditslide.Activities.RedditGallery
import me.ccrama.redditslide.Activities.RedditGalleryPager
import me.ccrama.redditslide.Activities.Search
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Tumblr
import me.ccrama.redditslide.Activities.TumblrPager
import me.ccrama.redditslide.Adapters.CommentAdapter
import me.ccrama.redditslide.Adapters.SubmissionViewHolder
import me.ccrama.redditslide.CommentCacheAsync
import me.ccrama.redditslide.ContentType
import me.ccrama.redditslide.DataShare
import me.ccrama.redditslide.ForceTouch.PeekViewActivity
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.Hidden
import me.ccrama.redditslide.LastComments
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SubmissionCache
import me.ccrama.redditslide.Toolbox.ToolboxUI
import me.ccrama.redditslide.Toolbox.ToolboxUI.CompletedRemovalCallback
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.views.CreateCardView
import me.ccrama.redditslide.views.DoEditorActions
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.ClipboardUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.SubmissionParser
import net.dean.jraw.ApiException
import net.dean.jraw.fluent.FluentRedditClient
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.oauth.InvalidScopeException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.FlairTemplate
import net.dean.jraw.models.Ruleset
import net.dean.jraw.models.SubredditRule
import net.dean.jraw.models.VoteDirection
import org.apache.commons.text.StringEscapeUtils
import java.util.Arrays
import java.util.Locale

class PopulateSubmissionViewHolder() {
    var reason: String? = null
    var chosen = booleanArrayOf(false, false, false)
    var oldChosen = booleanArrayOf(false, false, false)
    private fun showBottomSheet(
        mContext: Activity,
        submission: IPost, holder: SubmissionViewHolder, posts: MutableList<IPost>,
        baseSub: String?, recyclerview: RecyclerView, full: Boolean
    ) {
        val attrs = intArrayOf(R.attr.tintColor)
        val ta = mContext.obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val profile =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_account_circle, null)
        val sub =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_bookmark_border, null)
        val saved = ResourcesCompat.getDrawable(
            mContext.resources, R.drawable.ic_star,
            null
        )
        val hide =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_visibility_off, null)
        val report = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_report, null)
        val copy = ResourcesCompat.getDrawable(
            mContext.resources, R.drawable.ic_content_copy,
            null
        )
        val readLater =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_download, null)
        val open =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_open_in_browser, null)
        val link = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_link, null)
        val reddit = ResourcesCompat.getDrawable(
            mContext.resources, R.drawable.ic_forum,
            null
        )
        val filter =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_filter_list, null)
        val crosspost = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_forward, null)
        val drawableSet = Arrays.asList(
            profile, sub, saved, hide, report, copy,
            open, link, reddit, readLater, filter, crosspost
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(mContext).title(CompatUtil.fromHtml(submission.title))
        val isReadLater = mContext is PostReadLater
        val isAddedToReadLaterList = false//ReadLater.isToBeReadLater(submission)
        if (Authentication.didOnline) {
            b.sheet(1, (profile)!!, "/u/" + submission.creator.name)
                .sheet(2, (sub)!!, "/c/" + submission.groupName)
            var save: String = mContext.getString(R.string.btn_save)
            if (false /*ActionStates.isSaved(submission)*/) {
                save = mContext.getString(R.string.comment_unsave)
            }
            if (Authentication.isLoggedIn) {
                b.sheet(3, (saved)!!, (save))
            }
        }
        if (isAddedToReadLaterList) {
            b.sheet(28, (readLater)!!, "Mark As Read")
        } else {
            b.sheet(28, (readLater)!!, "Read later")
        }
        if (Authentication.didOnline) {
            if (Authentication.isLoggedIn) {
                b.sheet(12, (report)!!, mContext.getString(R.string.btn_report))
                b.sheet(13, (crosspost)!!, mContext.getString(R.string.btn_crosspost))
            }
        }
        if (submission.body?.isNotEmpty() == true && full) {
            b.sheet(25, (copy)!!, mContext.getString(R.string.submission_copy_text))
        }
        val hidden = submission.isHidden
        if (!full && Authentication.didOnline) {
            if (!hidden) {
                b.sheet(5, (hide)!!, mContext.getString(R.string.submission_hide))
            } else {
                b.sheet(5, (hide)!!, mContext.getString(R.string.submission_unhide))
            }
        }
        b.sheet(7, (open)!!, mContext.getString(R.string.open_externally))
        b.sheet(4, (link)!!, mContext.getString(R.string.submission_share_permalink))
            .sheet(8, (reddit)!!, mContext.getString(R.string.submission_share_reddit_url))
        if ((mContext is MainActivity) || (mContext is SubredditView)) {
            b.sheet(10, (filter)!!, mContext.getString(R.string.filter_content))
        }
        b.listener(object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    1 -> {
                        val i = Intent(mContext, Profile::class.java)
                        i.putExtra(Profile.EXTRA_PROFILE, submission.creator.name)
                        mContext.startActivity(i)
                    }

                    2 -> {
                        val i = Intent(mContext, SubredditView::class.java)
                        i.putExtra(SubredditView.EXTRA_SUBREDDIT, submission.groupName)
                        mContext.startActivityForResult(i, 14)
                    }

                    10 -> {
                        val choices: Array<String>
                        val flair =
                            if (submission.flair.text != null) submission.flair.text else ""
                        if (flair.isEmpty()) {
                            choices = arrayOf(
                                mContext.getString(
                                    R.string.filter_posts_sub,
                                    submission.groupName
                                ),
                                mContext.getString(
                                    R.string.filter_posts_user,
                                    submission.creator.name
                                ),
                                mContext.getString(
                                    R.string.filter_posts_urls,
                                    submission.domain
                                ),
                                mContext.getString(
                                    R.string.filter_open_externally,
                                    submission.domain
                                )
                            )
                            chosen = booleanArrayOf(
                                SettingValues.subredditFilters.contains(
                                    submission.groupName.lowercase()
                                ),
                                SettingValues.userFilters.contains(
                                    submission.creator.name.lowercase()
                                ),
                                SettingValues.domainFilters.contains(
                                    submission.domain!!.lowercase()
                                ),
                                SettingValues.alwaysExternal.contains(
                                    submission.domain!!.lowercase()
                                )
                            )
                            oldChosen = chosen.clone()
                        } else {
                            choices = arrayOf(
                                mContext.getString(
                                    R.string.filter_posts_sub,
                                    submission.groupName
                                ),
                                mContext.getString(
                                    R.string.filter_posts_user,
                                    submission.creator.name
                                ),
                                mContext.getString(
                                    R.string.filter_posts_urls,
                                    submission.domain
                                ),
                                mContext.getString(
                                    R.string.filter_open_externally,
                                    submission.domain
                                ),
                                mContext.getString(R.string.filter_posts_flair, flair, baseSub)
                            )
                        }
                        chosen = booleanArrayOf(
                            SettingValues.subredditFilters.contains(
                                submission.groupName.lowercase()
                            ),
                            SettingValues.userFilters.contains(
                                submission.creator.name.lowercase()
                            ),
                            SettingValues.domainFilters.contains(
                                submission.domain!!.lowercase()
                            ),
                            SettingValues.alwaysExternal.contains(
                                submission.domain!!.lowercase()
                            ),
                            SettingValues.flairFilters.contains(
                                baseSub + ":" + flair.lowercase().trim { it <= ' ' })
                        )
                        oldChosen = chosen.clone()
                        AlertDialog.Builder(mContext)
                            .setTitle(R.string.filter_title)
                            .setMultiChoiceItems(
                                choices,
                                chosen
                            ) { dialog1: DialogInterface?, which1: Int, isChecked: Boolean ->
                                chosen[which1] = isChecked
                            }
                            .setPositiveButton(
                                R.string.filter_btn
                            ) { dialog12: DialogInterface?, which12: Int ->
                                var filtered: Boolean = false
                                if (chosen[0] && chosen[0] != oldChosen[0]) {
                                    SettingValues.subredditFilters += submission.groupName
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen[0] && chosen[0] != oldChosen[0]) {
                                    SettingValues.subredditFilters -= submission.groupName
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen[1] && chosen[1] != oldChosen[1]) {
                                    SettingValues.userFilters += submission.creator.name
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen[1] && chosen[1] != oldChosen[1]) {
                                    SettingValues.userFilters -= submission.creator.name
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen[2] && chosen[2] != oldChosen[2]) {
                                    SettingValues.domainFilters += submission.domain!!
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen[2] && chosen[2] != oldChosen[2]) {
                                    SettingValues.domainFilters -= submission.domain!!
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen[3] && chosen[3] != oldChosen[3]) {
                                    SettingValues.alwaysExternal += submission.domain!!
                                        .lowercase().trim { it <= ' ' }
                                } else if (!chosen[3] && chosen[3] != oldChosen[3]) {
                                    SettingValues.alwaysExternal -= submission.domain!!
                                        .lowercase().trim { it <= ' ' }
                                }
                                if (chosen.size > 4) {
                                    val s: String = ("$baseSub:$flair")
                                        .lowercase().trim { it <= ' ' }
                                    if (chosen[4] && chosen[4] != oldChosen[4]) {
                                        SettingValues.flairFilters += s
                                        filtered = true
                                    } else if (!chosen[4] && chosen[4] != oldChosen[4]) {
                                        SettingValues.flairFilters -= s
                                    }
                                }
                                if (filtered) {
                                    val toRemove: ArrayList<IPost> = ArrayList()
                                    for (s: IPost? in posts) {
                                        if (s is IPost && PostMatch.doesMatch(s)) {
                                            toRemove.add(s)
                                        }
                                    }
                                    val s: OfflineSubreddit = OfflineSubreddit.getSubreddit(
                                        baseSub,
                                        false, mContext
                                    )!!
                                    for (remove: IPost? in toRemove) {
                                        val pos: Int = posts.indexOf(remove)
                                        posts.removeAt(pos)
                                        if (baseSub != null) {
                                            s.hideMulti(pos)
                                        }
                                    }
                                    s.writeToMemoryNoStorage()
                                    recyclerview.adapter!!
                                        .notifyDataSetChanged()
                                }
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    }

                    3 -> saveSubmission(submission, mContext, holder, full)
                    5 -> {
                        hideSubmission(submission, posts, baseSub, recyclerview, mContext)
                    }

                    7 -> {
                        LinkUtil.openExternally(submission.url!!)
                        if (submission.isNsfw && !SettingValues.storeNSFWHistory) {
                            //Do nothing if the post is NSFW and storeNSFWHistory is not enabled
                        } else if (SettingValues.storeHistory) {
                            HasSeen.addSeen(submission.permalink)
                        }
                    }

                    13 -> LinkUtil.crosspost(submission, mContext)
                    28 -> if (!isAddedToReadLaterList) {
                        ReadLater.setReadLater(submission, true)
                        val s = Snackbar.make(
                            holder.itemView, "Added to read later!",
                            Snackbar.LENGTH_SHORT
                        )
                        val view = s.view
                        val tv = view.findViewById<TextView>(
                            com.google.android.material.R.id.snackbar_text
                        )
                        tv.setTextColor(Color.WHITE)
                        s.setAction(R.string.btn_undo, View.OnClickListener {
                            ReadLater.setReadLater(submission, false)
                            val s2 = Snackbar.make(
                                holder.itemView,
                                "Removed from read later", Snackbar.LENGTH_SHORT
                            )
                            LayoutUtils.showSnackbar(s2)
                        })
                        if (NetworkUtil.isConnected(mContext)) {
                            CommentCacheAsync(
                                listOf(submission), mContext,
                                CommentCacheAsync.SAVED_SUBMISSIONS, booleanArrayOf(true, true)
                            ).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR
                            )
                        }
                        s.show()
                    } else {
                        ReadLater.setReadLater(submission, false)
                        if (isReadLater || !Authentication.didOnline) {
                            val pos = posts.indexOf(submission)
                            posts.remove(submission)
                            recyclerview.adapter!!
                                .notifyItemRemoved(holder.bindingAdapterPosition)
                            val s2 = Snackbar.make(
                                holder.itemView, "Removed from read later",
                                Snackbar.LENGTH_SHORT
                            )
                            val view2 = s2.view
                            val tv2 = view2.findViewById<TextView>(
                                com.google.android.material.R.id.snackbar_text
                            )
                            tv2.setTextColor(Color.WHITE)
                            s2.setAction(R.string.btn_undo) {
                                posts.add(pos, submission)
                                recyclerview.adapter!!.notifyDataSetChanged()
                            }
                        } else {
                            val s2 = Snackbar.make(
                                holder.itemView, "Removed from read later",
                                Snackbar.LENGTH_SHORT
                            )
                            val view2 = s2.view
                            val tv2 = view2.findViewById<TextView>(
                                com.google.android.material.R.id.snackbar_text
                            )
                            s2.show()
                        }
                        OfflineSubreddit.newSubreddit(CommentCacheAsync.SAVED_SUBMISSIONS)
                            .deleteFromMemory(submission.permalink)
                    }

                    4 -> defaultShareText(
                        CompatUtil.fromHtml(submission.title).toString(),
                        StringEscapeUtils.escapeHtml4(submission.url), mContext
                    )

                    12 -> {
                        val reportDialog = MaterialDialog.Builder(mContext)
                            .customView(R.layout.report_dialog, true)
                            .title(R.string.report_post)
                            .positiveText(R.string.btn_report)
                            .negativeText(R.string.btn_cancel)
                            .onPositive { dialog, which ->
                                val reasonGroup = dialog.customView!!
                                    .findViewById<RadioGroup>(R.id.report_reasons)
                                val reportReason: String
                                if (reasonGroup.checkedRadioButtonId == R.id.report_other) {
                                    reportReason = (dialog.customView!!
                                        .findViewById<View>(R.id.input_report_reason) as EditText).text.toString()
                                } else {
                                    reportReason = (reasonGroup
                                        .findViewById<View>(reasonGroup.checkedRadioButtonId) as RadioButton)
                                        .text.toString()
                                }
                                AsyncReportTask(submission, holder.itemView)
                                    .executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR,
                                        reportReason
                                    )
                            }.build()
                        val reasonGroup =
                            reportDialog.customView!!.findViewById<RadioGroup>(R.id.report_reasons)
                        reasonGroup.setOnCheckedChangeListener { group, checkedId ->
                            if (checkedId == R.id.report_other) reportDialog.customView!!.findViewById<View>(
                                R.id.input_report_reason
                            ).visibility = View.VISIBLE else reportDialog.customView!!
                                .findViewById<View>(R.id.input_report_reason).visibility =
                                View.GONE
                        }

                        // Load sub's report reasons and show the appropriate ones
                        object : AsyncTask<Void?, Void?, Ruleset>() {
                            override fun doInBackground(vararg voids: Void?): Ruleset {
                                return Authentication.reddit!!.getRules(submission.groupName)
                            }

                            override fun onPostExecute(rules: Ruleset) {
                                reportDialog.customView!!.findViewById<View>(R.id.report_loading).visibility =
                                    View.GONE
                                if (rules.subredditRules.size > 0) {
                                    val subHeader = TextView(mContext)
                                    subHeader.text = mContext.getString(
                                        R.string.report_sub_rules,
                                        submission.groupName
                                    )
                                    reasonGroup.addView(subHeader, reasonGroup.childCount - 2)
                                }
                                for (rule: SubredditRule in rules.subredditRules) {
                                    if ((rule.kind == SubredditRule.RuleKind.LINK
                                                || rule.kind == SubredditRule.RuleKind.ALL)
                                    ) {
                                        val btn = RadioButton(mContext)
                                        btn.text = rule.violationReason
                                        reasonGroup.addView(btn, reasonGroup.childCount - 2)
                                        btn.layoutParams.width =
                                            WindowManager.LayoutParams.MATCH_PARENT
                                    }
                                }
                                if (rules.siteRules.size > 0) {
                                    val siteHeader = TextView(mContext)
                                    siteHeader.setText(R.string.report_site_rules)
                                    reasonGroup.addView(siteHeader, reasonGroup.childCount - 2)
                                }
                                for (rule: String? in rules.siteRules) {
                                    val btn = RadioButton(mContext)
                                    btn.text = rule
                                    reasonGroup.addView(btn, reasonGroup.childCount - 2)
                                    btn.layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                                }
                            }
                        }.execute()
                        reportDialog.show()
                    }

                    8 -> if (SettingValues.shareLongLink) {
                        defaultShareText(
                            submission.title,
                            "https://reddit.com" + submission.permalink,
                            mContext
                        )
                    } else {
                        defaultShareText(
                            submission.title,
                            submission.permalink,
                            mContext
                        )
                    }

                    6 -> {
                        ClipboardUtil.copyToClipboard(mContext, "Link", submission.url)
                        Toast.makeText(
                            mContext, R.string.submission_link_copied,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    25 -> {
                        val showText = TextView(mContext)
                        showText.text = StringEscapeUtils.unescapeHtml4(
                            submission.title + "\n\n" + submission.body
                        )
                        showText.setTextIsSelectable(true)
                        val sixteen = DisplayUtil.dpToPxVertical(24)
                        showText.setPadding(sixteen, 0, sixteen, 0)
                        AlertDialog.Builder(mContext)
                            .setView(showText)
                            .setTitle("Select text to copy")
                            .setCancelable(true)
                            .setPositiveButton(
                                "COPY SELECTED"
                            ) { dialog13: DialogInterface?, which13: Int ->
                                val selected: String = showText.text
                                    .toString()
                                    .substring(
                                        showText.selectionStart,
                                        showText.selectionEnd
                                    )
                                if (selected.isNotEmpty()) {
                                    ClipboardUtil.copyToClipboard(
                                        mContext,
                                        "Selftext",
                                        selected
                                    )
                                } else {
                                    ClipboardUtil.copyToClipboard(
                                        mContext, "Selftext",
                                        CompatUtil.fromHtml(
                                            (submission.title
                                                    + "\n\n"
                                                    + submission.body)
                                        )
                                    )
                                }
                                Toast.makeText(
                                    mContext,
                                    R.string.submission_comment_copied,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .setNeutralButton(
                                "COPY ALL"
                            ) { dialog14: DialogInterface?, which14: Int ->
                                ClipboardUtil.copyToClipboard(
                                    mContext, "Selftext",
                                    StringEscapeUtils.unescapeHtml4(
                                        (submission.title
                                                + "\n\n"
                                                + submission.body)
                                    )
                                )
                                Toast.makeText(
                                    mContext,
                                    R.string.submission_text_copied,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .show()
                    }
                }
            }
        })
        b.show()
    }

    private fun saveSubmission(
        submission: IPost, mContext: Activity,
        holder: SubmissionViewHolder, full: Boolean
    ) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    if (ActionStates.isSaved(submission)) {
                        //AccountManager(Authentication.reddit).unsave(submission)
                        ActionStates.setSaved(submission, false)
                    } else {
                        //AccountManager(Authentication.reddit).save(submission)
                        ActionStates.setSaved(submission, true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                val s: Snackbar
                try {
                    if (ActionStates.isSaved(submission)) {
                        BlendModeUtil.tintImageViewAsSrcAtop(
                            (holder.save as ImageView),
                            ContextCompat.getColor(mContext, R.color.md_amber_500)
                        )
                        holder.save.setContentDescription(mContext.getString(R.string.btn_unsave))
                        s = Snackbar.make(
                            holder.itemView, R.string.submission_info_saved,
                            Snackbar.LENGTH_LONG
                        )
                        if (Authentication.me!!.hasGold()) {
                            s.setAction(
                                R.string.category_categorize
                            ) { categorizeSaved(submission, holder.itemView, mContext) }
                        }
                        AnimatorUtil.setFlashAnimation(
                            holder.itemView, holder.save,
                            ContextCompat.getColor(mContext, R.color.md_amber_500)
                        )
                    } else {
                        s = Snackbar.make(
                            holder.itemView, R.string.submission_info_unsaved,
                            Snackbar.LENGTH_SHORT
                        )
                        val getTintColor = if (holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id)
                                    == "none") || full
                        ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                        BlendModeUtil.tintImageViewAsSrcAtop(
                            (holder.save as ImageView),
                            getTintColor
                        )
                        holder.save.setContentDescription(mContext.getString(R.string.btn_save))
                    }
                    LayoutUtils.showSnackbar(s)
                } catch (ignored: Exception) {
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun categorizeSaved(
        submission: IPost, itemView: View,
        mContext: Context
    ) {
        object : AsyncTask<Void?, Void?, List<String?>>() {
            var d: Dialog? = null
            public override fun onPreExecute() {
                d = MaterialDialog.Builder(mContext).progress(true, 100)
                    .title(R.string.profile_category_loading)
                    .content(R.string.misc_please_wait)
                    .show()
            }

            override fun doInBackground(vararg params: Void?): List<String?> {
                return try {
                    val categories: MutableList<String?> = ArrayList(
                        AccountManager(Authentication.reddit).savedCategories
                    )
                    categories.add("New category")
                    categories
                } catch (e: Exception) {
                    e.printStackTrace()
                    object : ArrayList<String?>() {
                        init {
                            add("New category")
                        }
                    }
                    //sub probably has no flairs?
                }
            }

            public override fun onPostExecute(data: List<String?>) {
                try {
                    MaterialDialog.Builder(mContext).items(data)
                        .title(R.string.sidebar_select_flair)
                        .itemsCallback(object : ListCallback {
                            override fun onSelection(
                                dialog: MaterialDialog, itemView: View,
                                which: Int, text: CharSequence
                            ) {
                                val t = data[which]
                                if (which == data.size - 1) {
                                    MaterialDialog.Builder(mContext).title(
                                        R.string.category_set_name
                                    )
                                        .input(mContext.getString(
                                            R.string.category_set_name_hint
                                        ),
                                            null,
                                            false
                                        ) { dialog1: MaterialDialog?, input: CharSequence? -> }
                                        .positiveText(R.string.btn_set)
                                        .onPositive(
                                            object : SingleButtonCallback {
                                                override fun onClick(
                                                    dialog: MaterialDialog,
                                                    which: DialogAction
                                                ) {
                                                    val flair = dialog.inputEditText!!
                                                        .text
                                                        .toString()
                                                    object : AsyncTask<Void?, Void?, Boolean>() {
                                                        override fun doInBackground(
                                                            vararg params: Void?
                                                        ): Boolean {
                                                            try {
                                                                //AccountManager(Authentication.reddit).save(submission, flair)
                                                                return true
                                                            } catch (e: ApiException) {
                                                                e.printStackTrace()
                                                                return false
                                                            }
                                                        }

                                                        override fun onPostExecute(
                                                            done: Boolean
                                                        ) {
                                                            val s: Snackbar
                                                            if (done) {
                                                                if (itemView != null) {
                                                                    s = Snackbar.make(
                                                                        itemView,
                                                                        R.string.submission_info_saved,
                                                                        Snackbar.LENGTH_SHORT
                                                                    )
                                                                    LayoutUtils.showSnackbar(s)
                                                                }
                                                            } else {
                                                                if (itemView != null) {
                                                                    s = Snackbar.make(
                                                                        itemView,
                                                                        R.string.category_set_error,
                                                                        Snackbar.LENGTH_SHORT
                                                                    )
                                                                    LayoutUtils.showSnackbar(s)
                                                                }
                                                            }
                                                        }
                                                    }.executeOnExecutor(
                                                        THREAD_POOL_EXECUTOR
                                                    )
                                                }
                                            })
                                        .negativeText(R.string.btn_cancel)
                                        .show()
                                } else {
                                    object : AsyncTask<Void?, Void?, Boolean>() {
                                        override fun doInBackground(vararg params: Void?): Boolean {
                                            return try {
                                                //AccountManager(Authentication.reddit).save(submission, t)
                                                true
                                            } catch (e: ApiException) {
                                                e.printStackTrace()
                                                false
                                            }
                                        }

                                        override fun onPostExecute(done: Boolean) {
                                            val s: Snackbar
                                            if (done) {
                                                if (itemView != null) {
                                                    s = Snackbar.make(
                                                        itemView,
                                                        R.string.submission_info_saved,
                                                        Snackbar.LENGTH_SHORT
                                                    )
                                                    LayoutUtils.showSnackbar(s)
                                                }
                                            } else {
                                                if (itemView != null) {
                                                    s = Snackbar.make(
                                                        itemView,
                                                        R.string.category_set_error,
                                                        Snackbar.LENGTH_SHORT
                                                    )
                                                    LayoutUtils.showSnackbar(s)
                                                }
                                            }
                                        }
                                    }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                                }
                            }
                        })
                        .show()
                    if (d != null) {
                        d!!.dismiss()
                    }
                } catch (ignored: Exception) {
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun hideSubmission(
        submission: IPost,
        posts: MutableList<IPost>, baseSub: String?, recyclerview: RecyclerView, c: Context?
    ) {
        val pos = posts.indexOf(submission)
        if (pos != -1) {
            if (submission.isHidden) {
                posts.removeAt(pos)
                Hidden.undoHidden(submission)
                recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                val snack = Snackbar.make(
                    recyclerview, R.string.submission_info_unhidden,
                    Snackbar.LENGTH_LONG
                )
                LayoutUtils.showSnackbar(snack)
            } else {
                val t = posts[pos]
                posts.removeAt(pos)
                Hidden.setHidden(t)
                val s: OfflineSubreddit?
                var success = false
                if (baseSub != null) {
                    s = OfflineSubreddit.getSubreddit(baseSub, false, c)
                    try {
                        s!!.hide(pos)
                        success = true
                    } catch (e: Exception) {
                    }
                } else {
                    success = false
                    s = null
                }
                recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                val finalSuccess = success
                val snack = Snackbar.make(
                    recyclerview, R.string.submission_info_hidden,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.btn_undo) {
                        if ((baseSub != null) && (s != null) && finalSuccess) {
                            s.unhideLast()
                        }
                        posts.add(pos, t)
                        recyclerview.adapter!!.notifyItemInserted(pos + 1)
                        Hidden.undoHidden(t)
                    }
                LayoutUtils.showSnackbar(snack)
            }
        }
    }

    private fun showModBottomSheet(
        mContext: Activity,
        submission: IPost, posts: MutableList<IPost>, holder: SubmissionViewHolder,
        recyclerview: RecyclerView, reports: Map<String, Int>,
        reports2: Map<String, String>
    ) {
        val res = mContext.resources
        val attrs = intArrayOf(R.attr.tintColor)
        val ta = mContext.obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val profile =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_account_circle, null)
        val report = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_report, null)
        val approve = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_thumb_up, null)
        val nsfw =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_visibility_off, null)
        val spoiler =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_remove_circle, null)
        val pin =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_bookmark_border, null)
        val lock = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_lock, null)
        val flair = ResourcesCompat.getDrawable(
            mContext.resources,
            R.drawable.ic_format_quote, null
        )
        val remove = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_close, null)
        val remove_reason =
            ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_announcement, null)
        val ban = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_gavel, null)
        val spam = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_flag, null)
        val distinguish = ResourcesCompat.getDrawable(
            mContext.resources, R.drawable.ic_star,
            null
        )
        val note = ResourcesCompat.getDrawable(mContext.resources, R.drawable.ic_note, null)
        val drawableSet = Arrays.asList(
            profile, report, approve, spam, nsfw,
            pin, flair, remove, spoiler, remove_reason,
            ban, spam, distinguish, lock, note
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(mContext).title(CompatUtil.fromHtml(submission.title))
        val reportCount = reports.size + reports2.size
        b.sheet(
            0, (report)!!,
            res.getQuantityString(R.plurals.mod_btn_reports, reportCount, reportCount)
        )
        if (SettingValues.toolboxEnabled) {
            b.sheet(24, (note)!!, res.getString(R.string.mod_usernotes_view))
        }
        val approved = false
        val whoApproved = ""
        b.sheet(1, (approve)!!, res.getString(R.string.mod_btn_approve))
        b.sheet(6, (remove)!!, mContext.getString(R.string.mod_btn_remove))
            .sheet(7, (remove_reason)!!, res.getString(R.string.mod_btn_remove_reason))
            .sheet(30, (spam)!!, res.getString(R.string.mod_btn_spam))

        // b.sheet(2, spam, mContext.getString(R.string.mod_btn_spam)) todo this
        b.sheet(20, (flair)!!, res.getString(R.string.mod_btn_submission_flair))
        val isNsfw = submission.isNsfw
        if (isNsfw) {
            b.sheet(3, (nsfw)!!, res.getString(R.string.mod_btn_unmark_nsfw))
        } else {
            b.sheet(3, (nsfw)!!, res.getString(R.string.mod_btn_mark_nsfw))
        }
        val isSpoiler = submission.isSpoiler
        if (isSpoiler) {
            b.sheet(12, (nsfw)!!, res.getString(R.string.mod_btn_unmark_spoiler))
        } else {
            b.sheet(12, (nsfw)!!, res.getString(R.string.mod_btn_mark_spoiler))
        }
        val locked = submission.isLocked
        if (locked) {
            b.sheet(9, (lock)!!, res.getString(R.string.mod_btn_unlock_thread))
        } else {
            b.sheet(9, (lock)!!, res.getString(R.string.mod_btn_lock_thread))
        }
        val stickied = submission.isFeatured
        if (!SubmissionCache.removed.contains(submission.permalink)) {
            if (stickied) {
                b.sheet(4, (pin)!!, res.getString(R.string.mod_btn_unpin))
            } else {
                b.sheet(4, (pin)!!, res.getString(R.string.mod_btn_pin))
            }
        }
        val distinguished = (submission.regalia == DistinguishedStatus.MODERATOR
                || submission.regalia == DistinguishedStatus.ADMIN)
        if (submission.creator.name.equals(Authentication.name, ignoreCase = true)) {
            if (distinguished) {
                b.sheet(5, (distinguish)!!, "Undistingiush")
            } else {
                b.sheet(5, (distinguish)!!, "Distinguish")
            }
        }
        val finalWhoApproved = whoApproved
        val finalApproved = approved
        b.sheet(8, (profile)!!, res.getString(R.string.mod_btn_author))
        b.sheet(23, (ban)!!, mContext.getString(R.string.mod_ban_user))
        b.listener(object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    0 -> object : AsyncTask<Void?, Void?, ArrayList<String>>() {
                        override fun doInBackground(vararg params: Void?): ArrayList<String> {
                            val finalReports = ArrayList<String>()
                            for (entry: Map.Entry<String, Int> in reports.entries) {
                                finalReports.add(entry.value.toString() + "× " + entry.key)
                            }
                            for (entry: Map.Entry<String, String> in reports2.entries) {
                                finalReports.add(entry.key + ": " + entry.value)
                            }
                            if (finalReports.isEmpty()) {
                                finalReports.add(mContext.getString(R.string.mod_no_reports))
                            }
                            return finalReports
                        }

                        public override fun onPostExecute(data: ArrayList<String>) {
                            AlertDialog.Builder(mContext)
                                .setTitle(R.string.mod_reports)
                                .setItems(data.toTypedArray<CharSequence>(), null)
                                .show()
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                    1 -> if (finalApproved) {
                        val i = Intent(mContext, Profile::class.java)
                        i.putExtra(Profile.EXTRA_PROFILE, finalWhoApproved)
                        mContext.startActivity(i)
                    } else {
                        approveSubmission(mContext, posts, submission, recyclerview, holder)
                    }

                    2 -> {}
                    3 -> if (isNsfw) {
                        unNsfwSubmission(mContext, submission, holder)
                    } else {
                        setPostNsfw(mContext, submission, holder)
                    }

                    12 -> if (isSpoiler) {
                        unSpoiler(mContext, submission, holder)
                    } else {
                        setSpoiler(mContext, submission, holder)
                    }

                    9 -> if (locked) {
                        unLockSubmission(mContext, submission, holder)
                    } else {
                        lockSubmission(mContext, submission, holder)
                    }

                    4 -> if (stickied) {
                        unStickySubmission(mContext, submission, holder)
                    } else {
                        stickySubmission(mContext, submission, holder)
                    }

                    5 -> if (distinguished) {
                        unDistinguishSubmission(mContext, submission, holder)
                    } else {
                        distinguishSubmission(mContext, submission, holder)
                    }

                    6 -> removeSubmission(mContext, submission, posts, recyclerview, holder, false)
                    7 -> if ((SettingValues.removalReasonType == SettingValues.RemovalReasonType.TOOLBOX.ordinal
                                && ToolboxUI.canShowRemoval(submission.groupName))
                    ) {
                        ToolboxUI.showRemoval(
                            mContext,
                            submission,
                            object : CompletedRemovalCallback {
                                override fun onComplete(success: Boolean) {
                                    if (success) {
                                        SubmissionCache.removed.add(submission.permalink)
                                        SubmissionCache.approved.remove(submission.permalink)
                                        SubmissionCache.updateInfoSpannable(
                                            submission, mContext,
                                            submission.groupName
                                        )
                                        if (mContext is ModQueue) {
                                            val pos = posts.indexOf(submission)
                                            posts.remove(submission)
                                            if (pos == 0) {
                                                recyclerview.adapter!!.notifyDataSetChanged()
                                            } else {
                                                recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                                            }
                                        } else {
                                            recyclerview.adapter!!.notifyItemChanged(holder.bindingAdapterPosition)
                                        }
                                        val s = Snackbar.make(
                                            holder.itemView, R.string.submission_removed,
                                            Snackbar.LENGTH_LONG
                                        )
                                        LayoutUtils.showSnackbar(s)
                                    } else {
                                        AlertDialog.Builder(mContext)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_retry_later)
                                            .show()
                                    }
                                }
                            })
                    } else { // Show a Slide reason dialog if we can't show a toolbox or reddit one
                        doRemoveSubmissionReason(mContext, submission, posts, recyclerview, holder)
                    }

                    30 -> removeSubmission(mContext, submission, posts, recyclerview, holder, true)
                    8 -> {
                        val i = Intent(mContext, Profile::class.java)
                        i.putExtra(Profile.EXTRA_PROFILE, submission.creator.name)
                        mContext.startActivity(i)
                    }

                    20 -> doSetFlair(mContext, submission, holder)
                    23 ->                         //ban a user
                        showBan(mContext, holder.itemView, submission, "", "", "", "")

                    24 -> ToolboxUI.showUsernotes(
                        mContext, submission.creator.name, submission.groupName,
                        "l," + submission.id
                    )
                }
            }
        })
        b.show()
    }

    private fun doRemoveSubmissionReason(
        mContext: Activity,
        submission: IPost, posts: MutableList<IPost>, recyclerview: RecyclerView,
        holder: SubmissionViewHolder
    ) {
        reason = ""
        MaterialDialog.Builder(mContext).title(R.string.mod_remove_title)
            .positiveText(R.string.btn_remove)
            .alwaysCallInputCallback()
            .input(mContext.getString(R.string.mod_remove_hint),
                mContext.getString(R.string.mod_remove_template), false
            ) { dialog, input -> reason = input.toString() }
            .inputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            .neutralText(R.string.mod_remove_insert_draft)
            .onPositive { dialog, which ->
                removeSubmissionReason(
                    submission, mContext, posts, reason!!, holder,
                    recyclerview
                )
            }
            .negativeText(R.string.btn_cancel)
            .onNegative {_, _ -> }
            .show()
    }

    private fun removeSubmissionReason(
        submission: IPost,
        mContext: Activity, posts: MutableList<IPost>, reason: String,
        holder: SubmissionViewHolder, recyclerview: RecyclerView
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    SubmissionCache.removed.add(submission.permalink)
                    SubmissionCache.approved.remove(submission.permalink)
                    SubmissionCache.updateInfoSpannable(
                        submission, mContext,
                        submission.groupName
                    )
                    if (mContext is ModQueue) {
                        val pos = posts.indexOf(submission)
                        posts.remove(submission)
                        if (pos == 0) {
                            recyclerview.adapter!!.notifyDataSetChanged()
                        } else {
                            recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                        }
                    } else {
                        recyclerview.adapter!!.notifyItemChanged(holder.bindingAdapterPosition)
                    }
                    val s = Snackbar.make(
                        holder.itemView, R.string.submission_removed,
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    val toDistinguish = null
                        //AccountManager(Authentication.reddit).reply(submission, reason)
                        //ModerationManager(Authentication.reddit).remove(submission, false)
                        //ModerationManager(Authentication.reddit).setDistinguishedStatus(
                        //    Authentication.reddit!!["t1_$toDistinguish"][0],
                        //    DistinguishedStatus.MODERATOR
                        //)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun removeSubmission(
        mContext: Activity,
        submission: IPost, posts: MutableList<IPost>, recyclerview: RecyclerView,
        holder: SubmissionViewHolder, spam: Boolean
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                SubmissionCache.removed.add(submission.permalink)
                SubmissionCache.approved.remove(submission.permalink)
                SubmissionCache.updateInfoSpannable(
                    submission, mContext,
                    submission.groupName
                )
                if (b) {
                    if (mContext is ModQueue) {
                        val pos = posts.indexOf(submission)
                        posts.remove(submission)
                        if (pos == 0) {
                            recyclerview.adapter!!.notifyDataSetChanged()
                        } else {
                            recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                        }
                    } else {
                        recyclerview.adapter!!.notifyItemChanged(holder.bindingAdapterPosition)
                    }
                    val s = Snackbar.make(
                        holder.itemView, R.string.submission_removed,
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).remove(submission, spam)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                } catch (e: NetworkException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun doSetFlair(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, ArrayList<String?>?>() {
            var flair: ArrayList<FlairTemplate>? = null
            protected override fun doInBackground(vararg params: Void?): ArrayList<String?>? {
                val allFlairs = FluentRedditClient(Authentication.reddit).subreddit(
                    submission.groupName
                ).flair()
                try {
                    flair = ArrayList(/*allFlairs.options(submission)*/)
                    val finalFlairs = ArrayList<String?>()
                    for (temp: FlairTemplate in flair!!) {
                        finalFlairs.add(temp.text)
                    }
                    return finalFlairs
                } catch (e: Exception) {
                    e.printStackTrace()
                    //sub probably has no flairs?
                }
                return null
            }

            public override fun onPostExecute(data: ArrayList<String?>?) {
                try {
                    if (data!!.isEmpty()) {
                        AlertDialog.Builder(mContext)
                            .setTitle(R.string.mod_flair_none_found)
                            .setPositiveButton(R.string.btn_ok, null)
                            .show()
                    } else {
                        showFlairSelectionDialog(mContext, submission, data, flair, holder)
                    }
                } catch (ignored: Exception) {
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun showFlairSelectionDialog(
        mContext: Activity, submission: IPost,
        data: ArrayList<String?>?, flair: ArrayList<FlairTemplate>?,
        holder: SubmissionViewHolder
    ) {
        MaterialDialog.Builder(mContext).items((data)!!)
            .title(R.string.sidebar_select_flair)
            .itemsCallback { dialog, itemView, which, text ->
                val t = flair!![which]
                if (t.isTextEditable) {
                    showFlairEditDialog(mContext, submission, t, holder)
                } else {
                    setFlair(mContext, null, submission, t, holder)
                }
            }
            .show()
    }

    private fun showFlairEditDialog(
        mContext: Activity, submission: IPost,
        t: FlairTemplate, holder: SubmissionViewHolder
    ) {
        MaterialDialog.Builder(mContext).title(R.string.sidebar_select_flair_text)
            .input(mContext.getString(R.string.mod_flair_hint), t.text, true
            ) { dialog: MaterialDialog?, input: CharSequence? -> }
            .positiveText(R.string.btn_set)
            .onPositive { dialog, which ->
                val flair = dialog.inputEditText!!.text.toString()
                setFlair(mContext, flair, submission, t, holder)
            }
            .negativeText(R.string.btn_cancel)
            .show()
    }

    private fun setFlair(
        mContext: Context, flair: String?, submission: IPost,
        t: FlairTemplate, holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            override fun doInBackground(vararg params: Void?): Boolean {
                return try {
                    //ModerationManager(Authentication.reddit).setFlair(
                    //    submission.groupName, t, flair, submission
                    //)
                    true
                } catch (e: ApiException) {
                    e.printStackTrace()
                    false
                }
            }

            override fun onPostExecute(done: Boolean) {
                var s: Snackbar? = null
                if (done) {
                    if (holder.itemView != null) {
                        s = Snackbar.make(
                            holder.itemView, R.string.snackbar_flair_success,
                            Snackbar.LENGTH_SHORT
                        )
                    }
                    if (holder.itemView != null) {
                        SubmissionCache.updateTitleFlair(submission, flair, mContext)
                        doText(holder, submission, mContext, submission.groupName, false)
                    }
                } else {
                    if (holder.itemView != null) {
                        s = Snackbar.make(
                            holder.itemView, R.string.snackbar_flair_error,
                            Snackbar.LENGTH_SHORT
                        )
                    }
                }
                if (s != null) {
                    LayoutUtils.showSnackbar(s)
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doText(
        holder: SubmissionViewHolder, submission: IPost?, mContext: Context,
        baseSub: String?, full: Boolean
    ) {
        val t = SubmissionCache.getTitleLine(submission!!, mContext)
        val l = SubmissionCache.getInfoLine(submission!!, mContext, baseSub!!)
        val c = SubmissionCache.getCrosspostLine(submission!!, mContext)
        val textSizeAttr = intArrayOf(R.attr.font_cardtitle, R.attr.font_cardinfo)
        val a = mContext.obtainStyledAttributes(textSizeAttr)
        val textSizeT = a.getDimensionPixelSize(0, 18)
        val textSizeI = a.getDimensionPixelSize(1, 14)
        t!!.setSpan(AbsoluteSizeSpan(textSizeT), 0, t.length, 0)
        l!!.setSpan(AbsoluteSizeSpan(textSizeI), 0, l.length, 0)
        val s = SpannableStringBuilder()
        if (SettingValues.titleTop) {
            s.append(t)
            s.append("\n")
            s.append(l)
        } else {
            s.append(l)
            s.append("\n")
            s.append(t)
        }
        if (!full && c != null) {
            c.setSpan(AbsoluteSizeSpan(textSizeI), 0, c.length, 0)
            s.append("\n")
            s.append(c)
        }
        a.recycle()
        holder.title.text = s
    }

    private fun stickySubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, R.string.really_pin_submission_message,
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setSticky(submission, true)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                } catch (e: NetworkException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun unStickySubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, R.string.really_unpin_submission_message,
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setSticky(submission, false)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun lockSubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s =
                        Snackbar.make(holder.itemView, R.string.mod_locked, Snackbar.LENGTH_LONG)
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setLocked(submission)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun unLockSubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s =
                        Snackbar.make(holder.itemView, R.string.mod_unlocked, Snackbar.LENGTH_LONG)
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setUnlocked(submission)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun distinguishSubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, "IPost distinguished",
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setDistinguishedStatus(
                    //    submission,
                    //    DistinguishedStatus.MODERATOR
                    //)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun unDistinguishSubmission(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, "IPost distinguish removed",
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setDistinguishedStatus(
                    //    submission,
                    //    DistinguishedStatus.MODERATOR
                    //)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun setPostNsfw(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(holder.itemView, "NSFW status set", Snackbar.LENGTH_LONG)
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setNsfw(submission, true)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun unNsfwSubmission(
        mContext: Context, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        //todo update view with NSFW tag
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, "NSFW status removed",
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setNsfw(submission, false)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun setSpoiler(
        mContext: Activity, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, "Spoiler status set",
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setSpoiler(submission, true)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun unSpoiler(
        mContext: Context, submission: IPost,
        holder: SubmissionViewHolder
    ) {
        //todo update view with NSFW tag
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, "Spoiler status removed",
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).setSpoiler(submission, false)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun approveSubmission(
        mContext: Context, posts: MutableList<IPost>,
        submission: IPost, recyclerview: RecyclerView,
        holder: SubmissionViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    SubmissionCache.approved.add(submission.permalink)
                    SubmissionCache.removed.remove(submission.permalink)
                    SubmissionCache.updateInfoSpannable(
                        submission, mContext,
                        submission.groupName
                    )
                    if (mContext is ModQueue) {
                        val pos = posts.indexOf(submission)
                        posts.remove(submission)
                        if (pos == 0) {
                            recyclerview.adapter!!.notifyDataSetChanged()
                        } else {
                            recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                        }
                    } else {
                        recyclerview.adapter!!.notifyItemChanged(holder.bindingAdapterPosition)
                    }
                    try {
                        val s = Snackbar.make(
                            holder.itemView, R.string.mod_approved,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } catch (ignored: Exception) {
                    }
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            protected override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    //ModerationManager(Authentication.reddit).approve(submission)
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun showBan(
        mContext: Context, mToolbar: View?, submission: IPost,
        rs: String?, nt: String?, msg: String?, t: String?
    ) {
        val l = LinearLayout(mContext)
        l.orientation = LinearLayout.VERTICAL
        val sixteen = DisplayUtil.dpToPxVertical(16)
        l.setPadding(sixteen, 0, sixteen, 0)
        val reason = EditText(mContext)
        reason.setHint(R.string.mod_ban_reason)
        reason.setText(rs)
        reason.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        l.addView(reason)
        val note = EditText(mContext)
        note.setHint(R.string.mod_ban_note_mod)
        note.setText(nt)
        note.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        l.addView(note)
        val message = EditText(mContext)
        message.setHint(R.string.mod_ban_note_user)
        message.setText(msg)
        message.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        l.addView(message)
        val time = EditText(mContext)
        time.setHint(R.string.mod_ban_time)
        time.setText(t)
        time.inputType = InputType.TYPE_CLASS_NUMBER
        l.addView(time)
        AlertDialog.Builder(mContext)
            .setView(l)
            .setTitle(mContext.getString(R.string.mod_ban_title, submission.creator.name))
            .setCancelable(true)
            .setPositiveButton(R.string.mod_btn_ban
            ) { dialog: DialogInterface?, which: Int ->
                //to ban
                if (reason.text.toString().isEmpty()) {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.mod_ban_reason_required)
                        .setMessage(R.string.misc_please_try_again)
                        .setPositiveButton(
                            R.string.btn_ok,
                            DialogInterface.OnClickListener { dialog1: DialogInterface?, which1: Int ->
                                showBan(
                                    mContext, mToolbar, submission,
                                    reason.text.toString(),
                                    note.text.toString(),
                                    message.text.toString(),
                                    time.text.toString()
                                )
                            })
                        .setCancelable(false)
                        .show()
                } else {
                    object : AsyncTask<Void?, Void?, Boolean>() {
                        protected override fun doInBackground(vararg params: Void?): Boolean {
                            try {
                                var n: String? = note.text.toString()
                                var m: String? = message.text.toString()
                                if (n!!.isEmpty()) {
                                    n = null
                                }
                                if (m!!.isEmpty()) {
                                    m = null
                                }
                                if (time.text.toString().isEmpty()) {
                                    //ModerationManager(
                                    //    Authentication.reddit
                                    //).banUserPermanently(
                                    //    submission.groupName,
                                    //    submission.creator.name,
                                    //    reason.text.toString(), n, m
                                    //)
                                } else {
                                    //ModerationManager(Authentication.reddit).banUser(
                                    //    submission.groupName,
                                    //    submission.creator.name,
                                    //    reason.text.toString(),
                                    //    n,
                                    //    m,
                                    //    time.text.toString().toInt()
                                    //)
                                }
                                return true
                            } catch (e: Exception) {
                                if (e is InvalidScopeException) {
                                    scope = true
                                }
                                e.printStackTrace()
                                return false
                            }
                        }

                        var scope: Boolean = false
                        override fun onPostExecute(done: Boolean) {
                            val s: Snackbar?
                            if (done) {
                                s = Snackbar.make(
                                    (mToolbar)!!, R.string.mod_ban_success,
                                    Snackbar.LENGTH_SHORT
                                )
                            } else {
                                if (scope) {
                                    AlertDialog.Builder(mContext)
                                        .setTitle(R.string.mod_ban_reauth)
                                        .setMessage(R.string.mod_ban_reauth_question)
                                        .setPositiveButton(
                                            R.string.btn_ok,
                                            DialogInterface.OnClickListener { dialog12: DialogInterface?, which12: Int ->
                                                val i: Intent =
                                                    Intent(mContext, Reauthenticate::class.java)
                                                mContext.startActivity(i)
                                            })
                                        .setNegativeButton(R.string.misc_maybe_later, null)
                                        .setCancelable(false)
                                        .show()
                                }
                                s = Snackbar.make(
                                    (mToolbar)!!, R.string.mod_ban_fail,
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                    .setAction(R.string.misc_try_again
                                    ) {
                                        showBan(
                                            mContext, mToolbar,
                                            submission,
                                            reason.text.toString(),
                                            note.text.toString(),
                                            message.text
                                                .toString(),
                                            time.text.toString()
                                        )
                                    }
                            }
                            if (s != null) {
                                LayoutUtils.showSnackbar(s)
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    fun populateSubmissionViewHolder(
        holder: SubmissionViewHolder, submission: IPost, mContext: Activity,
        fullscreen: Boolean, full: Boolean, posts: MutableList<IPost>,
        recyclerview: RecyclerView, same: Boolean, offline: Boolean,
        baseSub: String?, adapter: CommentAdapter?
    ) {
        holder.itemView.findViewById<View>(R.id.vote).visibility = View.GONE
        if ((!offline
                    && (UserSubscriptions.modOf != null
                    ) && (submission.groupName != null
                    ) && UserSubscriptions.modOf!!.contains(
                submission.groupName.lowercase()
            ))
        ) {
            holder.mod.visibility = View.VISIBLE
            val reports = submission.userReports
            val reports2 = submission.moderatorReports
            if (reports.size + reports2.size > 0) {
                BlendModeUtil.tintImageViewAsSrcAtop(
                    (holder.mod as ImageView),
                    ContextCompat.getColor(mContext, R.color.md_red_300)
                )
            } else {
                val getTintColor = if ((holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id) == "none")
                            || full)
                ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                BlendModeUtil.tintImageViewAsSrcAtop((holder.mod as ImageView), getTintColor)
            }
            holder.mod.setOnClickListener {
                showModBottomSheet(
                    mContext, submission, posts, holder, recyclerview, reports,
                    reports2
                )
            }
        } else {
            holder.mod.visibility = View.GONE
        }
        holder.menu.setOnClickListener {
            showBottomSheet(
                mContext,
                submission,
                holder,
                posts,
                baseSub,
                recyclerview,
                full
            )
        }

        //Use this to offset the submission score
        var submissionScore = submission.score
        val commentCount = submission.commentCount
        val more = LastComments.commentsSince(submission)
        holder.comments.text = String.format(
            Locale.getDefault(), "%d %s", commentCount,
            (if ((more > 0 && SettingValues.commentLastVisit)) "(+$more)" else "")
        )
        val scoreRatio =
            if ((SettingValues.upvotePercentage && full && (submission.upvoteRatio != null))) ("("
                    + (submission.upvoteRatio * 100).toInt() + "%)") else ""
        if (!scoreRatio.isEmpty()) {
            val percent = holder.itemView.findViewById<TextView>(R.id.percent)
            percent.visibility = View.VISIBLE
            percent.text = scoreRatio
            val numb = (submission.upvoteRatio)
            if (numb <= .5) {
                if (numb <= .1) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_500))
                } else if (numb <= .3) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_400))
                } else {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_300))
                }
            } else {
                if (numb >= .9) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_500))
                } else if (numb >= .7) {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_400))
                } else {
                    percent.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_300))
                }
            }
        }
        val downvotebutton = holder.downvote as ImageView
        val upvotebutton = holder.upvote as ImageView
        if (submission.isArchived) {
            downvotebutton.visibility = View.GONE
            upvotebutton.visibility = View.GONE
        } else if (Authentication.isLoggedIn && Authentication.didOnline) {
            if (SettingValues.actionbarVisible && downvotebutton.visibility != View.VISIBLE) {
                downvotebutton.visibility = View.VISIBLE
                upvotebutton.visibility = View.VISIBLE
            }
        }
        when (ActionStates.getVoteDirection(submission)) {
            VoteDirection.UPVOTE -> {
                holder.score.setTextColor(ContextCompat.getColor(mContext, R.color.md_orange_500))
                BlendModeUtil.tintImageViewAsSrcAtop(
                    upvotebutton,
                    ContextCompat.getColor(mContext, R.color.md_orange_500)
                )
                upvotebutton.contentDescription = mContext.getString(R.string.btn_upvoted)
                holder.score.setTypeface(null, Typeface.BOLD)
                val getTintColor = if ((holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id) == "none")
                            || full)
                ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, getTintColor)
                downvotebutton.contentDescription = mContext.getString(R.string.btn_downvote)
                if (submission.myVote != VoteDirection.UPVOTE) {
                    if (submission.myVote == VoteDirection.DOWNVOTE) ++submissionScore
                    ++submissionScore //offset the score by +1
                }
            }

            VoteDirection.DOWNVOTE -> {
                holder.score.setTextColor(ContextCompat.getColor(mContext, R.color.md_blue_500))
                BlendModeUtil.tintImageViewAsSrcAtop(
                    downvotebutton,
                    ContextCompat.getColor(mContext, R.color.md_blue_500)
                )
                downvotebutton.contentDescription = mContext.getString(R.string.btn_downvoted)
                holder.score.setTypeface(null, Typeface.BOLD)
                val getTintColor = if ((holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id) == "none")
                            || full)
                ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, getTintColor)
                upvotebutton.contentDescription = mContext.getString(R.string.btn_upvote)
                if (submission.myVote != VoteDirection.DOWNVOTE) {
                    if (submission.myVote == VoteDirection.UPVOTE) --submissionScore
                    --submissionScore //offset the score by +1
                }
            }

            VoteDirection.NO_VOTE -> {
                holder.score.setTextColor(holder.comments.currentTextColor)
                holder.score.setTypeface(null, Typeface.NORMAL)
                val getTintColor = if ((holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id) == "none")
                            || full)
                ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                val imageViewSet = Arrays.asList(downvotebutton, upvotebutton)
                BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, getTintColor)
                upvotebutton.contentDescription = mContext.getString(R.string.btn_upvote)
                downvotebutton.contentDescription = mContext.getString(R.string.btn_downvote)
            }
        }


        //if the submission is already at 0pts, keep it at 0pts
        submissionScore = Math.max(submissionScore, 0)
        if (submissionScore >= 10000 && SettingValues.abbreviateScores) {
            holder.score.text = String.format(
                Locale.getDefault(), "%.1fk",
                ((submissionScore.toDouble()) / 1000)
            )
        } else {
            holder.score.text = String.format(Locale.getDefault(), "%d", submissionScore)
        }

        //Save the score so we can use it in the OnClickListeners for the vote buttons
        val SUBMISSION_SCORE = submissionScore
        val hideButton: ImageView? = holder.hide as ImageView?
        if (hideButton != null) {
            if (SettingValues.hideButton && Authentication.isLoggedIn) {
                hideButton.setOnClickListener(View.OnClickListener { hideSubmission(submission, posts, baseSub, recyclerview, mContext) })
            } else {
                hideButton.visibility = View.GONE
            }
        }
        if (Authentication.isLoggedIn && Authentication.didOnline) {
            if (ActionStates.isSaved(submission)) {
                BlendModeUtil.tintImageViewAsSrcAtop(
                    (holder.save as ImageView),
                    ContextCompat.getColor(mContext, R.color.md_amber_500)
                )
                holder.save.setContentDescription(mContext.getString(R.string.btn_unsave))
            } else {
                val getTintColor = if ((holder.itemView.getTag(holder.itemView.id) != null
                            && (holder.itemView.getTag(holder.itemView.id) == "none")
                            || full)
                ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                BlendModeUtil.tintImageViewAsSrcAtop((holder.save as ImageView), getTintColor)
                holder.save.setContentDescription(mContext.getString(R.string.btn_save))
            }
            holder.save.setOnClickListener { saveSubmission(submission, mContext, holder, full) }
        }
        if (((!SettingValues.saveButton && !full
                    ) || !Authentication.isLoggedIn
                    || !Authentication.didOnline)
        ) {
            holder.save.visibility = View.GONE
        }
        val thumbImage2: ImageView? = (holder.thumbimage as ImageView)
        if (holder.leadImage.thumbImage2 == null) {
            holder.leadImage.setThumbnail(thumbImage2)
        }
        val type = submission.contentType!!
        addClickFunctions(holder.leadImage, type, mContext, submission, holder, full)
        if (thumbImage2 != null) {
            addClickFunctions(thumbImage2, type, mContext, submission, holder, full)
        }
        if (full) {
            addClickFunctions(
                holder.itemView.findViewById(R.id.wraparea), type, mContext,
                submission, holder, full
            )
        }
        if (full) {
            holder.leadImage.wrapArea = holder.itemView.findViewById(R.id.wraparea)
        }
        /*
        if (full && (((submission.dataNode != null) && submission.dataNode.has("crosspost_parent_list")
                    && (submission.dataNode["crosspost_parent_list"] != null) && (submission.dataNode["crosspost_parent_list"][0] != null)))
        ) {
            holder.itemView.findViewById<View>(R.id.crosspost).visibility = View.VISIBLE
            (holder.itemView.findViewById<View>(R.id.crossinfo) as TextView).text =
                SubmissionCache.getCrosspostLine(submission, mContext)
            (mContext.applicationContext as App).imageLoader!!
                .displayImage(
                    submission.dataNode["crosspost_parent_list"][0]["thumbnail"].asText(),
                    (holder.itemView.findViewById<View>(R.id.crossthumb) as ImageView)
                )
            holder.itemView.findViewById<View>(R.id.crosspost)
                .setOnClickListener {
                    OpenRedditLink.openUrl(
                        mContext,
                        submission.dataNode["crosspost_parent_list"][0]["permalink"].asText(),
                        true
                    )
                }
        }
         */
        holder.leadImage.setSubmission(submission, full, baseSub, type)
        holder.itemView.setOnLongClickListener {
            if (offline) {
                val s = Snackbar.make(
                    holder.itemView, mContext.getString(R.string.offline_msg),
                    Snackbar.LENGTH_SHORT
                )
                LayoutUtils.showSnackbar(s)
            } else {
                if (SettingValues.actionbarTap && !full) {
                    CreateCardView.toggleActionbar(holder.itemView)
                } else {
                    holder.itemView.findViewById<View>(R.id.menu).callOnClick()
                }
            }
            true
        }
        doText(holder, submission, mContext, baseSub, full)
        if ((!full
                    && isSelftextEnabled(baseSub)
                    && submission.url == null
                    && submission.body.orEmpty().isNotEmpty()
                    && !submission.isNsfw
                    && !submission.isSpoiler)
        ) {
            holder.body.visibility = View.VISIBLE
            val text = submission.body!!
            val typef = FontPreferences(mContext).fontTypeComment.typeface
            val typeface: Typeface = if (typef >= 0) {
                RobotoTypefaces.obtainTypeface(mContext, typef)
            } else {
                Typeface.DEFAULT
            }
            holder.body.setTypeface(typeface)
            holder.body.setTextHtml(
                CompatUtil.fromHtml(
                    text.substring(0, if (text.contains("\n")) text.indexOf("\n") else text.length)
                )
                    .toString()
                    .replace("<sup>", "<sup><small>")
                    .replace("</sup>", "</small></sup>"), "none "
            )
            holder.body.setOnClickListener { holder.itemView.callOnClick() }
            holder.body.setOnLongClickListener {
                holder.menu.callOnClick()
                true
            }
        } else if (!full) {
            holder.body.visibility = View.GONE
        }
        if (full) {
            if (submission.body?.isNotEmpty() == true) {
                val typef = FontPreferences(mContext).fontTypeComment.typeface
                val typeface = if (typef >= 0) {
                    RobotoTypefaces.obtainTypeface(mContext, typef)
                } else {
                    Typeface.DEFAULT
                }
                holder.firstTextView.setTypeface(typeface)
                setViews(
                    submission.body!!,
                    if (submission.groupName == null) "all" else submission.groupName,
                    holder
                )
                holder.itemView.findViewById<View>(R.id.body_area).visibility = View.VISIBLE
            } else {
                holder.itemView.findViewById<View>(R.id.body_area).visibility = View.GONE
            }
        }
        try {
            val points = holder.score
            val comments = holder.comments
            if (Authentication.isLoggedIn && !offline && Authentication.didOnline) {
                run {
                    downvotebutton.setOnClickListener {
                        if (SettingValues.storeHistory && !full) {
                            if (!submission.isNsfw || SettingValues.storeNSFWHistory) {
                                HasSeen.addSeen(submission.permalink)
                                if (mContext is MainActivity) {
                                    holder.title.setAlpha(0.54f)
                                    holder.body.setAlpha(0.54f)
                                }
                            }
                        }
                        val getTintColor: Int =
                            if ((holder.itemView.getTag(holder.itemView.getId()) != null
                                        && (holder.itemView.getTag(holder.itemView.getId()) == "none")
                                        || full)
                            ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                        if ((ActionStates.getVoteDirection(submission)
                                    != VoteDirection.DOWNVOTE)
                        ) { //has not been downvoted
                            points.setTextColor(
                                ContextCompat.getColor(mContext, R.color.md_blue_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                downvotebutton,
                                ContextCompat.getColor(mContext, R.color.md_blue_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, getTintColor)
                            downvotebutton.setContentDescription(mContext.getString(R.string.btn_downvoted))
                            AnimatorUtil.setFlashAnimation(
                                holder.itemView, downvotebutton,
                                ContextCompat.getColor(mContext, R.color.md_blue_500)
                            )
                            holder.score.setTypeface(null, Typeface.BOLD)
                            val DOWNVOTE_SCORE: Int =
                                if ((SUBMISSION_SCORE == 0)) 0 else (SUBMISSION_SCORE
                                        - 1) //if a post is at 0 votes, keep it at 0 when downvoting
                            //Vote(false, points, mContext).execute(submission)
                            ActionStates.setVoteDirection(submission, VoteDirection.DOWNVOTE)
                        } else { //un-downvoted a post
                            points.setTextColor(comments.getCurrentTextColor())
                            //Vote(points, mContext).execute(submission)
                            holder.score.setTypeface(null, Typeface.NORMAL)
                            ActionStates.setVoteDirection(submission, VoteDirection.NO_VOTE)
                            BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, getTintColor)
                            downvotebutton.setContentDescription(mContext.getString(R.string.btn_downvote))
                        }
                        setSubmissionScoreText(submission, holder)
                        if ((!full
                                    && !SettingValues.actionbarVisible
                                    && ((SettingValues.defaultCardView
                                    != CreateCardView.CardEnum.DESKTOP)))
                        ) {
                            CreateCardView.toggleActionbar(holder.itemView)
                        }
                    }
                }
                run {
                    upvotebutton.setOnClickListener {
                        if (SettingValues.storeHistory && !full) {
                            if (!submission.isNsfw || SettingValues.storeNSFWHistory) {
                                HasSeen.addSeen(submission.permalink)
                                if (mContext is MainActivity) {
                                    holder.title.setAlpha(0.54f)
                                    holder.body.setAlpha(0.54f)
                                }
                            }
                        }
                        val getTintColor: Int =
                            if ((holder.itemView.getTag(holder.itemView.getId()) != null
                                        && (holder.itemView.getTag(holder.itemView.getId()) == "none")
                                        || full)
                            ) Palette.getCurrentTintColor(mContext) else Palette.getWhiteTintColor()
                        if ((ActionStates.getVoteDirection(submission)
                                    != VoteDirection.UPVOTE)
                        ) { //has not been upvoted
                            points.setTextColor(
                                ContextCompat.getColor(mContext, R.color.md_orange_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                upvotebutton,
                                ContextCompat.getColor(mContext, R.color.md_orange_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, getTintColor)
                            upvotebutton.setContentDescription(mContext.getString(R.string.btn_upvoted))
                            AnimatorUtil.setFlashAnimation(
                                holder.itemView, upvotebutton,
                                ContextCompat.getColor(mContext, R.color.md_orange_500)
                            )
                            holder.score.setTypeface(null, Typeface.BOLD)
                            //Vote(true, points, mContext).execute(submission)
                            ActionStates.setVoteDirection(submission, VoteDirection.UPVOTE)
                        } else { //un-upvoted a post
                            points.setTextColor(comments.getCurrentTextColor())
                            //Vote(points, mContext).execute(submission)
                            holder.score.setTypeface(null, Typeface.NORMAL)
                            ActionStates.setVoteDirection(submission, VoteDirection.NO_VOTE)
                            BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, getTintColor)
                            upvotebutton.setContentDescription(mContext.getString(R.string.btn_upvote))
                        }
                        setSubmissionScoreText(submission, holder)
                        if ((!full
                                    && !SettingValues.actionbarVisible
                                    && ((SettingValues.defaultCardView
                                    != CreateCardView.CardEnum.DESKTOP)))
                        ) {
                            CreateCardView.toggleActionbar(holder.itemView)
                        }
                    }
                }
            } else {
                upvotebutton.visibility = View.GONE
                downvotebutton.visibility = View.GONE
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        val edit = holder.edit
        if (((Authentication.name != null
                    ) && (Authentication.name!!.lowercase()
                    == submission.creator.name.lowercase()) && Authentication.didOnline)
        ) {
            edit.visibility = View.VISIBLE
            edit.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    object : AsyncTask<Void?, Void?, ArrayList<String?>?>() {
                        var flairlist: MutableList<FlairTemplate>? = null

                        override fun doInBackground(vararg params: Void?): ArrayList<String?>? {
                            val allFlairs = FluentRedditClient(Authentication.reddit).subreddit(
                                submission.groupName
                            ).flair()
                            try {
                                //flairlist = allFlairs.options(submission)
                                val finalFlairs = ArrayList<String?>()
                                for (temp: FlairTemplate in flairlist!!) {
                                    finalFlairs.add(temp.text)
                                }
                                return finalFlairs
                            } catch (e: Exception) {
                                e.printStackTrace()
                                //sub probably has no flairs?
                            }
                            return null
                        }

                        public override fun onPostExecute(data: ArrayList<String?>?) {
                            val flair = (data != null && !data.isEmpty())
                            val attrs = intArrayOf(R.attr.tintColor)
                            val ta = mContext.obtainStyledAttributes(attrs)
                            val color2 = ta.getColor(0, Color.WHITE)
                            val edit_drawable = mContext.resources.getDrawable(R.drawable.ic_edit)
                            val nsfw_drawable =
                                mContext.resources.getDrawable(R.drawable.ic_visibility_off)
                            val delete_drawable =
                                mContext.resources.getDrawable(R.drawable.ic_delete)
                            val flair_drawable =
                                mContext.resources.getDrawable(R.drawable.ic_text_fields)
                            val drawableSet = Arrays.asList(
                                edit_drawable, nsfw_drawable, delete_drawable, flair_drawable
                            )
                            BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color2)
                            ta.recycle()
                            val b = BottomSheet.Builder(mContext).title(
                                CompatUtil.fromHtml(submission.title)
                            )
                            if (submission.url == null) {
                                b.sheet(
                                    1, edit_drawable,
                                    mContext.getString(R.string.edit_selftext)
                                )
                            }
                            if (submission.isNsfw) {
                                b.sheet(
                                    4, nsfw_drawable,
                                    mContext.getString(R.string.mod_btn_unmark_nsfw)
                                )
                            } else {
                                b.sheet(
                                    4, nsfw_drawable,
                                    mContext.getString(R.string.mod_btn_mark_nsfw)
                                )
                            }
                            if (submission.isSpoiler) {
                                b.sheet(
                                    5,
                                    nsfw_drawable,
                                    mContext.getString(R.string.mod_btn_unmark_spoiler)
                                )
                            } else {
                                b.sheet(
                                    5,
                                    nsfw_drawable,
                                    mContext.getString(R.string.mod_btn_mark_spoiler)
                                )
                            }
                            b.sheet(
                                2, delete_drawable,
                                mContext.getString(R.string.delete_submission)
                            )
                            if (flair) {
                                b.sheet(
                                    3, flair_drawable,
                                    mContext.getString(R.string.set_submission_flair)
                                )
                            }
                            b.listener(object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface, which: Int) {
                                    when (which) {
                                        1 -> {
                                            val inflater = mContext.layoutInflater
                                            val dialoglayout =
                                                inflater.inflate(R.layout.edit_comment, null)
                                            val e = dialoglayout.findViewById<EditText>(
                                                R.id.entry
                                            )
                                            e.setText(
                                                StringEscapeUtils.unescapeHtml4(
                                                    submission.body
                                                )
                                            )
                                            DoEditorActions.doActions(
                                                e, dialoglayout,
                                                (mContext as AppCompatActivity).supportFragmentManager,
                                                mContext, null, null
                                            )
                                            val builder = AlertDialog.Builder(mContext)
                                                .setCancelable(false)
                                                .setView(dialoglayout)
                                            val d: Dialog = builder.create()
                                            d.window!!
                                                .setSoftInputMode(
                                                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                                )
                                            d.show()
                                            dialoglayout.findViewById<View>(R.id.cancel)
                                                .setOnClickListener { d.dismiss() }
                                            dialoglayout.findViewById<View>(R.id.submit)
                                                .setOnClickListener(object : View.OnClickListener {
                                                    override fun onClick(v: View) {
                                                        val text = e.text.toString()
                                                        object : AsyncTask<Void?, Void?, Void?>() {
                                                            override fun doInBackground(
                                                                vararg params: Void?
                                                            ): Void? {
                                                                try {
                                                                    //AccountManager(Authentication.reddit).updateContribution(submission, text)
                                                                    adapter?.dataSet?.reloadSubmission(
                                                                        adapter
                                                                    )
                                                                    d.dismiss()
                                                                } catch (e: Exception) {
                                                                    (mContext).runOnUiThread {
                                                                        AlertDialog.Builder(
                                                                            mContext
                                                                        )
                                                                            .setTitle(R.string.comment_delete_err)
                                                                            .setMessage(R.string.comment_delete_err_msg)
                                                                            .setPositiveButton(
                                                                                R.string.btn_yes
                                                                            ) { dialog1: DialogInterface, which1: Int ->
                                                                                dialog1.dismiss()
                                                                                doInBackground()
                                                                            }
                                                                            .setNegativeButton(
                                                                                R.string.btn_no
                                                                            ) { dialog12: DialogInterface, which12: Int -> dialog12.dismiss() }
                                                                            .show()
                                                                    }
                                                                }
                                                                return null
                                                            }

                                                            override fun onPostExecute(
                                                                aVoid: Void?
                                                            ) {
                                                                adapter?.notifyItemChanged(
                                                                    1
                                                                )
                                                            }
                                                        }.executeOnExecutor(
                                                            THREAD_POOL_EXECUTOR
                                                        )
                                                    }
                                                })
                                        }

                                        2 -> {
                                            AlertDialog.Builder(mContext)
                                                .setTitle(R.string.really_delete_submission)
                                                .setPositiveButton(
                                                    R.string.btn_yes
                                                ) { dialog13: DialogInterface?, which13: Int ->
                                                    object : AsyncTask<Void?, Void?, Void>() {
                                                        override fun doInBackground(
                                                            vararg params: Void?
                                                        ): Void? {
                                                            try {
                                                                //ModerationManager(Authentication.reddit).delete(submission)
                                                            } catch (e: ApiException) {
                                                                e.printStackTrace()
                                                            }
                                                            return null
                                                        }

                                                        override fun onPostExecute(
                                                            aVoid: Void
                                                        ) {
                                                            (mContext).runOnUiThread {
                                                                (holder.title)
                                                                    .setTextHtml(
                                                                        mContext.getString(
                                                                            R.string.content_deleted
                                                                        )
                                                                    )
                                                                if ((holder.firstTextView
                                                                            != null)
                                                                ) {
                                                                    holder.firstTextView
                                                                        .setText(
                                                                            R.string.content_deleted
                                                                        )
                                                                    holder.commentOverflow
                                                                        .setVisibility(
                                                                            View.GONE
                                                                        )
                                                                } else {
                                                                    if ((holder.itemView
                                                                            .findViewById<View?>(
                                                                                R.id.body
                                                                            )
                                                                                != null)
                                                                    ) {
                                                                        (holder.itemView
                                                                            .findViewById<View>(
                                                                                R.id.body
                                                                            ) as TextView)
                                                                            .setText(
                                                                                R.string.content_deleted
                                                                            )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }.executeOnExecutor(
                                                        THREAD_POOL_EXECUTOR
                                                    )
                                                }
                                                .setNegativeButton(R.string.btn_cancel, null)
                                                .show()
                                        }

                                        3 -> {
                                            MaterialDialog.Builder(mContext).items((data)!!)
                                                .title(R.string.sidebar_select_flair)
                                                .itemsCallback(
                                                    object : ListCallback {
                                                        override fun onSelection(
                                                            dialog: MaterialDialog,
                                                            itemView: View, which: Int,
                                                            text: CharSequence
                                                        ) {
                                                            val t = flairlist!![which]
                                                            if (t.isTextEditable) {
                                                                MaterialDialog.Builder(
                                                                    mContext
                                                                ).title(
                                                                    R.string.mod_btn_submission_flair_text
                                                                )
                                                                    .input(mContext.getString(
                                                                        R.string.mod_flair_hint
                                                                    ),
                                                                        t.text,
                                                                        true
                                                                    ) { dialog14: MaterialDialog?, input: CharSequence? -> }
                                                                    .positiveText(
                                                                        R.string.btn_set
                                                                    )
                                                                    .onPositive(
                                                                        object :
                                                                            SingleButtonCallback {
                                                                            override fun onClick(
                                                                                dialog: MaterialDialog,
                                                                                which: DialogAction
                                                                            ) {
                                                                                val flair =
                                                                                    dialog.inputEditText!!
                                                                                        .text
                                                                                        .toString()
                                                                                object :
                                                                                    AsyncTask<Void?, Void?, Boolean>() {
                                                                                    override fun doInBackground(
                                                                                        vararg params: Void?
                                                                                    ): Boolean {
                                                                                        try {
                                                                                            //ModerationManager(Authentication.reddit)
                                                                                            //    .setFlair(submission.groupName, t, flair, submission)
                                                                                            return true
                                                                                        } catch (e: ApiException) {
                                                                                            e.printStackTrace()
                                                                                            return false
                                                                                        }
                                                                                    }

                                                                                    override fun onPostExecute(
                                                                                        done: Boolean
                                                                                    ) {
                                                                                        var s: Snackbar? =
                                                                                            null
                                                                                        if (done) {
                                                                                            if ((holder.itemView
                                                                                                        != null)
                                                                                            ) {
                                                                                                s =
                                                                                                    Snackbar.make(
                                                                                                        holder.itemView,
                                                                                                        R.string.snackbar_flair_success,
                                                                                                        Snackbar.LENGTH_SHORT
                                                                                                    )
                                                                                                SubmissionCache
                                                                                                    .updateTitleFlair(
                                                                                                        submission,
                                                                                                        flair,
                                                                                                        mContext
                                                                                                    )
                                                                                                holder.title.text =
                                                                                                    SubmissionCache
                                                                                                        .getTitleLine(
                                                                                                            submission,
                                                                                                            mContext
                                                                                                        )
                                                                                            }
                                                                                        } else {
                                                                                            if ((holder.itemView
                                                                                                        != null)
                                                                                            ) {
                                                                                                s =
                                                                                                    Snackbar.make(
                                                                                                        holder.itemView,
                                                                                                        R.string.snackbar_flair_error,
                                                                                                        Snackbar.LENGTH_SHORT
                                                                                                    )
                                                                                            }
                                                                                        }
                                                                                        if (s != null) {
                                                                                            LayoutUtils.showSnackbar(
                                                                                                s
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }.executeOnExecutor(
                                                                                    THREAD_POOL_EXECUTOR
                                                                                )
                                                                            }
                                                                        })
                                                                    .negativeText(
                                                                        R.string.btn_cancel
                                                                    )
                                                                    .show()
                                                            } else {
                                                                object :
                                                                    AsyncTask<Void?, Void?, Boolean>() {
                                                                    override fun doInBackground(
                                                                        vararg params: Void?
                                                                    ): Boolean {
                                                                        try {
                                                                            //ModerationManager(Authentication.reddit)
                                                                            //    .setFlair(submission.groupName, t, null, submission)
                                                                            return true
                                                                        } catch (e: ApiException) {
                                                                            e.printStackTrace()
                                                                            return false
                                                                        }
                                                                    }

                                                                    override fun onPostExecute(
                                                                        done: Boolean
                                                                    ) {
                                                                        var s: Snackbar? = null
                                                                        if (done) {
                                                                            if ((holder.itemView
                                                                                        != null)
                                                                            ) {
                                                                                s = Snackbar.make(
                                                                                    holder.itemView,
                                                                                    R.string.snackbar_flair_success,
                                                                                    Snackbar.LENGTH_SHORT
                                                                                )
                                                                                SubmissionCache
                                                                                    .updateTitleFlair(
                                                                                        submission,
                                                                                        t.cssClass,
                                                                                        mContext
                                                                                    )
                                                                                holder.title.text =
                                                                                    SubmissionCache
                                                                                        .getTitleLine(
                                                                                            submission,
                                                                                            mContext
                                                                                        )
                                                                            }
                                                                        } else {
                                                                            if ((holder.itemView
                                                                                        != null)
                                                                            ) {
                                                                                s = Snackbar.make(
                                                                                    holder.itemView,
                                                                                    R.string.snackbar_flair_error,
                                                                                    Snackbar.LENGTH_SHORT
                                                                                )
                                                                            }
                                                                        }
                                                                        if (s != null) {
                                                                            LayoutUtils.showSnackbar(
                                                                                s
                                                                            )
                                                                        }
                                                                    }
                                                                }.executeOnExecutor(
                                                                    THREAD_POOL_EXECUTOR
                                                                )
                                                            }
                                                        }
                                                    })
                                                .show()
                                        }

                                        4 -> if (submission.isNsfw) {
                                            unNsfwSubmission(mContext, submission, holder)
                                        } else {
                                            setPostNsfw(mContext, submission, holder)
                                        }

                                        5 -> if (submission.isSpoiler) {
                                            unSpoiler(mContext, submission, holder)
                                        } else {
                                            setSpoiler(mContext, submission, holder)
                                        }
                                    }
                                }
                            }).show()
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            })
        } else {
            edit.visibility = View.GONE
        }
        if (HasSeen.getSeen(submission) && !full) {
            holder.title.alpha = 0.54f
            holder.body.alpha = 0.54f
        } else {
            holder.title.alpha = 1f
            if (!full) {
                holder.body.alpha = 1f
            }
        }
    }

    private fun setSubmissionScoreText(submission: IPost, holder: SubmissionViewHolder) {
        var submissionScore = submission.score
        when (ActionStates.getVoteDirection(submission)) {
            VoteDirection.UPVOTE -> {
                if (submission.myVote != VoteDirection.UPVOTE) {
                    if (submission.myVote == VoteDirection.DOWNVOTE) ++submissionScore
                    ++submissionScore //offset the score by +1
                }
            }

            VoteDirection.DOWNVOTE -> {
                if (submission.myVote != VoteDirection.DOWNVOTE) {
                    if (submission.myVote == VoteDirection.UPVOTE) --submissionScore
                    --submissionScore //offset the score by +1
                }
            }

            VoteDirection.NO_VOTE -> if (submission.myVote == VoteDirection.UPVOTE && submission.creator.name
                    .equals(Authentication.name, ignoreCase = true)
            ) {
                submissionScore--
            }
        }


        //if the submission is already at 0pts, keep it at 0pts
        submissionScore = Math.max(submissionScore, 0)
        if (submissionScore >= 10000 && SettingValues.abbreviateScores) {
            holder.score.text = String.format(
                Locale.getDefault(), "%.1fk",
                ((submissionScore.toDouble()) / 1000)
            )
        } else {
            holder.score.text = String.format(Locale.getDefault(), "%d", submissionScore)
        }
    }

    private fun setViews(rawHTML: String, subredditName: String, holder: SubmissionViewHolder) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        if (!blocks[0].startsWith("<table>") && !blocks[0].startsWith("<pre>")) {
            holder.firstTextView.setTextHtml(blocks[0], subredditName)
            startIndex = 1
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                holder.commentOverflow.setViews(blocks, subredditName)
            } else {
                holder.commentOverflow.setViews(
                    blocks.subList(startIndex, blocks.size),
                    subredditName
                )
            }
        }
    }

    class AsyncReportTask(private val submission: IPost, private val contextView: View?) :
        AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg reason: String?): Void? {
            try {
                //AccountManager(Authentication.reddit).report(submission, reason[0])
            } catch (e: ApiException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            if (contextView != null) {
                try {
                    val s =
                        Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT)
                    LayoutUtils.showSnackbar(s)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    companion object {
        private fun addClickFunctions(
            base: View, type: ContentType.Type,
            contextActivity: Activity, submission: IPost,
            holder: SubmissionViewHolder?, full: Boolean
        ) {
            base.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (NetworkUtil.isConnected(contextActivity) || (!NetworkUtil.isConnected(
                            contextActivity
                        ) && ContentType.fullImage(type))
                    ) {
                        if (SettingValues.storeHistory && !full) {
                            if (!submission.isNsfw || SettingValues.storeNSFWHistory) {
                                HasSeen.addSeen(submission.permalink)
                                if ((contextActivity is MainActivity
                                            || contextActivity is MultiredditOverview
                                            || contextActivity is SubredditView
                                            || contextActivity is Search
                                            || contextActivity is Profile)
                                ) {
                                    holder!!.title.alpha = 0.54f
                                    holder.body.alpha = 0.54f
                                }
                            }
                        }
                        if ((contextActivity !is PeekViewActivity
                                    || !contextActivity.isPeeking()
                                    || ((base is HeaderImageLinkView
                                    && base.popped)))
                        ) {
                            if ((!PostMatch.openExternal(submission.url!!)
                                        || type == ContentType.Type.VIDEO)
                            ) {
                                when (type) {
                                    ContentType.Type.STREAMABLE -> if (SettingValues.video) {
                                        val myIntent =
                                            Intent(contextActivity, MediaView::class.java)
                                        myIntent.putExtra(
                                            MediaView.SUBREDDIT,
                                            submission.groupName
                                        )
                                        myIntent.putExtra(MediaView.EXTRA_URL, submission.url)
                                        myIntent.putExtra(
                                            ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                            submission.title
                                        )
                                        PopulateBase.addAdaptorPosition(
                                            myIntent, submission,
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(myIntent)
                                    } else {
                                        LinkUtil.openExternally(submission.url!!)
                                    }

                                    ContentType.Type.IMGUR, ContentType.Type.DEVIANTART, ContentType.Type.XKCD, ContentType.Type.IMAGE -> openImage(
                                        type, contextActivity, submission, holder!!.leadImage,
                                        holder.bindingAdapterPosition
                                    )

                                    ContentType.Type.EMBEDDED -> if (SettingValues.video) {
                                        val data = CompatUtil.fromHtml(
                                            submission.url!!
                                        ).toString()
                                        run {
                                            val i: Intent = Intent(
                                                contextActivity,
                                                FullscreenVideo::class.java
                                            )
                                            i.putExtra(FullscreenVideo.EXTRA_HTML, data)
                                            contextActivity.startActivity(i)
                                        }
                                    } else {
                                        LinkUtil.openExternally(submission.url!!)
                                    }

                                    ContentType.Type.REDDIT -> openRedditContent(
                                        submission.url,
                                        contextActivity
                                    )

                                    ContentType.Type.REDDIT_GALLERY -> if (SettingValues.album) {
                                        val i: Intent
                                        if (SettingValues.albumSwipe) {
                                            i = Intent(
                                                contextActivity,
                                                RedditGalleryPager::class.java
                                            )
                                            i.putExtra(
                                                AlbumPager.SUBREDDIT,
                                                submission.groupName
                                            )
                                        } else {
                                            i = Intent(contextActivity, RedditGallery::class.java)
                                            i.putExtra(
                                                Album.SUBREDDIT,
                                                submission.groupName
                                            )
                                        }
                                        i.putExtra(
                                            ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                            submission.title
                                        )
                                        i.putExtra(
                                            RedditGallery.SUBREDDIT,
                                            submission.groupName
                                        )
                                        val urls = ArrayList<GalleryImage>()
                                        /*
                                        val dataNode = submission.dataNode
                                        if (dataNode.has("gallery_data")) {
                                            JsonUtil.getGalleryData(dataNode, urls)
                                        } else if (dataNode.has("crosspost_parent_list")) { //Else, try getting crosspost gallery data
                                            val crosspost_parent =
                                                dataNode["crosspost_parent_list"][0]
                                            if (crosspost_parent.has("gallery_data")) {
                                                JsonUtil.getGalleryData(crosspost_parent, urls)
                                            }
                                        }
                                         */
                                        val urlsBundle = Bundle()
                                        urlsBundle.putSerializable(RedditGallery.GALLERY_URLS, urls)
                                        i.putExtras(urlsBundle)
                                        PopulateBase.addAdaptorPosition(
                                            i, submission,
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(i)
                                        contextActivity.overridePendingTransition(
                                            R.anim.slideright,
                                            R.anim.fade_out
                                        )
                                    } else {
                                        LinkUtil.openExternally(submission.url!!)
                                    }

                                    ContentType.Type.LINK -> LinkUtil.openUrl(
                                        submission.url!!,
                                        Palette.getColor(submission.groupName),
                                        contextActivity, holder!!.bindingAdapterPosition,
                                        submission
                                    )

                                    ContentType.Type.SELF -> if (holder != null) {
                                        override = true
                                        holder.itemView.performClick()
                                    }

                                    ContentType.Type.ALBUM -> if (SettingValues.album) {
                                        val i: Intent
                                        if (SettingValues.albumSwipe) {
                                            i = Intent(contextActivity, AlbumPager::class.java)
                                            i.putExtra(
                                                AlbumPager.SUBREDDIT,
                                                submission.groupName
                                            )
                                        } else {
                                            i = Intent(contextActivity, Album::class.java)
                                            i.putExtra(
                                                Album.SUBREDDIT,
                                                submission.groupName
                                            )
                                        }
                                        i.putExtra(
                                            ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                            submission.title
                                        )
                                        i.putExtra(Album.EXTRA_URL, submission.url)
                                        PopulateBase.addAdaptorPosition(
                                            i, submission,
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(i)
                                        contextActivity.overridePendingTransition(
                                            R.anim.slideright,
                                            R.anim.fade_out
                                        )
                                    } else {
                                        LinkUtil.openExternally(submission.url!!)
                                    }

                                    ContentType.Type.TUMBLR -> if (SettingValues.album) {
                                        val i: Intent
                                        if (SettingValues.albumSwipe) {
                                            i = Intent(contextActivity, TumblrPager::class.java)
                                            i.putExtra(
                                                TumblrPager.SUBREDDIT,
                                                submission.groupName
                                            )
                                        } else {
                                            i = Intent(contextActivity, Tumblr::class.java)
                                            i.putExtra(
                                                Tumblr.SUBREDDIT,
                                                submission.groupName
                                            )
                                        }
                                        i.putExtra(Album.EXTRA_URL, submission.url)
                                        PopulateBase.addAdaptorPosition(
                                            i, submission,
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(i)
                                        contextActivity.overridePendingTransition(
                                            R.anim.slideright,
                                            R.anim.fade_out
                                        )
                                    } else {
                                        LinkUtil.openExternally(submission.url!!)
                                    }

                                    ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.GIF, ContentType.Type.VREDDIT_DIRECT -> openGif(
                                        contextActivity, submission,
                                        holder!!.bindingAdapterPosition
                                    )

                                    ContentType.Type.NONE -> holder?.itemView?.performClick()
                                    ContentType.Type.VIDEO -> if (!LinkUtil.tryOpenWithVideoPlugin(
                                            submission.url!!
                                        )
                                    ) {
                                        LinkUtil.openUrl(
                                            submission.url!!,
                                            Palette.getStatusBarColor(), contextActivity
                                        )
                                    }

                                    else -> {}
                                }
                            } else {
                                LinkUtil.openExternally(submission.url!!)
                            }
                        }
                    } else {
                        if ((contextActivity !is PeekViewActivity
                                    || !contextActivity.isPeeking())
                        ) {
                            val s = Snackbar.make(
                                holder!!.itemView, R.string.go_online_view_content,
                                Snackbar.LENGTH_SHORT
                            )
                            LayoutUtils.showSnackbar(s)
                        }
                    }
                }
            })
        }

        @JvmStatic
        fun openRedditContent(url: String?, c: Context?) {
            OpenRedditLink.openUrl(c, url, true)
        }

        @JvmStatic
        fun openImage(
            type: ContentType.Type, contextActivity: Activity,
            submission: IPost, baseView: HeaderImageLinkView?, adapterPosition: Int
        ) {
            if (SettingValues.image) {
                val myIntent = Intent(contextActivity, MediaView::class.java)
                myIntent.putExtra(MediaView.SUBREDDIT, submission.groupName)
                myIntent.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    submission.title
                )
                val previewUrl: String
                val url = submission.url
                if (((baseView != null
                            ) && baseView.lq
                            && SettingValues.loadImageLq
                            && (type != ContentType.Type.XKCD))
                ) {
                    myIntent.putExtra(MediaView.EXTRA_LQ, true)
                    myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, baseView.loadedUrl)
                } else if ((submission.hasPreview && ((type != ContentType.Type.XKCD)))
                ) { //Load the preview image which has probably already been cached in memory instead of the direct link
                    previewUrl = submission.preview!!
                    if (baseView == null || (!SettingValues.loadImageLq && baseView.lq)) {
                        myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl)
                    } else {
                        myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, baseView.loadedUrl)
                    }
                }
                myIntent.putExtra(MediaView.EXTRA_URL, url)
                PopulateBase.addAdaptorPosition(myIntent, submission, adapterPosition)
                myIntent.putExtra(MediaView.EXTRA_SHARE_URL, submission.url)
                contextActivity.startActivity(myIntent)
            } else {
                LinkUtil.openExternally(submission.url!!)
            }
        }

        @JvmStatic
        fun openGif(
            contextActivity: Activity, submission: IPost,
            adapterPosition: Int
        ) {
            if (SettingValues.gif) {
                DataShare.sharedSubmission = submission
                val myIntent = Intent(contextActivity, MediaView::class.java)
                myIntent.putExtra(MediaView.SUBREDDIT, submission.groupName)
                myIntent.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    submission.title
                )
                val t = AsyncLoadGif.getVideoType(submission.url)
                /*
                if ((t.shouldLoadPreview()
                            && submission.dataNode.has("preview")
                            && submission.dataNode["preview"]["images"][0].has("variants")
                            && submission.dataNode["preview"]["images"][0]["variants"].has("mp4"))
                ) {
                    myIntent.putExtra(
                        MediaView.EXTRA_URL, StringEscapeUtils.unescapeJson(
                            submission.dataNode["preview"]["images"][0]["variants"]["mp4"]["source"]["url"]
                                .asText()
                        ).replace("&amp;", "&")
                    )
                } else if ((t.shouldLoadPreview()
                            && submission.dataNode.has("preview")
                            && submission.dataNode["preview"]["reddit_video_preview"].has("fallback_url"))
                ) {
                    myIntent.putExtra(
                        MediaView.EXTRA_URL, StringEscapeUtils.unescapeJson(
                            submission.dataNode["preview"]["reddit_video_preview"]["fallback_url"]
                                .asText()
                        ).replace("&amp;", "&")
                    )
                } else if (((t == AsyncLoadGif.VideoType.DIRECT
                            ) && submission.dataNode
                        .has("media")
                            && submission.dataNode["media"].has("reddit_video")
                            && submission.dataNode["media"]["reddit_video"]
                        .has("fallback_url"))
                ) {
                    myIntent.putExtra(
                        MediaView.EXTRA_URL, StringEscapeUtils.unescapeJson(
                            submission.dataNode["media"]["reddit_video"]["fallback_url"]
                                .asText()
                        ).replace("&amp;", "&")
                    )
                } else if (t != AsyncLoadGif.VideoType.OTHER) {
                    myIntent.putExtra(MediaView.EXTRA_URL, submission.url)
                } else {
                    LinkUtil.openUrl(
                        submission.url!!,
                        Palette.getColor(submission.groupName), contextActivity,
                        adapterPosition, submission
                    )
                    return
                }
                 */
                if (submission.hasPreview) { //Load the preview image which has probably already been cached in memory instead of the direct link
                    val previewUrl = submission.preview
                    myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl)
                }
                PopulateBase.addAdaptorPosition(myIntent, submission, adapterPosition)
                contextActivity.startActivity(myIntent)
            } else {
                LinkUtil.openExternally(submission.url!!)
            }
        }
    }
}
