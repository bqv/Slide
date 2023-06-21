package me.ccrama.redditslide.Activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.tabs.TabLayout
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.decreasePreviewsLeft
import me.ccrama.redditslide.Activities.Shadowbox
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.Fragments.MultiredditView
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserSubscriptions.MultiCallback
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.Views.PreCachingLayoutManager
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.SortingUtil
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod

class MultiredditOverview : BaseActivityAnim() {
    var adapter: MultiredditOverviewPagerAdapter? = null
    private var pager: ViewPager? = null
    private var profile: String? = null
    private var tabs: TabLayout? = null
    private var usedArray: List<MultiReddit>? = null
    private var initialMulti: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_multireddits, menu)
        if (!profile!!.isEmpty()) {
            menu.findItem(R.id.action_edit).isVisible = false
            menu.findItem(R.id.create).isVisible = false
        }

        //   if (mShowInfoButton) menu.findItem(R.id.action_info).setVisible(true);
        //   else menu.findItem(R.id.action_info).setVisible(false);
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        /* removed for now
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return ((MultiredditView) adapter!!.currentFragment).onKeyDown(keyCode);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return ((MultiredditView) adapter!!.currentFragment).onKeyDown(keyCode);
            default:
                return super.dispatchKeyEvent(event);
        }*/
        return super.dispatchKeyEvent(event)
    }

    val currentPage: Int
        get() {
            var position = 0
            val currentOrientation = resources.configuration.orientation
            if ((adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager is LinearLayoutManager
                && currentOrientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                position =
                    ((adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager as LinearLayoutManager?)!!
                        .findFirstVisibleItemPosition() - 1
            } else if ((adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager is CatchStaggeredGridLayoutManager) {
                var firstVisibleItems: IntArray? = null
                firstVisibleItems = ((adapter!!.currentFragment as MultiredditView?)!!.rv!!
                    .layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(
                    firstVisibleItems
                )
                if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                    position = firstVisibleItems[0] - 1
                }
            } else {
                position =
                    ((adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager as PreCachingLayoutManager?)!!
                        .findFirstVisibleItemPosition() - 1
            }
            return position
        }
    var term: String? = null
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                try {
                    onBackPressed()
                } catch (ignored: Exception) {
                }
                true
            }

            R.id.action_edit -> {
                run {
                    if (profile!!.isEmpty() && UserSubscriptions.multireddits != null
                        && !UserSubscriptions.multireddits!!.isEmpty()
                    ) {
                        val i = Intent(this@MultiredditOverview, CreateMulti::class.java)
                        i.putExtra(
                            CreateMulti.EXTRA_MULTI,
                            UserSubscriptions.multireddits!![pager!!.currentItem]?.displayName
                        )
                        startActivity(i)
                    }
                }
                true
            }

            R.id.search -> {
                run {
                    val m = object : MultiCallback {
                        override fun onComplete(multireddits: List<MultiReddit?>?) {
                            if (!multireddits.isNullOrEmpty()) {
                                searchMulti = multireddits[pager!!.currentItem]
                                val builder = MaterialDialog.Builder(this@MultiredditOverview)
                                    .title(R.string.search_title)
                                    .alwaysCallInputCallback()
                                    .input(
                                        getString(R.string.search_msg), ""
                                    ) { materialDialog, charSequence ->
                                        term = charSequence.toString()
                                    }

                                //Add "search current sub" if it is not frontpage/all/random
                                builder.positiveText(
                                    getString(
                                        R.string.search_subreddit,
                                        "/m/" + searchMulti!!.displayName
                                    )
                                )
                                    .onPositive { materialDialog, dialogAction ->
                                        val i = Intent(
                                            this@MultiredditOverview,
                                            Search::class.java
                                        )
                                        i.putExtra(Search.EXTRA_TERM, term)
                                        i.putExtra(
                                            Search.EXTRA_MULTIREDDIT,
                                            searchMulti!!.displayName
                                        )
                                        startActivity(i)
                                    }
                                builder.show()
                            }
                        }
                    }
                    if (profile!!.isEmpty()) {
                        UserSubscriptions.getMultireddits(m)
                    } else {
                        UserSubscriptions.getPublicMultireddits(m, profile!!)
                    }
                }
                true
            }

            R.id.create -> {
                if (profile!!.isEmpty()) {
                    val i2 = Intent(this@MultiredditOverview, CreateMulti::class.java)
                    startActivity(i2)
                }
                true
            }

            R.id.action_sort -> {
                openPopup()
                true
            }

            R.id.subs -> {
                (findViewById<View>(R.id.drawer_layout) as DrawerLayout).openDrawer(Gravity.RIGHT)
                true
            }

            R.id.gallery -> {
                if (SettingValues.isPro) {
                    val posts =
                        (adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i2 = Intent(this, Gallery::class.java)
                        i2.putExtra(Gallery.EXTRA_PROFILE, profile)
                        i2.putExtra(
                            Gallery.EXTRA_MULTIREDDIT,
                            (adapter!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
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
                            decreasePreviewsLeft()
                            val posts =
                                (adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts
                            if (posts != null && !posts.isEmpty()) {
                                val i2 = Intent(
                                    this@MultiredditOverview,
                                    Gallery::class.java
                                )
                                i2.putExtra(Gallery.EXTRA_PROFILE, profile)
                                i2.putExtra(
                                    Gallery.EXTRA_MULTIREDDIT,
                                    (adapter!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!
                                        .getDisplayName()
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
                    val posts =
                        (adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts
                    if (posts != null && !posts.isEmpty()) {
                        val i = Intent(this, Shadowbox::class.java)
                        i.putExtra(Shadowbox.EXTRA_PAGE, currentPage)
                        i.putExtra(Shadowbox.EXTRA_PROFILE, profile)
                        i.putExtra(
                            Shadowbox.EXTRA_MULTIREDDIT,
                            (adapter!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                        )
                        startActivity(i)
                    }
                } else {
                    val b = ProUtil.proUpgradeMsg(this, R.string.general_shadowbox_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                    if (SettingValues.previews > 0 && adapter != null && (adapter!!.currentFragment as MultiredditView?)!!.posts != null && (adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts != null && !(adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts.isEmpty()) {
                        b.setNeutralButton(
                            getString(R.string.pro_previews, SettingValues.previews)
                        ) { dialog: DialogInterface?, which: Int ->
                            decreasePreviewsLeft()
                            val posts =
                                (adapter!!.currentFragment as MultiredditView?)!!.posts!!.posts
                            if (posts != null && !posts.isEmpty()) {
                                val i = Intent(
                                    this@MultiredditOverview,
                                    Shadowbox::class.java
                                )
                                i.putExtra(Shadowbox.EXTRA_PAGE, currentPage)
                                i.putExtra(Shadowbox.EXTRA_PROFILE, profile)
                                i.putExtra(
                                    Shadowbox.EXTRA_MULTIREDDIT,
                                    (adapter!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!
                                        .displayName
                                )
                                startActivity(i)
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

    private fun buildDialog(wasException: Boolean = false) {
        try {
            val b = AlertDialog.Builder(this@MultiredditOverview)
                .setCancelable(false)
                .setOnDismissListener { dialog: DialogInterface? -> finish() }
            if (wasException) {
                b.setTitle(R.string.err_title)
                    .setMessage(R.string.err_loading_content)
                    .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> finish() }
            } else if (profile!!.isEmpty()) {
                b.setTitle(R.string.multireddit_err_title)
                    .setMessage(R.string.multireddit_err_msg)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        val i = Intent(this@MultiredditOverview, CreateMulti::class.java)
                        startActivity(i)
                    }
                    .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int -> finish() }
            } else {
                b.setTitle(R.string.public_multireddit_err_title)
                    .setMessage(R.string.public_multireddit_err_msg)
                    .setNegativeButton(R.string.btn_go_back) { dialog: DialogInterface?, which: Int -> finish() }
            }
            b.show()
        } catch (e: Exception) {
        }
    }

    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        multiActivity = this
        super.onCreate(savedInstance)
        applyColorTheme("")
        setContentView(R.layout.activity_multireddits)
        setupAppBar(R.id.toolbar, R.string.title_multireddits, true, false)
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getDefaultColor())
        tabs = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabs!!.tabMode = TabLayout.MODE_SCROLLABLE
        pager = findViewById<View>(R.id.content_view) as ViewPager
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        profile = ""
        initialMulti = ""
        if (intent.extras != null) {
            profile = intent.extras!!
                .getString(EXTRA_PROFILE, "")
            initialMulti = intent.extras!!
                .getString(EXTRA_MULTI, "")
        }
        if (profile.equals(Authentication.name, ignoreCase = true)) {
            profile = ""
        }
        val callback = object : MultiCallback {
            override fun onComplete(multiReddits: List<MultiReddit?>?) {
                if (!multiReddits.isNullOrEmpty()) {
                    setDataSet(multiReddits.filterNotNull())
                } else {
                    buildDialog()
                }
            }
        }
        if (profile!!.isEmpty()) {
            UserSubscriptions.getMultireddits(callback)
        } else {
            UserSubscriptions.getPublicMultireddits(callback, profile!!)
        }
    }

    fun openPopup() {
        val popup = PopupMenu(this@MultiredditOverview, findViewById(R.id.anchor), Gravity.RIGHT)
        val id =
            ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!
                .displayName
                .lowercase()
        val base = SortingUtil.getSortingSpannables("multi$id")
        for (s in base) {
            // Do not add option for "Best" in any subreddit except for the frontpage.
            if (s.toString() == getString(R.string.sorting_best)) {
                continue
            }
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            var i = 0
            for (s in base) {
                if (s == item.title) {
                    break
                }
                i++
            }
            LogUtil.v("Chosen is $i")
            if (pager!!.adapter != null) {
                when (i) {
                    0 -> {
                        SortingUtil.setSorting(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), Sorting.HOT
                        )
                        reloadSubs()
                    }

                    1 -> {
                        SortingUtil.setSorting(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), Sorting.NEW
                        )
                        reloadSubs()
                    }

                    2 -> {
                        SortingUtil.setSorting(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), Sorting.RISING
                        )
                        reloadSubs()
                    }

                    3 -> {
                        SortingUtil.setSorting(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), Sorting.TOP
                        )
                        openPopupTime()
                    }

                    4 -> {
                        SortingUtil.setSorting(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), Sorting.CONTROVERSIAL
                        )
                        openPopupTime()
                    }
                }
            }
            true
        }
        popup.show()
    }

    fun openPopupTime() {
        val popup = PopupMenu(this@MultiredditOverview, findViewById(R.id.anchor), Gravity.RIGHT)
        val id =
            ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!.currentFragment as MultiredditView?)!!.posts!!.multiReddit!!
                .displayName
                .lowercase()
        val base = SortingUtil.getSortingTimesSpannables("multi$id")
        for (s in base) {
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            var i = 0
            for (s in base) {
                if (s == item.title) {
                    break
                }
                i++
            }
            LogUtil.v("Chosen is $i")
            if (pager!!.adapter != null) {
                when (i) {
                    0 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.HOUR
                        )
                        reloadSubs()
                    }

                    1 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.DAY
                        )
                        reloadSubs()
                    }

                    2 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.WEEK
                        )
                        reloadSubs()
                    }

                    3 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.MONTH
                        )
                        reloadSubs()
                    }

                    4 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.YEAR
                        )
                        reloadSubs()
                    }

                    5 -> {
                        SortingUtil.setTime(
                            "multi"
                                    + ((pager!!.adapter as MultiredditOverviewPagerAdapter?)!!
                                .currentFragment as MultiredditView?)!!.posts!!.multiReddit!!.displayName
                                .lowercase(), TimePeriod.ALL
                        )
                        reloadSubs()
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun reloadSubs() {
        val current = pager!!.currentItem
        adapter = MultiredditOverviewPagerAdapter(supportFragmentManager)
        pager!!.adapter = adapter
        pager!!.currentItem = current
    }

    private fun setDataSet(data: List<MultiReddit>) {
        try {
            usedArray = data
            if (usedArray!!.isEmpty()) {
                buildDialog()
            } else {
                if (adapter == null) {
                    adapter = MultiredditOverviewPagerAdapter(supportFragmentManager)
                } else {
                    adapter!!.notifyDataSetChanged()
                }
                pager!!.adapter = adapter
                pager!!.offscreenPageLimit = 1
                tabs!!.setupWithViewPager(pager)
                if (!initialMulti!!.isEmpty()) {
                    for (i in usedArray!!.indices) {
                        if (usedArray!![i].displayName.equals(initialMulti, ignoreCase = true)) {
                            pager!!.currentItem = i
                            break
                        }
                    }
                }
                tabs!!.setSelectedTabIndicatorColor(
                    ColorPreferences(this@MultiredditOverview).getColor(
                        usedArray!![0].displayName
                    )
                )
                doDrawerSubs(0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val window = this.window
                    window.statusBarColor = Palette.getDarkerColor(
                        usedArray!![0].displayName
                    )
                }
                val header = findViewById<View>(R.id.header)
                tabs!!.addOnTabSelectedListener(object :
                    TabLayout.ViewPagerOnTabSelectedListener(pager) {
                    override fun onTabReselected(tab: TabLayout.Tab) {
                        super.onTabReselected(tab)
                        var pastVisiblesItems = 0
                        val firstVisibleItems =
                            ((adapter!!.currentFragment as MultiredditView?)!!.rv!!
                                .layoutManager as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(
                                null
                            )
                        if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                            for (firstVisibleItem in firstVisibleItems) {
                                pastVisiblesItems = firstVisibleItem
                            }
                        }
                        if (pastVisiblesItems > 8) {
                            (adapter!!.currentFragment as MultiredditView?)!!.rv!!.scrollToPosition(
                                0
                            )
                            if (header != null) {
                                header.animate()
                                    .translationY(header.height.toFloat())
                                    .setInterpolator(LinearInterpolator()).duration = 0
                            }
                        } else {
                            (adapter!!.currentFragment as MultiredditView?)!!.rv!!.smoothScrollToPosition(
                                0
                            )
                        }
                    }
                })
                findViewById<View>(R.id.header).setBackgroundColor(
                    Palette.getColor(usedArray!![0].displayName)
                )
            }
        } catch (e: NullPointerException) {
            buildDialog(true)
            Log.e(LogUtil.getTag(), "Cannot load multis:\n$e")
        }
    }

    fun doDrawerSubs(position: Int) {
        val current = usedArray!![position]
        val l = findViewById<View>(R.id.sidebar_scroll) as LinearLayout
        l.removeAllViews()
        val toSort = CaseInsensitiveArrayList()
        for (s in current.subreddits) {
            toSort.add(s.displayName.lowercase())
        }
        for (sub in UserSubscriptions.sortNoExtras(toSort)) {
            val convertView = layoutInflater.inflate(R.layout.subforsublist, l, false)
            val t = convertView.findViewById<TextView>(R.id.name)
            t.text = sub
            val colorView = convertView.findViewById<View>(R.id.color)
            colorView.setBackgroundResource(R.drawable.circle)
            BlendModeUtil.tintDrawableAsModulate(
                colorView.background,
                Palette.getColor(sub)
            )
            convertView.setOnClickListener {
                val inte = Intent(this@MultiredditOverview, SubredditView::class.java)
                inte.putExtra(SubredditView.EXTRA_SUBREDDIT, sub)
                this@MultiredditOverview.startActivityForResult(inte, 4)
            }
            l.addView(convertView)
        }
    }

    inner class MultiredditOverviewPagerAdapter internal constructor(fm: FragmentManager?) :
        FragmentStatePagerAdapter(
            fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(i: Int): Fragment {
            val f: Fragment = MultiredditView()
            val args = Bundle()
            args.putInt("id", i)
            args.putString(EXTRA_PROFILE, profile)
            f.arguments = args
            return f
        }

        var currentFragment: Fragment? = null
            private set

        init {
            pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    findViewById<View>(R.id.header).animate()
                        .translationY(0f)
                        .setInterpolator(LinearInterpolator()).duration = 180
                    findViewById<View>(R.id.header).setBackgroundColor(
                        Palette.getColor(usedArray!![position].displayName)
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val window = window
                        window.statusBarColor = Palette.getDarkerColor(
                            usedArray!![position].displayName
                        )
                    }
                    tabs!!.setSelectedTabIndicatorColor(
                        ColorPreferences(this@MultiredditOverview).getColor(
                            usedArray!![position].displayName
                        )
                    )
                    doDrawerSubs(position)
                }
            })
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            if (currentFragment !== `object`) {
                currentFragment = `object` as Fragment
            }
            super.setPrimaryItem(container, position, `object`)
        }

        override fun getCount(): Int {
            return if (usedArray == null) {
                1
            } else {
                usedArray!!.size
            }
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return usedArray!![position].fullName
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 940 && adapter != null && adapter!!.currentFragment != null) {
            if (resultCode == RESULT_OK) {
                LogUtil.v("Doing hide posts")
                val posts = data!!.getIntegerArrayListExtra("seen")!!
                (adapter!!.currentFragment as MultiredditView?)!!.adapter!!.refreshView(posts)
                if (data.hasExtra("lastPage") && data.getIntExtra(
                        "lastPage",
                        0
                    ) != 0 && (adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager is LinearLayoutManager
                ) {
                    ((adapter!!.currentFragment as MultiredditView?)!!.rv!!.layoutManager as LinearLayoutManager?)!!
                        .scrollToPositionWithOffset(
                            data.getIntExtra("lastPage", 0) + 1,
                            mToolbar!!.height
                        )
                }
            } else {
                (adapter!!.currentFragment as MultiredditView?)!!.adapter!!.refreshView()
            }
        }
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_MULTI = "multi"
        @JvmField
        var multiActivity: Activity? = null
        @JvmField
        var searchMulti: MultiReddit? = null
    }
}
