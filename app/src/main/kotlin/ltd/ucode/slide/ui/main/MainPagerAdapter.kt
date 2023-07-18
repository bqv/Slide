package ltd.ucode.slide.ui.main

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.Fragments.SubmissionsView
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.StringUtil

open class MainPagerAdapter(private val mainActivity: MainActivity, fm: FragmentManager) : FragmentStateAdapter(fm, mainActivity.lifecycle) {
    protected var mCurrentFragment: SubmissionsView? = null
    private val pageChangeListener: ViewPager2.OnPageChangeCallback
        = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (positionOffset == 0f) {
                    mainActivity.header!!.animate()
                        .translationY(0f)
                        .setInterpolator(LinearInterpolator()).duration = 180
                    mainActivity.doSubSidebarNoLoad(mainActivity.usedArray!![position])
                }
            }

            override fun onPageSelected(position: Int) {
                App.currentPosition = position
                mainActivity.selectedSub = mainActivity.usedArray!![position]
                val page = mainActivity.adapter!!.currentFragment as SubmissionsView?
                if (mainActivity.hea != null) {
                    mainActivity.hea!!.setBackgroundColor(Palette.getColor(mainActivity.selectedSub))
                    if (mainActivity.accountsArea != null) {
                        mainActivity.accountsArea!!.setBackgroundColor(Palette.getDarkerColor(mainActivity.selectedSub))
                    }
                }
                val colorFrom = (mainActivity.header!!.background as ColorDrawable).color
                val colorTo = Palette.getColor(mainActivity.selectedSub)
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    mainActivity.header!!.setBackgroundColor(color)
                    run {
                        mainActivity.window.statusBarColor = Palette.getDarkerColor(color)
                        if (SettingValues.colorNavBar) {
                            mainActivity.window.navigationBarColor = Palette.getDarkerColor(color)
                        }
                    }
                }
                colorAnimation.interpolator = AccelerateDecelerateInterpolator()
                colorAnimation.duration = 200
                colorAnimation.start()
                mainActivity.setRecentBar(mainActivity.selectedSub)
                if (SettingValues.single || mainActivity.mTabLayout == null) {
                    //Smooth out the fading animation for the toolbar subreddit search UI
                    if ((SettingValues.subredditSearchMethod
                                == me.ccrama.redditslide.Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                || SettingValues.subredditSearchMethod
                                == me.ccrama.redditslide.Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                        && mainActivity.findViewById<AutoCompleteTextView>(R.id.toolbar_search).visibility
                        == View.VISIBLE
                    ) {
                        Handler().postDelayed(
                            { mainActivity.supportActionBar!!.title = mainActivity.selectedSub },
                            mainActivity.ANIMATE_DURATION + mainActivity.ANIMATE_DURATION_OFFSET
                        )
                    } else {
                        mainActivity.supportActionBar!!.setTitle(mainActivity.selectedSub)
                    }
                } else {
                    mainActivity.mTabLayout!!.setSelectedTabIndicatorColor(
                        ColorPreferences(mainActivity).getColor(mainActivity.selectedSub)
                    )
                }
                if (page?.adapter != null) {
                    val p = page.adapter!!.dataSet
                    if (p.offline && !MainActivity.isRestart) {
                        p.doMainActivityOffline(mainActivity, p.displayer)
                    }
                }
            }
        }

    init {
        mainActivity.pager!!.registerOnPageChangeCallback(pageChangeListener)

        if (mainActivity.pager!!.adapter != null) {
            mainActivity.pager!!.adapter!!.notifyDataSetChanged()
            mainActivity.pager!!.currentItem = 1
            mainActivity.pager!!.currentItem = 0
        }
    }

    override fun getItemCount(): Int {
        return if (mainActivity.usedArray == null) {
            1
        } else {
            mainActivity.usedArray!!.size
        }
    }

    override fun createFragment(i: Int): Fragment {
        val f = SubmissionsView()
        val args = Bundle()
        val name: String? = if (MainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray!![i])) {
            MainActivity.multiNameToSubsMap[mainActivity.usedArray!![i]]
        } else {
            mainActivity.usedArray!![i]
        }
        args.putString("id", name)
        f.arguments = args
        return f
    }

    fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
        /*
        if (mainActivity.reloadItemNumber == position || mainActivity.reloadItemNumber < 0) {
            super.setPrimaryItem(container, position, obj)
            if (mainActivity.usedArray!!.size >= position) doSetPrimary(obj, position)
        } else {
            MainActivity.shouldLoad = mainActivity.usedArray!![mainActivity.reloadItemNumber]
            if (MainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray!![mainActivity.reloadItemNumber])) {
                MainActivity.shouldLoad = MainActivity.multiNameToSubsMap[mainActivity.usedArray!![mainActivity.reloadItemNumber]]
            } else {
                MainActivity.shouldLoad = mainActivity.usedArray!![mainActivity.reloadItemNumber]
            }
        }
         */TODO("hmm")
    }

    open fun doSetPrimary(obj: Any?, position: Int) {
        if (obj != null && currentFragment !== obj && position != mainActivity.toOpenComments && obj is SubmissionsView) {
            MainActivity.shouldLoad = mainActivity.usedArray!![position]
            if (MainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray!![position])) {
                MainActivity.shouldLoad = MainActivity.multiNameToSubsMap[mainActivity.usedArray!![position]]
            } else {
                MainActivity.shouldLoad = mainActivity.usedArray!![position]
            }
            mCurrentFragment = obj
            if (mCurrentFragment!!.posts == null && mCurrentFragment!!.isAdded) {
                mCurrentFragment!!.doAdapter()
            }
        }
    }

    open val currentFragment: Fragment?
        get() = mCurrentFragment

    fun getPageTitle(position: Int): CharSequence? {
        return if (mainActivity.usedArray != null) {
            StringUtil.abbreviate(mainActivity.usedArray!![position], 25)
        } else {
            ""
        }
    }
}
