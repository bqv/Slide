package me.ccrama.redditslide.Fragments

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import ltd.ucode.network.reddit.data.RedditSubmission
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.commentsScreen.CommentsScreen
import me.ccrama.redditslide.Activities.Shadowbox
import me.ccrama.redditslide.Adapters.AlbumView
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils.GetAlbumWithCallback
import me.ccrama.redditslide.ImgurAlbum.Image
import me.ccrama.redditslide.SubmissionViews.PopulateShadowboxInfo.doActionbar
import net.dean.jraw.models.Submission

class AlbumFull : Fragment() {
    var gallery = false
    private var list: View? = null
    private var i = 0
    private val s: Submission? = null
    var hidden = false
    var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.submission_albumcard, container, false)
        doActionbar(RedditSubmission(s!!), rootView!!, requireActivity(), true)
        if (s.url.contains("gallery")) {
            gallery = true
        }
        list = rootView!!.findViewById(R.id.images)
        list!!.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        (list as RecyclerView?)!!.layoutManager = layoutManager
        (list as RecyclerView?)!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                var va: ValueAnimator? = null
                if (dy > 0 && !hidden) {
                    hidden = true
                    if (va != null && va.isRunning) va.cancel()
                    val base = rootView!!.findViewById<View>(R.id.base)
                    va = ValueAnimator.ofFloat(1.0f, 0.2f)
                    val mDuration = 250 //in millis
                    va.duration = mDuration.toLong()
                    va.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation ->
                        val value = animation.animatedValue as Float
                        base.alpha = value
                    })
                    va.start()
                } else if (hidden && dy <= 0) {
                    val base = rootView!!.findViewById<View>(R.id.base)
                    if (va != null && va.isRunning) va.cancel()
                    hidden = false
                    va = ValueAnimator.ofFloat(0.2f, 1.0f)
                    val mDuration = 250 //in millis
                    va.duration = mDuration.toLong()
                    va.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation ->
                        val value = animation.animatedValue as Float
                        base.alpha = value
                    })
                    va.start()
                }
            }

        })
        val openClick = View.OnClickListener {
            (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                PanelState.EXPANDED
        }
        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
        val title = rootView!!.findViewById<View>(R.id.title)
        title.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView!!.findViewById<SlidingUpPanelLayout>(R.id.sliding_layout).panelHeight = title.measuredHeight
                    title.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        rootView!!.findViewById<SlidingUpPanelLayout>(R.id.sliding_layout).addPanelSlideListener(
            object : PanelSlideListener {
                override fun onPanelSlide(panel: View, slideOffset: Float) {
                }

                override fun onPanelStateChanged(
                    panel: View,
                    previousState: PanelState,
                    newState: PanelState
                ) {
                    if (newState === PanelState.EXPANDED) {
                        rootView!!.findViewById<View>(R.id.base)
                            .setOnClickListener {
                                val i2 = Intent(activity, CommentsScreen::class.java)
                                i2.putExtra(CommentsScreen.EXTRA_PAGE, i)
                                i2.putExtra(
                                    CommentsScreen.EXTRA_SUBREDDIT,
                                    (activity as Shadowbox?)!!.subreddit
                                )
                                activity!!.startActivity(i2)
                            }
                    } else {
                        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
                    }
                }
            })
        LoadIntoRecycler(s.url, requireActivity()).executeOnExecutor(
            AsyncTask.THREAD_POOL_EXECUTOR
        )
        return rootView
    }

    inner class LoadIntoRecycler    //todo htis dontClose = true;
        (var url: String, baseActivity: Activity) : GetAlbumWithCallback(
        url, baseActivity
    ) {
        override fun doWithData(jsonElements: List<Image>) {
            super.doWithData(jsonElements)
            val adapter = AlbumView(
                baseActivity, jsonElements, 0,
                s!!.subredditName, s.title
            )
            (list as RecyclerView?)!!.adapter = adapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        i = bundle!!.getInt("page", 0)
        if ((activity as Shadowbox?)!!.subredditPosts == null
            || (activity as Shadowbox?)!!.subredditPosts!!.posts.size < bundle.getInt(
                "page", 0
            )
        ) {
            requireActivity().finish()
        } else {
            //s = ((Shadowbox) getActivity()).subredditPosts.getPosts().get(bundle.getInt("page", 0)).getSubmission();
        }
    }
}
