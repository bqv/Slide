package me.ccrama.redditslide.Adapters

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.cocosw.bottomsheet.BottomSheet
import com.devspark.robototextview.RobotoTypefaces
import com.google.android.material.snackbar.Snackbar
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.App.Companion.defaultShareText
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import me.ccrama.redditslide.ActionStates.getVoteDirection
import me.ccrama.redditslide.Activities.Profile
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.HasSeen.addSeen
import me.ccrama.redditslide.Hidden.setHidden
import me.ccrama.redditslide.Hidden.undoHidden
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SubmissionViews.PopulateSubmissionViewHolder
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.CompatUtil
import me.ccrama.redditslide.util.LayoutUtils.showSnackbar
import me.ccrama.redditslide.util.LinkUtil
import me.ccrama.redditslide.util.MiscUtil
import me.ccrama.redditslide.util.SubmissionParser
import me.ccrama.redditslide.util.TimeUtils
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.CreateCardView.CreateView
import me.ccrama.redditslide.views.CreateCardView.colorCard
import me.ccrama.redditslide.views.CreateCardView.resetColorCard
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Contribution
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import java.util.Locale

class ContributionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>, IFallibleAdapter {
    private val SPACER = 6
    @JvmField val mContext: ComponentActivity
    private val listView: RecyclerView
    private val isHiddenPost: Boolean
    var dataSet: GeneralPosts<Contribution>

    constructor(mContext: ComponentActivity, dataSet: GeneralPosts<Contribution>, listView: RecyclerView) {
        this.mContext = mContext
        this.listView = listView
        this.dataSet = dataSet
        isHiddenPost = false
    }

    constructor(mContext: ComponentActivity, dataSet: GeneralPosts<Contribution>, listView: RecyclerView, isHiddenPost: Boolean) {
        this.mContext = mContext
        this.listView = listView
        this.dataSet = dataSet
        this.isHiddenPost = isHiddenPost
    }

    private val LOADING_SPINNER = 5
    private val NO_MORE = 3
    override fun getItemViewType(position: Int): Int {
        var position = position
        if (position == 0 && !dataSet.posts.isEmpty()) {
            return SPACER
        } else if (!dataSet.posts.isEmpty()) {
            position -= 1
        }
        if (position == dataSet.posts.size && !dataSet.posts.isEmpty() && !dataSet.nomore) {
            return LOADING_SPINNER
        } else if (position == dataSet.posts.size && dataSet.nomore) {
            return NO_MORE
        }
        return if (dataSet.posts[position] is Comment) COMMENT else 2
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        return if (i == SPACER) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.spacer, viewGroup, false)
            SpacerViewHolder(v)
        } else if (i == COMMENT) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.profile_comment, viewGroup, false)
            ProfileCommentViewHolder(v)
        } else if (i == LOADING_SPINNER) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.loadingmore, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else if (i == NO_MORE) {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.nomoreposts, viewGroup, false)
            SubmissionFooterViewHolder(v)
        } else {
            val v = CreateView(viewGroup)
            SubmissionViewHolder(v)
        }
    }

    class SubmissionFooterViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    override fun onBindViewHolder(firstHolder: RecyclerView.ViewHolder, pos: Int) {
        val i = if (pos != 0) pos - 1 else pos
        if (firstHolder is SubmissionViewHolder) {
            val holder = firstHolder
            val submission = dataSet.posts[i] as Submission
            resetColorCard(holder.itemView)
            if (submission.subredditName != null) colorCard(submission.subredditName.lowercase(), holder.itemView, "no_subreddit", false)
            holder.itemView.setOnLongClickListener { v: View? ->
                val inflater = mContext.layoutInflater
                val dialoglayout = inflater.inflate(R.layout.postmenu, null)
                val title = dialoglayout.findViewById<TextView>(R.id.title)
                title.text = CompatUtil.fromHtml(submission.title)
                (dialoglayout.findViewById<View>(R.id.userpopup) as TextView).text = "/u/" + submission.author
                (dialoglayout.findViewById<View>(R.id.subpopup) as TextView).text = "/c/" + submission.subredditName
                dialoglayout.findViewById<View>(R.id.sidebar).setOnClickListener { v16: View? ->
                    val i13 = Intent(mContext, Profile::class.java)
                    i13.putExtra(Profile.EXTRA_PROFILE, submission.author)
                    mContext.startActivity(i13)
                }
                dialoglayout.findViewById<View>(R.id.wiki).setOnClickListener { v15: View? ->
                    val i12 = Intent(mContext, SubredditView::class.java)
                    i12.putExtra(SubredditView.EXTRA_SUBREDDIT, submission.subredditName)
                    mContext.startActivity(i12)
                }
                dialoglayout.findViewById<View>(R.id.save).setOnClickListener { v13: View? ->
                    if (submission.isSaved) {
                        (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(R.string.submission_save)
                    } else {
                        (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(R.string.submission_post_saved)
                    }
                    AsyncSave(mContext, firstHolder.itemView).execute(submission)
                }
                dialoglayout.findViewById<View>(R.id.copy).visibility = View.GONE
                if (submission.isSaved) {
                    (dialoglayout.findViewById<View>(R.id.savedtext) as TextView).setText(R.string.submission_post_saved)
                }
                dialoglayout.findViewById<View>(R.id.gild).setOnClickListener { v14: View? ->
                    val urlString = "https://reddit.com" + submission.permalink
                    val i1 = Intent(mContext, Website::class.java)
                    i1.putExtra(LinkUtil.EXTRA_URL, urlString)
                    mContext.startActivity(i1)
                }
                dialoglayout.findViewById<View>(R.id.share).setOnClickListener { v12: View? ->
                    if (submission.isSelfPost) {
                        if (SettingValues.shareLongLink) {
                            defaultShareText("", "https://reddit.com" + submission.permalink, mContext)
                        } else {
                            defaultShareText("", "https://redd.it/" + submission.id, mContext)
                        }
                    } else {
                        BottomSheet.Builder(mContext)
                            .title(R.string.submission_share_title)
                            .grid()
                            .sheet(R.menu.share_menu)
                            .listener { dialog: DialogInterface?, which: Int ->
                                when (which) {
                                    R.id.reddit_url -> if (SettingValues.shareLongLink) {
                                        defaultShareText(submission.title, "https://reddit.com" + submission.permalink, mContext)
                                    } else {
                                        defaultShareText(submission.title, "https://redd.it/" + submission.id, mContext)
                                    }

                                    R.id.link_url -> defaultShareText(submission.title, submission.url, mContext)
                                }
                            }.show()
                    }
                }
                if (!Authentication.isLoggedIn || !Authentication.didOnline) {
                    dialoglayout.findViewById<View>(R.id.save).visibility = View.GONE
                    dialoglayout.findViewById<View>(R.id.gild).visibility = View.GONE
                }
                title.setBackgroundColor(Palette.getColor(submission.subredditName))
                val builder = AlertDialog.Builder(mContext)
                    .setView(dialoglayout)
                val d: Dialog = builder.show()
                dialoglayout.findViewById<View>(R.id.hide).setOnClickListener { v1: View? ->
                    val pos12 = dataSet.posts.indexOf(submission)
                    val old = dataSet.posts[pos12]
                    dataSet.posts.remove(submission)
                    notifyItemRemoved(pos12 + 1)
                    d.dismiss()
                    setHidden(RedditSubmission((old as Submission)))
                    val s = Snackbar.make(listView, R.string.submission_info_hidden, Snackbar.LENGTH_LONG).setAction(R.string.btn_undo, View.OnClickListener {
                        dataSet.posts.add(pos12, old)
                        notifyItemInserted(pos12 + 1)
                        undoHidden(RedditSubmission(old))
                    })
                    showSnackbar(s)
                }
                true
            }
            PopulateSubmissionViewHolder(TODO(), TODO())
                .populateSubmissionViewHolder(holder, RedditSubmission(submission), mContext, false, false, mutableListOf(), listView, false, false, null, null)
            val hideButton = holder.itemView.findViewById<ImageView>(R.id.hide)
            if (hideButton != null && isHiddenPost) {
                hideButton.setOnClickListener { v: View? ->
                    val pos1 = dataSet.posts.indexOf(submission)
                    val old = dataSet.posts[pos1]
                    dataSet.posts.remove(submission)
                    notifyItemRemoved(pos1 + 1)
                    undoHidden(RedditSubmission((old as Submission)))
                }
            }
            holder.itemView.setOnClickListener { v: View? ->
                var url = "www.reddit.com" + submission.permalink
                url = url.replace("?ref=search_posts", "")
                OpenRedditLink.openUrl(mContext, url, true)
                if (SettingValues.storeHistory) {
                    if (SettingValues.storeNSFWHistory && submission.isNsfw || !submission.isNsfw) addSeen(submission.fullName)
                }
                notifyItemChanged(pos)
            }
        } else if (firstHolder is ProfileCommentViewHolder) {
            //IS COMMENT
            val holder = firstHolder
            val comment = dataSet.posts[i] as Comment
            val scoreText: String
            scoreText = if (comment.isScoreHidden) {
                "[" + mContext.getString(R.string.misc_score_hidden).uppercase(Locale.getDefault()) + "]"
            } else {
                String.format(Locale.getDefault(), "%d", comment.score)
            }
            var score = SpannableStringBuilder(scoreText)
            if (score == null || score.toString().isEmpty()) {
                score = SpannableStringBuilder("0")
            }
            if (!scoreText.contains("[")) {
                score.append(String.format(Locale.getDefault(), " %s", mContext.resources.getQuantityString(R.plurals.points, comment.score)))
            }
            holder.score.text = score
            if (Authentication.isLoggedIn) {
                if (getVoteDirection(comment) == VoteDirection.UPVOTE) {
                    holder.score.setTextColor(mContext.resources.getColor(R.color.md_orange_500))
                } else if (getVoteDirection(comment) == VoteDirection.DOWNVOTE) {
                    holder.score.setTextColor(mContext.resources.getColor(R.color.md_blue_500))
                } else {
                    holder.score.setTextColor(holder.time.currentTextColor)
                }
            }
            val spacer = mContext.getString(R.string.submission_properties_seperator)
            val titleString = SpannableStringBuilder()
            val timeAgo = TimeUtils.getTimeAgo(comment.created.time, mContext)
            var time = if (timeAgo == null || timeAgo.isEmpty()) "just now" else timeAgo //some users were crashing here
            time += if (comment.editDate != null) " (edit " + TimeUtils.getTimeAgo(comment.editDate.time, mContext) + ")" else ""
            titleString.append(time)
            titleString.append(spacer)
            if (comment.subredditName != null) {
                val subname = comment.subredditName
                val subreddit = SpannableStringBuilder("/c/$subname")
                if (SettingValues.colorSubName && Palette.getColor(subname) != Palette.getDefaultColor()) {
                    subreddit.setSpan(ForegroundColorSpan(Palette.getColor(subname)), 0, subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    subreddit.setSpan(StyleSpan(Typeface.BOLD), 0, subreddit.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                titleString.append(subreddit)
            }
            holder.time.text = titleString
            setViews(comment.dataNode["body_html"].asText(), comment.subredditName, holder)
            val type = FontPreferences(mContext).fontTypeComment.typeface
            val typeface: Typeface = if (type >= 0) {
                RobotoTypefaces.obtainTypeface(mContext, type!!)
            } else {
                Typeface.DEFAULT
            }
            holder.content.setTypeface(typeface)
            (holder.gild as TextView).text = ""
            if (!SettingValues.hideCommentAwards && (comment.timesSilvered > 0 || comment.timesGilded > 0 || comment.timesPlatinized > 0)) {
                val a = mContext.obtainStyledAttributes(
                    FontPreferences(mContext).postFontStyle.resId,
                    R.styleable.FontStyle)
                val fontsize = (a.getDimensionPixelSize(R.styleable.FontStyle_font_cardtitle, -1) * .75).toInt()
                a.recycle()
                holder.gild.setVisibility(View.VISIBLE)
                // Add silver, gold, platinum icons and counts in that order
                MiscUtil.addAwards(mContext, fontsize, holder, comment.timesSilvered, R.drawable.silver)
                MiscUtil.addAwards(mContext, fontsize, holder, comment.timesGilded, R.drawable.gold)
                MiscUtil.addAwards(mContext, fontsize, holder, comment.timesPlatinized, R.drawable.platinum)
            } else if (holder.gild.getVisibility() == View.VISIBLE) holder.gild.setVisibility(View.GONE)
            if (comment.submissionTitle != null) holder.title.text = CompatUtil.fromHtml(comment.submissionTitle) else holder.title.text = CompatUtil.fromHtml(comment.author)
            holder.itemView.setOnClickListener { v: View? -> OpenRedditLink.openUrl(mContext, comment.submissionId, comment.subredditName, comment.id) }
            holder.content.setOnClickListener { v: View? -> OpenRedditLink.openUrl(mContext, comment.submissionId, comment.subredditName, comment.id) }
        } else if (firstHolder is SpacerViewHolder) {
            firstHolder.itemView.layoutParams = LinearLayout.LayoutParams(firstHolder.itemView.width, mContext.findViewById<View>(R.id.header).height)
            if (listView.layoutManager is CatchStaggeredGridLayoutManager) {
                val layoutParams = StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mContext.findViewById<View>(R.id.header).height)
                layoutParams.isFullSpan = true
                firstHolder.itemView.layoutParams = layoutParams
            }
        }
    }

    class SpacerViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

    private fun setViews(rawHTML: String, subredditName: String, holder: ProfileCommentViewHolder) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks[0] != "<div class=\"md\">") {
            holder.content.visibility = View.VISIBLE
            holder.content.setTextHtml(blocks[0], subredditName)
            startIndex = 1
        } else {
            holder.content.text = ""
            holder.content.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                holder.overflow.setViews(blocks, subredditName)
            } else {
                holder.overflow.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
        } else {
            holder.overflow.removeAllViews()
        }
    }

    override fun getItemCount(): Int {
        return if (dataSet.posts == null || dataSet.posts.isEmpty()) {
            0
        } else {
            dataSet.posts.size + 2
        }
    }

    override fun setError(b: Boolean) {
        listView.adapter = ErrorAdapter()
    }

    override fun undoSetError() {
        listView.adapter = this
    }

    class EmptyViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
    companion object {
        private const val COMMENT = 1
    }
}
