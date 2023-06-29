package me.ccrama.redditslide.ui.settings

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.util.Consumer
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Visuals.Palette

class SettingsFilter : BaseActivityAnim() {
    var title: EditText? = null
    var text: EditText? = null
    var domain: EditText? = null
    var subreddit: EditText? = null
    var flair: EditText? = null
    var user: EditText? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings_filters)
        setupAppBar(R.id.toolbar, R.string.settings_title_filter, true, true)
        title = findViewById<View>(R.id.title) as EditText
        text = findViewById<View>(R.id.text) as EditText
        domain = findViewById<View>(R.id.domain) as EditText
        subreddit = findViewById<View>(R.id.subreddit) as EditText
        flair = findViewById<View>(R.id.flair) as EditText
        user = findViewById<View>(R.id.user) as EditText
        title!!.setOnEditorActionListener(makeOnEditorActionListener { e: String ->
            SettingValues.titleFilters += e
        })
        text!!.setOnEditorActionListener(makeOnEditorActionListener { e: String ->
            SettingValues.textFilters += e
        })
        domain!!.setOnEditorActionListener(makeOnEditorActionListener { e: String ->
            SettingValues.domainFilters += e
        })
        subreddit!!.setOnEditorActionListener(makeOnEditorActionListener { e: String ->
            SettingValues.subredditFilters += e
        })
        user!!.setOnEditorActionListener(makeOnEditorActionListener { e: String ->
            SettingValues.userFilters += e
        })
        flair!!.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = v.text.toString().lowercase().trim { it <= ' ' }
                if (text.matches(Regex(""".+:.+"""))) {
                    SettingValues.flairFilters += text
                    v.text = ""
                    updateFilters()
                }
            }
            false
        }
        updateFilters()
    }

    /**
     * Makes an OnEditorActionListener that calls filtersAdd when done is pressed
     *
     * @param filtersAdd called when done is pressed
     * @return The new OnEditorActionListener
     */
    private fun makeOnEditorActionListener(filtersAdd: Consumer<String>): OnEditorActionListener {
        return OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = v.text.toString().lowercase().trim { it <= ' ' }
                if (!text.isEmpty()) {
                    filtersAdd.accept(text)
                    v.text = ""
                    updateFilters()
                }
            }
            false
        }
    }

    /**
     * Iterate through filters and add an item for each to the layout with id, with a remove button calling filtersRemoved
     *
     * @param id            ID of linearlayout containing items
     * @param filters       Set of filters to iterate through
     * @param filtersRemove Method to call on remove button press
     */
    private fun updateList(id: Int, filters: Set<String>, filtersRemove: Consumer<String>) {
        (findViewById<View>(id) as LinearLayout).removeAllViews()
        for (s in filters) {
            val t = layoutInflater.inflate(
                R.layout.account_textview,
                findViewById<View>(id) as LinearLayout,
                false
            )
            (t.findViewById<View>(R.id.name) as TextView).text = s
            t.findViewById<View>(R.id.remove).setOnClickListener { v: View? ->
                filtersRemove.accept(s)
                updateFilters()
            }
            (findViewById<View>(id) as LinearLayout).addView(t)
        }
    }

    /**
     * Updates the filters shown in the UI
     */
    fun updateFilters() {
        updateList(
            R.id.domainlist,
            SettingValues.domainFilters
        ) { o: String -> SettingValues.domainFilters -= o }
        updateList(
            R.id.subredditlist,
            SettingValues.subredditFilters
        ) { o: String -> SettingValues.subredditFilters -= o }
        updateList(
            R.id.userlist,
            SettingValues.userFilters
        ) { o: String -> SettingValues.userFilters -= o }
        updateList(
            R.id.selftextlist,
            SettingValues.textFilters
        ) { o: String -> SettingValues.textFilters -= o }
        updateList(
            R.id.titlelist,
            SettingValues.titleFilters
        ) { o: String -> SettingValues.titleFilters -= o }
        (findViewById<View>(R.id.flairlist) as LinearLayout).removeAllViews()
        for (s in SettingValues.flairFilters) {
            val t = layoutInflater.inflate(
                R.layout.account_textview,
                findViewById<View>(R.id.domainlist) as LinearLayout,
                false
            )
            val b = SpannableStringBuilder()
            val subname = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val subreddit = SpannableStringBuilder(" /r/$subname ")
            if (SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor()) {
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
            b.append(subreddit).append(s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1])
            (t.findViewById<View>(R.id.name) as TextView).text = b
            t.findViewById<View>(R.id.remove).setOnClickListener { v: View? ->
                SettingValues.flairFilters -= s
                updateFilters()
            }
            (findViewById<View>(R.id.flairlist) as LinearLayout).addView(t)
        }
    }

    override fun onPause() {
        super.onPause()
    }
}
