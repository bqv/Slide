package me.ccrama.redditslide.Activities

import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.DataShare
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.views.DoEditorActions.doActions
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.InboxManager
import net.dean.jraw.models.Captcha
import net.dean.jraw.models.PrivateMessage

class SendMessage : BaseActivity() {
    var URL: String? = null
    private var reply: Boolean? = null
    private var previousMessage: PrivateMessage? = null
    private var subject: EditText? = null
    private var to: EditText? = null
    private var bodytext: String? = null
    private var subjecttext: String? = null
    private var totext: String? = null
    private var body: EditText? = null
    private var messageSentStatus //the String to show in the Toast for when the message is sent
            : String? = null
    private var messageSent = true //whether or not the message was sent successfully
    var author: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_sendmessage)
        val b = findViewById<View>(R.id.toolbar) as Toolbar
        val name: String
        reply = intent != null && intent.hasExtra(EXTRA_REPLY)
        subject = findViewById<View>(R.id.subject) as EditText
        to = findViewById<View>(R.id.to) as EditText
        body = findViewById<View>(R.id.body) as EditText
        val oldMSG = findViewById<View>(R.id.oldMSG)
        val sendingAs = findViewById<View>(R.id.sendas) as TextView
        sendingAs.text = "Sending as /u/" + Authentication.name
        author = Authentication.name
        sendingAs.setOnClickListener {
            val items = ArrayList<String>()
            items.add("/u/" + Authentication.name)
            if (UserSubscriptions.modOf != null && !UserSubscriptions.modOf!!.isEmpty()) for (s in UserSubscriptions.modOf!!) {
                items.add("/r/$s")
            }
            MaterialDialog(this@SendMessage)
                .title(text = "Send message as")
                .listItems(items = items) { dialog: MaterialDialog?, which: Int, text: CharSequence? ->
                    author = text as String?
                    sendingAs.text = "Sending as $author"
                }
                .negativeButton(R.string.btn_cancel) { }
                .show()
        }
        if (intent != null && intent.hasExtra(EXTRA_NAME)) {
            name = intent.extras!!.getString(EXTRA_NAME, "")
            to!!.setText(name)
            to!!.inputType = InputType.TYPE_NULL
            if (reply!!) {
                b.title = getString(R.string.mail_reply_to, name)
                previousMessage = DataShare.sharedMessage
                if (previousMessage!!.subject != null) subject!!.setText(
                    getString(
                        R.string.mail_re,
                        previousMessage!!.subject
                    )
                )
                subject!!.inputType = InputType.TYPE_NULL

                //Disable if replying to another user, as they are already set
                to!!.isEnabled = false
                subject!!.isEnabled = false
                body!!.requestFocus()
                oldMSG.setOnClickListener {
                    AlertDialog.Builder(this@SendMessage)
                        .setTitle(getString(R.string.mail_author_wrote, name))
                        .setMessage(previousMessage!!.body)
                        .create()
                        .show()
                }
            } else {
                b.title = getString(R.string.mail_send_to, name)
                oldMSG.visibility = View.GONE
            }
        } else {
            name = ""
            oldMSG.visibility = View.GONE
            b.setTitle(R.string.mail_send)
        }
        if (intent.hasExtra(EXTRA_MESSAGE)) {
            body!!.setText(intent.getStringExtra(EXTRA_MESSAGE))
        }
        if (intent.hasExtra(EXTRA_SUBJECT)) {
            subject!!.setText(intent.getStringExtra(EXTRA_SUBJECT))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = this.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
        setupUserAppBar(R.id.toolbar, null, true, name)
        setRecentBar(b.title.toString(), Palette.getDefaultColor())
        if (reply!! || UserSubscriptions.modOf == null || UserSubscriptions.modOf!!.isEmpty()) {
            sendingAs.visibility = View.GONE
        }
        findViewById<View>(R.id.send).setOnClickListener {
            bodytext = body!!.text.toString()
            totext = to!!.text.toString()
            subjecttext = subject!!.text.toString()
            (findViewById<View>(R.id.send) as FloatingActionButton).hide()
            AsyncDo(null, null).execute()
        }
        doActions(
            (findViewById<View>(R.id.body) as EditText),
            findViewById(R.id.area),
            supportFragmentManager,
            this@SendMessage,
            if (previousMessage == null) null else previousMessage!!.body,
            null
        )
    }

    private inner class AsyncDo(var captcha: Captcha?, var tried: String?) :
        AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg voids: Void?): Void? {
            sendMessage(captcha, tried)
            return null
        }

        fun sendMessage(captcha: Captcha?, captchaAttempt: String?) {
            if (reply!!) {
                try {
                    AccountManager(Authentication.reddit).reply(previousMessage, bodytext)
                } catch (e: ApiException) {
                    messageSent = false
                    e.printStackTrace()
                }
            } else {
                try {
                    if (captcha != null) InboxManager(Authentication.reddit).compose(
                        totext,
                        subjecttext,
                        bodytext,
                        captcha,
                        captchaAttempt
                    ) else {
                        var to = author
                        if (to!!.startsWith("/r/")) {
                            to = to.substring(3)
                            InboxManager(Authentication.reddit).compose(
                                to, totext, subjecttext,
                                bodytext
                            )
                        } else {
                            InboxManager(Authentication.reddit).compose(
                                totext, subjecttext,
                                bodytext
                            )
                        }
                    }
                } catch (e: ApiException) {
                    messageSent = false
                    e.printStackTrace()

                    //Display a Toast with an error if the user doesn't exist
                    if (e.reason == "USER_DOESNT_EXIST" || e.reason == "NO_USER") {
                        messageSentStatus = getString(R.string.msg_send_user_dne)
                    } else if (e.reason.lowercase().contains("captcha")) {
                        messageSentStatus = getString(R.string.misc_captcha_incorrect)
                    }

                    //todo show captcha
                }
            }
        }

        public override fun onPostExecute(voids: Void?) {
            //If the error wasn't that the user doesn't exist, show a generic failure message
            if (messageSentStatus == null) {
                messageSentStatus = getString(R.string.msg_sent_failure)
                (findViewById<View>(R.id.send) as FloatingActionButton).show()
            }
            val MESSAGE_SENT =
                if (messageSent) getString(R.string.msg_sent_success) else messageSentStatus!!
            Toast.makeText(this@SendMessage, MESSAGE_SENT, Toast.LENGTH_SHORT).show()

            //Only finish() this Activity if the message sent successfully
            if (messageSent) {
                finish()
            } else {
                (findViewById<View>(R.id.send) as FloatingActionButton).show()
                messageSent = true
            }
        }
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_REPLY = "reply"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SUBJECT = "subject"
    }
}
