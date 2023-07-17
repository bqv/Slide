package me.ccrama.redditslide.Activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.Fragments.ContributionsView
import me.ccrama.redditslide.Fragments.HistoryView
import me.ccrama.redditslide.UserTags
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LinkUtil.formatURL
import me.ccrama.redditslide.util.LinkUtil.openUrl
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.SortingUtil
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.fluent.FluentRedditClient
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.Account
import net.dean.jraw.models.Trophy
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import uz.shift.colorpicker.LineColorPicker
import uz.shift.colorpicker.OnColorChangedListener
import java.util.Locale

class Profile : BaseActivityAnim() {
    private var name: String? = null
    private var account: Account? = null
    private var trophyCase: List<Trophy>? = null
    private var pager: ViewPager? = null
    private var tabs: TabLayout? = null
    private var usedArray: Array<String>? = null
    var isSavedView = false
    private var friend = false
    private var sortItem: MenuItem? = null
    private var categoryItem: MenuItem? = null
    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        name = intent.extras!!
            .getString(EXTRA_PROFILE, "")
        shareUrl = "https://reddit.com/u/$name"
        applyColorTheme()
        setContentView(R.layout.activity_profile)
        setupUserAppBar(R.id.toolbar, name, true, name)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        profSort = Sorting.HOT
        profTime = TimePeriod.ALL
        findViewById<View>(R.id.header).setBackgroundColor(Palette.getColorUser(name))
        tabs = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabs!!.tabMode = TabLayout.MODE_SCROLLABLE
        tabs!!.setSelectedTabIndicatorColor(ColorPreferences(this@Profile).getColor("no sub"))
        pager = findViewById<View>(R.id.content_view) as ViewPager
        if ((name == Authentication.name)) setDataSet(
            arrayOf(
                getString(R.string.profile_overview),
                getString(R.string.profile_comments),
                getString(R.string.profile_submitted),
                getString(R.string.profile_gilded),
                getString(R.string.profile_upvoted),
                getString(R.string.profile_downvoted),
                getString(R.string.profile_saved),
                getString(R.string.profile_hidden),
                getString(R.string.profile_history)
            )
        ) else setDataSet(
            arrayOf(
                getString(R.string.profile_overview),
                getString(R.string.profile_comments),
                getString(R.string.profile_submitted),
                getString(R.string.profile_gilded)
            )
        )
        getProfile().execute(name)
        pager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                isSavedView = position == 6
                findViewById<View>(R.id.header).animate()
                    .translationY(0f)
                    .setInterpolator(LinearInterpolator()).duration = 180
                if (sortItem != null) {
                    sortItem!!.isVisible = position < 3
                }
                if ((categoryItem != null) && (Authentication.me != null) && Authentication.me!!.hasGold()) {
                    categoryItem!!.isVisible = position == 6
                }
            }
        })
        if (intent.hasExtra(EXTRA_SAVED) && (name == Authentication.name)) {
            pager!!.currentItem = 6
        }
        if (intent.hasExtra(EXTRA_COMMENT) && (name == Authentication.name)) {
            pager!!.currentItem = 1
        }
        if (intent.hasExtra(EXTRA_SUBMIT) && (name == Authentication.name)) {
            pager!!.currentItem = 2
        }
        if (intent.hasExtra(EXTRA_HISTORY) && (name == Authentication.name)) {
            pager!!.currentItem = 8
        }
        if (intent.hasExtra(EXTRA_UPVOTE) && (name == Authentication.name)) {
            pager!!.currentItem = 4
        }
        isSavedView = pager!!.currentItem == 6
        if (pager!!.currentItem != 0) {
            LayoutUtils.scrollToTabAfterLayout(tabs, pager!!.currentItem)
        }
    }

    private fun doClick() {
        if (account == null) {
            try {
                AlertDialog.Builder(this@Profile)
                    .setTitle(R.string.profile_err_title)
                    .setMessage(R.string.profile_err_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .setCancelable(false)
                    .setOnDismissListener { dialog: DialogInterface? -> onBackPressed() }
                    .show()
            } catch (e: WindowManager.BadTokenException) {
                Log.w(LogUtil.getTag(), "Activity already in background, dialog not shown $e")
            }
            return
        }
        if ((account!!.dataNode.has("is_suspended") && account!!.dataNode["is_suspended"].asBoolean()
                    && !name.equals(Authentication.name, ignoreCase = true))
        ) {
            try {
                AlertDialog.Builder(this@Profile)
                    .setTitle(R.string.account_suspended)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, whichButton: Int -> finish() }
                    .setOnDismissListener { dialog: DialogInterface? -> finish() }
                    .show()
            } catch (e: WindowManager.BadTokenException) {
                Log.w(LogUtil.getTag(), "Activity already in background, dialog not shown $e")
            }
        }
    }

    private fun setDataSet(data: Array<String>) {
        usedArray = data
        val adapter = ProfilePagerAdapter(supportFragmentManager)
        pager!!.adapter = adapter
        pager!!.offscreenPageLimit = 1
        tabs!!.setupWithViewPager(pager)
    }

    private inner class getProfile : AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg params: String?): Void? {
            try {
                if (!isValidUsername(params[0]!!)) {
                    account = null
                    return null
                }
                account = Authentication.reddit!!.getUser(params[0])
                trophyCase = FluentRedditClient(Authentication.reddit).user(params[0]).trophyCase()
            } catch (ignored: RuntimeException) {
            }
            return null
        }

        public override fun onPostExecute(voidd: Void?) {
            doClick()
        }
    }

    private inner class ProfilePagerAdapter(fm: FragmentManager?) :
        FragmentStatePagerAdapter(
            (fm)!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(i: Int): Fragment {
            if (i < 8) {
                val f: Fragment = ContributionsView()
                val args = Bundle()
                args.putString("id", name)
                val place: String
                when (i) {
                    1 -> place = "comments"
                    2 -> place = "submitted"
                    3 -> place = "gilded"
                    4 -> place = "liked"
                    5 -> place = "disliked"
                    6 -> place = "saved"
                    7 -> place = "hidden"
                    0 -> place = "overview"
                    else -> place = "overview"
                }
                args.putString("where", place)
                f.arguments = args
                return f
            } else {
                return HistoryView()
            }
        }

        override fun getCount(): Int {
            return if (usedArray == null) {
                1
            } else {
                usedArray!!.size
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return usedArray!![position]
        }
    }

    fun openPopup() {
        val popup = PopupMenu(this@Profile, findViewById(R.id.anchor), Gravity.RIGHT)
        val base = SortingUtil.getSortingSpannables(profSort)
        for (s: Spannable? in base) {
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s: Spannable in base) {
                if ((s == item.title)) {
                    break
                }
                i++
            }
            when (i) {
                0 -> profSort = (Sorting.HOT)
                1 -> profSort = (Sorting.NEW)
                2 -> profSort = (Sorting.RISING)
                3 -> {
                    profSort = (Sorting.TOP)
                    openPopupTime()
                    return@OnMenuItemClickListener true
                }

                4 -> {
                    profSort = (Sorting.CONTROVERSIAL)
                    openPopupTime()
                    return@OnMenuItemClickListener true
                }
            }
            SortingUtil.sorting[name!!.lowercase()] = profSort
            val current = pager!!.currentItem
            val adapter = ProfilePagerAdapter(supportFragmentManager)
            pager!!.adapter = adapter
            pager!!.offscreenPageLimit = 1
            tabs!!.setupWithViewPager(pager)
            pager!!.currentItem = current
            true
        })
        popup.show()
    }

    fun openPopupTime() {
        val popup = PopupMenu(this@Profile, findViewById(R.id.anchor), Gravity.RIGHT)
        val base = SortingUtil.getSortingTimesSpannables(profTime)
        for (s: Spannable? in base) {
            val m = popup.menu.add(s)
        }
        popup.setOnMenuItemClickListener { item ->
            LogUtil.v("Chosen is " + item.order)
            var i = 0
            for (s: Spannable in base) {
                if ((s == item.title)) {
                    break
                }
                i++
            }
            when (i) {
                0 -> profTime = (TimePeriod.HOUR)
                1 -> profTime = (TimePeriod.DAY)
                2 -> profTime = (TimePeriod.WEEK)
                3 -> profTime = (TimePeriod.MONTH)
                4 -> profTime = (TimePeriod.YEAR)
                5 -> profTime = (TimePeriod.ALL)
            }
            SortingUtil.sorting[name!!.lowercase()] = profSort
            SortingUtil.times[name!!.lowercase()] = profTime
            val current = pager!!.currentItem
            val adapter = ProfilePagerAdapter(supportFragmentManager)
            pager!!.adapter = adapter
            pager!!.offscreenPageLimit = 1
            tabs!!.setupWithViewPager(pager)
            pager!!.currentItem = current
            true
        }
        popup.show()
    }

    @JvmField
    var category: String? = null
    var subreddit: String? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_profile, menu)
        //used to hide the sort item on certain Profile tabs
        sortItem = menu.findItem(R.id.sort)
        categoryItem = menu.findItem(R.id.category)
        categoryItem!!.isVisible = false
        sortItem!!.isVisible = false
        val position = if (pager == null) 0 else pager!!.currentItem
        if (sortItem != null) {
            sortItem!!.isVisible = position < 3
        }
        if ((categoryItem != null) && (Authentication.me != null) && Authentication.me!!.hasGold()) {
            categoryItem!!.isVisible = position == 6
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            (android.R.id.home) -> onBackPressed()
            (R.id.category) -> object : AsyncTask<Void?, Void?, List<String>>() {
                var d: Dialog? = null
                public override fun onPreExecute() {
                    d = MaterialDialog(this@Profile)
                        //.progress(true, 100)
                        .message(R.string.misc_please_wait)
                        .title(R.string.profile_category_loading)
                        .also { it.show() }
                }

                override fun doInBackground(vararg params: Void?): List<String> {
                    return try {
                        val categories: MutableList<String> =
                            ArrayList(AccountManager(Authentication.reddit).savedCategories)
                        categories.add(0, "No category")
                        categories
                    } catch (e: Exception) {
                        e.printStackTrace()
                        //probably has no categories?
                        listOf("No category")
                    }
                }

                public override fun onPostExecute(data: List<String>) {
                    try {
                        MaterialDialog(this@Profile)
                            .title(R.string.profile_category_select)
                            .listItems(items = data) { dialog: MaterialDialog, which: Int, text: CharSequence ->
                                val t = data[which]
                                category = if (which == 0) null else t
                                val current = pager!!.currentItem
                                val adapter = ProfilePagerAdapter(supportFragmentManager)
                                pager!!.adapter = adapter
                                pager!!.offscreenPageLimit = 1
                                tabs!!.setupWithViewPager(pager)
                                pager!!.currentItem = current
                            }
                            .show()
                        if (d != null) {
                            d!!.dismiss()
                        }
                    } catch (ignored: Exception) {
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            (R.id.info) -> {
                if (account != null && trophyCase != null) {
                    val inflater = layoutInflater
                    val dialoglayout = inflater.inflate(R.layout.colorprofile, null)
                    val title = dialoglayout.findViewById<TextView>(R.id.title)
                    title.text = name
                    if ((account!!.dataNode.has("is_employee")
                                && account!!.dataNode["is_employee"].asBoolean())
                    ) {
                        val admin = SpannableStringBuilder("[A]")
                        admin.setSpan(
                            RelativeSizeSpan(.67f), 0, admin.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        title.append(" ")
                        title.append(admin)
                    }
                    dialoglayout.findViewById<View>(R.id.share).setOnClickListener {
                        defaultShareText(
                            getString(R.string.profile_share, name),
                            "https://www.reddit.com/u/$name", this@Profile
                        )
                    }
                    val currentColor = Palette.getColorUser(name)
                    title.setBackgroundColor(currentColor)
                    val info = getString(R.string.profile_age,
                        TimeUtils.getTimeSince(account!!.created.time, this@Profile)
                    )
                    /*todo better if (account.hasGold() &&account.getDataNode().has("gold_expiration") ) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(account.getDataNode().get("gold_expiration").asLong());
                    info.append("Gold expires on " + new SimpleDateFormat("dd/MM/yy").format(c.getTime()));
                }*/(dialoglayout.findViewById<View>(R.id.moreinfo) as TextView).text = info
                    var tag = UserTags.getUserTag(name)
                    tag = if (tag.isEmpty()) {
                        getString(R.string.profile_tag_user)
                    } else {
                        getString(R.string.profile_tag_user_existing, tag)
                    }
                    (dialoglayout.findViewById<View>(R.id.tagged) as TextView).text = tag
                    val l = dialoglayout.findViewById<LinearLayout>(R.id.trophies_inner)
                    dialoglayout.findViewById<View>(R.id.tag).setOnClickListener { v: View? ->
                        val b: MaterialDialog = MaterialDialog(this@Profile)
                            .title(text = getString(R.string.profile_tag_set, name))
                            .input(hintRes = R.string.profile_tag,
                                prefill = UserTags.getUserTag(name)) { dialog, input -> }
                        b.positiveButton(R.string.profile_btn_tag) { dialog: MaterialDialog ->
                            UserTags.setUserTag(
                                name,
                                dialog.getInputField().text.toString()
                            )
                            var tag1: String = UserTags.getUserTag(name)
                            tag1 = if (tag1.isEmpty()) {
                                getString(R.string.profile_tag_user)
                            } else {
                                getString(R.string.profile_tag_user_existing, tag1)
                            }
                            (dialoglayout.findViewById<View>(R.id.tagged) as TextView).text =
                                tag1
                        }
                        b.neutralButton(R.string.btn_cancel) { _ -> }
                        if (UserTags.isUserTagged(name)) {
                            b.negativeButton(R.string.profile_btn_untag) { dialog: MaterialDialog ->
                                UserTags.removeUserTag(name)
                                var tag1: String = UserTags.getUserTag(name)
                                tag1 = if (tag1.isEmpty()) {
                                    getString(R.string.profile_tag_user)
                                } else {
                                    getString(R.string.profile_tag_user_existing, tag1)
                                }
                                (dialoglayout.findViewById<View>(R.id.tagged) as TextView).text =
                                    tag1
                            }
                        }
                        b.show()
                    }
                    if (trophyCase!!.isEmpty()) {
                        dialoglayout.findViewById<View>(R.id.trophies).visibility = View.GONE
                    } else {
                        for (t: Trophy in trophyCase!!) {
                            val view = layoutInflater.inflate(R.layout.trophy, null)
                            (applicationContext as App).imageLoader!!.displayImage(
                                t.icon, (view.findViewById<View>(
                                    R.id.image
                                ) as ImageView)
                            )
                            (view.findViewById<View>(R.id.trophyTitle) as TextView).text =
                                t.fullName
                            if (t.aboutUrl != null && !t.aboutUrl.equals(
                                    "null",
                                    ignoreCase = true
                                )
                            ) {
                                view.setOnClickListener {
                                    openUrl(
                                        formatURL(t.aboutUrl).toString(),
                                        Palette.getColorUser(account!!.fullName),
                                        this@Profile
                                    )
                                }
                            }
                            l.addView(view)
                        }
                    }
                    if (Authentication.isLoggedIn) {
                        dialoglayout.findViewById<View>(R.id.pm)
                            .setOnClickListener {
                                val i = Intent(this@Profile, SendMessage::class.java)
                                i.putExtra(SendMessage.EXTRA_NAME, name)
                                startActivity(i)
                            }
                        friend = account!!.isFriend
                        if (friend) {
                            (dialoglayout.findViewById<View>(R.id.friend) as TextView).setText(R.string.profile_remove_friend)
                        } else {
                            (dialoglayout.findViewById<View>(R.id.friend) as TextView).setText(R.string.profile_add_friend)
                        }
                        dialoglayout.findViewById<View>(R.id.friend_body)
                            .setOnClickListener(object : View.OnClickListener {
                                override fun onClick(v: View) {
                                    object : AsyncTask<Void?, Void?, Void?>() {
                                        override fun doInBackground(vararg params: Void?): Void? {
                                            friend = if (friend) {
                                                try {
                                                    AccountManager(Authentication.reddit).deleteFriend(
                                                        name
                                                    )
                                                } catch (ignored: Exception) {
                                                    //Will throw java.lang.IllegalStateException: No Content-Type header was found, but it still works.
                                                }
                                                false
                                            } else {
                                                AccountManager(Authentication.reddit).updateFriend(
                                                    name
                                                )
                                                true
                                            }
                                            return null
                                        }

                                        public override fun onPostExecute(voids: Void?) {
                                            if (friend) {
                                                (dialoglayout.findViewById<View>(R.id.friend) as TextView).setText(
                                                    R.string.profile_remove_friend
                                                )
                                            } else {
                                                (dialoglayout.findViewById<View>(R.id.friend) as TextView).setText(
                                                    R.string.profile_add_friend
                                                )
                                            }
                                        }
                                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                                }
                            })
                        dialoglayout.findViewById<View>(R.id.block_body)
                            .setOnClickListener(object : View.OnClickListener {
                                override fun onClick(v: View) {
                                    object : AsyncTask<Void?, Void?, Boolean>() {
                                        override fun doInBackground(vararg params: Void?): Boolean {
                                            val map: MutableMap<String?, String?> = mutableMapOf()
                                            map["account_id"] = "t2_" + account!!.id
                                            try {
                                                Authentication.reddit!!.execute(
                                                    Authentication.reddit.request().post(map)
                                                        .path("/api/block_user")
                                                        .build()
                                                )
                                            } catch (ex: Exception) {
                                                return false
                                            }
                                            return true
                                        }

                                        public override fun onPostExecute(blocked: Boolean) {
                                            if (!blocked) {
                                                Toast.makeText(
                                                    baseContext,
                                                    getString(R.string.err_block_user),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    baseContext,
                                                    getString(R.string.success_block_user),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                                }
                            })
                    } else {
                        dialoglayout.findViewById<View>(R.id.pm).visibility = View.GONE
                    }
                    dialoglayout.findViewById<View>(R.id.multi_body).setOnClickListener {
                        val inte = Intent(this@Profile, MultiredditOverview::class.java)
                        inte.putExtra(EXTRA_PROFILE, name)
                        this@Profile.startActivity(inte)
                    }
                    val body = dialoglayout.findViewById<View>(R.id.body2)
                    body.visibility = View.INVISIBLE
                    val center = dialoglayout.findViewById<View>(R.id.colorExpandFrom)
                    dialoglayout.findViewById<View>(R.id.color)
                        .setOnClickListener {
                            val cx = center.width / 2
                            val cy = center.height / 2
                            val finalRadius = Math.max(body.width, body.height)
                            val anim = ViewAnimationUtils.createCircularReveal(
                                body,
                                cx,
                                cy,
                                0f,
                                finalRadius.toFloat()
                            )
                            body.visibility = View.VISIBLE
                            anim.start()
                        }
                    val colorPicker = dialoglayout.findViewById<LineColorPicker>(R.id.picker)
                    val colorPicker2 = dialoglayout.findViewById<LineColorPicker>(R.id.picker2)
                    colorPicker.colors = ColorPreferences.getBaseColors(this@Profile)
                    colorPicker.setOnColorChangedListener(object : OnColorChangedListener {
                        override fun onColorChanged(c: Int) {
                            colorPicker2.colors = ColorPreferences.getColors(baseContext, c)
                            colorPicker2.setSelectedColor(c)
                        }
                    })
                    for (i: Int in colorPicker.colors) {
                        for (i2: Int in ColorPreferences.getColors(baseContext, i)) {
                            if (i2 == currentColor) {
                                colorPicker.setSelectedColor(i)
                                colorPicker2.colors = ColorPreferences.getColors(baseContext, i)
                                colorPicker2.setSelectedColor(i2)
                                break
                            }
                        }
                    }
                    colorPicker2.setOnColorChangedListener(object : OnColorChangedListener {
                        override fun onColorChanged(i: Int) {
                            findViewById<View>(R.id.header).setBackgroundColor(colorPicker2.color)
                            if (mToolbar != null) mToolbar!!.setBackgroundColor(colorPicker2.color)
                            run {
                                val window = window
                                window.statusBarColor = Palette.getDarkerColor(colorPicker2.color)
                            }
                            title.setBackgroundColor(colorPicker2.color)
                        }
                    })
                    run {
                        val dialogButton: TextView = dialoglayout.findViewById(R.id.ok)

                        // if button is clicked, close the custom dialog
                        dialogButton.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(v: View) {
                                Palette.setColorUser(name, colorPicker2.color)
                                val cx: Int = center.width / 2
                                val cy: Int = center.height / 2
                                val initialRadius: Int = body.width
                                val anim: Animator = ViewAnimationUtils.createCircularReveal(
                                    body,
                                    cx,
                                    cy,
                                    initialRadius.toFloat(),
                                    0f
                                )
                                anim.addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        super.onAnimationEnd(animation)
                                        body.visibility = View.GONE
                                    }
                                })
                                anim.start()
                            }
                        })
                    }
                    run {
                        val dialogButton: TextView = dialoglayout.findViewById(R.id.reset)

                        // if button is clicked, close the custom dialog
                        dialogButton.setOnClickListener {
                            Palette.removeUserColor(name)
                            Snackbar.make(
                                dialogButton,
                                "User color removed",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            val cx: Int = center.width / 2
                            val cy: Int = center.height / 2
                            val initialRadius: Int = body.width
                            val anim: Animator = ViewAnimationUtils.createCircularReveal(
                                body,
                                cx,
                                cy,
                                initialRadius.toFloat(),
                                0f
                            )
                            anim.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    super.onAnimationEnd(animation)
                                    body.visibility = View.GONE
                                }
                            })
                            anim.start()
                        }
                    }
                    (dialoglayout.findViewById<View>(R.id.commentkarma) as TextView).text =
                        String.format(
                            Locale.getDefault(), "%d", account!!.commentKarma
                        )
                    (dialoglayout.findViewById<View>(R.id.linkkarma) as TextView).text =
                        String.format(
                            Locale.getDefault(), "%d", account!!.linkKarma
                        )
                    (dialoglayout.findViewById<View>(R.id.totalKarma) as TextView).text =
                        String.format(
                            Locale.getDefault(), "%d", account!!.commentKarma + account!!.linkKarma
                        )
                    AlertDialog.Builder(this@Profile)
                        .setOnDismissListener { dialogInterface: DialogInterface? ->
                            findViewById<View>(R.id.header).setBackgroundColor(currentColor)
                            if (mToolbar != null) mToolbar!!.setBackgroundColor(currentColor)
                            run {
                                val window: Window = window
                                window.statusBarColor = Palette.getDarkerColor(currentColor)
                            }
                        }
                        .setView(dialoglayout)
                        .show()
                }
                return true
            }

            (R.id.sort) -> {
                openPopup()
                return true
            }
        }
        return false
    }

    companion object {
        @JvmField
        val EXTRA_PROFILE = "profile"
        val EXTRA_SAVED = "saved"
        val EXTRA_COMMENT = "comment"
        val EXTRA_SUBMIT = "submitted"
        val EXTRA_UPVOTE = "upvoted"
        val EXTRA_HISTORY = "history"
        private fun isValidUsername(user: String): Boolean {
            /* https://github.com/reddit/reddit/blob/master/r2/r2/lib/validator/validator.py#L261 */
            return user.matches(Regex("^[a-zA-Z0-9_-]{3,20}$"))
        }

        var profSort: Sorting? = null
        var profTime: TimePeriod? = null
    }
}
