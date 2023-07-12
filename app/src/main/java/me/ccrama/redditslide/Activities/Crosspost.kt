package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.network.data.IPost
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.stubs.SimpleTextWatcher
import me.ccrama.redditslide.views.CommentOverflow
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.Subreddit

class Crosspost : BaseActivity() {
    private var inboxReplies: SwitchCompat? = null
    var tchange: AsyncTask<Void?, Void?, Subreddit?>? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_crosspost)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = this.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
        setupAppBar(R.id.toolbar, R.string.title_crosspost, true, true)
        inboxReplies = findViewById<View>(R.id.replies) as SwitchCompat
        val subredditText = (findViewById<View>(R.id.subreddittext) as AutoCompleteTextView)
        (findViewById<View>(R.id.crossposttext) as EditText).setText(
            (toCrosspost!!.title
                    + getString(R.string.submission_properties_seperator)
                    + "/u/"
                    + toCrosspost!!.user.name)
        )
        findViewById<View>(R.id.crossposttext).isEnabled = false
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            UserSubscriptions.getAllSubreddits(this) as List<Any>
        )
        subredditText.setAdapter(adapter)
        subredditText.threshold = 2
        subredditText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (tchange != null) {
                    tchange!!.cancel(true)
                }
                findViewById<View>(R.id.submittext).visibility = View.GONE
            }
        })
        subredditText.onFocusChangeListener = object : OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                findViewById<View>(R.id.submittext).visibility = View.GONE
                if (!hasFocus) {
                    tchange = object : AsyncTask<Void?, Void?, Subreddit?>() {
                        override fun doInBackground(vararg params: Void?): Subreddit? {
                            try {
                                return Authentication.reddit!!.getSubreddit(
                                    subredditText.text.toString()
                                )
                            } catch (ignored: Exception) {
                            }
                            return null
                        }

                        override fun onPostExecute(s: Subreddit?) {
                            if (s != null) {
                                val text: String? = s.dataNode.get("submit_text_html").asText()
                                if (!text.isNullOrEmpty() && text != "null") {
                                    findViewById<View>(R.id.submittext).visibility = View.VISIBLE
                                    setViews(
                                        text, subredditText.text.toString(),
                                        findViewById<View>(R.id.submittext) as SpoilerRobotoTextView,
                                        findViewById<View>(R.id.commentOverflow) as CommentOverflow
                                    )
                                }
                            } else {
                                findViewById<View>(R.id.submittext).visibility = View.GONE
                            }
                        }
                    }
                    tchange!!.execute()
                }
            }
        }
        (findViewById<View>(R.id.titletext) as EditText).setText(
            toCrosspost!!.title
        )
        findViewById<View>(R.id.suggest).setOnClickListener(View.OnClickListener {
            (findViewById<View>(R.id.titletext) as EditText).setText(
                toCrosspost!!.title
            )
        })
        findViewById<View>(R.id.send).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                (findViewById<View>(R.id.send) as FloatingActionButton).hide()
                AsyncDo().execute()
            }
        })
    }

    fun setViews(
        rawHTML: String, subredditName: String?, firstTextView: SpoilerRobotoTextView,
        commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks.get(0) != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0] + " ", subredditName)
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subredditName)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    private inner class AsyncDo() : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg voids: Void?): Void? {
            try {
                try {
                    val s = AccountManager(Authentication.reddit).crosspost(
                        toCrosspost!!.submission,
                        (findViewById<View>(R.id.subreddittext) as AutoCompleteTextView).text
                            .toString(),
                        (findViewById<View>(R.id.titletext) as EditText).text.toString(), null,
                        ""
                    )
                    AccountManager(Authentication.reddit).sendRepliesToInbox(
                        s,
                        inboxReplies!!.isChecked
                    )
                    OpenRedditLink.openUrl(
                        this@Crosspost,
                        ("reddit.com/r/"
                                + (findViewById<View>(
                            R.id.subreddittext
                        ) as AutoCompleteTextView).text.toString()
                                + "/comments/"
                                + s.fullName.substring(3)), true
                    )
                    finish()
                } catch (e: ApiException) {
                    e.printStackTrace()
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            showErrorRetryDialog(
                                (getString(R.string.misc_err)
                                        + ": "
                                        + e.explanation
                                        + "\n"
                                        + getString(R.string.misc_retry))
                            )
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread(object : Runnable {
                    override fun run() {
                        showErrorRetryDialog(getString(R.string.misc_retry))
                    }
                })
            }
            return null
        }
    }

    private fun showErrorRetryDialog(message: String) {
        AlertDialog.Builder(this@Crosspost)
            .setTitle(R.string.err_title)
            .setMessage(message)
            .setNegativeButton(
                R.string.btn_no,
                { dialogInterface: DialogInterface?, i: Int -> finish() })
            .setPositiveButton(
                R.string.btn_yes,
                { dialogInterface: DialogInterface?, i: Int -> (findViewById<View>(R.id.send) as FloatingActionButton).show() })
            .setOnDismissListener({ dialog: DialogInterface? -> (findViewById<View>(R.id.send) as FloatingActionButton).show() })
            .create()
            .show()
    }

    companion object {
        var toCrosspost: IPost? = null
    }
}
