package me.ccrama.redditslide.Activities

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.text.Spannable
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.CompoundButton
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.InputCallback
import com.afollestad.materialdialogs.MaterialDialog.ListCallback
import com.afollestad.materialdialogs.MaterialDialog.ListCallbackSingleChoice
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.clearSort
import ltd.ucode.slide.SettingValues.expandedToolbar
import ltd.ucode.slide.SettingValues.fab
import ltd.ucode.slide.SettingValues.fabType
import ltd.ucode.slide.SettingValues.getBaseSubmissionSort
import ltd.ucode.slide.SettingValues.getBaseTimePeriod
import ltd.ucode.slide.SettingValues.hasSort
import ltd.ucode.slide.SettingValues.setSubSorting
import ltd.ucode.slide.SettingValues.showNSFWContent
import ltd.ucode.slide.SettingValues.single
import me.ccrama.redditslide.Activities.Shadowbox
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.Fragments.SubmissionsView
import me.ccrama.redditslide.ImageFlairs
import me.ccrama.redditslide.Notifications.CheckForMail
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostMatch
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.Views.CommentOverflow
import me.ccrama.redditslide.Views.PreCachingLayoutManager
import me.ccrama.redditslide.Views.SidebarLayout
import me.ccrama.redditslide.Views.ToggleSwipeViewPager
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.ui.settings.SettingsSubAdapter
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.util.SubmissionParser
import net.dean.jraw.ApiException
import net.dean.jraw.http.MultiRedditUpdateRequest
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.ModerationManager
import net.dean.jraw.managers.MultiRedditManager
import net.dean.jraw.models.FlairTemplate
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.MultiSubreddit
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Subreddit
import net.dean.jraw.models.UserRecord
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import net.dean.jraw.paginators.UserRecordPaginator

class SubredditView : BaseActivity() {
    var canSubmit = true
    @JvmField
    var subreddit: String? = null
    @JvmField
    var openingComments: Submission? = null
    @JvmField
    var currentComment = 0
    @JvmField
    var adapter: SubredditPagerAdapter? = null
    var term: String? = null
    @JvmField
    var pager: ToggleSwipeViewPager? = null
    @JvmField
    var singleMode = false
    @JvmField
    var commentPager = false
    var loaded = false
    var header: View? = null
    var sub: Subreddit? = null
    private var drawerLayout: DrawerLayout? = null
    private var currentlySubbed = false
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == 2) {
            // Make sure the request was successful
            pager!!.adapter = SubredditPagerAdapter(supportFragmentManager)
        } else if (requestCode == 1) {
            restartTheme()
        } else if (requestCode == 940) {
            if (adapter != null && adapter!!.currentFragment != null) {
                if (resultCode == RESULT_OK) {
                    LogUtil.v("Doing hide posts")
                    val posts = data!!.getIntegerArrayListExtra("seen")
                    (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.refreshView((posts)!!)
                    if ((data.hasExtra("lastPage")
                                && (data.getIntExtra("lastPage", 0) != 0
                                ) && (adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is LinearLayoutManager)
                    ) {
                        ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager as LinearLayoutManager?)!!
                            .scrollToPositionWithOffset(
                                data.getIntExtra("lastPage", 0) + 1,
                                mToolbar!!.height
                            )
                    }
                } else {
                    (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.refreshView()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.START)
            || drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.END)
        ) {
            drawerLayout!!.closeDrawers()
        } else if (commentPager && pager!!.currentItem == 2) {
            pager!!.currentItem = pager!!.currentItem - 1
        } else {
            super.onBackPressed()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        if (SettingValues.commentPager && single) {
            disableSwipeBackLayout()
        }
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.background = null
        super.onCreate(savedInstanceState)
        if (!restarting) {
            overridePendingTransition(R.anim.slideright, 0)
        } else {
            restarting = false
        }
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT, "")
        applyColorTheme(subreddit)
        setContentView(R.layout.activity_singlesubreddit)
        setupSubredditAppBar(R.id.toolbar, subreddit, true, subreddit)
        header = findViewById<View>(R.id.header)
        drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        setResult(3)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        pager = findViewById<View>(R.id.content_view) as ToggleSwipeViewPager
        singleMode = single
        commentPager = false
        if (singleMode) commentPager = SettingValues.commentPager
        if (commentPager) {
            adapter = SubredditPagerAdapterComment(supportFragmentManager)
            pager!!.setSwipeLeftOnly(false)
            pager!!.setSwipingEnabled(true)
        } else {
            adapter = SubredditPagerAdapter(supportFragmentManager)
        }
        pager!!.adapter = adapter
        pager!!.currentItem = 1
        mToolbar!!.setOnClickListener {
            var pastVisiblesItems = 0
            val firstVisibleItems = (((adapter!!.currentFragment) as SubmissionsView?)!!.rv!!
                .getLayoutManager() as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(
                null
            )
            if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                for (firstVisibleItem: Int in firstVisibleItems) {
                    pastVisiblesItems = firstVisibleItem
                }
            }
            if (pastVisiblesItems > 8) {
                ((adapter!!.currentFragment) as SubmissionsView?)!!.rv!!.scrollToPosition(0)
                header!!.animate()
                    .translationY(header!!.height.toFloat())
                    .setInterpolator(LinearInterpolator()).duration = 180
            } else {
                ((adapter!!.currentFragment) as SubmissionsView?)!!.rv!!.smoothScrollToPosition(0)
            }
            ((adapter!!.currentFragment) as SubmissionsView?)!!.resetScroll()
        }
        if (subreddit != "random"
            && subreddit != "all"
            && subreddit != "frontpage"
            && subreddit != "friends"
            && subreddit != "mod"
            && subreddit != "myrandom"
            && subreddit != "randnsfw"
            && subreddit != "popular"
            && !subreddit!!.contains("+")
        ) {
            executeAsyncSubreddit(subreddit)
        } else {
            drawerLayout!!.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        if (expandedToolbar) {
            inflater.inflate(R.menu.menu_single_subreddit_expanded, menu)
        } else {
            inflater.inflate(R.menu.menu_single_subreddit, menu)
        }
        if (fab && fabType == Constants.FAB_DISMISS) {
            menu.findItem(R.id.hide_posts).isVisible = false
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        //Hide the "Submit" menu item if the currently viewed sub is the frontpage or /r/all.
        if ((subreddit == "frontpage" || subreddit == "all" || subreddit == "popular") || subreddit == "friends" || subreddit == "mod") {
            menu.findItem(R.id.submit).isVisible = false
            menu.findItem(R.id.sidebar).isVisible = false
        }
        mToolbar!!.menu
            .findItem(R.id.theme)
            .setOnMenuItemClickListener {
                val style = ColorPreferences(this@SubredditView).getThemeSubreddit(
                    subreddit
                )
                val contextThemeWrapper: Context = ContextThemeWrapper(this@SubredditView, style)
                val localInflater = layoutInflater.cloneInContext(contextThemeWrapper)
                val dialoglayout = localInflater.inflate(R.layout.colorsub, null)
                val arrayList = ArrayList<String?>()
                arrayList.add(subreddit)
                SettingsSubAdapter.showSubThemeEditor(
                    arrayList, this@SubredditView,
                    dialoglayout
                )
                false
            }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.filter -> {
                filterContent(subreddit)
                return true
            }

            R.id.submit -> {
                val i = Intent(this, Submit::class.java)
                if (canSubmit) i.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                startActivity(i)
                return true
            }

            R.id.action_refresh -> {
                if (adapter != null && adapter!!.currentFragment != null) {
                    (adapter!!.currentFragment as SubmissionsView?)!!.forceRefresh()
                }
                return true
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
                return true
            }

            R.id.gallery -> {
                if (SettingValues.isPro) {
                    val posts = (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i2 = Intent(this, Gallery::class.java)
                        i2.putExtra(
                            "offline",
                            if (((adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached
                                        != null)
                            ) (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.cached!!.time else 0L
                        )
                        i2.putExtra(
                            Gallery.EXTRA_SUBREDDIT,
                            (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                        )
                        startActivity(i2)
                    }
                } else {
                    ProUtil.proUpgradeMsg(this, R.string.general_gallerymode_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                        .show()
                }
                return true
            }

            R.id.search -> {
                val builder = MaterialDialog.Builder(this).title(R.string.search_title)
                    .alwaysCallInputCallback()
                    .input(getString(R.string.search_msg), "",
                        InputCallback { materialDialog, charSequence ->
                            term = charSequence.toString()
                        })
                    .neutralText(R.string.search_all)
                    .onNeutral(object : SingleButtonCallback {
                        override fun onClick(
                            materialDialog: MaterialDialog,
                            dialogAction: DialogAction
                        ) {
                            val i = Intent(this@SubredditView, Search::class.java)
                            i.putExtra(Search.EXTRA_TERM, term)
                            startActivity(i)
                        }
                    })

                //Add "search current sub" if it is not frontpage/all/random
                if ((!subreddit.equals("frontpage", ignoreCase = true)
                            && !subreddit.equals("all", ignoreCase = true)
                            && !subreddit.equals("random", ignoreCase = true)
                            && !subreddit.equals("popular", ignoreCase = true)
                            && subreddit != "myrandom"
                            && subreddit != "randnsfw"
                            && !subreddit.equals("friends", ignoreCase = true)
                            && !subreddit.equals("mod", ignoreCase = true))
                ) {
                    builder.positiveText(getString(R.string.search_subreddit, subreddit))
                        .onPositive(object : SingleButtonCallback {
                            override fun onClick(
                                materialDialog: MaterialDialog,
                                dialogAction: DialogAction
                            ) {
                                val i = Intent(this@SubredditView, Search::class.java)
                                i.putExtra(Search.EXTRA_TERM, term)
                                i.putExtra(Search.EXTRA_SUBREDDIT, subreddit)
                                Log.v(
                                    LogUtil.getTag(),
                                    "INTENT SHOWS $term AND $subreddit"
                                )
                                startActivity(i)
                            }
                        })
                }
                builder.show()
                return true
            }

            R.id.sidebar -> {
                drawerLayout!!.openDrawer(Gravity.RIGHT)
                return true
            }

            R.id.hide_posts -> {
                (adapter!!.currentFragment as SubmissionsView?)!!.clearSeenPosts(false)
                return true
            }

            R.id.action_shadowbox -> {
                if (SettingValues.isPro) {
                    val posts =
                        ((pager!!.adapter as SubredditPagerAdapter?)!!.currentFragment as SubmissionsView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i2 = Intent(this, Shadowbox::class.java)
                        i2.putExtra(Shadowbox.EXTRA_PAGE, currentPage)
                        i2.putExtra(
                            Shadowbox.EXTRA_SUBREDDIT,
                            (adapter!!.currentFragment as SubmissionsView?)!!.posts!!.subreddit
                        )
                        startActivity(i2)
                    }
                } else {
                    ProUtil.proUpgradeMsg(this, R.string.general_shadowbox_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                        .show()
                }
                return true
            }

            else -> return false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sub != null) {
            if (sub!!.isNsfw && (!SettingValues.storeHistory || !SettingValues.storeNSFWHistory)) {
                val e = App.cachedData!!.edit()
                for (s in OfflineSubreddit.getAll(sub!!.displayName)) {
                    e.remove(s)
                }
                e.apply()
            } else if (!SettingValues.storeHistory) {
                val e = App.cachedData!!.edit()
                for (s in OfflineSubreddit.getAll(sub!!.displayName)) {
                    e.remove(s)
                }
                e.apply()
            }
        }
    }

    fun doPageSelectedComments(position: Int) {
        header!!.animate().translationY(0f).setInterpolator(LinearInterpolator()).duration = 180
        pager!!.setSwipeLeftOnly(false)
        App.currentPosition = position
        if ((position == 1 && adapter != null) && adapter!!.currentFragment != null) {
            (adapter!!.currentFragment as SubmissionsView?)!!.adapter!!.refreshView()
        }
    }

    fun doSubSidebar(subOverride: String) {
        findViewById<View>(R.id.loader).visibility = View.VISIBLE
        invalidateOptionsMenu()
        if ((!subOverride.equals("all", ignoreCase = true)
                    && !subOverride.equals("frontpage", ignoreCase = true)
                    && !subOverride.equals("random", ignoreCase = true)
                    && !subOverride.equals("popular", ignoreCase = true)
                    && !subOverride.equals("myrandom", ignoreCase = true)
                    && !subOverride.equals("randnsfw", ignoreCase = true)
                    &&
                    !subOverride.equals("friends", ignoreCase = true)
                    && !subOverride.equals("mod", ignoreCase = true)
                    &&
                    !subOverride.contains("+")
                    && !subOverride.contains(".")
                    && !subOverride.contains("/m/"))
        ) {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
            }
            loaded = true
            val dialoglayout = findViewById<View>(R.id.sidebarsub)
            run {
                val submit = (dialoglayout.findViewById<View>(R.id.submit))
                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    submit.visibility = View.GONE
                }
                if (fab && fabType == Constants.FAB_POST) {
                    submit.visibility = View.GONE
                }
                submit.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(view: View) {
                        val inte = Intent(this@SubredditView, Submit::class.java)
                        if (!subOverride.contains("/m/") && canSubmit) {
                            inte.putExtra(Submit.EXTRA_SUBREDDIT, subOverride)
                        }
                        this@SubredditView.startActivity(inte)
                    }
                })
            }
            dialoglayout.findViewById<View>(R.id.wiki)
                .setOnClickListener {
                    val i = Intent(this@SubredditView, Wiki::class.java)
                    i.putExtra(Wiki.EXTRA_SUBREDDIT, subOverride)
                    startActivity(i)
                }
            dialoglayout.findViewById<View>(R.id.syncflair)
                .setOnClickListener { ImageFlairs.syncFlairs(this@SubredditView, subreddit) }
            dialoglayout.findViewById<View>(R.id.submit)
                .setOnClickListener {
                    val i = Intent(this@SubredditView, Submit::class.java)
                    if ((!subOverride.contains("/m/") || !subOverride.contains(".")) && canSubmit) {
                        i.putExtra(Submit.EXTRA_SUBREDDIT, subOverride)
                    }
                    startActivity(i)
                }
            val sort = dialoglayout.findViewById<TextView>(R.id.sort)
            var sortingis = Sorting.HOT
            if (hasSort((subreddit)!!)) {
                sortingis = getBaseSubmissionSort((subreddit)!!)
                sort.text = (sortingis.name
                        + (if ((sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP)) " of "
                        + getBaseTimePeriod((subreddit)!!).name else ""))
            } else {
                sort.text = "Set default sorting"
            }
            val sortid = SortingUtil.getSortingId(sortingis)
            dialoglayout.findViewById<View>(R.id.sorting)
                .setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        val l2: DialogInterface.OnClickListener =
                            object : DialogInterface.OnClickListener {
                                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                                    when (i) {
                                        0 -> sorts = Sorting.HOT
                                        1 -> sorts = Sorting.NEW
                                        2 -> sorts = Sorting.RISING
                                        3 -> {
                                            sorts = Sorting.TOP
                                            askTimePeriod(sorts!!, subreddit, dialoglayout)
                                            return
                                        }

                                        4 -> {
                                            sorts = Sorting.CONTROVERSIAL
                                            askTimePeriod(sorts!!, subreddit, dialoglayout)
                                            return
                                        }
                                    }
                                    setSubSorting((sorts)!!, time, (subreddit)!!)
                                    val sortingis = getBaseSubmissionSort((subreddit)!!)
                                    sort.text = (sortingis.name
                                            + (if ((sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP)) " of "
                                            + getBaseTimePeriod((subreddit)!!).name else ""))
                                    reloadSubs()
                                }
                            }
                        AlertDialog.Builder(this@SubredditView)
                            .setTitle(R.string.sorting_choose)
                            .setSingleChoiceItems(SortingUtil.getSortingStrings(), sortid, l2)
                            .setNegativeButton("Reset default sorting") { dialog: DialogInterface?, which: Int ->
                                clearSort((subreddit)!!)
                                val sort1: TextView = dialoglayout.findViewById(R.id.sort)
                                if (hasSort((subreddit)!!)) {
                                    val sortingis1: Sorting = getBaseSubmissionSort((subreddit)!!)
                                    sort1.setText(
                                        (sortingis1.name
                                                + (if ((sortingis1 == Sorting.CONTROVERSIAL || sortingis1 == Sorting.TOP)) " of "
                                                + getBaseTimePeriod((subreddit)!!).name else ""))
                                    )
                                } else {
                                    sort1.setText("Set default sorting")
                                }
                                reloadSubs()
                            }
                            .show()
                    }
                })
            dialoglayout.findViewById<View>(R.id.theme)
                .setOnClickListener {
                    val style =
                        ColorPreferences(this@SubredditView).getThemeSubreddit(subOverride)
                    val contextThemeWrapper: Context =
                        ContextThemeWrapper(this@SubredditView, style)
                    val localInflater = layoutInflater.cloneInContext(contextThemeWrapper)
                    val dialoglayout = localInflater.inflate(R.layout.colorsub, null)
                    val arrayList = ArrayList<String>()
                    arrayList.add(subOverride)
                    SettingsSubAdapter.showSubThemeEditor(
                        arrayList, this@SubredditView,
                        dialoglayout
                    )
                }
            dialoglayout.findViewById<View>(R.id.mods)
                .setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        val d: Dialog = MaterialDialog.Builder(this@SubredditView).title(
                            R.string.sidebar_findingmods
                        )
                            .cancelable(true)
                            .content(R.string.misc_please_wait)
                            .progress(true, 100)
                            .show()
                        object : AsyncTask<Void?, Void?, Void?>() {
                            var mods: ArrayList<UserRecord>? = null
                            override fun doInBackground(vararg params: Void?): Void? {
                                mods = ArrayList()
                                val paginator = UserRecordPaginator(
                                    Authentication.reddit, subOverride,
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
                                for (rec: UserRecord in mods!!) {
                                    names.add(rec.fullName)
                                }
                                d.dismiss()
                                MaterialDialog.Builder(this@SubredditView).title(
                                    getString(R.string.sidebar_submods, subreddit)
                                )
                                    .items(names)
                                    .itemsCallback { dialog, itemView, which, text ->
                                        val i = Intent(this@SubredditView, Profile::class.java)
                                        i.putExtra(Profile.EXTRA_PROFILE, names[which])
                                        startActivity(i)
                                    }
                                    .positiveText(R.string.btn_message)
                                    .onPositive { dialog, which ->
                                        val i = Intent(
                                            this@SubredditView,
                                            SendMessage::class.java
                                        )
                                        i.putExtra(SendMessage.EXTRA_NAME, "/r/$subOverride")
                                        startActivity(i)
                                    }
                                    .show()
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                })
            dialoglayout.findViewById<View>(R.id.flair).visibility = View.GONE
            if (Authentication.didOnline && Authentication.isLoggedIn) {
                object : AsyncTask<View?, Void?, View>() {
                    var flairs: List<FlairTemplate>? = null
                    var flairText: ArrayList<String?>? = null
                    var current: String? = null
                    var m: AccountManager? = null
                    override fun doInBackground(vararg params: View?): View {
                        try {
                            m = AccountManager(Authentication.reddit)
                            val node = m!!.getFlairChoicesRootNode(subOverride, null)
                            flairs = m!!.getFlairChoices(subOverride, node)
                            val currentF = m!!.getCurrentFlair(subOverride, node)
                            if (currentF != null) {
                                if (currentF.text.isEmpty()) {
                                    current = ("[" + currentF.cssClass + "]")
                                } else {
                                    current = (currentF.text)
                                }
                            }
                            flairText = ArrayList()
                            for (temp: FlairTemplate in flairs!!) {
                                if (temp.text.isEmpty()) {
                                    flairText!!.add("[" + temp.cssClass + "]")
                                } else {
                                    flairText!!.add(temp.text)
                                }
                            }
                        } catch (e1: Exception) {
                            e1.printStackTrace()
                        }
                        return params[0]!!
                    }

                    override fun onPostExecute(flair: View) {
                        if (((flairs != null
                                    ) && !flairs!!.isEmpty()
                                    && (flairText != null
                                    ) && !flairText!!.isEmpty())
                        ) {
                            flair.visibility = View.VISIBLE
                            if (current != null) {
                                (dialoglayout.findViewById<View>(R.id.flair_text) as TextView).text =
                                    getString(R.string.sidebar_flair, current)
                            }
                            flair.setOnClickListener(object : View.OnClickListener {
                                override fun onClick(v: View) {
                                    MaterialDialog.Builder(this@SubredditView).items(flairText!!)
                                        .title(R.string.sidebar_select_flair)
                                        .itemsCallback(object : ListCallback {
                                            override fun onSelection(
                                                dialog: MaterialDialog,
                                                itemView: View, which: Int,
                                                text: CharSequence
                                            ) {
                                                val t = flairs!![which]
                                                if (t.isTextEditable) {
                                                    MaterialDialog.Builder(
                                                        this@SubredditView
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
                                                        .onPositive(
                                                            object : SingleButtonCallback {
                                                                override fun onClick(
                                                                    dialog: MaterialDialog,
                                                                    which: DialogAction
                                                                ) {
                                                                    val flair = dialog.inputEditText!!
                                                                        .getText()
                                                                        .toString()
                                                                    object :
                                                                        AsyncTask<Void?, Void?, Boolean>() {
                                                                        override fun doInBackground(
                                                                            vararg params: Void?
                                                                        ): Boolean {
                                                                            try {
                                                                                ModerationManager(
                                                                                    Authentication.reddit
                                                                                )
                                                                                    .setFlair(
                                                                                        subOverride,
                                                                                        t,
                                                                                        flair,
                                                                                        Authentication.name
                                                                                    )
                                                                                val currentF =
                                                                                    m!!.getCurrentFlair(
                                                                                        subOverride
                                                                                    )
                                                                                if (currentF
                                                                                        .text
                                                                                        .isEmpty()
                                                                                ) {
                                                                                    current = (("["
                                                                                            + currentF
                                                                                        .cssClass
                                                                                            + "]"))
                                                                                } else {
                                                                                    current =
                                                                                        (currentF
                                                                                            .text)
                                                                                }
                                                                                return true
                                                                            } catch (e: Exception) {
                                                                                e.printStackTrace()
                                                                                return false
                                                                            }
                                                                        }

                                                                        override fun onPostExecute(
                                                                            done: Boolean
                                                                        ) {
                                                                            val s: Snackbar
                                                                            if (done) {
                                                                                if ((current
                                                                                            != null)
                                                                                ) {
                                                                                    (dialoglayout
                                                                                        .findViewById<View>(
                                                                                            R.id.flair_text
                                                                                        ) as TextView).text =
                                                                                        getString(
                                                                                            R.string.sidebar_flair,
                                                                                            current
                                                                                        )
                                                                                }
                                                                                s = Snackbar.make(
                                                                                    (mToolbar)!!,
                                                                                    R.string.snackbar_flair_success,
                                                                                    Snackbar.LENGTH_SHORT
                                                                                )
                                                                            } else {
                                                                                s = Snackbar.make(
                                                                                    (mToolbar)!!,
                                                                                    R.string.snackbar_flair_error,
                                                                                    Snackbar.LENGTH_SHORT
                                                                                )
                                                                            }
                                                                            if (s != null) {
                                                                                LayoutUtils.showSnackbar(
                                                                                    s
                                                                                )
                                                                            }
                                                                        }
                                                                    }.executeOnExecutor(
                                                                        THREAD_POOL_EXECUTOR
                                                                    )
                                                                }
                                                            })
                                                        .negativeText(R.string.btn_cancel)
                                                        .show()
                                                } else {
                                                    object : AsyncTask<Void?, Void?, Boolean>() {
                                                        override fun doInBackground(
                                                            vararg params: Void?
                                                        ): Boolean {
                                                            try {
                                                                ModerationManager(
                                                                    Authentication.reddit
                                                                ).setFlair(
                                                                    subOverride, t, null,
                                                                    Authentication.name
                                                                )
                                                                val currentF = m!!.getCurrentFlair(
                                                                    subOverride
                                                                )
                                                                if (currentF.text
                                                                        .isEmpty()
                                                                ) {
                                                                    current = (("["
                                                                            + currentF.cssClass
                                                                            + "]"))
                                                                } else {
                                                                    current = (currentF.text)
                                                                }
                                                                return true
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                                return false
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
                                                                    ) as TextView).text = getString(
                                                                        R.string.sidebar_flair,
                                                                        current
                                                                    )
                                                                }
                                                                s = Snackbar.make(
                                                                    (mToolbar)!!,
                                                                    R.string.snackbar_flair_success,
                                                                    Snackbar.LENGTH_SHORT
                                                                )
                                                            } else {
                                                                s = Snackbar.make(
                                                                    (mToolbar)!!,
                                                                    R.string.snackbar_flair_error,
                                                                    Snackbar.LENGTH_SHORT
                                                                )
                                                            }
                                                            if (s != null) {
                                                                LayoutUtils.showSnackbar(s)
                                                            }
                                                        }
                                                    }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                                                }
                                            }
                                        })
                                        .show()
                                }
                            })
                        }
                    }
                }.execute(dialoglayout.findViewById(R.id.flair) as View)
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

    fun doSubSidebarNoLoad(subOverride: String) {
        findViewById<View>(R.id.loader).visibility = View.GONE
        invalidateOptionsMenu()
        if (!subOverride.equals("all", ignoreCase = true) && !subOverride.equals(
                "frontpage",
                ignoreCase = true
            ) &&
            !subOverride.equals("friends", ignoreCase = true) && !subOverride.equals(
                "mod",
                ignoreCase = true
            ) &&
            !subOverride.contains("+") && !subOverride.contains(".") && !subOverride.contains(
                "/m/"
            )
        ) {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
            }
            findViewById<View>(R.id.sidebar_text).visibility = View.GONE
            findViewById<View>(R.id.sub_title).visibility = View.GONE
            findViewById<View>(R.id.subscribers).visibility = View.GONE
            findViewById<View>(R.id.active_users).visibility = View.GONE
            findViewById<View>(R.id.header_sub).setBackgroundColor(Palette.getColor(subOverride))
            (findViewById<View>(R.id.sub_infotitle) as TextView).text =
                subOverride

            //Sidebar buttons should use subOverride's accent color
            val subColor = ColorPreferences(this).getColor(subOverride)
            (findViewById<View>(R.id.theme_text) as TextView).setTextColor(subColor)
            (findViewById<View>(R.id.wiki_text) as TextView).setTextColor(subColor)
            (findViewById<View>(R.id.post_text) as TextView).setTextColor(subColor)
            (findViewById<View>(R.id.mods_text) as TextView).setTextColor(subColor)
            (findViewById<View>(R.id.flair_text) as TextView).setTextColor(subColor)
            (findViewById<View>(R.id.sorting).findViewById<View>(R.id.sort) as TextView).setTextColor(
                subColor
            )
            (findViewById<View>(R.id.sync) as TextView).setTextColor(subColor)
        } else {
            if (drawerLayout != null) {
                drawerLayout!!.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.END
                )
            }
        }
    }

    fun executeAsyncSubreddit(sub: String?) {
        AsyncGetSubreddit().execute(sub)
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
        val FILTER_TITLE = getString(
            R.string.content_to_hide,
            if (subreddit == "frontpage") "frontpage" else "/r/$subreddit"
        )
        AlertDialog.Builder(this)
            .setTitle(FILTER_TITLE)
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
            if ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is LinearLayoutManager
                && currentOrientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                position =
                    ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager as LinearLayoutManager?)!!
                        .findFirstCompletelyVisibleItemPosition() - 1
            } else if ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!.layoutManager is CatchStaggeredGridLayoutManager) {
                var firstVisibleItems: IntArray? = null
                firstVisibleItems = ((adapter!!.currentFragment as SubmissionsView?)!!.rv!!
                    .getLayoutManager() as CatchStaggeredGridLayoutManager?)!!.findFirstCompletelyVisibleItemPositions(
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
    var time = TimePeriod.DAY
    var sorts: Sorting? = null
    private fun askTimePeriod(sort: Sorting, sub: String?, dialoglayout: View) {
        val l2 = DialogInterface.OnClickListener { dialogInterface, i ->
            when (i) {
                0 -> time = TimePeriod.HOUR
                1 -> time = TimePeriod.DAY
                2 -> time = TimePeriod.WEEK
                3 -> time = TimePeriod.MONTH
                4 -> time = TimePeriod.YEAR
                5 -> time = TimePeriod.ALL
            }
            setSubSorting(sort, time, (sub)!!)
            SortingUtil.setSorting(sub, sort)
            SortingUtil.setTime(sub, time)
            val sort = dialoglayout.findViewById<TextView>(R.id.sort)
            val sortingis = getBaseSubmissionSort("Default sorting: $subreddit")
            sort.text = (sortingis.name
                    + (if ((sortingis == Sorting.CONTROVERSIAL || sortingis == Sorting.TOP)) " of "
                    + getBaseTimePeriod((subreddit)!!).name else ""))
            reloadSubs()
        }
        AlertDialog.Builder(this@SubredditView)
            .setTitle(R.string.sorting_choose)
            .setSingleChoiceItems(
                SortingUtil.getSortingTimesStrings(),
                SortingUtil.getSortingTimeId(""),
                l2
            )
            .show()
    }

    fun openPopup() {
        val popup = PopupMenu(this@SubredditView, findViewById(R.id.anchor), Gravity.RIGHT)
        val base = SortingUtil.getSortingSpannables(subreddit)
        for (s in base) {
            // Do not add option for "Best" in any subreddit except for the frontpage.
            if (!subreddit.equals(
                    "frontpage",
                    ignoreCase = true
                ) && s.toString() == getString(R.string.sorting_best)
            ) {
                continue
            }
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s: Spannable in base) {
                if ((s == item.title)) {
                    break
                }
                i++
            }
            when (i) {
                0 -> {
                    SortingUtil.setSorting(subreddit, Sorting.HOT)
                    reloadSubs()
                }

                1 -> {
                    SortingUtil.setSorting(subreddit, Sorting.NEW)
                    reloadSubs()
                }

                2 -> {
                    SortingUtil.setSorting(subreddit, Sorting.RISING)
                    reloadSubs()
                }

                3 -> {
                    SortingUtil.setSorting(subreddit, Sorting.TOP)
                    openPopupTime()
                }

                4 -> {
                    SortingUtil.setSorting(subreddit, Sorting.CONTROVERSIAL)
                    openPopupTime()
                }

                5 -> {
                    SortingUtil.setSorting(subreddit, Sorting.BEST)
                    reloadSubs()
                }
            }
            true
        }
        popup.show()
    }

    fun openPopupTime() {
        val popup = PopupMenu(this@SubredditView, findViewById(R.id.anchor), Gravity.RIGHT)
        val base = SortingUtil.getSortingTimesSpannables(subreddit)
        for (s in base) {
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s: Spannable in base) {
                if ((s == item.title)) {
                    break
                }
                i++
            }
            when (i) {
                0 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.HOUR)
                    reloadSubs()
                }

                1 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.DAY)
                    reloadSubs()
                }

                2 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.WEEK)
                    reloadSubs()
                }

                3 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.MONTH)
                    reloadSubs()
                }

                4 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.YEAR)
                    reloadSubs()
                }

                5 -> {
                    SortingUtil.setTime(subreddit, TimePeriod.ALL)
                    reloadSubs()
                }
            }
            true
        }
        popup.show()
    }

    fun restartTheme() {
        val intent = this.intent
        intent.putExtra(EXTRA_SUBREDDIT, subreddit)
        finish()
        restarting = true
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun changeSubscription(subreddit: Subreddit, isChecked: Boolean) {
        if (isChecked) {
            UserSubscriptions.addSubreddit(
                subreddit.displayName.lowercase(),
                this@SubredditView
            )
        } else {
            UserSubscriptions.removeSubreddit(
                subreddit.displayName.lowercase(),
                this@SubredditView
            )
            pager!!.currentItem = pager!!.currentItem - 1
            restartTheme()
        }
        val s = Snackbar.make(
            mToolbar!!,
            if (isChecked) getString(R.string.misc_subscribed) else getString(R.string.misc_unsubscribed),
            Snackbar.LENGTH_SHORT
        )
        LayoutUtils.showSnackbar(s)
    }

    private fun doSubOnlyStuff(subreddit: Subreddit) {
        if (!isFinishing) {
            findViewById<View>(R.id.loader).visibility = View.GONE
            if (subreddit.dataNode.has("subreddit_type") && !subreddit.dataNode["subreddit_type"]
                    .isNull
            ) {
                canSubmit = !subreddit.dataNode["subreddit_type"]
                    .asText()
                    .equals("RESTRICTED", ignoreCase = true)
            }
            if (subreddit.sidebar != null && !subreddit.sidebar.isEmpty()) {
                findViewById<View>(R.id.sidebar_text).visibility = View.VISIBLE
                val text = subreddit.dataNode["description_html"].asText().trim { it <= ' ' }
                val body = findViewById<View>(R.id.sidebar_text) as SpoilerRobotoTextView
                val overflow = findViewById<View>(R.id.commentOverflow) as CommentOverflow
                setViews(text, subreddit.displayName, body, overflow)

                //get all subs that have Notifications enabled
                val rawSubs = StringUtil.stringToArray(
                    appRestart.getString(CheckForMail.SUBS_TO_GET, "")
                )
                val subThresholds = HashMap<String, Int>()
                for (s: String in rawSubs) {
                    try {
                        val split =
                            s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        subThresholds[split[0].lowercase()] = Integer.valueOf(split[1])
                    } catch (ignored: Exception) {
                        //do nothing
                    }
                }

                //whether or not this subreddit was in the keySet
                val isNotified = subThresholds.containsKey(subreddit.displayName.lowercase())
                (findViewById<View>(R.id.notify_posts_state) as AppCompatCheckBox).isChecked =
                    isNotified
            } else {
                findViewById<View>(R.id.sidebar_text).visibility = View.GONE
            }
            val collection = findViewById<View>(R.id.collection)
            if (Authentication.isLoggedIn) {
                collection.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        object : AsyncTask<Void?, Void?, Void?>() {
                            var multis = HashMap<String?, MultiReddit>()
                            override fun doInBackground(vararg params: Void?): Void? {
                                if (UserSubscriptions.multireddits == null) {
                                    UserSubscriptions.syncMultiReddits(this@SubredditView)
                                }
                                for (r in UserSubscriptions.multireddits!!.filterNotNull()) {
                                    multis[r.displayName] = r
                                }
                                return null
                            }

                            override fun onPostExecute(aVoid: Void?) {
                                MaterialDialog.Builder(this@SubredditView).title(
                                    "Add /r/" + subreddit.displayName + " to"
                                )
                                    .items(multis.keys)
                                    .itemsCallback(object : ListCallback {
                                        override fun onSelection(
                                            dialog: MaterialDialog,
                                            itemView: View, which: Int,
                                            text: CharSequence
                                        ) {
                                            object : AsyncTask<Void?, Void?, Void?>() {
                                                override fun doInBackground(vararg params: Void?): Void? {
                                                    try {
                                                        val multiName = multis.keys
                                                            .toTypedArray()[which]
                                                        val subs: MutableList<String> = ArrayList()
                                                        for (sub: MultiSubreddit in multis[multiName]!!.subreddits) {
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
                                                            this@SubredditView
                                                        )
                                                        runOnUiThread {
                                                            drawerLayout!!.closeDrawers()
                                                            val s = Snackbar.make(
                                                                (mToolbar)!!,
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
                                                                    (mToolbar)!!,
                                                                    getString(
                                                                        R.string.multi_error
                                                                    ),
                                                                    Snackbar.LENGTH_LONG
                                                                )
                                                                    .setAction(
                                                                        R.string.btn_ok,
                                                                        null
                                                                    )
                                                                    .show()
                                                            }
                                                        }
                                                        e.printStackTrace()
                                                    } catch (e: ApiException) {
                                                        runOnUiThread {
                                                            runOnUiThread {
                                                                Snackbar.make(
                                                                    (mToolbar)!!,
                                                                    getString(
                                                                        R.string.multi_error
                                                                    ),
                                                                    Snackbar.LENGTH_LONG
                                                                )
                                                                    .setAction(
                                                                        R.string.btn_ok,
                                                                        null
                                                                    )
                                                                    .show()
                                                            }
                                                        }
                                                        e.printStackTrace()
                                                    }
                                                    return null
                                                }
                                            }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                                        }
                                    })
                                    .show()
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                })
            } else {
                collection.visibility = View.GONE
            }
            run {
                val subscribe: TextView? = findViewById<View>(R.id.subscribe) as TextView
                currentlySubbed =
                    if (Authentication.isLoggedIn) subreddit.isUserSubscriber else UserSubscriptions.getSubscriptions(
                        this
                    )!!.contains(subreddit.displayName.lowercase())
                MiscUtil.doSubscribeButtonText(currentlySubbed, subscribe)
                assert(subscribe != null)
                subscribe!!.setOnClickListener(object : View.OnClickListener {
                    private fun doSubscribe() {
                        if (Authentication.isLoggedIn) {
                            AlertDialog.Builder(this@SubredditView)
                                .setTitle(getString(R.string.subscribe_to, subreddit.displayName))
                                .setPositiveButton(
                                    R.string.reorder_add_subscribe
                                ) { dialog: DialogInterface?, which: Int ->
                                    object : AsyncTask<Void?, Void?, Boolean?>() {
                                        public override fun onPostExecute(success: Boolean?) {
                                            if (!success!!) { // If subreddit was removed from account or not
                                                AlertDialog.Builder(this@SubredditView)
                                                    .setTitle(R.string.force_change_subscription)
                                                    .setMessage(R.string.force_change_subscription_desc)
                                                    .setPositiveButton(
                                                        R.string.btn_yes,
                                                        { dialog1: DialogInterface?, which1: Int ->
                                                            changeSubscription(
                                                                subreddit,
                                                                true
                                                            ) // Force add the subscription
                                                            val s: Snackbar = Snackbar.make(
                                                                (mToolbar)!!,
                                                                getString(R.string.misc_subscribed),
                                                                Snackbar.LENGTH_SHORT
                                                            )
                                                            LayoutUtils.showSnackbar(s)
                                                        })
                                                    .setNegativeButton(R.string.btn_no, null)
                                                    .setCancelable(false)
                                                    .show()
                                            } else {
                                                changeSubscription(subreddit, true)
                                            }
                                        }

                                        override fun doInBackground(
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
                                .setNegativeButton(R.string.btn_cancel, null)
                                .setNeutralButton(
                                    R.string.btn_add_to_sublist
                                ) { dialog: DialogInterface?, which: Int ->
                                    changeSubscription(
                                        subreddit,
                                        true
                                    ) // Force add the subscription
                                    val s: Snackbar = Snackbar.make(
                                        (mToolbar)!!,
                                        R.string.sub_added,
                                        Snackbar.LENGTH_SHORT
                                    )
                                    LayoutUtils.showSnackbar(s)
                                }
                                .show()
                        } else {
                            changeSubscription(subreddit, true)
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

                    private fun doUnsubscribe() {
                        if (Authentication.didOnline) {
                            AlertDialog.Builder(this@SubredditView)
                                .setTitle(
                                    getString(
                                        R.string.unsubscribe_from,
                                        subreddit.displayName
                                    )
                                )
                                .setPositiveButton(
                                    R.string.reorder_remove_unsubscribe
                                ) { dialog: DialogInterface?, which: Int ->
                                    object : AsyncTask<Void?, Void?, Boolean?>() {
                                        public override fun onPostExecute(success: Boolean?) {
                                            if (!success!!) { // If subreddit was removed from account or not
                                                AlertDialog.Builder(
                                                    this@SubredditView
                                                )
                                                    .setTitle(R.string.force_change_subscription)
                                                    .setMessage(R.string.force_change_subscription_desc)
                                                    .setPositiveButton(
                                                        R.string.btn_yes,
                                                        { dialog12: DialogInterface?, which12: Int ->
                                                            changeSubscription(
                                                                subreddit,
                                                                false
                                                            ) // Force add the subscription
                                                            val s: Snackbar = Snackbar.make(
                                                                (mToolbar)!!,
                                                                getString(R.string.misc_unsubscribed),
                                                                Snackbar.LENGTH_SHORT
                                                            )
                                                            LayoutUtils.showSnackbar(s)
                                                        })
                                                    .setNegativeButton(R.string.btn_no, null)
                                                    .setCancelable(false)
                                                    .show()
                                            } else {
                                                changeSubscription(
                                                    subreddit,
                                                    false
                                                )
                                            }
                                        }

                                        override fun doInBackground(
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
                                .setNeutralButton(
                                    R.string.just_unsub
                                ) { dialog: DialogInterface?, which: Int ->
                                    changeSubscription(
                                        subreddit,
                                        false
                                    ) // Force add the subscription
                                    val s: Snackbar = Snackbar.make(
                                        (mToolbar)!!,
                                        R.string.misc_unsubscribed,
                                        Snackbar.LENGTH_SHORT
                                    )
                                    LayoutUtils.showSnackbar(s)
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        } else {
                            changeSubscription(subreddit, false)
                        }
                    }
                })
            }
            run {
                val notifyStateCheckBox: AppCompatCheckBox? =
                    findViewById<View>(R.id.notify_posts_state) as AppCompatCheckBox
                assert(notifyStateCheckBox != null)
                notifyStateCheckBox!!.setOnCheckedChangeListener(
                    object : CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            buttonView: CompoundButton,
                            isChecked: Boolean
                        ) {
                            if (isChecked) {
                                val sub = subreddit.displayName
                                if ((!sub.equals("all", ignoreCase = true)
                                            && !sub.equals("frontpage", ignoreCase = true)
                                            &&
                                            !sub.equals("friends", ignoreCase = true)
                                            && !sub.equals("mod", ignoreCase = true)
                                            &&
                                            !sub.contains("+")
                                            && !sub.contains(".")
                                            && !sub.contains("/m/"))
                                ) {
                                    AlertDialog.Builder(this@SubredditView)
                                        .setTitle(getString(R.string.sub_post_notifs_title, sub))
                                        .setMessage(R.string.sub_post_notifs_msg)
                                        .setPositiveButton(
                                            R.string.btn_ok
                                        ) { dialog: DialogInterface?, which: Int ->
                                            MaterialDialog.Builder(this@SubredditView)
                                                .title(R.string.sub_post_notifs_threshold)
                                                .items(
                                                    *arrayOf<String>(
                                                        "1", "5", "10", "20", "40", "50"
                                                    )
                                                )
                                                .alwaysCallSingleChoiceCallback()
                                                .itemsCallbackSingleChoice(0,
                                                    object : ListCallbackSingleChoice {
                                                        override fun onSelection(
                                                            dialog: MaterialDialog,
                                                            itemView: View,
                                                            which: Int,
                                                            text: CharSequence
                                                        ): Boolean {
                                                            val subs: ArrayList<String> =
                                                                StringUtil.stringToArray(
                                                                    appRestart
                                                                        .getString(
                                                                            CheckForMail.SUBS_TO_GET,
                                                                            ""
                                                                        )
                                                                )
                                                            subs.add(
                                                                (sub
                                                                        + ":"
                                                                        + text)
                                                            )
                                                            appRestart
                                                                .edit()
                                                                .putString(
                                                                    CheckForMail.SUBS_TO_GET,
                                                                    StringUtil.arrayToString(
                                                                        subs
                                                                    )
                                                                )
                                                                .commit()
                                                            return true
                                                        }
                                                    })
                                                .cancelable(false)
                                                .show()
                                        }
                                        .setNegativeButton(R.string.btn_cancel, null)
                                        .setNegativeButton(
                                            R.string.btn_cancel
                                        ) { dialog: DialogInterface?, which: Int ->
                                            notifyStateCheckBox.isChecked = false
                                        }
                                        .setOnCancelListener { dialog: DialogInterface? ->
                                            notifyStateCheckBox.isChecked = false
                                        }
                                        .show()
                                } else {
                                    notifyStateCheckBox.isChecked = false
                                    Toast.makeText(
                                        this@SubredditView,
                                        R.string.sub_post_notifs_err, Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            } else {
                                val cancelIntent =
                                    Intent(this@SubredditView, CancelSubNotifs::class.java)
                                cancelIntent.putExtra(
                                    CancelSubNotifs.EXTRA_SUB,
                                    subreddit.displayName
                                )
                                startActivity(cancelIntent)
                            }
                        }
                    })
            }
            if (!subreddit.publicDescription.isEmpty()) {
                findViewById<View>(R.id.sub_title).visibility = View.VISIBLE
                setViews(
                    subreddit.dataNode["public_description_html"].asText(),
                    subreddit.displayName.lowercase(),
                    (findViewById<View>(R.id.sub_title) as SpoilerRobotoTextView),
                    findViewById<View>(R.id.sub_title_overflow) as CommentOverflow
                )
            } else {
                findViewById<View>(R.id.sub_title).visibility = View.GONE
            }
            if (subreddit.dataNode.has("icon_img") && !subreddit.dataNode["icon_img"]
                    .asText()
                    .isEmpty()
            ) {
                (application as App).imageLoader!!
                    .displayImage(
                        subreddit.dataNode["icon_img"].asText(),
                        findViewById<View>(R.id.subimage) as ImageView
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
                        findViewById<View>(R.id.sub_banner) as ImageView
                    )
            } else {
                findViewById<View>(R.id.sub_banner).visibility = View.GONE
            }
            (findViewById<View>(R.id.subscribers) as TextView).text = getString(
                R.string.subreddit_subscribers_string,
                subreddit.localizedSubscriberCount
            )
            findViewById<View>(R.id.subscribers).visibility = View.VISIBLE
            (findViewById<View>(R.id.active_users) as TextView).text = getString(
                R.string.subreddit_active_users_string_new,
                subreddit.localizedAccountsActive
            )
            findViewById<View>(R.id.active_users).visibility = View.VISIBLE
        }
    }

    private fun reloadSubs() {
        restartTheme()
    }

    private fun setViews(
        rawHTML: String, subreddit: String, firstTextView: SpoilerRobotoTextView,
        commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subreddit)
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subreddit)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subreddit)
            }
            val sidebar = findViewById<View>(R.id.drawer_layout) as SidebarLayout
            for (i in 0 until commentOverflow.childCount) {
                val maybeScrollable = commentOverflow.getChildAt(i)
                if (maybeScrollable is HorizontalScrollView) {
                    sidebar.addScrollable(maybeScrollable)
                }
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    open inner class SubredditPagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(
        fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        private var mCurrentFragment: SubmissionsView? = null
        private var blankPage: BlankFragment? = null

        init {
            pager!!.clearOnPageChangeListeners()
            pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (position == 0) {
                        val params = header!!.layoutParams as CoordinatorLayout.LayoutParams
                        params.setMargins(
                            header!!.width - positionOffsetPixels, 0,
                            -(header!!.width - positionOffsetPixels), 0
                        )
                        header!!.layoutParams = params
                        if (positionOffsetPixels == 0) {
                            finish()
                            overridePendingTransition(0, R.anim.fade_out)
                        }
                    }
                    if (position == 0) {
                        (pager!!.adapter as SubredditPagerAdapter?)!!.blankPage!!.doOffset(
                            positionOffset
                        )
                        pager!!.setBackgroundColor(Palette.adjustAlpha(positionOffset * 0.7f))
                    }
                }
            })
            if (pager!!.adapter != null) {
                pager!!.currentItem = 1
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getItem(i: Int): Fragment {
            return if (i == 1) {
                val f = SubmissionsView()
                val args = Bundle()
                args.putString("id", subreddit)
                f.arguments = args
                f
            } else {
                blankPage = BlankFragment()
                blankPage!!
            }
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
            doSetPrimary(`object`, position)
        }

        override fun saveState(): Parcelable? {
            return null
        }

        open fun doSetPrimary(`object`: Any?, position: Int) {
            if ((`object` != null) && currentFragment !== `object` && position != 3 && `object` is SubmissionsView) {
                mCurrentFragment = `object`
                if (mCurrentFragment!!.posts == null && mCurrentFragment!!.isAdded) {
                    mCurrentFragment!!.doAdapter()
                }
            }
        }

        open val currentFragment: Fragment?
            get() = mCurrentFragment
    }

    inner class SubredditPagerAdapterComment(fm: FragmentManager?) : SubredditPagerAdapter(fm) {
        @JvmField
        var size = 2
        @JvmField
        var storedFragment: Fragment? = null
        var blankPage: BlankFragment? = null
        private var mCurrentFragment: SubmissionsView? = null

        init {
            pager!!.clearOnPageChangeListeners()
            pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (position == 0) {
                        val params = header!!.layoutParams as CoordinatorLayout.LayoutParams
                        params.setMargins(
                            header!!.width - positionOffsetPixels, 0,
                            -(header!!.width - positionOffsetPixels), 0
                        )
                        header!!.layoutParams = params
                        if (positionOffsetPixels == 0) {
                            finish()
                            overridePendingTransition(0, R.anim.fade_out)
                        }
                        blankPage!!.doOffset(positionOffset)
                        pager!!.setBackgroundColor(Palette.adjustAlpha(positionOffset * 0.7f))
                    } else if (positionOffset == 0f) {
                        if (position == 1) {
                            doPageSelectedComments(position)
                        } else {
                            //todo if (mAsyncGetSubreddit != null) {
                            //mAsyncGetSubreddit.cancel(true);
                            //}
                            if (header!!.translationY == 0f) {
                                header!!.animate()
                                    .translationY(-header!!.height.toFloat())
                                    .setInterpolator(LinearInterpolator()).duration = 180
                            }
                            pager!!.setSwipeLeftOnly(true)
                            themeSystemBars(openingComments!!.subredditName.lowercase())
                            setRecentBar(openingComments!!.subredditName.lowercase())
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

        override fun saveState(): Parcelable? {
            return null
        }

        override val currentFragment: Fragment?
            get() = mCurrentFragment

        override fun doSetPrimary(`object`: Any?, position: Int) {
            if (position != 2 && position != 0) {
                if (currentFragment !== `object`) {
                    mCurrentFragment = (`object` as SubmissionsView?)
                    if ((mCurrentFragment != null
                                ) && (mCurrentFragment!!.posts == null
                                ) && mCurrentFragment!!.isAdded
                    ) {
                        mCurrentFragment!!.doAdapter()
                    }
                }
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            return if (`object` !== storedFragment) POSITION_NONE else POSITION_UNCHANGED
        }

        override fun getItem(i: Int): Fragment {
            return if (i == 0) {
                blankPage = BlankFragment()
                blankPage!!
            } else if (openingComments == null || i != 2) {
                val f = SubmissionsView()
                val args = Bundle()
                args.putString("id", subreddit)
                f.arguments = args
                f
            } else {
                val f: Fragment = CommentPage()
                val args = Bundle()
                val name = openingComments!!.fullName
                args.putString("id", name.substring(3))
                args.putBoolean("archived", openingComments!!.isArchived)
                args.putBoolean(
                    "contest",
                    openingComments!!.dataNode["contest_mode"].asBoolean()
                )
                args.putBoolean("locked", openingComments!!.isLocked)
                args.putInt("page", currentComment)
                args.putString("subreddit", openingComments!!.subredditName)
                args.putString("baseSubreddit", subreddit)
                f.arguments = args
                f
            }
        }

        override fun getCount(): Int {
            return size
        }
    }

    private inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        public override fun onPostExecute(subreddit: Subreddit?) {
            if (subreddit != null) {
                setResult(RESULT_OK)
                sub = subreddit
                try {
                    doSubSidebarNoLoad(sub!!.displayName)
                    doSubSidebar(sub!!.displayName)
                    doSubOnlyStuff(sub!!)
                } catch (e: NullPointerException) { //activity has been killed
                    if (!isFinishing) finish()
                }
                this@SubredditView.subreddit = sub!!.displayName
                if (subreddit.isNsfw
                    && SettingValues.storeHistory
                    && SettingValues.storeNSFWHistory
                ) {
                    UserSubscriptions.addSubToHistory(subreddit.displayName)
                } else if (SettingValues.storeHistory && !subreddit.isNsfw) {
                    UserSubscriptions.addSubToHistory(subreddit.displayName)
                }

                // Over 18 interstitial for signed out users or those who haven't enabled NSFW content
                if (subreddit.isNsfw && !showNSFWContent) {
                    AlertDialog.Builder(this@SubredditView)
                        .setTitle(getString(R.string.over18_title, subreddit.displayName))
                        .setMessage(
                            """${getString(R.string.over18_desc)}

""" + getString(
                                if (Authentication.isLoggedIn) R.string.over18_desc_loggedin else R.string.over18_desc_loggedout
                            )
                        )
                        .setCancelable(false)
                        .setPositiveButton(R.string.misc_continue) { dialog: DialogInterface?, which: Int ->
                            (adapter!!.currentFragment as SubmissionsView?)!!.doAdapter(
                                true
                            )
                        }
                        .setNeutralButton(R.string.btn_go_back) { dialog: DialogInterface?, which: Int ->
                            finish()
                            overridePendingTransition(0, R.anim.fade_out)
                        }
                        .show()
                }
            }
        }

        override fun doInBackground(vararg params: String?): Subreddit? {
            return try {
                val result = Authentication.reddit!!.getSubreddit(params[0])
                if (result.isNsfw == null) {
                    // Sub is probably a user profile backing subreddit for a deleted/suspended user
                    throw Exception("Sub has null values where it shouldn't")
                }
                result
            } catch (e: Exception) {
                runOnUiThread {
                    try {
                        AlertDialog.Builder(this@SubredditView)
                            .setTitle(R.string.subreddit_err)
                            .setMessage(getString(R.string.subreddit_err_msg_new, params[0]))
                            .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface, which: Int ->
                                dialog.dismiss()
                                setResult(4)
                                finish()
                            }
                            .setOnDismissListener { dialog: DialogInterface? ->
                                setResult(4)
                                finish()
                            }
                            .show()
                    } catch (ignored: Exception) {
                    }
                }
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        const val EXTRA_SUBREDDIT = "subreddit"
        var restarting = false
    }
}
