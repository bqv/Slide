package ltd.ucode.slide.ui

import android.annotation.TargetApi
import android.app.ActivityManager.TaskDescription
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import ltd.ucode.slide.App.Companion.setDefaultErrorHandler
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.ForceTouch.PeekViewActivity
import me.ccrama.redditslide.SwipeLayout.SwipeBackLayout
import me.ccrama.redditslide.SwipeLayout.Utils
import me.ccrama.redditslide.SwipeLayout.app.SwipeBackActivityBase
import me.ccrama.redditslide.SwipeLayout.app.SwipeBackActivityHelper
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import java.util.Locale

/**
 * This is an activity which is the base for most of Slide's activities. It has support for handling
 * of swiping, setting up the AppBar (toolbar), and coloring of applicable views.
 */
open class BaseActivity : PeekViewActivity(), SwipeBackActivityBase {
    @JvmField
    var mToolbar: Toolbar? = null
    protected var mHelper: SwipeBackActivityHelper? = null
    protected var overrideRedditSwipeAnywhere = false
    protected var enableSwipeBackLayout = true
    protected var overrideSwipeFromAnywhere = false
    protected var verticalExit = false
    var mNfcAdapter: NfcAdapter? = null

    /**
     * Enable fullscreen immersive mode if setting is checked
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (SettingValues.immersiveMode) {
            if (hasFocus) {
                hideDecor()
            }
        }
        if (enableSwipeBackLayout) {
            Utils.convertActivityToTranslucent(this)
        }
    }

    fun hideDecor() {
        try {
            if (SettingValues.immersiveMode) {
                val decorView = window.decorView
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                    if (visibility == 0) {
                        decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_FULLSCREEN
                    } else {
                        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun showDecor() {
        try {
            if (!SettingValues.immersiveMode) {
                val decorView = window.decorView
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                decorView.setOnSystemUiVisibilityChangeListener(null)
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            try {
                onBackPressed()
            } catch (ignored: IllegalStateException) {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var shouldInterceptAlways = false

    /**
     * Force English locale if setting is checked
     */
    fun applyOverrideLanguage() {
        if (SettingValues.overrideLanguage) {
            val locale = Locale("en", "US")
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            baseContext.resources
                .updateConfiguration(
                    config,
                    baseContext.resources.displayMetrics
                )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyOverrideLanguage()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofill()
        }
        /**
         * Enable fullscreen immersive mode if setting is checked
         *
         * Adding this check in the onCreate method prevents the status/nav bars from appearing
         * briefly when changing from one activity to another
         *
         */
        hideDecor()
        if (enableSwipeBackLayout) {
            mHelper = SwipeBackActivityHelper(this)
            mHelper!!.onActivityCreate()
            if (SettingValues.swipeAnywhere || overrideRedditSwipeAnywhere) {
                if (overrideSwipeFromAnywhere) {
                    shouldInterceptAlways = true
                } else {
                    if (verticalExit) {
                        mHelper!!.swipeBackLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT or SwipeBackLayout.EDGE_BOTTOM or SwipeBackLayout.EDGE_TOP)
                    } else {
                        mHelper!!.swipeBackLayout
                            .setEdgeTrackingEnabled(
                                SwipeBackLayout.EDGE_LEFT
                                        or SwipeBackLayout.EDGE_TOP
                            )
                    }
                    mHelper!!.swipeBackLayout.setFullScreenSwipeEnabled(true)
                }
            } else {
                shouldInterceptAlways = true
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    protected open fun setAutofill() {
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (enableSwipeBackLayout) mHelper!!.onPostCreate()
    }

    override fun <T : View?> findViewById(@IdRes id: Int): T {
        val v: T = super.findViewById(id)
        return if (v == null && mHelper != null) mHelper!!.findViewById(id) as T else v
    }

    override fun getSwipeBackLayout(): SwipeBackLayout? {
        return if (enableSwipeBackLayout) {
            mHelper!!.swipeBackLayout
        } else {
            null
        }
    }

    override fun setSwipeBackEnable(enable: Boolean) {
        if (enableSwipeBackLayout) swipeBackLayout!!.setEnableGesture(enable)
    }

    override fun scrollToFinishActivity() {
        if (enableSwipeBackLayout) {
            Utils.convertActivityToTranslucent(this)
            swipeBackLayout!!.scrollToFinishActivity()
        }
    }

    /**
     * Disables the Swipe-Back-Layout. Should be called before calling super.onCreate()
     */
    protected fun disableSwipeBackLayout() {
        enableSwipeBackLayout = false
    }

    protected fun overrideSwipeFromAnywhere() {
        overrideSwipeFromAnywhere = true
    }

    protected fun overrideRedditSwipeAnywhere() {
        overrideRedditSwipeAnywhere = true
    }

    /**
     * Applies the activity's base color theme. Should be called before inflating any layouts.
     */
    protected fun applyColorTheme() {
        theme.applyStyle(FontPreferences(this).commentFontStyle.resId, true)
        theme.applyStyle(FontPreferences(this).postFontStyle.resId, true)
        theme.applyStyle(ColorPreferences(this).fontStyle.baseId, true)
    }

    /**
     * Applies the activity's base color theme based on the theme of a specific subreddit. Should be
     * called before inflating any layouts.
     *
     * @param subreddit The subreddit to base the theme on
     */
    protected fun applyColorTheme(subreddit: String?) {
        theme.applyStyle(FontPreferences(this).postFontStyle.resId, true)
        theme.applyStyle(ColorPreferences(this).getThemeSubreddit(subreddit), true)
        theme.applyStyle(FontPreferences(this).commentFontStyle.resId, true)
    }

    /**
     * Applies the activity's base color theme based on the theme of a specific subreddit. Should be
     * called before inflating any layouts.
     *
     *
     * This will take the accent colors from the sub theme but return the AMOLED with contrast base
     * theme.
     *
     * @param subreddit The subreddit to base the theme on
     */
    protected fun applyDarkColorTheme(subreddit: String?) {
        theme.applyStyle(FontPreferences(this).postFontStyle.resId, true)
        theme.applyStyle(ColorPreferences(this).getDarkThemeSubreddit(subreddit), true)
        theme.applyStyle(FontPreferences(this).commentFontStyle.resId, true)
    }

    public override fun onResume() {
        super.onResume()
        setDefaultErrorHandler(this) //set defualt reddit api issue handler
        hideDecor()
    }

    public override fun onDestroy() {
        super.onDestroy()
        setDefaultErrorHandler(null) //remove defualt reddit api issue handler (mem leaks)
    }

    /**
     * Sets up the activity's support toolbar and colorizes the status bar.
     *
     * @param toolbar        The toolbar's id
     * @param title          String resource for the toolbar's title
     * @param enableUpButton Whether or not the toolbar should have up navigation
     */
    protected fun setupAppBar(
        @IdRes toolbar: Int, @StringRes title: Int, enableUpButton: Boolean,
        colorToolbar: Boolean
    ) {
        setupAppBar(toolbar, getString(title), enableUpButton, colorToolbar)
    }

    /**
     * Sets up the activity's support toolbar and colorizes the status bar.
     *
     * @param toolbar        The toolbar's id
     * @param title          String to be set as the toolbar title
     * @param enableUpButton Whether or not the toolbar should have up navigation
     */
    protected fun setupAppBar(
        @IdRes toolbar: Int, title: String?, enableUpButton: Boolean,
        colorToolbar: Boolean
    ) {
        val systemBarColor = Palette.getStatusBarColor()
        mToolbar = findViewById<Toolbar>(toolbar)
        if (colorToolbar) {
            mToolbar!!.setBackgroundColor(Palette.getDefaultColor())
        }
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(enableUpButton)
            supportActionBar!!.setTitle(title)
        }
        themeSystemBars(systemBarColor)
        setRecentBar(title, systemBarColor)
    }

    /**
     * Sets up the activity's support toolbar and colorizes the status bar to a specific color
     *
     * @param toolbar        The toolbar's id
     * @param title          String to be set as the toolbar title
     * @param enableUpButton Whether or not the toolbar should have up navigation
     * @param color          Color to color the tab bar
     */
    protected fun setupAppBar(
        @IdRes toolbar: Int, title: String?, enableUpButton: Boolean, color: Int,
        @IdRes appbar: Int
    ) {
        val systemBarColor = Palette.getDarkerColor(color)
        mToolbar = findViewById<Toolbar>(toolbar)
        findViewById<View>(appbar).setBackgroundColor(color)
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(enableUpButton)
            supportActionBar!!.setTitle(title)
        }
        themeSystemBars(systemBarColor)
        setRecentBar(title, systemBarColor)
    }

    /**
     * Sets up the activity's support toolbar and colorizes the status bar. Applies color theming
     * based on the theme for the username specified.
     *
     * @param toolbar        The toolbar's id
     * @param title          String to be set as the toolbar title
     * @param enableUpButton Whether or not the toolbar should have up navigation
     * @param username       The username to base the theme on
     */
    protected fun setupUserAppBar(
        @IdRes toolbar: Int, title: String?,
        enableUpButton: Boolean, username: String?
    ) {
        val systemBarColor = Palette.getUserStatusBarColor(username)
        mToolbar = findViewById<Toolbar>(toolbar)
        mToolbar!!.setBackgroundColor(Palette.getColorUser(username))
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(enableUpButton)
            if (title != null) {
                supportActionBar!!.setTitle(title)
            }
        }
        themeSystemBars(systemBarColor)
        setRecentBar(title, systemBarColor)
    }

    /**
     * Sets up the activity's support toolbar and colorizes the status bar. Applies color theming
     * based on the theme for the subreddit specified.
     *
     * @param toolbar        The toolbar's id
     * @param title          String to be set as the toolbar title
     * @param enableUpButton Whether or not the toolbar should have up navigation
     * @param subreddit      The subreddit to base the theme on
     */
    protected fun setupSubredditAppBar(
        @IdRes toolbar: Int, title: String?, enableUpButton: Boolean,
        subreddit: String?
    ) {
        mToolbar = findViewById<Toolbar>(toolbar)
        mToolbar!!.setBackgroundColor(Palette.getColor(subreddit))
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(enableUpButton)
            supportActionBar!!.title = title
        }
        themeSystemBars(subreddit)
        setRecentBar(title, Palette.getSubredditStatusBarColor(subreddit))
    }

    /**
     * Sets the status bar and navigation bar color for the activity based on a specific subreddit.
     *
     * @param subreddit The subreddit to base the color on.
     */
    fun themeSystemBars(subreddit: String?) {
        themeSystemBars(Palette.getSubredditStatusBarColor(subreddit))
    }

    /**
     * Sets the status bar and navigation bar color for the activity
     *
     * @param color The color to tint the bars with
     */
    protected fun themeSystemBars(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
            if (SettingValues.colorNavBar) {
                window.navigationBarColor = color
            }
        }
    }

    /**
     * Sets the title and color of the recent bar based on the subreddit
     *
     * @param subreddit Name of the subreddit
     */
    fun setRecentBar(subreddit: String?) {
        setRecentBar(subreddit, Palette.getColor(subreddit))
    }

    var shareUrl: String? = null
        set(url: String?) {
            if (url != null) {
                field = url
                mNfcAdapter = null
                Log.i("LinkDetails", "NFC is not available on this device")
            }
        }

    /**
     * Sets the title in the recent overview with the given title and the default color
     *
     * @param title Title as string for the recent app bar
     * @param color Color for the recent app bar
     */
    fun setRecentBar(title: String?, color: Int) {
        var title = title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (title == null || title.isEmpty()) {
                title = getString(R.string.app_name)
            }
            setRecentBarTaskDescription(title, color)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setRecentBarTaskDescription(title: String?, color: Int) {
        val icon = R.drawable.ic_launcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setTaskDescription(TaskDescription(title, icon, color))
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, icon)
            setTaskDescription(TaskDescription(title, bitmap, color))
            bitmap.recycle()
        }
    }
}
