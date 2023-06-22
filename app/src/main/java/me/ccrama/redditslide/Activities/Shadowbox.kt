package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Adapters.MultiredditPosts
import me.ccrama.redditslide.Adapters.SubmissionDisplay
import me.ccrama.redditslide.Adapters.SubredditPosts
import me.ccrama.redditslide.ContentType
import me.ccrama.redditslide.Fragments.AlbumFull
import me.ccrama.redditslide.Fragments.MediaFragment
import me.ccrama.redditslide.Fragments.SelftextFull
import me.ccrama.redditslide.Fragments.TitleFull
import me.ccrama.redditslide.Fragments.TumblrFull
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.LastComments
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.PostLoader

class Shadowbox : FullScreenActivity(), SubmissionDisplay {
    @JvmField
    var subredditPosts: PostLoader? = null
    @JvmField
    var subreddit: String? = null
    var firstPage = 0
    private var count = 0
    @JvmField
    var pager: ViewPager? = null
    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT)
        firstPage = intent.extras!!.getInt(EXTRA_PAGE, 0)
        subreddit = intent.extras!!.getString(EXTRA_SUBREDDIT)
        val multireddit = intent.extras!!.getString(EXTRA_MULTIREDDIT)
        val profile = intent.extras!!
            .getString(EXTRA_PROFILE, "")
        subredditPosts = multireddit?.let { MultiredditPosts(it, profile) } ?: SubredditPosts(
            subreddit!!, this@Shadowbox
        )
        subreddit = if (multireddit == null) subreddit else "multi$multireddit"
        if (multireddit == null) {
            shareUrl = "https://reddit.com/r/$subreddit"
        }
        applyDarkColorTheme(subreddit)
        super.onCreate(savedInstance)
        setContentView(R.layout.activity_slide)
        val offline = intent.getLongExtra("offline", 0L)
        val submissions =
            OfflineSubreddit.getSubreddit(subreddit, offline, !Authentication.didOnline, this)
        subredditPosts!!.posts.addAll(submissions!!.submissions.orEmpty())
        count = subredditPosts!!.posts.size
        pager = findViewById<View>(R.id.content_view) as ViewPager
        submissionsPager = ShadowboxPagerAdapter(supportFragmentManager)
        pager!!.adapter = submissionsPager
        pager!!.currentItem = firstPage
        pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (SettingValues.storeHistory) {
                    if (subredditPosts!!.posts[position].isNsfw && !SettingValues.storeNSFWHistory) {
                    } else HasSeen.addSeen(
                        subredditPosts!!.posts[position].permalink
                    )
                }
            }
        })
    }

    var submissionsPager: ShadowboxPagerAdapter? = null
    override fun updateSuccess(submissions: List<IPost>, startIndex: Int) {
        if (SettingValues.storeHistory) LastComments.setCommentsSince(submissions)
        runOnUiThread {
            count = subredditPosts!!.posts.size
            if (startIndex != -1) {
                // TODO determine correct behaviour
                //comments.notifyItemRangeInserted(startIndex, posts.posts.size());
                submissionsPager!!.notifyDataSetChanged()
            } else {
                submissionsPager!!.notifyDataSetChanged()
            }
        }
    }

    override fun updateOffline(submissions: List<IPost>, cacheTime: Long) {
        runOnUiThread {
            count = subredditPosts!!.posts.size
            submissionsPager!!.notifyDataSetChanged()
        }
    }

    override fun updateOfflineError() {}
    override fun updateError() {}
    override fun updateViews() {}
    override fun onAdapterUpdated() {
        submissionsPager!!.notifyDataSetChanged()
    }

    inner class ShadowboxPagerAdapter internal constructor(fm: FragmentManager?) :
        FragmentStatePagerAdapter(
            fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(i: Int): Fragment {
            var f: Fragment? = null
            val t: ContentType.Type = subredditPosts!!.posts[i].contentType!!
            if (subredditPosts!!.posts.size - 2 <= i && subredditPosts!!.hasMore()) {
                subredditPosts!!.loadMore(this@Shadowbox.applicationContext, this@Shadowbox, false)
            }
            when (t) {
                ContentType.Type.GIF, ContentType.Type.IMAGE, ContentType.Type.IMGUR, ContentType.Type.REDDIT, ContentType.Type.EXTERNAL, ContentType.Type.SPOILER, ContentType.Type.DEVIANTART, ContentType.Type.EMBEDDED, ContentType.Type.XKCD, ContentType.Type.REDDIT_GALLERY, ContentType.Type.VREDDIT_DIRECT, ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.LINK, ContentType.Type.STREAMABLE, ContentType.Type.VIDEO -> {
                    f = MediaFragment()
                    val submission = subredditPosts!!.posts[i]
                    val args = Bundle()
                    submission.submission?.let { submission ->
                        var previewUrl = ""
                        if (t != ContentType.Type.XKCD && submission.dataNode
                                .has("preview") && submission.dataNode
                                .get("preview")
                                .get("images")
                                .get(0)
                                .get("source")
                                .has("height")
                        ) { //Load the preview image which has probably already been cached in memory instead of the direct link
                            previewUrl = submission.dataNode
                                .get("preview")
                                .get("images")
                                .get(0)
                                .get("source")
                                .get("url")
                                .asText()
                        }
                        args.putString("contentUrl", submission.url)
                        args.putString("firstUrl", previewUrl)
                        args.putInt("page", i)
                        args.putString("sub", subreddit)
                    }
                    f.setArguments(args)
                }

                ContentType.Type.SELF, ContentType.Type.NONE -> {
                    f = if (subredditPosts!!.posts[i].submission?.selftext.isNullOrEmpty()) {
                        TitleFull()
                    } else {
                        SelftextFull()
                    }
                    val args = Bundle()
                    args.putInt("page", i)
                    args.putString("sub", subreddit)
                    f.arguments = args
                }

                ContentType.Type.TUMBLR -> {
                    f = TumblrFull()
                    val args = Bundle()
                    args.putInt("page", i)
                    args.putString("sub", subreddit)
                    f.setArguments(args)
                }

                ContentType.Type.ALBUM -> {
                    f = AlbumFull()
                    val args = Bundle()
                    args.putInt("page", i)
                    args.putString("sub", subreddit)
                    f.setArguments(args)
                }
            }
            return f
        }

        override fun getCount(): Int {
            return count
        }
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_PAGE = "page"
        const val EXTRA_SUBREDDIT = "subreddit"
        const val EXTRA_MULTIREDDIT = "multireddit"
    }
}
