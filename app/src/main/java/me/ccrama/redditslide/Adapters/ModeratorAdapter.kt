package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.cocosw.bottomsheet.BottomSheet
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.ActionStates.getVoteDirection
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.Adapters.CommentAdapterHelper.viewReports
import me.ccrama.redditslide.Hidden.setHidden
import me.ccrama.redditslide.Hidden.undoHidden
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder
import me.ccrama.redditslide.Toolbox.ToolboxUI.CompletedRemovalCallback
import me.ccrama.redditslide.Toolbox.ToolboxUI.appendToolboxNote
import me.ccrama.redditslide.Toolbox.ToolboxUI.canShowRemoval
import me.ccrama.redditslide.Toolbox.ToolboxUI.showRemoval
import me.ccrama.redditslide.Toolbox.ToolboxUI.showUsernotes
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.CreateCardView.CreateView
import me.ccrama.redditslide.views.CreateCardView.colorCard
import me.ccrama.redditslide.views.CreateCardView.resetColorCard
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import net.dean.jraw.ApiException
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.ModerationManager
import net.dean.jraw.models.Comment
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import java.util.Arrays

class ModeratorAdapter(
    val mContext: ComponentActivity,
    var dataSet: ModeratorPosts,
    private val listView: RecyclerView
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter {
    private val SPACER = 6
    override fun setError(b: Boolean) {
        listView.adapter = ErrorAdapter()
    }

    override fun undoSetError() {
        listView.adapter = this
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position == 0 && !dataSet.posts.isEmpty()) {
            return SPACER
        } else if (!dataSet.posts.isEmpty()) {
            position -= 1
        }
        if (dataSet.posts[position].fullName.startsWith("t1")) //IS COMMENT
            return COMMENT
        return if (dataSet.posts.get(position).fullName.startsWith("t4")) MESSAGE else POST
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        (itemView)!!
    )

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        if (i == SPACER) {
            val v =
                LayoutInflater.from(viewGroup.context).inflate(R.layout.spacer, viewGroup, false)
            return SpacerViewHolder(v)
        } else if (i == MESSAGE) {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.top_level_message, viewGroup, false)
            return MessageViewHolder(v)
        }
        if (i == COMMENT) {
            val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.profile_comment, viewGroup, false)
            return ProfileCommentViewHolder(v)
        } else {
            val v = CreateView(viewGroup)
            return SubmissionViewHolder(v)
        }
    }

    override fun onBindViewHolder(firstHold: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if (firstHold is SubmissionViewHolder) {
            val holder = firstHold
            val submission = dataSet.posts[i] as Submission
            resetColorCard(holder.itemView)
            colorCard(submission.subredditName.lowercase(), holder.itemView, "no_subreddit", false)
            holder.itemView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    val inflater = mContext.layoutInflater
                    val dialoglayout = inflater.inflate(R.layout.postmenu, null)
                    val title = dialoglayout.findViewById<TextView>(R.id.title)
                    title.text = CompatUtil.fromHtml(submission.title)
                    (dialoglayout.findViewById<View>(R.id.userpopup) as TextView).text =
                        "/u/" + submission.author
                    (dialoglayout.findViewById<View>(R.id.subpopup) as TextView).text =
                        "/c/" + submission.subredditName
                    dialoglayout.findViewById<View>(R.id.sidebar).setOnClickListener(
                        View.OnClickListener {
                            val i = Intent(mContext, Profile::class.java)
                            i.putExtra(Profile.EXTRA_PROFILE, submission.author)
                            mContext.startActivity(i)
                        })
                    dialoglayout.findViewById<View>(R.id.wiki)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                val i = Intent(mContext, SubredditView::class.java)
                                i.putExtra(SubredditView.EXTRA_SUBREDDIT, submission.subredditName)
                                mContext.startActivity(i)
                            }
                        })
                    dialoglayout.findViewById<View>(R.id.save)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                if (submission.isSaved) {
                                    (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(
                                        R.string.submission_save
                                    )
                                } else {
                                    (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(
                                        R.string.submission_post_saved
                                    )
                                }
                                AsyncSave(mContext, firstHold.itemView).execute(submission)
                            }
                        })
                    dialoglayout.findViewById<View>(R.id.copy).visibility = View.GONE
                    if (submission.isSaved) {
                        (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(R.string.submission_post_saved)
                    }
                    dialoglayout.findViewById<View>(R.id.gild)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                val urlString = "https://reddit.com" + submission.permalink
                                val i = Intent(mContext, Website::class.java)
                                i.putExtra(LinkUtil.EXTRA_URL, urlString)
                                mContext.startActivity(i)
                            }
                        })
                    dialoglayout.findViewById<View>(R.id.share)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                if (submission.isSelfPost) {
                                    if (SettingValues.shareLongLink) {
                                        defaultShareText(
                                            "",
                                            "https://reddit.com" + submission.permalink,
                                            mContext
                                        )
                                    } else {
                                        defaultShareText(
                                            "",
                                            "https://redd.it/" + submission.id,
                                            mContext
                                        )
                                    }
                                } else {
                                    BottomSheet.Builder(mContext)
                                        .title(R.string.submission_share_title)
                                        .grid()
                                        .sheet(R.menu.share_menu)
                                        .listener(object : DialogInterface.OnClickListener {
                                            override fun onClick(
                                                dialog: DialogInterface,
                                                which: Int
                                            ) {
                                                when (which) {
                                                    R.id.reddit_url -> if (SettingValues.shareLongLink) {
                                                        defaultShareText(
                                                            submission.title,
                                                            "https://reddit.com" + submission.permalink,
                                                            mContext
                                                        )
                                                    } else {
                                                        defaultShareText(
                                                            submission.title,
                                                            "https://redd.it/" + submission.id,
                                                            mContext
                                                        )
                                                    }

                                                    R.id.link_url -> defaultShareText(
                                                        submission.title,
                                                        submission.url,
                                                        mContext
                                                    )
                                                }
                                            }
                                        }).show()
                                }
                            }
                        })
                    if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                        dialoglayout.findViewById<View>(R.id.save).visibility = View.GONE
                        dialoglayout.findViewById<View>(R.id.gild).visibility = View.GONE
                    }
                    title.setBackgroundColor(Palette.getColor(submission.subredditName))
                    val builder = AlertDialog.Builder(
                        mContext
                    )
                        .setView(dialoglayout)
                    val d: Dialog = builder.show()
                    dialoglayout.findViewById<View>(R.id.hide)
                        .setOnClickListener {
                            val pos = dataSet.posts.indexOf(submission)
                            val old = dataSet.posts[pos]
                            dataSet.posts.remove(submission)
                            notifyItemRemoved(pos + 1)
                            d.dismiss()
                            setHidden(RedditSubmission((old as Submission)))
                            val s = Snackbar.make(
                                listView,
                                R.string.submission_info_hidden,
                                Snackbar.LENGTH_LONG
                            ).setAction(
                                R.string.btn_undo
                            ) {
                                dataSet.posts.add(pos, old)
                                notifyItemInserted(pos + 1)
                                undoHidden(RedditSubmission(old))
                            }
                            LayoutUtils.showSnackbar(s)
                        }
                    return true
                }
            })
            PopulateSubmissionViewHolder(TODO("postRepository"), TODO("commentRepository"))
                .populateSubmissionViewHolder(holder, RedditSubmission(submission), mContext,
                    fullscreen = false,
                    full = false,
                    posts = mutableListOf(),
                    recyclerview = listView,
                    same = false,
                    offline = false,
                    baseSub = null,
                    adapter = null
                )
            val hideButton = holder.itemView.findViewById<ImageView>(R.id.hide)
            if (hideButton != null) {
                hideButton.visibility = View.GONE
            }
            holder.itemView.setOnClickListener {
                var url = "www.reddit.com" + submission.permalink
                url = url.replace("?ref=search_posts", "")
                OpenRedditLink.openUrl(mContext, url, true)
            }
        } else if (firstHold is ProfileCommentViewHolder) {
            //IS COMMENT
            val holder = firstHold
            val comment = dataSet.posts[i] as Comment
            val author = SpannableStringBuilder(comment.author)
            val authorcolor = Palette.getFontColorUser(comment.author)
            if (comment.distinguishedStatus == DistinguishedStatus.ADMIN) {
                author.replace(0, author.length, " " + comment.author + " ")
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext,
                        android.R.color.white,
                        R.color.md_red_300,
                        false
                    ),
                    0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (comment.distinguishedStatus == DistinguishedStatus.SPECIAL) {
                author.replace(0, author.length, " " + comment.author + " ")
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext,
                        android.R.color.white,
                        R.color.md_red_500,
                        false
                    ),
                    0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (comment.distinguishedStatus == DistinguishedStatus.MODERATOR) {
                author.replace(0, author.length, " " + comment.author + " ")
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext,
                        android.R.color.white,
                        R.color.md_green_300,
                        false
                    ),
                    0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (Authentication.name != null && (comment.author
                    .lowercase()
                        == Authentication.name!!.lowercase())
            ) {
                author.replace(0, author.length, " " + comment.author + " ")
                author.setSpan(
                    RoundedBackgroundSpan(
                        mContext, android.R.color.white, R.color.md_deep_orange_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (authorcolor != 0) {
                author.setSpan(
                    ForegroundColorSpan(authorcolor), 0, author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            appendToolboxNote(mContext, author, comment.subredditName, comment.author)
            holder.user.text = author
            holder.user.append(mContext.resources.getString(R.string.submission_properties_seperator))
            holder.user.visibility = View.VISIBLE
            holder.score.text =
                comment.score.toString() + " " + mContext.resources.getQuantityString(
                    R.plurals.points, comment.score
                )
            if (Authentication.isLoggedIn) {
                if (getVoteDirection(comment) == VoteDirection.UPVOTE) {
                    holder.score.setTextColor(mContext.resources.getColor(R.color.md_orange_500))
                } else if (getVoteDirection(comment) == VoteDirection.DOWNVOTE) {
                    holder.score.setTextColor(mContext.resources.getColor(R.color.md_blue_500))
                } else {
                    holder.score.setTextColor(holder.time.currentTextColor)
                }
            }
            val spacer = mContext.getString(R.string.submission_properties_seperator)
            val titleString = SpannableStringBuilder()
            val timeAgo = TimeUtils.getTimeAgo(comment.created.time, mContext)
            var time =
                (if ((timeAgo == null || timeAgo.isEmpty())) "just now" else timeAgo) //some users were crashing here
            time = time + ((if ((comment.editDate != null)) " (edit " + TimeUtils.getTimeAgo(
                comment.editDate.time,
                mContext
            ) + ")" else ""))
            titleString.append(time)
            titleString.append(spacer)
            val mod = holder.itemView.findViewById<ImageView>(R.id.mod)
            try {
                if (UserSubscriptions.modOf!!.contains(comment.subredditName)) {
                    //todo
                    mod.visibility = View.GONE
                } else {
                    mod.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.d(LogUtil.getTag(), "Error loading mod $e")
            }
            if ((UserSubscriptions.modOf != null) && UserSubscriptions.modOf!!.contains(
                    comment.subredditName.lowercase()
                )
            ) {
                mod.visibility = View.VISIBLE
                val reports = comment.userReports
                val reports2 = comment.moderatorReports
                if (reports.size + reports2.size > 0) {
                    BlendModeUtil.tintImageViewAsSrcAtop(
                        mod, ContextCompat.getColor(mContext, R.color.md_red_300)
                    )
                } else {
                    val attrs = intArrayOf(R.attr.tintColor)
                    val ta = mContext.obtainStyledAttributes(attrs)
                    val color = ta.getColor(0, Color.WHITE)
                    BlendModeUtil.tintImageViewAsSrcAtop(mod, color)
                    ta.recycle()
                }
                mod.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        showModBottomSheet(
                            mContext,
                            comment, holder, reports, reports2
                        )
                    }
                })
            } else {
                mod.visibility = View.GONE
            }
            if (comment.subredditName != null) {
                val subname = comment.subredditName
                val subreddit = SpannableStringBuilder("/c/$subname")
                if ((SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor())) {
                    subreddit.setSpan(
                        ForegroundColorSpan(Palette.getColor(subname)),
                        0,
                        subreddit.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    subreddit.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        subreddit.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                titleString.append(subreddit)
            }
            holder.time.text = titleString
            setViews(comment.dataNode["body_html"].asText(), comment.subredditName, holder)
            (holder.gild as TextView).text = ""
            if (!SettingValues.hideCommentAwards && ((comment.timesSilvered > 0) || (comment.timesGilded > 0) || (comment.timesPlatinized > 0))) {
                val a = mContext.obtainStyledAttributes(
                    FontPreferences(mContext).postFontStyle.resId,
                    R.styleable.FontStyle
                )
                val fontsize = (a.getDimensionPixelSize(
                    R.styleable.FontStyle_font_cardtitle,
                    -1
                ) * .75).toInt()
                a.recycle()
                holder.gild.setVisibility(View.VISIBLE)
                // Add silver, gold, platinum icons and counts in that order
                MiscUtil.addAwards(
                    mContext,
                    fontsize,
                    holder,
                    comment.timesSilvered,
                    R.drawable.silver
                )
                MiscUtil.addAwards(mContext, fontsize, holder, comment.timesGilded, R.drawable.gold)
                MiscUtil.addAwards(
                    mContext,
                    fontsize,
                    holder,
                    comment.timesPlatinized,
                    R.drawable.platinum
                )
            } else if (holder.gild.getVisibility() == View.VISIBLE) holder.gild.setVisibility(View.GONE)
            if (comment.submissionTitle != null) holder.title.text =
                CompatUtil.fromHtml(comment.submissionTitle) else holder.title.text =
                CompatUtil.fromHtml(comment.author)
            holder.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    OpenRedditLink.openUrl(
                        mContext,
                        comment.submissionId,
                        comment.subredditName,
                        comment.id
                    )
                }
            })
            holder.content.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    OpenRedditLink.openUrl(
                        mContext,
                        comment.submissionId,
                        comment.subredditName,
                        comment.id
                    )
                }
            })
        }
        if (firstHold is SpacerViewHolder) {
            firstHold.itemView.findViewById<View>(R.id.height).layoutParams =
                LinearLayout.LayoutParams(
                    firstHold.itemView.width, mContext.findViewById<View>(
                        R.id.header
                    ).height
                )
        }
    }

    private fun setViews(rawHTML: String, subredditName: String, holder: ProfileCommentViewHolder) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks.get(0) != "<div class=\"md\">") {
            holder.content.visibility = View.VISIBLE
            holder.content.setTextHtml(blocks[0], subredditName)
            startIndex = 1
        } else {
            holder.content.text = ""
            holder.content.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                holder.overflow.setViews(blocks, subredditName)
            } else {
                holder.overflow.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
        } else {
            holder.overflow.removeAllViews()
        }
    }

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 1
        }
    }

    companion object {
        val COMMENT = 1
        val MESSAGE = 2
        val POST = 3
        fun showModBottomSheet(
            mContext: Context,
            comment: Comment, holder: ProfileCommentViewHolder,
            reports: Map<String, Int>, reports2: Map<String, String>
        ) {
            val attrs = intArrayOf(R.attr.tintColor)
            val ta = mContext.obtainStyledAttributes(attrs)

            //Initialize drawables
            val color = ta.getColor(0, Color.WHITE)
            val profile = mContext.resources.getDrawable(R.drawable.ic_account_circle)
            val report = mContext.resources.getDrawable(R.drawable.ic_report)
            val approve = mContext.resources.getDrawable(R.drawable.ic_thumb_up)
            val nsfw = mContext.resources.getDrawable(R.drawable.ic_visibility_off)
            val pin = mContext.resources.getDrawable(R.drawable.ic_bookmark_border)
            val distinguish = mContext.resources.getDrawable(R.drawable.ic_star)
            val remove = mContext.resources.getDrawable(R.drawable.ic_close)
            val ban = mContext.resources.getDrawable(R.drawable.ic_gavel)
            val spam = mContext.resources.getDrawable(R.drawable.ic_flag)
            val note = mContext.resources.getDrawable(R.drawable.ic_note)
            val removeReason = mContext.resources.getDrawable(R.drawable.ic_announcement)
            val lock = mContext.resources.getDrawable(R.drawable.ic_lock)

            //Tint drawables
            val drawableSet = Arrays.asList(
                profile, report, approve, nsfw, distinguish, remove,
                pin, ban, spam, note, removeReason, lock
            )
            BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
            ta.recycle()

            //Bottom sheet builder
            val b = BottomSheet.Builder(
                (mContext as Activity)
            ).title(
                CompatUtil.fromHtml(comment.body)
            )
            val reportCount = reports.size + reports2.size
            if (reportCount == 0) {
                b.sheet(0, report, mContext.getString(R.string.mod_no_reports))
            } else {
                b.sheet(
                    0, report, mContext.getResources()
                        .getQuantityString(R.plurals.mod_btn_reports, reportCount, reportCount)
                )
            }
            if (SettingValues.toolboxEnabled) {
                b.sheet(24, note, mContext.getString(R.string.mod_usernotes_view))
            }
            b.sheet(1, approve, mContext.getString(R.string.mod_btn_approve))
            b.sheet(6, remove, mContext.getString(R.string.btn_remove))
            b.sheet(7, removeReason, mContext.getString(R.string.mod_btn_remove_reason))
            b.sheet(10, spam, mContext.getString(R.string.mod_btn_spam))
            val locked = (comment.dataNode.has("locked")
                    && comment.dataNode["locked"].asBoolean())
            if (locked) {
                b.sheet(25, lock, mContext.getString(R.string.mod_btn_unlock_comment))
            } else {
                b.sheet(25, lock, mContext.getString(R.string.mod_btn_lock_comment))
            }
            val distinguished = !comment.dataNode["distinguished"].isNull
            if (comment.author.equals(Authentication.name, ignoreCase = true)) {
                if (!distinguished) {
                    b.sheet(9, distinguish, mContext.getString(R.string.mod_distinguish))
                } else {
                    b.sheet(9, distinguish, mContext.getString(R.string.mod_undistinguish))
                }
            }
            b.sheet(8, profile, mContext.getString(R.string.mod_btn_author))
            b.sheet(23, ban, mContext.getString(R.string.mod_ban_user))
            b.listener { dialog, which ->
                when (which) {
                    0 -> viewReports(mContext, reports, reports2)
                    1 -> doApproval(mContext, holder, comment)
                    9 -> if (distinguished) {
                        unDistinguishComment(mContext, holder, comment)
                    } else {
                        distinguishComment(mContext, holder, comment)
                    }

                    6 -> removeComment(mContext, holder, comment, false)
                    7 -> if ((SettingValues.removalReasonType == SettingValues.RemovalReasonType.TOOLBOX.ordinal
                                && canShowRemoval(comment.subredditName))
                    ) {
                        showRemoval(mContext, comment, object : CompletedRemovalCallback {
                            override fun onComplete(success: Boolean) {
                                if (success) {
                                    val s = Snackbar.make(
                                        holder.itemView, R.string.comment_removed,
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
                    } else { // Show a Slide reason dialog if we can't show a toolbox or reddit reason
                        doRemoveCommentReason(mContext, holder, comment)
                    }

                    10 -> removeComment(mContext, holder, comment, true)
                    8 -> {
                        val i = Intent(mContext, Profile::class.java)
                        i.putExtra(Profile.EXTRA_PROFILE, comment.author)
                        mContext.startActivity(i)
                    }

                    23 -> {}
                    24 -> showUsernotes(
                        mContext, comment.author, comment.subredditName,
                        "l," + comment.parentId + "," + comment.id
                    )

                    25 -> lockUnlockComment(mContext, holder, comment, !locked)
                }
            }
            b.show()
        }

        fun doApproval(
            mContext: Context?, holder: ProfileCommentViewHolder,
            comment: Comment?
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        Snackbar.make(holder.itemView, R.string.mod_approved, Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        ModerationManager(Authentication.reddit).approve(comment)
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            }.execute()
        }

        fun distinguishComment(
            mContext: Context?, holder: ProfileCommentViewHolder,
            comment: Comment?
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        val s = Snackbar.make(
                            holder.itemView, R.string.comment_distinguished,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        ModerationManager(Authentication.reddit).setDistinguishedStatus(
                            comment,
                            DistinguishedStatus.MODERATOR
                        )
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            }.execute()
        }

        fun unDistinguishComment(
            mContext: Context?, holder: ProfileCommentViewHolder,
            comment: Comment?
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        val s = Snackbar.make(
                            holder.itemView, R.string.comment_undistinguished,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        ModerationManager(Authentication.reddit).setDistinguishedStatus(
                            comment,
                            DistinguishedStatus.NORMAL
                        )
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            }.execute()
        }

        fun removeComment(
            mContext: Context?, holder: ProfileCommentViewHolder,
            comment: Comment?, spam: Boolean
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        val s = Snackbar.make(
                            holder.itemView, R.string.comment_removed,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        ModerationManager(Authentication.reddit).remove(comment, spam)
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            }.execute()
        }

        /**
         * Show a removal dialog to input a reason, then remove comment and post reason
         * @param mContext context
         * @param holder commentviewholder
         * @param comment comment
         */
        fun doRemoveCommentReason(
            mContext: Context, holder: ProfileCommentViewHolder,
            comment: Comment
        ) {
            MaterialDialog(mContext)
                .title(R.string.mod_remove_title)
                .input(
                    hintRes = R.string.mod_remove_hint,
                    prefillRes = R.string.mod_remove_template,
                    waitForPositiveButton = false,
                    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                ) { dialog, input -> }
                .positiveButton(R.string.btn_remove) { dialog: MaterialDialog ->
                    removeCommentReason(
                        comment,
                        mContext,
                        holder,
                        dialog.getInputField().text.toString()
                    )
                }
                .neutralButton(R.string.mod_remove_insert_draft)
                .negativeButton(R.string.btn_cancel)
                .show()
        }

        /**
         * Remove a comment and post a reason
         * @param comment comment
         * @param mContext context
         * @param holder commentviewholder
         * @param reason reason
         */
        fun removeCommentReason(
            comment: Comment, mContext: Context?,
            holder: ProfileCommentViewHolder, reason: String?
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        val s = Snackbar.make(
                            holder.itemView,
                            R.string.comment_removed,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        AccountManager(Authentication.reddit).reply(comment, reason)
                        ModerationManager(Authentication.reddit).remove(comment, false)
                        ModerationManager(Authentication.reddit).setDistinguishedStatus(
                            Authentication.reddit!![comment.fullName][0],
                            DistinguishedStatus.MODERATOR
                        )
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

        fun lockUnlockComment(
            mContext: Context?, holder: ProfileCommentViewHolder,
            comment: Comment?, lock: Boolean
        ) {
            object : AsyncTask<Void?, Void?, Boolean>() {
                public override fun onPostExecute(b: Boolean) {
                    if (b) {
                        val s = Snackbar.make(
                            holder.itemView,
                            if (lock) R.string.mod_locked else R.string.mod_unlocked,
                            Snackbar.LENGTH_LONG
                        )
                        LayoutUtils.showSnackbar(s)
                    } else {
                        AlertDialog.Builder((mContext)!!)
                            .setTitle(R.string.err_general)
                            .setMessage(R.string.err_retry_later)
                            .show()
                    }
                }

                override fun doInBackground(vararg params: Void?): Boolean {
                    try {
                        if (lock) {
                            ModerationManager(Authentication.reddit).setLocked(comment)
                        } else {
                            ModerationManager(Authentication.reddit).setUnlocked(comment)
                        }
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        return false
                    }
                    return true
                }
            }.execute()
        }
    }
}
