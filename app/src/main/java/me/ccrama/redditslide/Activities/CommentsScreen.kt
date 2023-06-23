package me.ccrama.redditslide.Activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import ltd.ucode.lemmy.data.type.PostId
import ltd.ucode.slide.App.Companion.setDefaultErrorHandler
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.fullCommentOverride
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.CommentsScreen.CommentsScreenPagerAdapter
import me.ccrama.redditslide.Adapters.MultiredditPosts
import me.ccrama.redditslide.Adapters.SubmissionDisplay
import me.ccrama.redditslide.Adapters.SubredditPosts
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.LastComments
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.KeyboardUtil

/**
 * This activity is responsible for the view when clicking on a post, showing the post and its
 * comments underneath with the slide left/right for the next post.
 *
 *
 * When the end of the currently loaded posts is being reached, more posts are loaded asynchronously
 * in [CommentsScreenPagerAdapter].
 *
 *
 * Comments are displayed in the [CommentPage] fragment.
 */
class CommentsScreen : BaseActivityAnim(), SubmissionDisplay {
    @JvmField var currentPosts: ArrayList<IPost?>? = null
    @JvmField var subredditPosts: PostLoader? = null
    var firstPage = 0
    var comments: CommentsScreenPagerAdapter? = null
    private var subreddit: String? = null
    private var baseSubreddit: String? = null
    var multireddit: String? = null
    var profile: String? = null
    var postId: PostId? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        return if (SettingValues.commentVolumeNav) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_SEARCH -> (comments!!.currentFragment as CommentPage?)!!.onKeyDown(
                    keyCode,
                    event
                )

                else -> super.dispatchKeyEvent(event)
            }
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    override fun onPause() {
        super.onPause()
        KeyboardUtil.hideKeyboard(this, findViewById<View>(android.R.id.content).windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!appRestart.contains("tutorialSwipeComment")) {
            appRestart.edit().putBoolean("tutorialSwipeComment", true).apply()
        } else if (!appRestart.contains("tutorial_comm")) {
            appRestart.edit().putBoolean("tutorial_comm", true).apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 14) {
            comments!!.notifyDataSetChanged()
            //todo make this work
        }
        if (requestCode == 333) {
            appRestart.edit().putBoolean("tutorialSwipeComments", true).apply()
        }
    }

    @JvmField var currentPage = 0
    var seen: ArrayList<Int>? = null
    var popup = false
    public override fun onCreate(savedInstance: Bundle?) {
        popup = (SettingValues.isPro
                && (resources.configuration.orientation
                == Configuration.ORIENTATION_LANDSCAPE) && !fullCommentOverride)
        seen = ArrayList()
        if (popup) {
            disableSwipeBackLayout()
            applyColorTheme()
            setTheme(R.style.popup)
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            super.onCreate(savedInstance)
            setContentView(R.layout.activity_slide_popup)
        } else {
            overrideSwipeFromAnywhere()
            applyColorTheme()
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.decorView.background = null
            super.onCreate(savedInstance)
            setContentView(R.layout.activity_slide)
        }
        setDefaultErrorHandler(this)
        firstPage = intent.extras!!.getInt(EXTRA_PAGE, -1)
        baseSubreddit = intent.extras!!.getString(EXTRA_SUBREDDIT)
        subreddit = baseSubreddit
        multireddit = intent.extras!!.getString(EXTRA_MULTIREDDIT)
        profile = intent.extras!!.getString(EXTRA_PROFILE, "")
        postId = PostId(intent.extras!!.getInt(EXTRA_POSTID))
        currentPosts = ArrayList()
        if (multireddit != null) {
            subredditPosts = MultiredditPosts(multireddit!!, profile!!)
        } else {
            baseSubreddit = subreddit!!.lowercase()
            subredditPosts = SubredditPosts(baseSubreddit!!, this@CommentsScreen)
        }
        if (firstPage == RecyclerView.NO_POSITION || firstPage < 0) {
            firstPage = 0
            //IS SINGLE POST
        } else {
            val o = OfflineSubreddit.getSubreddit(
                if (multireddit == null) baseSubreddit else "multi$multireddit",
                OfflineSubreddit.currentid, !Authentication.didOnline, this@CommentsScreen
            )
            subredditPosts!!.posts.addAll(o!!.submissions.orEmpty())
            currentPosts!!.addAll(subredditPosts!!.posts)
        }
        if (intent.hasExtra(EXTRA_FULLNAME)) {
            val fullname = intent.getStringExtra(EXTRA_FULLNAME)
            for (i in currentPosts!!.indices) {
                if (currentPosts!![i]!!.permalink == fullname) {
                    if (i != firstPage) firstPage = i
                    break
                }
            }
        }
        if (currentPosts!!.isEmpty() || currentPosts!!.size < firstPage || currentPosts!![firstPage] == null || firstPage < 0) {
            finish()
        } else {
            updateSubredditAndSubmission(currentPosts!![firstPage])
            val pager = findViewById<View>(R.id.content_view) as ViewPager
            comments = CommentsScreenPagerAdapter(supportFragmentManager)
            pager.adapter = comments
            currentPage = firstPage
            pager.currentItem = firstPage + 1
            pager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (position <= firstPage && positionOffsetPixels == 0) {
                        finish()
                    }
                    if (position == firstPage && !popup) {
                        if ((pager.adapter as CommentsScreenPagerAdapter?)!!.blankPage != null) {
                            (pager.adapter as CommentsScreenPagerAdapter?)!!.blankPage!!.doOffset(
                                positionOffset
                            )
                        }
                        pager.setBackgroundColor(Palette.adjustAlpha(positionOffset * 0.7f))
                    }
                }

                override fun onPageSelected(position: Int) {
                    var position = position
                    if (position != firstPage && position < currentPosts!!.size) {
                        position -= 1
                        if (position < 0) position = 0
                        updateSubredditAndSubmission(currentPosts!![position])
                        if (currentPosts!!.size - 2 <= position && subredditPosts!!.hasMore()) {
                            subredditPosts!!.loadMore(
                                this@CommentsScreen.applicationContext,
                                this@CommentsScreen, false
                            )
                        }
                        currentPage = position
                        seen!!.add(position)
                        val conData = Bundle()
                        conData.putIntegerArrayList("seen", seen)
                        conData.putInt("lastPage", position)
                        val intent = Intent()
                        intent.putExtras(conData)
                        setResult(RESULT_OK, intent)
                    }
                }
            }
            )
        }
        if (!appRestart.contains("tutorialSwipeComments")) {
            val i = Intent(this, SwipeTutorial::class.java)
            i.putExtra(
                "subtitle",
                "Swipe from the left edge to exit comments.\n\nYou can swipe in the middle to get to the previous/next submission."
            )
            startActivityForResult(i, 333)
        }
    }

    private fun updateSubredditAndSubmission(post: IPost?) {
        subreddit = post!!.permalink
        if (post.permalink == null) {
            subreddit = "Promoted"
        }
        themeSystemBars(subreddit)
        setRecentBar(subreddit)
    }

    override fun updateSuccess(submissions: List<IPost>, startIndex: Int) {
        if (SettingValues.storeHistory) LastComments.setCommentsSince(submissions)
        currentPosts!!.clear()
        currentPosts!!.addAll(submissions)
        runOnUiThread {
            if (startIndex != -1) {
                // TODO determine correct behaviour
                //comments.notifyItemRangeInserted(startIndex, posts.posts.size());
                comments!!.notifyDataSetChanged()
            } else {
                comments!!.notifyDataSetChanged()
            }
        }
    }

    override fun updateOffline(submissions: List<IPost>, cacheTime: Long) {
        runOnUiThread { comments!!.notifyDataSetChanged() }
    }

    override fun updateOfflineError() {}
    override fun updateError() {}
    override fun updateViews() {}
    override fun onAdapterUpdated() {
        comments!!.notifyDataSetChanged()
    }

    inner class CommentsScreenPagerAdapter internal constructor(fm: FragmentManager?) :
        FragmentStatePagerAdapter(
            fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        private var mCurrentFragment: CommentPage? = null
        var blankPage: BlankFragment? = null
        val currentFragment: Fragment?
            get() = mCurrentFragment

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
            if (currentFragment !== `object` && `object` is CommentPage) {
                mCurrentFragment = `object`
                if (!mCurrentFragment!!.loaded && mCurrentFragment!!.isAdded) {
                    mCurrentFragment!!.doAdapter(true)
                }
            }
        }

        override fun getItem(i: Int): Fragment {
            var i = i
            return if (i <= firstPage || i == 0) {
                blankPage = BlankFragment()
                blankPage!!
            } else {
                i -= 1
                val f: Fragment = CommentPage()
                val args = Bundle()
                val name = currentPosts!![i]!!.permalink
                args.putString("id", name.substring(3))
                args.putBoolean("archived", currentPosts!![i]!!.isArchived)
                args.putBoolean("contest", currentPosts!![i]!!.isContest)
                args.putBoolean("locked", currentPosts!![i]!!.isLocked)
                args.putInt("page", i)
                args.putString("subreddit", currentPosts!![i]!!.groupName)
                args.putString(
                    "baseSubreddit",
                    if (multireddit == null) baseSubreddit else "multi$multireddit"
                )
                f.arguments = args
                f
            }
        }

        override fun getCount(): Int {
            return currentPosts!!.size + 1
        }
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_PAGE = "page"
        const val EXTRA_SUBREDDIT = "subreddit"
        const val EXTRA_MULTIREDDIT = "multireddit"
        const val EXTRA_FULLNAME = "fullname"
        const val EXTRA_POSTID = "postid"
        const val EXTRA_POSTS = "posts"
    }
}
