package me.ccrama.redditslide.Adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devspark.robototextview.RobotoTypefaces
import com.lusfold.androidkeyvaluestore.KVStore
import com.mikepenz.itemanimators.AlphaInAnimator
import com.mikepenz.itemanimators.SlideRightAlphaAnimator
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.App
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.Authentication.Companion.doVerify
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.SettingValues.commentLastVisit
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.ActionStates.getVoteDirection
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Drafts
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.LastComments.setComments
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.views.CommentOverflow
import me.ccrama.redditslide.views.DoEditorActions
import me.ccrama.redditslide.views.PreCachingLayoutManagerComments
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.OnSingleClickListener
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.stubs.SimpleTextWatcher
import net.dean.jraw.ApiException
import net.dean.jraw.RedditClient
import net.dean.jraw.http.UserAgent
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.VoteDirection
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.util.Arrays

class CommentAdapter(
    private val mPage: CommentPage, dataSet: SubmissionComments, listView: RecyclerView,
    submission: IPost?, fm: FragmentManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val postRepository get() = mPage.postRepository
    private val commentRepository get() = mPage.commentRepository

    private val SPACER = 6
    @JvmField var awardIcons: Array<Bitmap> = emptyArray()
    var mContext: Context? = mPage.requireContext()
    @JvmField var dataSet: SubmissionComments = dataSet
    @JvmField var submission: IPost? = submission
    var currentlySelected: CommentViewHolder? = null
    var currentNode: CommentView? = null
    @JvmField var currentSelectedItem: Int? = 0
    var shiftFrom = 0
    var fm: FragmentManager
    var clickpos = 0
    var currentPos = 0
    var isHolder: CommentViewHolder? = null
    var isClicking = false
    var keys = HashMap<Int, Int>()
    @JvmField var currentComments: ArrayList<CommentObject?>?
    @JvmField var deleted = ArrayList<Int>()
    @JvmField var listView: RecyclerView
    var shifted: Int
    var toShiftTo = 0
    var hidden: HashSet<Int>
    var hiddenPersons: ArrayList<Int>
    var toCollapse: ArrayList<Int>
    private var backedText = ""
    private var currentlyEditingId = 0
    var submissionViewHolder: SubmissionViewHolder? = null
    @JvmField var lastSeen: Long = 0
    @JvmField var approved = ArrayList<String>()
    @JvmField var removed = ArrayList<String>()

    fun reset(
        mContext: Context?, dataSet: SubmissionComments, listView: RecyclerView,
        submission: IPost, reset: Boolean
    ) {
        doTimes()
        this.mContext = mContext
        this.listView = listView
        this.dataSet = dataSet
        this.submission = submission
        hidden = HashSet()
        currentComments = dataSet.comments
        if (currentComments != null) {
            for (i in currentComments!!.indices) {
                keys[currentComments!![i]!!.comment!!.comment.id.id] = i
            }
        }
        hiddenPersons = ArrayList()
        toCollapse = ArrayList()
        if (currentSelectedItem != null && currentSelectedItem!! > 0 && !reset) {
            notifyDataSetChanged()
        } else {
            if (currentComments != null && !reset) {
                notifyItemRangeChanged(2, currentComments!!.size + 1)
            } else if (currentComments == null) {
                currentComments = ArrayList()
                notifyDataSetChanged()
            } else {
                notifyDataSetChanged()
            }
        }
        if (currentSelectedItem != null && currentSelectedItem!! > 0 && currentComments != null && currentComments!!.isNotEmpty()) {
            var i = 2
            for (n in currentComments!!) {
                if (n is CommentItem && n.comment!!.comment.id.id == currentSelectedItem!!) {
                    (listView.layoutManager as PreCachingLayoutManagerComments?)!!.scrollToPositionWithOffset(
                        i, mPage.headerHeight
                    )
                    break
                }
                i++
            }
            mPage.resetScroll(true)
        }
        if (mContext is BaseActivity) {
            mContext.shareUrl = "https://reddit.com" + submission.permalink
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        return when (i) {
            SPACER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.spacer_post, viewGroup, false)
                SpacerViewHolder(v)
            }

            HEADER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.submission_fullscreen, viewGroup, false)
                SubmissionViewHolder(v)
            }

            2 -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.comment, viewGroup, false)
                CommentViewHolder(v)
            }

            else -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.morecomment, viewGroup, false)
                MoreCommentViewHolder(v)
            }
        }
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    fun expandAll() {
        if (currentComments == null) return
        for (o in currentComments!!) {
            if (o!!.comment!!.comment.isTopLevel) {
                hiddenPersons.remove(o.comment!!.comment.id.id)
                //unhideAll(o.comment)
            }
        }
        notifyItemChanged(2)
    }

    fun collapseAll() {
        if (currentComments == null) return
        for (o in currentComments!!) {
            if (o!!.comment!!.comment.isTopLevel) {
                if (!hiddenPersons.contains(o.comment!!.comment.id.id)) {
                    hiddenPersons.add(o.comment!!.comment.id.id)
                }
                //hideAll(o.comment)
            }
        }
        notifyItemChanged(2)
    }

    fun doScoreText(holder: CommentViewHolder, comment: CommentView, adapter: CommentAdapter?) {
        holder.content.text = CommentAdapterHelper.getScoreString(
            comment!!, mContext!!, holder, submission,
            adapter!!
        )
    }

    fun doTimes() {
        if (submission != null && commentLastVisit && !dataSet.single && (SettingValues.storeHistory
                    && (!submission!!.isNsfw || SettingValues.storeNSFWHistory))
        ) {
            //lastSeen = HasSeen.getSeenTime(submission!!)
            var fullname = submission!!.permalink
            if (fullname.contains("t3_")) {
                fullname = fullname.substring(3)
            }
            HasSeen.seenTimes[fullname] = System.currentTimeMillis()
            KVStore.getInstance().insert(fullname, System.currentTimeMillis().toString())
        }
        if (submission != null) {
            if (SettingValues.storeHistory) {
                if (submission!!.isNsfw && !SettingValues.storeNSFWHistory) {
                } else {
                    HasSeen.addSeen(submission!!.permalink)
                }
                setComments(submission!!)
            }
        }
    }

    override fun onBindViewHolder(firstHolder: RecyclerView.ViewHolder, old: Int) {
        val pos = if (old != 0) old - 1 else old
        if (firstHolder is CommentViewHolder) {
            val holder = firstHolder
            var datasetPosition = pos - 1
            datasetPosition = getRealPosition(datasetPosition)
            if (pos > toShiftTo) {
                shifted = 0
            }
            if (pos < shiftFrom) {
                shifted = 0
            }
            val baseNode: CommentView? = currentComments!![datasetPosition]!!.comment
            val comment = baseNode!!
            if (pos == itemCount - 1) {
                holder.itemView.setPadding(
                    0, 0, 0, mContext!!.resources
                        .getDimension(R.dimen.overview_top_padding_single).toInt()
                )
            } else {
                holder.itemView.setPadding(0, 0, 0, 0)
            }
            doScoreText(holder, comment, this)

            //Long click listeners
            val onLongClickListener = View.OnLongClickListener {
                if (SettingValues.swap) {
                    doOnClick(holder, comment, baseNode) } else {
                    doLongClick(holder, comment, baseNode)
                }
                true
            }
            holder.firstTextView.setOnLongClickListener(onLongClickListener)
            holder.commentOverflow.setOnLongClickListener(onLongClickListener)
            holder.itemView.setOnLongClickListener {
                if (currentlyEditingId != comment!!.comment.id.id) {
                    if (SettingValues.swap) {
                        doOnClick(holder, comment, baseNode)
                    } else {
                        doLongClick(holder, comment, baseNode)
                    }
                }
                true
            }

            //Single click listeners
            val singleClick: OnSingleClickListener = object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (currentlyEditingId != comment!!.comment.id.id) {
                        if (SettingValues.swap) {
                            doLongClick(holder, comment, baseNode)
                        } else {
                            doOnClick(holder, comment, baseNode)
                        }
                    }
                }
            }
            holder.itemView.setOnClickListener(singleClick)
            holder.commentOverflow.setOnClickListener(singleClick)
            if (!toCollapse.contains(comment!!.comment.id.id) || !SettingValues.collapseComments) {
                setViews(
                    comment.comment.contentHtml,
                    submission!!.groupName, holder, singleClick, onLongClickListener
                )
            }
            holder.firstTextView.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    val SpoilerRobotoTextView = v as SpoilerRobotoTextView
                    if (SettingValues.swap) {
                        if (!SpoilerRobotoTextView.isSpoilerClicked) {
                            doLongClick(holder, comment, baseNode)
                        } else if (SpoilerRobotoTextView.isSpoilerClicked) {
                            SpoilerRobotoTextView.resetSpoilerClicked()
                        }
                    } else if (!SpoilerRobotoTextView.isSpoilerClicked) {
                        doOnClick(holder, comment, baseNode)
                    } else if (SpoilerRobotoTextView.isSpoilerClicked) {
                        SpoilerRobotoTextView.resetSpoilerClicked()
                    }
                }
            })
            /*
            if (ImageFlairs.isSynced(comment.groupName) && comment.creator.nameFlair != null && comment.authorFlair.cssClass != null && !comment.authorFlair.cssClass.isEmpty()) {
                var set = false
                for (s in comment.creator.nameFlair.cssClass.split(" ".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    val loader = ImageFlairs.getFlairImageLoader(mContext)
                    val file = DiskCacheUtils.findInCache(
                        comment.groupName.lowercase()
                                + ":"
                                + s.lowercase(),
                        loader.diskCache
                    )
                    if (file != null && file.exists()) {
                        set = true
                        holder.imageFlair.visibility = View.VISIBLE
                        val decodedImgUri = Uri.fromFile(file).toString()
                        loader.displayImage(decodedImgUri, holder.imageFlair)
                        break
                    }
                }
                if (!set) {
                    holder.imageFlair.setImageDrawable(null)
                    holder.imageFlair.visibility = View.GONE
                }
            } else {
                holder.imageFlair.visibility = View.GONE
            }
             */
            //Set typeface for body
            val type = FontPreferences(mContext).fontTypeComment.typeface
            val typeface: Typeface = if (type >= 0) {
                RobotoTypefaces.obtainTypeface(mContext!!, type)
            } else {
                Typeface.DEFAULT
            }
            holder.firstTextView.setTypeface(typeface)


            //Show padding on top
            if (baseNode.comment.isTopLevel) {
                holder.itemView.findViewById<View>(R.id.next).visibility = View.VISIBLE
            } else if (holder.itemView.findViewById<View>(R.id.next).visibility == View.VISIBLE) {
                holder.itemView.findViewById<View>(R.id.next).visibility = View.GONE
            }

            //Should be collapsed?
            if (hiddenPersons.contains(comment.comment.id.id) || toCollapse.contains(
                    comment.comment.id.id
                )
            ) {
                val childnumber = getChildNumber(baseNode)
                if (hiddenPersons.contains(comment.comment.id.id) && childnumber > 0) {
                    holder.childrenNumber.visibility = View.VISIBLE
                    holder.childrenNumber.text = "+$childnumber"
                } else {
                    holder.childrenNumber.visibility = View.GONE
                }
                if (SettingValues.collapseComments && toCollapse.contains(comment.comment.id.id)) {
                    holder.firstTextView.visibility = View.GONE
                    holder.commentOverflow.visibility = View.GONE
                }
            } else {
                holder.childrenNumber.visibility = View.GONE
                holder.commentOverflow.visibility = View.VISIBLE
            }
            holder.dot.visibility = View.VISIBLE
            val dwidth = ((if (SettingValues.largeDepth) 5 else 3) * Resources.getSystem()
                .displayMetrics.density).toInt()
            var width = 0

            //Padding on the left, starting with the third comment
            for (i in 2 until baseNode.comment.depth) {
                width += dwidth
            }
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.setMargins(width, 0, 0, 0)
            holder.itemView.layoutParams = params
            val params2 = holder.dot.layoutParams as RelativeLayout.LayoutParams
            params2.width = dwidth
            holder.dot.layoutParams = params2
            if (baseNode.comment.depth - 1 > 0) {
                val i22 = baseNode.comment.depth - 2
                val commentOp = dataSet.commentOPs.get(comment.comment.id.id)
                if (SettingValues.highlightCommentOP && commentOp != null && comment != null && commentOp == comment.creator.name) {
                    holder.dot.setBackgroundColor(
                        ContextCompat.getColor(mContext!!, R.color.md_purple_500)
                    )
                } else {
                    if (i22 % 5 == 0) {
                        holder.dot.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext!!,
                                if (!SettingValues.colorCommentDepth) R.color.md_grey_700 else R.color.md_blue_500
                            )
                        )
                    } else if (i22 % 4 == 0) {
                        holder.dot.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext!!,
                                if (!SettingValues.colorCommentDepth) R.color.md_grey_600 else R.color.md_green_500
                            )
                        )
                    } else if (i22 % 3 == 0) {
                        holder.dot.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext!!,
                                if (!SettingValues.colorCommentDepth) R.color.md_grey_500 else R.color.md_yellow_500
                            )
                        )
                    } else if (i22 % 2 == 0) {
                        holder.dot.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext!!,
                                if (!SettingValues.colorCommentDepth) R.color.md_grey_400 else R.color.md_orange_500
                            )
                        )
                    } else {
                        holder.dot.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext!!,
                                if (!SettingValues.colorCommentDepth) R.color.md_grey_300 else R.color.md_red_500
                            )
                        )
                    }
                }
            } else {
                holder.dot.visibility = View.GONE
            }
            if (currentSelectedItem != null && comment.comment.id.id == currentSelectedItem!!
                && currentSelectedItem!! != 0
                && currentlyEditingId != comment.comment.id.id
            ) {
                doHighlighted(holder, comment, baseNode, false)
            } else if (currentlyEditingId != comment.comment.id.id) {
                setCommentStateUnhighlighted(holder, baseNode, false)
            }
            if (deleted.contains(comment.comment.id.id)) {
                holder.firstTextView.setText(R.string.comment_deleted)
                holder.content.setText(R.string.comment_deleted)
            }
            if (currentlyEditingId == comment.comment.id.id) {
                setCommentStateUnhighlighted(holder, baseNode, false)
                setCommentStateHighlighted(holder, comment, baseNode, true, false)
            }
            if (SettingValues.collapseDeletedComments) {
                if (comment.comment.content.startsWith("[removed]") || comment.comment.content.startsWith("[deleted]")) {
                    holder.firstTextView.visibility = View.GONE
                    holder.commentOverflow.visibility = View.GONE
                }
            }
        } else if (firstHolder is SubmissionViewHolder && submission != null) {
            submissionViewHolder = firstHolder
            PopulateSubmissionViewHolder(postRepository, commentRepository).populateSubmissionViewHolder(
                firstHolder, submission!!, mPage.requireActivity(), true, true,
                mutableListOf<IPost>(), listView, false, false, submission!!.groupName, this
            )
            if (Authentication.isLoggedIn && Authentication.didOnline) {
                if (submission!!.isArchived || submission!!.isLocked) {
                    firstHolder.itemView.findViewById<View>(R.id.reply).visibility = View.GONE
                } else {
                    firstHolder.itemView.findViewById<View>(R.id.reply)
                        .setOnClickListener(object : OnSingleClickListener() {
                            override fun onSingleClick(v: View) {
                                doReplySubmission(firstHolder)
                            }
                        })
                    firstHolder.itemView.findViewById<View>(R.id.discard)
                        .setOnClickListener(object : OnSingleClickListener() {
                            override fun onSingleClick(v: View) {
                                firstHolder.itemView.findViewById<View>(R.id.innerSend).visibility =
                                    View.GONE
                                currentlyEditing = null
                                editingPosition = -1
                                if (SettingValues.fastscroll) {
                                    mPage.fastScroll!!.visibility = View.VISIBLE
                                }
                                if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                                mPage.overrideFab = false
                                currentlyEditingId = 0
                                backedText = ""
                                val view =
                                    mPage.requireActivity().findViewById<View>(android.R.id.content)
                                if (view != null) {
                                    KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                                }
                            }
                        })
                }
            } else {
                firstHolder.itemView.findViewById<View>(R.id.innerSend).visibility = View.GONE
                firstHolder.itemView.findViewById<View>(R.id.reply).visibility = View.GONE
            }
            firstHolder.itemView.findViewById<View>(R.id.more)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        firstHolder.itemView.findViewById<View>(R.id.menu).callOnClick()
                    }
                })
        } else if (firstHolder is MoreCommentViewHolder) {
            val holder = firstHolder
            var nextPos = pos - 1
            nextPos = getRealPosition(nextPos)
            val baseNode = currentComments!![nextPos] as MoreChildItem?
            if (baseNode!!.children.count > 0) {
                try {
                    holder.content.text = mContext!!.getString(
                        R.string.comment_load_more_string_new,
                        baseNode.children.localizedCount
                    )
                } catch (e: Exception) {
                    holder.content.setText(R.string.comment_load_more_number_unknown)
                }
            } else if (!baseNode.children.childrenIds.isEmpty()) {
                holder.content.setText(R.string.comment_load_more_number_unknown)
            } else {
                holder.content.setText(R.string.thread_continue)
            }
            val dwidth = ((if (SettingValues.largeDepth) 5 else 3) * Resources.getSystem()
                .displayMetrics.density).toInt()
            var width = 0
            for (i in 1 until baseNode.comment!!.comment.depth) {
                width += dwidth
            }
            val progress = holder.loading
            progress.visibility = View.GONE
            val finalNextPos = nextPos
            holder.content.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    if (baseNode.children.childrenIds.isEmpty()) {
                        val toGoTo = ("https://reddit.com"
                                + submission!!.permalink
                                + baseNode.comment!!.comment.id
                                + "?context=0")
                        OpenRedditLink.openUrl(mContext, toGoTo, true)
                    } else if (progress.visibility == View.GONE) {
                        progress.visibility = View.VISIBLE
                        holder.content.setText(R.string.comment_loading_more)
                        currentLoading = AsyncLoadMore(
                            getRealPosition(holder.bindingAdapterPosition - 2),
                            holder.bindingAdapterPosition, holder, finalNextPos,
                            baseNode.comment!!.permalink
                        )
                        currentLoading!!.execute(baseNode)
                    }
                }
            })
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.setMargins(width, 0, 0, 0)
            holder.itemView.layoutParams = params
        }
        if (firstHolder is SpacerViewHolder) {
            //Make a space the size of the toolbar minus 1 so there isn't a gap
            firstHolder.itemView.findViewById<View>(R.id.height).layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Constants.SINGLE_HEADER_VIEW_OFFSET - DisplayUtil.dpToPxVertical(1)
                            + mPage.shownHeaders
                )
        }
    }

    var currentLoading: AsyncLoadMore? = null
    var changedProfile: String? = null
    private fun doReplySubmission(submissionViewHolder: RecyclerView.ViewHolder) {
        val replyArea = submissionViewHolder.itemView.findViewById<View>(R.id.innerSend)
        if (replyArea.visibility == View.GONE) {
            expandSubmissionReply(replyArea)
            val replyLine = submissionViewHolder.itemView.findViewById<EditText>(R.id.replyLine)
            DoEditorActions.doActions(
                replyLine,
                submissionViewHolder.itemView,
                fm,
                mPage.requireActivity(),
                if (submission!!.url.isNullOrBlank()) submission!!.body else null,
                arrayOf(
                    submission!!.creator.name
                )
            )
            currentlyEditing = submissionViewHolder.itemView.findViewById(R.id.replyLine)
            val profile = submissionViewHolder.itemView.findViewById<TextView>(R.id.profile)
            changedProfile = Authentication.name
            profile.text = "/u/$changedProfile"
            profile.setOnClickListener {
                val accounts = HashMap<String?, String>()
                for (s in authentication.getStringSet(
                    "accounts",
                    HashSet()
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
                val i = keys.indexOf(changedProfile)
                AlertDialog.Builder(mContext!!)
                    .setTitle(R.string.replies_switch_accounts)
                    .setSingleChoiceItems(
                        keys.toTypedArray(),
                        i
                    ) { dialog: DialogInterface?, which: Int ->
                        changedProfile = keys[which]
                        profile.text = "/u/$changedProfile"
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
            currentlyEditing!!.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    mPage.fastScroll!!.visibility = View.GONE
                    if (mPage.fab != null) mPage.fab!!.visibility = View.GONE
                    mPage.overrideFab = true
                } else if (SettingValues.fastscroll) {
                    mPage.fastScroll!!.visibility = View.VISIBLE
                    if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                    mPage.overrideFab = false
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                replyLine.onFocusChangeListener = OnFocusChangeListener { v: View, b: Boolean ->
                    if (b) {
                        v.postDelayed({ if (!v.hasFocus()) v.requestFocus() }, 100)
                    }
                }
            }
            replyLine.requestFocus()
            KeyboardUtil.toggleKeyboard(
                mContext,
                InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
            )
            editingPosition = submissionViewHolder.bindingAdapterPosition
            submissionViewHolder.itemView.findViewById<View>(R.id.send)
                .setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        dataSet.refreshLayout.isRefreshing = true
                        if (SettingValues.fastscroll) {
                            mPage.fastScroll!!.visibility = View.VISIBLE
                        }
                        if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                        mPage.overrideFab = false
                        if (currentlyEditing != null) {
                            val text = currentlyEditing!!.text.toString()
                            ReplyTaskComment(submission!!.submission, changedProfile).execute(text)
                            replyArea.visibility = View.GONE
                            currentlyEditing!!.setText("")
                            currentlyEditing = null
                            editingPosition = -1
                            //Hide soft keyboard
                            val view =
                                mPage.requireActivity().findViewById<View>(android.R.id.content)
                            if (view != null) {
                                KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                            }
                        }
                    }
                })
        } else {
            val view = mPage.requireActivity().findViewById<View>(android.R.id.content)
            if (view != null) {
                KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
            }
            collapseAndHide(replyArea)
        }
    }

    fun setViews(
        rawHTML: String, groupName: String?,
        firstTextView: SpoilerRobotoTextView, commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], groupName)
            startIndex = 1
        } else {
            firstTextView.text = ""
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, groupName)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), groupName)
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    fun setViews(
        rawHTML: String, groupName: String?,
        firstTextView: SpoilerRobotoTextView, commentOverflow: CommentOverflow,
        click: View.OnClickListener?, onLongClickListener: View.OnLongClickListener?
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0] + " ", groupName)
            startIndex = 1
        } else {
            firstTextView.text = ""
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, groupName, click, onLongClickListener)
            } else {
                commentOverflow.setViews(
                    blocks.subList(startIndex, blocks.size), groupName,
                    click, onLongClickListener
                )
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    private fun setViews(rawHTML: String, groupName: String, holder: CommentViewHolder) {
        setViews(rawHTML, groupName, holder.firstTextView, holder.commentOverflow)
    }

    private fun setViews(
        rawHTML: String, groupName: String, holder: CommentViewHolder,
        click: View.OnClickListener, longClickListener: View.OnLongClickListener
    ) {
        setViews(
            rawHTML, groupName, holder.firstTextView, holder.commentOverflow, click,
            longClickListener
        )
    }

    var editingPosition = 0
    private fun collapseAndHide(v: View) {
        val finalHeight = v.height
        mAnimator = AnimatorUtil.slideAnimator(finalHeight, 0, v)
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                //Height=0, but it set visibility to GONE
                v.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                v.visibility = View.GONE
            }
        })
        mAnimator!!.start()
    }

    private fun collapseAndRemove(v: View) {
        val finalHeight = v.height
        mAnimator = AnimatorUtil.slideAnimator(finalHeight, 0, v)
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                //Height=0, but it set visibility to GONE
                (v as LinearLayout).removeAllViews()
            }

            override fun onAnimationCancel(animation: Animator) {
                (v as LinearLayout).removeAllViews()
            }
        })
        mAnimator!!.start()
    }

    private fun doShowMenu(l: View) {
        l.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l.measure(widthSpec, heightSpec)
        val l2 = l.findViewById<View>(R.id.menu)
        val widthSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l2.measure(widthSpec2, heightSpec2)
        val mAnimator = AnimatorUtil.slideAnimator(l.measuredHeight, l2.measuredHeight, l)
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                l2.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {
                l2.visibility = View.VISIBLE
            }
        })
        mAnimator!!.start()
    }

    var mAnimator: ValueAnimator? = null
    private fun expand(l: View) {
        l.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l.measure(widthSpec, heightSpec)
        val l2 =
            if (l.findViewById<View?>(R.id.replyArea) == null) l.findViewById(R.id.innerSend) else l.findViewById<View>(
                R.id.replyArea
            )
        val widthSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l2.measure(widthSpec2, heightSpec2)
        mAnimator = AnimatorUtil.slideAnimator(0, l.measuredHeight - l2.measuredHeight, l)
        mAnimator!!.start()
    }

    private fun expandAndSetParams(l: View) {
        l.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l.measure(widthSpec, heightSpec)
        val l2 =
            if (l.findViewById<View?>(R.id.replyArea) == null) l.findViewById(R.id.innerSend) else l.findViewById<View>(
                R.id.replyArea
            )
        val widthSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l2.measure(widthSpec2, heightSpec2)
        mAnimator = AnimatorUtil.slideAnimator(
            l.measuredHeight - l2.measuredHeight,
            l.measuredHeight - (l.measuredHeight - l2.measuredHeight), l
        )
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val params = l.layoutParams as RelativeLayout.LayoutParams
                params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                params.addRule(RelativeLayout.BELOW, R.id.commentOverflow)
                l.layoutParams = params
            }

            override fun onAnimationCancel(animation: Animator) {
                val params = l.layoutParams as RelativeLayout.LayoutParams
                params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                params.addRule(RelativeLayout.BELOW, R.id.commentOverflow)
                l.layoutParams = params
            }
        })
        mAnimator!!.start()
    }

    private fun expandSubmissionReply(l: View) {
        l.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        l.measure(widthSpec, heightSpec)
        mAnimator = AnimatorUtil.slideAnimator(0, l.measuredHeight, l)
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val params = l.layoutParams as LinearLayout.LayoutParams
                params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                l.layoutParams = params
            }

            override fun onAnimationCancel(animation: Animator) {
                val params = l.layoutParams as LinearLayout.LayoutParams
                params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                l.layoutParams = params
            }
        })
        mAnimator!!.start()
    }

    @JvmField
    var currentBaseNode: CommentView? = null
    fun setCommentStateHighlighted(
        holder: CommentViewHolder, n: CommentView?,
        baseNode: CommentView?, isReplying: Boolean, animate: Boolean
    ) {
        if (currentlySelected != null && currentlySelected !== holder) {
            setCommentStateUnhighlighted(currentlySelected!!, currentBaseNode, true)
        }
        if (mContext is BaseActivity) {
            (mContext as BaseActivity).shareUrl = ("https://reddit.com"
                    + submission!!.permalink
                    + n!!.comment.id.id
                    + "?context=3")
        }

        // If a comment is hidden and (Swap long press == true), then a single click will un-hide the comment
        // and expand to show all children comments
        if (SettingValues.swap && holder.firstTextView.visibility == View.GONE && !isReplying) {
            hiddenPersons.remove(n!!.comment.id.id)
            unhideAll(baseNode, holder.bindingAdapterPosition + 1)
            if (toCollapse.contains(n.comment.id.id) && SettingValues.collapseComments) {
                setViews(
                    n!!.comment.contentHtml, submission!!.groupName,
                    holder
                )
            }
            CommentAdapterHelper.hideChildrenObject(holder.childrenNumber)
            holder.commentOverflow.visibility = View.VISIBLE
            toCollapse.remove(n.comment.id.id)
        } else {
            currentlySelected = holder
            currentBaseNode = baseNode
            val color = Palette.getColor(n!!.community.name)
            currentSelectedItem = n!!.comment.id.id
            currentNode = baseNode
            val inflater = mPage.requireActivity().layoutInflater
            resetMenu(holder.menuArea, false)
            val baseView = inflater.inflate(
                if (SettingValues.rightHandedCommentMenu) R.layout.comment_menu_right_handed else R.layout.comment_menu,
                holder.menuArea
            )
            if (!isReplying) {
                baseView.visibility = View.GONE
                if (animate) {
                    expand(baseView)
                } else {
                    baseView.visibility = View.VISIBLE
                    val widthSpec =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    val heightSpec =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    baseView.measure(widthSpec, heightSpec)
                    val l2 =
                        if (baseView.findViewById<View?>(R.id.replyArea) == null) baseView.findViewById(
                            R.id.innerSend
                        ) else baseView.findViewById<View>(R.id.replyArea)
                    val widthSpec2 =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    val heightSpec2 =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    l2.measure(widthSpec2, heightSpec2)
                    val layoutParams = baseView.layoutParams
                    layoutParams.height = baseView.measuredHeight - l2.measuredHeight
                    baseView.layoutParams = layoutParams
                }
            }
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.setMargins(0, 0, 0, 0)
            holder.itemView.layoutParams = params
            val reply = baseView.findViewById<View>(R.id.reply)
            val send = baseView.findViewById<View>(R.id.send)
            val menu = baseView.findViewById<View>(R.id.menu)
            val replyArea = baseView.findViewById<View>(R.id.replyArea)
            val more = baseView.findViewById<View>(R.id.more)
            val upvote = baseView.findViewById<ImageView>(R.id.upvote)
            val downvote = baseView.findViewById<ImageView>(R.id.downvote)
            val discard = baseView.findViewById<View>(R.id.discard)
            val replyLine = baseView.findViewById<EditText>(R.id.replyLine)
            val mod = baseView.findViewById<ImageView>(R.id.mod)
            val comment = baseNode!!
            if (getVoteDirection(comment) == VoteDirection.UPVOTE) {
                BlendModeUtil.tintImageViewAsModulate(upvote, holder.textColorUp)
                upvote.contentDescription = mContext!!.resources.getString(R.string.btn_upvoted)
            } else if (getVoteDirection(comment) == VoteDirection.DOWNVOTE) {
                BlendModeUtil.tintImageViewAsModulate(downvote, holder.textColorDown)
                downvote.contentDescription = mContext!!.resources.getString(R.string.btn_downvoted)
            } else {
                downvote.clearColorFilter()
                downvote.contentDescription = mContext!!.resources.getString(R.string.btn_downvote)
                upvote.clearColorFilter()
                upvote.contentDescription = mContext!!.resources.getString(R.string.btn_upvote)
            }
            try {
                if (UserSubscriptions.modOf!!.contains(submission!!.groupName)) {
                    //todo
                    mod.visibility = View.GONE
                } else {
                    mod.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.d(LogUtil.getTag(), "Error loading mod $e")
            }
            if (UserSubscriptions.modOf != null && UserSubscriptions.modOf!!.contains(
                    submission!!.groupName.lowercase()
                )
            ) {
                /*
                mod.visibility = View.VISIBLE
                val reports = comment.userReports
                val reports2 = comment.moderatorReports
                if (reports.size + reports2.size > 0) {
                    BlendModeUtil.tintImageViewAsSrcAtop(
                        mod, ContextCompat.getColor(mContext!!, R.color.md_red_300)
                    )
                } else {
                    BlendModeUtil.tintImageViewAsSrcAtop(mod, Color.WHITE)
                }
                mod.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        /*
                        CommentAdapterHelper.showModBottomSheet(
                            this@CommentAdapter, mContext!!,
                            baseNode, comment, holder, reports, reports2
                        )
                         */
                    }
                })
                 */
            } else {
                mod.visibility = View.GONE
            }
            val edit = baseView.findViewById<ImageView>(R.id.edit)
            if (Authentication.name != null && (Authentication.name!!.lowercase()
                        == comment.creator.name.lowercase()) && Authentication.didOnline
            ) {
                edit.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        /*
                        CommentAdapterHelper.doCommentEdit(
                            this@CommentAdapter,
                            mContext!!,
                            fm,
                            baseNode,
                            if (baseNode.comment.isTopLevel) submission!!.body else baseNode.parent.comment.body,
                            holder
                        )
                         */
                    }
                })
            } else {
                edit.visibility = View.GONE
            }
            val delete = baseView.findViewById<ImageView>(R.id.delete)
            if (Authentication.name != null && (Authentication.name!!.lowercase()
                        == comment.creator.name.lowercase()) && Authentication.didOnline
            ) {
                delete.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        /*
                        CommentAdapterHelper.deleteComment(
                            this@CommentAdapter, mContext!!, baseNode,
                            holder
                        )
                         */
                    }
                })
            } else {
                delete.visibility = View.GONE
            }
            if (Authentication.isLoggedIn
                && !submission!!.isArchived
                && !submission!!.isLocked
                && !deleted.contains(n!!.comment.id.id)
                && comment.creator.name != "[deleted]"
                && Authentication.didOnline
            ) {
                if (isReplying) {
                    baseView.visibility = View.VISIBLE
                    val widthSpec =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    val heightSpec =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    baseView.measure(widthSpec, heightSpec)
                    val l2 =
                        if (baseView.findViewById<View?>(R.id.replyArea) == null) baseView.findViewById(
                            R.id.innerSend
                        ) else baseView.findViewById<View>(R.id.replyArea)
                    val widthSpec2 =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    val heightSpec2 =
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    l2.measure(widthSpec2, heightSpec2)
                    val params2 = baseView.layoutParams as RelativeLayout.LayoutParams
                    params2.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                    params2.addRule(RelativeLayout.BELOW, R.id.commentOverflow)
                    baseView.layoutParams = params2
                    replyArea.visibility = View.VISIBLE
                    menu.visibility = View.GONE
                    currentlyEditing = replyLine
                    currentlyEditing!!.onFocusChangeListener =
                        OnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                mPage.fastScroll!!.visibility = View.GONE
                                if (mPage.fab != null) {
                                    mPage.fab!!.visibility = View.GONE
                                }
                                mPage.overrideFab = true
                            } else if (SettingValues.fastscroll) {
                                mPage.fastScroll!!.visibility = View.VISIBLE
                                if (mPage.fab != null) {
                                    mPage.fab!!.visibility = View.VISIBLE
                                }
                                mPage.overrideFab = false
                            }
                        }
                    val profile = baseView.findViewById<TextView>(R.id.profile)
                    changedProfile = Authentication.name
                    profile.text = "/u/$changedProfile"
                    profile.setOnClickListener {
                        val accounts = HashMap<String?, String>()
                        for (s in authentication.getStringSet(
                            "accounts",
                            HashSet()
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
                        val i = keys.indexOf(changedProfile)
                        AlertDialog.Builder(mContext!!)
                            .setTitle(R.string.sorting_choose)
                            .setSingleChoiceItems(
                                keys.toTypedArray(),
                                i
                            ) { dialog: DialogInterface?, which: Int ->
                                changedProfile = keys[which]
                                profile.text = "/u/$changedProfile"
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    }
                    replyLine.requestFocus()
                    KeyboardUtil.toggleKeyboard(
                        mContext,
                        InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                    currentlyEditingId = n!!.comment.id.id
                    replyLine.setText(backedText)
                    replyLine.addTextChangedListener(object : SimpleTextWatcher() {
                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            backedText = s.toString()
                        }
                    })
                    editingPosition = holder.bindingAdapterPosition
                }
                reply.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        expandAndSetParams(baseView)

                        //If the base theme is Light or Sepia, tint the Editor actions to be white
                        if (SettingValues.currentTheme == 1 || SettingValues.currentTheme == 5) {
                            val saveDraft =
                                replyArea.findViewById<View>(R.id.savedraft) as ImageView
                            val draft = replyArea.findViewById<View>(R.id.draft) as ImageView
                            val imagerep = replyArea.findViewById<View>(R.id.imagerep) as ImageView
                            val link = replyArea.findViewById<View>(R.id.link) as ImageView
                            val bold = replyArea.findViewById<View>(R.id.bold) as ImageView
                            val italics = replyArea.findViewById<View>(R.id.italics) as ImageView
                            val bulletlist =
                                replyArea.findViewById<View>(R.id.bulletlist) as ImageView
                            val numlist = replyArea.findViewById<View>(R.id.numlist) as ImageView
                            val draw = replyArea.findViewById<View>(R.id.draw) as ImageView
                            val quote = replyArea.findViewById<View>(R.id.quote) as ImageView
                            val size = replyArea.findViewById<View>(R.id.size) as ImageView
                            val strike = replyArea.findViewById<View>(R.id.strike) as ImageView
                            val author = replyArea.findViewById<View>(R.id.author) as ImageView
                            val spoiler = replyArea.findViewById<View>(R.id.spoiler) as ImageView
                            val imageViewSet = Arrays.asList(
                                saveDraft, draft, imagerep, link, bold, italics, bulletlist,
                                numlist, draw, quote, size, strike, author, spoiler
                            )
                            BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, Color.WHITE)
                            BlendModeUtil.tintDrawableAsSrcIn(replyLine.background, Color.WHITE)
                        }
                        replyArea.visibility = View.VISIBLE
                        menu.visibility = View.GONE
                        currentlyEditing = replyLine
                        DoEditorActions.doActions(
                            currentlyEditing, replyArea, fm,
                            mPage.requireActivity(), comment.comment.content, getParents(baseNode)
                        )
                        currentlyEditing!!.onFocusChangeListener =
                            OnFocusChangeListener { v, hasFocus ->
                                if (hasFocus) {
                                    mPage.fastScroll!!.visibility = View.GONE
                                    if (mPage.fab != null) mPage.fab!!.visibility = View.GONE
                                    mPage.overrideFab = true
                                } else if (SettingValues.fastscroll) {
                                    mPage.fastScroll!!.visibility = View.VISIBLE
                                    if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                                    mPage.overrideFab = false
                                }
                            }
                        val profile = baseView.findViewById<TextView>(R.id.profile)
                        changedProfile = Authentication.name
                        profile.text = "/u/$changedProfile"
                        profile.setOnClickListener {
                            val accounts = HashMap<String?, String>()
                            for (s in authentication.getStringSet(
                                "accounts", HashSet()
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
                            val i = keys.indexOf(changedProfile)
                            AlertDialog.Builder(mContext!!)
                                .setTitle(R.string.sorting_choose)
                                .setSingleChoiceItems(
                                    keys.toTypedArray(),
                                    i
                                ) { dialog: DialogInterface?, which: Int ->
                                    changedProfile = keys[which]
                                    profile.text = "/u/$changedProfile"
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            replyLine.onFocusChangeListener =
                                OnFocusChangeListener { view: View, b: Boolean ->
                                    if (b) {
                                        view.postDelayed(
                                            { if (!view.hasFocus()) view.requestFocus() },
                                            100
                                        )
                                    }
                                }
                        }
                        replyLine.requestFocus() // TODO: Not working when called a second time
                        KeyboardUtil.toggleKeyboard(
                            mContext,
                            InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
                        )
                        currentlyEditingId = n!!.comment.id.id
                        replyLine.addTextChangedListener(object : SimpleTextWatcher() {
                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                backedText = s.toString()
                            }
                        })
                        editingPosition = holder.bindingAdapterPosition
                    }
                })
                send.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        currentlyEditingId = 0
                        backedText = ""
                        doShowMenu(baseView)
                        if (SettingValues.fastscroll) {
                            mPage.fastScroll!!.visibility = View.VISIBLE
                            if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                            mPage.overrideFab = false
                        }
                        dataSet.refreshLayout.isRefreshing = true
                        if (currentlyEditing != null) {
                            val text = currentlyEditing!!.text.toString()
                            //ReplyTaskComment(n, baseNode, holder, changedProfile).execute(text)
                            currentlyEditing = null
                            editingPosition = -1
                        }
                        //Hide soft keyboard
                        val view =
                            mPage.requireActivity().findViewById<View>(android.R.id.content)
                        if (view != null) {
                            KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                        }
                    }
                })
                discard.setOnClickListener(object : OnSingleClickListener() {
                    override fun onSingleClick(v: View) {
                        currentlyEditing = null
                        editingPosition = -1
                        currentlyEditingId = 0
                        backedText = ""
                        mPage.overrideFab = false
                        val view =
                            mPage.requireActivity().findViewById<View>(android.R.id.content)
                        if (view != null) {
                            KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                        }
                        doShowMenu(baseView)
                    }
                })
            } else {
                if (reply.visibility == View.VISIBLE) {
                    reply.visibility = View.GONE
                }
                if (((submission!!.isArchived
                            || deleted.contains(n!!.comment.id.id)) || comment.creator.name == "[deleted]"
                            && Authentication.isLoggedIn
                            && Authentication.didOnline) && upvote.visibility == View.VISIBLE
                ) {
                    upvote.visibility = View.GONE
                }
                if (((submission!!.isArchived
                            || deleted.contains(n!!.comment.id.id)) || comment.creator.name == "[deleted]"
                            && Authentication.isLoggedIn
                            && Authentication.didOnline) && downvote.visibility == View.VISIBLE
                ) {
                    downvote.visibility = View.GONE
                }
            }
            more.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    /*
                    CommentAdapterHelper.showOverflowBottomSheet(
                        this@CommentAdapter, mContext!!,
                        holder, baseNode
                    )
                     */
                }
            })
            upvote.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    setCommentStateUnhighlighted(holder, comment, baseNode, true)
                    if (getVoteDirection(comment) == VoteDirection.UPVOTE) {
                        /*
                        Vote(v, mContext).execute(n)
                        setVoteDirection(comment, VoteDirection.NO_VOTE)
                        doScoreText(holder, n, this@CommentAdapter)
                        upvote.clearColorFilter()
                        */
                    } else {
                        /*
                        Vote(true, v, mContext).execute(n)
                        setVoteDirection(comment, VoteDirection.UPVOTE)
                        downvote.clearColorFilter() // reset colour
                        doScoreText(holder, n, this@CommentAdapter)
                        BlendModeUtil.tintImageViewAsModulate(upvote, holder.textColorUp)
                        */
                    }
                }
            })
            downvote.setOnClickListener(object : OnSingleClickListener() {
                override fun onSingleClick(v: View) {
                    setCommentStateUnhighlighted(holder, comment, baseNode, true)
                    if (getVoteDirection(comment) == VoteDirection.DOWNVOTE) {
                        /*
                        Vote(v, mContext).execute(n)
                        setVoteDirection(comment, VoteDirection.NO_VOTE)
                        doScoreText(holder, n, this@CommentAdapter)
                        downvote.clearColorFilter()
                        */
                    } else {
                        /*
                        Vote(false, v, mContext).execute(n)
                        setVoteDirection(comment, VoteDirection.DOWNVOTE)
                        upvote.clearColorFilter() // reset colour
                        doScoreText(holder, n, this@CommentAdapter)
                        BlendModeUtil.tintImageViewAsModulate(downvote, holder.textColorDown)
                        */
                    }
                }
            })
            menu.setBackgroundColor(color)
            replyArea.setBackgroundColor(color)
            if (!isReplying) {
                menu.visibility = View.VISIBLE
                replyArea.visibility = View.GONE
            }
            holder.itemView.findViewById<View>(R.id.background)
                .setBackgroundColor(
                    Color.argb(
                        50, Color.red(color), Color.green(color),
                        Color.blue(color)
                    )
                )
        }
    }

    fun doHighlighted(
        holder: CommentViewHolder, n: CommentView?,
        baseNode: CommentView?, animate: Boolean
    ) {
        if (mAnimator != null && mAnimator!!.isRunning) {
            holder.itemView.postDelayed({
                setCommentStateHighlighted(
                    holder,
                    n,
                    baseNode,
                    false,
                    true
                )
            }, mAnimator!!.duration)
        } else {
            setCommentStateHighlighted(holder, n, baseNode, false, animate)
        }
    }

    var currentlyEditing: EditText? = null

    init {
        this.mContext = mPage.context
        this.listView = listView
        this.dataSet = dataSet
        this.fm = fm
        this.submission = submission
        hidden = HashSet()
        currentComments = dataSet.comments
        if (currentComments != null) {
            for (i in currentComments!!.indices) {
                keys[currentComments!![i]!!.comment!!.comment.id.id] = i
            }
        }
        hiddenPersons = ArrayList()
        toCollapse = ArrayList()
        shifted = 0

        // As per reddit API gids: 0=silver, 1=gold, 2=platinum
        awardIcons = arrayOf(
            BitmapFactory.decodeResource(mContext!!.resources, R.drawable.silver),
            BitmapFactory.decodeResource(mContext!!.resources, R.drawable.gold),
            BitmapFactory.decodeResource(mContext!!.resources, R.drawable.platinum)
        )
    }

    fun resetMenu(v: LinearLayout, collapsed: Boolean) {
        v.removeAllViews()
        val params = v.layoutParams as RelativeLayout.LayoutParams
        if (collapsed) {
            params.height = 0
        } else {
            params.height = RelativeLayout.LayoutParams.WRAP_CONTENT
        }
        v.layoutParams = params
    }

    fun setCommentStateUnhighlighted(
        holder: CommentViewHolder,
        baseNode: CommentView?, animate: Boolean
    ) {
        if (animate) {
            collapseAndRemove(holder.menuArea)
        } else {
            resetMenu(holder.menuArea, true)
        }
        var color: Int
        val c = baseNode!!
        if (lastSeen != 0L
            && lastSeen < c.comment.published.toInstant(UtcOffset.ZERO).toEpochMilliseconds()
            && !dataSet.single
            && commentLastVisit
            && Authentication.name != c.creator.name
        ) {
            color = Palette.getColor(baseNode.community.name)
            color = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
        } else {
            val typedValue = TypedValue()
            val theme = mContext!!.theme
            theme.resolveAttribute(R.attr.card_background, typedValue, true)
            color = typedValue.data
        }
        val dwidth = (3 * Resources.getSystem().displayMetrics.density).toInt()
        var width = 0

        //Padding on the left, starting with the third comment
        for (i in 2 until baseNode.comment.depth) {
            width += dwidth
        }
        val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
        params.setMargins(width, 0, 0, 0)
        holder.itemView.layoutParams = params
        holder.itemView.findViewById<View>(R.id.background).setBackgroundColor(color)
    }

    fun setCommentStateUnhighlighted(
        holder: CommentViewHolder, comment: CommentView,
        baseNode: CommentView?, animate: Boolean
    ) {
        if (currentlyEditing != null && !currentlyEditing!!.text.toString()
                .isEmpty() && holder.bindingAdapterPosition <= editingPosition
        ) {
            AlertDialog.Builder(mContext!!)
                .setTitle(R.string.discard_comment_title)
                .setMessage(R.string.comment_discard_msg)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    currentlyEditing = null
                    editingPosition = -1
                    if (SettingValues.fastscroll) {
                        mPage.fastScroll!!.visibility = View.VISIBLE
                    }
                    if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                    mPage.overrideFab = false
                    currentlyEditingId = 0
                    backedText = ""
                    val view = mPage.requireActivity().findViewById<View>(android.R.id.content)
                    if (view != null) {
                        KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                    }
                    if (mContext is BaseActivity) {
                        (mContext as BaseActivity).shareUrl =
                            "https://reddit.com" + submission!!.permalink
                    }
                    setCommentStateUnhighlighted(holder, comment, baseNode, true)
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        } else {
            if (mContext is BaseActivity) {
                (mContext as BaseActivity).shareUrl = "https://freddit.com" + submission!!.permalink
            }
            currentlySelected = null
            currentSelectedItem = 0
            if (animate) {
                collapseAndRemove(holder.menuArea)
            } else {
                resetMenu(holder.menuArea, true)
            }
            val dwidth = (3 * Resources.getSystem().displayMetrics.density).toInt()
            var width = 0

            //Padding on the left, starting with the third comment
            for (i in 2 until baseNode!!.comment.depth) {
                width += dwidth
            }
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.setMargins(width, 0, 0, 0)
            holder.itemView.layoutParams = params
            val typedValue = TypedValue()
            val theme = mContext!!.theme
            theme.resolveAttribute(R.attr.card_background, typedValue, true)
            val color = typedValue.data
            holder.itemView.findViewById<View>(R.id.background).setBackgroundColor(color)
        }
    }

    fun doLongClick(
        holder: CommentViewHolder, comment: CommentView?,
        baseNode: CommentView?
    ) {
        if (currentlyEditing != null && !currentlyEditing!!.text.toString().isEmpty()) {
            AlertDialog.Builder(mContext!!)
                .setTitle(R.string.discard_comment_title)
                .setMessage(R.string.comment_discard_msg)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    currentlyEditing = null
                    editingPosition = -1
                    if (SettingValues.fastscroll) {
                        mPage.fastScroll!!.visibility = View.VISIBLE
                    }
                    if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                    mPage.overrideFab = false
                    currentlyEditingId = 0
                    backedText = ""
                    val view = mPage.requireActivity().findViewById<View>(android.R.id.content)
                    if (view != null) {
                        KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                    }
                    doLongClick(holder, comment, baseNode)
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        } else {
            if (currentSelectedItem != null && currentSelectedItem!! == comment!!.comment.id.id) {
                setCommentStateUnhighlighted(holder, comment, baseNode, true)
            } else {
                doHighlighted(holder, comment, baseNode, true)
            }
        }
    }

    fun doOnClick(holder: CommentViewHolder, comment: CommentView?, baseNode: CommentView?) {
        if (currentSelectedItem != null && currentSelectedItem!! == comment!!.comment.id.id) {
            if (SettingValues.swap) {
                //If the comment is highlighted and the user is long pressing the comment,
                //hide the comment.
                doOnClick(holder, baseNode, comment)
            }
            setCommentStateUnhighlighted(holder, comment, baseNode, true)
        } else {
            doOnClick(holder, baseNode, comment, null)
        }
    }

    fun doOnClick(holder: CommentViewHolder, baseNode: CommentView?, comment: CommentView?, TODO: Nothing?) {
        if (currentlyEditing != null && !currentlyEditing!!.text.toString()
                .isEmpty() && holder.bindingAdapterPosition <= editingPosition
        ) {
            AlertDialog.Builder(mContext!!)
                .setTitle(R.string.discard_comment_title)
                .setMessage(R.string.comment_discard_msg)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    currentlyEditing = null
                    editingPosition = -1
                    if (SettingValues.fastscroll) {
                        mPage.fastScroll!!.visibility = View.VISIBLE
                    }
                    if (mPage.fab != null) mPage.fab!!.visibility = View.VISIBLE
                    mPage.overrideFab = false
                    currentlyEditingId = 0
                    backedText = ""
                    val view = mPage.requireActivity().findViewById<View>(android.R.id.content)
                    if (view != null) {
                        KeyboardUtil.hideKeyboard(mContext, view.windowToken, 0)
                    }
                    doOnClick(holder, baseNode, comment)
                }
                .setNegativeButton(R.string.btn_no, null)
                .show()
        } else {
            if (isClicking) {
                isClicking = false
                resetMenu(holder.menuArea, true)
                isHolder!!.itemView.findViewById<View>(R.id.menu).visibility = View.GONE
            } else {
                if (hiddenPersons.contains(comment!!.comment.id.id)) {
                    hiddenPersons.remove(comment.comment.id.id)
                    unhideAll(baseNode, holder.bindingAdapterPosition + 1)
                    if (toCollapse.contains(comment.comment.id.id)
                        && SettingValues.collapseComments
                    ) {
                        setViews(
                            comment.comment.contentHtml,
                            submission!!.groupName, holder
                        )
                    }
                    CommentAdapterHelper.hideChildrenObject(holder.childrenNumber)
                    if (!holder.firstTextView.text.toString().isEmpty()) {
                        holder.firstTextView.visibility = View.VISIBLE
                    } else {
                        holder.firstTextView.visibility = View.GONE
                    }
                    holder.commentOverflow.visibility = View.VISIBLE
                    toCollapse.remove(comment.comment.id.id)
                } else {
                    val childNumber = getChildNumber(baseNode)
                    if (childNumber > 0) {
                        hideAll(baseNode, holder.bindingAdapterPosition + 1)
                        if (!hiddenPersons.contains(comment.comment.id.id)) {
                            hiddenPersons.add(comment.comment.id.id)
                        }
                        if (childNumber > 0) {
                            CommentAdapterHelper.showChildrenObject(holder.childrenNumber)
                            holder.childrenNumber.text = "+$childNumber"
                        }
                    } else {
                        if (!SettingValues.collapseComments) {
                            doLongClick(holder, comment, baseNode)
                        }
                    }
                    toCollapse.add(comment.comment.id.id)
                    if ((holder.firstTextView.visibility == View.VISIBLE
                                || holder.commentOverflow.visibility == View.VISIBLE)
                        && SettingValues.collapseComments
                    ) {
                        holder.firstTextView.visibility = View.GONE
                        holder.commentOverflow.visibility = View.GONE
                    } else if (SettingValues.collapseComments) {
                        if (!holder.firstTextView.text.toString().isEmpty()) {
                            holder.firstTextView.visibility = View.VISIBLE
                        } else {
                            holder.firstTextView.visibility = View.GONE
                        }
                        holder.commentOverflow.visibility = View.VISIBLE
                    }
                }
                clickpos = holder.bindingAdapterPosition + 1
            }
        }
    }

    private fun getChildNumber(user: CommentView?): Int {
        var i = 0
        /*
        for (ignored in user!!.walkTree()) {
            i++
            if (ignored.hasMoreComments() && dataSet.online) {
                i++
            }
        }
         */
        return i - 1
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        position -= if (position == 0 ||
            (currentComments != null && currentComments!!.isNotEmpty() && position == currentComments!!.size - hidden.size + 2) ||
            (currentComments != null) && currentComments!!.isEmpty() && position == 2
        ) {
            return SPACER
        } else {
            1
        }
        if (position == 0) {
            return HEADER
        }
        return if (currentComments!![getRealPosition(position - 1)] is CommentItem) 2 else 3
    }

    override fun getItemCount(): Int {
        return if (currentComments == null) {
            2
        } else {
            3 + (currentComments!!.size - hidden.size)
        }
    }

    fun unhideAll(n: CommentView?, i: Int) {
        try {
            val counter = unhideNumber(n, 0)
            if (SettingValues.collapseComments) {
                listView.itemAnimator = null
            } else {
                try {
                    listView.itemAnimator = AlphaInAnimator()
                } catch (ignored: Exception) {
                }
            }
            notifyItemRangeInserted(i, counter)
        } catch (ignored: Exception) {
        }
    }

    fun unhideAll(n: CommentView?) {
        unhideNumber(n, 0)
        if (SettingValues.collapseComments) {
            listView.itemAnimator = null
        } else {
            listView.itemAnimator = AlphaInAnimator()
        }
        notifyDataSetChanged()
    }

    fun hideAll(n: CommentView?) {
        hideNumber(n, 0)
        if (SettingValues.collapseComments) {
            listView.itemAnimator = null
        } else {
            listView.itemAnimator = AlphaInAnimator()
        }
        notifyDataSetChanged()
    }

    fun hideAll(n: CommentView?, i: Int) {
        val counter = hideNumber(n, 0)
        if (SettingValues.collapseComments) {
            listView.itemAnimator = null
        } else {
            listView.itemAnimator = AlphaInAnimator()
        }
        notifyItemRangeRemoved(i, counter)
    }

    fun parentHidden(n: CommentView?): Boolean {
        /*
        var n = n!!.parent
        while (n != null && n.comment.depth > 0) {
            val name = n.comment.id.id
            if (hiddenPersons.contains(name) || hidden.contains(name)) {
                return true
            }
            n = n.parent
        }
         */
        return false
    }

    fun unhideNumber(n: CommentView?, i: Int): Int {
        var i = i
        /*
        for (ignored in n!!.children) {
            if (ignored.comment.id.id != n.permalink) {
                val parentHidden = parentHidden(ignored)
                if (parentHidden) {
                    continue
                }
                var name = ignored.comment.id.id
                if (hidden.contains(name) || hiddenPersons.contains(name)) {
                    hidden.remove(name)
                    i++
                    if (ignored.hasMoreComments()
                        && !hiddenPersons.contains(name)
                        && dataSet.online
                    ) {
                        name = name + "more"
                        if (hidden.contains(name)) {
                            hidden.remove(name)
                            toCollapse.remove(name)
                            i++
                        }
                    }
                }
                i += unhideNumber(ignored, 0)
            }
        }
        if (n.hasMoreComments() && !parentHidden(n) && !hiddenPersons.contains(
                n.comment.id.id
            ) && dataSet.online
        ) {
            val fullname = n.comment.id.id + "more"
            if (hidden.contains(fullname)) {
                i++
                hidden.remove(fullname)
            }
        }
         */
        return i
    }

    fun hideNumber(n: CommentView?, i: Int): Int {
        var i = i
        /*
        for (ignored in n!!.children) {
            if (ignored.comment.id.id != n.permalink) {
                var fullname = ignored.comment.id.id
                if (!hidden.contains(fullname)) {
                    i++
                    hidden.add(fullname)
                }
                if (ignored.hasMoreComments() && dataSet.online) {
                    if (currentLoading != null && currentLoading!!.fullname == fullname) {
                        currentLoading!!.cancel(true)
                    }
                    fullname = fullname + "more"
                    if (!hidden.contains(fullname)) {
                        i++
                        hidden.add(fullname)
                    }
                }
                i += hideNumber(ignored, 0)
            }
        }
        if (n.hasMoreComments() && dataSet.online) {
            val fullname = -n.comment.id.id
            if (!hidden.contains(fullname)) {
                i++
                hidden.add(fullname)
            }
        }
         */
        return i
    }

    fun getParents(comment: CommentView?): Array<String?> {
        val bodies = arrayOfNulls<String>(comment!!.comment.depth + 1)
        bodies[0] = comment.creator.name
        return emptyArray()/*
        var parent = comment.parent
        var index = 1
        while (parent != null) {
            bodies[index] = parent.comment.creator.name
            index++
            parent = parent.parent
        }
        bodies[index - 1] = submission!!.creator.name

        //Reverse the array so IPost > Author > ... > Current OP
        for (i in 0 until bodies.size / 2) {
            val temp = bodies[i]
            bodies[i] = bodies[bodies.size - i - 1]
            bodies[bodies.size - i - 1] = temp
        }
        return bodies
        */
    }

    fun getRealPosition(position: Int): Int {
        val hElements = getHiddenCountUpTo(position)
        var diff = 0
        var i = 0
        while (i < hElements) {
            diff++
            if (currentComments!!.size > position + diff && hidden.contains(
                    currentComments!![position + diff]!!.comment!!.comment.id.id
                )
            ) {
                i--
            }
            i++
        }
        return position + diff
    }

    private fun getHiddenCountUpTo(location: Int): Int {
        var count = 0
        var i = 0
        while (i <= location && i < currentComments!!.size) {
            if (currentComments!!.size > i && hidden.contains(currentComments!![i]!!.comment!!.comment.id.id)) {
                count++
            }
            i++
        }
        return count
    }

    inner class AsyncLoadMore(
        var position: Int, var holderPos: Int, var holder: MoreCommentViewHolder, var dataPos: Int,
        var fullname: String
    ) : AsyncTask<MoreChildItem?, Void?, Int?>() {
        public override fun onPostExecute(data: Int?) {
            var data = data
            currentLoading = null
            if (!isCancelled && data != null) {
                shifted += data
                mPage.requireActivity().runOnUiThread {
                    currentComments!!.removeAt(position)
                    notifyItemRemoved(holderPos)
                }
                val oldSize = currentComments!!.size
                currentComments!!.addAll(position, finalData!!)
                val newSize = currentComments!!.size
                for (i2 in currentComments!!.indices) {
                    keys[currentComments!![i2]!!.comment!!.comment.id.id] = i2
                }
                data = newSize - oldSize
                listView.itemAnimator = SlideRightAlphaAnimator()
                notifyItemRangeInserted(holderPos, data)
                currentPos = holderPos
                toShiftTo =
                    (listView.layoutManager as LinearLayoutManager?)!!.findLastVisibleItemPosition()
                shiftFrom =
                    (listView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
            } else if (data == null && currentComments!![dataPos] is MoreChildItem) {
                val baseNode = currentComments!![dataPos] as MoreChildItem?
                if (baseNode!!.children.count > 0) {
                    holder.content.text = mContext!!.getString(
                        R.string.comment_load_more,
                        baseNode.children.count
                    )
                } else if (!baseNode.children.childrenIds.isEmpty()) {
                    holder.content.setText(R.string.comment_load_more_number_unknown)
                } else {
                    holder.content.setText(R.string.thread_continue)
                }
                holder.loading.visibility = View.GONE
            }
        }

        var finalData: ArrayList<CommentObject?>? = null
        override fun doInBackground(vararg params: MoreChildItem?): Int? {
            finalData = ArrayList()
            var i = 0
            if (params.size > 0) {
                try {
                    /*
                    val node: CommentView? = params[0].comment
                    node!!.loadMoreComments(Authentication.reddit)
                    val waiting = HashMap<Int, MoreChildItem>()
                    for (n in node.walkTree()) {
                        if (!keys.containsKey(n.comment.id.id)) {
                            val obj: CommentObject = CommentItem(n)
                            val removed = ArrayList<Int>()
                            val map: MutableMap<Int, MoreChildItem> =
                                TreeMap(Collections.reverseOrder())
                            map.putAll(waiting)
                            for (i2 in map.keys) {
                                if (i2 >= n.depth) {
                                    finalData!!.add(waiting[i2])
                                    removed.add(i2)
                                    waiting.remove(i2)
                                    i++
                                }
                            }
                            finalData!!.add(obj)
                            i++
                            if (n.hasMoreComments()) {
                                waiting[n.depth] = MoreChildItem(n, n.moreChildren)
                            }
                        }
                    }
                    if (node.hasMoreComments()) {
                        finalData!!.add(MoreChildItem(node, node.moreChildren))
                        i++
                    }
                     */ throw Exception("TODO")
                } catch (e: Exception) {
                    Log.w(LogUtil.getTag(), "Cannot load more comments $e")
                    val writer: Writer = StringWriter()
                    val printWriter = PrintWriter(writer)
                    e.printStackTrace(printWriter)
                    val stacktrace = writer.toString().replace(";", ",")
                    if (stacktrace.contains("UnknownHostException") || stacktrace.contains(
                            "SocketTimeoutException"
                        ) || stacktrace.contains("ConnectException")
                    ) {
                        //is offline
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(mContext!!)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_connection_failed_msg)
                                    .setNegativeButton(R.string.btn_ok, null)
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    } else if (stacktrace.contains("403 Forbidden") || stacktrace.contains(
                            "401 Unauthorized"
                        )
                    ) {
                        //Un-authenticated
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(mContext!!)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_refused_request_msg)
                                    .setNegativeButton(R.string.btn_no, null)
                                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                                        App.authentication!!.updateToken(
                                            mContext!!
                                        )
                                    }
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    } else if (stacktrace.contains("404 Not Found") || stacktrace.contains(
                            "400 Bad Request"
                        )
                    ) {
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post {
                            try {
                                AlertDialog.Builder(mContext!!).setTitle(R.string.err_title)
                                    .setMessage(R.string.err_could_not_find_content_msg)
                                    .setNegativeButton(R.string.btn_close, null)
                                    .show()
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                    return null
                }
            }
            return i
        }
    }

    inner class AsyncForceLoadChild(var position: Int, var holderPos: Int, var node: CommentView?) :
        AsyncTask<String?, Void?, Int>() {
        public override fun onPostExecute(data: Int) {
            if (data != -1) {
                listView.itemAnimator = SlideRightAlphaAnimator()
                notifyItemInserted(holderPos + 1)
                currentPos = holderPos + 1
                toShiftTo =
                    (listView.layoutManager as LinearLayoutManager?)!!.findLastVisibleItemPosition()
                shiftFrom =
                    (listView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
                dataSet.refreshLayout.isRefreshing = false
            } else {
                //Comment could not be found, force a reload
                val handler2 = Handler()
                handler2.postDelayed({
                    mPage.requireActivity().runOnUiThread {
                        dataSet.refreshLayout.isRefreshing = false
                        dataSet.loadMoreReply(this@CommentAdapter)
                    }
                }, 2000)
            }
        }

        override fun doInBackground(vararg params: String?): Int {
            var i = 0
            if (params.isNotEmpty()) {
                try {
                    /*
                    node!!.insertComment(Authentication.reddit, "t1_" + params[0])
                    for (n in node!!.walkTree()) {
                        if (n.comment.id.id.contains(params[0]!!)) {
                            currentComments!!.add(position, CommentItem(n!!))
                            i++
                        }
                    }
                     */ throw Exception("TODO")
                } catch (e: Exception) {
                    Log.w(LogUtil.getTag(), "Cannot load more comments $e")
                    i = -1
                }
                shifted += i
                if (currentComments != null) {
                    for (i2 in currentComments!!.indices) {
                        keys[currentComments!![i2]!!.comment!!.comment.id.id] = i2
                    }
                } else {
                    i = -1
                }
            }
            return i
        }
    }

    fun editComment(n: CommentView?, holder: CommentViewHolder) {
        if (n == null) {
            dataSet.loadMoreReply(this)
        } else {
            val position = getRealPosition(holder.bindingAdapterPosition - 1)
            val holderpos = holder.bindingAdapterPosition
            currentComments!!.removeAt(position - 1)
            currentComments!!.add(position - 1, CommentItem(n))
            listView.itemAnimator = SlideRightAlphaAnimator()
            mPage.requireActivity().runOnUiThread { notifyItemChanged(holderpos) }
        }
    }

    inner class ReplyTaskComment : AsyncTask<String?, Void?, String?> {
        var sub: Contribution?
        var node: CommentView? = null
        var holder: CommentViewHolder? = null
        var isSubmission = false
        var profileName: String?

        constructor(
            n: Contribution?, node: CommentView?, holder: CommentViewHolder?,
            profileName: String?
        ) {
            sub = n
            this.holder = holder
            this.node = node
            this.profileName = profileName
        }

        constructor(n: Contribution?, profileName: String?) {
            sub = n
            isSubmission = true
            this.profileName = profileName
        }

        public override fun onPostExecute(s: String?) {
            if (s == null || s.isEmpty()) {
                if (commentBack != null && !commentBack!!.isEmpty()) {
                    Drafts.addDraft(commentBack)
                    try {
                        AlertDialog.Builder(mContext!!)
                            .setTitle(R.string.err_comment_post)
                            .setMessage(
                                (if (why == null) "" else mContext!!.getString(
                                    R.string.err_comment_post_reason,
                                    why
                                ))
                                        + mContext!!.getString(R.string.err_comment_post_message)
                            )
                            .setPositiveButton(R.string.btn_ok, null)
                            .show()
                    } catch (ignored: Exception) {
                    }
                } else {
                    try {
                        AlertDialog.Builder(mContext!!)
                            .setTitle(R.string.err_comment_post)
                            .setMessage(
                                (if (why == null) "" else mContext!!.getString(
                                    R.string.err_comment_post_reason,
                                    why
                                ))
                                        + mContext!!.getString(R.string.err_comment_post_nosave_message)
                            )
                            .setPositiveButton(R.string.btn_ok, null)
                            .show()
                    } catch (ignored: Exception) {
                    }
                }
            } else {
                if (isSubmission) {
                    //AsyncForceLoadChild(0, 0, submission!!.comments!!).execute(s)
                } else {
                    AsyncForceLoadChild(
                        getRealPosition(holder!!.bindingAdapterPosition - 1),
                        holder!!.bindingAdapterPosition, node
                    ).execute(s)
                }
            }
        }

        var why: String? = null
        var commentBack: String? = null
        override fun doInBackground(vararg comment: String?): String? {
            return if (Authentication.me != null) {
                try {
                    commentBack = comment[0]
                    if (profileName == Authentication.name) {
                        AccountManager(Authentication.reddit).reply(sub, comment[0])
                    } else {
                        LogUtil.v("Switching to $profileName")
                        AccountManager(getAuthenticatedClient(profileName)).reply(
                            sub,
                            comment[0]
                        )
                    }
                } catch (e: Exception) {
                    if (e is ApiException) {
                        why = e.explanation
                    }
                    null
                }
            } else {
                null
            }
        }
    }

    private fun getAuthenticatedClient(profileName: String?): RedditClient {
        val token: String
        val reddit = RedditClient(
            UserAgent.of("android:ltd.ucode.slide:v" + BuildConfig.VERSION_NAME)
        )
        val accounts = HashMap<String?, String>()
        for (s in authentication.getStringSet(
            "accounts",
            HashSet<String>()
        )!!) {
            if (s.contains(":")) {
                accounts[s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]] =
                    s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
            } else {
                accounts[s] = ""
            }
        }
        val keys = ArrayList(accounts.keys)
        if (accounts.containsKey(profileName) && accounts[profileName]!!.isNotEmpty()) {
            token = accounts[profileName]!!
        } else {
            val tokens = ArrayList(
                authentication.getStringSet("tokens", HashSet())
            )
            var index = keys.indexOf(profileName)
            if (keys.indexOf(profileName) > tokens.size) {
                index -= 1
            }
            token = tokens[index]
        }
        doVerify(token, null, true, mContext)
        return reddit
    }

    companion object {
        const val HEADER = 1
    }
}
