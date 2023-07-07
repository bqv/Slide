package me.ccrama.redditslide.Toolbox

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.Reauthenticate
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.Toolbox.RemovalReasons.RemovalReason
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import net.dean.jraw.ApiException
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.oauth.InvalidScopeException
import net.dean.jraw.managers.InboxManager
import net.dean.jraw.managers.ModerationManager
import net.dean.jraw.models.Comment
import net.dean.jraw.models.DistinguishedStatus
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Misc UI stuff for toolbox - usernote display, removal display, etc.
 */
object ToolboxUI {
    @JvmStatic
    fun showRemoval(
        context: Context, thing: Comment?,
        callback: CompletedRemovalCallback
    ) {}

    @JvmStatic
    fun showRemoval(
        context: Context, thing: CommentView?,
        callback: CompletedRemovalCallback
    ) {}
    /**
     * Shows a removal reason dialog
     *
     * @param context Context
     * @param thing   IPost or Comment being removed
     */
    @JvmStatic
    fun showRemoval(
        context: Context, thing: IPost?,
        callback: CompletedRemovalCallback
    ) {
        val removalReasons: RemovalReasons
        val builder = MaterialDialog(context)

        // Set the dialog title
        when (thing) {
            is Comment -> {
                builder.title(text = context.resources.getString(
                    R.string.toolbox_removal_title, thing.subredditName)
                )
                removalReasons = Toolbox.getConfig(thing.subredditName).removalReasons
            }

            is IPost -> {
                builder.title(text = context.resources.getString(
                    R.string.toolbox_removal_title, thing.groupName)
                )
                removalReasons = Toolbox.getConfig(thing.groupName).removalReasons
            }

            else -> {
                return
            }
        }
        val dialogContent =
            LayoutInflater.from(context).inflate(R.layout.toolbox_removal_dialog, null)
        val headerToggle = dialogContent.findViewById<CheckBox>(R.id.toolbox_header_toggle)
        val headerText = dialogContent.findViewById<TextView>(R.id.toolbox_header_text)
        val reasonsList = dialogContent.findViewById<LinearLayout>(R.id.toolbox_reasons_list)
        val footerToggle = dialogContent.findViewById<CheckBox>(R.id.toolbox_footer_toggle)
        val footerText = dialogContent.findViewById<TextView>(R.id.toolbox_footer_text)
        val actions = dialogContent.findViewById<RadioGroup>(R.id.toolbox_action)
        val actionSticky = dialogContent.findViewById<CheckBox>(R.id.sticky_comment)
        val actionModmail = dialogContent.findViewById<CheckBox>(R.id.pm_modmail)
        val actionLock = dialogContent.findViewById<CheckBox>(R.id.lock)
        val logReason = dialogContent.findViewById<EditText>(R.id.toolbox_log_reason)

        // Check if removal should be logged and set related views
        val log = !removalReasons.logSub.isEmpty()
        if (log) {
            dialogContent.findViewById<View>(R.id.none).visibility = View.VISIBLE
            if (removalReasons.logTitle.contains("{reason}")) {
                logReason.visibility = View.VISIBLE
                logReason.setText(removalReasons.logReason)
            }
        }

        // Hide lock option if removing a comment
        if (thing is Comment) {
            actionLock.visibility = View.GONE
        }

        // Set up the header and footer options
        headerText.text = replaceTokens(removalReasons.header, thing)
        if (removalReasons.header.isEmpty()) {
            (headerToggle.parent as View).visibility = View.GONE
        }
        footerText.text = replaceTokens(removalReasons.footer, thing)
        if (removalReasons.footer.isEmpty()) {
            (footerToggle.parent as View).visibility = View.GONE
        }

        // Set up the removal reason list
        for (reason: RemovalReason in removalReasons.reasons) {
            val checkBox = CheckBox(context)
            checkBox.maxLines = 2
            checkBox.ellipsize = TextUtils.TruncateAt.END
            val tv = TypedValue()
            val found = context.theme.resolveAttribute(R.attr.fontColor, tv, true)
            checkBox.setTextColor(if (found) tv.data else Color.WHITE)
            checkBox.text = if (reason.title.isEmpty()) reason.text else reason.title
            reasonsList.addView(checkBox)
        }

        // Set default states of checkboxes/radiobuttons
        if (SettingValues.toolboxMessageType == SettingValues.ToolboxRemovalMessageType.COMMENT.ordinal) {
            (actions.findViewById<View>(R.id.comment) as RadioButton).isChecked = true
        } else if (SettingValues.toolboxMessageType == SettingValues.ToolboxRemovalMessageType.PM.ordinal) {
            (actions.findViewById<View>(R.id.pm) as RadioButton).isChecked = true
        } else if (SettingValues.toolboxMessageType == SettingValues.ToolboxRemovalMessageType.BOTH.ordinal) {
            (actions.findViewById<View>(R.id.both) as RadioButton).isChecked = true
        } else {
            (actions.findViewById<View>(R.id.none) as RadioButton).isChecked = true
        }
        actionSticky.isChecked = SettingValues.toolboxSticky
        actionModmail.isChecked = SettingValues.toolboxModmail
        actionLock.isChecked = SettingValues.toolboxLock

        // Set up dialog buttons
        builder.customView(view = dialogContent, scrollable = false)
        builder.negativeButton(R.string.btn_cancel)
        builder.positiveButton(R.string.mod_btn_remove) { dialog ->
            val removalString = StringBuilder()
            val flairText = StringBuilder()
            val flairCSS = StringBuilder()

            // Add the header to the removal message
            if (headerToggle.isChecked) {
                removalString.append(removalReasons.header)
                removalString.append("\n\n")
            }
            // Add the removal reasons
            for (i in 0 until reasonsList.childCount) {
                if ((reasonsList.getChildAt(i) as CheckBox).isChecked) {
                    removalString.append(removalReasons.reasons[i].text)
                    removalString.append("\n\n")
                    flairText.append(if (flairText.length > 0) " " else "")
                    flairText.append(removalReasons.reasons[i].flairText)
                    flairCSS.append(if (flairCSS.length > 0) " " else "")
                    flairCSS.append(removalReasons.reasons[i].flairCSS)
                }
            }
            // Add the footer
            if (footerToggle.isChecked) {
                removalString.append(removalReasons.footer)
            }
            // Add PM footer
            if (actions.checkedRadioButtonId == R.id.pm || actions.checkedRadioButtonId == R.id.both) {
                removalString.append("\n\n---\n[[Link to your {kind}]({url})]")
            }
            // Remove the item and send the message if desired
            AsyncRemoveTask(callback).execute(
                thing,  // thing
                actions.checkedRadioButtonId,  // action ID
                replaceTokens(removalString.toString(), thing),  // removal reason
                replaceTokens(removalReasons.pmSubject, thing),  // removal PM subject
                actionModmail.isChecked,  // modmail?
                actionSticky.isChecked,  // sticky?
                actionLock.isChecked,  // lock?
                log,  // log the removal?
                replaceTokens(removalReasons.logTitle, thing) // log post title
                    .replace("{reason}", logReason.text.toString()),
                removalReasons.logSub, arrayOf(flairText.toString(), flairCSS.toString())
            )
        }
        builder.show()
    }

    /**
     * Checks if a Toolbox removal dialog can be shown for a subreddit
     *
     * @param subreddit Subreddit
     * @return whether a toolbox removal dialog can be shown
     */
    @JvmStatic
    fun canShowRemoval(subreddit: String?): Boolean {
        return (SettingValues.toolboxEnabled
                && (Toolbox.getConfig(subreddit) != null
                ) && (Toolbox.getConfig(subreddit).removalReasons != null))
    }

    /**
     * Replace toolbox tokens with the appropriate replacements
     * Does NOT include log-related tokens, those must be handled after logging.
     *
     * @param reason    String to be parsed
     * @param parameter Item being acted upon
     * @return String with replacements made
     */
    fun replaceTokens(reason: String, parameter: IPost?): String {
        if (parameter is Comment) {
            val thing = parameter
            return reason.replace("{subreddit}", thing.subredditName)
                .replace("{author}", thing.author)
                .replace("{kind}", "comment")
                .replace("{mod}", (Authentication.name)!!)
                .replace("{title}", "")
                .replace(
                    "{url}", ("https://www.reddit.com"
                            + thing.dataNode["permalink"].asText())
                )
                .replace("{domain}", "")
                .replace("{link}", "undefined")
        } else if (parameter is IPost) {
            val thing = parameter
            return reason.replace("{subreddit}", thing.groupName)
                .replace("{author}", thing.creator.name)
                .replace("{kind}", "submission")
                .replace("{mod}", (Authentication.name)!!)
                .replace("{title}", thing.title)
                .replace(
                    "{url}", ("https://www.reddit.com"
                            + thing.permalink)
                )
                .replace("{domain}", thing.domain.orEmpty())
                .replace("{link}", thing.url.orEmpty())
        } else {
            throw IllegalArgumentException("Must be passed a submission or comment!")
        }
    }

    /**
     * Shows a user's usernotes in a dialog
     *
     * @param context     context
     * @param author      user to show usernotes for
     * @param subreddit   subreddit to get usernotes from
     * @param currentLink Link, in Toolbox format, for the current item - used for adding usernotes
     */
    @JvmStatic
    fun showUsernotes(context: Context, author: String?, subreddit: String?, currentLink: String?) {
        val adapter = UsernoteListAdapter(context, subreddit, author)
        AlertDialog.Builder(context)
            .setTitle(context.resources.getString(R.string.mod_usernotes_title, author))
            .setAdapter(adapter, null)
            .setNeutralButton(R.string.mod_usernotes_add) { dialog: DialogInterface?, which: Int ->
                // set up layout for add note dialog
                val layout: LinearLayout = LinearLayout(context)
                val spinner: Spinner = Spinner(context)
                val noteText: EditText = EditText(context)
                layout.addView(spinner)
                layout.addView(noteText)
                noteText.setHint(R.string.toolbox_note_text_placeholder)
                layout.setOrientation(LinearLayout.VERTICAL)
                val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                spinner.setLayoutParams(params)
                noteText.setLayoutParams(params)

                // create list of types, add default "no type" type
                val types: MutableList<CharSequence> = ArrayList()
                val defaultType: SpannableStringBuilder = SpannableStringBuilder(
                    " " + context.getString(R.string.toolbox_note_default) + " "
                )
                defaultType.setSpan(
                    BackgroundColorSpan(Color.parseColor("#808080")),
                    0, defaultType.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                defaultType.setSpan(
                    ForegroundColorSpan(Color.WHITE), 0, defaultType.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                types.add(defaultType)

                // add additional types
                val config: ToolboxConfig? = Toolbox.getConfig(subreddit)
                val typeMap: Map<String, Map<String?, String>>
                if ((config != null
                            ) && (config.getUsernoteTypes() != null
                            ) && (config.getUsernoteTypes().size > 0)
                ) {
                    typeMap = Toolbox.getConfig(subreddit).getUsernoteTypes()
                } else {
                    typeMap = Toolbox.DEFAULT_USERNOTE_TYPES
                }
                for (stringStringMap: Map<String?, String> in typeMap.values) {
                    val typeString: SpannableStringBuilder =
                        SpannableStringBuilder(" [" + stringStringMap.get("text") + "] ")
                    typeString.setSpan(
                        BackgroundColorSpan(Color.parseColor(stringStringMap.get("color"))),
                        0, typeString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    typeString.setSpan(
                        ForegroundColorSpan(Color.WHITE), 0, typeString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    types.add(typeString)
                }
                spinner.setAdapter(
                    ArrayAdapter(
                        context, android.R.layout.simple_spinner_dropdown_item,
                        types
                    )
                )

                // show add note dialog
                MaterialDialog(context).show {
                    customView(view = layout, scrollable = true)
                    noAutoDismiss()
                    positiveButton(R.string.btn_add) {dialog: MaterialDialog ->
                        if (noteText.getText().isEmpty()) {
                            noteText.setError(context.getString(R.string.toolbox_note_text_required))
                            return@positiveButton
                        }
                        val selected: Int = spinner.selectedItemPosition
                        AsyncAddUsernoteTask(context).execute(
                            subreddit,
                            author,
                            noteText.getText().toString(),
                            currentLink,
                            if (selected - 1 >= 0) typeMap.keys.toTypedArray().get(selected - 1)
                                .toString() else null
                        )
                        dialog.dismiss()
                    }
                    negativeButton(R.string.btn_cancel) { dialog1: MaterialDialog -> dialog1.dismiss() }
                }
            }
            .setPositiveButton(R.string.btn_close, null)
            .show()
    }

    /**
     * Appends a usernote to builder if a usernote in the subreddit is available, and the current
     * user has it enabled.
     *
     * @param context Android context
     * @param builder The builder to append the usernote to
     * @param subreddit The subreddit to look for notes in
     * @param user The user to look for
     */
    @JvmStatic
    fun appendToolboxNote(
        context: Context, builder: SpannableStringBuilder,
        subreddit: String?, user: String?
    ) {
        if (!SettingValues.toolboxEnabled || !Authentication.mod) {
            return
        }
        val notes = Toolbox.getUsernotes(subreddit) ?: return
        val notesForUser = notes.getNotesForUser(user)
        if (notesForUser == null || notesForUser.isEmpty()) {
            return
        }
        val noteBuilder =
            SpannableStringBuilder("\u00A0" + notes.getDisplayNoteForUser(user) + "\u00A0")
        noteBuilder.setSpan(
            RoundedBackgroundSpan(
                context.resources.getColor(android.R.color.white),
                notes.getDisplayColorForUser(user), false, context
            ), 0,
            noteBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(" ")
        builder.append(noteBuilder)
    }

    class UsernoteListAdapter(context: Context, subreddit: String?, user: String?) :
        ArrayAdapter<UsernoteListItem?>(
            context,
            R.layout.usernote_list_item,
            R.id.usernote_note_text
        ) {
        init {
            val usernotes = Toolbox.getUsernotes(subreddit)
            if (usernotes?.getNotesForUser(user) != null) {
                for (note: Usernote in usernotes.getNotesForUser(user)) {
                    val dateString = SimpleDateFormat.getDateTimeInstance(
                        SimpleDateFormat.SHORT,
                        SimpleDateFormat.SHORT
                    )
                        .format(Date(note.time))
                    val authorDateText = SpannableStringBuilder(
                        usernotes.getModNameFromModIndex(note.mod) + "\n" + dateString
                    )
                    authorDateText.setSpan(
                        RelativeSizeSpan(.92f), authorDateText.length - dateString.length,
                        authorDateText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    val noteText = SpannableStringBuilder(
                        usernotes.getWarningTextFromWarningIndex(note.warning, true)
                    )
                    noteText.setSpan(
                        ForegroundColorSpan(
                            usernotes.getColorFromWarningIndex(note.warning)
                        ),
                        0, noteText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    if (noteText.length > 0) {
                        noteText.append(" ")
                    }
                    noteText.append(note.noteText)
                    val link = note.getLinkAsURL(subreddit)
                    add(UsernoteListItem(authorDateText, noteText, link, note, subreddit, user))
                }
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val item = getItem(position)
            val authorDatetime = view.findViewById<TextView>(R.id.usernote_author_datetime)
            authorDatetime.text = item!!.authorDatetime
            val noteText = view.findViewById<TextView>(R.id.usernote_note_text)
            noteText.text = item.noteText
            view.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (item.getLink() != null) {
                        OpenRedditLink.openUrl(view.context, item.getLink(), true)
                    }
                }
            })
            view.findViewById<View>(R.id.delete).setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    AsyncRemoveUsernoteTask(item.note, context)
                        .execute(item.subreddit, item.user)
                    remove(item)
                }
            })
            return view
        }
    }

    class UsernoteListItem(
        val authorDatetime: CharSequence,
        val noteText: CharSequence,
        private val link: String,
        val note: Usernote,
        val subreddit: String?,
        val user: String?
    ) {

        fun getLink(): String? {
            return link
        }
    }

    /**
     * Removes a post/comment, optionally locking first if a post.
     * Parameters are: thing (extends IPost),
     * action ID (int),
     * removal reason (String),
     * removal subject (String),
     * modmail (boolean),
     * sticky (boolean),
     * lock (boolean),
     * log (boolean),
     * logtitle (String),
     * logsub (String)
     * flair (String[] - [text, css])
     */
    class AsyncRemoveTask(var callback: CompletedRemovalCallback) :
        AsyncTask<Any?, Void?, Boolean>() {
        /**
         * Runs the removal and necessary action(s)
         *
         * @param objects ...
         * @return Success
         */
        override fun doInBackground(vararg objects: Any?): Boolean {
            val thing = objects[0] as IPost
            val action = objects[1] as Int
            val removalString = objects[2] as String
            val pmSubject = objects[3] as String
            val modmail = objects[4] as Boolean
            val sticky = objects[5] as Boolean
            val lock = objects[6] as Boolean
            val log = objects[7] as Boolean
            val logTitle = objects[8] as String
            val logSub = objects[9] as String
            val flair = objects[10] as Array<String>
            var success = true
            var logResult: String = ""
            if (log) {
                // Log the removal
                val s = logRemoval(
                    logSub, logTitle, ("https://www.reddit.com"
                            + thing.permalink)
                )
                if (s != null) {
                    logResult = s.permalink
                } else {
                    success = false
                }
            }
            when (action) {
                R.id.comment -> success = success and postRemovalComment(
                    thing,
                    removalString.replace("{loglink}", (logResult)),
                    sticky
                )

                R.id.pm -> if (thing is Comment) {
                    success = success and sendRemovalPM(
                        if (modmail) thing.subredditName else "",
                        thing.author,
                        pmSubject.replace("{loglink}", (logResult)),
                        removalString
                    )
                } else {
                    success = success and sendRemovalPM(
                        if (modmail) (thing as IPost).groupName else "",
                        (thing as IPost).creator.name,
                        pmSubject.replace("{loglink}", (logResult)),
                        removalString
                    )
                }

                R.id.both -> {
                    success = success and postRemovalComment(
                        thing,
                        removalString.replace("{loglink}", (logResult)),
                        sticky
                    )
                    if (thing is Comment) {
                        success = success and sendRemovalPM(
                            if (modmail) thing.subredditName else "",
                            thing.author,
                            pmSubject.replace("{loglink}", (logResult)),
                            removalString
                        )
                    } else {
                        success = success and sendRemovalPM(
                            if (modmail) (thing as IPost).groupName else "",
                            (thing as IPost).creator.name,
                            pmSubject.replace("{loglink}", (logResult)),
                            removalString
                        )
                    }
                }
            }

            // Remove the item and lock/apply necessary flair
            try {
                //ModerationManager(Authentication.reddit).remove(
                //    objects[0] as IPost,
                //    false
                //)
                if (lock && thing is IPost) {
                    //ModerationManager(Authentication.reddit).setLocked(thing)
                }
                if ((flair[0].length > 0 || flair[1].length > 0) && thing is IPost) {
                    //ModerationManager(Authentication.reddit).setFlair(
                    //    thing.groupName,
                    //    thing, flair[0], flair[1]
                    //)
                }
            } catch (e: ApiException) {
                success = false
            } catch (e: NetworkException) {
                success = false
            }
            return success
        }

        /**
         * Run the callback
         *
         * @param success Whether doInBackground was a complete success
         */
        override fun onPostExecute(success: Boolean) {
            // Run the callback on the UI thread
            callback.onComplete(success)
        }

        /**
         * Send a removal PM
         *
         * @param from    empty string if from user, sub name if from sub
         * @param to      recipient
         * @param subject subject
         * @param body    body
         * @return success
         */
        private fun sendRemovalPM(
            from: String,
            to: String,
            subject: String,
            body: String
        ): Boolean {
            try {
                InboxManager(Authentication.reddit).compose(from, to, subject, body)
                return true
            } catch (e: ApiException) {
                return false
            } catch (e: NetworkException) {
                return false
            }
        }

        /**
         * Post a removal comment
         *
         * @param thing   thing to reply to
         * @param comment comment text
         * @param sticky  whether to sticky the comment
         * @return success
         */
        private fun postRemovalComment(
            thing: IPost,
            comment: String,
            sticky: Boolean
        ): Boolean {
            try {
                // Reply with a comment and get that comment's ID
                val id = null!!//AccountManager(Authentication.reddit).reply(thing, comment)

                // Sticky or distinguish the posted comment
                if (sticky) {
                    ModerationManager(Authentication.reddit)
                        .setSticky(Authentication.reddit!!["t1_$id"][0] as Comment, true)
                } else {
                    ModerationManager(Authentication.reddit).setDistinguishedStatus(
                        Authentication.reddit!!["t1_$id"][0], DistinguishedStatus.MODERATOR
                    )
                }
                return true
            } catch (e: ApiException) {
                return false
            } catch (e: NetworkException) {
                return false
            }
        }

        /**
         * Log a removal to a logsub
         *
         * @param logSub name of log sub
         * @param title  title of post
         * @return resulting submission
         */
        private fun logRemoval(logSub: String, title: String, link: String): IPost? {
            try {
                //return AccountManager(Authentication.reddit).submit(
                //    SubmissionBuilder(
                //        URL(link),
                //        logSub,
                //        title
                //    )
                //)
                return null
            } catch (e: MalformedURLException) {
                return null
            } catch (e: ApiException) {
                return null
            } catch (e: NetworkException) {
                return null
            }
        }

        /**
         * Convenience method to execute the task with the correct parameters
         *
         * @param thing         Thing being removed
         * @param action        Action to take
         * @param removalReason Removal reason
         * @param pmSubject     Removal PM subject
         * @param modmail       Whether to send PM as modmail
         * @param sticky        Whether to sticky removal comment
         * @param lock          Whether to lock removed thread
         * @param log           Whether to log the removal
         * @param logTitle      Log post title
         * @param logSub        Log subreddit
         * @param flair         Flair [text, CSS]
         */
        fun execute(
            thing: IPost?,
            action: Int,
            removalReason: String?,
            pmSubject: String?,
            modmail: Boolean,
            sticky: Boolean,
            lock: Boolean,
            log: Boolean,
            logTitle: String?,
            logSub: String?,
            flair: Array<String>?
        ) {
            super.execute(
                thing,
                action,
                removalReason,
                pmSubject,
                modmail,
                sticky,
                lock,
                log,
                logTitle,
                logSub,
                flair
            )
        }
    }

    /**
     * Add a usernote for a subreddit
     * Parameters are:
     * subreddit
     * user
     * note text
     * link
     * type
     */
    class AsyncAddUsernoteTask internal constructor(context: Context) :
        AsyncTask<String?, Void?, Boolean>() {
        private val contextRef: WeakReference<Context>

        init {
            contextRef = WeakReference(context)
        }

        override fun doInBackground(vararg strings: String?): Boolean {
            val reason: String
            try {
                Toolbox.downloadUsernotes(strings[0])
            } catch (e: NetworkException) {
                return false
            }
            if (Toolbox.getUsernotes(strings[0]) == null) {
                Toolbox.createUsernotes(strings[0])
                reason = "create usernotes config"
            } else {
                reason = "create new note on user " + strings[1]
            }
            Toolbox.getUsernotes(strings[0]).createNote(
                strings[1],  // user
                strings[2],  // note text
                strings[3],  // link
                System.currentTimeMillis(),  // time
                Authentication.name,  // mod
                strings[4] // type
            )
            try {
                Toolbox.uploadUsernotes(strings[0], reason)
            } catch (e: InvalidScopeException) { // we don't have wikiedit scope, need to reauth to get it
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean) {
            if (!success) {
                val context = contextRef.get() ?: return
                MaterialDialog(context).show {
                    title(R.string.toolbox_wiki_edit_reauth)
                    message(R.string.toolbox_wiki_edit_reauth_question)
                    negativeButton(R.string.misc_maybe_later)
                    positiveButton(R.string.btn_yes) { dialog1: MaterialDialog? ->
                        context.startActivity(
                            Intent(context, Reauthenticate::class.java)
                        )
                    }
                }
            }
        }
    }

    /**
     * Remove a usernote from a subreddit
     * Parameters are:
     * subreddit
     * user
     */
    class AsyncRemoveUsernoteTask internal constructor(
        private val note: Usernote,
        context: Context
    ) : AsyncTask<String?, Void?, Boolean>() {
        private val contextRef: WeakReference<Context>

        init {
            contextRef = WeakReference(context)
        }

        override fun doInBackground(vararg strings: String?): Boolean {
            try {
                Toolbox.downloadUsernotes(strings[0])
            } catch (e: NetworkException) {
                return false
            }
            Toolbox.getUsernotes(strings[0]).removeNote(strings[1], note)
            try {
                Toolbox.uploadUsernotes(
                    strings[0],
                    "delete note " + note.time + " on user " + strings[1]
                )
            } catch (e: InvalidScopeException) { // we don't have wikiedit scope, need to reauth to get it
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean) {
            if (!success) {
                val context = contextRef.get() ?: return
                AlertDialog.Builder(context)
                    .setTitle(R.string.toolbox_wiki_edit_reauth)
                    .setMessage(R.string.toolbox_wiki_edit_reauth_question)
                    .setNegativeButton(R.string.misc_maybe_later, null)
                    .setPositiveButton(R.string.btn_yes
                    ) { dialog1: DialogInterface?, which1: Int ->
                        context.startActivity(
                            Intent(context, Reauthenticate::class.java)
                        )
                    }
                    .show()
            }
        }
    }

    /**
     * A callback for code to be run on the UI thread after removal.
     */
    interface CompletedRemovalCallback {
        /**
         * Called when the removal is completed
         *
         * @param success Whether the removal and reason-sending process was 100% successful or not
         */
        fun onComplete(success: Boolean)
    }
}
