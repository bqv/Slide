package me.ccrama.redditslide.Activities

import android.R
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
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
import ltd.ucode.network.ContentType.Companion.isGif
import ltd.ucode.slide.R.attr
import ltd.ucode.slide.R.drawable
import ltd.ucode.slide.R.id
import ltd.ucode.slide.R.layout
import ltd.ucode.slide.R.string
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.imageDownloadButton
import me.ccrama.redditslide.Adapters.ImageGridAdapterTumblr
import me.ccrama.redditslide.Fragments.BlankFragment
import me.ccrama.redditslide.Fragments.SubmissionsView.Companion.datachanged
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Tumblr.Photo
import me.ccrama.redditslide.Tumblr.TumblrUtils.GetTumblrPostWithCallback
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
import java.net.URI
import java.net.URISyntaxException

/**
 * This is an extension of Album.java which utilizes a
 * ViewPager2 for Imgur content instead of a RecyclerView (horizontal vs vertical). It also supports
 * gifs and progress bars which Album.java doesn't.
 */
class TumblrPager : FullScreenActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.home) {
            onBackPressed()
        }
        if (id == ltd.ucode.slide.R.id.vertical) {
            albumSwipe = false
            val i = Intent(this@TumblrPager, Tumblr::class.java)
            if (intent.hasExtra(MediaView.SUBMISSION_URL)) {
                i.putExtra(
                    MediaView.SUBMISSION_URL,
                    intent.getStringExtra(MediaView.SUBMISSION_URL)
                )
            }
            if (intent.hasExtra(SUBREDDIT)) {
                i.putExtra(SUBREDDIT, intent.getStringExtra(SUBREDDIT))
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
            val adapterPosition: Int = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
            finish()
            datachanged(adapterPosition)
            //getIntent().getStringExtra(MediaView.SUBMISSION_SUBREDDIT));
            //SubmissionAdapter.setOpen(this, getIntent().getStringExtra(MediaView.SUBMISSION_URL));
        }
        if (id == ltd.ucode.slide.R.id.download) {
            var index: Int = 0
            for (elem: Photo in images!!) {
                doImageSave(false, elem.originalSize.url, index)
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

    public override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE),
            true
        )
        setContentView(layout.album_pager)

        //Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mToolbar = findViewById<View>(id.toolbar) as Toolbar
        mToolbar!!.setTitle(string.type_album)
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, Color.WHITE, this)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        if (intent.hasExtra(SUBREDDIT)) {
            subreddit = intent.getStringExtra(SUBREDDIT)
        }
        mToolbar!!.popupTheme = ColorPreferences(this).getDarkThemeSubreddit(ColorPreferences.FONT_STYLE)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        val url: String = intent.extras!!.getString("url", "")
        shareUrl = url
        LoadIntoPager(url, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        if (!appRestart.contains("tutorialSwipe")) {
            startActivityForResult(Intent(this, SwipeTutorial::class.java), 3)
        }
    }

    inner class LoadIntoPager constructor(var url: String, baseActivity: Activity) :
        GetTumblrPostWithCallback(
            url, baseActivity
        ) {
        override fun onError() {
            val i: Intent = Intent(this@TumblrPager, Website::class.java)
            i.putExtra(LinkUtil.EXTRA_URL, url)
            startActivity(i)
            finish()
        }

        override fun doWithData(jsonElements: List<Photo>) {
            super.doWithData(jsonElements)
            findViewById<View>(ltd.ucode.slide.R.id.progress).visibility = View.GONE
            images = ArrayList(jsonElements)
            p = findViewById<View>(ltd.ucode.slide.R.id.images_horizontal) as ViewPager2?
            if (supportActionBar != null) {
                supportActionBar!!.setSubtitle(1.toString() + "/" + images!!.size)
            }
            val adapter: TumblrViewPagerAdapter =
                TumblrViewPagerAdapter(supportFragmentManager)
            p!!.adapter = adapter
            p!!.currentItem = 1
            findViewById<View>(ltd.ucode.slide.R.id.grid).setOnClickListener {
                val l: LayoutInflater = layoutInflater
                val body: View =
                    l.inflate(layout.album_grid_dialog, null, false)
                val gridview: GridView = body.findViewById(ltd.ucode.slide.R.id.images)
                gridview.adapter = ImageGridAdapterTumblr(this@TumblrPager, images)
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@TumblrPager)
                    .setView(body)
                val d: Dialog = builder.create()
                gridview.onItemClickListener =
                    AdapterView.OnItemClickListener { parent, v, position, id ->
                        p!!.currentItem = position + 1
                        d.dismiss()
                    }
                d.show()
            }
            p!!.registerOnPageChangeCallback(object : OnPageChangeCallback() {
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

    var p: ViewPager2? = null
    var images: List<Photo>? = null
    var subreddit: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(ltd.ucode.slide.R.menu.album_pager, menu)
        adapterPosition = intent.getIntExtra(MediaView.ADAPTER_POSITION, -1)
        if (adapterPosition < 0) {
            menu.findItem(id.comments).isVisible = false
        }
        return true
    }

    private inner class TumblrViewPagerAdapter(fm: FragmentManager) : FragmentStateAdapter(fm, lifecycle) {
        override fun createFragment(i: Int): Fragment {
            var i: Int = i
            if (i == 0) {
                return BlankFragment()
            }
            i--
            val current: Photo = images!!.get(i)
            try {
                if (isGif(URI(current.originalSize.url))) {
                    //do gif stuff
                    val f: Fragment = Gif()
                    val args: Bundle = Bundle()
                    args.putInt("page", i)
                    f.arguments = args
                    return f
                } else {
                    val f: Fragment = ImageFullNoSubmission()
                    val args: Bundle = Bundle()
                    args.putInt("page", i)
                    f.arguments = args
                    return f
                }
            } catch (e: URISyntaxException) {
                val f: Fragment = ImageFullNoSubmission()
                val args: Bundle = Bundle()
                args.putInt("page", i)
                f.arguments = args
                return f
            }
        }

        override fun getItemCount(): Int {
            if (images == null) {
                return 0
            }
            return images!!.size + 1
        }
    }

    class Gif : Fragment() {
        private var i: Int = 0
        private var gif: View? = null
        var rootView: ViewGroup? = null
        var loader: ProgressBar? = null
        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            if (isVisible) {
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
                layout.submission_gifcard_album, container,
                false
            ) as ViewGroup?
            loader = rootView!!.findViewById(ltd.ucode.slide.R.id.gifprogress)
            gif = rootView!!.findViewById(ltd.ucode.slide.R.id.gif)
            gif!!.visibility = View.VISIBLE
            val v: ExoVideoView? = gif as ExoVideoView?
            v!!.clearFocus()
            val url: String =
                (activity as TumblrPager?)!!.images!![i].originalSize.url
            AsyncLoadGif(
                requireActivity(),
                rootView!!.findViewById(ltd.ucode.slide.R.id.gif),
                loader,
                null,
                null,
                false,
                true,
                rootView!!.findViewById(ltd.ucode.slide.R.id.size),
                (activity as TumblrPager?)!!.subreddit!!,
                null
            ).execute(url)
            rootView!!.findViewById<View>(ltd.ucode.slide.R.id.more)
                .setOnClickListener {
                    (activity as TumblrPager?)!!.showBottomSheetImage(
                        url,
                        true,
                        i
                    )
                }
            rootView!!.findViewById<View>(ltd.ucode.slide.R.id.save)
                .setOnClickListener { MediaView.doOnClick!!.run() }
            return rootView
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val bundle: Bundle? = arguments
            i = bundle!!.getInt("page", 0)
        }
    }

    fun showBottomSheetImage(
        contentUrl: String?, isGif: Boolean,
        index: Int
    ) {
        val attrs: IntArray = intArrayOf(attr.tintColor)
        val ta: TypedArray = obtainStyledAttributes(attrs)
        val color: Int = ta.getColor(0, Color.WHITE)
        val external: Drawable =
            resources.getDrawable(drawable.ic_open_in_browser)
        val share: Drawable = resources.getDrawable(drawable.ic_share)
        val image: Drawable = resources.getDrawable(drawable.ic_image)
        val save: Drawable = resources.getDrawable(drawable.ic_download)
        val drawableSet: List<Drawable> = listOf(external, share, image, save)
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b: BottomSheet.Builder = BottomSheet.Builder(this).title(contentUrl)
        b.sheet(2, external, getString(string.open_externally))
        b.sheet(5, share, getString(string.submission_link_share))
        if (!isGif) b.sheet(3, image, getString(string.share_image))
        b.sheet(4, save, getString(string.submission_save_image))
        b.listener { dialog, which ->
            when (which) {
                (2) -> {
                    openExternally((contentUrl)!!)
                }

                (3) -> {
                    ShareUtil.shareImage(contentUrl, this@TumblrPager)
                }

                (5) -> {
                    defaultShareText("", contentUrl, this@TumblrPager)
                }

                (4) -> {
                    doImageSave(isGif, contentUrl, index)
                }
            }
        }
        b.show()
    }

    fun doImageSave(isGif: Boolean, contentUrl: String?, index: Int) {
        if (!isGif) {
            if (appRestart.getString("imagelocation", "")!!.isEmpty()) {
                showFirstDialog()
            } else if (!File(appRestart.getString("imagelocation", "")).exists()) {
                showErrorDialog()
            } else {
                val i: Intent = Intent(this, ImageDownloadNotificationService::class.java)
                i.putExtra("actuallyLoaded", contentUrl)
                if (subreddit != null && !subreddit!!.isEmpty()) i.putExtra("subreddit", subreddit)
                i.putExtra("index", index)
                startService(i)
            }
        } else {
            MediaView.doOnClick!!.run()
        }
    }

    class ImageFullNoSubmission : Fragment() {
        private var i: Int = 0
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView: ViewGroup = inflater.inflate(
                layout.album_image_pager,
                container,
                false
            ) as ViewGroup
            val current: Photo = (activity as TumblrPager?)!!.images!!.get(i)
            val url: String = current.originalSize.url
            var lq: Boolean = false
            if (SettingValues.loadImageLq && ((SettingValues.lowResAlways
                        || (!NetworkUtil.isConnectedWifi(activity)
                        && SettingValues.lowResMobile))) && (current.altSizes != null) && !current.altSizes
                    .isEmpty()
            ) {
                val lqurl: String =
                    current.altSizes.get(current.altSizes.size / 2).url
                loadImage(rootView, this, lqurl)
                lq = true
            } else {
                loadImage(rootView, this, url)
            }
            run {
                rootView.findViewById<View>(ltd.ucode.slide.R.id.more)
                    .setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View) {
                            (activity as TumblrPager?)!!.showBottomSheetImage(url, false, i)
                        }
                    })
                run {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.save)
                        .setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v2: View) {
                                (activity as TumblrPager?)!!.doImageSave(false, url, i)
                            }
                        })
                    if (!imageDownloadButton) {
                        rootView.findViewById<View>(ltd.ucode.slide.R.id.save).visibility =
                            View.INVISIBLE
                    }
                }
            }
            run {
                val title: String = ""
                var description: String = ""
                if (current.caption != null) {
                    val text: List<String> = SubmissionParser.getBlocks(current.caption)
                    description = text.get(0).trim({ it <= ' ' })
                }
                if (title.isEmpty() && description.isEmpty()) {
                    rootView.findViewById<View>(ltd.ucode.slide.R.id.panel).visibility = View.GONE
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
                    setTextWithLinks(
                        title,
                        rootView.findViewById(ltd.ucode.slide.R.id.title)
                    )
                    setTextWithLinks(
                        description,
                        rootView.findViewById(ltd.ucode.slide.R.id.body)
                    )
                }
                run {
                    val type: Int = FontPreferences(context).fontTypeComment.typeface
                    val typeface: Typeface = if (type >= 0) {
                        RobotoTypefaces.obtainTypeface(requireContext(), type!!)
                    } else {
                        Typeface.DEFAULT
                    }
                    rootView.findViewById<SpoilerRobotoTextView>(ltd.ucode.slide.R.id.body).typeface = typeface
                }
                run {
                    val type: Int = FontPreferences(context).fontTypeTitle.typeface
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
                        loadImage(rootView, this@ImageFullNoSubmission, url)
                        rootView.findViewById<View>(ltd.ucode.slide.R.id.hq).visibility = View.GONE
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
                rootView.findViewById<View>(ltd.ucode.slide.R.id.comments).visibility = View.GONE
            }
            return rootView
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val bundle: Bundle? = arguments
            i = bundle!!.getInt("page", 0)
        }
    }

    private fun showFirstDialog() {
        runOnUiThread { DialogUtil.showFirstDialog(this@TumblrPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun showErrorDialog() {
        runOnUiThread { DialogUtil.showErrorDialog(this@TumblrPager) { _, folder -> onFolderSelection(folder) } }
    }

    private fun onFolderSelection(folder: File) {
        appRestart.edit().putString("imagelocation", folder.absolutePath).apply()
        Toast.makeText(
            this, (
                    getString(
                        string.settings_set_image_location,
                        folder.absolutePath
                    )
                            + folder.absolutePath), Toast.LENGTH_LONG
        ).show()
    }


    companion object {
        private var adapterPosition: Int = 0
        @JvmField
        val SUBREDDIT: String = "subreddit"
        private fun loadImage(rootView: View, f: Fragment, url: String) {
            val image: SubsamplingScaleImageView = rootView.findViewById(id.image)
            image.setMinimumDpi(70)
            image.setMinimumTileDpi(240)
            val fakeImage: ImageView = ImageView(f.activity)
            val size: TextView = rootView.findViewById(id.size)
            fakeImage.layoutParams = LinearLayout.LayoutParams(image.width, image.height)
            fakeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            (f.requireActivity().application as App).imageLoader!!
                .displayImage(url, ImageViewAware(fakeImage),
                    DisplayImageOptions.Builder().resetViewBeforeLoading(true)
                        .cacheOnDisk(true)
                        .imageScaleType(ImageScaleType.NONE)
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
                            (rootView.findViewById<View>(id.progress)).visibility =
                                View.GONE
                        }

                        override fun onLoadingCancelled(imageUri: String, view: View) {
                            Log.v("Slide", "LOADING CANCELLED")
                        }
                    }
                ) { imageUri, view, current, total ->
                    size.text = FileUtil.readableFileSize(total.toLong())
                    (rootView.findViewById<View>(id.progress) as ProgressBar).progress =
                        Math.round(100.0f * current / total)
                }
        }
    }
}
