package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cocosw.bottomsheet.BottomSheet
import com.devspark.robototextview.RobotoTypefaces
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.Activities.Inbox
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.SendMessage
import me.ccrama.redditslide.Adapters.ContributionAdapter.EmptyViewHolder
import me.ccrama.redditslide.DataShare
import me.ccrama.redditslide.Drafts
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserTags
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.ClipboardUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LayoutUtils.showSnackbar
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.DoEditorActions.doActions
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.InboxManager
import net.dean.jraw.models.Captcha
import net.dean.jraw.models.Message
import net.dean.jraw.models.PrivateMessage
import java.util.Arrays

class InboxAdapter(val mContext: Context, var dataSet: InboxMessages, private val listView: RecyclerView) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IFallibleAdapter {
    private val SPACER = 6

    init {
        val isSame = false
    }

    override fun setError(b: Boolean) {
        listView.adapter = ErrorAdapter()
    }

    override fun undoSetError() {
        listView.adapter = this
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if ((position == 0 && !dataSet.posts.isNullOrEmpty()
                || ((position == dataSet.posts!!.size + 1
                ) && dataSet.nomore
                && !dataSet.where.equals("where", ignoreCase = true)))) {
            return SPACER
        } else {
            position -= 1
        }
        if ((position == dataSet.posts!!.size) && !dataSet.posts.isNullOrEmpty() && !dataSet.nomore) {
            return 5
        }
        return if ((dataSet.posts[position].subject.orEmpty().lowercase().contains("re:")
                && dataSet.where.equals("messages", ignoreCase = true))) {
            2
        } else TOP_LEVEL
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        when (i) {
            SPACER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.spacer, viewGroup, false)
                return SpacerViewHolder(v)
            }
            TOP_LEVEL -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.top_level_message, viewGroup, false)
                return MessageViewHolder(v)
            }
            5 -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.loadingmore, viewGroup, false)
                return EmptyViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.message_reply, viewGroup, false)
                return MessageViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if ((viewHolder !is EmptyViewHolder
                && viewHolder !is SpacerViewHolder)) {
            val messageViewHolder = viewHolder as MessageViewHolder
            val comment = dataSet.posts!![i]
            messageViewHolder.time.text = TimeUtils.getTimeAgo(comment.created.time, mContext)
            val titleString = SpannableStringBuilder()
            var author = comment.author
            var direction: String = "from "
            if ((!dataSet.where.contains("mod")
                    && comment.dataNode.has("dest")
                    && !Authentication.name.equals(
                    comment.dataNode["dest"].asText(), ignoreCase = true)
                    && comment.dataNode.get("dest").asText() != "reddit")) {
                author = comment.dataNode["dest"].asText().replace("#", "/c/")
                direction = "to "
            }
            if (comment.dataNode.has("subreddit") && author == null || author!!.isEmpty()) {
                direction = "via /c/" + comment.subreddit
            }
            titleString.append(direction)
            if (author != null) {
                titleString.append(author)
                titleString.append(" ")
                if (UserTags.isUserTagged(author)) {
                    val pinned = SpannableStringBuilder(" " + UserTags.getUserTag(author) + " ")
                    pinned.setSpan(
                        RoundedBackgroundSpan(mContext, android.R.color.white, R.color.md_blue_500,
                            false), 0, pinned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    titleString.append(pinned)
                    titleString.append(" ")
                }
                if (UserSubscriptions.friends!!.contains(author)) {
                    val pinned = SpannableStringBuilder(
                        " " + mContext.getString(R.string.profile_friend) + " ")
                    pinned.setSpan(RoundedBackgroundSpan(mContext, android.R.color.white,
                        R.color.md_deep_orange_500, false), 0, pinned.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    titleString.append(pinned)
                    titleString.append(" ")
                }
            }
            val spacer = mContext.getString(R.string.submission_properties_seperator)
            if (comment.dataNode.has("subreddit") && !comment.dataNode["subreddit"]
                    .isNull) {
                titleString.append(spacer)
                val subname = comment.dataNode["subreddit"].asText()
                val subreddit = SpannableStringBuilder("/c/$subname")
                if (((SettingValues.colorSubName
                        && Palette.getColor(subname) != Palette.getDefaultColor()))) {
                    subreddit.setSpan(ForegroundColorSpan(Palette.getColor(subname)), 0,
                        subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    subreddit.setSpan(StyleSpan(Typeface.BOLD), 0, subreddit.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                titleString.append(subreddit)
            }
            messageViewHolder.user.text = titleString
            val b = SpannableStringBuilder()
            if ((mContext is Inbox
                    && (comment.created.time > mContext.last
                    ) && !comment.isRead)) {
                val tagNew = SpannableStringBuilder("\u00A0NEW\u00A0")
                tagNew.setSpan(RoundedBackgroundSpan(Color.WHITE,
                    mContext.getResources().getColor(R.color.md_green_400), true, mContext), 0,
                    tagNew.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                b.append(tagNew)
                b.append(" ")
            }
            b.append(comment.subject)
            if (comment.dataNode.has("link_title")) {
                val link = SpannableStringBuilder((" "
                    + CompatUtil.fromHtml(comment.dataNode["link_title"].asText())
                    + " "))
                link.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, link.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                link.setSpan(RelativeSizeSpan(0.8f), 0, link.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                b.append(link)
            }
            messageViewHolder.title.text = b
            if (comment.isRead) {
                messageViewHolder.title.setTextColor(
                    messageViewHolder.content.currentTextColor)
            } else {
                messageViewHolder.title.setTextColor(
                    ContextCompat.getColor(mContext, R.color.md_red_400))
            }
            messageViewHolder.itemView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    val attrs = intArrayOf(R.attr.tintColor)
                    val ta = mContext.obtainStyledAttributes(attrs)
                    val color = ta.getColor(0, Color.WHITE)
                    val profile = mContext.resources.getDrawable(R.drawable.ic_account_circle)
                    val reply = mContext.resources.getDrawable(R.drawable.ic_reply)
                    val unhide = mContext.resources.getDrawable(R.drawable.ic_visibility)
                    val hide = mContext.resources.getDrawable(R.drawable.ic_visibility_off)
                    val copy = mContext.resources.getDrawable(R.drawable.ic_content_copy)
                    val reddit = mContext.resources.getDrawable(R.drawable.ic_forum)
                    val drawableSet = Arrays.asList(
                        profile, hide, copy, reddit, reply, unhide)
                    BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
                    ta.recycle()
                    val b = BottomSheet.Builder((mContext as Activity)).title(
                        CompatUtil.fromHtml(comment.subject))
                    var author = comment.author
                    if ((!dataSet.where.contains("mod")
                            && comment.dataNode.has("dest")
                            && !Authentication.name.equals(
                            comment.dataNode["dest"].asText(), ignoreCase = true)
                            && comment.dataNode.get("dest").asText() != "reddit")) {
                        author = comment.dataNode["dest"].asText().replace("#", "/c/")
                    }
                    if (comment.author != null) {
                        b.sheet(1, profile, "/u/$author")
                    }
                    var read: String = mContext.getString(R.string.mail_mark_read)
                    var rDrawable: Drawable? = hide
                    if (comment.isRead) {
                        read = mContext.getString(R.string.mail_mark_unread)
                        rDrawable = unhide
                    }
                    b.sheet(2, (rDrawable)!!, (read))
                    b.sheet(3, reply, mContext.getString(R.string.btn_reply))
                    b.sheet(25, copy, mContext.getString(R.string.misc_copy_text))
                    if (comment.isComment) {
                        b.sheet(30, reddit, mContext.getString(R.string.mail_view_full_thread))
                    }
                    val finalAuthor = author
                    b.listener(DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            1 -> {
                                val i = Intent(mContext, Profile::class.java)
                                i.putExtra(Profile.EXTRA_PROFILE, finalAuthor)
                                mContext.startActivity(i)
                            }

                            2 -> {
                                if (comment.isRead) {
                                    comment.read = false
                                    AsyncSetRead(false).execute(comment)
                                    messageViewHolder.title.setTextColor(
                                        ContextCompat.getColor(mContext,
                                            R.color.md_red_400))
                                } else {
                                    comment.read = true
                                    AsyncSetRead(true).execute(comment)
                                    messageViewHolder.title.setTextColor(
                                        messageViewHolder.content.currentTextColor)
                                }
                            }

                            3 -> {
                                doInboxReply(comment)
                            }

                            25 -> {
                                ClipboardUtil.copyToClipboard(mContext, "Message", comment.body)
                                Toast.makeText(mContext,
                                    mContext.getString(R.string.mail_message_copied),
                                    Toast.LENGTH_SHORT).show()
                            }

                            30 -> {
                                val context = comment.dataNode["context"].asText()
                                OpenRedditLink.openUrl(mContext,
                                    "https://reddit.com" + context.substring(0,
                                        context.lastIndexOf("/")), true)
                            }
                        }
                    }).show()
                    return true
                }
            })
            messageViewHolder.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (comment.isRead) {
                        if (comment is PrivateMessage) {
                            DataShare.sharedMessage = comment
                            val i = Intent(mContext, SendMessage::class.java)
                            i.putExtra(SendMessage.EXTRA_NAME, comment.getAuthor())
                            i.putExtra(SendMessage.EXTRA_REPLY, true)
                            mContext.startActivity(i)
                        } else {
                            OpenRedditLink.openUrl(mContext,
                                comment.dataNode["context"].asText(), true)
                        }
                    } else {
                        comment.read = true
                        AsyncSetRead(true).execute(comment)
                        messageViewHolder.title.setTextColor(
                            messageViewHolder.content.currentTextColor)
                        run {
                            val b: SpannableStringBuilder = SpannableStringBuilder(comment.getSubject())
                            if (comment.getDataNode().has("link_title")) {
                                val link: SpannableStringBuilder = SpannableStringBuilder(
                                    (" "
                                        + CompatUtil.fromHtml(
                                        comment.getDataNode().get("link_title").asText())
                                        + " "))
                                link.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, link.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                link.setSpan(RelativeSizeSpan(0.8f), 0, link.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                b.append(link)
                            }
                            messageViewHolder.title.setText(b)
                        }
                    }
                }
            }) //Set typeface for body
            val type = FontPreferences(mContext).fontTypeComment.typeface
            val typeface: Typeface = if (type >= 0) {
                RobotoTypefaces.obtainTypeface(mContext, type!!)
            } else {
                Typeface.DEFAULT
            }
            messageViewHolder.content.typeface = typeface
            setViews(comment.dataNode["body_html"].asText(), "FORCE_LINK_CLICK",
                messageViewHolder)
        }
        if (viewHolder is SpacerViewHolder) {
            viewHolder.itemView.findViewById<View>(R.id.height).layoutParams = LinearLayout.LayoutParams(viewHolder.itemView.width,
                (mContext as Activity).findViewById<View>(R.id.header).height)
        }
    }

    private fun doInboxReply(replyTo: Message) {
        val inflater = (mContext as Activity).layoutInflater
        val dialoglayout = inflater.inflate(R.layout.edit_comment, null)
        val e = dialoglayout.findViewById<EditText>(R.id.entry)
        doActions(e, dialoglayout,
            (mContext as AppCompatActivity).supportFragmentManager, (mContext as Activity),
            replyTo.body, null)
        val builder = AlertDialog.Builder(mContext)
            .setView(dialoglayout)
        val d: Dialog = builder.create()
        d.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.show()
        dialoglayout.findViewById<View>(R.id.cancel).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                d.dismiss()
            }
        })
        dialoglayout.findViewById<View>(R.id.submit).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val text = e.text.toString()
                AsyncReplyTask(replyTo, text).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                d.dismiss()
            }
        })
    }

    private inner class AsyncReplyTask(var replyTo: Message, var text: String) : AsyncTask<Void?, Void?, Void?>() {
        var trying: String? = null
        override fun doInBackground(vararg voids: Void?): Void? {
            sendMessage(null, null)
            return null
        }

        var sent = false
        fun sendMessage(captcha: Captcha?, captchaAttempt: String?) {
            try {
                AccountManager(Authentication.reddit).reply(replyTo,
                    text)
                sent = true
            } catch (e: ApiException) {
                sent = false
                e.printStackTrace()
            }
        }

        public override fun onPostExecute(voids: Void?) {
            val s: Snackbar
            if (sent) {
                s = Snackbar.make(listView, "Reply sent!", Snackbar.LENGTH_LONG)
                showSnackbar(s)
            } else {
                s = Snackbar.make(listView, "Sending failed! Reply saved as a draft.",
                    Snackbar.LENGTH_LONG)
                showSnackbar(s)
                Drafts.addDraft(text)
                sent = true
            }
        }
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder((itemView)!!)

    private fun setViews(rawHTML: String, subredditName: String, holder: MessageViewHolder) {
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
                holder.commentOverflow.setViews(blocks, subredditName)
            } else {
                holder.commentOverflow.setViews(blocks.subList(startIndex, blocks.size),
                    subredditName)
            }
        } else {
            holder.commentOverflow.removeAllViews()
        }
    }

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 2
        }
    }

    private class AsyncSetRead(var b: Boolean) : AsyncTask<Message?, Void?, Void?>() {
        override fun doInBackground(vararg params: Message?): Void? {
            InboxManager(Authentication.reddit).setRead(b, params[0])
            return null
        }
    }

    companion object {
        private val TOP_LEVEL = 1
    }
}
