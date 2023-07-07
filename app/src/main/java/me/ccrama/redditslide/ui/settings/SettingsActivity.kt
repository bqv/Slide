package me.ccrama.redditslide.ui.settings

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.common.base.Strings
import dagger.hilt.android.AndroidEntryPoint
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.ui.settings.dragSort.ReorderSubreddits
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.stubs.SimpleTextWatcher
import java.io.File
import java.util.Arrays
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), RestartActivity {
    @Inject
    lateinit var postRepository: PostRepository
    @Inject
    lateinit var commentRepository: CommentRepository

    private var scrollY = 0
    private var prev_text: String? = null
    private val mSettingsGeneralFragment: SettingsGeneralFragment =
        SettingsGeneralFragment(this)
    private val mManageOfflineContentFragment = ManageOfflineContentFragment(this)
    private val mSettingsThemeFragment: SettingsThemeFragment<*> = SettingsThemeFragment(this)
    private val mSettingsFontFragment = SettingsFontFragment(this)
    private val mSettingsCommentsFragment = SettingsCommentsFragment(this)
    private val mSettingsHandlingFragment = SettingsHandlingFragment(this)
    private val mSettingsHistoryFragment = SettingsHistoryFragment(this)
    private val mSettingsDataFragment = SettingsDataFragment(this)
    private val mSettingsRedditFragment = SettingsRedditFragment(this)
    private val settings_activities: List<Int> = ArrayList(
        Arrays.asList(
            R.layout.activity_settings_general_child,
            R.layout.activity_manage_history_child,
            R.layout.activity_settings_theme_child,
            R.layout.activity_settings_font_child,
            R.layout.activity_settings_comments_child,
            R.layout.activity_settings_handling_child,
            R.layout.activity_settings_history_child,
            R.layout.activity_settings_datasaving_child,
            R.layout.activity_settings_reddit_child
        )
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESTART_SETTINGS_RESULT) {
            restartActivity()
        }
    }

    override fun restartActivity() {
        val i = Intent(this@SettingsActivity, SettingsActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        i.putExtra("position", scrollY)
        i.putExtra("prev_text", prev_text)
        startActivity(i)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (findViewById<View>(R.id.settings_search).visibility == View.VISIBLE) {
                    findViewById<View>(R.id.settings_search).visibility = View.GONE
                    findViewById<View>(R.id.search).visibility = View.VISIBLE
                } else {
                    onBackPressed()
                }
                return true
            }

            R.id.search -> {
                run {
                    findViewById<View>(R.id.settings_search).visibility = View.VISIBLE
                    findViewById<View>(R.id.search).visibility = View.GONE
                }
                return true
            }

            else -> return false
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_settings)
        setupAppBar(R.id.toolbar, R.string.title_settings, true, true)
        if (intent != null && !Strings.isNullOrEmpty(intent.getStringExtra("prev_text"))) {
            prev_text = intent.getStringExtra("prev_text")
        } else if (savedInstanceState != null) {
            prev_text = savedInstanceState.getString("prev_text")
        }
        if (!Strings.isNullOrEmpty(prev_text)) {
            (findViewById<View>(R.id.settings_search) as EditText).setText(prev_text)
        }
        BuildLayout(prev_text)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("position", scrollY)
        outState.putString("prev_text", prev_text)
        super.onSaveInstanceState(outState)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SEARCH
            && event.action == KeyEvent.ACTION_DOWN
        ) {
            onOptionsItemSelected(mToolbar!!.menu.findItem(R.id.search))
            //            (findViewById(R.id.settings_search)).requestFocus();
            val motionEventDown = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                0f, 0f, 0
            )
            val motionEventUp = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                0f, 0f, 0
            )
            findViewById<View>(R.id.settings_search).dispatchTouchEvent(motionEventDown)
            findViewById<View>(R.id.settings_search).dispatchTouchEvent(motionEventUp)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun BuildLayout(text: String?) {
        val parent = findViewById<View>(R.id.settings_parent) as LinearLayout

        /* Clear the settings out, then re-add the default top-level settings */parent.removeAllViews()
        parent.addView(layoutInflater.inflate(R.layout.activity_settings_child, null))
        Bind()

        /* The EditView contains text that we can use to search for matching settings */if (!Strings.isNullOrEmpty(
                text
            )
        ) {
            val inflater = layoutInflater
            for (activity in settings_activities) {
                parent.addView(inflater.inflate(activity, null))
            }
            mSettingsGeneralFragment.Bind()
            mManageOfflineContentFragment.Bind()
            mSettingsThemeFragment.Bind()
            mSettingsFontFragment.Bind()
            mSettingsCommentsFragment.Bind()
            mSettingsHandlingFragment.Bind()
            mSettingsHistoryFragment.Bind()
            mSettingsDataFragment.Bind()
            mSettingsRedditFragment.Bind()

            /* Go through each subview and scan it for matching text, non-matches */loopViews(
                parent, text!!.lowercase(
                    Locale.getDefault()
                ), true, ""
            )
        }

        /* Try to clean up the mess we've made */System.gc()
    }

    private fun Bind() {
        SettingValues.expandedSettings = true
        setSettingItems()
        val mScrollView = findViewById<View>(R.id.base) as ScrollView
        SettingValues.setListener { sharedPreferences: SharedPreferences?, key: String? ->
            changed = true
        }
        mScrollView.post {
            val observer = mScrollView.viewTreeObserver
            if (intent.hasExtra("position")) {
                mScrollView.scrollTo(0, intent.getIntExtra("position", 0))
            }
            if (intent.hasExtra("prev_text")) {
                prev_text = intent.getStringExtra("prev_text")
            }
            observer.addOnScrollChangedListener { scrollY = mScrollView.scrollY }
        }
    }

    private fun loopViews(
        parent: ViewGroup,
        text: String,
        isRootViewGroup: Boolean,
        indent: String
    ): Boolean {
        var foundText = false
        var prev_child_is_View = false
        var i = 0
        while (i < parent.childCount) {
            val child = parent.getChildAt(i)
            var childRemoved = false

            /* Found some text, remove labels and check for matches on non-labels */if (child is TextView) {

                // Found text at the top-level that is probably a label, or an explicitly tagged label
                if (isRootViewGroup || (child.getTag()?.toString() == "label")) {
                    parent.removeView(child)
                    childRemoved = true
                    i--
                } else if (child.text.toString().lowercase(Locale.getDefault()).contains(text)) {
                    foundText = true
                }

                // No match
            } else if (child != null && prev_child_is_View && child.javaClass == View::class.java) {
                parent.removeView(child)
                childRemoved = true
                i--
            } else if (child is ViewGroup) {
                // Look for matching TextView in the ViewGroup, remove the ViewGroup if no match is found
                if (!loopViews(child, text, false, "$indent  ")) {
                    parent.removeView(child)
                    childRemoved = true
                    i--
                } else {
                    foundText = true
                }
            }
            if (child != null && !childRemoved) {
                prev_child_is_View = child.javaClass == View::class.java
            }
            i++
        }
        return foundText
    }

    private fun setSettingItems() {
        val pro = findViewById<View>(R.id.settings_child_pro)
        if (SettingValues.isPro) {
            pro.visibility = View.GONE
        } else {
            pro.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    ProUtil.proUpgradeMsg(this@SettingsActivity, R.string.settings_support_slide)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                        .show()
                }
            })
        }
        (findViewById<View>(R.id.settings_search) as EditText).addTextChangedListener(object :
            SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val text = s.toString().trim { it <= ' ' }
                /* No idea why, but this event can fire many times when there is no change */if (text.equals(
                        prev_text,
                        ignoreCase = true
                    )
                ) return
                BuildLayout(text)
                prev_text = text
            }
        })
        findViewById<View>(R.id.settings_child_general).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsGeneral::class.java)
                startActivityForResult(i, RESTART_SETTINGS_RESULT)
            }
        })
        findViewById<View>(R.id.settings_child_history).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsHistory::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_about).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsAbout::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_offline).setOnClickListener {
            val i = Intent(this@SettingsActivity, ManageOfflineContent::class.java)
            startActivity(i)
        }
        findViewById<View>(R.id.settings_child_datasave).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsData::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_subtheme).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsSubreddit::class.java)
                startActivityForResult(i, RESTART_SETTINGS_RESULT)
            }
        })
        findViewById<View>(R.id.settings_child_filter).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsFilter::class.java)
                startActivity(i)
            }
        })
        if (BuildConfig.DEBUG) {
            findViewById<View>(R.id.settings_child_synccit).setOnClickListener(object :
                OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    throw RuntimeException("Test error")
                }
            })
        } else {
            findViewById<View>(R.id.settings_child_synccit).visibility = View.GONE
        }
        findViewById<View>(R.id.settings_child_reorder).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val inte = Intent(this@SettingsActivity, ReorderSubreddits::class.java)
                startActivity(inte)
            }
        })
        findViewById<View>(R.id.settings_child_maintheme).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsTheme::class.java)
                startActivityForResult(i, RESTART_SETTINGS_RESULT)
            }
        })
        findViewById<View>(R.id.settings_child_handling).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsHandling::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_layout).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, EditCardsLayout::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_backup).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsBackup::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_font).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val i = Intent(this@SettingsActivity, SettingsFont::class.java)
                startActivity(i)
            }
        })
        findViewById<View>(R.id.settings_child_tablet).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                /*  Intent inte = new Intent(Overview.this, Overview.class);
                    inte.putExtra("type", UpdateSubreddits.COLLECTIONS);
                    Overview.this.startActivity(inte);*/
                if (SettingValues.isPro) {
                    val inflater = layoutInflater
                    val dialoglayout = inflater.inflate(R.layout.tabletui, null)
                    val res = resources
                    dialoglayout.findViewById<View>(R.id.title)
                        .setBackgroundColor(Palette.getDefaultColor())
                    //todo final Slider portrait = (Slider) dialoglayout.findViewById(R.id.portrait);
                    val landscape = dialoglayout.findViewById<SeekBar>(R.id.landscape)

                    //todo  portrait.setBackgroundColor(Palette.getDefaultColor());
                    landscape.progress = App.dpWidth - 1
                    (dialoglayout.findViewById<View>(R.id.progressnumber) as TextView).text =
                        res.getQuantityString(
                            R.plurals.landscape_columns,
                            landscape.progress + 1, landscape.progress + 1
                        )
                    landscape.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar, progress: Int,
                            fromUser: Boolean
                        ) {
                            (dialoglayout.findViewById<View>(R.id.progressnumber) as TextView).text =
                                res.getQuantityString(
                                    R.plurals.landscape_columns,
                                    landscape.progress + 1,
                                    landscape.progress + 1
                                )
                            changed = true
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    })
                    val builder = AlertDialog.Builder(this@SettingsActivity)
                        .setView(dialoglayout)
                    val dialog: Dialog = builder.create()
                    dialog.show()
                    dialog.setOnDismissListener(object : DialogInterface.OnDismissListener {
                        override fun onDismiss(dialog: DialogInterface) {
                            App.dpWidth = landscape.progress + 1
                            SettingValues.colours.edit()
                                .putInt("tabletOVERRIDE", landscape.progress + 1)
                                .apply()
                        }
                    })
                    val s = dialog.findViewById<SwitchCompat>(R.id.dualcolumns)
                    s.isChecked = SettingValues.dualPortrait
                    s.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            buttonView: CompoundButton,
                            isChecked: Boolean
                        ) {
                            SettingValues.dualPortrait = isChecked
                        }
                    })
                    val s2 = dialog.findViewById<SwitchCompat>(R.id.fullcomment)
                    s2.isChecked = SettingValues.fullCommentOverride
                    s2.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            buttonView: CompoundButton,
                            isChecked: Boolean
                        ) {
                            SettingValues.fullCommentOverride = isChecked
                        }
                    })
                    val s3 = dialog.findViewById<SwitchCompat>(R.id.singlecolumnmultiwindow)
                    s3.isChecked = SettingValues.singleColumnMultiWindow
                    s3.setOnCheckedChangeListener { buttonView, isChecked ->
                        SettingValues.singleColumnMultiWindow = isChecked
                    }
                } else {
                    ProUtil.proUpgradeMsg(this@SettingsActivity, R.string.general_multicolumn_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                        .show()
                }
            }
        })
        if (BuildConfig.isFDroid) {
            (findViewById<View>(R.id.settings_child_donatetext) as TextView).text =
                "Donate via Ko-fi"
        }
        findViewById<View>(R.id.settings_child_support).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://ko-fi.com/I2I7MHVKK")
                )
                startActivity(browserIntent)
            }
        })
        findViewById<View>(R.id.settings_child_comments).setOnClickListener(object :
            OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                val inte = Intent(this@SettingsActivity, SettingsComments::class.java)
                startActivity(inte)
            }
        })
        if (Authentication.isLoggedIn && NetworkUtil.isConnected(this)) {
            findViewById<View>(R.id.settings_child_reddit_settings).setOnClickListener(object :
                OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    val i = Intent(this@SettingsActivity, SettingsReddit::class.java)
                    startActivity(i)
                }
            })
        } else {
            findViewById<View>(R.id.settings_child_reddit_settings).isEnabled = false
            findViewById<View>(R.id.settings_child_reddit_settings).alpha = 0.25f
        }
        if (Authentication.mod) {
            findViewById<View>(R.id.settings_child_moderation).visibility = View.VISIBLE
            findViewById<View>(R.id.settings_child_moderation).setOnClickListener {
                val i = Intent(this@SettingsActivity, SettingsModeration::class.java)
                startActivity(i)
            }
        }
    }

    private fun onFolderSelection(folder: File) {
        mSettingsGeneralFragment.onFolderSelection(folder)
    }

    override fun onDestroy() {
        super.onDestroy()
        SettingValues.clearListener()
    }

    companion object {
        private const val RESTART_SETTINGS_RESULT = 2
        @JvmField
        var changed //whether or not a Setting was changed
                = false
    }
}
