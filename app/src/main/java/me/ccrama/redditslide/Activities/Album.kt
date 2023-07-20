package me.ccrama.redditslide.Activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Adapters.AlbumView
import me.ccrama.redditslide.Fragments.BlankFragment
import ltd.ucode.slide.ui.submissionView.SubmissionsViewFragment.Companion.datachanged
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils.GetAlbumWithCallback
import me.ccrama.redditslide.ImgurAlbum.Image
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.views.PreCachingLayoutManager
import me.ccrama.redditslide.views.ToolbarColorizeHelper
import java.io.File

/**
 * This class is responsible for accessing the Imgur api to get
 * the album json data from a URL or Imgur hash. It extends FullScreenActivity and supports swipe
 * from anywhere.
 */
class Album : FullScreenActivity() {
    private var images: List<Image>? = null
    private var adapterPosition = 0
    fun onFolderSelection(folder: File) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this,
            getString(R.string.settings_set_image_location, folder.absolutePath),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
        }
        if (id == R.id.slider) {
            albumSwipe = true
            val i = Intent(this@Album, AlbumPager::class.java)
            val adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            i.putExtra(MediaView.ADAPTER_POSITION, adapterPosition)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (submissionTitle != null) {
                i.putExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE, submissionTitle)
            }
            i.putExtra("url", url)
            startActivity(i)
            finish()
        }
        if (id == R.id.grid) {
            mToolbar!!.findViewById<View>(R.id.grid).callOnClick()
        }
        if (id == R.id.comments) {
            datachanged(adapterPosition)
            finish()
        }
        if (id == R.id.external) {
            openExternally(url!!)
        }
        if (id == R.id.download) {
            for (elem in images!!) {
                doImageSave(false, elem.imageUrl)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                DialogUtil.showFirstDialog(this@Album) { _, file ->
                    this@Album.onFolderSelection(file)
                }
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                DialogUtil.showErrorDialog(this@Album) { _, file ->
                    this@Album.onFolderSelection(file)
                }
            } else {
                val i = Intent(this, ImageDownloadNotificationService::class.java)
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                if (submissionTitle != null) {
                    i.putExtra(
                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                        submissionTitle
                    )
                }
                startService(i)
            }
        } else {
            MediaView.doOnClick!!.run()
        }
    }

    var url: String? = null
    var subreddit: String? = null
    var submissionTitle: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.album_vertical, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(R.id.comments).isVisible = false
        }
        return true
    }

    @JvmField
    var album: AlbumPagerAdapter? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
            true
        )
        setContentView(R.layout.album)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.extras!!.getString(SUBREDDIT)
        }
        if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
            submissionTitle =
                intent.extras!!.getString(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
        }
        val pager = findViewById<View>(R.id.images) as ViewPager2
        album = AlbumPagerAdapter(supportFragmentManager)
        pager.adapter = album
        pager.currentItem = 1
        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == 0 && positionOffsetPixels == 0) {
                    finish()
                }
                if (position == 0
                    && (pager.adapter as AlbumPagerAdapter?)!!.blankPage != null
                ) {
                    (pager.adapter as AlbumPagerAdapter?)!!.blankPage!!.doOffset(positionOffset)
                    (pager.adapter as AlbumPagerAdapter?)!!.blankPage!!.realBack.setBackgroundColor(
                        Palette.adjustAlpha(positionOffset * 0.7f)
                    )
                }
            }
        }
        )
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            appRestart.edit().putBoolean("tutorialSwipe", true).apply()
        }
    }

    inner class AlbumPagerAdapter(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        var blankPage: BlankFragment? = null
        @JvmField var album: AlbumFrag? = null

        override fun createFragment(i: Int): Fragment {
            return if (i == 0) {
                blankPage = BlankFragment()
                blankPage!!
            } else {
                album = AlbumFrag()
                album!!
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }

    class AlbumFrag : Fragment() {
        var rootView: View? = null
        @JvmField
        var recyclerView: RecyclerView? = null
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            rootView = inflater.inflate(R.layout.fragment_verticalalbum, container, false)
            val mLayoutManager = PreCachingLayoutManager(activity)
            recyclerView = rootView!!.findViewById(R.id.images)
            recyclerView!!.layoutManager = mLayoutManager
            (activity!! as Album).url = requireActivity().intent.extras!!
                .getString(EXTRA_URL, "")
            (activity!! as BaseActivity).shareUrl = (activity!! as Album).url
            LoadIntoRecycler((activity!! as Album).url!!, requireActivity()).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR
            )
            (activity!! as Album).mToolbar = rootView!!.findViewById(R.id.toolbar)
            (activity!! as Album).mToolbar!!.setTitle(R.string.type_album)
            ToolbarColorizeHelper.colorizeToolbar(
                (activity!! as Album).mToolbar, Color.WHITE,
                activity
            )
            (activity!! as Album).setSupportActionBar((activity!! as Album).mToolbar)
            (activity!! as Album).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (activity!! as Album).mToolbar!!.popupTheme =
                ColorPreferences(activity).getDarkThemeSubreddit(
                    ColorPreferences.FONT_STYLE
                )
            return rootView
        }

        inner class LoadIntoRecycler(var url: String, baseActivity: Activity) :
            GetAlbumWithCallback(
                url, baseActivity
            ) {
            override fun onError() {
                if (activity != null) {
                    activity!!.runOnUiThread {
                        try {
                            AlertDialog.Builder(activity!!)
                                .setTitle(R.string.error_album_not_found)
                                .setMessage(R.string.error_album_not_found_text)
                                .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int -> activity!!.finish() }
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                                    val i = Intent(
                                        activity, Website::class.java
                                    )
                                    i.putExtra(LinkUtil.EXTRA_URL, url)
                                    startActivity(i)
                                    activity!!.finish()
                                }
                                .show()
                        } catch (e: Exception) {
                        }
                    }
                }
            }

            override fun doWithData(jsonElements: List<Image>) {
                super.doWithData(jsonElements)
                if (activity != null) {
                    activity!!.findViewById<View>(R.id.progress).visibility = View.GONE
                    val albumActivity = activity as Album?
                    albumActivity!!.images = ArrayList(jsonElements)
                    val adapter = AlbumView(
                        baseActivity, albumActivity.images,
                        activity!!.findViewById<View>(R.id.toolbar).height,
                        albumActivity.subreddit, albumActivity.submissionTitle
                    )
                    recyclerView!!.adapter = adapter
                }
            }
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val SUBREDDIT = "subreddit"
    }
}
