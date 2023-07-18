package me.ccrama.redditslide.Activities

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gun0912.tedbottompicker.TedBottomPicker
import gun0912.tedbottompicker.TedBottomSheetDialogFragment
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Drafts
import me.ccrama.redditslide.Flair.RichFlair
import me.ccrama.redditslide.ImgurAlbum.UploadImgur
import me.ccrama.redditslide.ImgurAlbum.UploadImgurAlbum
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.util.HttpUtil
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TitleExtractor
import me.ccrama.redditslide.util.stubs.SimpleTextWatcher
import me.ccrama.redditslide.views.CommentOverflow
import me.ccrama.redditslide.views.DoEditorActions
import me.ccrama.redditslide.views.ImageInsertEditText
import net.dean.jraw.ApiException
import net.dean.jraw.http.HttpRequest
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Subreddit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL

class Submit : BaseActivity() {
    private val sent: Boolean = false
    private val trying: String? = null
    private var URL: String? = null
    private var selectedFlairID: String? = null
    private var inboxReplies: SwitchCompat? = null
    private var image: View? = null
    private var link: View? = null
    private var self: View? = null
    var tchange: AsyncTask<Void?, Void?, Subreddit?>? = null
    private var client: OkHttpClient? = null
    private var gson: Gson? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            val text: String = (findViewById<View>(R.id.bodytext) as EditText).text.toString()
            if (text.isNotEmpty() && sent) {
                Drafts.addDraft(text)
                Toast.makeText(applicationContext, R.string.msg_save_draft, Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_submit)
        val window: Window = this.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        setupAppBar(R.id.toolbar, R.string.title_submit_post, true, true)
        inboxReplies = findViewById<View>(R.id.replies) as SwitchCompat?
        val intent: Intent = intent
        val subreddit: String? = intent.getStringExtra(EXTRA_SUBREDDIT)
        val initialBody: String? = intent.getStringExtra(EXTRA_BODY)
        self = findViewById<View>(R.id.selftext)
        val subredditText: AutoCompleteTextView =
            (findViewById<View>(R.id.subreddittext) as AutoCompleteTextView)
        image = findViewById<View>(R.id.image)
        link = findViewById<View>(R.id.url)
        image!!.visibility = View.GONE
        link!!.visibility = View.GONE
        if (subreddit != null
            && subreddit != "frontpage"
            && subreddit != "all"
            && subreddit != "friends"
            && subreddit != "mod"
            && !subreddit.contains("/m/")
            && !subreddit.contains("+")) {
            subredditText.setText(subreddit)
        }
        if (initialBody != null) {
            (self!!.findViewById<View>(R.id.bodytext) as ImageInsertEditText).setText(initialBody)
        }
        val adapter: ArrayAdapter<*> = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            UserSubscriptions.getAllSubreddits(this)
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
        subredditText.onFocusChangeListener = object : View.OnFocusChangeListener {
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
                                if (s.subredditType.name == "RESTRICTED") {
                                    subredditText.setText("")
                                    AlertDialog.Builder(this@Submit)
                                        .setTitle(R.string.err_submit_restricted)
                                        .setMessage(R.string.err_submit_restricted_text)
                                        .setPositiveButton(R.string.btn_ok, null)
                                        .show()
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
        findViewById<View>(R.id.selftextradio).setOnClickListener {
            self!!.visibility = View.VISIBLE
            image!!.visibility = View.GONE
            link!!.visibility = View.GONE
        }
        findViewById<View>(R.id.imageradio).setOnClickListener {
            self!!.visibility = View.GONE
            image!!.visibility = View.VISIBLE
            link!!.visibility = View.GONE
        }
        findViewById<View>(R.id.linkradio).setOnClickListener {
            self!!.visibility = View.GONE
            image!!.visibility = View.GONE
            link!!.visibility = View.VISIBLE
        }
        findViewById<View>(R.id.flair).setOnClickListener { showFlairChooser() }
        findViewById<View>(R.id.suggest).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                object : AsyncTask<String?, Void?, String?>() {
                    var d: Dialog? = null
                    override fun doInBackground(vararg params: String?): String? {
                        try {
                            return TitleExtractor.getPageTitle(params.get(0))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return null
                    }

                    override fun onPreExecute() {
                        d = MaterialDialog(this@Submit)
                            //.progress(true, 100)
                            .title(R.string.editor_finding_title)
                            .message(R.string.misc_please_wait)
                            .also { it.show() }
                    }

                    override fun onPostExecute(s: String?) {
                        if (s != null) {
                            (findViewById<View>(R.id.titletext) as EditText).setText(s)
                            d!!.dismiss()
                        } else {
                            d!!.dismiss()
                            AlertDialog.Builder(this@Submit)
                                .setTitle(R.string.title_not_found)
                                .setPositiveButton(R.string.btn_ok, null)
                                .show()
                        }
                    }
                }.execute((findViewById<View>(R.id.urltext) as EditText).text.toString())
            }
        })
        findViewById<View>(R.id.selImage).setOnClickListener { view: View? ->
            val tedBottomPicker: TedBottomSheetDialogFragment = TedBottomPicker.with(this@Submit)
                .setOnMultiImageSelectedListener { uris: List<Uri?> ->
                    handleImageIntent(uris)
                } //.setLayoutResource(R.layout.image_sheet_dialog)
                .setTitle("Choose a photo")
                .create()
            tedBottomPicker.show(supportFragmentManager)
            KeyboardUtil.hideKeyboard(
                this@Submit,
                findViewById<View>(R.id.bodytext).windowToken,
                0
            )
        }
        DoEditorActions.doActions(
            findViewById<EditText>(R.id.bodytext),
            findViewById(R.id.selftext), supportFragmentManager, this@Submit, null, null
        )
        if (intent.hasExtra(Intent.EXTRA_TEXT) && intent.extras!!
                .getString(Intent.EXTRA_TEXT, "").isNotEmpty() && !intent.getBooleanExtra(EXTRA_IS_SELF, false)
        ) {
            val data: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (data!!.contains("\n")) {
                (findViewById<View>(R.id.titletext) as EditText).setText(
                    data.substring(0, data.indexOf("\n"))
                )
                (findViewById<View>(R.id.urltext) as EditText).setText(
                    data.substring(data.indexOf("\n"))
                )
            } else {
                (findViewById<View>(R.id.urltext) as EditText).setText(data)
            }
            self!!.visibility = View.GONE
            image!!.visibility = View.GONE
            link!!.visibility = View.VISIBLE
            (findViewById<View>(R.id.linkradio) as RadioButton).isChecked = true
        } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            val imageUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (imageUri != null) {
                handleImageIntent(object : ArrayList<Uri?>() {
                    init {
                        add(imageUri)
                    }
                })
                self!!.visibility = View.GONE
                image!!.visibility = View.VISIBLE
                link!!.visibility = View.GONE
                (findViewById<View>(R.id.imageradio) as RadioButton).isChecked = true
            }
        }
        if (intent.hasExtra(Intent.EXTRA_SUBJECT) && intent.extras!!
                .getString(Intent.EXTRA_SUBJECT, "").isNotEmpty()
        ) {
            val data: String? = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            (findViewById<View>(R.id.titletext) as EditText).setText(data)
        }
        findViewById<View>(R.id.send).setOnClickListener {
            (findViewById<View>(R.id.send) as FloatingActionButton).hide()
            AsyncDo().execute()
        }
    }

    private fun setImage(URL: String) {
        this.URL = URL
        runOnUiThread {
            findViewById<View>(R.id.imagepost).visibility = View.VISIBLE
            (application as App).imageLoader!!
                .displayImage(URL, (findViewById<View>(R.id.imagepost) as ImageView?))
        }
    }

    private fun showFlairChooser() {
        client = App.client
        gson = Gson()
        val subreddit: String =
            (findViewById<View>(R.id.subreddittext) as EditText).text.toString()
        val d: Dialog = MaterialDialog(this@Submit)
            .title(R.string.submit_findingflairs)
            .cancelable(true)
            .message(R.string.misc_please_wait)
            //.progress(true, 100)
            .also { it.show() }
        object : AsyncTask<Void?, Void?, JsonArray?>() {
            var flairs: ArrayList<JsonObject>? = null
            override fun doInBackground(vararg params: Void?): JsonArray? {
                flairs = ArrayList()
                val r: HttpRequest = Authentication.reddit!!.request()
                    .path("/r/$subreddit/api/link_flair_v2.json")
                    .get()
                    .build()
                val request: Request = Request.Builder()
                    .headers(r.headers.newBuilder().set("User-Agent", "Slide flair search").build())
                    .url(r.url)
                    .build()
                return HttpUtil.getJsonArray(client, gson, request)
            }

            override fun onPostExecute(result: JsonArray?) {
                if (result == null) {
                    LogUtil.v("Error loading content")
                    d.dismiss()
                } else {
                    try {
                        val flairs: HashMap<String, RichFlair> = HashMap<String, RichFlair>()
                        for (obj: JsonElement in result) {
                            val choice: RichFlair = ObjectMapper().disable(
                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                            ).readValue(obj.toString(), RichFlair::class.java)
                            val title: String = choice.text
                            flairs[title] = choice
                        }
                        d.dismiss()
                        val allKeys: ArrayList<String> = ArrayList(flairs.keys)
                        MaterialDialog(this@Submit)
                            .title(text = getString(R.string.submit_flairchoices, subreddit))
                            .listItems(items = allKeys) { dialog: MaterialDialog?, which: Int, text: CharSequence? ->
                                val selected: RichFlair? = flairs[allKeys.get(which)]
                                selectedFlairID = selected!!.getId()
                                (findViewById<View>(R.id.flair) as TextView).text = getString(
                                    R.string.submit_selected_flair,
                                    selected!!.getText()
                                )
                            }
                            .show()
                    } catch (e: Exception) {
                        LogUtil.v(e.toString())
                        d.dismiss()
                        LogUtil.v("Error parsing flairs")
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun setViews(
        rawHTML: String, subredditName: String?, firstTextView: SpoilerRobotoTextView,
        commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks: List<String> = SubmissionParser.getBlocks(rawHTML)
        var startIndex: Int = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (!(blocks.get(0) == "<div class=\"md\">")) {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks.get(0) + " ", subredditName)
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

    fun handleImageIntent(uris: List<Uri?>) {
        if (uris.size == 1) {
            // Get the Image from data (single image)
            try {
                UploadImgurSubmit(this, uris[0])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            //Multiple images
            try {
                UploadImgurAlbumSubmit(this, *uris.filterNotNull().toTypedArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class AsyncDo : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg voids: Void?): Void? {
            try {
                if (self!!.visibility == View.VISIBLE) {
                    val text: String =
                        (findViewById<View>(R.id.bodytext) as EditText).text.toString()
                    try {
                        val builder: AccountManager.SubmissionBuilder =
                            AccountManager.SubmissionBuilder(
                                (findViewById<View>(R.id.bodytext) as EditText).text
                                    .toString(), (findViewById<View>(
                                    R.id.subreddittext
                                ) as AutoCompleteTextView).text.toString(),
                                (findViewById<View>(R.id.titletext) as EditText).text
                                    .toString()
                            )
                        if (selectedFlairID != null) {
                            builder.flairID(selectedFlairID)
                        }
                        val s: Submission = AccountManager(Authentication.reddit).submit(builder)
                        AccountManager(Authentication.reddit).sendRepliesToInbox(
                            s,
                            inboxReplies!!.isChecked()
                        )
                        OpenRedditLink.openUrl(
                            this@Submit,
                            "reddit.com/r/" + (findViewById<View>(
                                R.id.subreddittext
                            ) as AutoCompleteTextView).text.toString() + "/comments/" + s
                                .fullName
                                .substring(3), true
                        )
                        this@Submit.finish()
                    } catch (e: ApiException) {
                        Drafts.addDraft(text)
                        e.printStackTrace()
                        runOnUiThread {
                            showErrorRetryDialog(
                                (getString(R.string.misc_err)
                                        + ": "
                                        + e.explanation
                                        + "\n"
                                        + getString(R.string.misc_retry_draft))
                            )
                        }
                    }
                } else if (link!!.visibility == View.VISIBLE) {
                    try {
                        val s: Submission = AccountManager(Authentication.reddit).submit(
                            AccountManager.SubmissionBuilder(
                                URL(
                                    (findViewById<View>(R.id.urltext) as EditText).text
                                        .toString()
                                ), (findViewById<View>(
                                    R.id.subreddittext
                                ) as AutoCompleteTextView).text.toString(),
                                (findViewById<View>(R.id.titletext) as EditText).text
                                    .toString()
                            )
                        )
                        AccountManager(Authentication.reddit).sendRepliesToInbox(
                            s,
                            inboxReplies!!.isChecked()
                        )
                        OpenRedditLink.openUrl(
                            this@Submit,
                            "reddit.com/r/" + (findViewById<View>(
                                R.id.subreddittext
                            ) as AutoCompleteTextView).text.toString() + "/comments/" + s
                                .fullName
                                .substring(3), true
                        )
                        this@Submit.finish()
                    } catch (e: ApiException) {
                        e.printStackTrace()
                        runOnUiThread {
                            if (e is ApiException) {
                                showErrorRetryDialog(
                                    (getString(R.string.misc_err)
                                            + ": "
                                            + e.explanation
                                            + "\n"
                                            + getString(R.string.misc_retry))
                                )
                            } else {
                                showErrorRetryDialog(
                                    getString(R.string.misc_err) + ": " + getString(
                                        R.string.err_invalid_url
                                    ) + "\n" + getString(
                                        R.string.misc_retry
                                    )
                                )
                            }
                        }
                    }
                } else if (image!!.visibility == View.VISIBLE) {
                    try {
                        val s: Submission = AccountManager(Authentication.reddit).submit(
                            AccountManager.SubmissionBuilder(
                                URL(URL),
                                (findViewById<View>(
                                    R.id.subreddittext
                                ) as AutoCompleteTextView).text.toString(),
                                (findViewById<View>(R.id.titletext) as EditText).text
                                    .toString()
                            )
                        )
                        AccountManager(Authentication.reddit).sendRepliesToInbox(
                            s,
                            inboxReplies!!.isChecked()
                        )
                        OpenRedditLink.openUrl(
                            this@Submit,
                            "reddit.com/r/" + (findViewById<View>(
                                R.id.subreddittext
                            ) as AutoCompleteTextView).text.toString() + "/comments/" + s
                                .fullName
                                .substring(3), true
                        )
                        this@Submit.finish()
                    } catch (e: Exception) {
                        runOnUiThread {
                            if (e is ApiException) {
                                showErrorRetryDialog(
                                    getString(R.string.misc_err) + ": " + e
                                        .explanation + "\n" + getString(
                                        R.string.misc_retry
                                    )
                                )
                            } else {
                                showErrorRetryDialog(
                                    getString(R.string.misc_err) + ": " + getString(
                                        R.string.err_invalid_url
                                    ) + "\n" + getString(
                                        R.string.misc_retry
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { showErrorRetryDialog(getString(R.string.misc_retry)) }
            }
            return null
        }
    }

    private inner class UploadImgurSubmit constructor(c: Context, u: Uri?) : UploadImgur() {
        private val uri: Uri?

        init {
            this.c = c
            this.uri = u
            dialog = MaterialDialog(c)
                .title(res = R.string.editor_uploading_image)
                //.progress(false, 100)
                .cancelable(false)
                .noAutoDismiss()
            MaterialDialog(c)
                .title(R.string.editor_upload_image_question)
                .cancelable(false)
                .noAutoDismiss()
                .positiveButton(R.string.btn_upload) { d: MaterialDialog ->
                    d.dismiss()
                    dialog!!.show()
                    execute(uri)
                }
                .negativeButton(R.string.btn_cancel) { d: MaterialDialog ->
                    d.dismiss()
                }
                .show()
        }

        override fun onPostExecute(result: JSONObject?) {
            dialog!!.dismiss()
            try {
                val url: String = result!!.getJSONObject("data").getString("link")
                setImage(url)
            } catch (e: Exception) {
                AlertDialog.Builder(c!!)
                    .setTitle(R.string.err_title)
                    .setMessage(R.string.editor_err_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private inner class UploadImgurAlbumSubmit constructor(c: Context, vararg u: Uri) : UploadImgurAlbum() {
        private val uris: Array<out Uri>

        init {
            this.c = c
            this.uris = u
            dialog = MaterialDialog(c)
                .title(R.string.editor_uploading_image)
                //.progress(false, 100)
                .cancelable(false)
            MaterialDialog(c)
                .title(R.string.editor_upload_image_question)
                .cancelable(false)
                .noAutoDismiss()
                .positiveButton(R.string.btn_upload) { d: MaterialDialog ->
                    d.dismiss()
                    dialog!!.show()
                    execute(*uris)
                }
                .negativeButton(R.string.btn_cancel) { d: MaterialDialog ->
                    d.dismiss()
                }
                .show()
        }

        override fun onPostExecute(result: String?) {
            dialog!!.dismiss()
            try {
                (findViewById<View>(R.id.linkradio) as RadioButton).isChecked = true
                link!!.visibility = View.VISIBLE
                (findViewById<View>(R.id.urltext) as EditText).setText(finalUrl)
            } catch (e: Exception) {
                AlertDialog.Builder(c!!)
                    .setTitle(R.string.err_title)
                    .setMessage(R.string.editor_err_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private fun showErrorRetryDialog(message: String) {
        AlertDialog.Builder(this@Submit)
            .setTitle(R.string.err_title)
            .setMessage(message)
            .setNegativeButton(
                R.string.btn_no,
                ({ dialogInterface: DialogInterface?, i: Int -> finish() })
            )
            .setPositiveButton(
                R.string.btn_yes,
                ({ dialogInterface: DialogInterface?, i: Int ->
                    (findViewById<View>(
                        R.id.send
                    ) as FloatingActionButton).show()
                })
            )
            .setOnDismissListener(({ dialog: DialogInterface? ->
                (findViewById<View>(
                    R.id.send
                ) as FloatingActionButton).show()
            }))
            .create()
            .show()
    }

    companion object {
        @JvmField
        val EXTRA_SUBREDDIT: String = "subreddit"
        @JvmField
        val EXTRA_BODY: String = "body"
        @JvmField
        val EXTRA_IS_SELF: String = "is_self"
    }
}
