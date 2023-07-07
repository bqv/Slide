package me.ccrama.redditslide.Activities

import android.R
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.cocosw.bottomsheet.BottomSheet
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.devspark.robototextview.RobotoTypefaces
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.assist.FailReason
import com.nostra13.universalimageloader.core.assist.ImageScaleType
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.imageDownloadButton
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Adapters.ImageGridAdapter
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils.GetAlbumWithCallback
import me.ccrama.redditslide.ImgurAlbum.Image
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.DialogUtil
import me.ccrama.redditslide.util.FileUtil
import me.ccrama.redditslide.util.GifUtils.AsyncLoadGif
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LinkUtil.setTextWithLinks
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.ShareUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.views.ExoVideoView
import me.ccrama.redditslide.views.ToolbarColorizeHelper
import java.io.File
import java.util.Arrays
import kotlin.math.roundToInt

/**
 * This is an extension of Album.java which utilizes a
 * ViewPager for Imgur content instead of a RecyclerView (horizontal vs vertical). It also supports
 * gifs and progress bars which Album.java doesn't.
 */
class AlbumPager : FullScreenActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.home) {
            onBackPressed()
        }
        if (id == ltd.ucode.slide.R.id.vertical) {
            albumSwipe = false
            val i = Intent(this@AlbumPager, Album::class.java)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (intent.hasExtra(SUBREDDIT)) {
                i.putExtra(SUBREDDIT, intent.getStringExtra(SUBREDDIT))
            }
            if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
                i.putExtra(
                    ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                    intent.getStringExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
                )
            }
            i.putExtras(intent)
            startActivity(i)
            finish()
        }
        if (id == ltd.ucode.slide.R.id.grid) {
            mToolbar!!.findViewById<View>(ltd.ucode.slide.R.id.grid).callOnClick()
        }
        if (id == ltd.ucode.slide.R.id.external) {
            openExternally(intent.extras!!.getString("url", ""))
        }
        if (id == ltd.ucode.slide.R.id.comments) {
            val adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            finish()
            datachanged(adapterPosition)
            //getIntent().getStringExtra(MediaView.SUBMISSION_SUBREDDIT));
            //SubmissionAdapter.setOpen(this, getIntent().getStringExtra(MediaView.SUBMISSION_URL));
        }
        if (id == ltd.ucode.slide.R.id.download && images != null) {
            var index = 0
            for (elem: Image in images!!) {
                doImageSave(false, elem.imageUrl, index)
                index++
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            appRestart.edit().putBoolean("tutorialSwipe", true).apply()
        }
    }

    var subreddit: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
            true
        )
        setContentView(ltd.ucode.slide.R.layout.album_pager)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.getStringExtra(SUBREDDIT)
        }
        mToolbar = findViewById<View>(ltd.ucode.slide.R.id.toolbar) as Toolbar
        mToolbar!!.setTitle(ltd.ucode.slide.R.string.type_album)
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mToolbar!!.popupTheme =
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        val url = intent.extras!!.getString("url", "")
        shareUrl = url
        pagerLoad = LoadIntoPager(url, this)
        pagerLoad!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
    }

    var pagerLoad: LoadIntoPager? = null

    inner class LoadIntoPager(var url: String, baseActivity: Activity) : GetAlbumWithCallback(
        url, baseActivity
    ) {
        override fun onError() {
            runOnUiThread {
                try {
                    AlertDialog.Builder(this@AlbumPager)
                        .setTitle(ltd.ucode.slide.R.string.error_album_not_found)
                        .setMessage(ltd.ucode.slide.R.string.error_album_not_found_text)
                        .setNegativeButton(
                            ltd.ucode.slide.R.string.btn_no
                        ) { dialog: DialogInterface?, which: Int -> finish() }
                        .setCancelable(false)
                        .setPositiveButton(
                            ltd.ucode.slide.R.string.btn_yes
                        ) { dialog: DialogInterface?, which: Int ->
                            val i: Intent = Intent(this@AlbumPager, Website::class.java)
                            i.putExtra(LinkUtil.EXTRA_URL, url)
                            startActivity(i)
                            finish()
                        }
                        .show()
                } catch (_: Exception) {
                }
            }
        }

        override fun doWithData(jsonElements: List<Image>) {
            super.doWithData(jsonElements)
            findViewById<View>(ltd.ucode.slide.R.id.progress).visibility = View.GONE
            images = ArrayList(jsonElements)
            p = findViewById<View>(ltd.ucode.slide.R.id.images_horizontal) as ViewPager
            if (supportActionBar != null) {
                supportActionBar!!.setSubtitle(1.toString() + "/" + images!!.size)
            }
            val adapter = AlbumViewPagerAdapter(supportFragmentManager)
            p!!.adapter = adapter
            p!!.currentItem = 1
            findViewById<View>(ltd.ucode.slide.R.id.grid).setOnClickListener {
                val l = layoutInflater
                val body = l.inflate(ltd.ucode.slide.R.layout.album_grid_dialog, null, false)
                val gridview = body.findViewById<GridView>(ltd.ucode.slide.R.id.images)
                gridview.adapter = ImageGridAdapter(this@AlbumPager, images)
                val b = AlertDialog.Builder(this@AlbumPager)
                    .setView(body)
                val d: Dialog = b.create()
                gridview.onItemClickListener =
                    AdapterView.OnItemClickListener { parent, v, position, id ->
                        p!!.currentItem = position + 1
                        d.dismiss()
                    }
                d.show()
            }
            p!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (position != 0) {
                        if (supportActionBar != null) {
                            supportActionBar!!.setSubtitle((position).toString() + "/" + images!!.size)
                        }
                    }
                    if (position == 0 && positionOffset < 0.2) {
                        finish()
                    }
                }
            })
            adapter.notifyDataSetChanged()
        }
    }

    var p: ViewPager? = null
    var images: List<Image>? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(ltd.ucode.slide.R.menu.album_pager, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(ltd.ucode.slide.R.id.comments).isVisible = false
        }
        return true
    }

    private inner class AlbumViewPagerAdapter internal constructor(m: FragmentManager?) :
        FragmentStatePagerAdapter(
            (m)!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(i: Int): Fragment {
            var i = i
            if (i == 0) {
                return BlankFragment()
            }
            i--
            val current = images!![i]
            if (current.isAnimated) {
                //do gif stuff
                val f: Fragment = Gif()
                val args = Bundle()
                args.putInt("page", i)
                f.arguments = args
                return f
            } else {
                val f: Fragment = ImageFullNoSubmission()
                val args = Bundle()
                args.putInt("page", i)
                f.arguments = args
                return f
            }
        }

        override fun getCount(): Int {
            return if (images == null) {
                0
            } else images!!.size + 1
        }
    }

    class Gif() : Fragment() {
        private var i = 0
        private var gif: View? = null
        var rootView: ViewGroup? = null
        var loader: ProgressBar? = null
        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            if (this.isVisible) {
                if (!isVisibleToUser) // If we are becoming invisible, then...
                {
                    (gif as ExoVideoView?)!!.pause()
                    gif!!.visibility = View.GONE
                }
                if (isVisibleToUser) // If we are becoming visible, then...
                {
                    (gif as ExoVideoView?)!!.play()
                    gif!!.visibility = View.VISIBLE
                }
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            rootView = inflater.inflate(
                ltd.ucode.slide.R.layout.submission_gifcard_album, container,
                false
            ) as ViewGroup
            loader = rootView!!.findViewById(ltd.ucode.slide.R.id.gifprogress)
            gif = rootView!!.findViewById(ltd.ucode.slide.R.id.gif)
            gif!!.visibility = View.VISIBLE
            val v = gif as ExoVideoView?
            v!!.clearFocus()
            val url = (activity as AlbumPager?)!!.images!![i].imageUrl
            AsyncLoadGif(
                requireActivity(),
                rootView!!.findViewById(ltd.ucode.slide.R.id.gif),
                loader,
                null,
                null,
                false,
                true,
                rootView!!.findViewById(ltd.ucode.slide.R.id.size),
                (activity as AlbumPager?)!!.subreddit!!,
                requireActivity().intent.getStringExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
            ).execute(url)
            rootView!!.findViewById<View>(ltd.ucode.slide.R.id.more).setOnClickListener {
                (activity as AlbumPager?)!!.showBottomSheetImage(
                    url,
                    true,
                    i
                )
            }
            rootView!!.findViewById<View>(ltd.ucode.slide.R.id.save)
                .setOnClickListener { MediaView.doOnClick!!.run() }
            if (!imageDownloadButton) {
                rootView!!.findViewById<View>(ltd.ucode.slide.R.id.save).visibility = View.INVISIBLE
            }
            return rootView
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val bundle = this.arguments
            i = bundle!!.getInt("page", 0)
        }
    }

    fun showBottomSheetImage(
        contentUrl: String?, isGif: Boolean,
        index: Int
    ) {
        val attrs = intArrayOf(ltd.ucode.slide.R.attr.tintColor)
        val ta = obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val external = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_open_in_browser)
        val share = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_share)
        val image = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_image)
        val save = resources.getDrawable(ltd.ucode.slide.R.drawable.ic_download)
        val drawableSet = Arrays.asList(external, share, image, save)
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(this).title(contentUrl)
        b.sheet(2, external, getString(ltd.ucode.slide.R.string.open_externally))
        b.sheet(5, share, getString(ltd.ucode.slide.R.string.submission_link_share))
        if (!isGif) b.sheet(3, image, getString(ltd.ucode.slide.R.string.share_image))
        b.sheet(4, save, getString(ltd.ucode.slide.R.string.submission_save_image))
        b.listener(object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    (2) -> {
                        openExternally((contentUrl)!!)
                    }

                    (3) -> {
                        ShareUtil.shareImage(contentUrl, this@AlbumPager)
                    }

                    (5) -> {
                        defaultShareText("", contentUrl, this@AlbumPager)
                    }

                    (4) -> {
                        doImageSave(isGif, contentUrl, index)
                    }
                }
            }
        })
        b.show()
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?, index: Int) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                showFirstDialog()
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog()
            } else {
                val i = Intent(this, ImageDownloadNotificationService::class.java)
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                if (intent.hasExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)) {
                    i.putExtra(
                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                        intent.getStringExtra(ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE)
                    )
                }
                i.putExtra("index", index)
                startService(i)
            }
        } else {
            MediaView.doOnClick!!.run()
        }
    }

    class ImageFullNoSubmission() : Fragment() {
        private var i = 0
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(
                ltd.ucode.slide.R.layout.album_image_pager,
                container,
                false
            ) as ViewGroup
            if ((activity as AlbumPager?)!!.images == null) {
                (activity as AlbumPager?)!!.pagerLoad!!.onError()
            } else {
                val current = (activity as AlbumPager?)!!.images!![i]
                val url = current.imageUrl
                var lq = false
                if (SettingValues.loadImageLq && (SettingValues.lowResAlways || ((!NetworkUtil.isConnectedWifi(
                        activity
                    )
                            && SettingValues.lowResMobile)))
                ) {
                    val lqurl = (url.substring(0, url.lastIndexOf("."))
                            + (if (SettingValues.lqLow) "m" else (if (SettingValues.lqMid) "l" else "h"))
                            + url.substring(url.lastIndexOf(".")))
                    loadImage(rootView, this, lqurl, (activity as AlbumPager?)!!.images!!.size == 1)
                    lq = true
                } else {
                    loadImage(rootView, this, url, (activity as AlbumPager?)!!.images!!.size == 1)
                }
                run {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.more)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                (getActivity() as AlbumPager?)!!.showBottomSheetImage(url, false, i)
                            }
                        })
                    run {
                        rootView.findViewById<View>(ltd.ucode.slide.R.id.save)
                            .setOnClickListener(object : View.OnClickListener {
                                override fun onClick(v2: View) {
                                    (getActivity() as AlbumPager?)!!.doImageSave(false, url, i)
                                }
                            })
                        if (!imageDownloadButton) {
                            rootView.findViewById<View>(ltd.ucode.slide.R.id.save)
                                .setVisibility(View.INVISIBLE)
                        }
                    }
                }
                run {
                    var title: String = ""
                    var description: String = ""
                    if (current.getTitle() != null) {
                        val text: List<String> = SubmissionParser.getBlocks(current.getTitle())
                        title = text.get(0).trim { it <= ' ' }
                    }
                    if (current.getDescription() != null) {
                        val text: List<String> =
                            SubmissionParser.getBlocks(current.getDescription())
                        description = text.get(0).trim { it <= ' ' }
                    }
                    if (title.isEmpty() && description.isEmpty()) {
                        rootView.findViewById<View>(ltd.ucode.slide.R.id.panel)
                            .setVisibility(View.GONE)
                        (rootView.findViewById<View>(ltd.ucode.slide.R.id.margin)).setPadding(
                            0,
                            0,
                            0,
                            0
                        )
                    } else if (title.isEmpty()) {
                        setTextWithLinks(
                            description,
                            rootView.findViewById(ltd.ucode.slide.R.id.title)
                        )
                    } else {
                        setTextWithLinks(title, rootView.findViewById(ltd.ucode.slide.R.id.title))
                        setTextWithLinks(
                            description,
                            rootView.findViewById(ltd.ucode.slide.R.id.body)
                        )
                    }
                    run {
                        val type: Int =
                            FontPreferences(context).fontTypeComment.typeface
                        val typeface: Typeface = if (type >= 0) {
                            RobotoTypefaces.obtainTypeface(requireContext(), type!!)
                        } else {
                            Typeface.DEFAULT
                        }
                        (rootView.findViewById<View>(ltd.ucode.slide.R.id.body) as SpoilerRobotoTextView).setTypeface(
                            typeface
                        )
                    }
                    run {
                        val type: Int =
                            FontPreferences(context).fontTypeTitle.typeface
                        val typeface: Typeface = if (type >= 0) {
                            RobotoTypefaces.obtainTypeface(requireContext(), type!!)
                        } else {
                            Typeface.DEFAULT
                        }
                        (rootView.findViewById<View>(ltd.ucode.slide.R.id.title) as SpoilerRobotoTextView).setTypeface(
                            typeface
                        )
                    }
                    val l: SlidingUpPanelLayout =
                        rootView.findViewById(ltd.ucode.slide.R.id.sliding_layout)
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.title)
                        .setOnClickListener { l.panelState = PanelState.EXPANDED }
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.body)
                        .setOnClickListener { l.panelState = PanelState.EXPANDED }
                }
                if (lq) {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.hq)
                        .setOnClickListener {
                            loadImage(
                                rootView, this@ImageFullNoSubmission, url,
                                (activity as AlbumPager?)!!.images!!.size == 1
                            )
                            rootView.findViewById<View>(ltd.ucode.slide.R.id.hq).visibility =
                                View.GONE
                        }
                } else {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.hq).visibility = View.GONE
                }
                if (requireActivity().intent.hasExtra(MediaView.SUBMISSION_URL)) {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.comments)
                        .setOnClickListener {
                            requireActivity().finish()
                            datachanged(adapterPosition)
                        }
                } else {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.comments).visibility =
                        View.GONE
                }
            }
            return rootView
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val bundle = this.arguments
            i = bundle!!.getInt("page", 0)
        }
    }

    private fun showFirstDialog() {
        runOnUiThread { DialogUtil.showFirstDialog(this@AlbumPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun showErrorDialog() {
        runOnUiThread { DialogUtil.showErrorDialog(this@AlbumPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun onFolderSelection(
        folder: File
    ) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this, (
                    getString(
                        ltd.ucode.slide.R.string.settings_set_image_location,
                        folder.absolutePath
                    )
                            + folder.absolutePath), Toast.LENGTH_LONG
        ).show()
    }


    companion object {
        private var adapterPosition = 0
        @JvmField
        val SUBREDDIT = "subreddit"
        @JvmStatic
        fun loadImage(rootView: View, f: Fragment, url: String?, single: Boolean) {
            val image = rootView.findViewById<SubsamplingScaleImageView>(ltd.ucode.slide.R.id.image)
            image.setMinimumDpi(70)
            image.setMinimumTileDpi(240)
            val fakeImage = ImageView(f.activity)
            val size = rootView.findViewById<TextView>(ltd.ucode.slide.R.id.size)
            fakeImage.layoutParams = LinearLayout.LayoutParams(image.width, image.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            (f.requireActivity().application as App).imageLoader!!
                .displayImage(url, ImageViewAware(fakeImage),
                    DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                        .cacheOnDisk(true)
                        .imageScaleType(if (single) ImageScaleType.NONE else ImageScaleType.NONE_SAFE)
                        .cacheInMemory(false)
                        .build(), object : ImageLoadingListener {
                        override fun onLoadingStarted(imageUri: String, view: View) {
                            size.visibility = View.VISIBLE
                        }

                        override fun onLoadingFailed(
                            imageUri: String, view: View,
                            failReason: FailReason
                        ) {
                            Log.v("Slide", "LOADING FAILED")
                        }

                        override fun onLoadingComplete(
                            imageUri: String, view: View,
                            loadedImage: Bitmap
                        ) {
                            size.visibility = View.GONE
                            image.setImage(ImageSource.Bitmap(loadedImage))
                            (rootView.findViewById<View>(ltd.ucode.slide.R.id.progress)).visibility =
                                View.GONE
                        }

                        override fun onLoadingCancelled(imageUri: String, view: View) {
                            Log.v("Slide", "LOADING CANCELLED")
                        }
                    }
                ) { imageUri, view, current, total ->
                    size.text = FileUtil.readableFileSize(total.toLong())
                    (rootView.findViewById<View>(ltd.ucode.slide.R.id.progress) as ProgressBar).progress =
                        (100.0f * current / total).roundToInt()
                }
        }
    }
}
