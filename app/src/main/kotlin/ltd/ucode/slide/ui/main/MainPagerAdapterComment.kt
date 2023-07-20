package ltd.ucode.slide.ui.main

import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import me.ccrama.redditslide.Fragments.CommentPage
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.views.ViewPager2Extensions.setSwipeLeftOnly

class MainPagerAdapterComment(private val mainActivity: MainActivity,
                              fm: FragmentManager,
) : IMainPagerAdapter(fm, mainActivity.lifecycle) {
    protected var mCurrentFragment: SubmissionsViewFragment? = null
    @JvmField var size = mainActivity.usedArray!!.size
    @JvmField var storedFragment: Fragment? = null
    var mCurrentComments: CommentPage? = null

    init {
        mainActivity.pager!!.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (positionOffset == 0f) {
                    if (position != mainActivity.toOpenComments) {
                        mainActivity.pager!!.setSwipeLeftOnly(true)
                        mainActivity.header!!.setBackgroundColor(
                            Palette.getColor(
                                mainActivity.usedArray!![position]
                            )
                        )
                        mainActivity.doPageSelectedComments(position)
                        if (position == mainActivity.toOpenComments - 1 && mainActivity.adapter != null && mainActivity.adapter!!.currentFragment != null) {
                            val page = mainActivity.adapter!!.currentFragment as SubmissionsViewFragment?
                            if (page?.adapter != null) {
                                page.adapter!!.refreshView()
                            }
                        }
                    } else {
                        if (mainActivity.mAsyncGetSubreddit != null) {
                            mainActivity.mAsyncGetSubreddit!!.cancel(true)
                        }
                        if (mainActivity.header!!.translationY == 0f) {
                            mainActivity.header!!.animate()
                                .translationY(-mainActivity.header!!.height * 1.5f)
                                .setInterpolator(LinearInterpolator()).duration = 180
                        }
                        mainActivity.pager!!.setSwipeLeftOnly(true)
                        mainActivity.themeSystemBars(mainActivity.openingComments!!.groupName.lowercase())
                        mainActivity.setRecentBar(mainActivity.openingComments!!.groupName.lowercase())
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                if (position == mainActivity.toOpenComments - 1 && mainActivity.adapter != null && mainActivity.adapter!!.currentFragment != null) {
                    val page = mainActivity.adapter!!.currentFragment as SubmissionsViewFragment?
                    if (page?.adapter != null) {
                        page.adapter!!.refreshView()
                        val p = page.adapter!!.dataSet
                        if (p.offline && !MainActivity.isRestart) {
                            p.doMainActivityOffline(mainActivity, p.displayer)
                        }
                    }
                } else {
                    val page = mainActivity.adapter!!.currentFragment as SubmissionsViewFragment?
                    if (page?.adapter != null) {
                        val p = page.adapter!!.dataSet
                        if (p.offline && !MainActivity.isRestart) {
                            p.doMainActivityOffline(mainActivity, p.displayer)
                        }
                    }
                }
            }
        })
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (mainActivity.usedArray == null) {
            1
        } else {
            size
        }
    }

    override fun createFragment(i: Int): Fragment {
        return if (mainActivity.openingComments == null || i != mainActivity.toOpenComments) {
            val f = SubmissionsViewFragment()
            val args = Bundle()
            if (mainActivity.usedArray!!.size > i) {
                if (MainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray!![i])) {
                    //if (usedArray.get(i).co
                    args.putString("id", MainActivity.multiNameToSubsMap[mainActivity.usedArray!![i]])
                } else {
                    args.putString("id", mainActivity.usedArray!![i])
                }
            }
            f.arguments = args
            f
        } else {
            val f: Fragment = CommentPage()
            val args = Bundle()
            val name = mainActivity.openingComments!!.uri
            args.putString("id", mainActivity.openingComments!!.rowId.toString())
            args.putBoolean("archived", mainActivity.openingComments!!.isArchived)
            args.putBoolean("contest", mainActivity.openingComments!!.isContest)
            args.putBoolean("locked", mainActivity.openingComments!!.isLocked)
            args.putInt("page", mainActivity.currentComment)
            args.putString("subreddit", mainActivity.openingComments!!.groupName)
            args.putString("baseSubreddit", mainActivity.subToDo)
            f.arguments = args
            f
        }
    }

    override fun doSetPrimary(obj: Any?, position: Int) {
        if (position != mainActivity.toOpenComments) {
            if (MainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray!![position])) {
                MainActivity.shouldLoad = MainActivity.multiNameToSubsMap[mainActivity.usedArray!![position]]
            } else {
                MainActivity.shouldLoad = mainActivity.usedArray!![position]
            }
            if (currentFragment !== obj) {
                mCurrentFragment = obj as SubmissionsViewFragment?
                if (mCurrentFragment != null && mCurrentFragment!!.posts == null && mCurrentFragment!!.isAdded
                ) {
                    mCurrentFragment!!.doAdapter()
                }
            }
        } else if (obj is CommentPage) {
            mCurrentComments = obj
        }
    }

    override val currentFragment: Fragment?
        get() = mCurrentFragment

    fun getItemPosition(obj: Any): Int {
        /*
        return if (obj !== storedFragment) POSITION_NONE else POSITION_UNCHANGED
         */TODO("hmm")
    }

    /*
    override fun getPageTitle(position: Int): CharSequence? {
        return if (mainActivity.usedArray != null && position != mainActivity.toOpenComments) {
            StringUtil.abbreviate(mainActivity.usedArray!![position], 25)
        } else {
            ""
        }
    }
     *///TODO("hmm")
}
