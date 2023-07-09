package me.ccrama.redditslide.SubmissionViews

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.cocosw.bottomsheet.BottomSheet
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.ucode.lemmy.data.LemmyPost
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.actionbarVisible
import ltd.ucode.slide.SingleVote
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.ActionStates.getVoteDirection
import me.ccrama.redditslide.ActionStates.isSaved
import me.ccrama.redditslide.ActionStates.setSaved
import me.ccrama.redditslide.ActionStates.setVoteDirection
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.AnimatorUtil
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.ClipboardUtil
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LinkUtil.openExternally
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.RoundedBackgroundSpan
import me.ccrama.redditslide.views.TitleTextView
import net.dean.jraw.ApiException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.Ruleset
import net.dean.jraw.models.SubredditRule
import net.dean.jraw.models.VoteDirection
import java.util.Arrays
import java.util.Locale

object PopulateShadowboxInfo {
    private val ComponentActivity.postRepository: PostRepository
        get() = when (this) {
            is MainActivity -> this.postRepository
            else -> throw IllegalArgumentException(this::class.simpleName)
        }

    private val ComponentActivity.commentRepository: CommentRepository
        get() = when (this) {
            is MainActivity -> this.commentRepository
            else -> throw IllegalArgumentException(this::class.simpleName)
        }

    @JvmStatic
    fun doActionbar(s: IPost?, rootView: View, c: ComponentActivity, extras: Boolean) {
        val title = rootView.findViewById<TextView>(R.id.title)
        val desc = rootView.findViewById<TextView>(R.id.desc)
        var distingush: String = ""
        if (s != null) {
            if (s.regalia == DistinguishedStatus.MODERATOR) distingush =
                "[M]" else if (s.regalia == DistinguishedStatus.ADMIN) distingush =
                "[A]"
            title.text = CompatUtil.fromHtml(s.title)
            val spacer = c.getString(R.string.submission_properties_seperator)
            val titleString = SpannableStringBuilder()
            val subreddit = SpannableStringBuilder(" /c/" + s.groupName + " ")
            val subname = s.groupName.lowercase()
            if ((SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor())) {
                subreddit.setSpan(
                    ForegroundColorSpan(Palette.getColor(subname)),
                    0,
                    subreddit.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                subreddit.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    subreddit.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            titleString.append(subreddit)
            titleString.append(distingush)
            titleString.append(spacer)
            titleString.append(TimeUtils.getTimeAgo(s.discovered.toEpochMilliseconds(), c))
            desc.text = titleString
            (rootView.findViewById<View>(R.id.comments) as TextView).text =
                String.format(Locale.getDefault(), "%d", s.comments)
            (rootView.findViewById<View>(R.id.score) as TextView).text =
                String.format(Locale.getDefault(), "%d", s.score)
            if (extras) {
                val downvotebutton = rootView.findViewById<ImageView>(R.id.downvote)
                val upvotebutton = rootView.findViewById<ImageView>(R.id.upvote)
                if (s.isArchived || s.isLocked) {
                    downvotebutton.visibility = View.GONE
                    upvotebutton.visibility = View.GONE
                } else if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (actionbarVisible && downvotebutton.visibility != View.VISIBLE) {
                        downvotebutton.visibility = View.VISIBLE
                        upvotebutton.visibility = View.VISIBLE
                    }
                    when (getVoteDirection(s)) {
                        VoteDirection.UPVOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                ContextCompat.getColor(c, R.color.md_orange_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                upvotebutton,
                                ContextCompat.getColor(c, R.color.md_orange_500)
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.BOLD
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d",
                                    (s.score + (if (((s.user.name == Authentication.name))) 0 else 1))
                                )
                            BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, Color.WHITE)
                        }

                        VoteDirection.DOWNVOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                ContextCompat.getColor(c, R.color.md_blue_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                downvotebutton,
                                ContextCompat.getColor(c, R.color.md_blue_500)
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.BOLD
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d",
                                    (s.score + (if (((s.user.name == Authentication.name))) 0 else -1))
                                )
                            BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, Color.WHITE)
                        }

                        VoteDirection.NO_VOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                (rootView.findViewById<View>(
                                    R.id.comments
                                ) as TextView).currentTextColor
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(), "%d", s.score
                                )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.NORMAL
                            )
                            val imageViewSet = Arrays.asList(downvotebutton, upvotebutton)
                            BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, Color.WHITE)
                        }
                    }
                }
                val save = rootView.findViewById<View>(R.id.save) as ImageView
                if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (isSaved(s)) {
                        BlendModeUtil.tintImageViewAsSrcAtop(
                            save,
                            ContextCompat.getColor(c, R.color.md_amber_500)
                        )
                    } else {
                        BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE)
                    }
                    save.setOnClickListener {
                        object : AsyncTask<Void?, Void?, Void?>() {
                            override fun doInBackground(vararg params: Void?): Void? {
                                try {
                                    if (isSaved(s)) {
                                        //AccountManager(Authentication.reddit).unsave(s)
                                        setSaved(s, false)
                                    } else {
                                        //AccountManager(Authentication.reddit).save(s)
                                        setSaved(s, true)
                                    }
                                } catch (e: ApiException) {
                                    e.printStackTrace()
                                }
                                return null
                            }

                            override fun onPostExecute(aVoid: Void?) {
                                (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                                    PanelState.COLLAPSED
                                if (isSaved(s)) {
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        save,
                                        ContextCompat.getColor(c, R.color.md_amber_500)
                                    )
                                    AnimatorUtil.setFlashAnimation(
                                        rootView,
                                        save,
                                        ContextCompat.getColor(c, R.color.md_amber_500)
                                    )
                                } else {
                                    BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE)
                                }
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                }
                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    save.visibility = View.GONE
                }
                try {
                    val points = rootView.findViewById<TextView>(R.id.score)
                    val comments = rootView.findViewById<TextView>(R.id.comments)
                    if (Authentication.isLoggedIn && Authentication.didOnline) {
                        run {
                            downvotebutton.setOnClickListener {
                                (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).setPanelState(
                                    PanelState.COLLAPSED
                                )
                                if (SettingValues.storeHistory) {
                                    if (!s.isNsfw || SettingValues.storeNSFWHistory) {
                                        HasSeen.addSeen(s.uri)
                                    }
                                }
                                if (getVoteDirection(s) != VoteDirection.DOWNVOTE) { //has not been downvoted
                                    points.setTextColor(
                                        ContextCompat.getColor(
                                            c,
                                            R.color.md_blue_500
                                        )
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500)
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        Color.WHITE
                                    )
                                    AnimatorUtil.setFlashAnimation(
                                        rootView,
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500)
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                        null,
                                        Typeface.BOLD
                                    )
                                    val downvoteScore: Int =
                                        if ((s.score == 0)) 0 else s.score - 1 //if a post is at 0 votes, keep it at 0 when downvoting
                                    (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                        String.format(Locale.getDefault(), "%d", downvoteScore)
                                    )
                                    //Vote(false, points, c).execute(s)
                                    votePost(c, c.postRepository, s, SingleVote.DOWNVOTE) {
                                        setVoteDirection(s, VoteDirection.DOWNVOTE)
                                    }
                                } else {
                                    points.setTextColor(comments.getCurrentTextColor())
                                    //Vote(points, c).execute(s)
                                    votePost(c, c.postRepository, s, SingleVote.NOVOTE) {
                                        (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                            null,
                                            Typeface.NORMAL
                                        )
                                        (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                            String.format(Locale.getDefault(), "%d", s.score)
                                        )
                                        setVoteDirection(s, VoteDirection.NO_VOTE)
                                        BlendModeUtil.tintImageViewAsSrcAtop(
                                            downvotebutton,
                                            Color.WHITE
                                        )
                                    }
                                }
                            }
                        }
                        run {
                            upvotebutton.setOnClickListener {
                                (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).setPanelState(
                                    PanelState.COLLAPSED
                                )
                                if (SettingValues.storeHistory) {
                                    if (!s.isNsfw || SettingValues.storeNSFWHistory) {
                                        HasSeen.addSeen(s.uri)
                                    }
                                }
                                if (getVoteDirection(s) != VoteDirection.UPVOTE) { //has not been upvoted
                                    points.setTextColor(
                                        ContextCompat.getColor(
                                            c,
                                            R.color.md_orange_500
                                        )
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500)
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        Color.WHITE
                                    )
                                    AnimatorUtil.setFlashAnimation(
                                        rootView,
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500)
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                        null,
                                        Typeface.BOLD
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                        String.format(
                                            Locale.getDefault(),
                                            "%d",
                                            s.score + 1
                                        )
                                    )
                                    //Vote(true, points, c).execute(s)
                                    votePost(c, c.postRepository, s, SingleVote.UPVOTE) {
                                        setVoteDirection(s, VoteDirection.UPVOTE)
                                    }
                                } else {
                                    points.setTextColor(comments.getCurrentTextColor())
                                    //Vote(points, c).execute(s)
                                    votePost(c, c.postRepository, s, SingleVote.NOVOTE) {
                                        (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                            null,
                                            Typeface.NORMAL
                                        )
                                        (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                            String.format(Locale.getDefault(), "%d", s.score)
                                        )
                                        setVoteDirection(s, VoteDirection.NO_VOTE)
                                        BlendModeUtil.tintImageViewAsSrcAtop(
                                            upvotebutton,
                                            Color.WHITE
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        upvotebutton.visibility = View.GONE
                        downvotebutton.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                    ignored.printStackTrace()
                }
                rootView.findViewById<View>(R.id.menu)
                    .setOnClickListener { showBottomSheet(c, s, rootView) }
            }
        }
    }

    @JvmStatic
    fun doActionbar(node: CommentNode, rootView: View, c: ComponentActivity, extras: Boolean) {
        val s = node.comment
        val title = rootView.findViewById<TitleTextView>(R.id.title)
        val desc = rootView.findViewById<TextView>(R.id.desc)
        var distingush: String = ""
        if (s != null) {
            if (s.distinguishedStatus == DistinguishedStatus.MODERATOR) distingush =
                "[M]" else if (s.distinguishedStatus == DistinguishedStatus.ADMIN) distingush =
                "[A]"
            val commentTitle = SpannableStringBuilder()
            val level = SpannableStringBuilder()
            if (!node.isTopLevel) {
                level.append("[").append(node.depth.toString()).append("] ")
                level.setSpan(
                    RelativeSizeSpan(0.7f), 0, level.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                commentTitle.append(level)
            }
            commentTitle.append(
                CompatUtil.fromHtml(
                    s.dataNode["body_html"].asText().trim { it <= ' ' })
            )
            title.setTextHtml(commentTitle)
            title.maxLines = 3
            val spacer = c.getString(R.string.submission_properties_seperator)
            val titleString = SpannableStringBuilder()
            val author = SpannableStringBuilder(" /u/" + s.author + " ")
            val authorcolor = Palette.getFontColorUser(s.author)
            if (Authentication.name != null && (s.author.lowercase() == Authentication.name!!.lowercase())) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        c,
                        android.R.color.white,
                        R.color.md_deep_orange_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (s.distinguishedStatus == DistinguishedStatus.MODERATOR || s.distinguishedStatus == DistinguishedStatus.ADMIN) {
                author.setSpan(
                    RoundedBackgroundSpan(
                        c,
                        android.R.color.white,
                        R.color.md_green_300,
                        false
                    ), 0, author.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (authorcolor != 0) {
                author.setSpan(
                    ForegroundColorSpan(authorcolor),
                    0,
                    author.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            titleString.append(author)
            titleString.append(distingush)
            titleString.append(spacer)
            titleString.append(TimeUtils.getTimeAgo(s.created.time, c))
            desc.text = titleString
            (rootView.findViewById<View>(R.id.score) as TextView).text =
                String.format(Locale.getDefault(), "%d", s.score)
            if (extras) {
                val downvotebutton = rootView.findViewById<ImageView>(R.id.downvote)
                val upvotebutton = rootView.findViewById<ImageView>(R.id.upvote)
                if (s.isArchived) {
                    downvotebutton.visibility = View.GONE
                    upvotebutton.visibility = View.GONE
                } else if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (actionbarVisible && downvotebutton.visibility != View.VISIBLE) {
                        downvotebutton.visibility = View.VISIBLE
                        upvotebutton.visibility = View.VISIBLE
                    }
                    when (getVoteDirection(s)) {
                        VoteDirection.UPVOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                ContextCompat.getColor(c, R.color.md_orange_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                upvotebutton,
                                ContextCompat.getColor(c, R.color.md_orange_500)
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.BOLD
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d",
                                    (s.score + (if (((s.author == Authentication.name))) 0 else 1))
                                )
                            BlendModeUtil.tintImageViewAsSrcAtop(downvotebutton, Color.WHITE)
                        }

                        VoteDirection.DOWNVOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                ContextCompat.getColor(c, R.color.md_blue_500)
                            )
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                downvotebutton,
                                ContextCompat.getColor(c, R.color.md_blue_500)
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.BOLD
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d",
                                    (s.score + (if (((s.author == Authentication.name))) 0 else -1))
                                )
                            BlendModeUtil.tintImageViewAsSrcAtop(upvotebutton, Color.WHITE)
                        }

                        VoteDirection.NO_VOTE -> {
                            (rootView.findViewById<View>(R.id.score) as TextView).setTextColor(
                                (rootView.findViewById<View>(
                                    R.id.comments
                                ) as TextView).currentTextColor
                            )
                            (rootView.findViewById<View>(R.id.score) as TextView).text =
                                String.format(
                                    Locale.getDefault(), "%d", s.score
                                )
                            (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                null,
                                Typeface.NORMAL
                            )
                            val imageViewSet = Arrays.asList(downvotebutton, upvotebutton)
                            BlendModeUtil.tintImageViewsAsSrcAtop(imageViewSet, Color.WHITE)
                        }
                    }
                }
                val save = rootView.findViewById<View>(R.id.save) as ImageView
                if (Authentication.isLoggedIn && Authentication.didOnline) {
                    if (isSaved(s)) {
                        BlendModeUtil.tintImageViewAsSrcAtop(
                            save,
                            ContextCompat.getColor(c, R.color.md_amber_500)
                        )
                    } else {
                        BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE)
                    }
                    save.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View) {
                            object : AsyncTask<Void?, Void?, Void?>() {
                                override fun doInBackground(vararg params: Void?): Void? {
                                    try {
                                        if (isSaved(s)) {
                                            AccountManager(Authentication.reddit).unsave(s)
                                            setSaved(s, false)
                                        } else {
                                            AccountManager(Authentication.reddit).save(s)
                                            setSaved(s, true)
                                        }
                                    } catch (e: ApiException) {
                                        e.printStackTrace()
                                    }
                                    return null
                                }

                                override fun onPostExecute(aVoid: Void?) {
                                    (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                                        PanelState.COLLAPSED
                                    if (isSaved(s)) {
                                        BlendModeUtil.tintImageViewAsSrcAtop(
                                            save,
                                            ContextCompat.getColor(c, R.color.md_amber_500)
                                        )
                                        AnimatorUtil.setFlashAnimation(
                                            rootView,
                                            save,
                                            ContextCompat.getColor(c, R.color.md_amber_500)
                                        )
                                    } else {
                                        BlendModeUtil.tintImageViewAsSrcAtop(save, Color.WHITE)
                                    }
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                        }
                    })
                }
                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    save.visibility = View.GONE
                }
                try {
                    val points = rootView.findViewById<TextView>(R.id.score)
                    val comments = rootView.findViewById<TextView>(R.id.comments)
                    if (Authentication.isLoggedIn && Authentication.didOnline) {
                        run {
                            downvotebutton.setOnClickListener {
                                (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).setPanelState(
                                    PanelState.COLLAPSED
                                )
                                if (getVoteDirection(s) != VoteDirection.DOWNVOTE) { //has not been downvoted
                                    points.setTextColor(
                                        ContextCompat.getColor(
                                            c,
                                            R.color.md_blue_500
                                        )
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500)
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        Color.WHITE
                                    )
                                    AnimatorUtil.setFlashAnimation(
                                        rootView,
                                        downvotebutton,
                                        ContextCompat.getColor(c, R.color.md_blue_500)
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                        null,
                                        Typeface.BOLD
                                    )
                                    val downvoteScore: Int =
                                        if ((s.getScore() == 0)) 0 else s.getScore() - 1 //if a post is at 0 votes, keep it at 0 when downvoting
                                    (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                        String.format(Locale.getDefault(), "%d", downvoteScore)
                                    )
                                    //Vote(false, points, c).execute(s)
                                    voteComment(c, c.commentRepository, s, SingleVote.DOWNVOTE) {
                                        setVoteDirection(s, VoteDirection.DOWNVOTE)
                                    }
                                } else {
                                    points.setTextColor(comments.getCurrentTextColor())
                                    //Vote(points, c).execute(s)
                                    voteComment(c, c.commentRepository, s, SingleVote.NOVOTE) {
                                        (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                            null,
                                            Typeface.NORMAL
                                        )
                                        (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                            String.format(
                                                Locale.getDefault(),
                                                "%d",
                                                s.getScore()
                                            )
                                        )
                                        setVoteDirection(s, VoteDirection.NO_VOTE)
                                        BlendModeUtil.tintImageViewAsSrcAtop(
                                            downvotebutton,
                                            Color.WHITE
                                        )
                                    }
                                }
                            }
                        }
                        run {
                            upvotebutton.setOnClickListener {
                                (rootView.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).setPanelState(
                                    PanelState.COLLAPSED
                                )
                                if (getVoteDirection(s) != VoteDirection.UPVOTE) { //has not been upvoted
                                    points.setTextColor(
                                        ContextCompat.getColor(
                                            c,
                                            R.color.md_orange_500
                                        )
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500)
                                    )
                                    BlendModeUtil.tintImageViewAsSrcAtop(
                                        downvotebutton,
                                        Color.WHITE
                                    )
                                    AnimatorUtil.setFlashAnimation(
                                        rootView,
                                        upvotebutton,
                                        ContextCompat.getColor(c, R.color.md_orange_500)
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                        null,
                                        Typeface.BOLD
                                    )
                                    (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                        String.format(
                                            Locale.getDefault(),
                                            "%d",
                                            s.getScore() + 1
                                        )
                                    )
                                    //Vote(true, points, c).execute(s)
                                    voteComment(c, c.commentRepository, s, SingleVote.UPVOTE) {
                                        setVoteDirection(s, VoteDirection.UPVOTE)
                                    }
                                } else {
                                    points.setTextColor(comments.getCurrentTextColor())
                                    //Vote(points, c).execute(s)
                                    voteComment(c, c.commentRepository, s, SingleVote.NOVOTE) {
                                        (rootView.findViewById<View>(R.id.score) as TextView).setTypeface(
                                            null,
                                            Typeface.NORMAL
                                        )
                                        (rootView.findViewById<View>(R.id.score) as TextView).setText(
                                            String.format(
                                                Locale.getDefault(),
                                                "%d",
                                                s.getScore()
                                            )
                                        )
                                        setVoteDirection(s, VoteDirection.NO_VOTE)
                                        BlendModeUtil.tintImageViewAsSrcAtop(
                                            upvotebutton,
                                            Color.WHITE
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        upvotebutton.visibility = View.GONE
                        downvotebutton.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                    ignored.printStackTrace()
                }
            }
        }
    }

    fun showBottomSheet(mContext: Activity, submission: IPost, rootView: View?) {
        val attrs = intArrayOf(R.attr.tintColor)
        val ta = mContext.obtainStyledAttributes(attrs)
        val color = ta.getColor(0, Color.WHITE)
        val profile = mContext.resources.getDrawable(R.drawable.ic_account_circle)
        val sub = mContext.resources.getDrawable(R.drawable.ic_bookmark_border)
        val report = mContext.resources.getDrawable(R.drawable.ic_report)
        val copy = mContext.resources.getDrawable(R.drawable.ic_content_copy)
        val open = mContext.resources.getDrawable(R.drawable.ic_open_in_browser)
        val link = mContext.resources.getDrawable(R.drawable.ic_link)
        val reddit = mContext.resources.getDrawable(R.drawable.ic_forum)
        val drawableSet = Arrays.asList(
            profile, sub, report, copy, open, link, reddit
        )
        BlendModeUtil.tintDrawablesAsSrcAtop(drawableSet, color)
        ta.recycle()
        val b = BottomSheet.Builder(mContext)
            .title(CompatUtil.fromHtml(submission.title))
        if (Authentication.didOnline) {
            b.sheet(1, profile, "/u/" + submission.user.name)
                .sheet(2, sub, "/c/" + submission.groupName)
            if (Authentication.isLoggedIn) {
                b.sheet(12, report, mContext.getString(R.string.btn_report))
            }
        }
        b.sheet(7, open, mContext.getString(R.string.open_externally))
            .sheet(4, link, mContext.getString(R.string.submission_share_permalink))
            .sheet(8, reddit, mContext.getString(R.string.submission_share_reddit_url))
            .listener(object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    when (which) {
                        1 -> {
                            val i = Intent(mContext, Profile::class.java)
                            i.putExtra(Profile.EXTRA_PROFILE, submission.user.name)
                            mContext.startActivity(i)
                        }

                        2 -> {
                            val i = Intent(mContext, SubredditView::class.java)
                            i.putExtra(SubredditView.EXTRA_SUBREDDIT, submission.groupName)
                            mContext.startActivityForResult(i, 14)
                        }

                        7 -> openExternally(submission.link!!)
                        4 -> defaultShareText(submission.title, submission.link, mContext)
                        12 -> {
                            val reportDialog = MaterialDialog(mContext).show {
                                customView(R.layout.report_dialog, scrollable = true)
                                title(R.string.report_post)
                                negativeButton(R.string.btn_cancel)
                                positiveButton(R.string.btn_report) { dialog ->
                                    val reasonGroup =
                                        dialog.view.findViewById<RadioGroup>(R.id.report_reasons)
                                    val reportReason: String = if (reasonGroup.checkedRadioButtonId == R.id.report_other) {
                                        (dialog.view
                                            .findViewById<View>(R.id.input_report_reason) as EditText)
                                            .text.toString()
                                    } else {
                                        (reasonGroup
                                            .findViewById<View>(reasonGroup.checkedRadioButtonId) as RadioButton)
                                            .text.toString()
                                    }
                                    PopulateBase.AsyncReportTask(submission, (rootView)!!)
                                        .executeOnExecutor(
                                            AsyncTask.THREAD_POOL_EXECUTOR,
                                            reportReason
                                        )
                                }
                            }
                            val reasonGroup =
                                reportDialog.view.findViewById<RadioGroup>(R.id.report_reasons)
                            reasonGroup.setOnCheckedChangeListener { group, checkedId ->
                                reportDialog.view.findViewById<View>(R.id.input_report_reason).visibility =
                                    if (checkedId == R.id.report_other) View.VISIBLE else View.GONE
                            }

                            // Load sub's report reasons and show the appropriate ones
                            object : AsyncTask<Void?, Void?, Ruleset>() {
                                override fun doInBackground(vararg voids: Void?): Ruleset {
                                    return Authentication.reddit!!.getRules(submission.groupName)
                                }

                                override fun onPostExecute(rules: Ruleset) {
                                    reportDialog.view.findViewById<View>(R.id.report_loading).visibility =
                                        View.GONE
                                    if (rules.subredditRules.size > 0) {
                                        val subHeader = TextView(mContext)
                                        subHeader.text = mContext.getString(
                                            R.string.report_sub_rules,
                                            submission.groupName
                                        )
                                        reasonGroup.addView(subHeader, reasonGroup.childCount - 2)
                                    }
                                    for (rule: SubredditRule in rules.subredditRules) {
                                        if ((rule.kind == SubredditRule.RuleKind.LINK
                                                    || rule.kind == SubredditRule.RuleKind.ALL)
                                        ) {
                                            val btn = RadioButton(mContext)
                                            btn.text = rule.violationReason
                                            reasonGroup.addView(btn, reasonGroup.childCount - 2)
                                            btn.layoutParams.width =
                                                WindowManager.LayoutParams.MATCH_PARENT
                                        }
                                    }
                                    if (rules.siteRules.size > 0) {
                                        val siteHeader = TextView(mContext)
                                        siteHeader.setText(R.string.report_site_rules)
                                        reasonGroup.addView(siteHeader, reasonGroup.childCount - 2)
                                    }
                                    for (rule: String? in rules.siteRules) {
                                        val btn = RadioButton(mContext)
                                        btn.text = rule
                                        reasonGroup.addView(btn, reasonGroup.childCount - 2)
                                        btn.layoutParams.width =
                                            WindowManager.LayoutParams.MATCH_PARENT
                                    }
                                }
                            }.execute()
                            reportDialog.show()
                        }

                        8 -> if (SettingValues.shareLongLink) {
                            defaultShareText(
                                submission.title,
                                submission.uri,
                                mContext
                            )
                        } else {
                            defaultShareText(
                                submission.title,
                                "https://redd.it/" + submission.postId,
                                mContext
                            )
                        }

                        6 -> {
                            ClipboardUtil.copyToClipboard(mContext, "Link", submission.link)
                            Toast.makeText(
                                mContext,
                                R.string.submission_link_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        b.show()
    }

    private fun votePost(context: LifecycleOwner, postRepository: PostRepository,
                         submission: IPost, direction: SingleVote,
                         andThen: suspend () -> Unit = {}) {
        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                postRepository.likePost(
                    (submission as LemmyPost).instance,
                    PostId(submission.id),
                    direction
                )
            }
            andThen()
        }
    }

    private fun voteComment(context: LifecycleOwner, commentRepository: CommentRepository,
                            comment: Comment, direction: SingleVote,
                            andThen: suspend () -> Unit = {}) {
        context.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                commentRepository.likeComment(
                    AccountRepository.currentAccount,
                    CommentId(comment.id.toInt()),
                    direction
                )
            }
            andThen()
        }
    }
}
