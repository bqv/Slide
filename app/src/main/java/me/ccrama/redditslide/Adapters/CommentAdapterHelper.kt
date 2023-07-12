package me.ccrama.redditslide.Adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.cocosw.bottomsheet.BottomSheet
import com.google.android.material.snackbar.Snackbar
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.data.type.CommentView
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.commentLastVisit
import ltd.ucode.network.data.IPost
import me.ccrama.redditslide.ActionStates.getVoteDirection
import me.ccrama.redditslide.ActionStates.isSaved
import me.ccrama.redditslide.ActionStates.setSaved
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.Reauthenticate
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.Toolbox.ToolboxUI.CompletedRemovalCallback
import me.ccrama.redditslide.Toolbox.ToolboxUI.appendToolboxNote
import me.ccrama.redditslide.Toolbox.ToolboxUI.canShowRemoval
import me.ccrama.redditslide.Toolbox.ToolboxUI.showRemoval
import me.ccrama.redditslide.Toolbox.ToolboxUI.showUsernotes
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserTags
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.ClipboardUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.DoEditorActions
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import net.dean.jraw.ApiException
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.oauth.InvalidScopeException
import net.dean.jraw.managers.ModerationManager
import net.dean.jraw.models.Ruleset
import net.dean.jraw.models.SubredditRule
import net.dean.jraw.models.VoteDirection
import org.apache.commons.text.StringEscapeUtils
import java.util.Arrays
import java.util.Locale

object CommentAdapterHelper {
    fun showOverflowBottomSheet(
        adapter: CommentAdapter, mContext: Context,
        holder: CommentViewHolder, baseNode: CommentView
    ) {
        val attrs = intArrayOf(R.attr.tintColor)
        val n = baseNode
        val ta = mContext.obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val profile = mContext.resources.getDrawable(R.drawable.ic_account_circle)
        val saved = mContext.resources.getDrawable(R.drawable.ic_star)
        val gild = mContext.resources.getDrawable(R.drawable.ic_stars)
        val copy = mContext.resources.getDrawable(R.drawable.ic_content_copy)
        val share = mContext.resources.getDrawable(R.drawable.ic_share)
        val parent = mContext.resources.getDrawable(R.drawable.ic_forum)
        val replies = mContext.resources.getDrawable(R.drawable.ic_notifications)
        val permalink = mContext.resources.getDrawable(R.drawable.ic_link)
        val report = mContext.resources.getDrawable(R.drawable.ic_report)
        val drawableSet = Arrays.asList(
            profile, saved, gild, report, copy, share, parent, permalink, replies
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder((mContext as Activity)).title(CompatUtil.fromHtml(n.comment.contentHtml))
        if (Authentication.didOnline) {
            b.sheet(1, profile, "/u/" + n.creator.name)
            var save: String = mContext.getString(R.string.btn_save)
            if (isSaved(n)) {
                save = mContext.getString(R.string.comment_unsave)
            }
            if (Authentication.isLoggedIn) {
                b.sheet(3, saved, (save))
                b.sheet(16, report, mContext.getString(R.string.btn_report))
            }
            if (Authentication.name.equals(baseNode.creator.name, ignoreCase = true)) {
                b.sheet(50, replies, mContext.getString(R.string.disable_replies_comment))
            }
        }
        b.sheet(5, gild, mContext.getString(R.string.comment_gild))
            .sheet(7, copy, mContext.getString(R.string.misc_copy_text))
            .sheet(23, permalink, mContext.getString(R.string.comment_permalink))
            .sheet(4, share, mContext.getString(R.string.comment_share))
        if (!adapter.currentBaseNode!!.comment.isTopLevel) {
            b.sheet(10, parent, mContext.getString(R.string.comment_parent))
        }
        b.listener { dialog, which ->
            when (which) {
                1 -> {

                    //Go to author
                    val i = Intent(mContext, Profile::class.java)
                    i.putExtra(Profile.EXTRA_PROFILE, n.creator.name)
                    mContext.startActivity(i)
                }

                3 ->                         //Save comment
                    saveComment(n, mContext, holder)

                23 -> {

                    //Go to comment permalink
                    val s = ("https://${n.instanceName}/comment/${n.comment.id.id}"
                            + "?context=3")
                    OpenRedditLink.openUrl(mContext, s, true)
                }

                50 -> {
                    setReplies(
                        baseNode,
                        holder,
                        false
                    )
                }

                5 -> {

                    //Gild comment
                }

                16 -> {
                    //report
                    val reportDialog = MaterialDialog(mContext).show {
                        customView(R.layout.report_dialog, scrollable = true)
                        title(R.string.report_comment)
                        negativeButton(R.string.btn_cancel)
                        positiveButton(R.string.btn_report) { dialog ->
                            val reasonGroup =
                                dialog.view.findViewById<RadioGroup>(R.id.report_reasons)
                            val reportReason: String =
                                if (reasonGroup.checkedRadioButtonId == R.id.report_other) {
                                    (dialog.view.findViewById<View>(R.id.input_report_reason) as EditText).text.toString()
                                } else {
                                    (reasonGroup
                                        .findViewById<View>(reasonGroup.checkedRadioButtonId) as RadioButton)
                                        .text.toString()
                                }
                            AsyncReportTask(adapter.currentBaseNode, adapter.listView)
                                .execute(reportReason)
                        }
                    }
                    val reasonGroup =
                        reportDialog.view.findViewById<RadioGroup>(R.id.report_reasons)
                    reasonGroup.setOnCheckedChangeListener { group, checkedId ->
                        if (checkedId == R.id.report_other)
                            reportDialog.view.findViewById<View>(
                                R.id.input_report_reason
                            ).visibility = View.VISIBLE
                        else reportDialog.view
                            .findViewById<View>(R.id.input_report_reason).visibility = View.GONE
                    }

                    // Load sub's report reasons and show the appropriate ones
                    object : AsyncTask<Void?, Void?, Ruleset>() {
                        override fun doInBackground(vararg voids: Void?): Ruleset {
                            /*
                            return Authentication.reddit!!.getRules(
                                adapter.currentBaseNode!!.community.name
                            )
                             */throw Exception("TODO")
                        }

                        override fun onPostExecute(rules: Ruleset) {
                            reportDialog.view.findViewById<View>(R.id.report_loading).visibility =
                                View.GONE
                            if (rules.subredditRules.size > 0) {
                                val subHeader = TextView(mContext)
                                subHeader.text = mContext.getString(
                                    R.string.report_sub_rules,
                                    adapter.currentBaseNode!!.community.name
                                )
                                reasonGroup.addView(subHeader, reasonGroup.childCount - 2)
                            }
                            for (rule: SubredditRule in rules.subredditRules) {
                                if ((rule.kind == SubredditRule.RuleKind.COMMENT
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

                10 -> //View comment parent
                    viewCommentParent(adapter, holder, mContext, baseNode)

                7 -> {
                    //Show select and copy text to clipboard
                    val showText = TextView(mContext)
                    showText.text = StringEscapeUtils.unescapeHtml4(n.comment.contentHtml)
                    showText.setTextIsSelectable(true)
                    val sixteen = DisplayUtil.dpToPxVertical(24)
                    showText.setPadding(sixteen, 0, sixteen, 0)
                    AlertDialog.Builder(mContext)
                        .setView(showText)
                        .setTitle("Select text to copy")
                        .setCancelable(true)
                        .setPositiveButton("COPY") { dialog1: DialogInterface?, which1: Int ->
                            val selected: String = showText.getText()
                                .toString()
                                .substring(
                                    showText.getSelectionStart(),
                                    showText.getSelectionEnd()
                                )
                            ClipboardUtil.copyToClipboard(mContext, "Comment text", selected)
                            Toast.makeText(
                                mContext, R.string.submission_comment_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .setNeutralButton(
                            "COPY ALL"
                        ) { dialog12: DialogInterface?, which12: Int ->
                            ClipboardUtil.copyToClipboard(
                                mContext, "Comment text",
                                n.comment.contentHtml
                            )
                            Toast.makeText(
                                mContext,
                                R.string.submission_comment_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .show()
                }

                4 ->                         //Share comment
                    defaultShareText(
                        adapter.submission!!.title,
                        ("https://${n.instanceName}/comment/${n.comment.id.id}"
                                + "?context=3"), mContext
                    )
            }
        }
        b.show()
    }

    private fun setReplies(comment: CommentView, holder: CommentViewHolder, showReplies: Boolean) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    throw Exception("TODO")//AccountManager(Authentication.reddit).sendRepliesToInbox(comment, showReplies)
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                val s: Snackbar
                try {
                    if (holder.itemView != null) {
                        if (!showReplies) {
                            s = Snackbar.make(
                                holder.itemView, R.string.replies_disabled_comment,
                                Snackbar.LENGTH_LONG
                            )
                        } else {
                            s = Snackbar.make(
                                holder.itemView, R.string.replies_enabled_comment,
                                Snackbar.LENGTH_SHORT
                            )
                        }
                        LayoutUtils.showSnackbar(s)
                    }
                } catch (ignored: Exception) {
                }
            }
        }.execute()
    }

    private fun viewCommentParent(
        adapter: CommentAdapter, holder: CommentViewHolder,
        mContext: Context, baseNode: CommentView
    ) {
        val old = holder.bindingAdapterPosition
        val pos = if ((old < 2)) 0 else old - 1
        for (i in pos - 1 downTo 0) {
            val o = (adapter.currentComments!![adapter.getRealPosition(i)])!!
            if ((o is CommentItem
                        && (pos - 1 != i
                        ) && (o.comment!!.comment.depth < baseNode.comment.depth))
            ) {
                val inflater = (mContext as Activity).layoutInflater
                val dialoglayout = inflater.inflate(R.layout.parent_comment_dialog, null)
                /*
                Comment parent = o.comment.getComment();
                adapter.setViews(parent.getDataNode().get("body_html").asText(),
                        adapter.submission.getGroupName(),
                        dialoglayout.findViewById(R.id.firstTextView),
                        dialoglayout.findViewById(R.id.commentOverflow));
                 */AlertDialog.Builder(mContext)
                    .setView(dialoglayout)
                    .show()
                break
            }
        }
    }

    private fun saveComment(
        comment: CommentView, mContext: Context,
        holder: CommentViewHolder
    ) {
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    if (isSaved(comment)) {
                        //AccountManager(Authentication.reddit).unsave(comment)
                        setSaved(comment, false)
                    } else {
                        //AccountManager(Authentication.reddit).save(comment)
                        setSaved(comment, true)
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                val s: Snackbar
                try {
                    if (holder.itemView != null) {
                        s = if (isSaved(comment)) {
                            Snackbar.make(
                                holder.itemView, R.string.submission_comment_saved,
                                Snackbar.LENGTH_LONG
                            )
                        } else {
                            Snackbar.make(
                                holder.itemView, R.string.submission_comment_unsaved,
                                Snackbar.LENGTH_SHORT
                            )
                        }
                        LayoutUtils.showSnackbar(s)
                    }
                } catch (ignored: Exception) {
                }
            }
        }.execute()
    }

    private fun categorizeComment(comment: CommentView, mContext: Context) {
        object : AsyncTask<Void?, Void?, List<String?>>() {
            var d: Dialog? = null
            public override fun onPreExecute() {
                d = MaterialDialog(mContext).show {
                    //progress(true, 100)
                    message(R.string.misc_please_wait)
                    title(R.string.profile_category_loading)
                }
            }

            override fun doInBackground(vararg params: Void?): List<String?> {
                try {
                    /*
                    val categories: MutableList<String?> = ArrayList(
                        AccountManager(Authentication.reddit).savedCategories
                    )
                    categories.add("New category")
                    return categories
                     */throw Exception("TODO")
                } catch (e: Exception) {
                    e.printStackTrace()
                    return object : ArrayList<String?>() {
                        init {
                            add("New category")
                        }
                    }
                }
            }

            public override fun onPostExecute(data: List<String?>) {
                // No flairs here
            }
        }.execute()
    }

    fun showModBottomSheet(
        adapter: CommentAdapter, mContext: Context,
        baseNode: CommentView, comment: CommentView, holder: CommentViewHolder,
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
        val b = BottomSheet.Builder((mContext as Activity)).title(
            CompatUtil.fromHtml(comment.comment.contentHtml)
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
        val locked = false
        if (locked) {
            b.sheet(25, lock, mContext.getString(R.string.mod_btn_unlock_comment))
        } else {
            b.sheet(25, lock, mContext.getString(R.string.mod_btn_lock_comment))
        }
        val stickied = comment.comment.isDistinguished
        if (baseNode.comment.isTopLevel && comment.creator.name.equals(Authentication.name, ignoreCase = true)) {
            if (!stickied) {
                b.sheet(4, pin, mContext.getString(R.string.mod_sticky))
            } else {
                b.sheet(4, pin, mContext.getString(R.string.mod_unsticky))
            }
        }
        val distinguished = comment.comment.isDistinguished
        if (comment.creator.name.equals(Authentication.name, ignoreCase = true)) {
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
                1 -> doApproval(mContext, holder, comment, adapter)
                4 -> if (stickied) {
                    unStickyComment(mContext, holder, comment)
                } else {
                    stickyComment(mContext, holder, comment)
                }

                9 -> if (distinguished) {
                    unDistinguishComment(mContext, holder, comment)
                } else {
                    distinguishComment(mContext, holder, comment)
                }

                6 -> removeComment(mContext, holder, comment, adapter, false)
                7 -> if ((SettingValues.removalReasonType == SettingValues.RemovalReasonType.TOOLBOX.ordinal
                            && canShowRemoval(comment.community.name))
                ) {
                    showRemoval(mContext, comment, object : CompletedRemovalCallback {
                        override fun onComplete(success: Boolean) {
                            if (success) {
                                val s = Snackbar.make(
                                    holder.itemView, R.string.comment_removed,
                                    Snackbar.LENGTH_LONG
                                )
                                LayoutUtils.showSnackbar(s)
                                adapter.removed.add(comment.permalink)
                                adapter.approved.remove(comment.permalink)
                                holder.content.text = getScoreString(
                                    comment,
                                    mContext,
                                    holder,
                                    adapter.submission!!,
                                    adapter
                                )
                            } else {
                                AlertDialog.Builder(mContext)
                                    .setTitle(R.string.err_general)
                                    .setMessage(R.string.err_retry_later)
                                    .show()
                            }
                        }
                    })
                } else { // Show a Slide reason dialog if we can't show a toolbox or reddit one
                    doRemoveCommentReason(mContext, holder, comment, adapter)
                }

                10 -> removeComment(mContext, holder, comment, adapter, true)
                8 -> {
                    val i = Intent(mContext, Profile::class.java)
                    i.putExtra(Profile.EXTRA_PROFILE, comment.creator.name)
                    mContext.startActivity(i)
                }

                23 -> showBan(mContext, adapter.listView, comment, "", "", "", "")
                24 -> showUsernotes(
                    mContext, comment.creator.name, comment.community.name,
                    "l," + comment.comment.parent.id + "," + comment.comment.id.id
                )

                25 -> lockUnlockComment(mContext, holder, comment, !locked)
            }
        }
        b.show()
    }

    @JvmStatic
    fun showBan(
        mContext: Context, mToolbar: View?,
        submission: CommentView, rs: String?, nt: String?, msg: String?, t: String?
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
            .setPositiveButton(R.string.mod_btn_ban) { dialog: DialogInterface?, which: Int ->
                //to ban
                if (reason.getText().toString().isEmpty()) {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.mod_ban_reason_required)
                        .setMessage(R.string.misc_please_try_again)
                        .setPositiveButton(
                            R.string.btn_ok,
                            DialogInterface.OnClickListener { dialog1: DialogInterface?, which1: Int ->
                                showBan(
                                    mContext, mToolbar, submission,
                                    reason.getText().toString(),
                                    note.getText().toString(),
                                    message.getText().toString(),
                                    time.getText().toString()
                                )
                            })
                        .setCancelable(false)
                        .show()
                } else {
                    object : AsyncTask<Void?, Void?, Boolean>() {
                        override fun doInBackground(vararg params: Void?): Boolean {
                            try {
                                var n: String? = note.getText().toString()
                                var m: String? = message.getText().toString()
                                if (n!!.isEmpty()) {
                                    n = null
                                }
                                if (m!!.isEmpty()) {
                                    m = null
                                }
                                if (time.getText().toString().isEmpty()) {
                                    ModerationManager(Authentication.reddit).banUserPermanently(
                                        submission.community.name,
                                        submission.creator.name,
                                        reason.getText().toString(),
                                        n,
                                        m
                                    )
                                } else {
                                    ModerationManager(Authentication.reddit).banUser(
                                        submission.community.name,
                                        submission.creator.name,
                                        reason.getText().toString(),
                                        n, m, time.getText().toString().toInt()
                                    )
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
                                    .setAction(R.string.misc_try_again,
                                        object : View.OnClickListener {
                                            override fun onClick(v: View) {
                                                showBan(
                                                    mContext, mToolbar,
                                                    submission,
                                                    reason.getText().toString(),
                                                    note.getText().toString(),
                                                    message.getText()
                                                        .toString(),
                                                    time.getText().toString()
                                                )
                                            }
                                        })
                            }
                            if (s != null) {
                                LayoutUtils.showSnackbar(s)
                            }
                        }
                    }.execute()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    fun distinguishComment(
        mContext: Context?, holder: CommentViewHolder,
        comment: CommentView?
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
                    /*
                    ModerationManager(Authentication.reddit).setDistinguishedStatus(
                        comment,
                        DistinguishedStatus.MODERATOR
                    )
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    fun unDistinguishComment(
        mContext: Context?, holder: CommentViewHolder,
        comment: CommentView?
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
                    /*
                    ModerationManager(Authentication.reddit).setDistinguishedStatus(
                        comment,
                        DistinguishedStatus.NORMAL
                    )
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    fun stickyComment(
        mContext: Context?, holder: CommentViewHolder,
        comment: CommentView?
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, R.string.comment_stickied,
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
                    /*
                    ModerationManager(Authentication.reddit).setSticky(comment, true)
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    @JvmStatic
    fun viewReports(
        mContext: Context, reports: Map<String, Int>,
        reports2: Map<String, String>
    ) {
        object : AsyncTask<Void?, Void?, ArrayList<String>>() {
            override fun doInBackground(vararg params: Void?): ArrayList<String> {
                val finalReports = ArrayList<String>()
                for (entry: Map.Entry<String, Int> in reports.entries) {
                    finalReports.add("x" + entry.value + " " + entry.key)
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
        }.execute()
    }

    fun doApproval(
        mContext: Context, holder: CommentViewHolder,
        comment: CommentView, adapter: CommentAdapter
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    adapter.approved.add(comment.permalink)
                    adapter.removed.remove(comment.permalink)
                    holder.content.text = getScoreString(
                        comment, mContext, holder,
                        adapter.submission!!, adapter
                    )
                    Snackbar.make(holder.itemView, R.string.mod_approved, Snackbar.LENGTH_LONG)
                        .show()
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    /*
                    ModerationManager(Authentication.reddit).approve(comment)
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    fun unStickyComment(
        mContext: Context?, holder: CommentViewHolder,
        comment: CommentView?
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, R.string.comment_unstickied,
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
                    /*
                    ModerationManager(Authentication.reddit).setSticky(comment, false)
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    fun removeComment(
        mContext: Context, holder: CommentViewHolder,
        comment: CommentView, adapter: CommentAdapter, spam: Boolean
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, R.string.comment_removed,
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                    adapter.removed.add(comment.permalink)
                    adapter.approved.remove(comment.permalink)
                    holder.content.text = getScoreString(
                        comment, mContext, holder,
                        adapter.submission!!, adapter
                    )
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    /*
                    ModerationManager(Authentication.reddit).remove(comment, spam)
                    */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                } catch (e: NetworkException) {
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
     * @param adapter commentadapter
     */
    fun doRemoveCommentReason(
        mContext: Context,
        holder: CommentViewHolder, comment: CommentView, adapter: CommentAdapter
    ) {
        MaterialDialog(mContext).show {
            title(R.string.mod_remove_title)
            input(hintRes = R.string.mod_remove_hint,
                prefillRes = R.string.mod_remove_template,
                inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                waitForPositiveButton = false) { _, _ -> }
            neutralButton(R.string.mod_remove_insert_draft)
            positiveButton(R.string.btn_remove) { dialog ->
                removeCommentReason(
                    comment, mContext, holder, adapter,
                    dialog.getInputField().text.toString()
                )
            }
            negativeButton(R.string.btn_cancel)
        }
    }

    /**
     * Remove a comment and post a reason
     * @param comment comment
     * @param mContext context
     * @param holder commentviewholder
     * @param adapter commentadapter
     * @param reason reason
     */
    fun removeCommentReason(
        comment: CommentView, mContext: Context, holder: CommentViewHolder,
        adapter: CommentAdapter, reason: String?
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
                    adapter.removed.add(comment.permalink)
                    adapter.approved.remove(comment.permalink)
                    holder.content.text = getScoreString(
                        comment, mContext, holder,
                        adapter.submission!!, adapter
                    )
                } else {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.err_general)
                        .setMessage(R.string.err_retry_later)
                        .show()
                }
            }

            override fun doInBackground(vararg params: Void?): Boolean {
                try {
                    /*
                    AccountManager(Authentication.reddit).reply(comment, reason)
                    ModerationManager(Authentication.reddit).remove(comment, false)
                    ModerationManager(Authentication.reddit).setDistinguishedStatus(
                        Authentication.reddit!![comment.fullName][0],
                        DistinguishedStatus.MODERATOR
                    )
                     */throw Exception("TODO")
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
        mContext: Context?, holder: CommentViewHolder,
        comment: CommentView?, lock: Boolean
    ) {
        object : AsyncTask<Void?, Void?, Boolean>() {
            public override fun onPostExecute(b: Boolean) {
                if (b) {
                    val s = Snackbar.make(
                        holder.itemView, if (lock) R.string.mod_locked else R.string.mod_unlocked,
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
                    /*
                    if (lock) {
                        ModerationManager(Authentication.reddit).setLocked(comment)
                    } else {
                        ModerationManager(Authentication.reddit).setUnlocked(comment)
                    }
                     */throw Exception("TODO")
                } catch (e: ApiException) {
                    e.printStackTrace()
                    return false
                }
                return true
            }
        }.execute()
    }

    fun createApprovedLine(approvedBy: String?, c: Context): SpannableStringBuilder {
        val removedString = SpannableStringBuilder("\n")
        val mod = SpannableStringBuilder("Approved by ")
        mod.append(approvedBy)
        mod.setSpan(
            StyleSpan(Typeface.BOLD), 0, mod.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mod.setSpan(
            RelativeSizeSpan(0.8f), 0, mod.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mod.setSpan(
            ForegroundColorSpan(c.resources.getColor(R.color.md_green_300)), 0,
            mod.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        removedString.append(mod)
        return removedString
    }

    fun createRemovedLine(removedBy: String?, c: Context): SpannableStringBuilder {
        val removedString = SpannableStringBuilder("\n")
        var mod = SpannableStringBuilder("Removed by ")
        if (removedBy.equals(
                "true", ignoreCase = true
            )
        ) { //Probably shadowbanned or removed not by mod action
            mod = SpannableStringBuilder("Removed by Reddit")
        } else {
            mod.append(removedBy)
        }
        mod.setSpan(
            StyleSpan(Typeface.BOLD), 0, mod.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mod.setSpan(
            RelativeSizeSpan(0.8f), 0, mod.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mod.setSpan(
            ForegroundColorSpan(c.resources.getColor(R.color.md_red_300)), 0,
            mod.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        removedString.append(mod)
        return removedString
    }

    fun getScoreString(
        comment: CommentView, mContext: Context,
        holder: CommentViewHolder, submission: IPost?, adapter: CommentAdapter
    ): Spannable {
        val spacer =
            " " + mContext.getString(R.string.submission_properties_seperator_comments) + " "
        val titleString =
            SpannableStringBuilder("\u200B") //zero width space to fix first span height
        val author = SpannableStringBuilder(comment.creator.name)
        val authorcolor = Palette.getFontColorUser(comment.creator.name)
        author.setSpan(
            TypefaceSpan("sans-serif-condensed"), 0, author.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        author.setSpan(
            StyleSpan(Typeface.BOLD), 0, author.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (comment.comment.isDistinguished) {
            author.replace(0, author.length, " " + comment.creator.name + " ")
            author.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_red_300, false),
                0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (comment.comment.isDistinguished) {
            author.replace(0, author.length, " " + comment.creator.name + " ")
            author.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_red_500, false),
                0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (comment.comment.isDistinguished) {
            author.replace(0, author.length, " " + comment.creator.name + " ")
            author.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_green_300, false),
                0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (Authentication.name != null && (comment.creator.name.lowercase()
                    == Authentication.name!!.lowercase())
        ) {
            author.replace(0, author.length, " " + comment.creator.name + " ")
            author.setSpan(
                RoundedBackgroundSpan(
                    mContext, android.R.color.white, R.color.md_deep_orange_300,
                    false
                ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if ((submission != null) && (comment.creator.name
                .lowercase()
                    == submission.user.name.lowercase()) && comment.creator.name != "[deleted]"
        ) {
            author.replace(0, author.length, " " + comment.creator.name + " ")
            author.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_300, false),
                0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else if (authorcolor != 0) {
            author.setSpan(
                ForegroundColorSpan(authorcolor), 0, author.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        titleString.append(author)
        titleString.append(spacer)
        val scoreColor: Int = when (getVoteDirection(comment)) {
            VoteDirection.UPVOTE -> (holder.textColorUp)
            VoteDirection.DOWNVOTE -> (holder.textColorDown)
            else -> (holder.textColorRegular)
        }
        val scoreText: String =
            String.format(Locale.getDefault(), "%d", getScoreText(comment))
        var score: SpannableStringBuilder = SpannableStringBuilder(scoreText)
        if (score == null || score.toString().isEmpty()) {
            score = SpannableStringBuilder("0")
        }
        if (!scoreText.contains("[")) {
            score.append(
                String.format(
                    Locale.getDefault(), " %s", mContext.resources
                        .getQuantityString(R.plurals.points, comment.counts.score)
                )
            )
        }
        score.setSpan(
            ForegroundColorSpan(scoreColor), 0, score.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        titleString.append(score)
        titleString.append(spacer)
        val time = comment.comment.published.toInstant(TimeZone.UTC).toEpochMilliseconds()
        val timeAgo = TimeUtils.getTimeAgo(time, mContext)
        val timeSpan = SpannableStringBuilder().append(
            if ((timeAgo == null || timeAgo.isEmpty())) "just now" else timeAgo
        )
        if ((SettingValues.highlightTime
                    && (adapter.lastSeen != 0L
                    ) && (adapter.lastSeen < time
                    ) && !adapter.dataSet.single
                    && commentLastVisit)
        ) {
            timeSpan.setSpan(
                RoundedBackgroundSpan(
                    Color.WHITE,
                    Palette.getColor(comment.community.name), false, mContext
                ), 0,
                timeSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        titleString.append(timeSpan)
        titleString.append(
            (if ((comment.comment.updated != null)) " (edit " + TimeUtils.getTimeAgo(
                comment.comment.updated!!.toInstant(TimeZone.UTC).toEpochMilliseconds(), mContext
            ) + ")" else "")
        )
        titleString.append("  ")
        if (comment.comment.isDistinguished) {
            val pinned = SpannableStringBuilder(
                ("\u00A0"
                        + mContext.getString(R.string.submission_stickied)
                    .uppercase(Locale.getDefault())
                        + "\u00A0")
            )
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_green_300, false),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(pinned)
            titleString.append(" ")
        }
        /*
        if (!SettingValues.hideCommentAwards && ((comment.timesSilvered > 0) || (comment.timesGilded > 0) || (comment.timesPlatinized > 0))) {
            val a = mContext.obtainStyledAttributes(
                FontPreferences(mContext).postFontStyle.resId,
                R.styleable.FontStyle
            )
            val fontsize =
                (a.getDimensionPixelSize(R.styleable.FontStyle_font_cardtitle, -1) * .75).toInt()
            a.recycle()
            // Add silver, gold, platinum icons and counts in that order
            MiscUtil.addCommentAwards(
                mContext,
                fontsize,
                titleString,
                comment.timesSilvered,
                adapter.awardIcons[0]
            )
            MiscUtil.addCommentAwards(
                mContext,
                fontsize,
                titleString,
                comment.timesGilded,
                adapter.awardIcons[1]
            )
            MiscUtil.addCommentAwards(
                mContext,
                fontsize,
                titleString,
                comment.timesPlatinized,
                adapter.awardIcons[2]
            )
        }
         */
        if (UserTags.isUserTagged(comment.creator.name)) {
            val pinned = SpannableStringBuilder(
                "\u00A0" + UserTags.getUserTag(comment.creator.name) + "\u00A0"
            )
            pinned.setSpan(
                RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_500, false),
                0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(pinned)
            titleString.append(" ")
        }
        if (UserSubscriptions.friends!!.contains(comment.creator.name)) {
            val pinned = SpannableStringBuilder(
                "\u00A0" + mContext.getString(R.string.profile_friend) + "\u00A0"
            )
            pinned.setSpan(
                RoundedBackgroundSpan(
                    mContext, android.R.color.white, R.color.md_deep_orange_500,
                    false
                ), 0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            titleString.append(pinned)
            titleString.append(" ")
        }
        /*
        if (comment.authorFlair != null && ((comment.authorFlair.text != null
                    || comment.authorFlair.cssClass != null))
        ) {
            var flairText: String? = null
            if (((comment.authorFlair != null) && (
                        comment.authorFlair.text != null) &&
                        !comment.authorFlair.text.isEmpty())
            ) {
                flairText = comment.authorFlair.text
            } else if (((comment.authorFlair != null) && (
                        comment.authorFlair.cssClass != null) &&
                        !comment.authorFlair.cssClass.isEmpty())
            ) {
                flairText = comment.authorFlair.cssClass
            }
            if (flairText != null) {
                val typedValue = TypedValue()
                val theme = mContext.theme
                theme.resolveAttribute(R.attr.activity_background, typedValue, true)
                val color = typedValue.data
                val pinned =
                    SpannableStringBuilder("\u00A0" + CompatUtil.fromHtml(flairText) + "\u00A0")
                pinned.setSpan(
                    RoundedBackgroundSpan(
                        holder.firstTextView.currentTextColor, color,
                        false, mContext
                    ), 0, pinned.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                titleString.append(pinned)
                titleString.append(" ")
            }
        }
         */
        appendToolboxNote(mContext, titleString, comment.community.name, comment.creator.name)
        if (adapter.removed.contains(comment.permalink) || ((comment.isCreatorBanned
                    && !adapter.approved.contains(comment.permalink)))
        ) {
            titleString.append(
                createRemovedLine(
                    if ((comment.isCreatorBanned)) Authentication.name else "TODO",
                    mContext
                )
            )
        } /*else if (adapter.approved.contains(comment.permalink) && !adapter.removed.contains(comment.permalink))
        ) {
            titleString.append(
                createApprovedLine(
                    if ((comment.approvedBy == null)) Authentication.name else comment.approvedBy,
                    mContext
                )
            )
        }*/
        return titleString
    }

    private fun getScoreText(comment: CommentView): Int {
        var submissionScore = comment.counts.score
        when (getVoteDirection(comment)) {
            VoteDirection.UPVOTE -> {
                if (comment.myVote != +1) {
                    if (comment.myVote == -1) ++submissionScore
                    ++submissionScore //offset the score by +1
                }
            }

            VoteDirection.DOWNVOTE -> {
                if (comment.myVote != -1) {
                    if (comment.myVote == +1) --submissionScore
                    --submissionScore //offset the score by +1
                }
            }

            VoteDirection.NO_VOTE -> if (comment.myVote == +1 && comment.creator.name
                    .equals(Authentication.name, ignoreCase = true)
            ) {
                submissionScore--
            }
        }
        return submissionScore
    }

    fun doCommentEdit(
        adapter: CommentAdapter, mContext: Context,
        fm: FragmentManager?, baseNode: CommentView, replyText: String?,
        holder: CommentViewHolder?
    ) {
        val inflater = (mContext as Activity).layoutInflater
        val dialoglayout = inflater.inflate(R.layout.edit_comment, null)
        val e = dialoglayout.findViewById<EditText>(R.id.entry)
        e.setText(StringEscapeUtils.unescapeHtml4(baseNode.comment.contentHtml))
        DoEditorActions.doActions(
            e, dialoglayout, fm!!, mContext,
            StringEscapeUtils.unescapeHtml4(replyText), null
        )
        val builder = AlertDialog.Builder(mContext)
            .setCancelable(false)
            .setView(dialoglayout)
        val d: Dialog = builder.create()
        d.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.show()
        dialoglayout.findViewById<View>(R.id.cancel)
            .setOnClickListener { d.dismiss() }
        dialoglayout.findViewById<View>(R.id.submit)
            .setOnClickListener {
                val text = e.text.toString()
                AsyncEditTask(adapter, baseNode, text, mContext, d, holder).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR
                )
            }
    }

    fun deleteComment(
        adapter: CommentAdapter, mContext: Context,
        baseNode: CommentView, holder: CommentViewHolder
    ) {
        AlertDialog.Builder(mContext)
            .setTitle(R.string.comment_delete)
            .setMessage(R.string.comment_delete_msg)
            .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                AsyncDeleteTask(adapter, baseNode, holder, mContext)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
            .setNegativeButton(
                R.string.btn_no
            ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            .show()
    }

    fun showChildrenObject(v: View) {
        v.visibility = View.VISIBLE
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 250
        animator.interpolator = FastOutSlowInInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            v.alpha = value
            v.scaleX = value
            v.scaleY = value
        }
        animator.start()
    }

    fun hideChildrenObject(v: View) {
        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = 250
        animator.interpolator = FastOutSlowInInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            v.alpha = value
            v.scaleX = value
            v.scaleY = value
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(arg0: Animator) {
                v.visibility = View.GONE
            }

            override fun onAnimationCancel(arg0: Animator) {
                v.visibility = View.GONE
            }
        })
        animator.start()
    }

    class AsyncEditTask(
        var adapter: CommentAdapter, var baseNode: CommentView, var text: String,
        var mContext: Context, var dialog: Dialog, var holder: CommentViewHolder?
    ) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try {
                /*
                AccountManager(Authentication.reddit).updateContribution(
                    baseNode,
                    text
                )
                adapter.currentSelectedItem = baseNode.comment.fullName
                val n = baseNode.notifyCommentChanged(Authentication.reddit)
                adapter.editComment(n, (holder)!!)
                dialog.dismiss()
                 */throw Exception("TODO")
            } catch (e: Exception) {
                e.printStackTrace()
                (mContext as Activity).runOnUiThread {
                    AlertDialog.Builder(mContext)
                        .setTitle(R.string.comment_delete_err)
                        .setMessage(R.string.comment_delete_err_msg)
                        .setPositiveButton(
                            R.string.btn_yes
                        ) { dialog: DialogInterface, which: Int ->
                            dialog.dismiss()
                            AsyncEditTask(
                                adapter, baseNode, text, mContext,
                                this@AsyncEditTask.dialog, holder
                            )
                                .execute()
                        }
                        .setNegativeButton(
                            R.string.btn_no
                        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                        .show()
                }
            }
            return null
        }
    }

    class AsyncDeleteTask(
        var adapter: CommentAdapter, var baseNode: CommentView,
        var holder: CommentViewHolder, var mContext: Context
    ) : AsyncTask<Void?, Void?, Boolean>() {
        override fun onPostExecute(success: Boolean) {
            if (success) {
                holder.firstTextView.setTextHtml(mContext.getString(R.string.content_deleted))
                holder.content.setText(R.string.content_deleted)
            } else {
                AlertDialog.Builder(mContext)
                    .setTitle(R.string.comment_delete_err)
                    .setMessage(R.string.comment_delete_err_msg)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface, which: Int ->
                        dialog.dismiss()
                        doInBackground()
                    }
                    .setNegativeButton(
                        R.string.btn_no
                    ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                    .show()
            }
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                /*
                ModerationManager(Authentication.reddit).delete(
                    baseNode
                )
                adapter.deleted.add(baseNode.permalink)
                true
                 */throw Exception("TODO")
            } catch (e: ApiException) {
                e.printStackTrace()
                false
            }
        }
    }

    class AsyncReportTask(private val baseNode: CommentView?, private val contextView: View) :
        AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg reason: String?): Void? {
            try {
                /*
                AccountManager(Authentication.reddit).report(
                    baseNode!!.comment, reason[0]
                )
                 */throw Exception("TODO")
            } catch (e: ApiException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            val s = Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT)
            LayoutUtils.showSnackbar(s)
        }
    }
}
