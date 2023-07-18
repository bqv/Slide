package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ltd.ucode.network.ContentType
import ltd.ucode.network.ContentType.Companion.getContentType
import ltd.ucode.slide.R
import me.ccrama.redditslide.Adapters.CommentUrlObject
import me.ccrama.redditslide.Fragments.AlbumFullComments
import me.ccrama.redditslide.Fragments.MediaFragmentComment

class ShadowboxComments : FullScreenActivity() {
    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        if (comments == null || comments!!.isEmpty()) {
            finish()
        }
        applyDarkColorTheme(comments!![0].comment.comment.subredditName)
        super.onCreate(savedInstance)
        setContentView(R.layout.activity_slide)
        val pager = findViewById<View>(R.id.content_view) as ViewPager2
        commentPager = ShadowboxCommentsPagerAdapter(supportFragmentManager)
        pager.adapter = commentPager
    }

    var commentPager: ShadowboxCommentsPagerAdapter? = null

    inner class ShadowboxCommentsPagerAdapter internal constructor(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            var f: Fragment? = null
            val args = Bundle()
            val comment = comments!![i].comment.comment
            val url = comments!![i].url
            val t = getContentType(url)

            when (t) {
                ContentType.Type.GIF, ContentType.Type.IMAGE, ContentType.Type.IMGUR, ContentType.Type.REDDIT, ContentType.Type.EXTERNAL, ContentType.Type.XKCD, ContentType.Type.SPOILER, ContentType.Type.DEVIANTART, ContentType.Type.REDDIT_GALLERY, ContentType.Type.EMBEDDED, ContentType.Type.LINK, ContentType.Type.STREAMABLE, ContentType.Type.VIDEO -> {
                    f = MediaFragmentComment()
                    args.putString("contentUrl", url)
                    args.putString("firstUrl", url)
                    args.putInt("page", i)
                    args.putString("sub", comment.subredditName)
                    f.setArguments(args)
                }

                ContentType.Type.ALBUM -> {
                    f = AlbumFullComments()
                    args.putInt("page", i)
                    f.setArguments(args)
                }

                else -> {}
            }
            return f!!
        }

        override fun getItemCount(): Int = comments!!.size
    }

    companion object {
        var comments: ArrayList<CommentUrlObject>? = null
    }
}
