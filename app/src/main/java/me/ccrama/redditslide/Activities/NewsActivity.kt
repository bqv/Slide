package me.ccrama.redditslide.Activities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivity
import ltd.ucode.slide.ui.Slide
import me.ccrama.redditslide.Adapters.SubredditPostsRealm
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.Fragments.NewsView
import me.ccrama.redditslide.Synccit.MySynccitUpdateTask
import me.ccrama.redditslide.Synccit.SynccitRead
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.NetworkStateReceiver
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.ToggleSwipeViewPager
import net.dean.jraw.managers.AccountManager

class NewsActivity : BaseActivity(), NetworkStateReceiver.NetworkStateReceiverListener {
    lateinit var header: View
    val ANIMATE_DURATION: Long = 250 //duration of animations
    private val ANIMATE_DURATION_OFFSET: Long = 45

    //offset for smoothing out the exit animations
    lateinit var pager: ToggleSwipeViewPager
    var usedArray: CaseInsensitiveArrayList? = null
    var adapter: NewsPagerAdapter? = null
    lateinit var mTabLayout: TabLayout
    lateinit var selectedSub: String //currently selected subreddit
    var inNightMode = false
    var changed = false
    var currentlySubbed = false
    var back = 0
    private val headerHeight = 0 //height of the header
    var reloadItemNumber = -2

    lateinit var networkStateReceiver: NetworkStateReceiver

    override fun networkAvailable() {}

    override fun networkUnavailable() {}

    override fun onBackPressed() {
        finish()
    }

    override fun onPause() {
        super.onPause()
        changed = false
        if (!SettingValues.synccitName.isNullOrEmpty()) {
            MySynccitUpdateTask().execute(
                *SynccitRead.newVisited.toTypedArray<String>())
        }
        if (Authentication.isLoggedIn && Authentication.me != null && Authentication.me!!.hasGold()
            && SynccitRead.newVisited.isNotEmpty()) {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    try {
                        val returned = arrayOfNulls<String>(SynccitRead.newVisited.size)
                        for ((i, s) in SynccitRead.newVisited.withIndex()) {
                            returned[i] = if (!s.contains("t3_")) "t3_$s" else s
                        }
                        AccountManager(Authentication.reddit).storeVisits(*returned)
                        SynccitRead.newVisited = ArrayList<String>()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        //Upon leaving MainActivity--hide the toolbar search if it is visible
        if (findViewById<View>(R.id.toolbar_search).visibility == View.VISIBLE) {
            findViewById<View>(R.id.close_search_toolbar).performClick()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        inNightMode = SettingValues.isNight
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_news)
        mToolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            popupTheme = ColorPreferences(this@NewsActivity).fontStyle.baseId
        }
        setSupportActionBar(mToolbar)
        val window: Window = this.window
        window.statusBarColor = Palette.getDarkerColor(Palette.getDarkerColor(Palette.getDefaultColor()))
        mTabLayout = findViewById(R.id.sliding_tabs)
        header = findViewById(R.id.header)
        pager = findViewById(R.id.content_view)
        mTabLayout = findViewById(R.id.sliding_tabs)
        UserSubscriptions.doNewsSubs(this@NewsActivity)
        /**
         * int for the current base theme selected.
         * 0 = Dark, 1 = Light, 2 = AMOLED, 3 = Dark blue, 4 = AMOLED with contrast, 5 = Sepia
         */
        SettingValues.currentTheme = ColorPreferences(this).fontStyle.themeType
        networkStateReceiver = NetworkStateReceiver().apply {
            addListener(this@NewsActivity)
        }
        try {
            this.registerReceiver(networkStateReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        if (inNightMode != SettingValues.isNight) {
            restartTheme()
        }

        //CrashReportHandler.reinstall()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(networkStateReceiver)
        } catch (ignored: Exception) {
        }
        Slide.hasStarted = false
        super.onDestroy()
    }

    var shouldLoad: String? = null

    fun restartTheme() {
        val intent: Intent = this.intent
        val page = pager.currentItem
        intent.putExtra(EXTRA_PAGE_TO, page)
        finish()
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_real, R.anim.fading_out_real)
    }

    fun scrollToTop() {
        var pastVisiblesItems = 0
        if (adapter!!.currentFragment == null) return
        val firstVisibleItems: IntArray? = ((adapter!!.currentFragment as NewsView?)?.rv?.layoutManager as CatchStaggeredGridLayoutManager)
            .findFirstVisibleItemPositions(null)
        if (firstVisibleItems != null && firstVisibleItems.isNotEmpty()) {
            for (firstVisibleItem in firstVisibleItems) {
                pastVisiblesItems = firstVisibleItem
            }
        }
        if (pastVisiblesItems > 8) {
            (adapter!!.currentFragment as NewsView?)?.rv?.scrollToPosition(0)
            header.animate()
                .translationY(header.height.toFloat())
                .setInterpolator(LinearInterpolator()).duration = 0
        } else {
            (adapter!!.currentFragment as NewsView?)?.rv?.smoothScrollToPosition(0)
        }
        (adapter!!.currentFragment as NewsView?)?.resetScroll()
    }

    var toGoto = 0

    fun setDataSet(data: List<String?>?) {
        if (!data.isNullOrEmpty()) {
            usedArray = CaseInsensitiveArrayList(data)
            if (adapter == null) {
                adapter = NewsPagerAdapter(supportFragmentManager)
            } else {
                adapter!!.notifyDataSetChanged()
            }
            pager.adapter = adapter
            pager.offscreenPageLimit = 1
            if (toGoto == -1) {
                toGoto = 0
            }
            if (toGoto >= usedArray!!.size) {
                toGoto -= 1
            }
            shouldLoad = usedArray!![toGoto]
            selectedSub = usedArray!![toGoto]
            themeSystemBars(usedArray!![toGoto])
            mTabLayout.setSelectedTabIndicatorColor(
                ColorPreferences(this@NewsActivity).getColor(usedArray!![0]))
            pager.currentItem = toGoto
            TabLayoutMediator(mTabLayout, pager) { tab, position ->
                scrollToTop()
            }
            LayoutUtils.scrollToTabAfterLayout(mTabLayout, toGoto)
        } else if (NetworkUtil.isConnected(this)) {
            UserSubscriptions.doNewsSubs(this)
        }
    }

    fun updateMultiNameToSubs(subs: Map<String, String>) {
        newsSubToMap = subs
    }

    fun updateSubs(subs: ArrayList<String?>) {
        if (subs.isEmpty() && !NetworkUtil.isConnected(this)) {
            //todo this
        } else {
            if (loader != null) {
                header.visibility = View.VISIBLE
                setDataSet(subs)
                try {
                    setDataSet(subs)
                } catch (ignored: Exception) {
                }
                loader!!.finish()
                loader = null
            } else {
                setDataSet(subs)
            }
        }
    }

    inner class NewsPagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        protected lateinit var mCurrentFragment: NewsView

        private val pageChangeCallback: OnPageChangeCallback
            = object : OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (positionOffset == 0f) {
                        header.animate()
                            .translationY(0f)
                            .setInterpolator(LinearInterpolator()).duration = 180
                    }
                }

                override fun onPageSelected(position: Int) {
                    App.currentPosition = position
                    selectedSub = usedArray!![position]
                    val page: NewsView? = adapter!!.currentFragment as NewsView?
                    val colorFrom = (header.background as ColorDrawable).color
                    val colorTo = Palette.getColor(selectedSub)
                    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.addUpdateListener { animator ->
                        val color = animator.animatedValue as Int
                        header.setBackgroundColor(color)
                        window.statusBarColor = Palette.getDarkerColor(color)
                        if (SettingValues.colorNavBar) {
                            window.navigationBarColor = Palette.getDarkerColor(color)
                        }
                    }
                    colorAnimation.interpolator = AccelerateDecelerateInterpolator()
                    colorAnimation.duration = 200
                    colorAnimation.start()
                    setRecentBar(selectedSub)
                    mTabLayout.setSelectedTabIndicatorColor(
                        ColorPreferences(this@NewsActivity).getColor(selectedSub))
                    if (page?.adapter != null) {
                        val p: SubredditPostsRealm = page.adapter!!.dataSet
                        if (p.offline) {
                            p.doNewsActivityOffline(this@NewsActivity, p.displayer)
                        }
                    }
                }
            }

        init {
            pager.registerOnPageChangeCallback(pageChangeCallback)

            if (pager.adapter != null) {
                pager.adapter!!.notifyDataSetChanged()
                pager.currentItem = 1
                pager.currentItem = 0
            }
        }

        override fun getItemCount(): Int = if (usedArray == null) {
            1
        } else {
            usedArray!!.size
        }

        override fun createFragment(i: Int): Fragment {
            val f = NewsView()
            val args = Bundle()
            val name: String? = if (newsSubToMap.containsKey(usedArray!![i])) {
                newsSubToMap[usedArray!![i]]
            } else {
                usedArray!![i]
            }
            args.putString("id", name)
            f.arguments = args
            return f
        }

        fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
            /*
            if (reloadItemNumber == position || reloadItemNumber < 0) {
                super.setPrimaryItem(container, position, obj)
                if (usedArray!!.size >= position) doSetPrimary(obj, position)
            } else {
                shouldLoad = usedArray!![reloadItemNumber]
                shouldLoad = if (newsSubToMap.containsKey(usedArray!![reloadItemNumber])) {
                    newsSubToMap[usedArray!![reloadItemNumber]]
                } else {
                    usedArray!![reloadItemNumber]
                }
            }
             */TODO("hmm")
        }

        fun doSetPrimary(obj: Any?, position: Int) {
            if (obj != null && currentFragment !== obj && obj is NewsView) {
                shouldLoad = usedArray!![position]
                shouldLoad = if (newsSubToMap.containsKey(usedArray!![position])) {
                    newsSubToMap[usedArray!![position]]
                } else {
                    usedArray!![position]
                }
                mCurrentFragment = obj
                if (mCurrentFragment.posts == null && mCurrentFragment.isAdded) {
                    mCurrentFragment.doAdapter()
                }
            }
        }

        val currentFragment: Fragment?
            get() = mCurrentFragment

        fun getPageTitle(position: Int): CharSequence {
            return if (usedArray != null) {
                StringUtil.abbreviate(usedArray!![position], 25)
            } else {
                ""
            }
        }
    }

    companion object {
        const val IS_ONLINE = "online"

        // Instance state keys
        const val SUBS = "news"
        private const val EXTRA_PAGE_TO = "PAGE_TO"
        var loader: Loader? = null
        var newsSubToMap: Map<String, String> = HashMap()
    }
}
