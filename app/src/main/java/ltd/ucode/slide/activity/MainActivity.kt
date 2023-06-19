package ltd.ucode.slide.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.ViewStub
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.GravityCompat
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.lusfold.androidkeyvaluestore.KVStore
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.Announcement
import me.ccrama.redditslide.Activities.BaseActivity
import me.ccrama.redditslide.Activities.CancelSubNotifs
import me.ccrama.redditslide.Activities.Discover
import me.ccrama.redditslide.Activities.Gallery
import me.ccrama.redditslide.Activities.Inbox
import me.ccrama.redditslide.Activities.Loader
import me.ccrama.redditslide.Activities.Login
import me.ccrama.redditslide.Activities.ModQueue
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.Activities.PostReadLater
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.Search
import me.ccrama.redditslide.Activities.SendMessage
import me.ccrama.redditslide.Activities.Shadowbox
import me.ccrama.redditslide.Activities.Submit
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Wiki
import me.ccrama.redditslide.Adapters.SideArrayAdapter
import me.ccrama.redditslide.Autocache.AutoCacheScheduler
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.CommentCacheAsync
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.ContentType
import me.ccrama.redditslide.ForceTouch.util.DensityUtils
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.Fragments.DrawerItemsDialog.SettingsDrawerEnum
import me.ccrama.redditslide.Fragments.SubmissionsView
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.ImageFlairs
import me.ccrama.redditslide.Notifications.CheckForMail
import me.ccrama.redditslide.Notifications.CheckForMail.AsyncGetSubs
import me.ccrama.redditslide.Notifications.NotificationJobScheduler
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Synccit.MySynccitUpdateTask
import me.ccrama.redditslide.Synccit.SynccitRead
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.Views.CommentOverflow
import me.ccrama.redditslide.Views.PreCachingLayoutManager
import me.ccrama.redditslide.Views.SidebarLayout
import me.ccrama.redditslide.Views.ToggleSwipeViewPager
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.ui.settings.ManageOfflineContent
import me.ccrama.redditslide.ui.settings.SettingsActivity
import me.ccrama.redditslide.ui.settings.SettingsGeneralFragment
import me.ccrama.redditslide.ui.settings.SettingsSubAdapter
import me.ccrama.redditslide.ui.settings.SettingsThemeFragment
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.DrawableUtil
import me.ccrama.redditslide.util.EditTextValidator
import me.ccrama.redditslide.util.ImageUtil
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.NetworkStateReceiver
import me.ccrama.redditslide.util.NetworkStateReceiver.NetworkStateReceiverListener
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.util.stubs.SimpleTextWatcher
import net.dean.jraw.ApiException
import net.dean.jraw.http.MultiRedditUpdateRequest
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.ModerationManager
import net.dean.jraw.managers.MultiRedditManager
import net.dean.jraw.models.FlairTemplate
import net.dean.jraw.models.LoggedInAccount
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.Subreddit
import net.dean.jraw.models.UserRecord
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.SubredditPaginator
import net.dean.jraw.paginators.TimePeriod
import net.dean.jraw.paginators.UserRecordPaginator
import org.ligi.snackengage.SnackEngage
import org.ligi.snackengage.conditions.AfterNumberOfOpportunities
import org.ligi.snackengage.conditions.NeverAgainWhenClickedOnce
import org.ligi.snackengage.conditions.WithLimitedNumberOfTimes
import org.ligi.snackengage.snacks.BaseSnack
import org.ligi.snackengage.snacks.RateSnack
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : BaseActivity(), NetworkStateReceiverListener {
    var menu: Menu? = null
    var mTabLayout: TabLayout? = null
    var drawerSubList: ListView? = null

    val ANIMATE_DURATION: Long = 250 //duration of animations
    private val ANIMATE_DURATION_OFFSET: Long = 45 //offset for smoothing out the exit animations
    @JvmField
    var singleMode = false
    @JvmField
    var pager: ToggleSwipeViewPager? = null
    @JvmField
    var usedArray: CaseInsensitiveArrayList? = null
    @JvmField
    var drawerLayout: DrawerLayout? = null
    var hea: View? = null
    @JvmField
    var drawerSearch: EditText? = null
    var header: View? = null
    var subToDo: String? = null
    @JvmField
    var adapter: MainPagerAdapter? = null
    var toGoto = 0
    var first = true
    @JvmField
    var selectedSub //currently selected subreddit
            : String? = null
    var doImage: Runnable? = null
    var data: Intent? = null
    @JvmField
    var commentPager = false
    @JvmField
    var runAfterLoad: Runnable? = null
    var canSubmit = false

    //if the view mode is set to Subreddit Tabs, save the title ("Slide" or "Slide (debug)")
    @JvmField
    var tabViewModeTitle: String? = null
    @JvmField
    var currentComment = 0
    @JvmField
    var openingComments: IPost? = null
    @JvmField
    var toOpenComments = -1
    var inNightMode = false
    var changed = false
    var term: String? = null
    var headerMain: View? = null
    var d: MaterialDialog? = null
    var currentFlair: AsyncTask<View?, Void?, View?>? = null
    var sidebarBody: SpoilerRobotoTextView? = null
    var sidebarOverflow: CommentOverflow? = null
    var accountsArea: View? = null
    var sideArrayAdapter: SideArrayAdapter? = null
    var caching: AsyncTask<*, *, *>? = null
    var currentlySubbed = false
    var back = 0
    private var mAsyncGetSubreddit: AsyncGetSubreddit? = null
    private var headerHeight //height of the header
            = 0
    @JvmField
    var reloadItemNumber = -2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SETTINGS_RESULT) {
            var current = pager!!.currentItem
            if (commentPager && current == currentComment) current -= 1
            if (current < 0) current = 0
            adapter = MainPagerAdapter(supportFragmentManager)
            pager!!.adapter = adapter
            pager!!.currentItem = current
            if (mTabLayout != null) {
                mTabLayout!!.setupWithViewPager(pager)
                LayoutUtils.scrollToTabAfterLayout(mTabLayout, current)
            }
            setToolbarClick()
        } else if ((requestCode == 2001 || requestCode == 2002) && resultCode == RESULT_OK) {
            if (SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_DRAWER
                || SettingValues.subredditSearchMethod
                == Constants.SUBREDDIT_SEARCH_METHOD_BOTH
            ) {
                drawerLayout!!.closeDrawers()
                drawerSearch!!.setText("")
            }

            //clear the text from the toolbar search field
            if (findViewById<AutoCompleteTextView>(R.id.toolbar_search) != null) {
                findViewById<AutoCompleteTextView>(R.id.toolbar_search)?.setText("")
            }
            val view = this@MainActivity.currentFocus
            if (view != null) {
                KeyboardUtil.hideKeyboard(this, view.windowToken, 0)
            }
        } else if (requestCode == 2002 && resultCode != RESULT_OK) {
            mToolbar!!.performLongClick() //search was init from the toolbar, so return focus to the toolbar
        } else if (requestCode == 423 && resultCode == RESULT_OK) {
            (adapter as MainPagerAdapterComment?)!!.mCurrentComments!!.doResult(data)
        } else if (requestCode == 940) {
            if (adapter != null && adapter!!.currentFragment != null) {
                if (resultCode == RESULT_OK) {
                    val posts = data!!.getIntegerArrayListExtra("seen")
                    (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.refreshView(posts!!)
                    if (data.hasExtra("lastPage") && data.getIntExtra(
                            "lastPage",
                            0
                        ) != 0 && (adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is LinearLayoutManager
                    ) {
                        ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager as LinearLayoutManager?)
                            ?.scrollToPositionWithOffset(
                                data.getIntExtra("lastPage", 0) + 1,
                                mToolbar!!.height
                            )
                    }
                } else {
                    (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.refreshView()
                }
            }
        } else if (requestCode == RESET_ADAPTER_RESULT) {
            resetAdapter()
            setDrawerSubList()
        } else if (requestCode == TUTORIAL_RESULT) {
            UserSubscriptions.doMainActivitySubs(this)
        } else if (requestCode == INBOX_RESULT) {
            //update notification badge
            AsyncNotificationBadge().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else if (requestCode == 3333) {
            this.data = data
            if (doImage != null) {
                val handler = Handler()
                handler.post(doImage!!)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
        /* todo  if(resultCode == 4 && UserSubscriptions.hasChanged){
            UserSubscriptions.hasChanged = false;
            sideArrayAdapter.setSideItems(UserSubscriptions.getAllSubreddits(this));
            sideArrayAdapter.notifyDataSetChanged();
        }*/
    }

    override fun onBackPressed() {
        if (drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.START)
            || drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.END)
        ) {
            drawerLayout!!.closeDrawers()
        } else if (commentPager && pager!!.currentItem == toOpenComments) {
            pager!!.currentItem = pager!!.currentItem - 1
        } else if ((SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                    || SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
            && findViewById<AutoCompleteTextView>(R.id.toolbar_search).visibility == View.VISIBLE
        ) {
            findViewById<ImageView>(R.id.close_search_toolbar).performClick() //close GO_TO_SUB_FIELD
        } else if (SettingValues.backButtonBehavior
            == Constants.BackButtonBehaviorOptions.OpenDrawer.value
        ) {
            drawerLayout!!.openDrawer(GravityCompat.START)
        } else if (SettingValues.backButtonBehavior
            == Constants.BackButtonBehaviorOptions.GotoFirst.value
        ) {
            pager!!.currentItem = 0
        } else if (SettingValues.backButtonBehavior
            == Constants.BackButtonBehaviorOptions.ConfirmExit.value
        ) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.general_confirm_exit)
                .setMessage(R.string.general_confirm_exit_msg)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int -> finish() }
                .setNegativeButton(R.string.btn_no) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        changed = false
        if (!SettingValues.synccitName.isNullOrEmpty()) {
            MySynccitUpdateTask().execute(
                *SynccitRead.newVisited.toTypedArray()
            )
        }
        if (Authentication.isLoggedIn && Authentication.me != null //This is causing a crash, might not be important since the storeVisits will just not do anything without gold && Authentication.me.hasGold()
            && !SynccitRead.newVisited.isEmpty()
        ) {
            object : AsyncTask<Void?, Void?, Void?>() {
                protected override fun doInBackground(vararg params: Void?): Void? {
                    try {
                        val returned = arrayOfNulls<String>(SynccitRead.newVisited.size)
                        for ((i, s) in SynccitRead.newVisited.withIndex()) {
                            if (!s.contains("t3_")) {
                                returned[i] = "t3_$s"
                            } else {
                                returned[i] = s
                            }
                        }
                        AccountManager(Authentication.reddit).storeVisits(*returned)
                        SynccitRead.newVisited = ArrayList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        //Upon leaving MainActivity--hide the toolbar search if it is visible
        if (findViewById<AutoCompleteTextView>(R.id.toolbar_search).visibility == View.VISIBLE) {
            findViewById<ImageView>(R.id.close_search_toolbar).performClick()
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean? ->
            if (!isGranted!!) {
                runOnUiThread {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.err_permission)
                        .setMessage(R.string.err_permission_msg)
                        .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int -> requestPermission() }
                        .setNegativeButton(R.string.btn_no) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                        .show()
                }
            }
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changed = true
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changed = true
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putStringArrayList(SUBS, usedArray)
        savedInstanceState.putBoolean(LOGGED_IN, Authentication.isLoggedIn)
        savedInstanceState.putBoolean(IS_ONLINE, Authentication.didOnline)
        savedInstanceState.putString(USERNAME, Authentication.name)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (pager != null && SettingValues.commentPager && pager!!.currentItem == toOpenComments && SettingValues.commentVolumeNav
            && pager!!.adapter is MainPagerAdapterComment
        ) {
            return if (SettingValues.commentVolumeNav) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> (pager!!.adapter as MainPagerAdapterComment?)!!.mCurrentComments!!.onKeyDown(
                        keyCode, event
                    )

                    else -> super.dispatchKeyEvent(event)
                }
            } else {
                super.dispatchKeyEvent(event)
            }
        }
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        return if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            onKeyDown(keyCode, event)
        } else super.dispatchKeyEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        if (NetworkUtil.isConnected(this)) {
            if (SettingValues.expandedToolbar) {
                inflater.inflate(R.menu.menu_subreddit_overview_expanded, menu)
            } else {
                inflater.inflate(R.menu.menu_subreddit_overview, menu)
            }
            // Hide the "Share Slide" menu if the user has Pro installed
            if (SettingValues.isPro) {
                menu.findItem(R.id.share).isVisible = false
            }
            if (SettingValues.fab && SettingValues.fabType == Constants.FAB_DISMISS) {
                menu.findItem(R.id.hide_posts).isVisible = false
            }
        } else {
            inflater.inflate(R.menu.menu_subreddit_overview_offline, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        this.menu = menu
        /**
         * Hide the "Submit" and "Sidebar" menu items if the currently viewed sub is a multi,
         * domain, the frontpage, or /r/all. If the subreddit has a "." in it, we know it's a domain because
         * subreddits aren't allowed to have hard-stops in the name.
         */
        if (Authentication.didOnline && usedArray != null) {
            val subreddit = usedArray!![pager!!.currentItem]
            if ((subreddit.contains("/m/")
                        || subreddit.contains(".")
                        || subreddit.contains("+")) || subreddit == "frontpage" || subreddit == "all"
            ) {
                if (menu.findItem(R.id.submit) != null) {
                    menu.findItem(R.id.submit).isVisible = false
                }
                if (menu.findItem(R.id.sidebar) != null) {
                    menu.findItem(R.id.sidebar).isVisible = false
                }
            } else {
                if (menu.findItem(R.id.submit) != null) {
                    menu.findItem(R.id.submit).isVisible = true
                }
                if (menu.findItem(R.id.sidebar) != null) {
                    menu.findItem(R.id.sidebar).isVisible = true
                }
            }
            menu.findItem(R.id.theme)
                .setOnMenuItemClickListener {
                    val style = ColorPreferences(this@MainActivity).getThemeSubreddit(
                        subreddit
                    )
                    val contextThemeWrapper: Context = ContextThemeWrapper(this@MainActivity, style)
                    val localInflater = layoutInflater.cloneInContext(contextThemeWrapper)
                    val dialoglayout = localInflater.inflate(R.layout.colorsub, null)
                    val arrayList = ArrayList<String>()
                    arrayList.add(subreddit)
                    SettingsSubAdapter.showSubThemeEditor(
                        arrayList, this@MainActivity,
                        dialoglayout
                    )
                    /*
                        boolean old = SettingValues.isPicsEnabled(selectedSub);
                        SettingValues.setPicsEnabled(selectedSub, !item.isChecked());
                        item.setChecked(!item.isChecked());
                        reloadSubs();
                        invalidateOptionsMenu();*/false
                }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val subreddit = usedArray!![App.currentPosition]
        return when (item.itemId) {
            R.id.filter -> {
                filterContent(shouldLoad)
                true
            }

            R.id.sidebar -> {
                if (subreddit != "all"
                    && subreddit != "frontpage"
                    && !subreddit.contains(".")
                    && !subreddit.contains("+")
                    && !subreddit.contains(".")
                    && !subreddit.contains("/m/")
                ) {
                    drawerLayout!!.openDrawer(GravityCompat.END)
                } else {
                    Toast.makeText(this, R.string.sidebar_notfound, Toast.LENGTH_SHORT).show()
                }
                true
            }

            R.id.night -> {
                run {
                    val inflater = layoutInflater
                    val dialoglayout = inflater.inflate(R.layout.choosethemesmall, null)
                    val title = dialoglayout.findViewById<TextView>(R.id.title)
                    title.setBackgroundColor(Palette.getDefaultColor())
                    val builder = AlertDialog.Builder(this@MainActivity)
                        .setView(dialoglayout)
                    val d: Dialog = builder.show()
                    back = ColorPreferences(this@MainActivity).fontStyle.themeType
                    if (SettingValues.isNight) {
                        dialoglayout.findViewById<View>(R.id.nightmsg).visibility = View.VISIBLE
                    }
                    for (pair in ColorPreferences.themePairList) {
                        dialoglayout.findViewById<View>(pair.first!!)
                            .setOnClickListener {
                                val names = ColorPreferences(this@MainActivity).fontStyle
                                    .title
                                    .split("_".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                val name = names[names.size - 1]
                                val newName = name.replace("(", "")
                                for (theme in ColorPreferences.Theme.values()) {
                                    if (theme.toString().contains(newName)
                                        && theme.themeType == pair.second
                                    ) {
                                        back = theme.themeType
                                        ColorPreferences(this@MainActivity).fontStyle = theme
                                        d.dismiss()
                                        restartTheme()
                                        break
                                    }
                                }
                            }
                    }
                }
                true
            }

            R.id.action_refresh -> {
                if (adapter != null && adapter!!.currentFragment != null) {
                    (adapter!!.currentFragment as SubmissionsView?)!!.forceRefresh()
                }
                true
            }

            R.id.action_sort -> {
                if (subreddit.equals("friends", ignoreCase = true)) {
                    val s = Snackbar.make(
                        findViewById(R.id.anchor),
                        getString(R.string.friends_sort_error), Snackbar.LENGTH_SHORT
                    )
                    LayoutUtils.showSnackbar(s)
                } else {
                    openPopup()
                }
                true
            }

            R.id.search -> {
                val builder = MaterialDialog.Builder(this).title(R.string.search_title)
                    .alwaysCallInputCallback()
                    .input(
                        getString(R.string.search_msg), ""
                    ) { materialDialog, charSequence -> term = charSequence.toString() }

                //Add "search current sub" if it is not frontpage/all/random
                if (!subreddit.equals("frontpage", ignoreCase = true)
                    && !subreddit.equals("all", ignoreCase = true)
                    && !subreddit.contains(".")
                    && !subreddit.contains("/m/")
                    && !subreddit.equals("friends", ignoreCase = true)
                    && !subreddit.equals("random", ignoreCase = true)
                    && !subreddit.equals("popular", ignoreCase = true)
                    && !subreddit.equals("myrandom", ignoreCase = true)
                    && !subreddit.equals("randnsfw", ignoreCase = true)
                ) {
                    builder.positiveText(getString(R.string.search_subreddit, subreddit))
                        .onPositive { materialDialog, dialogAction ->
                            val i = Intent(this@MainActivity, Search::class.java)
                            i.putExtra(Search.EXTRA_TERM, term)
                            i.putExtra(Search.EXTRA_SUBREDDIT, subreddit)
                            Log.v(
                                LogUtil.getTag(),
                                "INTENT SHOWS $term AND $subreddit"
                            )
                            startActivity(i)
                        }
                    builder.neutralText(R.string.search_all)
                        .onNeutral { materialDialog, dialogAction ->
                            val i = Intent(this@MainActivity, Search::class.java)
                            i.putExtra(Search.EXTRA_TERM, term)
                            startActivity(i)
                        }
                } else {
                    builder.positiveText(R.string.search_all)
                        .onPositive { materialDialog, dialogAction ->
                            val i = Intent(this@MainActivity, Search::class.java)
                            i.putExtra(Search.EXTRA_TERM, term)
                            startActivity(i)
                        }
                }
                builder.show()
                true
            }

            R.id.save -> {
                saveOffline(
                    (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts.map(::RedditSubmission),
                    (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                )
                true
            }

            R.id.hide_posts -> {
                (adapter!!.currentFragment as SubmissionsView?)!!.clearSeenPosts(false)
                true
            }

            R.id.share -> {
                App.defaultShareText(
                    "Slide for Reddit",
                    "https://play.google.com/store/apps/details?id=me.ccrama.redditslide",
                    this@MainActivity
                )
                true
            }

            R.id.submit -> {
                run {
                    val i = Intent(this@MainActivity, Submit::class.java)
                    i.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                    startActivity(i)
                }
                true
            }

            R.id.gallery -> {
                if (SettingValues.isPro) {
                    val posts = (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i2 = Intent(this, Gallery::class.java)
                        i2.putExtra(
                            "offline",
                            if ((adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached
                                != null
                            ) (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached!!.time else 0L
                        )
                        i2.putExtra(
                            Gallery.EXTRA_SUBREDDIT,
                            (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                        )
                        startActivity(i2)
                    }
                } else {
                    val b = ProUtil.proUpgradeMsg(this, R.string.general_gallerymode_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                    if (SettingValues.previews > 0) {
                        b.setNeutralButton(
                            getString(R.string.pro_previews, SettingValues.previews)
                        ) { dialog: DialogInterface?, which: Int ->
                            SettingValues.decreasePreviewsLeft()
                            val posts =
                                (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                            if (posts != null && !posts.isEmpty()) {
                                val i2 = Intent(this@MainActivity, Gallery::class.java)
                                i2.putExtra(
                                    "offline",
                                    if ((adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached
                                        != null
                                    ) (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached!!.time else 0L
                                )
                                i2.putExtra(
                                    Gallery.EXTRA_SUBREDDIT,
                                    (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                                )
                                startActivity(i2)
                            }
                        }
                    }
                    b.show()
                }
                true
            }

            R.id.action_shadowbox -> {
                if (SettingValues.isPro) {
                    val posts = (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i2 = Intent(this, Shadowbox::class.java)
                        i2.putExtra(Shadowbox.EXTRA_PAGE, currentPage)
                        i2.putExtra(
                            "offline",
                            if ((adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached
                                != null
                            ) (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached!!.time else 0L
                        )
                        i2.putExtra(
                            Shadowbox.EXTRA_SUBREDDIT,
                            (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                        )
                        startActivity(i2)
                    }
                } else {
                    val b = ProUtil.proUpgradeMsg(this, R.string.general_shadowbox_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                    if (SettingValues.previews > 0) {
                        b.setNeutralButton(
                            "Preview (" + SettingValues.previews + ")"
                        ) { dialog: DialogInterface?, which: Int ->
                            SettingValues.decreasePreviewsLeft()
                            val posts =
                                (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                            if (posts != null && !posts.isEmpty()) {
                                val i2 = Intent(this@MainActivity, Shadowbox::class.java)
                                i2.putExtra(Shadowbox.EXTRA_PAGE, currentPage)
                                i2.putExtra(
                                    "offline",
                                    if ((adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached
                                        != null
                                    ) (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached!!.time else 0L
                                )
                                i2.putExtra(
                                    Shadowbox.EXTRA_SUBREDDIT,
                                    (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                                )
                                startActivity(i2)
                            }
                        }
                    }
                    b.show()
                }
                true
            }

            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        inNightMode = SettingValues.isNight
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            // Activity was brought to front and not created
            finish()
            return
        }
        if (!Slide.hasStarted) {
            Slide.hasStarted = true
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        }
        var first = false
        if (!SettingValues.colours.contains("Tutorial")) {
            first = true
            val i = Intent(this, Tutorial::class.java)
            doForcePrefs()
            startActivity(i)
        } else {
            if (Authentication.didOnline
                && NetworkUtil.isConnected(this@MainActivity)
                && !checkedPopups
            ) {
                runAfterLoad = Runnable {
                    runAfterLoad = null
                    if (Authentication.isLoggedIn) {
                        AsyncNotificationBadge().executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR
                        )
                    }
                    if (SettingValues.appRestart.getString(CheckForMail.SUBS_TO_GET, "")!!.isNotEmpty()) {
                        AsyncGetSubs(this@MainActivity).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR
                        )
                    }
                    object : AsyncTask<Void?, Void?, IPost?>() {
                        override fun doInBackground(vararg params: Void?): IPost? {
                            if (Authentication.isLoggedIn) UserSubscriptions.doOnlineSyncing()
                            try {
                                val p = SubredditPaginator(
                                    Authentication.reddit,
                                    "slideforreddit"
                                )
                                p.setLimit(2)
                                val posts = ArrayList(p.next())
                                for (s in posts) {
                                    var version = BuildConfig.VERSION_NAME
                                    if (version.length > 5) {
                                        version = version.substring(0, version.lastIndexOf("."))
                                    }
                                    if (s.isStickied && s.submissionFlair.text != null && s.submissionFlair
                                            .text
                                            .equals("Announcement", ignoreCase = true)
                                        && !SettingValues.appRestart.contains(
                                            "announcement" + s.fullName
                                        )
                                        && s.title.contains(version)
                                    ) {
                                        SettingValues.appRestart.edit()
                                            .putBoolean(
                                                "announcement" + s.fullName,
                                                true
                                            )
                                            .apply()
                                        return RedditSubmission(s)
                                    } else if ((BuildConfig.VERSION_NAME.contains("alpha")
                                                && s.isStickied) && s.submissionFlair.text != null && s.submissionFlair
                                            .text
                                            .equals("Alpha", ignoreCase = true)
                                        && !SettingValues.appRestart.contains(
                                            "announcement" + s.fullName
                                        )
                                        && s.title
                                            .contains(BuildConfig.VERSION_NAME)
                                    ) {
                                        SettingValues.appRestart.edit()
                                            .putBoolean(
                                                "announcement" + s.fullName,
                                                true
                                            )
                                            .apply()
                                        return RedditSubmission(s)
                                    } else if (s.isStickied
                                        && s.submissionFlair
                                            .text
                                            .equals("PRO", ignoreCase = true)
                                        && !SettingValues.isPro
                                        && !SettingValues.appRestart.contains(
                                            "announcement" + s.fullName
                                        )
                                    ) {
                                        SettingValues.appRestart.edit()
                                            .putBoolean(
                                                "announcement" + s.fullName,
                                                true
                                            )
                                            .apply()
                                        return RedditSubmission(s)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return null
                        }

                        override fun onPostExecute(s: IPost?) {
                            checkedPopups = true
                            if (s != null) {
                                SettingValues.appRestart.edit()
                                    .putString(
                                        "page",
                                        s.body
                                    )
                                    .apply()
                                SettingValues.appRestart.edit()
                                    .putString("title", s.title)
                                    .apply()
                                SettingValues.appRestart.edit().putString("url", s.url).apply()
                                val title: String
                                title = if (s.title.lowercase().contains("release")) {
                                    getString(R.string.btn_changelog)
                                } else {
                                    getString(R.string.btn_view)
                                }
                                val snack = Snackbar.make(
                                    pager!!, s.title,
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                    .setAction(title, object : OnSingleClickListener() {
                                        override fun onSingleClick(v: View) {
                                            val i = Intent(
                                                this@MainActivity,
                                                Announcement::class.java
                                            )
                                            startActivity(i)
                                        }
                                    })
                                LayoutUtils.showSnackbar(snack)
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    //todo this  new AsyncStartNotifSocket().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
        if (savedInstanceState != null && !changed) {
            Authentication.isLoggedIn = savedInstanceState.getBoolean(LOGGED_IN)
            Authentication.name = savedInstanceState.getString(USERNAME, "LOGGEDOUT")
            Authentication.didOnline = savedInstanceState.getBoolean(IS_ONLINE)
        } else {
            changed = false
        }
        if (intent.getBooleanExtra("EXIT", false)) finish()
        applyColorTheme()
        setContentView(R.layout.activity_overview)
        mToolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        setSupportActionBar(mToolbar)
        if (intent != null && intent.hasExtra(EXTRA_PAGE_TO)) {
            toGoto = intent.getIntExtra(EXTRA_PAGE_TO, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = this.window
            window.statusBarColor =
                Palette.getDarkerColor(Palette.getDarkerColor(Palette.getDefaultColor()))
        }
        mTabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        header = findViewById(R.id.header)

        //Gets the height of the header
        if (header != null) {
            header!!.viewTreeObserver
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        headerHeight = header!!.height
                        header!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
        }
        pager = findViewById<ToggleSwipeViewPager>(R.id.content_view)
        singleMode = SettingValues.single
        if (singleMode) {
            commentPager = SettingValues.commentPager
        }
        // Inflate tabs if single mode is disabled
        if (!singleMode) {
            mTabLayout = findViewById<ViewStub>(R.id.stub_tabs)?.inflate() as TabLayout?
        }
        // Disable swiping if single mode is enabled
        if (singleMode) {
            pager!!.setSwipingEnabled(false)
        }
        sidebarBody = findViewById<SpoilerRobotoTextView>(R.id.sidebar_text)
        sidebarOverflow = findViewById<CommentOverflow>(R.id.commentOverflow)
        if (!SettingValues.appRestart.getBoolean("isRestarting", false) && SettingValues.colours.contains(
                "Tutorial"
            )
        ) {
            LogUtil.v("Starting main " + Authentication.name)
            Authentication.isLoggedIn = SettingValues.appRestart.getBoolean("loggedin", false)
            Authentication.name = SettingValues.appRestart.getString("name", "LOGGEDOUT")
            UserSubscriptions.doMainActivitySubs(this)
        } else if (!first) {
            LogUtil.v("Starting main 2 " + Authentication.name)
            Authentication.isLoggedIn = SettingValues.appRestart.getBoolean("loggedin", false)
            Authentication.name = SettingValues.appRestart.getString("name", "LOGGEDOUT")
            SettingValues.appRestart.edit().putBoolean("isRestarting", false).commit()
            App.isRestarting = false
            UserSubscriptions.doMainActivitySubs(this)
        }
        if (!SettingValues.seen.contains("isCleared") && !SettingValues.seen.all.isEmpty()
            || !SettingValues.appRestart.contains("hasCleared")
        ) {
            object : AsyncTask<Void?, Void?, Void?>() {
                protected override fun doInBackground(vararg params: Void?): Void? {
                    val m = KVStore.getInstance()
                    val values = SettingValues.seen.all
                    for ((key, value) in values) {
                        if (key.length == 6 && value is Boolean) {
                            m.insert(key, "true")
                        } else if (value is Long) {
                            m.insert(key, SettingValues.seen.getLong(key, 0).toString())
                        }
                    }
                    SettingValues.seen.edit().clear().putBoolean("isCleared", true).apply()
                    if (SettingValues.hiddenPosts.all.isNotEmpty()) {
                        SettingValues.hidden.edit().clear().apply()
                        SettingValues.hiddenPosts.edit().clear().apply()
                    }
                    if (!SettingValues.appRestart.contains("hasCleared")) {
                        val e = SettingValues.appRestart.edit()
                        val toClear = SettingValues.appRestart.all
                        for ((key, value) in toClear) {
                            if (value is String
                                && value.length > 300
                            ) {
                                e.remove(key)
                            }
                        }
                        e.putBoolean("hasCleared", true)
                        e.apply()
                    }
                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    dismissProgressDialog()
                }

                override fun onPreExecute() {
                    d = MaterialDialog.Builder(this@MainActivity).title(
                        R.string.misc_setting_up
                    )
                        .content(R.string.misc_setting_up_message)
                        .progress(true, 100)
                        .cancelable(false)
                        .build()
                    d!!.show()
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        if (!BuildConfig.isFDroid && Authentication.isLoggedIn && NetworkUtil.isConnected(this@MainActivity)) {
            // Display an snackbar that asks the user to rate the app after this
            // activity was created 6 times, never again when once clicked or with a maximum of
            // two times.
            SnackEngage.from(this@MainActivity)
                .withSnack(
                    RateSnack().withConditions(
                        NeverAgainWhenClickedOnce(),
                        AfterNumberOfOpportunities(10), WithLimitedNumberOfTimes(2)
                    )
                        .overrideActionText(getString(R.string.misc_rate_msg))
                        .overrideTitleText(getString(R.string.misc_rate_title))
                        .withDuration(BaseSnack.DURATION_LONG)
                ) /*.withSnack(new CustomSnack(new Intent(MainActivity.this, SettingsReddit.class), "Thumbnails are disabled", "Change", "THUMBNAIL_INFO")
                            .withConditions(new AfterNumberOfOpportunities(2),
                                    new WithLimitedNumberOfTimes(2), new NeverAgainWhenClickedOnce())
                            .withDuration(BaseSnack.DURATION_LONG))*/
                .build()
                .engageWhenAppropriate()
        }
        if (SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
            || SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_BOTH
        ) {
            setupSubredditSearchToolbar()
        }
        /**
         * int for the current base theme selected.
         * 0 = Dark, 1 = Light, 2 = AMOLED, 3 = Dark blue, 4 = AMOLED with contrast, 5 = Sepia
         */
        SettingValues.currentTheme = ColorPreferences(this).fontStyle.themeType
        networkStateReceiver = NetworkStateReceiver()
        networkStateReceiver!!.addListener(this)
        try {
            this.registerReceiver(
                networkStateReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        } catch (e: Exception) {
        }
        LogUtil.v("Installed browsers")
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse("http://ccrama.me/")
        val allApps = packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_DISABLED_COMPONENTS
        )
        for (i in allApps) {
            if (i.activityInfo.isEnabled) LogUtil.v(i.activityInfo.packageName)
        }
    }

    override fun networkAvailable() {
        if (runAfterLoad == null && App.authentication != null) {
            Authentication.resetAdapter()
        }
    }

    var networkStateReceiver: NetworkStateReceiver? = null
    override fun networkUnavailable() {}
    fun checkClipboard() {
        try {
            val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val data = clipboard.primaryClip
                val s = data!!.getItemAt(0).text as String
                if (!s.isEmpty()) {
                    if (ContentType.getContentType(s) == ContentType.Type.REDDIT && !HasSeen.getSeen(
                            s
                        )
                    ) {
                        val snack = Snackbar.make(
                            mToolbar!!, "Reddit link found in your clipboard",
                            Snackbar.LENGTH_LONG
                        )
                        snack.setAction("OPEN") {
                            OpenRedditLink.openUrl(
                                this@MainActivity,
                                s,
                                false
                            )
                        }
                        snack.show()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (Authentication.isLoggedIn && Authentication.didOnline && NetworkUtil.isConnected(
                this@MainActivity
            ) && headerMain != null && runAfterLoad == null
        ) {
            AsyncNotificationBadge().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else if (Authentication.isLoggedIn && Authentication.name.equals(
                "loggedout",
                ignoreCase = true
            )
        ) {
            restartTheme() //force a restart because we should not be here
        }
        if (inNightMode != SettingValues.isNight) {
            (drawerLayout!!.findViewById<View>(R.id.toggle_night_mode) as SwitchCompat?)!!.isChecked =
                SettingValues.isNight
            restartTheme()
        }
        checkClipboard()
        if (pager != null && commentPager) {
            if (pager!!.currentItem != toOpenComments && shouldLoad != null) {
                if (usedArray != null && !shouldLoad!!.contains("+") && usedArray!!.indexOf(
                        shouldLoad
                    ) != pager!!.currentItem
                ) {
                    pager!!.currentItem = toOpenComments - 1
                }
            }
        }
        App.setDefaultErrorHandler(this)
        if (sideArrayAdapter != null) {
            sideArrayAdapter!!.updateHistory(UserSubscriptions.getHistory())
        }

        /* remove   if (datasetChanged && UserSubscriptions.hasSubs() && !usedArray.isEmpty()) {
            usedArray = new ArrayList<>(UserSubscriptions.getSubscriptions(this));
            adapter.notifyDataSetChanged();
            sideArrayAdapter.notifyDataSetChanged();
            datasetChanged = false;
            if (mTabLayout != null) {
                mTabLayout.setupWithViewPager(pager);
                LayoutUtils.scrollToTabAfterLayout(mTabLayout, pager.getCurrentItem());
            }
            setToolbarClick();
        }*/
        //Only refresh the view if a Setting was altered
        if (SettingsActivity.changed || SettingsThemeFragment.changed) {
            reloadSubs()
            //If the user changed a Setting regarding the app's theme, restartTheme()
            if (SettingsThemeFragment.changed /* todo maybe later || (usedArray != null && usedArray.size() != UserSubscriptions.getSubscriptions(this).size())*/) {
                restartTheme()
            }

            //Need to change the subreddit search method
            if (SettingsGeneralFragment.searchChanged) {
                setDrawerSubList()
                if (SettingValues.subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_DRAWER
                ) {
                    mToolbar!!.setOnLongClickListener(
                        null
                    ) //remove the long click listener from the toolbar
                    findViewById<View>(R.id.drawer_divider).visibility = View.GONE
                } else if (SettingValues.subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                ) {
                    setupSubredditSearchToolbar()
                } else if (SettingValues.subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH
                ) {
                    findViewById<View>(R.id.drawer_divider).visibility = View.GONE
                    setupSubredditSearchToolbar()
                    setDrawerSubList()
                }
                SettingsGeneralFragment.searchChanged = false
            }
            SettingsThemeFragment.changed = false
            SettingsActivity.changed = false
            setToolbarClick()
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(networkStateReceiver)
        } catch (ignored: Exception) {
        }
        dismissProgressDialog()
        Slide.hasStarted = false
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        return if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            onOptionsItemSelected(menu!!.findItem(R.id.search))
        } else super.onKeyDown(keyCode, event)
    }

    var accounts = HashMap<String, String>()
    fun doDrawer() {
        drawerSubList = findViewById<ListView>(R.id.drawerlistview)
        drawerSubList!!.dividerHeight = 0
        drawerSubList!!.descendantFocusability = ListView.FOCUS_BEFORE_DESCENDANTS
        val inflater = layoutInflater
        val header: View
        if (Authentication.isLoggedIn && Authentication.didOnline) {
            header = inflater.inflate(R.layout.drawer_loggedin, drawerSubList, false)
            headerMain = header
            hea = header.findViewById(R.id.back)
            drawerSubList!!.addHeaderView(header, null, false)
            (header.findViewById<View>(R.id.name) as TextView?)!!.text = Authentication.name
            header.findViewById<View>(R.id.multi)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        if (runAfterLoad == null) {
                            val inte = Intent(this@MainActivity, MultiredditOverview::class.java)
                            this@MainActivity.startActivity(inte)
                        }
                    }
                })
            header.findViewById<View>(R.id.multi).setOnLongClickListener {
                MaterialDialog.Builder(this@MainActivity).inputRange(3, 20)
                    .alwaysCallInputCallback()
                    .input(getString(R.string.user_enter), null) { dialog, input ->
                        val editText = dialog.inputEditText
                        EditTextValidator.validateUsername(editText)
                        if (input.length >= 3 && input.length <= 20) {
                            dialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
                        }
                    }
                    .positiveText(R.string.user_btn_gotomultis)
                    .onPositive { dialog, which ->
                        if (runAfterLoad == null) {
                            val inte = Intent(
                                this@MainActivity,
                                MultiredditOverview::class.java
                            )
                            inte.putExtra(
                                Profile.EXTRA_PROFILE,
                                dialog.inputEditText!!.text.toString()
                            )
                            this@MainActivity.startActivity(inte)
                        }
                    }
                    .negativeText(R.string.btn_cancel)
                    .show()
                true
            }
            header.findViewById<View>(R.id.discover)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Discover::class.java)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.prof_click)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.saved)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        inte.putExtra(Profile.EXTRA_SAVED, true)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.later)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, PostReadLater::class.java)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.history)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        inte.putExtra(Profile.EXTRA_HISTORY, true)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.commented)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        inte.putExtra(Profile.EXTRA_COMMENT, true)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.submitted)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        inte.putExtra(Profile.EXTRA_SUBMIT, true)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.upvoted)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(Profile.EXTRA_PROFILE, Authentication.name)
                        inte.putExtra(Profile.EXTRA_UPVOTE, true)
                        this@MainActivity.startActivity(inte)
                    }
                })
            /**
             * If the user is a known mod, show the "Moderation" drawer item quickly to
             * stop the UI from jumping
             */
            if (UserSubscriptions.modOf != null && !UserSubscriptions.modOf.isEmpty() && Authentication.mod) {
                header.findViewById<View>(R.id.mod).visibility = View.VISIBLE
            }
            //update notification badge
            val profStuff = header.findViewById<LinearLayout>(R.id.accountsarea)
            profStuff.visibility = View.GONE
            findViewById<View>(R.id.back).setOnClickListener {
                if (profStuff.visibility == View.GONE) {
                    expand(profStuff)
                    header.contentDescription = resources.getString(R.string.btn_collapse)
                    AnimatorUtil.flipAnimator(false, header.findViewById(R.id.headerflip)).start()
                } else {
                    collapse(profStuff)
                    header.contentDescription = resources.getString(R.string.btn_expand)
                    AnimatorUtil.flipAnimator(true, header.findViewById(R.id.headerflip)).start()
                }
            }
            for (s in SettingValues.authentication!!.getStringSet("accounts", HashSet())!!) {
                if (s.contains(":")) {
                    accounts[s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]] = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                } else {
                    accounts[s] = ""
                }
            }
            val keys = ArrayList(accounts.keys)
            val accountList = header.findViewById<LinearLayout>(R.id.accountsarea)
            for (accName in keys) {
                LogUtil.v(accName)
                val t = layoutInflater.inflate(
                    R.layout.account_textview_white, accountList,
                    false
                )
                (t.findViewById<View>(R.id.name) as TextView?)!!.text = accName
                LogUtil.v("Adding click to " + (t.findViewById<View>(R.id.name) as TextView?)?.text)
                t.findViewById<View>(R.id.remove).setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.profile_remove)
                        .setMessage(R.string.profile_remove_account)
                        .setNegativeButton(R.string.btn_delete) { dialog2: DialogInterface, which2: Int ->
                            val accounts2 = SettingValues.authentication.getStringSet(
                                "accounts", HashSet()
                            )
                            val done: MutableSet<String> = HashSet()
                            for (s in accounts2!!) {
                                if (!s.contains(accName)) {
                                    done.add(s)
                                }
                            }
                            SettingValues.authentication.edit()
                                .putStringSet("accounts", done)
                                .commit()
                            dialog2.dismiss()
                            accountList.removeView(t)
                            if (accName.equals(Authentication.name, ignoreCase = true)) {
                                var d = false
                                for (s in keys) {
                                    if (!s.equals(accName, ignoreCase = true)) {
                                        d = true
                                        LogUtil.v("Switching to $s")
                                        for ((key, value) in accounts) {
                                            LogUtil.v("$key:$value")
                                        }
                                        if (accounts.containsKey(s) && !accounts[s]
                                                !!.isEmpty()
                                        ) {
                                            SettingValues.authentication.edit()
                                                .putString("lasttoken", accounts[s])
                                                .remove("backedCreds")
                                                .commit()
                                        } else {
                                            val tokens = ArrayList(
                                                SettingValues.authentication.getStringSet(
                                                    "tokens", HashSet()
                                                )
                                            )
                                            var index = keys.indexOf(s)
                                            if (keys.indexOf(s) > tokens.size) {
                                                index -= 1
                                            }
                                            SettingValues.authentication.edit()
                                                .putString(
                                                    "lasttoken",
                                                    tokens[index]
                                                )
                                                .remove("backedCreds")
                                                .commit()
                                        }
                                        Authentication.name = s
                                        UserSubscriptions.switchAccounts()
                                        App.forceRestart(this@MainActivity, true)
                                        break
                                    }
                                }
                                if (!d) {
                                    Authentication.name = "LOGGEDOUT"
                                    Authentication.isLoggedIn = false
                                    SettingValues.authentication.edit()
                                        .remove("lasttoken")
                                        .remove("backedCreds")
                                        .commit()
                                    UserSubscriptions.switchAccounts()
                                    App.forceRestart(this@MainActivity, true)
                                }
                            } else {
                                accounts.remove(accName)
                                keys.remove(accName)
                            }
                        }
                        .setPositiveButton(R.string.btn_cancel, null)
                        .show()
                }
                t.setOnClickListener {
                    val accName = (t.findViewById<View>(R.id.name) as TextView?)?.text.toString()
                    LogUtil.v("Found name is $accName")
                    if (!accName.equals(Authentication.name, ignoreCase = true)) {
                        LogUtil.v("Switching to $accName")
                        if (!accounts[accName]!!.isEmpty()) {
                            LogUtil.v("Using token " + accounts[accName])
                            SettingValues.authentication.edit()
                                .putString("lasttoken", accounts[accName])
                                .remove("backedCreds")
                                .apply()
                        } else {
                            val tokens = ArrayList(
                                SettingValues.authentication.getStringSet("tokens", HashSet())
                            )
                            SettingValues.authentication.edit()
                                .putString("lasttoken", tokens[keys.indexOf(accName)])
                                .remove("backedCreds")
                                .apply()
                        }
                        Authentication.name = accName
                        UserSubscriptions.switchAccounts()
                        App.forceRestart(this@MainActivity, true)
                    }
                }
                accountList.addView(t)
            }
            header.findViewById<View>(R.id.godown).setOnClickListener { view ->
                val body = header.findViewById<LinearLayout>(R.id.expand_profile)
                if (body.visibility == View.GONE) {
                    expand(body)
                    AnimatorUtil.flipAnimator(false, view).start()
                    view.findViewById<View>(R.id.godown).contentDescription =
                        resources.getString(R.string.btn_collapse)
                } else {
                    collapse(body)
                    AnimatorUtil.flipAnimator(true, view).start()
                    view.findViewById<View>(R.id.godown).contentDescription =
                        resources.getString(R.string.btn_expand)
                }
            }
            header.findViewById<View>(R.id.guest_mode)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        Authentication.name = "LOGGEDOUT"
                        Authentication.isLoggedIn = false
                        SettingValues.authentication.edit()
                            .remove("lasttoken")
                            .remove("backedCreds")
                            .apply()
                        UserSubscriptions.switchAccounts()
                        App.forceRestart(this@MainActivity, true)
                    }
                })
            header.findViewById<View>(R.id.add)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Login::class.java)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.offline)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        SettingValues.appRestart.edit().putBoolean("forceoffline", true).commit()
                        App.forceRestart(this@MainActivity, false)
                    }
                })
            header.findViewById<View>(R.id.inbox)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Inbox::class.java)
                        this@MainActivity.startActivityForResult(inte, INBOX_RESULT)
                    }
                })
            headerMain = header
            if (runAfterLoad == null) {
                AsyncNotificationBadge().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        } else if (Authentication.didOnline) {
            header = inflater.inflate(R.layout.drawer_loggedout, drawerSubList, false)
            drawerSubList!!.addHeaderView(header, null, false)
            headerMain = header
            hea = header.findViewById(R.id.back)
            val profStuff = header.findViewById<LinearLayout>(R.id.accountsarea)
            profStuff.visibility = View.GONE
            findViewById<View>(R.id.back).setOnClickListener {
                if (profStuff.visibility == View.GONE) {
                    expand(profStuff)
                    AnimatorUtil.flipAnimator(false, header.findViewById(R.id.headerflip)).start()
                    header.findViewById<View>(R.id.headerflip).contentDescription =
                        resources.getString(R.string.btn_collapse)
                } else {
                    collapse(profStuff)
                    AnimatorUtil.flipAnimator(true, header.findViewById(R.id.headerflip)).start()
                    header.findViewById<View>(R.id.headerflip).contentDescription =
                        resources.getString(R.string.btn_expand)
                }
            }
            val accounts = HashMap<String, String>()
            for (s in SettingValues.authentication.getStringSet("accounts", HashSet())!!) {
                if (s.contains(":")) {
                    accounts[s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]] = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                } else {
                    accounts[s] = ""
                }
            }
            val keys = ArrayList(accounts.keys)
            val accountList = header.findViewById<LinearLayout>(R.id.accountsarea)
            for (accName in keys) {
                LogUtil.v(accName)
                val t = layoutInflater.inflate(
                    R.layout.account_textview_white, accountList,
                    false
                )
                (t.findViewById<View>(R.id.name) as TextView?)!!.text = accName
                t.findViewById<View>(R.id.remove).setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.profile_remove)
                        .setMessage(R.string.profile_remove_account)
                        .setNegativeButton(R.string.btn_delete) { dialog2: DialogInterface, which2: Int ->
                            val accounts2 = SettingValues.authentication.getStringSet(
                                "accounts", HashSet()
                            )
                            val done: MutableSet<String> = HashSet()
                            for (s in accounts2!!) {
                                if (!s.contains(accName)) {
                                    done.add(s)
                                }
                            }
                            SettingValues.authentication.edit()
                                .putStringSet("accounts", done)
                                .commit()
                            dialog2.dismiss()
                            accountList.removeView(t)
                            if (accName.equals(Authentication.name, ignoreCase = true)) {
                                var d = false
                                for (s in keys) {
                                    if (!s.equals(accName, ignoreCase = true)) {
                                        d = true
                                        LogUtil.v("Switching to $s")
                                        if (!accounts[s]!!.isEmpty()) {
                                            SettingValues.authentication.edit()
                                                .putString("lasttoken", accounts[s])
                                                .remove("backedCreds")
                                                .commit()
                                        } else {
                                            val tokens = ArrayList(
                                                SettingValues.authentication.getStringSet(
                                                    "tokens", HashSet()
                                                )
                                            )
                                            SettingValues.authentication.edit()
                                                .putString("lasttoken", tokens[keys.indexOf(s)])
                                                .remove("backedCreds")
                                                .commit()
                                        }
                                        Authentication.name = s
                                        UserSubscriptions.switchAccounts()
                                        App.forceRestart(this@MainActivity, true)
                                    }
                                }
                                if (!d) {
                                    Authentication.name = "LOGGEDOUT"
                                    Authentication.isLoggedIn = false
                                    SettingValues.authentication.edit()
                                        .remove("lasttoken")
                                        .remove("backedCreds")
                                        .commit()
                                    UserSubscriptions.switchAccounts()
                                    App.forceRestart(this@MainActivity, true)
                                }
                            } else {
                                accounts.remove(accName)
                                keys.remove(accName)
                            }
                        }
                        .setPositiveButton(R.string.btn_cancel, null)
                        .show()
                }
                t.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        if (!accName.equals(Authentication.name, ignoreCase = true)) {
                            if (!accounts[accName]!!.isEmpty()) {
                                SettingValues.authentication.edit()
                                    .putString("lasttoken", accounts[accName])
                                    .remove("backedCreds")
                                    .commit()
                            } else {
                                val tokens = ArrayList(
                                    SettingValues.authentication.getStringSet("tokens", HashSet())
                                )
                                SettingValues.authentication.edit()
                                    .putString("lasttoken", tokens[keys.indexOf(accName)])
                                    .remove("backedCreds")
                                    .commit()
                            }
                            Authentication.isLoggedIn = true
                            Authentication.name = accName
                            UserSubscriptions.switchAccounts()
                            App.forceRestart(this@MainActivity, true)
                        }
                    }
                })
                accountList.addView(t)
            }
            header.findViewById<View>(R.id.add)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Login::class.java)
                        this@MainActivity.startActivity(inte)
                    }
                })
            header.findViewById<View>(R.id.offline)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        SettingValues.appRestart.edit().putBoolean("forceoffline", true).commit()
                        App.forceRestart(this@MainActivity, false)
                    }
                })
            headerMain = header
            header.findViewById<View>(R.id.multi).setOnClickListener {
                MaterialDialog.Builder(this@MainActivity).inputRange(3, 20)
                    .alwaysCallInputCallback()
                    .input(getString(R.string.user_enter), null) { dialog, input ->
                        val editText = dialog.inputEditText
                        EditTextValidator.validateUsername(editText)
                        if (input.length >= 3 && input.length <= 20) {
                            dialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
                        }
                    }
                    .positiveText(R.string.user_btn_gotomultis)
                    .onPositive { dialog, which ->
                        if (runAfterLoad == null) {
                            val inte = Intent(
                                this@MainActivity,
                                MultiredditOverview::class.java
                            )
                            inte.putExtra(
                                Profile.EXTRA_PROFILE,
                                dialog.inputEditText!!.text.toString()
                            )
                            this@MainActivity.startActivity(inte)
                        }
                    }
                    .negativeText(R.string.btn_cancel)
                    .show()
            }
        } else {
            header = inflater.inflate(R.layout.drawer_offline, drawerSubList, false)
            headerMain = header
            drawerSubList!!.addHeaderView(header, null, false)
            hea = header.findViewById(R.id.back)
            header.findViewById<View>(R.id.online)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        SettingValues.appRestart.edit().remove("forceoffline").commit()
                        App.forceRestart(this@MainActivity, false)
                    }
                })
        }
        val expandSettings = header.findViewById<LinearLayout>(R.id.expand_settings)
        header.findViewById<View>(R.id.godown_settings).setOnClickListener { v ->
            if (expandSettings.visibility == View.GONE) {
                expand(expandSettings)
                header.findViewById<View>(R.id.godown_settings).contentDescription =
                    resources.getString(R.string.btn_collapse)
                AnimatorUtil.flipAnimator(false, v).start()
            } else {
                collapse(expandSettings)
                header.findViewById<View>(R.id.godown_settings).contentDescription =
                    resources.getString(R.string.btn_expand)
                AnimatorUtil.flipAnimator(true, v).start()
            }
        }
        run {
            // Set up quick setting toggles
            val toggleNightMode = expandSettings.findViewById<SwitchCompat>(R.id.toggle_night_mode)
            if (SettingValues.isPro) {
                toggleNightMode.visibility = View.VISIBLE
                toggleNightMode.isChecked = inNightMode
                toggleNightMode.setOnCheckedChangeListener { buttonView, isChecked ->
                    SettingValues.forcedNightModeState =
                        if (isChecked) SettingValues.ForcedState.FORCED_ON else SettingValues.ForcedState.FORCED_OFF
                    restartTheme()
                }
            }
            val toggleImmersiveMode =
                expandSettings.findViewById<SwitchCompat>(R.id.toggle_immersive_mode)
            toggleImmersiveMode.isChecked = SettingValues.immersiveMode
            toggleImmersiveMode.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.immersiveMode = isChecked
                if (isChecked) {
                    hideDecor()
                } else {
                    showDecor()
                }
            }
            val toggleNSFW = expandSettings.findViewById<SwitchCompat>(R.id.toggle_nsfw)
            toggleNSFW.isChecked = SettingValues.showNSFWContent
            toggleNSFW.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.showNSFWContent = isChecked
                reloadSubs()
            }
            val toggleRightThumbnails =
                expandSettings.findViewById<SwitchCompat>(R.id.toggle_right_thumbnails)
            toggleRightThumbnails.isChecked = SettingValues.switchThumb
            toggleRightThumbnails.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.switchThumb = isChecked
                reloadSubs()
            }
            val toggleReaderMode =
                expandSettings.findViewById<SwitchCompat>(R.id.toggle_reader_mode)
            toggleReaderMode.isChecked = SettingValues.readerMode
            toggleReaderMode.setOnCheckedChangeListener { buttonView, isChecked ->
                SettingValues.readerMode = isChecked
            }
        }
        header.findViewById<View>(R.id.manage).setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val i = Intent(this@MainActivity, ManageOfflineContent::class.java)
                startActivity(i)
            }
        })
        if (Authentication.didOnline) {
            val support = header.findViewById<View>(R.id.support)
            if (SettingValues.isPro) {
                support.visibility = View.GONE
            } else {
                support.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        ProUtil.proUpgradeMsg(this@MainActivity, R.string.settings_support_slide)
                            .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                            .show()
                    }
                })
            }
            header.findViewById<View>(R.id.prof).setOnClickListener {
                MaterialDialog.Builder(this@MainActivity).inputRange(3, 20)
                    .alwaysCallInputCallback()
                    .input(getString(R.string.user_enter), null) { dialog, input ->
                        val editText = dialog.inputEditText
                        EditTextValidator.validateUsername(editText)
                        if (input.length >= 3 && input.length <= 20) {
                            dialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
                        }
                    }
                    .positiveText(R.string.user_btn_goto)
                    .onPositive { dialog, which ->
                        val inte = Intent(this@MainActivity, Profile::class.java)
                        inte.putExtra(
                            Profile.EXTRA_PROFILE,
                            dialog.inputEditText!!.text.toString()
                        )
                        this@MainActivity.startActivity(inte)
                    }
                    .negativeText(R.string.btn_cancel)
                    .show()
            }
        }
        header.findViewById<View>(R.id.settings)
            .setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    val i = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(i)
                    // Cancel sub loading because exiting the settings will reload it anyway
                    if (mAsyncGetSubreddit != null) mAsyncGetSubreddit!!.cancel(true)
                    drawerLayout!!.closeDrawers()
                }
            })

        /*  footer.findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent inte = new Intent(Overview.this, Setting.class);
                Overview.this.startActivityForResult(inte, 3);
            }
        });*/
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val actionBarDrawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this@MainActivity,
            drawerLayout,
            toolbar,
            R.string.btn_open,
            R.string.btn_close
        ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, 0f) // this disables the animation
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                if (drawerLayout!!.isDrawerOpen(GravityCompat.END)) {
                    var current = pager!!.currentItem
                    if (current == toOpenComments && toOpenComments != 0) {
                        current -= 1
                    }
                    val compare = usedArray!![current]
                    if (compare == "random" || compare == "myrandom" || compare == "randnsfw") {
                        if (adapter != null && adapter!!.currentFragment != null && ((adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.dataSet.subredditRandom
                                    != null)
                        ) {
                            val sub =
                                (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.dataSet.subredditRandom
                            doSubSidebarNoLoad(sub!!)
                            doSubSidebar(sub!!)
                        }
                    } else {
                        doSubSidebar(usedArray!![current])
                    }
                }
            }

            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                KeyboardUtil.hideKeyboard(this@MainActivity, drawerLayout!!.windowToken, 0)
            }
        }
        drawerLayout!!.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        header.findViewById<View>(R.id.back).setBackgroundColor(Palette.getColor("alsdkfjasld"))
        accountsArea = header.findViewById(R.id.accountsarea)
        if (accountsArea != null) {
            accountsArea!!.setBackgroundColor(Palette.getDarkerColor("alsdkfjasld"))
        }
        setDrawerSubList()
        hideDrawerItems()
    }

    fun hideDrawerItems() {
        for (settingDrawerItem in SettingsDrawerEnum
            .values()) {
            val drawerItem = drawerSubList!!.findViewById<View>(settingDrawerItem.drawerId)
            if (drawerItem != null && drawerItem.visibility == View.VISIBLE && SettingValues.selectedDrawerItems and settingDrawerItem.value == 0L) {
                drawerItem.visibility = View.GONE
            }
        }
    }

    fun doForcePrefs() {
        val domains = HashSet<String>()
        for (s in SettingValues.alwaysExternal.orEmpty()) {
            if (!s.isEmpty()) {
                if (!s.contains("youtu")) domains.add(s.trim { it <= ' ' })
            }
        }

        // Make some domains open externally by default, can be used with Chrome Customtabs if they remove the option in settings
        domains.add("youtube.com")
        domains.add("youtu.be")
        domains.add("play.google.com")
        SettingValues.alwaysExternal = domains
    }

    fun doFriends(friends: List<String?>?) {
        runOnUiThread {
            if (friends != null && !friends.isEmpty() && headerMain!!.findViewById<View?>(R.id.friends) != null) {
                headerMain!!.findViewById<View>(R.id.friends).visibility = View.VISIBLE
                headerMain!!.findViewById<View>(R.id.friends)
                    .setOnClickListener(object : OnSingleClickListener() {
                        override fun onSingleClick(view: View) {
                            MaterialDialog.Builder(this@MainActivity).title("Friends")
                                .items(friends)
                                .itemsCallback { dialog, itemView, which, text ->
                                    val i = Intent(
                                        this@MainActivity,
                                        Profile::class.java
                                    )
                                    i.putExtra(
                                        Profile.EXTRA_PROFILE,
                                        friends[which]
                                    )
                                    startActivity(i)
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    })
            } else if (Authentication.isLoggedIn
                && headerMain!!.findViewById<View?>(R.id.friends) != null
            ) {
                headerMain!!.findViewById<View>(R.id.friends).visibility = View.GONE
            }
        }
    }

    fun doPageSelectedComments(position: Int) {
        pager!!.setSwipeLeftOnly(false)
        header!!.animate().translationY(0f).setInterpolator(LinearInterpolator()).duration = 180
        App.currentPosition = position
        if (position + 1 != currentComment) {
            doSubSidebarNoLoad(usedArray!![position])
        }
        val page = adapter!!.currentFragment as SubmissionsView?
        if (page != null && page.adapter != null) {
            val p = page.adapter!!.dataSet
            if (p.offline && p.cached != null) {
                Toast.makeText(
                    this@MainActivity, getString(
                        R.string.offline_last_update,
                        TimeUtils.getTimeAgo(p.cached!!.time, this@MainActivity)
                    ), Toast.LENGTH_LONG
                )
                    .show()
            }
        }
        if (hea != null) {
            hea!!.setBackgroundColor(Palette.getColor(usedArray!![position]))
            if (accountsArea != null) {
                accountsArea!!.setBackgroundColor(
                    Palette.getDarkerColor(
                        usedArray!![position]
                    )
                )
            }
        }
        header!!.setBackgroundColor(Palette.getColor(usedArray!![position]))
        themeSystemBars(usedArray!![position])
        setRecentBar(usedArray!![position])
        if (SettingValues.single) {
            supportActionBar!!.title = usedArray!![position]
        } else {
            if (mTabLayout != null) {
                mTabLayout!!.setSelectedTabIndicatorColor(
                    ColorPreferences(this@MainActivity).getColor(usedArray!![position])
                )
            }
        }
        selectedSub = usedArray!![position]
    }

    fun doSubOnlyStuff(subreddit: Subreddit) {
        findViewById<View>(R.id.loader).visibility = View.GONE
        canSubmit = if (subreddit.subredditType != null) {
            subreddit.subredditType != Subreddit.Type.RESTRICTED
        } else {
            true
        }
        if (subreddit.sidebar != null && !subreddit.sidebar.isEmpty()) {
            findViewById<View>(R.id.sidebar_text).visibility = View.VISIBLE
            val text = subreddit.dataNode["description_html"].asText().trim { it <= ' ' }
            setViews(text, subreddit.displayName, sidebarBody, sidebarOverflow)

            //get all subs that have Notifications enabled
            val rawSubs =
                StringUtil.stringToArray(SettingValues.appRestart.getString(CheckForMail.SUBS_TO_GET, ""))
            val subThresholds = HashMap<String, Int>()
            for (s in rawSubs) {
                try {
                    val split = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    subThresholds[split[0].lowercase()] = Integer.valueOf(split[1])
                } catch (ignored: Exception) {
                    //do nothing
                }
            }

            //whether or not this subreddit was in the keySet
            val isNotified = subThresholds.containsKey(subreddit.displayName.lowercase())
            findViewById<AppCompatCheckBox>(R.id.notify_posts_state)!!.isChecked = isNotified
        } else {
            findViewById<View>(R.id.sidebar_text).visibility = View.GONE
        }
        run {
            val collection = findViewById<View>(R.id.collection)
            if (Authentication.isLoggedIn) {
                collection.setOnClickListener {
                    object : AsyncTask<Void?, Void?, Void?>() {
                        var multis = HashMap<String?, MultiReddit>()
                        protected override fun doInBackground(vararg params: Void?): Void? {
                            if (UserSubscriptions.multireddits == null) {
                                UserSubscriptions.syncMultiReddits(this@MainActivity)
                            }
                            for (r in UserSubscriptions.multireddits) {
                                multis[r.displayName] = r
                            }
                            return null
                        }

                        override fun onPostExecute(aVoid: Void?) {
                            MaterialDialog.Builder(this@MainActivity).title(
                                getString(
                                    R.string.multi_add_to,
                                    subreddit.displayName
                                )
                            )
                                .items(multis.keys)
                                .itemsCallback { dialog, itemView, which, text ->
                                    object : AsyncTask<Void?, Void?, Void?>() {
                                        protected override fun doInBackground(vararg params: Void?): Void? {
                                            try {
                                                val multiName = multis.keys
                                                    .toTypedArray()[which]
                                                val subs: MutableList<String> = ArrayList()
                                                for (sub in multis[multiName]!!.subreddits) {
                                                    subs.add(sub.displayName)
                                                }
                                                subs.add(subreddit.displayName)
                                                MultiRedditManager(
                                                    Authentication.reddit
                                                ).createOrUpdate(
                                                    MultiRedditUpdateRequest.Builder(
                                                        Authentication.name,
                                                        multiName
                                                    ).subreddits(
                                                        subs
                                                    ).build()
                                                )
                                                UserSubscriptions.syncMultiReddits(
                                                    this@MainActivity
                                                )
                                                runOnUiThread {
                                                    drawerLayout!!.closeDrawers()
                                                    val s = Snackbar.make(
                                                        mToolbar!!,
                                                        getString(
                                                            R.string.multi_subreddit_added,
                                                            multiName
                                                        ),
                                                        Snackbar.LENGTH_LONG
                                                    )
                                                    LayoutUtils.showSnackbar(s)
                                                }
                                            } catch (e: NetworkException) {
                                                runOnUiThread {
                                                    runOnUiThread {
                                                        Snackbar.make(
                                                            mToolbar!!,
                                                            getString(
                                                                R.string.multi_error
                                                            ),
                                                            Snackbar.LENGTH_LONG
                                                        )
                                                            .setAction(R.string.btn_ok, null)
                                                            .show()
                                                    }
                                                }
                                                e.printStackTrace()
                                            } catch (e: ApiException) {
                                                runOnUiThread {
                                                    runOnUiThread {
                                                        Snackbar.make(
                                                            mToolbar!!,
                                                            getString(
                                                                R.string.multi_error
                                                            ),
                                                            Snackbar.LENGTH_LONG
                                                        )
                                                            .setAction(R.string.btn_ok, null)
                                                            .show()
                                                    }
                                                }
                                                e.printStackTrace()
                                            }
                                            return null
                                        }
                                    }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                                }
                                .show()
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            } else {
                collection.visibility = View.GONE
            }
        }
        run {
            val notifyStateCheckBox = findViewById<AppCompatCheckBox>(R.id.notify_posts_state)
            notifyStateCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    val sub = subreddit.displayName
                    if (!sub.equals("all", ignoreCase = true)
                        && !sub.equals("frontpage", ignoreCase = true)
                        && !sub.equals("friends", ignoreCase = true)
                        && !sub.equals("mod", ignoreCase = true)
                        && !sub.contains("+")
                        && !sub.contains(".")
                        && !sub.contains("/m/")
                    ) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.sub_post_notifs_title, sub))
                            .setMessage(R.string.sub_post_notifs_msg)
                            .setPositiveButton(R.string.btn_ok) { dialog, which ->
                                MaterialDialog.Builder(this@MainActivity)
                                    .title(R.string.sub_post_notifs_threshold)
                                    .items(
                                        *arrayOf<String>(
                                            "1", "5", "10", "20", "40", "50"
                                        )
                                    )
                                    .alwaysCallSingleChoiceCallback()
                                    .itemsCallbackSingleChoice(
                                        0
                                    ) { dialog, itemView, which, text ->
                                        val subs = StringUtil.stringToArray(
                                            SettingValues.appRestart
                                                .getString(
                                                    CheckForMail.SUBS_TO_GET,
                                                    ""
                                                )
                                        )
                                        subs.add(
                                            sub
                                                    + ":"
                                                    + text
                                        )
                                        SettingValues.appRestart
                                            .edit()
                                            .putString(
                                                CheckForMail.SUBS_TO_GET,
                                                StringUtil.arrayToString(
                                                    subs
                                                )
                                            )
                                            .commit()
                                        return@itemsCallbackSingleChoice true
                                    }
                                    .cancelable(false)
                                    .show()
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .setNegativeButton(R.string.btn_cancel) { dialog: DialogInterface?, which: Int ->
                                notifyStateCheckBox!!.isChecked = false
                            }
                            .setOnCancelListener { dialog: DialogInterface? ->
                                notifyStateCheckBox!!.isChecked = false
                            }
                            .show()
                    } else {
                        notifyStateCheckBox!!.isChecked = false
                        Toast.makeText(
                            this@MainActivity, R.string.sub_post_notifs_err,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val cancelIntent = Intent(this@MainActivity, CancelSubNotifs::class.java)
                    cancelIntent.putExtra(
                        CancelSubNotifs.EXTRA_SUB,
                        subreddit.displayName
                    )
                    startActivity(cancelIntent)
                }
            }
        }
        run {
            val subscribe = findViewById<TextView>(R.id.subscribe)
            currentlySubbed = !Authentication.isLoggedIn && usedArray!!.contains(
                subreddit.displayName.lowercase()
            ) || subreddit.isUserSubscriber
            MiscUtil.doSubscribeButtonText(currentlySubbed, subscribe)
            assert(subscribe != null)
            subscribe!!.setOnClickListener(object : View.OnClickListener {
                private fun doSubscribe() {
                    if (Authentication.isLoggedIn) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.subscribe_to, subreddit.displayName))
                            .setPositiveButton(R.string.reorder_add_subscribe) { dialog: DialogInterface?, which: Int ->
                                object : AsyncTask<Void?, Void?, Boolean?>() {
                                    public override fun onPostExecute(success: Boolean?) {
                                        if (!success!!) { // If subreddit was removed from account or not
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle(R.string.force_change_subscription)
                                                .setMessage(R.string.force_change_subscription_desc)
                                                .setPositiveButton(R.string.btn_yes) { dialog1: DialogInterface?, which1: Int ->
                                                    changeSubscription(
                                                        subreddit,
                                                        true
                                                    ) // Force add the subscription
                                                    val s = Snackbar.make(
                                                        mToolbar!!,
                                                        getString(R.string.misc_subscribed),
                                                        Snackbar.LENGTH_LONG
                                                    )
                                                    LayoutUtils.showSnackbar(s)
                                                }
                                                .setNegativeButton(R.string.btn_no, null)
                                                .setCancelable(false)
                                                .show()
                                        } else {
                                            changeSubscription(subreddit, true)
                                        }
                                    }

                                    protected override fun doInBackground(
                                        vararg params: Void?
                                    ): Boolean? {
                                        try {
                                            AccountManager(
                                                Authentication.reddit
                                            ).subscribe(
                                                subreddit
                                            )
                                        } catch (e: NetworkException) {
                                            return false // Either network crashed or trying to unsubscribe to a subreddit that the account isn't subscribed to
                                        }
                                        return true
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                            }
                            .setNeutralButton(R.string.btn_add_to_sublist) { dialog: DialogInterface?, which: Int ->
                                changeSubscription(subreddit, true) // Force add the subscription
                                val s = Snackbar.make(
                                    mToolbar!!, R.string.sub_added,
                                    Snackbar.LENGTH_LONG
                                )
                                LayoutUtils.showSnackbar(s)
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    } else {
                        changeSubscription(subreddit, true)
                    }
                }

                private fun doUnsubscribe() {
                    if (Authentication.didOnline) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.unsubscribe_from, subreddit.displayName))
                            .setPositiveButton(R.string.reorder_remove_unsubscribe) { dialog: DialogInterface?, which: Int ->
                                object : AsyncTask<Void?, Void?, Boolean?>() {
                                    public override fun onPostExecute(success: Boolean?) {
                                        if (!success!!) { // If subreddit was removed from account or not
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle(R.string.force_change_subscription)
                                                .setMessage(R.string.force_change_subscription_desc)
                                                .setPositiveButton(R.string.btn_yes) { dialog12: DialogInterface?, which12: Int ->
                                                    changeSubscription(
                                                        subreddit,
                                                        false
                                                    ) // Force add the subscription
                                                    val s = Snackbar.make(
                                                        mToolbar!!,
                                                        getString(R.string.misc_unsubscribed),
                                                        Snackbar.LENGTH_LONG
                                                    )
                                                    LayoutUtils.showSnackbar(s)
                                                }
                                                .setNegativeButton(R.string.btn_no, null)
                                                .setCancelable(false)
                                                .show()
                                        } else {
                                            changeSubscription(subreddit, false)
                                        }
                                    }

                                    protected override fun doInBackground(
                                        vararg params: Void?
                                    ): Boolean? {
                                        try {
                                            AccountManager(
                                                Authentication.reddit
                                            ).unsubscribe(
                                                subreddit
                                            )
                                        } catch (e: NetworkException) {
                                            return false // Either network crashed or trying to unsubscribe to a subreddit that the account isn't subscribed to
                                        }
                                        return true
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                            }
                            .setNeutralButton(R.string.just_unsub) { dialog: DialogInterface?, which: Int ->
                                changeSubscription(subreddit, false) // Force add the subscription
                                val s = Snackbar.make(
                                    mToolbar!!,
                                    R.string.misc_unsubscribed,
                                    Snackbar.LENGTH_LONG
                                )
                                LayoutUtils.showSnackbar(s)
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    } else {
                        changeSubscription(subreddit, false)
                    }
                }

                override fun onClick(v: View) {
                    if (!currentlySubbed) {
                        doSubscribe()
                    } else {
                        doUnsubscribe()
                    }
                    MiscUtil.doSubscribeButtonText(currentlySubbed, subscribe)
                }
            })
        }
        if (!subreddit.publicDescription.isEmpty()) {
            findViewById<View>(R.id.sub_title).visibility = View.VISIBLE
            setViews(
                subreddit.dataNode["public_description_html"].asText(),
                subreddit.displayName.lowercase(),
                findViewById<SpoilerRobotoTextView>(R.id.sub_title),
                findViewById<CommentOverflow>(R.id.sub_title_overflow)
            )
        } else {
            findViewById<View>(R.id.sub_title).visibility = View.GONE
        }
        findViewById<ImageView>(R.id.subimage)?.setImageResource(0)
        if (subreddit.dataNode.has("icon_img") && !subreddit.dataNode["icon_img"]
                .asText()
                .isEmpty()
        ) {
            findViewById<View>(R.id.subimage).visibility = View.VISIBLE
            (application as App).imageLoader!!
                .displayImage(
                    subreddit.dataNode["icon_img"].asText(),
                    findViewById<ImageView>(R.id.subimage)
                )
        } else {
            findViewById<View>(R.id.subimage).visibility = View.GONE
        }
        val bannerImage = subreddit.bannerImage
        if (bannerImage != null && !bannerImage.isEmpty()) {
            findViewById<View>(R.id.sub_banner).visibility = View.VISIBLE
            (application as App).imageLoader!!
                .displayImage(
                    bannerImage,
                    findViewById<ImageView>(R.id.sub_banner)
                )
        } else {
            findViewById<View>(R.id.sub_banner).visibility = View.GONE
        }
        findViewById<TextView>(R.id.subscribers)!!.text = getString(
            R.string.subreddit_subscribers_string,
            subreddit.localizedSubscriberCount
        )
        findViewById<View>(R.id.subscribers).visibility = View.VISIBLE
        findViewById<TextView>(R.id.active_users)!!.text = getString(
            R.string.subreddit_active_users_string_new,
            subreddit.localizedAccountsActive
        )
        findViewById<View>(R.id.active_users).visibility = View.VISIBLE
    }

    var sorts: Sorting? = null
    fun doSubSidebar(subreddit: String) {
        if (mAsyncGetSubreddit != null) {
            mAsyncGetSubreddit!!.cancel(true)
        }
        findViewById<View>(R.id.loader).visibility = View.VISIBLE
        invalidateOptionsMenu()
        if (!subreddit.equals("all", ignoreCase = true)
            && !subreddit.equals("frontpage", ignoreCase = true)
            && !subreddit.equals("friends", ignoreCase = true)
            && !subreddit.equals("mod", ignoreCase = true)
            && !subreddit.contains("+")
            && !subreddit.contains(".")
            && !subreddit.contains("/m/")
        ) {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
            }
            mAsyncGetSubreddit = AsyncGetSubreddit()
            mAsyncGetSubreddit!!.execute(subreddit)
            val dialoglayout: View = findViewById(R.id.sidebarsub)
            run {
                val submit = dialoglayout.findViewById<View>(R.id.submit)
                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    submit.visibility = View.GONE
                }
                if (SettingValues.fab && SettingValues.fabType == Constants.FAB_POST) {
                    submit.visibility = View.GONE
                }
                submit.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@MainActivity, Submit::class.java)
                        if (!subreddit.contains("/m/") && canSubmit) {
                            inte.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                        }
                        this@MainActivity.startActivity(inte)
                    }
                })
            }
            dialoglayout.findViewById<View>(R.id.wiki).setOnClickListener {
                val i = Intent(this@MainActivity, Wiki::class.java)
                i.putExtra(Wiki.EXTRA_SUBREDDIT, subreddit)
                startActivity(i)
            }
            dialoglayout.findViewById<View>(R.id.syncflair)
                .setOnClickListener { ImageFlairs.syncFlairs(this@MainActivity, subreddit) }
            dialoglayout.findViewById<View>(R.id.submit).setOnClickListener {
                val i = Intent(this@MainActivity, Submit::class.java)
                if ((!subreddit.contains("/m/") || !subreddit.contains(".")) && canSubmit) {
                    i.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                }
                startActivity(i)
            }
            val sort = dialoglayout.findViewById<TextView>(R.id.sort)
            var sortingis = Sorting.HOT
            if (SettingValues.hasSort(subreddit)) {
                sortingis = SettingValues.getBaseSubmissionSort(subreddit)
                sort.text = (sortingis.name
                        + if (sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP) " of "
                        + SettingValues.getBaseTimePeriod(subreddit).name else "")
            } else {
                sort.text = "Set default sorting"
            }
            val sortid = SortingUtil.getSortingId(sortingis)
            dialoglayout.findViewById<View>(R.id.sorting).setOnClickListener(View.OnClickListener {
                val l2 = DialogInterface.OnClickListener { dialogInterface, i ->
                    when (i) {
                        0 -> sorts = Sorting.HOT
                        1 -> sorts = Sorting.NEW
                        2 -> sorts = Sorting.RISING
                        3 -> {
                            sorts = Sorting.TOP
                            askTimePeriod(sorts!!, subreddit, dialoglayout)
                            return@OnClickListener
                        }

                        4 -> {
                            sorts = Sorting.CONTROVERSIAL
                            askTimePeriod(sorts!!, subreddit, dialoglayout)
                            return@OnClickListener
                        }
                    }
                    SettingValues.setSubSorting(sorts!!, time, subreddit)
                    val sortingis = SettingValues.getBaseSubmissionSort(subreddit)
                    sort.text = (sortingis.name
                            + if (sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP) " of "
                            + SettingValues.getBaseTimePeriod(subreddit).name else "")
                    reloadSubs()
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.sorting_choose)
                    .setSingleChoiceItems(SortingUtil.getSortingStrings(), sortid, l2)
                    .setNegativeButton("Reset default sorting") { dialog: DialogInterface?, which: Int ->
                        SettingValues.clearSort(subreddit)
                        val sort1 = dialoglayout.findViewById<TextView>(R.id.sort)
                        if (SettingValues.hasSort(subreddit)) {
                            val sortingis1 = SettingValues.getBaseSubmissionSort(subreddit)
                            sort1.text = (sortingis1.name
                                    + if (sortingis1 == Sorting.CONTROVERSIAL || sortingis1 == Sorting.TOP) " of "
                                    + SettingValues.getBaseTimePeriod(subreddit).name else "")
                        } else {
                            sort1.text = "Set default sorting"
                        }
                        reloadSubs()
                    }
                    .show()
            })
            dialoglayout.findViewById<View>(R.id.theme).setOnClickListener {
                val style = ColorPreferences(this@MainActivity).getThemeSubreddit(subreddit)
                val contextThemeWrapper: Context = ContextThemeWrapper(this@MainActivity, style)
                val localInflater = layoutInflater.cloneInContext(contextThemeWrapper)
                val dialoglayout = localInflater.inflate(R.layout.colorsub, null)
                val arrayList = ArrayList<String>()
                arrayList.add(subreddit)
                SettingsSubAdapter.showSubThemeEditor(
                    arrayList, this@MainActivity,
                    dialoglayout
                )
            }
            dialoglayout.findViewById<View>(R.id.mods).setOnClickListener {
                val d: Dialog = MaterialDialog.Builder(this@MainActivity).title(
                    R.string.sidebar_findingmods
                )
                    .cancelable(true)
                    .content(R.string.misc_please_wait)
                    .progress(true, 100)
                    .show()
                object : AsyncTask<Void?, Void?, Void?>() {
                    var mods: ArrayList<UserRecord>? = null
                    protected override fun doInBackground(vararg params: Void?): Void? {
                        mods = ArrayList()
                        val paginator = UserRecordPaginator(
                            Authentication.reddit, subreddit,
                            "moderators"
                        )
                        paginator.sorting = Sorting.HOT
                        paginator.timePeriod = TimePeriod.ALL
                        while (paginator.hasNext()) {
                            mods!!.addAll(paginator.next())
                        }
                        return null
                    }

                    override fun onPostExecute(aVoid: Void?) {
                        val names = ArrayList<String?>()
                        for (rec in mods!!) {
                            names.add(rec.fullName)
                        }
                        d.dismiss()
                        MaterialDialog.Builder(this@MainActivity).title(
                            getString(R.string.sidebar_submods, subreddit)
                        )
                            .items(names)
                            .itemsCallback { dialog, itemView, which, text ->
                                val i = Intent(this@MainActivity, Profile::class.java)
                                i.putExtra(Profile.EXTRA_PROFILE, names[which])
                                startActivity(i)
                            }
                            .positiveText(R.string.btn_message)
                            .onPositive { dialog, which ->
                                val i = Intent(
                                    this@MainActivity,
                                    SendMessage::class.java
                                )
                                i.putExtra(SendMessage.EXTRA_NAME, "/r/$subreddit")
                                startActivity(i)
                            }
                            .show()
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
            dialoglayout.findViewById<View>(R.id.flair).visibility = View.GONE
            if (Authentication.didOnline && Authentication.isLoggedIn) {
                if (currentFlair != null) currentFlair!!.cancel(true)
                currentFlair = object : AsyncTask<View?, Void?, View?>() {
                    var flairs: List<FlairTemplate>? = null
                    var flairText: ArrayList<String?>? = null
                    var current: String? = null
                    var m: AccountManager? = null
                    protected override fun doInBackground(vararg params: View?): View? {
                        try {
                            m = AccountManager(Authentication.reddit)
                            val node = m!!.getFlairChoicesRootNode(subreddit, null)
                            flairs = m!!.getFlairChoices(subreddit, node)
                            val currentF = m!!.getCurrentFlair(subreddit, node)
                            if (currentF != null) {
                                current = if (currentF.text.isEmpty()) {
                                    "[" + currentF.cssClass + "]"
                                } else {
                                    currentF.text
                                }
                            }
                            flairText = ArrayList()
                            for (temp in flairs!!) {
                                if (temp.text.isEmpty()) {
                                    flairText!!.add("[" + temp.cssClass + "]")
                                } else {
                                    flairText!!.add(temp.text)
                                }
                            }
                        } catch (e1: Exception) {
                            e1.printStackTrace()
                        }
                        return params[0]
                    }

                    override fun onPostExecute(flair: View?) {
                        if (flairs != null && !flairs!!.isEmpty() && flairText != null && !flairText!!.isEmpty()) {
                            flair!!.visibility = View.VISIBLE
                            if (current != null) {
                                (dialoglayout.findViewById<View>(R.id.flair_text) as TextView?)!!.text =
                                    getString(R.string.sidebar_flair, current)
                            }
                            flair!!.setOnClickListener {
                                MaterialDialog.Builder(this@MainActivity).items(flairText!!)
                                    .title(R.string.sidebar_select_flair)
                                    .itemsCallback { dialog, itemView, which, text ->
                                        val t = flairs!![which]
                                        if (t.isTextEditable) {
                                            MaterialDialog.Builder(
                                                this@MainActivity
                                            ).title(
                                                R.string.sidebar_select_flair_text
                                            )
                                                .input(
                                                    getString(
                                                        R.string.mod_flair_hint
                                                    ),
                                                    t.text, true
                                                ) { dialog1: MaterialDialog?, input: CharSequence? -> }
                                                .positiveText(R.string.btn_set)
                                                .onPositive { dialog, which ->
                                                    val flair = dialog.inputEditText!!
                                                        .text
                                                        .toString()
                                                    object : AsyncTask<Void?, Void?, Boolean>() {
                                                        protected override fun doInBackground(
                                                            vararg params: Void?
                                                        ): Boolean {
                                                            return try {
                                                                ModerationManager(
                                                                    Authentication.reddit
                                                                )
                                                                    .setFlair(
                                                                        subreddit,
                                                                        t,
                                                                        flair,
                                                                        Authentication.name
                                                                    )
                                                                val currentF = m!!.getCurrentFlair(
                                                                    subreddit
                                                                )
                                                                current = if (currentF
                                                                        .text
                                                                        .isEmpty()
                                                                ) {
                                                                    ("["
                                                                            + currentF
                                                                        .cssClass
                                                                            + "]")
                                                                } else {
                                                                    currentF
                                                                        .text
                                                                }
                                                                true
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                                false
                                                            }
                                                        }

                                                        override fun onPostExecute(
                                                            done: Boolean
                                                        ) {
                                                            val s: Snackbar
                                                            if (done) {
                                                                if (current
                                                                    != null
                                                                ) {
                                                                    (dialoglayout
                                                                        .findViewById<View>(
                                                                            R.id.flair_text
                                                                        ) as TextView?)!!.text =
                                                                        getString(
                                                                            R.string.sidebar_flair,
                                                                            current
                                                                        )
                                                                }
                                                                s = Snackbar.make(
                                                                    mToolbar!!,
                                                                    R.string.snackbar_flair_success,
                                                                    Snackbar.LENGTH_SHORT
                                                                )
                                                            } else {
                                                                s = Snackbar.make(
                                                                    mToolbar!!,
                                                                    R.string.snackbar_flair_error,
                                                                    Snackbar.LENGTH_SHORT
                                                                )
                                                            }
                                                            if (s
                                                                != null
                                                            ) {
                                                                LayoutUtils.showSnackbar(s)
                                                            }
                                                        }
                                                    }.executeOnExecutor(
                                                        THREAD_POOL_EXECUTOR
                                                    )
                                                }
                                                .negativeText(R.string.btn_cancel)
                                                .show()
                                        } else {
                                            object : AsyncTask<Void?, Void?, Boolean>() {
                                                protected override fun doInBackground(
                                                    vararg params: Void?
                                                ): Boolean {
                                                    return try {
                                                        ModerationManager(
                                                            Authentication.reddit
                                                        ).setFlair(
                                                            subreddit, t, null,
                                                            Authentication.name
                                                        )
                                                        val currentF = m!!.getCurrentFlair(
                                                            subreddit
                                                        )
                                                        current = if (currentF.text
                                                                .isEmpty()
                                                        ) {
                                                            ("["
                                                                    + currentF.cssClass
                                                                    + "]")
                                                        } else {
                                                            currentF.text
                                                        }
                                                        true
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        false
                                                    }
                                                }

                                                override fun onPostExecute(
                                                    done: Boolean
                                                ) {
                                                    val s: Snackbar
                                                    if (done) {
                                                        if (current != null) {
                                                            (dialoglayout.findViewById<View>(
                                                                R.id.flair_text
                                                            ) as TextView?)!!.text = getString(
                                                                R.string.sidebar_flair,
                                                                current
                                                            )
                                                        }
                                                        s = Snackbar.make(
                                                            mToolbar!!,
                                                            R.string.snackbar_flair_success,
                                                            Snackbar.LENGTH_SHORT
                                                        )
                                                    } else {
                                                        s = Snackbar.make(
                                                            mToolbar!!,
                                                            R.string.snackbar_flair_error,
                                                            Snackbar.LENGTH_SHORT
                                                        )
                                                    }
                                                    if (s != null) {
                                                        LayoutUtils.showSnackbar(s)
                                                    }
                                                }
                                            }.executeOnExecutor(
                                                THREAD_POOL_EXECUTOR
                                            )
                                        }
                                    }
                                    .show()
                            }
                        }
                    }
                }
                currentFlair!!.execute(dialoglayout.findViewById<View>(R.id.flair))
            }
        } else {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.END
                )
            }
        }
    }

    var time = TimePeriod.DAY
    private fun askTimePeriod(sort: Sorting, sub: String, dialoglayout: View) {
        val l2 = DialogInterface.OnClickListener { dialogInterface, i ->
            when (i) {
                0 -> time = TimePeriod.HOUR
                1 -> time = TimePeriod.DAY
                2 -> time = TimePeriod.WEEK
                3 -> time = TimePeriod.MONTH
                4 -> time = TimePeriod.YEAR
                5 -> time = TimePeriod.ALL
            }
            SettingValues.setSubSorting(sort, time, sub)
            SortingUtil.setSorting(sub, sort)
            SortingUtil.setTime(sub, time)
            val sort = dialoglayout.findViewById<TextView>(R.id.sort)
            if (SettingValues.hasSort(sub)) {
                val sortingis = SettingValues.getBaseSubmissionSort(sub)
                sort.text = (sortingis.name
                        + if (sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP) " of "
                        + SettingValues.getBaseTimePeriod(sub).name else "")
            } else {
                sort.text = "Set default sorting"
            }
            reloadSubs()
        }
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.sorting_choose)
            .setSingleChoiceItems(
                SortingUtil.getSortingTimesStrings(),
                SortingUtil.getSortingTimeId(""),
                l2
            )
            .show()
    }

    fun doSubSidebarNoLoad(subreddit: String) {
        if (mAsyncGetSubreddit != null) {
            mAsyncGetSubreddit!!.cancel(true)
        }
        findViewById<View>(R.id.loader).visibility = View.GONE
        invalidateOptionsMenu()
        if (!subreddit.equals("all", ignoreCase = true)
            && !subreddit.equals("frontpage", ignoreCase = true)
            && !subreddit.equals("friends", ignoreCase = true)
            && !subreddit.equals("mod", ignoreCase = true)
            && !subreddit.contains("+")
            && !subreddit.contains(".")
            && !subreddit.contains("/m/")
        ) {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
            }
            findViewById<View>(R.id.sidebar_text).visibility = View.GONE
            findViewById<View>(R.id.sub_title).visibility = View.GONE
            findViewById<View>(R.id.subscribers).visibility = View.GONE
            findViewById<View>(R.id.active_users).visibility = View.GONE
            findViewById<View>(R.id.header_sub).setBackgroundColor(Palette.getColor(subreddit))
            findViewById<TextView>(R.id.sub_infotitle)!!.text = subreddit

            //Sidebar buttons should use subreddit's accent color
            val subColor = ColorPreferences(this).getColor(subreddit)
            findViewById<TextView>(R.id.theme_text)?.setTextColor(subColor)
            findViewById<TextView>(R.id.wiki_text)?.setTextColor(subColor)
            findViewById<TextView>(R.id.post_text)?.setTextColor(subColor)
            findViewById<TextView>(R.id.mods_text)?.setTextColor(subColor)
            findViewById<TextView>(R.id.flair_text)?.setTextColor(subColor)
            (drawerLayout!!.findViewById<View>(R.id.sorting)
                .findViewById<View>(R.id.sort) as TextView?)?.setTextColor(subColor)
            findViewById<TextView>(R.id.sync)?.setTextColor(subColor)
        } else {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.END
                )
            }
        }
    }

    /**
     * Starts the enter animations for various UI components of the toolbar subreddit search
     *
     * @param ANIMATION_DURATION     duration of the animation in ms
     * @param SUGGESTIONS_BACKGROUND background of subreddit suggestions list
     * @param GO_TO_SUB_FIELD        search field in toolbar
     * @param CLOSE_BUTTON           button that clears the search and closes the search UI
     */
    fun enterAnimationsForToolbarSearch(
        ANIMATION_DURATION: Long,
        SUGGESTIONS_BACKGROUND: CardView, GO_TO_SUB_FIELD: AutoCompleteTextView,
        CLOSE_BUTTON: ImageView
    ) {
        SUGGESTIONS_BACKGROUND.animate()
            .translationY(headerHeight.toFloat())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION + ANIMATE_DURATION_OFFSET)
            .start()
        GO_TO_SUB_FIELD.animate()
            .alpha(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION)
            .start()
        CLOSE_BUTTON.animate()
            .alpha(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION)
            .start()
    }

    /**
     * Starts the exit animations for various UI components of the toolbar subreddit search
     *
     * @param ANIMATION_DURATION     duration of the animation in ms
     * @param SUGGESTIONS_BACKGROUND background of subreddit suggestions list
     * @param GO_TO_SUB_FIELD        search field in toolbar
     * @param CLOSE_BUTTON           button that clears the search and closes the search UI
     */
    fun exitAnimationsForToolbarSearch(
        ANIMATION_DURATION: Long,
        SUGGESTIONS_BACKGROUND: CardView, GO_TO_SUB_FIELD: AutoCompleteTextView,
        CLOSE_BUTTON: ImageView
    ) {
        SUGGESTIONS_BACKGROUND.animate()
            .translationY(-SUGGESTIONS_BACKGROUND.height.toFloat())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION + ANIMATE_DURATION_OFFSET)
            .start()
        GO_TO_SUB_FIELD.animate()
            .alpha(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION)
            .start()
        CLOSE_BUTTON.animate()
            .alpha(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(ANIMATION_DURATION)
            .start()

//Helps smooth the transition between the toolbar title being reset and the search elements
//fading out.
        val OFFSET_ANIM = if (ANIMATION_DURATION == 0L) 0 else ANIMATE_DURATION_OFFSET

        //Hide the various UI components after the animations are complete and
        //reset the toolbar title
        Handler().postDelayed({
            SUGGESTIONS_BACKGROUND.visibility = View.GONE
            GO_TO_SUB_FIELD.visibility = View.GONE
            CLOSE_BUTTON.visibility = View.GONE
            if (SettingValues.single) {
                supportActionBar!!.title = selectedSub
            } else {
                supportActionBar!!.title = tabViewModeTitle
            }
        }, ANIMATION_DURATION + ANIMATE_DURATION_OFFSET)
    }

    fun filterContent(subreddit: String?) {
        val chosen = booleanArrayOf(
            PostMatch.isImage(subreddit!!.lowercase()),
            PostMatch.isAlbums(subreddit.lowercase()),
            PostMatch.isGif(subreddit.lowercase()),
            PostMatch.isVideo(subreddit.lowercase()),
            PostMatch.isUrls(subreddit.lowercase()),
            PostMatch.isSelftext(subreddit.lowercase()),
            PostMatch.isNsfw(subreddit.lowercase())
        )
        val currentSubredditName = usedArray!![App.currentPosition]

        //Title of the filter dialog
        val filterTitle: String
        filterTitle = if (currentSubredditName.contains("/m/")) {
            getString(R.string.content_to_hide, currentSubredditName)
        } else {
            if (currentSubredditName == "frontpage") {
                getString(R.string.content_to_hide, "frontpage")
            } else {
                getString(R.string.content_to_hide, "/r/$currentSubredditName")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(filterTitle)
            .setMultiChoiceItems(
                arrayOf(
                    getString(R.string.image_downloads), getString(R.string.type_albums),
                    getString(R.string.type_gifs), getString(R.string.type_videos),
                    getString(R.string.type_links), getString(R.string.type_selftext),
                    getString(R.string.type_nsfw_content)
                ),
                chosen
            ) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                chosen[which] = isChecked
            }
            .setPositiveButton(R.string.btn_save) { dialog: DialogInterface?, which: Int ->
                PostMatch.setChosen(chosen, subreddit)
                reloadSubs()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    val currentPage: Int
        get() {
            var position = 0
            val currentOrientation = resources.configuration.orientation
            if (adapter!!.currentFragment == null) {
                return 0
            }
            if ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is LinearLayoutManager
                && currentOrientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                position =
                    ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager as LinearLayoutManager?)!!
                        .findFirstCompletelyVisibleItemPosition() - 1
            } else if ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is CatchStaggeredGridLayoutManager) {
                var firstVisibleItems: IntArray? = null
                firstVisibleItems = ((adapter!!.currentFragment as SubmissionsView?)!!.rv
                    !!.layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstCompletelyVisibleItemPositions(
                    firstVisibleItems
                )
                if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                    position = firstVisibleItems[0] - 1
                }
            } else {
                position =
                    ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager as PreCachingLayoutManager?)!!
                        .findFirstCompletelyVisibleItemPosition() - 1
            }
            return position
        }

    fun openPopup() {
        val popup = PopupMenu(this@MainActivity, findViewById(R.id.anchor), Gravity.RIGHT)
        val id = ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id
        val base = SortingUtil.getSortingSpannables(id)
        for (s in base) {
            // Do not add option for "Best" in any subreddit except for the frontpage.
            if (id != "frontpage" && s.toString() == getString(R.string.sorting_best)) {
                continue
            }
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s in base) {
                if (s == item.title) {
                    break
                }
                i++
            }
            when (i) {
                0 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.HOT
                    )
                    reloadSubs()
                }

                1 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.NEW
                    )
                    reloadSubs()
                }

                2 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.RISING
                    )
                    reloadSubs()
                }

                3 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.TOP
                    )
                    openPopupTime()
                }

                4 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.CONTROVERSIAL
                    )
                    openPopupTime()
                }

                5 -> {
                    SortingUtil.setSorting(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        Sorting.BEST
                    )
                    reloadSubs()
                }
            }
            true
        }
        popup.show()
    }

    fun openPopupTime() {
        val popup = PopupMenu(this@MainActivity, findViewById(R.id.anchor), Gravity.RIGHT)
        val id = ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id
        val base = SortingUtil.getSortingTimesSpannables(id)
        for (s in base) {
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s in base) {
                if (s == item.title) {
                    break
                }
                i++
            }
            when (i) {
                0 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.HOUR
                    )
                    reloadSubs()
                }

                1 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.DAY
                    )
                    reloadSubs()
                }

                2 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.WEEK
                    )
                    reloadSubs()
                }

                3 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.MONTH
                    )
                    reloadSubs()
                }

                4 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.YEAR
                    )
                    reloadSubs()
                }

                5 -> {
                    SortingUtil.setTime(
                        ((pager!!.adapter as MainPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.id,
                        TimePeriod.ALL
                    )
                    reloadSubs()
                }
            }
            true
        }
        popup.show()
    }

    fun reloadSubs() {
        var current = pager!!.currentItem
        if (commentPager && current == currentComment) {
            current = current - 1
        }
        if (current < 0) {
            current = 0
        }
        reloadItemNumber = current
        if (adapter is MainPagerAdapterComment) {
            pager!!.adapter = null
            adapter = MainPagerAdapterComment(supportFragmentManager)
        } else {
            adapter = MainPagerAdapter(supportFragmentManager)
        }
        pager!!.adapter = adapter
        reloadItemNumber = -2
        shouldLoad = usedArray!![current]
        pager!!.currentItem = current
        if (mTabLayout != null) {
            mTabLayout!!.setupWithViewPager(pager)
            LayoutUtils.scrollToTabAfterLayout(mTabLayout, current)
        }
        if (SettingValues.single) {
            supportActionBar!!.title = shouldLoad
        }
        setToolbarClick()
        if (SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
            || SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_BOTH
        ) {
            setupSubredditSearchToolbar()
        }
    }

    fun resetAdapter() {
        if (UserSubscriptions.hasSubs()) {
            runOnUiThread {
                usedArray = CaseInsensitiveArrayList(
                    UserSubscriptions.getSubscriptions(this@MainActivity)
                )
                adapter = MainPagerAdapter(supportFragmentManager)
                pager!!.adapter = adapter
                if (mTabLayout != null) {
                    mTabLayout!!.setupWithViewPager(pager)
                    LayoutUtils.scrollToTabAfterLayout(mTabLayout, usedArray!!.indexOf(subToDo))
                }
                setToolbarClick()
                pager!!.currentItem = usedArray!!.indexOf(subToDo)
                val color = Palette.getColor(subToDo)
                hea!!.setBackgroundColor(color)
                header!!.setBackgroundColor(color)
                if (accountsArea != null) {
                    accountsArea!!.setBackgroundColor(Palette.getDarkerColor(color))
                }
                themeSystemBars(subToDo)
                setRecentBar(subToDo)
            }
        }
    }

    fun restartTheme() {
        isRestart = true
        restartPage = currentPage
        val intent = this.intent
        var page = pager!!.currentItem
        if (currentComment == page) page -= 1
        intent.putExtra(EXTRA_PAGE_TO, page)
        finish()
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_real, R.anim.fading_out_real)
    }

    fun saveOffline(submissions: List<IPost>?, subreddit: String) {
        val chosen = BooleanArray(2)
        AlertDialog.Builder(this)
            .setTitle(R.string.save_for_offline_viewing)
            .setMultiChoiceItems(
                arrayOf(getString(R.string.type_gifs)),
                booleanArrayOf(false)
            ) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                chosen[which] = isChecked
            }
            .setPositiveButton(R.string.btn_save) { dialog: DialogInterface?, which: Int ->
                caching = CommentCacheAsync(
                    submissions?.map { it.submission }, this@MainActivity, subreddit,
                    chosen
                ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
            .setPositiveButton(R.string.btn_save) { dialog: DialogInterface?, which: Int ->
                val service = Executors.newSingleThreadExecutor()
                CommentCacheAsync(
                    submissions?.map { it.submission }, this@MainActivity, subreddit,
                    chosen
                ).executeOnExecutor(service)
            }
            .show()
    }

    fun scrollToTop() {
        var pastVisiblesItems = 0
        if (adapter!!.currentFragment == null) return
        val firstVisibleItems = ((adapter!!.currentFragment as SubmissionsView?)!!.rv
            !!.layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(null)
        if (firstVisibleItems != null && firstVisibleItems.size > 0) {
            for (firstVisibleItem in firstVisibleItems) {
                pastVisiblesItems = firstVisibleItem
            }
        }
        if (pastVisiblesItems > 8) {
            (adapter!!.currentFragment as SubmissionsView?)!!.rv!!.scrollToPosition(0)
            header!!.animate()
                .translationY(header!!.height.toFloat())
                .setInterpolator(LinearInterpolator()).duration = 0
        } else {
            (adapter!!.currentFragment as SubmissionsView?)!!.rv!!.smoothScrollToPosition(0)
        }
        (adapter!!.currentFragment as SubmissionsView?)!!.resetScroll()
    }

    fun setDataSet(data: List<String>?) {
        if (data != null && !data.isEmpty()) {
            usedArray = CaseInsensitiveArrayList(data)
            if (adapter == null) {
                adapter = if (commentPager && singleMode) {
                    MainPagerAdapterComment(supportFragmentManager)
                } else {
                    MainPagerAdapter(supportFragmentManager)
                }
            } else {
                adapter!!.notifyDataSetChanged()
            }
            pager!!.adapter = adapter
            pager!!.offscreenPageLimit = 1
            if (toGoto == -1) {
                toGoto = 0
            }
            if (toGoto >= usedArray!!.size) {
                toGoto -= 1
            }
            shouldLoad = usedArray!![toGoto]
            selectedSub = usedArray!![toGoto]
            themeSystemBars(usedArray!![toGoto])
            val USEDARRAY_0 = usedArray!![0]
            header!!.setBackgroundColor(Palette.getColor(USEDARRAY_0))
            if (hea != null) {
                hea!!.setBackgroundColor(Palette.getColor(USEDARRAY_0))
                if (accountsArea != null) {
                    accountsArea!!.setBackgroundColor(Palette.getDarkerColor(USEDARRAY_0))
                }
            }
            if (!SettingValues.single) {
                mTabLayout!!.setSelectedTabIndicatorColor(
                    ColorPreferences(this@MainActivity).getColor(USEDARRAY_0)
                )
                pager!!.currentItem = toGoto
                mTabLayout!!.setupWithViewPager(pager)
                if (mTabLayout != null) {
                    mTabLayout!!.setupWithViewPager(pager)
                    LayoutUtils.scrollToTabAfterLayout(mTabLayout, toGoto)
                }
            } else {
                supportActionBar!!.title = usedArray!![toGoto]
                pager!!.currentItem = toGoto
            }
            setToolbarClick()
            setRecentBar(usedArray!![toGoto])
            doSubSidebarNoLoad(usedArray!![toGoto])
        } else if (NetworkUtil.isConnected(this)) {
            UserSubscriptions.doMainActivitySubs(this)
        }
    }

    fun setDrawerSubList() {
        val copy: ArrayList<String?>
        copy = if (NetworkUtil.isConnected(this)) {
            ArrayList(usedArray)
        } else {
            UserSubscriptions.getAllUserSubreddits(this)
        }
        copy.removeAll(Arrays.asList("", null))
        sideArrayAdapter = SideArrayAdapter(
            this, copy, UserSubscriptions.getAllSubreddits(this),
            drawerSubList
        )
        drawerSubList!!.adapter = sideArrayAdapter
        if (SettingValues.subredditSearchMethod
            != Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
        ) {
            drawerSearch = headerMain!!.findViewById(R.id.sort)
            drawerSearch!!.setVisibility(View.VISIBLE)
            drawerSubList!!.isFocusable = false
            headerMain!!.findViewById<View>(R.id.close_search_drawer)
                .setOnClickListener { drawerSearch!!.setText("") }
            drawerSearch!!.setOnFocusChangeListener(OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    )
                    drawerSubList!!.smoothScrollToPositionFromTop(
                        1, drawerSearch!!.getHeight(),
                        100
                    )
                } else {
                    window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    )
                }
            })
            drawerSearch!!.setOnEditorActionListener(OnEditorActionListener { arg0, arg1, arg2 ->
                if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                    //If it the input text doesn't match a subreddit from the list exactly, openInSubView is true
                    if (sideArrayAdapter!!.fitems == null || sideArrayAdapter!!.openInSubView
                        || !usedArray!!.contains(
                            drawerSearch!!.getText().toString().lowercase()
                        )
                    ) {
                        val inte = Intent(this@MainActivity, SubredditView::class.java)
                        inte.putExtra(
                            SubredditView.EXTRA_SUBREDDIT,
                            drawerSearch!!.getText().toString()
                        )
                        this@MainActivity.startActivityForResult(inte, 2001)
                    } else {
                        if (commentPager && adapter is MainPagerAdapterComment) {
                            openingComments = null
                            toOpenComments = -1
                            (adapter as MainPagerAdapterComment).size = usedArray!!.size + 1
                            adapter!!.notifyDataSetChanged()
                            if (usedArray!!.contains(
                                    drawerSearch!!.getText().toString().lowercase()
                                )
                            ) {
                                doPageSelectedComments(
                                    usedArray!!.indexOf(
                                        drawerSearch!!.getText().toString().lowercase()
                                    )
                                )
                            } else {
                                doPageSelectedComments(
                                    usedArray!!.indexOf(sideArrayAdapter!!.fitems[0])
                                )
                            }
                        }
                        if (usedArray!!.contains(
                                drawerSearch!!.getText().toString().lowercase()
                            )
                        ) {
                            pager!!.currentItem = usedArray!!.indexOf(
                                drawerSearch!!.getText().toString().lowercase()
                            )
                        } else {
                            pager!!.currentItem = usedArray!!.indexOf(sideArrayAdapter!!.fitems[0])
                        }
                        drawerLayout!!.closeDrawers()
                        drawerSearch!!.setText("")
                        val view = this@MainActivity.currentFocus
                        if (view != null) {
                            KeyboardUtil.hideKeyboard(this@MainActivity, view.windowToken, 0)
                        }
                    }
                }
                false
            })
            val close = findViewById<View>(R.id.close_search_drawer)
            close.visibility = View.GONE
            drawerSearch!!.addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    val result = editable.toString()
                    if (result.isEmpty()) {
                        close.visibility = View.GONE
                    } else {
                        close.visibility = View.VISIBLE
                    }
                    sideArrayAdapter!!.filter.filter(result)
                }
            })
        } else {
            if (drawerSearch != null) {
                drawerSearch!!.setOnClickListener(
                    null
                ) //remove the touch listener on the drawer search field
                drawerSearch!!.visibility = View.GONE
            }
        }
    }

    fun setToolbarClick() {
        if (mTabLayout != null) {
            mTabLayout!!.addOnTabSelectedListener(
                object : TabLayout.ViewPagerOnTabSelectedListener(pager) {
                    override fun onTabReselected(tab: TabLayout.Tab) {
                        super.onTabReselected(tab)
                        scrollToTop()
                    }
                })
        } else {
            LogUtil.v("notnull")
            mToolbar!!.setOnClickListener { scrollToTop() }
        }
    }

    fun updateColor(color: Int, subreddit: String?) {
        hea!!.setBackgroundColor(color)
        header!!.setBackgroundColor(color)
        if (accountsArea != null) {
            accountsArea!!.setBackgroundColor(Palette.getDarkerColor(color))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.statusBarColor = Palette.getDarkerColor(color)
        }
        setRecentBar(subreddit, color)
        findViewById<View>(R.id.header_sub).setBackgroundColor(color)
    }

    fun updateMultiNameToSubs(subs: Map<String, String>) {
        multiNameToSubsMap = subs
    }

    fun updateSubs(subs: ArrayList<String>) {
        if (subs.isEmpty() && !NetworkUtil.isConnected(this)) {
            findViewById<View>(R.id.toolbar).visibility = View.GONE
            d = MaterialDialog.Builder(this@MainActivity).title(
                R.string.offline_no_content_found
            )
                .positiveText(R.string.offline_enter_online)
                .negativeText(R.string.btn_close)
                .cancelable(false)
                .onNegative { dialog, which -> finish() }
                .onPositive { dialog, which ->
                    SettingValues.appRestart.edit().remove("forceoffline").commit()
                    App.forceRestart(this@MainActivity, false)
                }
                .show()
        } else {
            drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
            if (!resources.getBoolean(R.bool.isTablet)) {
                setDrawerEdge(this, Constants.DRAWER_SWIPE_EDGE, drawerLayout)
            } else {
                setDrawerEdge(this, Constants.DRAWER_SWIPE_EDGE_TABLET, drawerLayout)
            }
            if (loader != null) {
                header!!.visibility = View.VISIBLE
                setDataSet(subs)
                doDrawer()
                try {
                    setDataSet(subs)
                } catch (ignored: Exception) {
                }
                loader!!.finish()
                loader = null
            } else {
                setDataSet(subs)
                doDrawer()
            }
        }
        if (NetworkUtil.isConnected(this@MainActivity)) {
            val shortcuts = ArrayList<ShortcutInfoCompat?>()
            if (Authentication.isLoggedIn) {
                shortcuts.add(
                    ShortcutInfoCompat.Builder(this, "inbox")
                        .setShortLabel("Inbox")
                        .setLongLabel("Open your Inbox")
                        .setIcon(getIcon("inbox", R.drawable.ic_email))
                        .setIntent(
                            Intent(
                                Intent.ACTION_VIEW, null, this,
                                Inbox::class.java
                            )
                        )
                        .build()
                )
                shortcuts.add(
                    ShortcutInfoCompat.Builder(this, "submit")
                        .setShortLabel("Submit")
                        .setLongLabel("Create new Submission")
                        .setIcon(getIcon("submit", R.drawable.ic_edit))
                        .setIntent(
                            Intent(
                                Intent.ACTION_VIEW, null, this,
                                Submit::class.java
                            )
                        )
                        .build()
                )
                var count = 0
                for (s in subs) {
                    if (count == 2 || count == subs.size) {
                        break
                    }
                    if (!s.contains("/m/")) {
                        val sub = Intent(
                            Intent.ACTION_VIEW, null, this,
                            SubredditView::class.java
                        )
                        sub.putExtra(SubredditView.EXTRA_SUBREDDIT, s)
                        val frontpage =
                            (if (s.equals("frontpage", ignoreCase = true)) "" else "/r/") + s
                        shortcuts.add(
                            ShortcutInfoCompat.Builder(this, "sub$s")
                                .setShortLabel(frontpage)
                                .setLongLabel(frontpage)
                                .setIcon(getIcon(s, R.drawable.ic_bookmark_border))
                                .setIntent(sub)
                                .build()
                        )
                        count++
                    }
                }
            } else {
                var count = 0
                for (s in subs) {
                    if (count == 4 || count == subs.size) {
                        break
                    }
                    if (!s.contains("/m/")) {
                        val sub = Intent(
                            Intent.ACTION_VIEW, null, this,
                            SubredditView::class.java
                        )
                        sub.putExtra(SubredditView.EXTRA_SUBREDDIT, s)
                        val frontpage =
                            (if (s.equals("frontpage", ignoreCase = true)) "" else "/r/") + s
                        ShortcutInfoCompat.Builder(this, "sub$s")
                            .setShortLabel(frontpage)
                            .setLongLabel(frontpage)
                            .setIcon(getIcon(s, R.drawable.ic_bookmark_border))
                            .setIntent(sub)
                            .build()
                        count++
                    }
                }
            }
            Collections.reverse(shortcuts)
            ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
        }
    }

    private fun getIcon(subreddit: String, @DrawableRes overlay: Int): IconCompat {
        var color = Bitmap.createBitmap(
            DensityUtils.toDp(this, 148),
            DensityUtils.toDp(this, 148), Bitmap.Config.RGB_565
        )
        color.eraseColor(Palette.getColor(subreddit))
        color = ImageUtil.clipToCircle(color)
        val over =
            DrawableUtil.drawableToBitmap(ResourcesCompat.getDrawable(resources, overlay, null))
        val canvas = Canvas(color)
        canvas.drawBitmap(
            over, color.width / 2.0f - over.width / 2.0f,
            color.height / 2.0f - over.height / 2.0f, null
        )
        return IconCompat.createWithBitmap(color)
    }

    private fun changeSubscription(subreddit: Subreddit, isChecked: Boolean) {
        currentlySubbed = isChecked
        if (isChecked) {
            UserSubscriptions.addSubreddit(
                subreddit.displayName.lowercase(),
                this@MainActivity
            )
        } else {
            UserSubscriptions.removeSubreddit(
                subreddit.displayName.lowercase(),
                this@MainActivity
            )
            pager!!.currentItem = pager!!.currentItem - 1
            restartTheme()
        }
    }

    private fun collapse(v: LinearLayout) {
        val finalHeight = v.height
        val mAnimator = AnimatorUtil.slideAnimator(finalHeight, 0, v)
        mAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                v.visibility = View.GONE
            }
        })
        mAnimator.start()
    }

    private fun dismissProgressDialog() {
        if (d != null && d!!.isShowing) {
            d!!.dismiss()
        }
    }

    private fun expand(v: LinearLayout) {
        //set Visible
        v.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(widthSpec, heightSpec)
        val mAnimator = AnimatorUtil.slideAnimator(0, v.measuredHeight, v)
        mAnimator.start()
    }

    private fun setViews(
        rawHTML: String, subredditName: String, firstTextView: SpoilerRobotoTextView?,
        commentOverflow: CommentOverflow?
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView!!.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subredditName)
            firstTextView.setLinkTextColor(ColorPreferences(this).getColor(subredditName))
            startIndex = 1
        } else {
            firstTextView!!.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow!!.setViews(blocks, subredditName)
            } else {
                commentOverflow!!.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
            val sidebar = findViewById<SidebarLayout>(R.id.drawer_layout)
            for (i in 0 until commentOverflow.childCount) {
                val maybeScrollable = commentOverflow.getChildAt(i)
                if (maybeScrollable is HorizontalScrollView) {
                    sidebar!!.addScrollable(maybeScrollable)
                }
            }
        } else {
            commentOverflow!!.removeAllViews()
        }
    }

    /**
     * If the user has the Subreddit Search method set to "long press on toolbar title", an
     * OnLongClickListener needs to be set for the toolbar as well as handling all of the relevant
     * onClicks for the views of the search bar.
     */
    private fun setupSubredditSearchToolbar() {
        if (!NetworkUtil.isConnected(this)) {
            if (findViewById<View>(R.id.drawer_divider) != null) {
                findViewById<View>(R.id.drawer_divider).visibility = View.GONE
            }
        } else {
            if ((SettingValues.subredditSearchMethod == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                        || SettingValues.subredditSearchMethod
                        == Constants.SUBREDDIT_SEARCH_METHOD_BOTH) && usedArray != null && !usedArray!!.isEmpty()
            ) {
                if (findViewById<View>(R.id.drawer_divider) != null) {
                    if (SettingValues.subredditSearchMethod
                        == Constants.SUBREDDIT_SEARCH_METHOD_BOTH
                    ) {
                        findViewById<View>(R.id.drawer_divider).visibility = View.GONE
                    } else {
                        findViewById<View>(R.id.drawer_divider).visibility = View.VISIBLE
                    }
                }
                val TOOLBAR_SEARCH_SUGGEST_LIST =
                    findViewById<ListView>(R.id.toolbar_search_suggestions_list)
                val subs_copy = ArrayList(usedArray)
                val TOOLBAR_SEARCH_SUGGEST_ADAPTER = SideArrayAdapter(
                    this, subs_copy,
                    UserSubscriptions.getAllSubreddits(this),
                    TOOLBAR_SEARCH_SUGGEST_LIST
                )
                if (TOOLBAR_SEARCH_SUGGEST_LIST != null) {
                    TOOLBAR_SEARCH_SUGGEST_LIST.adapter = TOOLBAR_SEARCH_SUGGEST_ADAPTER
                }
                if (mToolbar != null) {
                    mToolbar!!.setOnLongClickListener {
                        val GO_TO_SUB_FIELD =
                            findViewById<AutoCompleteTextView>(R.id.toolbar_search)
                        val CLOSE_BUTTON = findViewById<ImageView>(R.id.close_search_toolbar)
                        val SUGGESTIONS_BACKGROUND =
                            findViewById<CardView>(R.id.toolbar_search_suggestions)

                        //if the view mode is set to Subreddit Tabs, save the title ("Slide" or "Slide (debug)")
                        tabViewModeTitle = if (!SettingValues.single) supportActionBar!!.title
                            .toString() else null
                        supportActionBar!!.setTitle(
                            ""
                        ) //clear title to make room for search field
                        if (GO_TO_SUB_FIELD != null && CLOSE_BUTTON != null && SUGGESTIONS_BACKGROUND != null) {
                            GO_TO_SUB_FIELD.visibility = View.VISIBLE
                            CLOSE_BUTTON.visibility = View.VISIBLE
                            SUGGESTIONS_BACKGROUND.visibility = View.VISIBLE

                            //run enter animations
                            enterAnimationsForToolbarSearch(
                                ANIMATE_DURATION,
                                SUGGESTIONS_BACKGROUND, GO_TO_SUB_FIELD, CLOSE_BUTTON
                            )

                            //Get focus of the search field and show the keyboard
                            GO_TO_SUB_FIELD.requestFocus()
                            KeyboardUtil.toggleKeyboard(
                                this@MainActivity,
                                InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY
                            )

                            //Close the search UI and keyboard when clicking the close button
                            CLOSE_BUTTON.setOnClickListener {
                                val view = this@MainActivity.currentFocus
                                if (view != null) {
                                    //Hide the keyboard
                                    KeyboardUtil.hideKeyboard(
                                        this@MainActivity,
                                        view.windowToken,
                                        0
                                    )
                                }

                                //run the exit animations
                                exitAnimationsForToolbarSearch(
                                    ANIMATE_DURATION,
                                    SUGGESTIONS_BACKGROUND, GO_TO_SUB_FIELD,
                                    CLOSE_BUTTON
                                )

                                //clear sub text when close button is clicked
                                GO_TO_SUB_FIELD.setText("")
                            }
                            GO_TO_SUB_FIELD.setOnEditorActionListener { arg0, arg1, arg2 ->
                                if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                                    //If it the input text doesn't match a subreddit from the list exactly, openInSubView is true
                                    if (sideArrayAdapter!!.fitems == null || sideArrayAdapter!!.openInSubView
                                        || !usedArray!!.contains(
                                            GO_TO_SUB_FIELD.text
                                                .toString()
                                                .lowercase()
                                        )
                                    ) {
                                        val intent = Intent(
                                            this@MainActivity,
                                            SubredditView::class.java
                                        )
                                        intent.putExtra(
                                            SubredditView.EXTRA_SUBREDDIT,
                                            GO_TO_SUB_FIELD.text
                                                .toString()
                                        )
                                        this@MainActivity.startActivityForResult(
                                            intent, 2002
                                        )
                                    } else {
                                        if (commentPager
                                            && adapter is MainPagerAdapterComment
                                        ) {
                                            openingComments = null
                                            toOpenComments = -1
                                            (adapter as MainPagerAdapterComment).size =
                                                usedArray!!.size + 1
                                            adapter!!.notifyDataSetChanged()
                                            if (usedArray!!.contains(
                                                    GO_TO_SUB_FIELD.text
                                                        .toString()
                                                        .lowercase()
                                                )
                                            ) {
                                                doPageSelectedComments(
                                                    usedArray!!.indexOf(
                                                        GO_TO_SUB_FIELD.text
                                                            .toString()
                                                            .lowercase()
                                                    )
                                                )
                                            } else {
                                                doPageSelectedComments(
                                                    usedArray!!.indexOf(
                                                        sideArrayAdapter!!.fitems[0]
                                                    )
                                                )
                                            }
                                        }
                                        if (usedArray!!.contains(
                                                GO_TO_SUB_FIELD.text
                                                    .toString()
                                                    .lowercase()
                                            )
                                        ) {
                                            pager!!.currentItem = usedArray!!.indexOf(
                                                GO_TO_SUB_FIELD.text
                                                    .toString()
                                                    .lowercase()
                                            )
                                        } else {
                                            pager!!.currentItem = usedArray!!.indexOf(
                                                sideArrayAdapter!!.fitems[0]
                                            )
                                        }
                                    }
                                    val view = this@MainActivity.currentFocus
                                    if (view != null) {
                                        //Hide the keyboard
                                        KeyboardUtil.hideKeyboard(
                                            this@MainActivity, view.windowToken, 0
                                        )
                                    }
                                    SUGGESTIONS_BACKGROUND.visibility = View.GONE
                                    GO_TO_SUB_FIELD.visibility = View.GONE
                                    CLOSE_BUTTON.visibility = View.GONE
                                    if (SettingValues.single) {
                                        supportActionBar!!.setTitle(selectedSub)
                                    } else {
                                        //Set the title back to "Slide" or "Slide (debug)"
                                        supportActionBar!!.setTitle(
                                            tabViewModeTitle
                                        )
                                    }
                                }
                                false
                            }
                            GO_TO_SUB_FIELD.addTextChangedListener(object : SimpleTextWatcher() {
                                override fun afterTextChanged(editable: Editable) {
                                    val RESULT = GO_TO_SUB_FIELD.text
                                        .toString()
                                        .replace(" ".toRegex(), "")
                                    TOOLBAR_SEARCH_SUGGEST_ADAPTER.filter.filter(RESULT)
                                }
                            })
                        }
                        true
                    }
                }
            }
        }
    }

    /*
    // Todo once API allows for getting the websocket URL
    public class AsyncStartNotifSocket extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void?... params) {
            try {
                String access = Authentication.authentication.getString("websocket_url", "");

                LogUtil.v(access);
                WebSocket ws = new WebSocketFactory().createSocket(access);
                ws.addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket, String s) {
                        LogUtil.v("Received" + s);
                    }
                });
                ws.connect();
            } catch (IOException | WebSocketException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
*/
    inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        public override fun onPostExecute(subreddit: Subreddit?) {
            subreddit?.let { doSubOnlyStuff(it) }
        }

        protected override fun doInBackground(vararg params: String?): Subreddit? {
            return try {
                Authentication.reddit!!.getSubreddit(params[0])
            } catch (e: Exception) {
                null
            }
        }
    }

    inner class AsyncNotificationBadge : AsyncTask<Void?, Void?, Void?>() {
        var count = 0
        var restart = false
        var modCount = 0
        protected override fun doInBackground(vararg params: Void?): Void? {
            try {
                val me: LoggedInAccount
                if (Authentication.me == null) {
                    Authentication.me = Authentication.reddit!!.me()
                    me = Authentication.me!!
                    if (Authentication.name.equals("loggedout", ignoreCase = true)) {
                        Authentication.name = me.fullName
                        SettingValues.appRestart.edit().putString("name", Authentication.name).apply()
                        restart = true
                        return null
                    }
                    Authentication.mod = me.isMod
                    SettingValues.authentication.edit()
                        .putBoolean(App.SHARED_PREF_IS_MOD, Authentication.mod)
                        .apply()
                    if (App.notificationTime != -1) {
                        App.notifications = NotificationJobScheduler(this@MainActivity)
                        App.notifications!!.start()
                    }
                    if (App.cachedData!!.contains("toCache")) {
                        App.autoCache = AutoCacheScheduler(this@MainActivity)
                        App.autoCache!!.start()
                    }
                    val name = me.fullName
                    Authentication.name = name
                    LogUtil.v("AUTHENTICATED")
                    if (Authentication.reddit!!.isAuthenticated) {
                        val accounts = SettingValues.authentication.getStringSet(
                            "accounts",
                            HashSet()
                        )
                        if (accounts!!.contains(name)) { //convert to new system
                            accounts.remove(name)
                            accounts.add(name + ":" + Authentication.refresh)
                            SettingValues.authentication.edit()
                                .putStringSet("accounts", accounts)
                                .commit() //force commit
                        }
                        Authentication.isLoggedIn = true
                        App.notFirst = true
                    }
                } else {
                    me = Authentication.reddit!!.me()
                }
                count = me.inboxCount //Force reload of the LoggedInAccount object
                UserSubscriptions.doFriendsOfMain(this@MainActivity)
            } catch (e: Exception) {
                Log.w(LogUtil.getTag(), "Cannot fetch inbox count")
                count = -1
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            if (restart) {
                restartTheme()
                return
            }
            if (Authentication.mod && Authentication.didOnline) {
                val mod = headerMain!!.findViewById<RelativeLayout>(R.id.mod)
                mod.visibility = View.VISIBLE
                mod.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        if (UserSubscriptions.modOf != null && !UserSubscriptions.modOf.isEmpty()) {
                            val inte = Intent(this@MainActivity, ModQueue::class.java)
                            this@MainActivity.startActivity(inte)
                        }
                    }
                })
            }
            if (count != -1) {
                val oldCount = SettingValues.appRestart.getInt("inbox", 0)
                if (count > oldCount) {
                    val s = Snackbar.make(
                        mToolbar!!,
                        resources.getQuantityString(
                            R.plurals.new_messages,
                            count - oldCount, count - oldCount
                        ), Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.btn_view, object : OnSingleClickListener() {
                            override fun onSingleClick(v: View) {
                                val i = Intent(this@MainActivity, Inbox::class.java)
                                i.putExtra(Inbox.EXTRA_UNREAD, true)
                                startActivity(i)
                            }
                        })
                    LayoutUtils.showSnackbar(s)
                }
                SettingValues.appRestart.edit().putInt("inbox", count).apply()
            }
            val badge = headerMain!!.findViewById<View>(R.id.count)
            if (count == 0) {
                if (badge != null) {
                    badge.visibility = View.GONE
                }
                val notificationManager = ContextCompat.getSystemService(
                    this@MainActivity,
                    NotificationManager::class.java
                )
                notificationManager?.cancel(0)
            } else if (count != -1) {
                if (badge != null) {
                    badge.visibility = View.VISIBLE
                }
                (headerMain!!.findViewById<View>(R.id.count) as TextView?)!!.text = String.format(
                    Locale.getDefault(), "%d", count
                )
            }

            /* Todo possibly
            View modBadge = headerMain.findViewById(R.id.count_mod);

            if (modCount == 0) {
                if (modBadge != null) modBadge.setVisibility(View.GONE);
            } else if (modCount != -1) {
                if (modBadge != null) modBadge.setVisibility(View.VISIBLE);
                ((TextView) headerMain.findViewById(R.id.count)).setText(String.format(Locale.getDefault(), "%d", count));
            }*/
        }
    }

    open inner class MainPagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(
        fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        protected var mCurrentFragment: SubmissionsView? = null

        init {
            pager!!.clearOnPageChangeListeners()
            pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (positionOffset == 0f) {
                        header!!.animate()
                            .translationY(0f)
                            .setInterpolator(LinearInterpolator()).duration = 180
                        doSubSidebarNoLoad(usedArray!![position])
                    }
                }

                override fun onPageSelected(position: Int) {
                    App.currentPosition = position
                    selectedSub = usedArray!![position]
                    val page = adapter!!.currentFragment as SubmissionsView?
                    if (hea != null) {
                        hea!!.setBackgroundColor(Palette.getColor(selectedSub))
                        if (accountsArea != null) {
                            accountsArea!!.setBackgroundColor(Palette.getDarkerColor(selectedSub))
                        }
                    }
                    val colorFrom = (header!!.background as ColorDrawable).color
                    val colorTo = Palette.getColor(selectedSub)
                    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.addUpdateListener { animator ->
                        val color = animator.animatedValue as Int
                        header!!.setBackgroundColor(color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            window.statusBarColor = Palette.getDarkerColor(color)
                            if (SettingValues.colorNavBar) {
                                window.navigationBarColor = Palette.getDarkerColor(color)
                            }
                        }
                    }
                    colorAnimation.interpolator = AccelerateDecelerateInterpolator()
                    colorAnimation.duration = 200
                    colorAnimation.start()
                    setRecentBar(selectedSub)
                    if (SettingValues.single || mTabLayout == null) {
                        //Smooth out the fading animation for the toolbar subreddit search UI
                        if ((SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                    || SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                            && findViewById<AutoCompleteTextView>(R.id.toolbar_search).visibility
                            == View.VISIBLE
                        ) {
                            Handler().postDelayed(
                                { supportActionBar!!.title = selectedSub },
                                ANIMATE_DURATION + ANIMATE_DURATION_OFFSET
                            )
                        } else {
                            supportActionBar!!.setTitle(selectedSub)
                        }
                    } else {
                        mTabLayout!!.setSelectedTabIndicatorColor(
                            ColorPreferences(this@MainActivity).getColor(selectedSub)
                        )
                    }
                    if (page != null && page.adapter != null) {
                        val p = page.adapter!!.dataSet
                        if (p.offline && !isRestart) {
                            p.doMainActivityOffline(this@MainActivity, p.displayer)
                        }
                    }
                }
            })
            if (pager!!.adapter != null) {
                pager!!.adapter!!.notifyDataSetChanged()
                pager!!.currentItem = 1
                pager!!.currentItem = 0
            }
        }

        override fun getCount(): Int {
            return if (usedArray == null) {
                1
            } else {
                usedArray!!.size
            }
        }

        override fun getItem(i: Int): Fragment {
            val f = SubmissionsView()
            val args = Bundle()
            val name: String?
            name = if (multiNameToSubsMap.containsKey(usedArray!![i])) {
                multiNameToSubsMap[usedArray!![i]]
            } else {
                usedArray!![i]
            }
            args.putString("id", name)
            f.arguments = args
            return f
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            if (reloadItemNumber == position || reloadItemNumber < 0) {
                super.setPrimaryItem(container, position, `object`)
                if (usedArray!!.size >= position) doSetPrimary(`object`, position)
            } else {
                shouldLoad = usedArray!![reloadItemNumber]
                if (multiNameToSubsMap.containsKey(usedArray!![reloadItemNumber])) {
                    shouldLoad = multiNameToSubsMap[usedArray!![reloadItemNumber]]
                } else {
                    shouldLoad = usedArray!![reloadItemNumber]
                }
            }
        }

        override fun saveState(): Parcelable? {
            return null
        }

        open fun doSetPrimary(`object`: Any?, position: Int) {
            if (`object` != null && currentFragment !== `object` && position != toOpenComments && `object` is SubmissionsView) {
                shouldLoad = usedArray!![position]
                if (multiNameToSubsMap.containsKey(usedArray!![position])) {
                    shouldLoad = multiNameToSubsMap[usedArray!![position]]
                } else {
                    shouldLoad = usedArray!![position]
                }
                mCurrentFragment = `object`
                if (mCurrentFragment!!.posts == null && mCurrentFragment!!.isAdded) {
                    mCurrentFragment!!.doAdapter()
                }
            }
        }

        open val currentFragment: Fragment?
            get() = mCurrentFragment

        override fun getPageTitle(position: Int): CharSequence? {
            return if (usedArray != null) {
                StringUtil.abbreviate(usedArray!![position], 25)
            } else {
                ""
            }
        }
    }

    inner class MainPagerAdapterComment(fm: FragmentManager?) : MainPagerAdapter(fm) {
        @JvmField
        var size = usedArray!!.size
        @JvmField
        var storedFragment: Fragment? = null
        var mCurrentComments: CommentPage? = null

        init {
            pager!!.clearOnPageChangeListeners()
            pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (positionOffset == 0f) {
                        if (position != toOpenComments) {
                            pager!!.setSwipeLeftOnly(true)
                            header!!.setBackgroundColor(
                                Palette.getColor(
                                    usedArray!![position]
                                )
                            )
                            doPageSelectedComments(position)
                            if (position == toOpenComments - 1 && adapter != null && adapter!!.currentFragment != null) {
                                val page = adapter!!.currentFragment as SubmissionsView?
                                if (page != null && page.adapter != null) {
                                    page.adapter!!.refreshView()
                                }
                            }
                        } else {
                            if (mAsyncGetSubreddit != null) {
                                mAsyncGetSubreddit!!.cancel(true)
                            }
                            if (header!!.translationY == 0f) {
                                header!!.animate()
                                    .translationY(-header!!.height * 1.5f)
                                    .setInterpolator(LinearInterpolator()).duration = 180
                            }
                            pager!!.setSwipeLeftOnly(true)
                            themeSystemBars(openingComments!!.groupName.lowercase())
                            setRecentBar(openingComments!!.groupName.lowercase())
                        }
                    }
                }

                override fun onPageSelected(position: Int) {
                    if (position == toOpenComments - 1 && adapter != null && adapter!!.currentFragment != null) {
                        val page = adapter!!.currentFragment as SubmissionsView?
                        if (page != null && page.adapter != null) {
                            page.adapter!!.refreshView()
                            val p = page.adapter!!.dataSet
                            if (p.offline && !isRestart) {
                                p.doMainActivityOffline(this@MainActivity, p.displayer)
                            }
                        }
                    } else {
                        val page = adapter!!.currentFragment as SubmissionsView?
                        if (page != null && page.adapter != null) {
                            val p = page.adapter!!.dataSet
                            if (p.offline && !isRestart) {
                                p.doMainActivityOffline(this@MainActivity, p.displayer)
                            }
                        }
                    }
                }
            })
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return if (usedArray == null) {
                1
            } else {
                size
            }
        }

        override fun getItem(i: Int): Fragment {
            return if (openingComments == null || i != toOpenComments) {
                val f = SubmissionsView()
                val args = Bundle()
                if (usedArray!!.size > i) {
                    if (multiNameToSubsMap.containsKey(usedArray!![i])) {
                        //if (usedArray.get(i).co
                        args.putString("id", multiNameToSubsMap[usedArray!![i]])
                    } else {
                        args.putString("id", usedArray!![i])
                    }
                }
                f.arguments = args
                f
            } else {
                val f: Fragment = CommentPage()
                val args = Bundle()
                val name = openingComments!!.permalink
                args.putString("id", name.substring(3))
                args.putBoolean("archived", openingComments!!.isArchived)
                args.putBoolean(
                    "contest",
                    openingComments!!.isContest
                )
                args.putBoolean("locked", openingComments!!.isLocked)
                args.putInt("page", currentComment)
                args.putString("subreddit", openingComments!!.groupName)
                args.putString("baseSubreddit", subToDo)
                f.arguments = args
                f
            }
        }

        override fun saveState(): Parcelable? {
            return null
        }

        override fun doSetPrimary(`object`: Any?, position: Int) {
            if (position != toOpenComments) {
                if (multiNameToSubsMap.containsKey(usedArray!![position])) {
                    shouldLoad = multiNameToSubsMap[usedArray!![position]]
                } else {
                    shouldLoad = usedArray!![position]
                }
                if (currentFragment !== `object`) {
                    mCurrentFragment = `object` as SubmissionsView?
                    if (mCurrentFragment != null && mCurrentFragment!!.posts == null && mCurrentFragment!!.isAdded
                    ) {
                        mCurrentFragment!!.doAdapter()
                    }
                }
            } else if (`object` is CommentPage) {
                mCurrentComments = `object`
            }
        }

        override val currentFragment: Fragment?
            get() = mCurrentFragment

        override fun getItemPosition(`object`: Any): Int {
            return if (`object` !== storedFragment) POSITION_NONE else POSITION_UNCHANGED
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if (usedArray != null && position != toOpenComments) {
                StringUtil.abbreviate(usedArray!![position], 25)
            } else {
                ""
            }
        }
    }

    companion object {
        const val EXTRA_PAGE_TO = "pageTo"
        const val IS_ONLINE = "online"

        // Instance state keys
        const val SUBS = "subscriptions"
        const val LOGGED_IN = "loggedIn"
        const val USERNAME = "username"
        const val TUTORIAL_RESULT = 55
        const val INBOX_RESULT = 66
        const val RESET_ADAPTER_RESULT = 3
        const val SETTINGS_RESULT = 2
        @JvmField
        var loader: Loader? = null
        var datasetChanged = false
        @JvmField
        var multiNameToSubsMap: Map<String, String> = HashMap()
        var checkedPopups = false
        @JvmField
        var shouldLoad: String? = null
        @JvmField
        var isRestart = false
        @JvmField
        var restartPage = 0

        /**
         * Set the drawer edge (i.e. how sensitive the drawer is) Based on a given screen width
         * percentage.
         *
         * @param displayWidthPercentage larger the value, the more sensitive the drawer swipe is;
         * percentage of screen width
         * @param drawerLayout           drawerLayout to adjust the swipe edge
         */
        private fun setDrawerEdge(
            activity: Activity, displayWidthPercentage: Float,
            drawerLayout: DrawerLayout?
        ) {
            try {
                val mDragger = drawerLayout!!.javaClass.superclass.getDeclaredField("mLeftDragger")
                mDragger.isAccessible = true
                val leftDragger = mDragger[drawerLayout] as ViewDragHelper
                val mEdgeSize = leftDragger.javaClass.getDeclaredField("mEdgeSize")
                mEdgeSize.isAccessible = true
                val currentEdgeSize = mEdgeSize.getInt(leftDragger)
                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                mEdgeSize.setInt(
                    leftDragger,
                    Math.max(currentEdgeSize, (displaySize.x * displayWidthPercentage).toInt())
                )
            } catch (e: Exception) {
                LogUtil.e("$e: Exception thrown while changing navdrawer edge size")
            }
        }

        @JvmField
        var randomoverride: String? = null
    }
}
