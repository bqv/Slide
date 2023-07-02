package me.ccrama.redditslide.SubmissionViews

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.cocosw.bottomsheet.BottomSheet
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.ActionStates
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Activities.AlbumPager
import me.ccrama.redditslide.Activities.FullscreenVideo
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.Activities.PostReadLater
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.Search
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Tumblr
import me.ccrama.redditslide.Activities.TumblrPager
import me.ccrama.redditslide.Adapters.CommentAdapter
import me.ccrama.redditslide.Adapters.NewsViewHolder
import me.ccrama.redditslide.CommentCacheAsync
import ltd.ucode.slide.ContentType
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
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
import me.ccrama.redditslide.views.CreateCardView
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.ClipboardUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.Ruleset
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditRule
import org.apache.commons.text.StringEscapeUtils
import java.util.Arrays

class PopulateNewsViewHolder(private val postRepository: PostRepository,
                             private val commentRepository: CommentRepository
) {
    var reason: String? = null
    var chosen = booleanArrayOf(false, false, false)
    var oldChosen = booleanArrayOf(false, false, false)

    fun <T : Contribution?> showBottomSheet(
        mContext: Activity,
        submission: Submission, holder: NewsViewHolder, posts: MutableList<T>,
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
        val drawableSet = Arrays.asList(
            profile, sub, saved, hide, report, copy,
            open, link, reddit, readLater, filter
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(mContext).title(CompatUtil.fromHtml(submission.title))
        val isReadLater = mContext is PostReadLater
        val isAddedToReadLaterList = ReadLater.isToBeReadLater(RedditSubmission(submission))
        if (Authentication.didOnline) {
            b.sheet(1, (profile)!!, "/u/" + submission.author)
                .sheet(2, (sub)!!, "/c/" + submission.subredditName)
            var save: String = mContext.getString(R.string.btn_save)
            if (ActionStates.isSaved(RedditSubmission(submission))) {
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
            }
        }
        if ((submission.selftext != null) && !submission.selftext.isEmpty() && full) {
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
                        i.putExtra(Profile.EXTRA_PROFILE, submission.author)
                        mContext.startActivity(i)
                    }

                    2 -> {
                        val i = Intent(mContext, SubredditView::class.java)
                        i.putExtra(SubredditView.EXTRA_SUBREDDIT, submission.subredditName)
                        mContext.startActivityForResult(i, 14)
                    }

                    10 -> {
                        val choices: Array<String>
                        val flair =
                            if (submission.submissionFlair.text != null) submission.submissionFlair.text else ""
                        if (flair.isEmpty()) {
                            choices = arrayOf(
                                mContext.getString(
                                    R.string.filter_posts_sub,
                                    submission.subredditName
                                ),
                                mContext.getString(
                                    R.string.filter_posts_user,
                                    submission.author
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
                                SettingValues.subredditFilters!!.contains(
                                    submission.subredditName.lowercase()
                                ),
                                SettingValues.userFilters!!.contains(
                                    submission.author.lowercase()
                                ),
                                SettingValues.domainFilters!!.contains(
                                    submission.domain.lowercase()
                                ),
                                SettingValues.alwaysExternal!!.contains(
                                    submission.domain.lowercase()
                                )
                            )
                            oldChosen = chosen.clone()
                        } else {
                            choices = arrayOf(
                                mContext.getString(
                                    R.string.filter_posts_sub,
                                    submission.subredditName
                                ),
                                mContext.getString(
                                    R.string.filter_posts_user,
                                    submission.author
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
                            SettingValues.subredditFilters!!.contains(
                                submission.subredditName.lowercase()
                            ),
                            SettingValues.userFilters!!.contains(
                                submission.author.lowercase()
                            ),
                            SettingValues.domainFilters!!.contains(
                                submission.domain.lowercase()
                            ),
                            SettingValues.alwaysExternal!!.contains(
                                submission.domain.lowercase()
                            ),
                            SettingValues.flairFilters!!.contains(
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
                                if (chosen.get(0) && chosen.get(0) != oldChosen.get(0)) {
                                    SettingValues.subredditFilters += submission.getSubredditName()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen.get(0) && chosen.get(0) != oldChosen.get(0)) {
                                    SettingValues.subredditFilters -= submission.getSubredditName()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen.get(1) && chosen.get(1) != oldChosen.get(1)) {
                                    SettingValues.userFilters += submission.getAuthor()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen.get(1) && chosen.get(1) != oldChosen.get(1)) {
                                    SettingValues.userFilters -= submission.getAuthor()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen.get(2) && chosen.get(2) != oldChosen.get(2)) {
                                    SettingValues.domainFilters += submission.getDomain()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = true
                                } else if (!chosen.get(2) && chosen.get(2) != oldChosen.get(2)) {
                                    SettingValues.domainFilters -= submission.getDomain()
                                        .lowercase().trim { it <= ' ' }
                                    filtered = false
                                }
                                if (chosen.get(3) && chosen.get(3) != oldChosen.get(3)) {
                                    SettingValues.alwaysExternal += submission.getDomain()
                                        .lowercase().trim { it <= ' ' }
                                } else if (!chosen.get(3) && chosen.get(3) != oldChosen.get(3)) {
                                    SettingValues.alwaysExternal -= submission.getDomain()
                                        .lowercase().trim { it <= ' ' }
                                }
                                if (chosen.size > 4) {
                                    val s: String = (baseSub + ":" + flair)
                                        .lowercase().trim { it <= ' ' }
                                    if (chosen.get(4) && chosen.get(4) != oldChosen.get(4)) {
                                        SettingValues.flairFilters += s
                                        filtered = true
                                    } else if (!chosen.get(4) && chosen.get(4) != oldChosen.get(
                                            4
                                        )
                                    ) {
                                        SettingValues.flairFilters -= s
                                    }
                                }
                                if (filtered) {
                                    val toRemove: ArrayList<Contribution> = ArrayList()
                                    for (s: Contribution? in posts) {
                                        if (s is Submission && PostMatch.doesMatch(RedditSubmission(s))) {
                                            toRemove.add(s)
                                        }
                                    }
                                    val s: OfflineSubreddit = OfflineSubreddit.getSubreddit(
                                        baseSub,
                                        false, mContext
                                    )!!
                                    for (remove: Contribution? in toRemove) {
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

                    5 -> hideSubmission(submission, posts, baseSub, recyclerview, mContext)
                    7 -> {
                        LinkUtil.openExternally(submission.url)
                        if (submission.isNsfw && !SettingValues.storeNSFWHistory) {
                            //Do nothing if the post is NSFW and storeNSFWHistory is not enabled
                        } else if (SettingValues.storeHistory) {
                            HasSeen.addSeen(submission.fullName)
                        }
                    }

                    28 -> if (!isAddedToReadLaterList) {
                        ReadLater.setReadLater(RedditSubmission(submission), true)
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
                            ReadLater.setReadLater(RedditSubmission(submission), false)
                            val s2 = Snackbar.make(
                                holder.itemView,
                                "Removed from read later", Snackbar.LENGTH_SHORT
                            )
                            LayoutUtils.showSnackbar(s2)
                        })
                        if (NetworkUtil.isConnected(mContext)) {
                            CommentCacheAsync(
                                listOf(RedditSubmission(submission)), mContext,
                                CommentCacheAsync.SAVED_SUBMISSIONS,
                                postRepository, commentRepository,
                                booleanArrayOf(true, true)
                            ).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR
                            )
                        }
                        s.show()
                    } else {
                        ReadLater.setReadLater(RedditSubmission(submission), false)
                        if (isReadLater || !Authentication.didOnline) {
                            val pos = posts.indexOf(submission as T)
                            posts.remove(submission as T)
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
                            s2.setAction(R.string.btn_undo, object : View.OnClickListener {
                                override fun onClick(view: View) {
                                    posts.add(pos, submission as T)
                                    recyclerview.adapter!!.notifyDataSetChanged()
                                }
                            })
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
                            .deleteFromMemory(submission.fullName)
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
                            .onPositive(object : SingleButtonCallback {
                                override fun onClick(dialog: MaterialDialog, which: DialogAction) {
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
                                    PopulateBase.AsyncReportTask(RedditSubmission(submission), holder.itemView)
                                        .executeOnExecutor(
                                            AsyncTask.THREAD_POOL_EXECUTOR,
                                            reportReason
                                        )
                                }
                            }).build()
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
                                return Authentication.reddit!!.getRules(submission.subredditName)
                            }

                            override fun onPostExecute(rules: Ruleset) {
                                reportDialog.customView!!.findViewById<View>(R.id.report_loading).visibility =
                                    View.GONE
                                if (rules.subredditRules.size > 0) {
                                    val subHeader = TextView(mContext)
                                    subHeader.text = mContext.getString(
                                        R.string.report_sub_rules,
                                        submission.subredditName
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

                    8 -> defaultShareText(
                        CompatUtil.fromHtml(submission.title).toString(),
                        submission.permalink, mContext
                    )

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
                            submission.title + "\n\n" + submission.selftext
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
                                val selected: String = showText.getText()
                                    .toString()
                                    .substring(
                                        showText.getSelectionStart(),
                                        showText.getSelectionEnd()
                                    )
                                if (!selected.isEmpty()) {
                                    ClipboardUtil.copyToClipboard(
                                        mContext,
                                        "Selftext",
                                        selected
                                    )
                                } else {
                                    ClipboardUtil.copyToClipboard(
                                        mContext, "Selftext",
                                        CompatUtil.fromHtml(
                                            (submission.getTitle()
                                                    + "\n\n"
                                                    + submission.getSelftext())
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
                                        (submission.getTitle()
                                                + "\n\n"
                                                + submission.getSelftext())
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

    fun <T : Contribution?> hideSubmission(
        submission: Submission,
        posts: MutableList<T>, baseSub: String?, recyclerview: RecyclerView, c: Context?
    ) {
        val pos = posts.indexOf(submission as T)
        if (pos != -1) {
            if (submission.isHidden) {
                posts.removeAt(pos)
                Hidden.undoHidden(RedditSubmission(submission))
                recyclerview.adapter!!.notifyItemRemoved(pos + 1)
                val snack = Snackbar.make(
                    recyclerview, R.string.submission_info_unhidden,
                    Snackbar.LENGTH_LONG
                )
                LayoutUtils.showSnackbar(snack)
            } else {
                val t = posts[pos]
                posts.removeAt(pos)
                Hidden.setHidden(RedditSubmission(t as Submission))
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
                        Hidden.undoHidden(RedditSubmission(t as Submission))
                    }
                LayoutUtils.showSnackbar(snack)
            }
        }
    }

    fun doText(
        holder: NewsViewHolder, submission: Submission?, mContext: Context,
        baseSub: String?
    ) {
        val t = SubmissionCache.getTitleLine(RedditSubmission(submission!!), mContext)
        val l = SubmissionCache.getInfoLine(RedditSubmission(submission!!), mContext, baseSub!!)
        val textSizeAttr = intArrayOf(R.attr.font_cardtitle, R.attr.font_cardinfo)
        val a = mContext.obtainStyledAttributes(textSizeAttr)
        val textSizeT = a.getDimensionPixelSize(0, 18)
        val textSizeI = a.getDimensionPixelSize(1, 14)
        a.recycle()
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
        holder.title.text = s
    }

    fun <T : Contribution?> populateNewsViewHolder(
        holder: NewsViewHolder, submission: Submission, mContext: Activity,
        fullscreen: Boolean, full: Boolean, posts: MutableList<T>,
        recyclerview: RecyclerView, same: Boolean, offline: Boolean,
        baseSub: String?, adapter: CommentAdapter?
    ) {
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
        val submissionScore = submission.score
        val commentCount = submission.commentCount
        val more = LastComments.commentsSince(RedditSubmission(submission))
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

        //Save the score so we can use it in the OnClickListeners for the vote buttons
        val thumbImage2 = holder.thumbnail
        if (holder.leadImage.thumbImage2 == null) {
            holder.leadImage.setThumbnail(thumbImage2)
        }
        val type = ContentType.getContentType(submission)
        addClickFunctions(holder.itemView, type, mContext, submission, holder, full)
        holder.comment.setOnClickListener {
            OpenRedditLink.openUrl(
                mContext,
                submission.permalink,
                true
            )
        }
        if (thumbImage2 != null) {
            addClickFunctions(thumbImage2, type, mContext, submission, holder, full)
        }
        holder.leadImage.setSubmissionNews(RedditSubmission(submission), full, baseSub, type)
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
        doText(holder, submission, mContext, baseSub)
        if (HasSeen.getSeen(submission) && !full) {
            holder.title.alpha = 0.54f
        } else {
            holder.title.alpha = 1f
        }
    }

    companion object {
        private fun addClickFunctions(
            base: View, type: ContentType.Type,
            contextActivity: Activity, submission: Submission,
            holder: NewsViewHolder?, full: Boolean
        ) {
            base.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (NetworkUtil.isConnected(contextActivity) || (!NetworkUtil.isConnected(
                            contextActivity
                        ) && ContentType.fullImage(type))
                    ) {
                        if (SettingValues.storeHistory && !full) {
                            if (!submission.isNsfw || SettingValues.storeNSFWHistory) {
                                HasSeen.addSeen(submission.fullName)
                                if ((contextActivity is MainActivity
                                            || contextActivity is MultiredditOverview
                                            || contextActivity is SubredditView
                                            || contextActivity is Search
                                            || contextActivity is Profile)
                                ) {
                                    holder!!.title.alpha = 0.54f
                                }
                            }
                        }
                        if ((contextActivity !is PeekViewActivity
                                    || !contextActivity.isPeeking()
                                    || ((base is HeaderImageLinkView
                                    && base.popped)))
                        ) {
                            if ((!PostMatch.openExternal(submission.url)
                                        || type == ContentType.Type.VIDEO)
                            ) {
                                when (type) {
                                    ContentType.Type.STREAMABLE -> if (SettingValues.video) {
                                        val myIntent =
                                            Intent(contextActivity, MediaView::class.java)
                                        myIntent.putExtra(
                                            MediaView.SUBREDDIT,
                                            submission.subredditName
                                        )
                                        myIntent.putExtra(MediaView.EXTRA_URL, submission.url)
                                        myIntent.putExtra(
                                            ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                            submission.title
                                        )
                                        contextActivity.startActivity(myIntent)
                                    } else {
                                        LinkUtil.openExternally(submission.url)
                                    }

                                    ContentType.Type.IMGUR, ContentType.Type.DEVIANTART, ContentType.Type.XKCD, ContentType.Type.IMAGE -> openImage(
                                        type, contextActivity, submission, holder!!.leadImage,
                                        holder.bindingAdapterPosition
                                    )

                                    ContentType.Type.EMBEDDED -> if (SettingValues.video) {
                                        val data = CompatUtil.fromHtml(
                                            submission.dataNode["media_embed"]["content"]
                                                .asText()
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
                                        LinkUtil.openExternally(submission.url)
                                    }

                                    ContentType.Type.REDDIT -> openRedditContent(
                                        submission.url,
                                        contextActivity
                                    )

                                    ContentType.Type.LINK -> LinkUtil.openUrl(
                                        submission.url,
                                        Palette.getColor(submission.subredditName),
                                        contextActivity, holder!!.bindingAdapterPosition,
                                        RedditSubmission(submission)
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
                                                submission.subredditName
                                            )
                                        } else {
                                            i = Intent(contextActivity, Album::class.java)
                                            i.putExtra(
                                                Album.SUBREDDIT,
                                                submission.subredditName
                                            )
                                        }
                                        i.putExtra(
                                            ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                            submission.title
                                        )
                                        i.putExtra(Album.EXTRA_URL, submission.url)
                                        PopulateBase.addAdaptorPosition(
                                            i, RedditSubmission(submission),
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(i)
                                        contextActivity.overridePendingTransition(
                                            R.anim.slideright,
                                            R.anim.fade_out
                                        )
                                    } else {
                                        LinkUtil.openExternally(submission.url)
                                    }

                                    ContentType.Type.TUMBLR -> if (SettingValues.album) {
                                        val i: Intent
                                        if (SettingValues.albumSwipe) {
                                            i = Intent(contextActivity, TumblrPager::class.java)
                                            i.putExtra(
                                                TumblrPager.SUBREDDIT,
                                                submission.subredditName
                                            )
                                        } else {
                                            i = Intent(contextActivity, Tumblr::class.java)
                                            i.putExtra(
                                                Tumblr.SUBREDDIT,
                                                submission.subredditName
                                            )
                                        }
                                        i.putExtra(Album.EXTRA_URL, submission.url)
                                        PopulateBase.addAdaptorPosition(
                                            i, RedditSubmission(submission),
                                            holder!!.bindingAdapterPosition
                                        )
                                        contextActivity.startActivity(i)
                                        contextActivity.overridePendingTransition(
                                            R.anim.slideright,
                                            R.anim.fade_out
                                        )
                                    } else {
                                        LinkUtil.openExternally(submission.url)
                                    }

                                    ContentType.Type.GIF -> openGif(
                                        contextActivity, submission,
                                        holder!!.bindingAdapterPosition
                                    )

                                    ContentType.Type.NONE -> holder?.itemView?.performClick()
                                    ContentType.Type.VIDEO -> if (!LinkUtil.tryOpenWithVideoPlugin(
                                            submission.url
                                        )
                                    ) {
                                        LinkUtil.openUrl(
                                            submission.url,
                                            Palette.getStatusBarColor(), contextActivity
                                        )
                                    }

                                    else -> {}
                                }
                            } else {
                                LinkUtil.openExternally(submission.url)
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

        fun openRedditContent(url: String?, c: Context?) {
            OpenRedditLink.openUrl(c, url, true)
        }

        fun openImage(
            type: ContentType.Type, contextActivity: Activity,
            submission: Submission, baseView: HeaderImageLinkView?, adapterPosition: Int
        ) {
            if (SettingValues.image) {
                val myIntent = Intent(contextActivity, MediaView::class.java)
                myIntent.putExtra(MediaView.SUBREDDIT, submission.subredditName)
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
                } else if ((submission.dataNode.has("preview")
                            && submission.dataNode["preview"]["images"][0]["source"]
                        .has("height")
                            && ((type
                            != ContentType.Type.XKCD)))
                ) { //Load the preview image which has probably already been cached in memory instead of the direct link
                    previewUrl = StringEscapeUtils.escapeHtml4(
                        submission.dataNode["preview"]["images"][0]["source"]["url"]
                            .asText()
                    )
                    if (baseView == null || (!SettingValues.loadImageLq && baseView.lq)) {
                        myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl)
                    } else {
                        myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, baseView.loadedUrl)
                    }
                }
                myIntent.putExtra(MediaView.EXTRA_URL, url)
                PopulateBase.addAdaptorPosition(myIntent, RedditSubmission(submission), adapterPosition)
                myIntent.putExtra(MediaView.EXTRA_SHARE_URL, submission.url)
                contextActivity.startActivity(myIntent)
            } else {
                LinkUtil.openExternally(submission.url)
            }
        }

        fun openGif(
            contextActivity: Activity, submission: Submission,
            adapterPosition: Int
        ) {
            if (SettingValues.gif) {
                DataShare.sharedSubmission = RedditSubmission(submission)
                val myIntent = Intent(contextActivity, MediaView::class.java)
                myIntent.putExtra(MediaView.SUBREDDIT, submission.subredditName)
                myIntent.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    submission.title
                )
                val t = AsyncLoadGif.getVideoType(submission.url)
                if ((t == AsyncLoadGif.VideoType.DIRECT) && submission.dataNode
                        .has("preview") && submission.dataNode["preview"]["images"][0]
                        .has("variants") && submission.dataNode["preview"]["images"][0]["variants"]
                        .has("mp4")
                ) {
                    myIntent.putExtra(
                        MediaView.EXTRA_URL, StringEscapeUtils.unescapeJson(
                            submission.dataNode["preview"]["images"][0]["variants"]["mp4"]["source"]["url"]
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
                } else {
                    myIntent.putExtra(MediaView.EXTRA_URL, submission.url)
                }
                if (submission.dataNode.has("preview") && submission.dataNode["preview"]["images"][0]["source"]
                        .has("height")
                ) { //Load the preview image which has probably already been cached in memory instead of the direct link
                    val previewUrl = StringEscapeUtils.escapeHtml4(
                        submission.dataNode["preview"]["images"][0]["source"]["url"]
                            .asText()
                    )
                    myIntent.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl)
                }
                PopulateBase.addAdaptorPosition(myIntent, RedditSubmission(submission), adapterPosition)
                contextActivity.startActivity(myIntent)
            } else {
                LinkUtil.openExternally(submission.url)
            }
        }
    }
}
