package me.ccrama.redditslide.Adapters

import android.os.AsyncTask
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.runBlocking
import ltd.ucode.lemmy.data.LemmyPost
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.id.CommunityId
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.lemmy.data.type.CommentSortType
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.data.IPost
import ltd.ucode.slide.repository.AccountRepository
import me.ccrama.redditslide.Fragments.CommentPage
import me.ccrama.redditslide.LastComments.setComments
import me.ccrama.redditslide.util.NetworkUtil
import net.dean.jraw.models.CommentSort
import java.util.TreeMap

class SubmissionComments {
    private val postRepository get() = page.postRepository
    private val commentRepository get() = page.commentRepository

    @JvmField var single = false
    @JvmField val refreshLayout: SwipeRefreshLayout
    private val fullName: String
    private val page: CommentPage
    @JvmField var comments: ArrayList<CommentObject?>? = null
    @JvmField var commentOPs = HashMap<Int, String>()
    var submission: IPost? = null
    private var context: String? = null
    private var defaultSorting = CommentSort.CONFIDENCE
    private var adapter: CommentAdapter? = null
    var mLoadData: LoadData? = null
    @JvmField var online = true
    var contextNumber = 5

    constructor(
        fullName: String,
        commentPage: CommentPage,
        layout: SwipeRefreshLayout,
        s: IPost
    ) {
        this.fullName = fullName
        page = commentPage
        online = NetworkUtil.isConnected(page.activity)
        refreshLayout = layout
        if (s.comments != null) {
            submission = s
            /*
            val baseComment = s.comments
            comments = ArrayList()
            val waiting: MutableMap<Int, MoreChildItem> = HashMap()
            for (n in baseComment.walkTree()) {
                val obj: CommentObject = CommentItem(n)
                val removed: MutableList<Int> = ArrayList()
                val map: MutableMap<Int, MoreChildItem> = TreeMap(Collections.reverseOrder())
                map.putAll(waiting)
                for (i in map.keys) {
                    if (i >= n.depth) {
                        comments!!.add(waiting[i])
                        removed.add(i)
                        waiting.remove(i)
                    }
                }
                comments!!.add(obj)
                if (n.hasMoreComments() && online) {
                    waiting[n.depth] = MoreChildItem(n, n.moreChildren)
                }
            }
            val map: MutableMap<Int, MoreChildItem> = TreeMap(Collections.reverseOrder())
            map.putAll(waiting)
            for (i in map.keys) {
                comments!!.add(waiting[i])
                waiting.remove(i)
            }
            if (baseComment.hasMoreComments() && online) {
                comments!!.add(MoreChildItem(baseComment, baseComment.moreChildren))
            }
            */
            if (adapter != null) {
                adapter!!.notifyDataSetChanged()
            }
            refreshLayout.isRefreshing = false
            refreshLayout.isEnabled = false
        }
    }

    constructor(fullName: String, commentPage: CommentPage, layout: SwipeRefreshLayout) {
        this.fullName = fullName
        page = commentPage
        refreshLayout = layout
    }

    constructor(
        fullName: String,
        commentPage: CommentPage,
        layout: SwipeRefreshLayout,
        context: String?,
        contextNumber: Int
    ) {
        this.fullName = fullName
        page = commentPage
        this.context = context
        refreshLayout = layout
        this.contextNumber = contextNumber
    }

    fun cancelLoad() {
        if (mLoadData != null) {
            mLoadData!!.cancel(true)
        }
    }

    fun setSorting(sort: CommentSort) {
        defaultSorting = sort
        mLoadData = LoadData(true)
        mLoadData!!.execute(fullName)
    }

    fun loadMore(adapter: CommentAdapter?, subreddit: String?) {
        this.adapter = adapter
        mLoadData = LoadData(true)
        mLoadData!!.execute(fullName)
    }

    fun loadMoreReply(adapter: CommentAdapter) {
        this.adapter = adapter
        adapter.currentSelectedItem = context?.toInt()
        mLoadData = LoadData(false)
        mLoadData!!.execute(fullName)
    }

    fun loadMore(adapter: CommentAdapter, subreddit: String?, forgetPlace: Boolean) {
        adapter.currentSelectedItem = 0
        this.adapter = adapter
        mLoadData = LoadData(true)
        mLoadData!!.execute(fullName)
    }

    fun reloadSubmission(commentAdapter: CommentAdapter) {
        val post = runBlocking {
            postRepository.getPost(AccountRepository.currentAccount, id = PostId(fullName.toInt()))
        }.success
        commentAdapter.submission = LemmyPost(post.postView.instanceName, post.postView)
    }

    inner class LoadData(val reset: Boolean) :
        AsyncTask<String?, Void?, ArrayList<CommentObject?>?>() {
        override fun onPostExecute(subs: ArrayList<CommentObject?>?) {
            if (page.isVisible && submission != null) {
                refreshLayout.isRefreshing = false
                page.doRefresh(false)
                /*
                if (submission!!.isArchived && !page.archived || submission!!.isLocked && !page.locked || submission!!.dataNode["contest_mode"].asBoolean() && !page.contest) page.doTopBarNotify(
                    submission!!, adapter
                )
                 */
                page.doData(reset)
                setComments(submission!!)
            }
        }

        override fun doInBackground(vararg subredditPaginators: String?): ArrayList<CommentObject?>? {
            single = context != null

            val paginator = commentRepository.getComments(
                AccountRepository.currentAccount,
                communityId = submission?.groupId as CommunityId?,
                communityName = null,
                parentId = context?.toInt()?.let(::CommentId),
                postId = PostId(fullName.split("/").last().toInt()),
                maxDepth = null, // contextNumber?
                limit = null,
                sort = CommentSortType.from(defaultSorting),
                type = null
            )

            val commentViews: MutableList<CommentView> = mutableListOf<CommentView>().also {
                while (paginator.hasNext) {
                    val page = runBlocking { paginator.next() }.success
                    it.addAll(page)
                } // assume closed set
            }
            val commentQueue = commentViews
                .associateBy { it.comment.id.id }.toMutableMap()
            val commentSort = commentViews
                .mapIndexed { index, commentView -> commentView.comment.id.id to index + 1 }.toMap()
            val rootSort: List<CommentView> = commentViews
                .filter { it.comment.parent.id == 0 }
            val commentChildren = commentViews
                .groupBy { it.comment.parent.id }
            // TODO: unfsck this ^

            val x = TreeMap<Int, CommentView>()
            comments = ArrayList<CommentObject?>()
            commentOPs = HashMap<Int, String>()
            for (headComment in rootSort) {
                commentQueue.remove(headComment.comment.id.id)

                comments!!.add(CommentItem(node = headComment))
                commentOPs!!.put(headComment.comment.id.id, headComment.creator.run { displayName ?: name })

                val queue = commentChildren[headComment.comment.id.id].orEmpty()
                    .sortedBy { commentSort[it.comment.id.id]!! }
                    .toMutableList()

                while (queue.isNotEmpty()) {
                    val nextComment = queue.removeFirst()
                    commentQueue.remove(nextComment.comment.id.id)

                    queue.addAll(0, commentChildren[nextComment.comment.id.id].orEmpty()
                        .sortedBy { commentSort[it.comment.id.id]!! })

                    comments!!.add(CommentItem(node = nextComment))
                    commentOPs!!.put(nextComment.comment.id.id, nextComment.creator.run { displayName ?: name })
                }
            } // runs in O(infinity)

            return comments!!
        }
    }
}
