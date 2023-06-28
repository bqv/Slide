package me.ccrama.redditslide.Fragments

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MarginLayoutParamsCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.InputCallback
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.itemanimators.AlphaInAnimator
import com.mikepenz.itemanimators.SlideUpAlphaAnimator
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.colours
import ltd.ucode.slide.SettingValues.defaultCardView
import ltd.ucode.slide.SettingValues.fabType
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.Search
import me.ccrama.redditslide.Activities.Submit
import me.ccrama.redditslide.Adapters.MultiredditAdapter
import me.ccrama.redditslide.Adapters.MultiredditPosts
import me.ccrama.redditslide.Adapters.SubmissionDisplay
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.HasSeen
import me.ccrama.redditslide.Hidden
import me.ccrama.redditslide.OfflineSubreddit
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler
import me.ccrama.redditslide.submission
import me.ccrama.redditslide.util.LayoutUtils
import me.ccrama.redditslide.views.CatchStaggeredGridLayoutManager
import me.ccrama.redditslide.views.CreateCardView
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.MultiSubreddit

class MultiredditView : Fragment(), SubmissionDisplay {
    var adapter: MultiredditAdapter? = null
    var posts: MultiredditPosts? = null
    var rv: RecyclerView? = null
    var fab: FloatingActionButton? = null
    var diff = 0
    private var refreshLayout: SwipeRefreshLayout? = null
    private var id = 0
    private var totalItemCount = 0
    private var visibleItemCount = 0
    private var pastVisiblesItems = 0
    private var profile: String? = null
    private fun createLayoutManager(numColumns: Int): RecyclerView.LayoutManager {
        return CatchStaggeredGridLayoutManager(numColumns, CatchStaggeredGridLayoutManager.VERTICAL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_verticalcontent, container, false)
        rv = v.findViewById(R.id.vertical_content)
        val mLayoutManager = createLayoutManager(
            LayoutUtils.getNumColumns(
                resources.configuration.orientation,
                activity
            )
        )
        rv!!.setLayoutManager(mLayoutManager)
        if (SettingValues.fab) {
            fab = v.findViewById(R.id.post_floating_action_button)
            if (fabType == Constants.FAB_POST) {
                fab!!.setOnClickListener(View.OnClickListener {
                    val subs = ArrayList<String?>()
                    for (s: MultiSubreddit in posts!!.multiReddit!!.subreddits) {
                        subs.add(s.displayName)
                    }
                    MaterialDialog.Builder(requireActivity())
                        .title(R.string.multi_submit_which_sub)
                        .items(subs)
                        .itemsCallback { dialog, itemView, which, text ->
                            val i = Intent(activity, Submit::class.java)
                            i.putExtra(Submit.EXTRA_SUBREDDIT, subs[which])
                            startActivity(i)
                        }.show()
                })
            } else if (fabType == Constants.FAB_SEARCH) {
                fab!!.setImageResource(R.drawable.ic_search)
                fab!!.setOnClickListener(object : View.OnClickListener {
                    var term: String? = null
                    override fun onClick(v: View) {
                        val builder = MaterialDialog.Builder((activity)!!)
                            .title(R.string.search_title)
                            .alwaysCallInputCallback()
                            .input(getString(R.string.search_msg), "",
                                InputCallback { materialDialog, charSequence ->
                                    term = charSequence.toString()
                                })
                        builder.positiveText(
                            getString(
                                R.string.search_subreddit,
                                "/m/" + posts!!.multiReddit!!.displayName
                            )
                        )
                            .onPositive(object : SingleButtonCallback {
                                override fun onClick(
                                    materialDialog: MaterialDialog,
                                    dialogAction: DialogAction
                                ) {
                                    val i = Intent(activity, Search::class.java)
                                    i.putExtra(Search.EXTRA_TERM, term)
                                    i.putExtra(
                                        Search.EXTRA_MULTIREDDIT,
                                        posts!!.multiReddit!!.displayName
                                    )
                                    startActivity(i)
                                }
                            })
                        builder.show()
                    }
                })
            } else {
                fab!!.setImageResource(R.drawable.ic_visibility_off)
                fab!!.setOnClickListener(View.OnClickListener {
                    if (!App.fabClear) {
                        AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.settings_fabclear)
                            .setMessage(R.string.settings_fabclear_msg)
                            .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int ->
                                colours.edit()
                                    .putBoolean(SettingValues.PREF_FAB_CLEAR, true)
                                    .apply()
                                App.fabClear = true
                                clearSeenPosts(false)
                            }
                            .show()
                    } else {
                        clearSeenPosts(false)
                    }
                })
                fab!!.setOnLongClickListener(View.OnLongClickListener {
                    if (!App.fabClear) {
                        AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.settings_fabclear)
                            .setMessage(R.string.settings_fabclear_msg)
                            .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int ->
                                colours.edit()
                                    .putBoolean(SettingValues.PREF_FAB_CLEAR, true)
                                    .apply()
                                App.fabClear = true
                                clearSeenPosts(true)
                            }
                            .show()
                    } else {
                        clearSeenPosts(true)
                    }
                    /*
                                    ToDo Make a sncakbar with an undo option of the clear all
                                    View.OnClickListener undoAction = new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            adapter.dataSet.posts = original;
                                            for(IPost post : adapter.dataSet.posts){
                                                if(HasSeen.getSeen(post.getFullName()))
                                                    Hidden.undoHidden(post);
                                            }
                                        }
                                    };*/
                    val s = Snackbar.make(
                        rv!!,
                        resources.getString(R.string.posts_hidden_forever),
                        Snackbar.LENGTH_LONG
                    )
                    LayoutUtils.showSnackbar(s)
                    false
                })
            }
        } else {
            v.findViewById<View>(R.id.post_floating_action_button).visibility = View.GONE
        }
        refreshLayout = v.findViewById(R.id.activity_main_swipe_refresh_layout)
        /**
         * If using List view mode, we need to remove the start margin from the SwipeRefreshLayout.
         * The scrollbar style of "outsideInset" creates a 4dp padding around it. To counter this,
         * change the scrollbar style to "insideOverlay" when list view is enabled.
         * To recap: this removes the margins from the start/end so list view is full-width.
         */
        if (defaultCardView === CreateCardView.CardEnum.LIST) {
            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            MarginLayoutParamsCompat.setMarginStart(params, 0)
            rv!!.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
            refreshLayout!!.setLayoutParams(params)
        }
        val multireddits: List<MultiReddit>? = if (profile!!.isEmpty()) {
            UserSubscriptions.multireddits!!.filterNotNull()
        } else {
            UserSubscriptions.public_multireddits[profile]!!.filterNotNull()
        }
        if (!multireddits.isNullOrEmpty()) {
            refreshLayout!!.setColorSchemeColors(
                *Palette.getColors(
                    multireddits[id].displayName, activity
                )
            )
        }

        //If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
        //So, we estimate the height of the header in dp
        refreshLayout!!.setProgressViewOffset(
            false,
            Constants.TAB_HEADER_VIEW_OFFSET - Constants.PTR_OFFSET_TOP,
            Constants.TAB_HEADER_VIEW_OFFSET + Constants.PTR_OFFSET_BOTTOM
        )
        refreshLayout!!.post(Runnable { refreshLayout!!.setRefreshing(true) })
        if (multireddits != null && !multireddits.isEmpty()) {
            posts = MultiredditPosts(multireddits[id].displayName, profile!!)
            adapter = MultiredditAdapter(requireActivity(), posts!!, rv!!, refreshLayout!!, this)
            rv!!.adapter = adapter
            rv!!.itemAnimator = SlideUpAlphaAnimator().withInterpolator(LinearOutSlowInInterpolator())
            posts!!.loadMore(requireActivity(), this, true, adapter)
            refreshLayout!!.setOnRefreshListener(
                OnRefreshListener {
                    posts!!.loadMore(requireActivity(), this@MultiredditView, true, adapter)

                    //TODO catch errors
                }
            )
            if (fab != null) {
                fab!!.show()
            }
            rv!!.addOnScrollListener(object : ToolbarScrollHideHandler(
                requireActivity().findViewById<Toolbar>(R.id.toolbar),
                requireActivity().findViewById<View>(R.id.header)
            ) {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    visibleItemCount = rv!!.getLayoutManager()!!.childCount
                    totalItemCount = rv!!.getLayoutManager()!!.itemCount
                    val firstVisibleItems =
                        (rv!!.getLayoutManager() as CatchStaggeredGridLayoutManager?)!!.findFirstVisibleItemPositions(
                            null
                        )
                    if (firstVisibleItems != null && firstVisibleItems.size > 0) {
                        for (firstVisibleItem in firstVisibleItems) {
                            pastVisiblesItems = firstVisibleItem
                            if (SettingValues.scrollSeen && pastVisiblesItems > 0 && SettingValues.storeHistory) {
                                HasSeen.addSeenScrolling(posts!!.posts[pastVisiblesItems - 1].permalink)
                            }
                        }
                    }
                    if (!posts!!.loading) {
                        if (visibleItemCount + pastVisiblesItems + 5 >= totalItemCount && !posts!!.nomore) {
                            posts!!.loading = true
                            posts!!.loadMore(activity!!, this@MultiredditView, false, adapter)
                        }
                    }
                    if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        diff += dy
                    } else {
                        diff = 0
                    }
                    if (fab != null) {
                        if ((dy <= 0) && fab!!.id != 0 && SettingValues.fab) {
                            if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_DRAGGING || diff < -fab!!.height * 2) fab!!.show()
                        } else {
                            fab!!.hide()
                        }
                    }
                }
            })
        }
        return v
    }

    private fun clearSeenPosts(forever: Boolean): List<IPost>? {
        if (posts!!.posts != null) {
            val originalDataSetPosts: List<IPost> = posts!!.posts
            val o = OfflineSubreddit.getSubreddit(
                "multi" + posts!!.multiReddit!!.displayName.lowercase(),
                false,
                activity
            )
            for (i in posts!!.posts.size downTo -1 + 1) {
                try {
                    if (HasSeen.getSeen(posts!!.posts[i].submission)) {
                        if (forever) {
                            Hidden.setHidden(posts!!.posts[i])
                        }
                        o!!.clearPost(posts!!.posts[i])
                        posts!!.posts.removeAt(i)
                        if (posts!!.posts.isEmpty()) {
                            adapter!!.notifyDataSetChanged()
                        } else {
                            rv!!.itemAnimator = AlphaInAnimator()
                            adapter!!.notifyItemRemoved(i + 1)
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    //Let the loop reset itself
                }
            }
            o!!.writeToMemoryNoStorage()
            rv!!.itemAnimator =
                SlideUpAlphaAnimator().withInterpolator(LinearOutSlowInInterpolator())
            return originalDataSetPosts
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        id = bundle!!.getInt("id", 0)
        profile = bundle.getString(EXTRA_PROFILE, "")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val currentOrientation = newConfig.orientation
        val mLayoutManager = rv!!.layoutManager as CatchStaggeredGridLayoutManager?
        mLayoutManager!!.spanCount = LayoutUtils.getNumColumns(currentOrientation, activity)
    }

    override fun updateSuccess(submissions: List<IPost>, startIndex: Int) {
        adapter!!.context.runOnUiThread {
            refreshLayout!!.isRefreshing = false
            if (startIndex != -1) {
                adapter!!.notifyItemRangeInserted(startIndex + 1, posts!!.posts.size)
            } else {
                adapter!!.notifyDataSetChanged()
            }
        }
    }

    override fun updateOffline(submissions: List<IPost>, cacheTime: Long) {
        adapter!!.setError(true)
        refreshLayout!!.isRefreshing = false
    }

    override fun updateOfflineError() {}
    override fun updateError() {}
    override fun updateViews() {
        try {
            adapter!!.notifyItemRangeChanged(0, adapter!!.dataSet.posts.size)
        } catch (e: Exception) {
        }
    }

    override fun onAdapterUpdated() {
        adapter!!.notifyDataSetChanged()
    }

    companion object {
        private const val EXTRA_PROFILE = "profile"
    }
}
