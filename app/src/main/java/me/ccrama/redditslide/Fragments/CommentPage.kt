package me.ccrama.redditslide.Fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.afollestad.materialdialogs.MaterialDialog
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.rey.material.widget.Slider
import ltd.ucode.reddit.data.RedditSubmission
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.forceRestart
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.albumSwipe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.SettingValues.fabType
import ltd.ucode.slide.SettingValues.getCommentSorting
import ltd.ucode.slide.SettingValues.setDefaultCommentSorting
import ltd.ucode.slide.activity.MainActivity
import me.ccrama.redditslide.Activities.Album
import me.ccrama.redditslide.Activities.AlbumPager
import me.ccrama.redditslide.Activities.CommentSearch
import me.ccrama.redditslide.Activities.CommentsScreen
import me.ccrama.redditslide.Activities.FullscreenVideo
import me.ccrama.redditslide.Activities.MediaView
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.Related
import me.ccrama.redditslide.Activities.SendMessage
import me.ccrama.redditslide.Activities.ShadowboxComments
import me.ccrama.redditslide.Activities.Submit
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Tumblr
import me.ccrama.redditslide.Activities.TumblrPager
import me.ccrama.redditslide.Activities.Wiki
import me.ccrama.redditslide.Adapters.CommentAdapter
import me.ccrama.redditslide.Adapters.CommentItem
import me.ccrama.redditslide.Adapters.CommentNavType
import me.ccrama.redditslide.Adapters.CommentUrlObject
import me.ccrama.redditslide.Adapters.MoreChildItem
import me.ccrama.redditslide.Adapters.SubmissionComments
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.ContentType
import me.ccrama.redditslide.DataShare
import me.ccrama.redditslide.Drafts
import me.ccrama.redditslide.ImageFlairs
import me.ccrama.redditslide.Notifications.ImageDownloadNotificationService
import me.ccrama.redditslide.OfflineSubreddit.Companion.getSubmissionFromStorage
import me.ccrama.redditslide.PostMatch.openExternal
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openGif
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openImage
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder.Companion.openRedditContent
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserSubscriptions.addSubreddit
import me.ccrama.redditslide.UserSubscriptions.getSubscriptions
import me.ccrama.redditslide.UserSubscriptions.syncMultiReddits
import me.ccrama.redditslide.Views.CommentOverflow
import me.ccrama.redditslide.Views.DoEditorActions
import me.ccrama.redditslide.Views.PreCachingLayoutManagerComments
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.ui.settings.SettingsSubAdapter
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.LinkUtil.openUrl
import me.ccrama.redditslide.util.LinkUtil.tryOpenWithVideoPlugin
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.StringUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import net.dean.jraw.ApiException
import net.dean.jraw.http.MultiRedditUpdateRequest
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.MultiRedditManager
import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Subreddit
import net.dean.jraw.models.UserRecord
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import net.dean.jraw.paginators.UserRecordPaginator
import java.io.IOException
import java.util.Calendar
import java.util.Locale

/**
 * Fragment which displays comment trees.
 *
 * @see CommentsScreen
 */
class CommentPage : Fragment(), Toolbar.OnMenuItemClickListener {
    var np = false
    @JvmField
    var archived = false
    @JvmField
    var locked = false
    @JvmField
    var contest = false
    var loadMore = false
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    var rv: RecyclerView? = null
    private var page = 0
    private var comments: SubmissionComments? = null
    private var single = false
    var adapter: CommentAdapter? = null
    private var fullname: String? = null
    private var context: String? = null
    private var contextNumber = 0
    private var contextThemeWrapper: ContextWrapper? = null
    private var mLayoutManager: PreCachingLayoutManagerComments? = null
    var subreddit: String? = null
    var loaded = false
    @JvmField
    var overrideFab = false
    private var upvoted = false
    private var downvoted = false
    private var currentlySubbed = false
    private var collapsed = SettingValues.collapseCommentsDefault
    fun doResult(data: Intent?) {
        if (data!!.hasExtra("fullname")) {
            val fullname = data.extras!!.getString("fullname")
            adapter!!.currentSelectedItem = fullname
            adapter!!.reset(getContext(), comments, rv, comments!!.submission, true)
            adapter!!.notifyDataSetChanged()
            var i = 2
            for (n in comments!!.comments) {
                if (n is CommentItem && n.comment.comment
                        .fullName
                        .contains(fullname!!)
                ) {
                    (rv!!.layoutManager as PreCachingLayoutManagerComments?)!!.scrollToPositionWithOffset(
                        i, toolbar!!.height
                    )
                    break
                }
                i++
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 423 && resultCode == Activity.RESULT_OK) {
            doResult(data)
        } else if (requestCode == 3333) {
            for (fragment in fragmentManager!!.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    var toolbarScroll: ToolbarScrollHideHandler? = null
    var toolbar: Toolbar? = null
    @JvmField
    var headerHeight = 0
    @JvmField
    var shownHeaders = 0
    fun doTopBar(s: Submission) {
        archived = s.isArchived
        locked = s.isLocked
        contest = s.dataNode["contest_mode"].asBoolean()
        doTopBar()
    }

    fun doTopBarNotify(submission: Submission, adapter2: CommentAdapter?) {
        doTopBar(submission)
        adapter2?.notifyItemChanged(0)
    }

    fun doRefresh(b: Boolean) {
        if (b) {
            v!!.findViewById<View>(R.id.progress).visibility = View.VISIBLE
        } else {
            v!!.findViewById<View>(R.id.progress).visibility = View.GONE
        }
    }

    fun doTopBar() {
        val loadallV = v!!.findViewById<View>(R.id.loadall)
        val npV = v!!.findViewById<View>(R.id.np)
        val archivedV = v!!.findViewById<View>(R.id.archived)
        val lockedV = v!!.findViewById<View>(R.id.locked)
        val headerV = v!!.findViewById<View>(R.id.toolbar)
        val contestV = v!!.findViewById<View>(R.id.contest)
        shownHeaders = 0
        headerV.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        loadallV.visibility = View.VISIBLE
        npV.visibility = View.VISIBLE
        archivedV.visibility = View.VISIBLE
        lockedV.visibility = View.VISIBLE
        contestV.visibility = View.VISIBLE
        if (!loadMore) {
            loadallV.visibility = View.GONE
        } else {
            shownHeaders += getTextViewMeasuredHeight(loadallV as TextView)
            loadallV.setOnClickListener {
                doRefresh(true)
                shownHeaders -= getTextViewMeasuredHeight(loadallV)
                headerHeight = headerV.measuredHeight + shownHeaders
                loadallV.setVisibility(View.GONE)
                if (adapter != null) {
                    adapter!!.notifyItemChanged(0)
                }

                //avoid crashes when load more is clicked before loading is finished
                if (comments!!.mLoadData != null) {
                    comments!!.mLoadData.cancel(true)
                }
                comments = SubmissionComments(fullname, this@CommentPage, mSwipeRefreshLayout)
                comments!!.setSorting(CommentSort.CONFIDENCE)
                loadMore = false
                mSwipeRefreshLayout!!.setProgressViewOffset(
                    false,
                    Constants.SINGLE_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
                    Constants.SINGLE_HEADER_VIEW_OFFSET + (Constants.PTR_OFFSET_BOTTOM
                            + shownHeaders)
                )
            }
        }
        if (!np && !archived) {
            npV.visibility = View.GONE
            archivedV.visibility = View.GONE
        } else if (archived) {
            shownHeaders += getTextViewMeasuredHeight(archivedV as TextView)
            npV.visibility = View.GONE
            archivedV.setBackgroundColor(Palette.getColor(subreddit))
        } else {
            npV.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            shownHeaders += getTextViewMeasuredHeight(npV as TextView)
            archivedV.visibility = View.GONE
            npV.setBackgroundColor(Palette.getColor(subreddit))
        }
        if (locked) {
            lockedV.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            shownHeaders += getTextViewMeasuredHeight(lockedV as TextView)
            lockedV.setBackgroundColor(Palette.getColor(subreddit))
        } else {
            lockedV.visibility = View.GONE
        }
        if (contest) {
            contestV.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            shownHeaders += getTextViewMeasuredHeight(contestV as TextView)
            contestV.setBackgroundColor(Palette.getColor(subreddit))
        } else {
            contestV.visibility = View.GONE
        }
        headerHeight = headerV.measuredHeight + shownHeaders

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp. Account for show headers.
        mSwipeRefreshLayout!!.setProgressViewOffset(
            false,
            Constants.SINGLE_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.SINGLE_HEADER_VIEW_OFFSET + (Constants.PTR_OFFSET_BOTTOM + shownHeaders)
        )
    }

    var v: View? = null
    @JvmField
    var fastScroll: View? = null
    @JvmField
    var fab: FloatingActionButton? = null
    var diff = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        v = localInflater.inflate(R.layout.fragment_verticalcontenttoolbar, container, false)
        rv = v.findViewById(R.id.vertical_content)
        rv.setLayoutManager(mLayoutManager)
        rv.getLayoutManager()!!.scrollToPosition(0)
        toolbar = v.findViewById(R.id.toolbar)
        toolbar.setPopupTheme(ColorPreferences(activity).fontStyle.baseId)
        if (!SettingValues.fabComments || archived || np || locked) {
            v.findViewById<View>(R.id.comment_floating_action_button).visibility = View.GONE
        } else {
            fab = v.findViewById(R.id.comment_floating_action_button)
            if (SettingValues.fastscroll) {
                val fabs = fab.getLayoutParams() as FrameLayout.LayoutParams
                fabs.setMargins(
                    fabs.leftMargin, fabs.topMargin, fabs.rightMargin,
                    fabs.bottomMargin * 3
                )
                fab.setLayoutParams(fabs)
            }
            fab.setOnClickListener(View.OnClickListener {
                val replyDialog = MaterialDialog.Builder(activity!!)
                    .customView(R.layout.edit_comment, false)
                    .cancelable(false)
                    .build()
                val replyView = replyDialog.customView

                // Make the account selector visible
                replyView!!.findViewById<View>(R.id.profile).visibility = View.VISIBLE
                val e = replyView.findViewById<EditText>(R.id.entry)

                //Tint the replyLine appropriately if the base theme is Light or Sepia
                if (SettingValues.currentTheme == 1 || SettingValues.currentTheme == 5) {
                    val TINT = ContextCompat.getColor(getContext()!!, R.color.md_grey_600)
                    e.setHintTextColor(TINT)
                    BlendModeUtil.tintDrawableAsSrcIn(e.background, TINT)
                }
                DoEditorActions.doActions(
                    e,
                    replyView,
                    activity!!.supportFragmentManager,
                    activity,
                    if (adapter!!.submission.isSelfPost) adapter!!.submission.selftext else null,
                    arrayOf(
                        adapter!!.submission.author
                    )
                )
                replyDialog.window
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                replyView.findViewById<View>(R.id.cancel)
                    .setOnClickListener { replyDialog.dismiss() }
                val profile = replyView.findViewById<TextView>(R.id.profile)
                val changedProfile = arrayOf(Authentication.name)
                profile.text = "/u/" + changedProfile[0]
                profile.setOnClickListener {
                    val accounts = HashMap<String?, String>()
                    for (s in authentication.getStringSet(
                        "accounts",
                        HashSet<String>()
                    )!!) {
                        if (s.contains(":")) {
                            accounts[s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[0]] =
                                s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[1]
                        } else {
                            accounts[s] = ""
                        }
                    }
                    val keys = ArrayList(accounts.keys)
                    val i = keys.indexOf(changedProfile[0])
                    val builder = MaterialDialog.Builder(getContext()!!)
                    builder.title(getString(R.string.replies_switch_accounts))
                    builder.items(*keys.toTypedArray())
                    builder.itemsCallbackSingleChoice(i) { dialog, itemView, which, text ->
                        changedProfile[0] = keys[which]
                        profile.text = "/u/" + changedProfile[0]
                        true
                    }
                    builder.alwaysCallSingleChoiceCallback()
                    builder.negativeText(R.string.btn_cancel)
                    builder.show()
                }
                replyView.findViewById<View>(R.id.submit)
                    .setOnClickListener {
                        adapter!!.dataSet.refreshLayout.isRefreshing = true
                        adapter!!.ReplyTaskComment(
                            adapter!!.submission,
                            changedProfile[0]
                        ).execute(
                            e.text.toString()
                        )
                        replyDialog.dismiss()
                    }
                replyDialog.show()
            })
        }
        if (fab != null) fab!!.show()
        resetScroll(false)
        fastScroll = v.findViewById(R.id.commentnav)
        if (!SettingValues.fastscroll) {
            fastScroll.setVisibility(View.GONE)
        } else {
            if (!SettingValues.showCollapseExpand) {
                v.findViewById<View>(R.id.collapse_expand).visibility = View.GONE
            } else {
                v.findViewById<View>(R.id.collapse_expand).visibility = View.VISIBLE
                v.findViewById<View>(R.id.collapse_expand).setOnClickListener {
                    if (adapter != null) {
                        if (collapsed) {
                            adapter!!.expandAll()
                        } else {
                            adapter!!.collapseAll()
                        }
                        collapsed = !collapsed
                    }
                }
            }
            v.findViewById<View>(R.id.down).setOnClickListener {
                if (adapter != null && adapter!!.keys != null && adapter!!.keys.size > 0) {
                    goDown()
                }
            }
            v.findViewById<View>(R.id.up)
                .setOnClickListener { if (adapter != null && adapter!!.keys != null && adapter!!.keys.size > 0) goUp() }
            v.findViewById<View>(R.id.nav).setOnClickListener {
                if (adapter != null && adapter!!.currentComments != null) {
                    var parentCount = 0
                    var opCount = 0
                    var linkCount = 0
                    var awardCount = 0
                    val op = adapter!!.submission.author
                    for (o in adapter!!.currentComments) {
                        if (o.comment != null && o !is MoreChildItem) {
                            if (o.comment.isTopLevel) parentCount++
                            if (o.comment.comment.timesGilded > 0 || o.comment.comment.timesSilvered > 0 || o.comment.comment.timesPlatinized > 0) awardCount++
                            if (o.comment.comment.author != null && o.comment.comment.author == op) {
                                opCount++
                            }
                            if (o.comment.comment.dataNode.has("body_html")
                                && o.comment.comment
                                    .dataNode["body_html"]
                                    .asText()
                                    .contains("&lt;/a")
                            ) {
                                linkCount++
                            }
                        }
                    }
                    AlertDialog.Builder(activity!!)
                        .setTitle(R.string.set_nav_mode)
                        .setSingleChoiceItems(
                            StringUtil.stringToArray(
                                "Parent comment ("
                                        + parentCount
                                        + ")"
                                        + ","
                                        +
                                        "Children comment (highlight child comment & navigate)"
                                        + ","
                                        +
                                        "OP ("
                                        + opCount
                                        + ")"
                                        + ","
                                        + "Time"
                                        + ","
                                        + "Link ("
                                        + linkCount
                                        + ")"
                                        + ","
                                        +
                                        (if (Authentication.isLoggedIn) "You" + "," else "")
                                        +
                                        "Awarded ("
                                        + awardCount
                                        + ")"
                            )
                                .toTypedArray(),
                            getCurrentSort()
                        ) { dialog: DialogInterface?, which: Int ->
                            when (which) {
                                0 -> currentSort = CommentNavType.PARENTS
                                1 -> currentSort = CommentNavType.CHILDREN
                                2 -> currentSort = CommentNavType.OP
                                3 -> {
                                    currentSort = CommentNavType.TIME
                                    val inflater1 = activity!!.layoutInflater
                                    val dialoglayout = inflater1.inflate(R.layout.commenttime, null)
                                    val landscape =
                                        dialoglayout.findViewById<Slider>(R.id.landscape)
                                    val since =
                                        dialoglayout.findViewById<TextView>(R.id.time_string)
                                    landscape.setValueRange(60, 18000, false)
                                    landscape.setOnPositionChangeListener { slider, b, v12, v1, i, i1 ->
                                        val c = Calendar.getInstance()
                                        sortTime = (c.timeInMillis
                                                - i1 * 1000L)
                                        var commentcount = 0
                                        for (o in adapter!!.currentComments) {
                                            if (o.comment != null && o.comment.comment
                                                    .dataNode
                                                    .has("created") && o.comment.comment
                                                    .created
                                                    .time > sortTime
                                            ) {
                                                commentcount += 1
                                            }
                                        }
                                        since.text = (TimeUtils.getTimeAgo(
                                            sortTime, activity
                                        )
                                                + " ("
                                                + commentcount
                                                + " comments)")
                                    }
                                    landscape.setValue(600f, false)
                                    AlertDialog.Builder(activity!!)
                                        .setView(dialoglayout)
                                        .setPositiveButton(R.string.btn_set, null)
                                        .show()
                                }

                                5 -> currentSort =
                                    (if (Authentication.isLoggedIn) CommentNavType.YOU else CommentNavType.GILDED) // gilded is 5 if not logged in
                                4 -> currentSort = CommentNavType.LINK
                                6 -> currentSort = CommentNavType.GILDED
                            }
                        }
                        .show()
                }
            }
        }
        v.findViewById<View>(R.id.up).setOnLongClickListener { //Scroll to top
            rv.getLayoutManager()!!.scrollToPosition(1)
            true
        }
        if (SettingValues.voteGestures) {
            v.findViewById<View>(R.id.up).setOnTouchListener(object : OnFlingGestureListener() {
                override fun onRightToLeft() {}
                override fun onLeftToRight() {}
                override fun onBottomToTop() {
                    adapter!!.submissionViewHolder.upvote.performClick()
                    val context = getContext()
                    val duration = Toast.LENGTH_SHORT
                    val text: CharSequence
                    if (!upvoted) {
                        text = getString(R.string.profile_upvoted)
                        downvoted = false
                    } else {
                        text = getString(R.string.vote_removed)
                    }
                    upvoted = !upvoted
                    val toast = Toast.makeText(context, text, duration)
                    toast.show()
                }

                override fun onTopToBottom() {}
            })
        }
        if (SettingValues.voteGestures) {
            v.findViewById<View>(R.id.down).setOnTouchListener(object : OnFlingGestureListener() {
                override fun onRightToLeft() {}
                override fun onLeftToRight() {}
                override fun onBottomToTop() {
                    adapter!!.submissionViewHolder.downvote.performClick()
                    val context = getContext()
                    val duration = Toast.LENGTH_SHORT
                    val text: CharSequence
                    if (!downvoted) {
                        text = getString(R.string.profile_downvoted)
                        upvoted = false
                    } else {
                        text = getString(R.string.vote_removed)
                    }
                    downvoted = !downvoted
                    val toast = Toast.makeText(context, text, duration)
                    toast.show()
                }

                override fun onTopToBottom() {}
            })
        }
        toolbar.setBackgroundColor(Palette.getColor(subreddit))
        mSwipeRefreshLayout = v.findViewById(R.id.activity_main_swipe_refresh_layout)
        mSwipeRefreshLayout.setColorSchemeColors(*Palette.getColors(subreddit, activity))
        mSwipeRefreshLayout.setOnRefreshListener(OnRefreshListener {
            if (comments != null) {
                comments!!.loadMore(adapter, subreddit, true)
            } else {
                mSwipeRefreshLayout.setRefreshing(false)
            }

            //TODO catch errors
        })
        toolbar.setTitle(subreddit)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener(View.OnClickListener { activity!!.onBackPressed() })
        toolbar.inflateMenu(R.menu.menu_comment_items)
        toolbar.setOnMenuItemClickListener(this)
        toolbar.setOnClickListener(View.OnClickListener {
            (rv.getLayoutManager() as LinearLayoutManager?)!!.scrollToPositionWithOffset(
                1,
                headerHeight
            )
            resetScroll(false)
        })
        addClickFunctionSubName(toolbar)
        doTopBar()
        if (Authentication.didOnline && !NetworkUtil.isConnectedNoOverride(activity)) {
            AlertDialog.Builder(activity!!)
                .setTitle(R.string.err_title)
                .setMessage(R.string.err_connection_failed_msg)
                .setNegativeButton(R.string.btn_close) { dialog: DialogInterface?, which: Int ->
                    if (activity !is MainActivity) {
                        activity!!.finish()
                    }
                }
                .setPositiveButton(R.string.btn_offline) { dialog: DialogInterface?, which: Int ->
                    appRestart.edit().putBoolean("forceoffline", true).commit()
                    forceRestart(activity, false)
                }
                .show()
        }
        doAdapter(
            activity !is CommentsScreen
                    || (activity as CommentsScreen?)!!.currentPage == page
        )
        return v
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                run {
                    if (comments!!.comments != null && comments!!.submission != null) {
                        DataShare.sharedComments = comments!!.comments
                        DataShare.subAuthor = comments!!.submission.author
                        val i = Intent(activity, CommentSearch::class.java)
                        if (activity is MainActivity) {
                            activity!!.startActivityForResult(i, 423)
                        } else {
                            startActivityForResult(i, 423)
                        }
                    }
                }
                return true
            }

            R.id.sidebar -> {
                doSidebarOpen()
                return true
            }

            R.id.related -> {
                if (adapter!!.submission.isSelfPost) {
                    AlertDialog.Builder(activity!!)
                        .setTitle("Selftext posts have no related submissions")
                        .setPositiveButton(R.string.btn_ok, null)
                        .show()
                } else {
                    val i = Intent(activity, Related::class.java)
                    i.putExtra(Related.EXTRA_URL, adapter!!.submission.url)
                    startActivity(i)
                }
                return true
            }

            R.id.shadowbox -> {
                if (SettingValues.isPro) {
                    if (comments!!.comments != null && comments!!.submission != null) {
                        ShadowboxComments.comments = ArrayList()
                        for (c in comments!!.comments) {
                            if (c is CommentItem) {
                                if (c.comment.comment
                                        .dataNode["body_html"]
                                        .asText()
                                        .contains("&lt;/a")
                                ) {
                                    val body = c.comment.comment
                                        .dataNode["body_html"]
                                        .asText()
                                    var url: String
                                    val split = body.split("&lt;a href=\"".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                        .toTypedArray()
                                    if (split.size > 1) {
                                        for (chunk in split) {
                                            url = chunk.substring(
                                                0,
                                                chunk.indexOf("\"", 1)
                                            )
                                            val t = ContentType.getContentType(url)
                                            if (ContentType.mediaType(t)) {
                                                ShadowboxComments.comments.add(
                                                    CommentUrlObject(
                                                        c.comment,
                                                        url, subreddit
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        val start = body.indexOf("&lt;a href=\"")
                                        url = body.substring(
                                            start,
                                            body.indexOf("\"", start + 1)
                                        )
                                        val t = ContentType.getContentType(url)
                                        if (ContentType.mediaType(t)) {
                                            ShadowboxComments.comments.add(
                                                CommentUrlObject(c.comment, url, subreddit)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (!ShadowboxComments.comments.isEmpty()) {
                            val i = Intent(activity, ShadowboxComments::class.java)
                            startActivity(i)
                        } else {
                            Snackbar.make(
                                mSwipeRefreshLayout!!,
                                R.string.shadowbox_comments_nolinks,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    ProUtil.proUpgradeMsg(getContext(), R.string.general_shadowbox_comments_ispro)
                        .setNegativeButton(R.string.btn_no_thanks) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                        .show()
                }
                return true
            }

            R.id.sort -> {
                openPopup(toolbar)
                return true
            }

            R.id.content -> {
                run {
                    if (adapter != null && adapter!!.submission != null) {
                        if (!openExternal(adapter!!.submission.url)) {
                            val type = ContentType.getContentType(
                                adapter!!.submission
                            )
                            when (type) {
                                ContentType.Type.STREAMABLE -> if (SettingValues.video) {
                                    val myIntent = Intent(activity, MediaView::class.java)
                                    myIntent.putExtra(MediaView.SUBREDDIT, subreddit)
                                    myIntent.putExtra(
                                        MediaView.EXTRA_URL,
                                        adapter!!.submission.url
                                    )
                                    myIntent.putExtra(
                                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                        adapter!!.submission.title
                                    )
                                    activity!!.startActivity(myIntent)
                                } else {
                                    openExternally(adapter!!.submission.url)
                                }

                                ContentType.Type.IMGUR, ContentType.Type.XKCD -> {
                                    val i2 = Intent(activity, MediaView::class.java)
                                    i2.putExtra(MediaView.SUBREDDIT, subreddit)
                                    i2.putExtra(
                                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                        adapter!!.submission.title
                                    )
                                    if (adapter!!.submission.dataNode.has("preview")
                                        && adapter!!.submission.dataNode["preview"]["images"][0]["source"]
                                            .has("height")
                                        && (type
                                                != ContentType.Type.XKCD)
                                    ) { //Load the preview image which has probably already been cached in memory instead of the direct link
                                        val previewUrl =
                                            adapter!!.submission.dataNode["preview"]["images"][0]["source"]["url"]
                                                .asText()
                                        i2.putExtra(MediaView.EXTRA_DISPLAY_URL, previewUrl)
                                    }
                                    i2.putExtra(
                                        MediaView.EXTRA_URL,
                                        adapter!!.submission.url
                                    )
                                    activity!!.startActivity(i2)
                                }

                                ContentType.Type.EMBEDDED -> if (SettingValues.video) {
                                    val data =
                                        adapter!!.submission.dataNode["media_embed"]["content"]
                                            .asText()
                                    run {
                                        val i = Intent(
                                            activity,
                                            FullscreenVideo::class.java
                                        )
                                        i.putExtra(FullscreenVideo.EXTRA_HTML, data)
                                        activity!!.startActivity(i)
                                    }
                                } else {
                                    openExternally(adapter!!.submission.url)
                                }

                                ContentType.Type.REDDIT -> openRedditContent(
                                    adapter!!.submission.url, activity
                                )

                                ContentType.Type.LINK -> openUrl(
                                    adapter!!.submission.url,
                                    Palette.getColor(
                                        adapter!!.submission.subredditName
                                    ),
                                    activity!!
                                )

                                ContentType.Type.NONE, ContentType.Type.SELF -> if (adapter!!.submission.selftext.isEmpty()) {
                                    val s = Snackbar.make(
                                        rv!!, R.string.submission_nocontent,
                                        Snackbar.LENGTH_SHORT
                                    )
                                    LayoutUtils.showSnackbar(s)
                                } else {
                                    val inflater = activity!!.layoutInflater
                                    val dialoglayout = inflater.inflate(
                                        R.layout.parent_comment_dialog,
                                        null
                                    )
                                    adapter!!.setViews(
                                        adapter!!.submission.dataNode["selftext_html"]
                                            .asText(),
                                        adapter!!.submission.subredditName,
                                        dialoglayout.findViewById(
                                            R.id.firstTextView
                                        ),
                                        dialoglayout.findViewById(
                                            R.id.commentOverflow
                                        )
                                    )
                                    AlertDialog.Builder(activity!!)
                                        .setView(dialoglayout)
                                        .show()
                                }

                                ContentType.Type.ALBUM -> if (SettingValues.album) {
                                    val i: Intent
                                    if (albumSwipe) {
                                        i = Intent(activity, AlbumPager::class.java)
                                        i.putExtra(
                                            Album.EXTRA_URL,
                                            adapter!!.submission.url
                                        )
                                        i.putExtra(AlbumPager.SUBREDDIT, subreddit)
                                    } else {
                                        i = Intent(activity, Album::class.java)
                                        i.putExtra(
                                            Album.EXTRA_URL,
                                            adapter!!.submission.url
                                        )
                                        i.putExtra(Album.SUBREDDIT, subreddit)
                                    }
                                    i.putExtra(
                                        ImageDownloadNotificationService.EXTRA_SUBMISSION_TITLE,
                                        adapter!!.submission.title
                                    )
                                    activity!!.startActivity(i)
                                    activity!!.overridePendingTransition(
                                        R.anim.slideright, R.anim.fade_out
                                    )
                                } else {
                                    openExternally(adapter!!.submission.url)
                                }

                                ContentType.Type.TUMBLR -> if (SettingValues.image) {
                                    val i: Intent
                                    if (albumSwipe) {
                                        i = Intent(
                                            activity,
                                            TumblrPager::class.java
                                        )
                                        i.putExtra(
                                            Album.EXTRA_URL,
                                            adapter!!.submission.url
                                        )
                                        i.putExtra(TumblrPager.SUBREDDIT, subreddit)
                                    } else {
                                        i = Intent(activity, Tumblr::class.java)
                                        i.putExtra(Tumblr.SUBREDDIT, subreddit)
                                        i.putExtra(
                                            Album.EXTRA_URL,
                                            adapter!!.submission.url
                                        )
                                    }
                                    activity!!.startActivity(i)
                                    activity!!.overridePendingTransition(
                                        R.anim.slideright, R.anim.fade_out
                                    )
                                } else {
                                    openExternally(adapter!!.submission.url)
                                }

                                ContentType.Type.IMAGE -> openImage(
                                    type, activity!!,
                                    RedditSubmission(adapter!!.submission), null, -1
                                )

                                ContentType.Type.VREDDIT_REDIRECT, ContentType.Type.VREDDIT_DIRECT, ContentType.Type.GIF -> openGif(
                                    activity!!,
                                    RedditSubmission(adapter!!.submission), -1
                                )

                                ContentType.Type.VIDEO -> if (!tryOpenWithVideoPlugin(
                                        adapter!!.submission.url
                                    )
                                ) {
                                    openUrl(
                                        adapter!!.submission.url,
                                        Palette.getStatusBarColor(), activity!!
                                    )
                                }
                            }
                        } else {
                            openExternally(adapter!!.submission.url)
                        }
                    }
                }
                return true
            }

            R.id.reload -> {
                if (comments != null) {
                    mSwipeRefreshLayout!!.isRefreshing = true
                    comments!!.loadMore(adapter, subreddit)
                }
                return true
            }

            R.id.collapse -> {
                run {
                    if (adapter != null) {
                        adapter!!.collapseAll()
                    }
                }
                return true
            }

            android.R.id.home -> {
                activity!!.onBackPressed()
                return true
            }
        }
        return false
    }

    private fun doSidebarOpen() {
        AsyncGetSubreddit().execute(subreddit)
    }

    private inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        override fun onPostExecute(baseSub: Subreddit) {
            try {
                d!!.dismiss()
            } catch (e: Exception) {
            }
            if (baseSub != null) {
                currentlySubbed = Authentication.isLoggedIn && baseSub.isUserSubscriber
                subreddit = baseSub.displayName
                try {
                    val sidebar = activity!!.layoutInflater.inflate(R.layout.subinfo, null)
                    run {
                        sidebar.findViewById<View>(R.id.loader).visibility = View.GONE
                        sidebar.findViewById<View>(R.id.sidebar_text).visibility = View.GONE
                        sidebar.findViewById<View>(R.id.sub_title).visibility = View.GONE
                        sidebar.findViewById<View>(R.id.subscribers).visibility = View.GONE
                        sidebar.findViewById<View>(R.id.active_users).visibility = View.GONE
                        sidebar.findViewById<View>(R.id.header_sub)
                            .setBackgroundColor(Palette.getColor(subreddit))
                        (sidebar.findViewById<View>(R.id.sub_infotitle) as TextView).text =
                            subreddit

                        //Sidebar buttons should use subreddit's accent color
                        val subColor = ColorPreferences(getContext()).getColor(subreddit)
                        (sidebar.findViewById<View>(R.id.theme_text) as TextView).setTextColor(
                            subColor
                        )
                        (sidebar.findViewById<View>(R.id.wiki_text) as TextView).setTextColor(
                            subColor
                        )
                        (sidebar.findViewById<View>(R.id.post_text) as TextView).setTextColor(
                            subColor
                        )
                        (sidebar.findViewById<View>(R.id.mods_text) as TextView).setTextColor(
                            subColor
                        )
                        (sidebar.findViewById<View>(R.id.flair_text) as TextView).setTextColor(
                            subColor
                        )
                    }
                    run {
                        sidebar.findViewById<View>(R.id.loader).visibility = View.VISIBLE
                        loaded = true
                        run {
                            val submit = sidebar.findViewById<View>(R.id.submit)
                            if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                                submit.visibility = View.GONE
                            }
                            if (SettingValues.fab && fabType == Constants.FAB_POST) {
                                submit.visibility = View.GONE
                            }
                            submit.setOnClickListener(object : OnSingleClickListener() {
                                override fun onSingleClick(view: View) {
                                    val inte = Intent(activity, Submit::class.java)
                                    inte.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                                    activity!!.startActivity(inte)
                                }
                            })
                        }
                        sidebar.findViewById<View>(R.id.wiki)
                            .setOnClickListener {
                                val i = Intent(activity, Wiki::class.java)
                                i.putExtra(Wiki.EXTRA_SUBREDDIT, subreddit)
                                startActivity(i)
                            }
                        sidebar.findViewById<View>(R.id.submit)
                            .setOnClickListener {
                                val i = Intent(activity, Submit::class.java)
                                i.putExtra(Submit.EXTRA_SUBREDDIT, subreddit)
                                startActivity(i)
                            }
                        sidebar.findViewById<View>(R.id.syncflair)
                            .setOnClickListener { ImageFlairs.syncFlairs(getContext(), subreddit) }
                        sidebar.findViewById<View>(R.id.theme)
                            .setOnClickListener {
                                val style = ColorPreferences(
                                    activity
                                ).getThemeSubreddit(subreddit)
                                val contextThemeWrapper: Context = ContextThemeWrapper(
                                    activity, style
                                )
                                val localInflater = activity!!.layoutInflater
                                    .cloneInContext(contextThemeWrapper)
                                val dialoglayout = localInflater.inflate(R.layout.colorsub, null)
                                val arrayList = ArrayList<String?>()
                                arrayList.add(subreddit)
                                SettingsSubAdapter.showSubThemeEditor(
                                    arrayList,
                                    activity, dialoglayout
                                )
                            }
                        sidebar.findViewById<View>(R.id.mods)
                            .setOnClickListener {
                                val d: Dialog = MaterialDialog.Builder(activity!!).title(
                                    R.string.sidebar_findingmods
                                )
                                    .cancelable(true)
                                    .content(R.string.misc_please_wait)
                                    .progress(true, 100)
                                    .show()
                                object : AsyncTask<Void?, Void?, Void?>() {
                                    var mods: ArrayList<UserRecord>? = null
                                    protected override fun doInBackground(vararg params: Void): Void {
                                        mods = ArrayList()
                                        val paginator = UserRecordPaginator(
                                            Authentication.reddit, subreddit,
                                            "moderators"
                                        )
                                        paginator.sorting = Sorting.HOT
                                        paginator.timePeriod = TimePeriod.ALL
                                        while (paginator.hasNext()) {
                                            mods!!.addAll(paginator.next())
                                        }
                                        return null
                                    }

                                    protected override fun onPostExecute(aVoid: Void) {
                                        val names = ArrayList<String?>()
                                        for (rec in mods!!) {
                                            names.add(rec.fullName)
                                        }
                                        d.dismiss()
                                        MaterialDialog.Builder(activity!!).title(
                                            getString(
                                                R.string.sidebar_submods,
                                                subreddit
                                            )
                                        )
                                            .items(names)
                                            .itemsCallback { dialog, itemView, which, text ->
                                                val i = Intent(
                                                    activity,
                                                    Profile::class.java
                                                )
                                                i.putExtra(
                                                    Profile.EXTRA_PROFILE,
                                                    names[which]
                                                )
                                                startActivity(i)
                                            }
                                            .positiveText(R.string.btn_message)
                                            .onPositive { dialog, which ->
                                                val i = Intent(
                                                    activity,
                                                    SendMessage::class.java
                                                )
                                                i.putExtra(
                                                    SendMessage.EXTRA_NAME,
                                                    "/r/$subreddit"
                                                )
                                                startActivity(i)
                                            }
                                            .show()
                                    }
                                }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                            }
                        sidebar.findViewById<View>(R.id.flair).visibility = View.GONE
                    }
                    run {
                        sidebar.findViewById<View>(R.id.loader).visibility = View.GONE
                        if (baseSub.sidebar != null && !baseSub.sidebar.isEmpty()) {
                            sidebar.findViewById<View>(R.id.sidebar_text).visibility = View.VISIBLE
                            val text = baseSub.dataNode["description_html"].asText()
                            val body =
                                sidebar.findViewById<SpoilerRobotoTextView>(R.id.sidebar_text)
                            val overflow =
                                sidebar.findViewById<CommentOverflow>(R.id.commentOverflow)
                            setViews(text, baseSub.displayName, body, overflow)
                        } else {
                            sidebar.findViewById<View>(R.id.sidebar_text).visibility = View.GONE
                        }
                        val collection = sidebar.findViewById<View>(R.id.collection)
                        if (Authentication.isLoggedIn) {
                            collection.setOnClickListener {
                                object : AsyncTask<Void?, Void?, Void?>() {
                                    var multis = HashMap<String?, MultiReddit?>()
                                    protected override fun doInBackground(vararg params: Void): Void {
                                        if (UserSubscriptions.multireddits == null) {
                                            syncMultiReddits(getContext())
                                        }
                                        for (r in UserSubscriptions.multireddits!!) {
                                            multis[r!!.displayName] = r
                                        }
                                        return null
                                    }

                                    protected override fun onPostExecute(aVoid: Void) {
                                        MaterialDialog.Builder(getContext()!!).title(
                                            "Add /r/" + baseSub.displayName + " to"
                                        )
                                            .items(multis.keys)
                                            .itemsCallback { dialog, itemView, which, text ->
                                                object : AsyncTask<Void?, Void?, Void?>() {
                                                    protected override fun doInBackground(
                                                        vararg params: Void
                                                    ): Void {
                                                        try {
                                                            val multiName = multis.keys
                                                                .toTypedArray()[which]
                                                            val subs: MutableList<String> =
                                                                ArrayList()
                                                            for (sub in multis[multiName]
                                                                .getSubreddits()) {
                                                                subs.add(
                                                                    sub.displayName
                                                                )
                                                            }
                                                            subs.add(
                                                                baseSub.displayName
                                                            )
                                                            MultiRedditManager(
                                                                Authentication.reddit
                                                            )
                                                                .createOrUpdate(
                                                                    MultiRedditUpdateRequest.Builder(
                                                                        Authentication.name,
                                                                        multiName
                                                                    )
                                                                        .subreddits(
                                                                            subs
                                                                        )
                                                                        .build()
                                                                )
                                                            syncMultiReddits(
                                                                getContext()
                                                            )
                                                            activity!!.runOnUiThread {
                                                                Snackbar.make(
                                                                    toolbar!!,
                                                                    getString(
                                                                        R.string.multi_subreddit_added,
                                                                        multiName
                                                                    ),
                                                                    Snackbar.LENGTH_LONG
                                                                )
                                                                    .show()
                                                            }
                                                        } catch (e: NetworkException) {
                                                            activity!!.runOnUiThread {
                                                                activity
                                                                    .runOnUiThread(
                                                                        Runnable {
                                                                            Snackbar.make(
                                                                                toolbar!!,
                                                                                getString(
                                                                                    R.string.multi_error
                                                                                ),
                                                                                Snackbar.LENGTH_LONG
                                                                            )
                                                                                .setAction(
                                                                                    R.string.btn_ok,
                                                                                    null
                                                                                )
                                                                                .show()
                                                                        })
                                                            }
                                                            e.printStackTrace()
                                                        } catch (e: ApiException) {
                                                            activity!!.runOnUiThread {
                                                                activity
                                                                    .runOnUiThread(
                                                                        Runnable {
                                                                            Snackbar.make(
                                                                                toolbar!!,
                                                                                getString(
                                                                                    R.string.multi_error
                                                                                ),
                                                                                Snackbar.LENGTH_LONG
                                                                            )
                                                                                .setAction(
                                                                                    R.string.btn_ok,
                                                                                    null
                                                                                )
                                                                                .show()
                                                                        })
                                                            }
                                                            e.printStackTrace()
                                                        }
                                                        return null
                                                    }
                                                }.executeOnExecutor(
                                                    THREAD_POOL_EXECUTOR
                                                )
                                            }
                                            .show()
                                    }
                                }.executeOnExecutor(THREAD_POOL_EXECUTOR)
                            }
                        } else {
                            collection.visibility = View.GONE
                        }
                        run {
                            val subscribe = sidebar.findViewById<TextView>(R.id.subscribe)
                            currentlySubbed =
                                if (Authentication.isLoggedIn) baseSub.isUserSubscriber else getSubscriptions(
                                    activity
                                )
                                    .contains(baseSub.displayName.lowercase())
                            MiscUtil.doSubscribeButtonText(currentlySubbed, subscribe)
                            subscribe.setOnClickListener(object : View.OnClickListener {
                                private fun doSubscribe() {
                                    if (Authentication.isLoggedIn) {
                                        AlertDialog.Builder(activity!!)
                                            .setTitle(
                                                getString(
                                                    R.string.subscribe_to,
                                                    baseSub.displayName
                                                )
                                            )
                                            .setPositiveButton(R.string.reorder_add_subscribe) { dialog: DialogInterface?, which: Int ->
                                                object : AsyncTask<Void?, Void?, Boolean?>() {
                                                    override fun onPostExecute(success: Boolean) {
                                                        if (!success) { // If subreddit was removed from account or not
                                                            AlertDialog.Builder(activity!!)
                                                                .setTitle(R.string.force_change_subscription)
                                                                .setMessage(R.string.force_change_subscription_desc)
                                                                .setPositiveButton(R.string.btn_yes) { dialog1: DialogInterface?, which1: Int ->
                                                                    changeSubscription(
                                                                        baseSub,
                                                                        true
                                                                    ) // Force add the subscription
                                                                    val s = Snackbar.make(
                                                                        toolbar!!,
                                                                        getString(R.string.misc_subscribed),
                                                                        Snackbar.LENGTH_SHORT
                                                                    )
                                                                    LayoutUtils.showSnackbar(s)
                                                                }
                                                                .setNegativeButton(
                                                                    R.string.btn_no,
                                                                    null
                                                                )
                                                                .setCancelable(false)
                                                                .show()
                                                        } else {
                                                            changeSubscription(baseSub, true)
                                                        }
                                                    }

                                                    protected override fun doInBackground(
                                                        vararg params: Void
                                                    ): Boolean {
                                                        try {
                                                            AccountManager(
                                                                Authentication.reddit
                                                            )
                                                                .subscribe(
                                                                    baseSub
                                                                )
                                                        } catch (e: NetworkException) {
                                                            return@setPositiveButton false // Either network crashed or trying to unsubscribe to a subreddit that the account isn't subscribed to
                                                        }
                                                        return@setPositiveButton true
                                                    }
                                                }.executeOnExecutor(
                                                    THREAD_POOL_EXECUTOR
                                                )
                                            }
                                            .setNegativeButton(R.string.btn_cancel, null)
                                            .setNeutralButton(R.string.btn_add_to_sublist) { dialog: DialogInterface?, which: Int ->
                                                changeSubscription(
                                                    baseSub,
                                                    true
                                                ) // Force add the subscription
                                                val s = Snackbar.make(
                                                    toolbar!!,
                                                    R.string.sub_added,
                                                    Snackbar.LENGTH_SHORT
                                                )
                                                LayoutUtils.showSnackbar(s)
                                            }
                                            .show()
                                    } else {
                                        changeSubscription(baseSub, true)
                                    }
                                }

                                override fun onClick(v: View) {
                                    if (!currentlySubbed) {
                                        doSubscribe()
                                    } else {
                                        doUnsubscribe()
                                    }
                                    MiscUtil.doSubscribeButtonText(currentlySubbed, subscribe)
                                }

                                private fun doUnsubscribe() {
                                    if (Authentication.didOnline) {
                                        AlertDialog.Builder(getContext()!!)
                                            .setTitle(
                                                getString(
                                                    R.string.unsubscribe_from,
                                                    baseSub.displayName
                                                )
                                            )
                                            .setPositiveButton(R.string.reorder_remove_unsubscribe) { dialog: DialogInterface?, which: Int ->
                                                object : AsyncTask<Void?, Void?, Boolean?>() {
                                                    override fun onPostExecute(success: Boolean) {
                                                        if (!success) { // If subreddit was removed from account or not
                                                            AlertDialog.Builder(getContext()!!)
                                                                .setTitle(R.string.force_change_subscription)
                                                                .setMessage(R.string.force_change_subscription_desc)
                                                                .setPositiveButton(R.string.btn_yes) { dialog12: DialogInterface?, which12: Int ->
                                                                    changeSubscription(
                                                                        baseSub,
                                                                        false
                                                                    ) // Force add the subscription
                                                                    val s = Snackbar.make(
                                                                        toolbar!!,
                                                                        getString(R.string.misc_unsubscribed),
                                                                        Snackbar.LENGTH_SHORT
                                                                    )
                                                                    LayoutUtils.showSnackbar(s)
                                                                }
                                                                .setNegativeButton(
                                                                    R.string.btn_no,
                                                                    null
                                                                )
                                                                .setCancelable(false)
                                                                .show()
                                                        } else {
                                                            changeSubscription(baseSub, false)
                                                        }
                                                    }

                                                    protected override fun doInBackground(
                                                        vararg params: Void
                                                    ): Boolean {
                                                        try {
                                                            AccountManager(
                                                                Authentication.reddit
                                                            )
                                                                .unsubscribe(
                                                                    baseSub
                                                                )
                                                        } catch (e: NetworkException) {
                                                            return@setPositiveButton false // Either network crashed or trying to unsubscribe to a subreddit that the account isn't subscribed to
                                                        }
                                                        return@setPositiveButton true
                                                    }
                                                }.executeOnExecutor(
                                                    THREAD_POOL_EXECUTOR
                                                )
                                            }
                                            .setNeutralButton(R.string.just_unsub) { dialog: DialogInterface?, which: Int ->
                                                changeSubscription(
                                                    baseSub,
                                                    false
                                                ) // Force add the subscription
                                                val s = Snackbar.make(
                                                    toolbar!!,
                                                    R.string.misc_unsubscribed,
                                                    Snackbar.LENGTH_SHORT
                                                )
                                                LayoutUtils.showSnackbar(s)
                                            }
                                            .setNegativeButton(R.string.btn_cancel, null)
                                            .show()
                                    } else {
                                        changeSubscription(baseSub, false)
                                    }
                                }
                            })
                        }
                        if (!baseSub.publicDescription.isEmpty()) {
                            sidebar.findViewById<View>(R.id.sub_title).visibility = View.VISIBLE
                            setViews(
                                baseSub.dataNode["public_description_html"].asText(),
                                baseSub.displayName.lowercase(),
                                sidebar.findViewById(R.id.sub_title),
                                sidebar.findViewById(
                                    R.id.sub_title_overflow
                                )
                            )
                        } else {
                            sidebar.findViewById<View>(R.id.sub_title).visibility = View.GONE
                        }
                        if (baseSub.dataNode.has("icon_img") && !baseSub.dataNode["icon_img"]
                                .asText()
                                .isEmpty()
                        ) {
                            (getContext()!!.applicationContext as App).imageLoader
                                .displayImage(
                                    baseSub.dataNode["icon_img"].asText(),
                                    sidebar.findViewById<View>(R.id.subimage) as ImageView
                                )
                        } else {
                            sidebar.findViewById<View>(R.id.subimage).visibility = View.GONE
                        }
                        val bannerImage = baseSub.bannerImage
                        if (bannerImage != null && !bannerImage.isEmpty()) {
                            sidebar.findViewById<View>(R.id.sub_banner).visibility = View.VISIBLE
                            (getContext()!!.applicationContext as App).imageLoader
                                .displayImage(
                                    bannerImage,
                                    sidebar.findViewById<View>(R.id.sub_banner) as ImageView
                                )
                        } else {
                            sidebar.findViewById<View>(R.id.sub_banner).visibility = View.GONE
                        }
                        (sidebar.findViewById<View>(R.id.subscribers) as TextView).text = getString(
                            R.string.subreddit_subscribers_string,
                            baseSub.localizedSubscriberCount
                        )
                        sidebar.findViewById<View>(R.id.subscribers).visibility = View.VISIBLE
                        (sidebar.findViewById<View>(R.id.active_users) as TextView).text =
                            getString(
                                R.string.subreddit_active_users_string_new,
                                baseSub.localizedAccountsActive
                            )
                        sidebar.findViewById<View>(R.id.active_users).visibility = View.VISIBLE
                    }
                    AlertDialog.Builder(getContext()!!)
                        .setPositiveButton(R.string.btn_close, null)
                        .setView(sidebar)
                        .show()
                } catch (e: NullPointerException) { //activity has been killed
                }
            }
        }

        protected override fun doInBackground(vararg params: String): Subreddit {
            return try {
                Authentication.reddit!!.getSubreddit(params[0])
            } catch (e: Exception) {
                try {
                    d!!.dismiss()
                } catch (ignored: Exception) {
                }
                null
            }
        }

        var d: Dialog? = null
        override fun onPreExecute() {
            d = MaterialDialog.Builder(activity!!).title(R.string.subreddit_sidebar_progress)
                .progress(true, 100)
                .content(R.string.misc_please_wait)
                .cancelable(false)
                .show()
        }
    }

    var commentSorting: CommentSort? = null
    private fun addClickFunctionSubName(toolbar: Toolbar?) {
        var titleTv: TextView? = null
        for (i in 0 until toolbar!!.childCount) {
            val view = toolbar.getChildAt(i)
            var text: CharSequence? = null
            if (view is TextView && view.text.also { text = it } != null) {
                titleTv = view
            }
        }
        if (titleTv != null) {
            val text = titleTv.text.toString()
            titleTv.setOnClickListener(View.OnClickListener {
                val i = Intent(activity, SubredditView::class.java)
                i.putExtra(SubredditView.EXTRA_SUBREDDIT, text)
                startActivity(i)
            })
        }
    }

    fun doAdapter(load: Boolean) {
        commentSorting = getCommentSorting(subreddit!!)
        if (load) doRefresh(true)
        if (load) loaded = true
        if ((!single
                    && activity is CommentsScreen) && (activity as CommentsScreen?)!!.subredditPosts != null && Authentication.didOnline && (activity as CommentsScreen?)!!.currentPosts != null && (activity as CommentsScreen?)!!.currentPosts!!.size > page
        ) {
            comments = try {
                SubmissionComments(fullname, this, mSwipeRefreshLayout)
            } catch (e: IndexOutOfBoundsException) {
                return
            }
            val s = (activity as CommentsScreen?)!!.currentPosts!![page]!!.submission
            if (s != null && s.dataNode.has("suggested_sort") && !s.dataNode["suggested_sort"]
                    .asText()
                    .equals("null", ignoreCase = true)
            ) {
                var sorting = s.dataNode["suggested_sort"].asText().uppercase(Locale.getDefault())
                sorting = sorting.replace("İ", "I")
                commentSorting = CommentSort.valueOf(sorting)
            } else if (s != null) {
                commentSorting = getCommentSorting(s.subredditName)
            }
            if (load) comments!!.setSorting(commentSorting)
            if (adapter == null) {
                adapter = CommentAdapter(this, comments, rv, s, fragmentManager)
                rv!!.adapter = adapter
            }
        } else if (activity is MainActivity) {
            if (Authentication.didOnline) {
                comments = SubmissionComments(fullname, this, mSwipeRefreshLayout)
                val s = (activity as MainActivity?)!!.openingComments!!.submission
                if (s != null && s.dataNode.has("suggested_sort") && !s.dataNode["suggested_sort"]
                        .asText()
                        .equals("null", ignoreCase = true)
                ) {
                    var sorting =
                        s.dataNode["suggested_sort"].asText().uppercase(Locale.getDefault())
                    sorting = sorting.replace("İ", "I")
                    commentSorting = CommentSort.valueOf(sorting)
                } else if (s != null) {
                    commentSorting = getCommentSorting(s.subredditName)
                }
                if (load) comments!!.setSorting(commentSorting)
                if (adapter == null) {
                    adapter = CommentAdapter(this, comments, rv, s, fragmentManager)
                    rv!!.adapter = adapter
                }
            } else {
                val s = (activity as MainActivity?)!!.openingComments!!.submission
                doRefresh(false)
                comments = SubmissionComments(fullname, this, mSwipeRefreshLayout, s)
                if (adapter == null) {
                    adapter = CommentAdapter(this, comments, rv, s, fragmentManager)
                    rv!!.adapter = adapter
                }
            }
        } else {
            var s: Submission? = null
            try {
                s = getSubmissionFromStorage(
                    (if (fullname!!.contains("_")) fullname else "t3_$fullname")!!, getContext(),
                    !NetworkUtil.isConnected(activity), ObjectMapper().reader()
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (s != null && s.comments != null) {
                doRefresh(false)
                comments = SubmissionComments(fullname, this, mSwipeRefreshLayout, s)
                if (adapter == null) {
                    adapter = CommentAdapter(this, comments, rv, s, fragmentManager)
                    rv!!.adapter = adapter
                }
            } else if (context!!.isEmpty()) {
                comments = SubmissionComments(fullname, this, mSwipeRefreshLayout)
                comments!!.setSorting(commentSorting)
                if (adapter == null) {
                    if (s != null) {
                        adapter = CommentAdapter(this, comments, rv, s, fragmentManager)
                    }
                    rv!!.adapter = adapter
                }
            } else {
                comments = if (context == App.EMPTY_STRING) {
                    SubmissionComments(fullname, this, mSwipeRefreshLayout)
                } else {
                    SubmissionComments(
                        fullname, this, mSwipeRefreshLayout, context,
                        contextNumber
                    )
                }
                if (load) comments!!.setSorting(commentSorting)
            }
        }
    }

    fun doData(b: Boolean?) {
        if (adapter == null || single) {
            adapter = CommentAdapter(
                this, comments, rv, comments!!.submission,
                fragmentManager
            )
            rv!!.adapter = adapter
            adapter!!.currentSelectedItem = context
            if (context!!.isEmpty()) {
                if (SettingValues.collapseCommentsDefault) {
                    adapter!!.collapseAll()
                }
            }
            adapter!!.reset(getContext(), comments, rv, comments!!.submission, b!!)
        } else if (!b!!) {
            try {
                adapter!!.reset(
                    getContext(),
                    comments,
                    rv,
                    if (activity is MainActivity) (activity as MainActivity?)!!.openingComments!!.submission else comments!!.submission,
                    b
                )
                if (SettingValues.collapseCommentsDefault) {
                    adapter!!.collapseAll()
                }
            } catch (ignored: Exception) {
            }
        } else {
            adapter!!.reset(getContext(), comments, rv, comments!!.submission, b)
            if (SettingValues.collapseCommentsDefault) {
                adapter!!.collapseAll()
            }
            adapter!!.notifyItemChanged(1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        subreddit = bundle!!.getString("subreddit", "")
        fullname = bundle.getString("id", "")
        page = bundle.getInt("page", 0)
        single = bundle.getBoolean("single", false)
        context = bundle.getString("context", "")
        contextNumber = bundle.getInt("contextNumber", 5)
        np = bundle.getBoolean("np", false)
        archived = bundle.getBoolean("archived", false)
        locked = bundle.getBoolean("locked", false)
        contest = bundle.getBoolean("contest", false)
        (loadMore = !context.isEmpty()) && context != App.EMPTY_STRING
        if (!single) loadMore = false
        val subredditStyle = ColorPreferences(activity).getThemeSubreddit(subreddit)
        contextThemeWrapper = ContextThemeWrapper(activity, subredditStyle)
        mLayoutManager = PreCachingLayoutManagerComments(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (comments != null) comments!!.cancelLoad()
        if (adapter != null && adapter!!.currentComments != null) {
            if (adapter!!.currentlyEditing != null && !adapter!!.currentlyEditing.text
                    .toString()
                    .isEmpty()
            ) {
                Drafts.addDraft(adapter!!.currentlyEditing.text.toString())
                Toast.makeText(
                    activity!!.applicationContext, R.string.msg_save_draft,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun getCurrentSort(): Int {
        return when (currentSort) {
            CommentNavType.PARENTS -> 0
            CommentNavType.CHILDREN -> 1
            CommentNavType.OP -> 2
            CommentNavType.TIME -> 3
            CommentNavType.GILDED -> 6
            CommentNavType.YOU -> 5
            CommentNavType.LINK -> 4
        }
        return 0
    }

    fun resetScroll(override: Boolean) {
        if (toolbarScroll == null) {
            toolbarScroll = object : ToolbarScrollHideHandler(
                toolbar, v!!.findViewById(R.id.header),
                v!!.findViewById(R.id.progress),
                if (SettingValues.commentAutoHide) v!!.findViewById<View>(R.id.commentnav) else null
            ) {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (SettingValues.fabComments) {
                        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING
                            && !overrideFab
                        ) {
                            diff += dy
                        } else if (!overrideFab) {
                            diff = 0
                        }
                        if (fab != null && !overrideFab) {
                            if (dy <= 0 && fab!!.id != 0) {
                                if (recyclerView.scrollState
                                    != RecyclerView.SCROLL_STATE_DRAGGING
                                    || diff < -fab!!.height * 2
                                ) {
                                    fab!!.show()
                                }
                            } else {
                                fab!!.hide()
                            }
                        }
                    }
                }
            }
            rv!!.addOnScrollListener(toolbarScroll)
        } else {
            toolbarScroll!!.reset = true
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //This is the filter
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            goDown()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            goUp()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            return onMenuItemClick(toolbar!!.menu.findItem(R.id.search))
        }
        return false
    }

    private fun reloadSubs() {
        mSwipeRefreshLayout!!.isRefreshing = true
        comments!!.setSorting(commentSorting)
        rv!!.scrollToPosition(0)
    }

    private fun openPopup(view: View?) {
        if (comments!!.comments != null && !comments!!.comments.isEmpty()) {
            val l2 = DialogInterface.OnClickListener { dialogInterface, i ->
                when (i) {
                    0 -> commentSorting = CommentSort.CONFIDENCE
                    1 -> commentSorting = CommentSort.TOP
                    2 -> commentSorting = CommentSort.NEW
                    3 -> commentSorting = CommentSort.CONTROVERSIAL
                    4 -> commentSorting = CommentSort.OLD
                    5 -> commentSorting = CommentSort.QA
                }
            }
            val i =
                if (commentSorting == CommentSort.CONFIDENCE) 0 else if (commentSorting == CommentSort.TOP) 1 else if (commentSorting == CommentSort.NEW) 2 else if (commentSorting == CommentSort.CONTROVERSIAL) 3 else if (commentSorting == CommentSort.OLD) 4 else if (commentSorting == CommentSort.QA) 5 else 0
            val res = requireActivity().baseContext.resources
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.sorting_choose)
                .setSingleChoiceItems(
                    arrayOf(
                        res.getString(R.string.sorting_best),
                        res.getString(R.string.sorting_top),
                        res.getString(R.string.sorting_new),
                        res.getString(R.string.sorting_controversial),
                        res.getString(R.string.sorting_old),
                        res.getString(R.string.sorting_ama)
                    ), i, l2
                )
                .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> reloadSubs() }
                .setNeutralButton(
                    getString(
                        R.string.sorting_defaultfor,
                        subreddit
                    )
                ) { dialog: DialogInterface?, which: Int ->
                    setDefaultCommentSorting(
                        commentSorting!!, subreddit!!
                    )
                    reloadSubs()
                }
                .show()
        }
    }

    fun doGoUp(old: Int) {
        var depth = -1
        if (adapter!!.currentlySelected != null) {
            depth = adapter!!.currentNode.depth
        }
        val pos = if (old < 2) 0 else old - 1
        for (i in pos - 1 downTo 0) {
            try {
                val o = adapter!!.currentComments[adapter!!.getRealPosition(i)]
                if (o is CommentItem && pos - 1 != i) {
                    var matches = false
                    when (currentSort) {
                        CommentNavType.PARENTS -> matches = o.comment.isTopLevel
                        CommentNavType.CHILDREN -> if (depth == -1) {
                            matches = o.comment.isTopLevel
                        } else {
                            matches = o.comment.depth == depth
                            if (matches) {
                                adapter!!.currentNode = o.comment
                                adapter!!.currentSelectedItem = o.comment.comment.fullName
                            }
                        }

                        CommentNavType.TIME -> matches = (o.comment.comment != null
                                && o.comment.comment.created.time > sortTime)

                        CommentNavType.GILDED -> matches = o.comment.comment.timesGilded > 0 || o.comment.comment.timesSilvered > 0 || o.comment.comment.timesPlatinized > 0
                        CommentNavType.OP -> matches =
                            adapter!!.submission != null && (o.comment.comment
                                .author
                                    == adapter!!.submission.author)

                        CommentNavType.YOU -> matches =
                            adapter!!.submission != null && (o.comment.comment
                                .author
                                    == Authentication.name)

                        CommentNavType.LINK -> matches = o.comment.comment
                            .dataNode["body_html"]
                            .asText()
                            .contains("&lt;/a")
                    }
                    if (matches) {
                        if (i + 2 == old) {
                            doGoUp(old - 1)
                        } else {
                            (rv!!.layoutManager as PreCachingLayoutManagerComments?)!!.scrollToPositionWithOffset(
                                i + 2,
                                if ((toolbar!!.parent as View).translationY != 0f) 0 else v!!.findViewById<View>(
                                    R.id.header
                                ).height
                            )
                        }
                        break
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun goUp() {
        val toGoto = mLayoutManager!!.findFirstVisibleItemPosition()
        if (adapter != null && adapter!!.currentComments != null && !adapter!!.currentComments.isEmpty()) {
            if (adapter!!.currentlyEditing != null && !adapter!!.currentlyEditing.text
                    .toString()
                    .isEmpty()
            ) {
                AlertDialog.Builder(activity!!)
                    .setTitle(R.string.discard_comment_title)
                    .setMessage(R.string.comment_discard_msg)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        adapter!!.currentlyEditing = null
                        doGoUp(toGoto)
                    }
                    .setNegativeButton(R.string.btn_no, null)
                    .show()
            } else {
                doGoUp(toGoto)
            }
        }
    }

    fun doGoDown(old: Int) {
        var depth = -1
        if (adapter!!.currentlySelected != null) {
            depth = adapter!!.currentNode.depth
        }
        var pos = old - 2
        if (pos < 0) pos = 0
        val original = adapter!!.currentComments[adapter!!.getRealPosition(pos)].getName()
        if (old < 2) {
            (rv!!.layoutManager as PreCachingLayoutManagerComments?)!!.scrollToPositionWithOffset(
                2,
                if ((toolbar!!.parent as View).translationY != 0f) 0 else v!!.findViewById<View>(R.id.header).height
            )
        } else {
            for (i in pos + 1 until adapter!!.currentComments.size) {
                try {
                    val o = adapter!!.currentComments[adapter!!.getRealPosition(i)]
                    if (o is CommentItem) {
                        var matches = false
                        when (currentSort) {
                            CommentNavType.PARENTS -> matches = o.comment.isTopLevel
                            CommentNavType.CHILDREN -> if (depth == -1) {
                                matches = o.comment.isTopLevel
                            } else {
                                matches = o.comment.depth == depth
                                if (matches) {
                                    adapter!!.currentNode = o.comment
                                    adapter!!.currentSelectedItem = o.comment.comment.fullName
                                }
                            }

                            CommentNavType.TIME -> matches =
                                o.comment.comment.created.time > sortTime

                            CommentNavType.GILDED -> matches =
                                (o.comment.comment.timesGilded > 0 || o.comment.comment.timesSilvered > 0 || o.comment.comment.timesPlatinized) > 0

                            CommentNavType.OP -> matches =
                                adapter!!.submission != null && (o.comment.comment
                                    .author
                                        == adapter!!.submission.author)

                            CommentNavType.YOU -> matches =
                                adapter!!.submission != null && (o.comment.comment
                                    .author
                                        == Authentication.name)

                            CommentNavType.LINK -> matches = o.comment.comment
                                .dataNode["body_html"]
                                .asText()
                                .contains("&lt;/a")
                        }
                        if (matches) {
                            if (o.getName() == original) {
                                doGoDown(i + 2)
                            } else {
                                (rv!!.layoutManager as PreCachingLayoutManagerComments?)!!.scrollToPositionWithOffset(
                                    i + 2,
                                    if ((toolbar!!.parent as View).translationY != 0f) 0 else v!!.findViewById<View>(
                                        R.id.header
                                    ).height
                                )
                            }
                            break
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun goDown() {
        (toolbar!!.parent as View).translationY = -(toolbar!!.parent as View).height.toFloat()
        val toGoto = mLayoutManager!!.findFirstVisibleItemPosition()
        if (adapter != null && adapter!!.currentComments != null && !adapter!!.currentComments.isEmpty()) {
            if (adapter!!.currentlyEditing != null && !adapter!!.currentlyEditing.text
                    .toString()
                    .isEmpty()
            ) {
                AlertDialog.Builder(activity!!)
                    .setTitle(R.string.discard_comment_title)
                    .setMessage(R.string.comment_discard_msg)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        adapter!!.currentlyEditing = null
                        doGoDown(toGoto)
                    }
                    .setNegativeButton(R.string.btn_no, null)
                    .show()
            } else {
                doGoDown(toGoto)
            }
        }
    }

    private fun changeSubscription(subreddit: Subreddit, isChecked: Boolean) {
        addSubreddit(subreddit.displayName.lowercase(), getContext())
        val s = Snackbar.make(
            toolbar!!,
            if (isChecked) getString(R.string.misc_subscribed) else getString(R.string.misc_unsubscribed),
            Snackbar.LENGTH_SHORT
        )
        LayoutUtils.showSnackbar(s)
    }

    private fun setViews(
        rawHTML: String, subreddit: String, firstTextView: SpoilerRobotoTextView,
        commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subreddit)
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subreddit)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subreddit)
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    var currentSort = CommentNavType.PARENTS
    var sortTime: Long = 0

    /**
     * This method will get the measured height of the text view taking into account if the text is multiline. This is done by
     * drawing the text using TextPaint and measuring the height of the text in a text view with padding and alignment using StaticLayout.
     * More details can be found in this thread: https://stackoverflow.com/questions/41779934/how-is-staticlayout-used-in-android/41779935#41779935
     */
    private fun getTextViewMeasuredHeight(tv: TextView): Int {
        val textPaint = TextPaint()
        textPaint.typeface = tv.typeface
        textPaint.textSize = tv.textSize
        textPaint.color = tv.currentTextColor

        //Since these text views takes the whole width of the screen, we get the width of the screen and subtract right and left padding to get the actual width of the text view
        val deviceWidth = resources.displayMetrics.widthPixels - tv.paddingLeft - tv.paddingRight
        val alignment = Layout.Alignment.ALIGN_CENTER
        val spacingMultiplier = tv.lineSpacingMultiplier
        val spacingAddition = tv.lineSpacingExtra
        val staticLayout = StaticLayout(
            tv.text,
            textPaint,
            deviceWidth,
            alignment,
            spacingMultiplier,
            spacingAddition,
            false
        )

        //Add top and bottom padding to the height and return the value
        return staticLayout.height + tv.paddingTop + tv.paddingBottom
    }
}
