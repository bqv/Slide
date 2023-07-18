package me.ccrama.redditslide.Fragments

import android.animation.ValueAnimator
import android.app.Activity
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
import ltd.ucode.slide.R
import me.ccrama.redditslide.Activities.ShadowboxComments
import me.ccrama.redditslide.Adapters.AlbumView
import me.ccrama.redditslide.Adapters.CommentUrlObject
import me.ccrama.redditslide.ImgurAlbum.AlbumUtils.GetAlbumWithCallback
import me.ccrama.redditslide.ImgurAlbum.Image
import me.ccrama.redditslide.OpenRedditLink
import me.ccrama.redditslide.SubmissionViews.PopulateShadowboxInfo.doActionbar

class AlbumFullComments : Fragment() {
    var gallery = false
    private var list: View? = null
    private var s: CommentUrlObject? = null
    var hidden = false
    var rootView: View? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(
            R.layout.submission_albumcard, container, false
        )
        doActionbar(s!!.comment, rootView!!, requireActivity(), true)
        val url = s!!.url
        if (url.contains("gallery")) {
            gallery = true
        }
        list = rootView!!.findViewById(R.id.images)
        list!!.setVisibility(View.VISIBLE)
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

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        val openClick = View.OnClickListener {
            (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelState =
                PanelState.EXPANDED
        }
        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
        val title = rootView!!.findViewById<View>(R.id.title)
        title.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).panelHeight =
                    title.measuredHeight
                title.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        (rootView!!.findViewById<View>(R.id.sliding_layout) as SlidingUpPanelLayout).addPanelSlideListener(
            object : PanelSlideListener {
                override fun onPanelSlide(panel: View, slideOffset: Float) {
                }

                override fun onPanelStateChanged(
                    panel: View,
                    previousState: PanelState,
                    newState: PanelState
                ) {
                    if (newState === PanelState.EXPANDED) {
                        val c = s!!.comment.comment
                        rootView!!.findViewById<View>(R.id.base)
                            .setOnClickListener { v: View? ->
                                val url1 = "https://reddit.com/r/${c.subredditName}/comments/${
                                    c.dataNode["link_id"].asText().substring(3)
                                }/nothing/${c.id}?context=3"
                                OpenRedditLink.openUrl(activity, url1, true)
                            }
                    } else {
                        rootView!!.findViewById<View>(R.id.base).setOnClickListener(openClick)
                    }
                }
            })
        LoadIntoRecycler(url, requireActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return rootView
    }

    inner class LoadIntoRecycler    //todo htis dontClose = true;
        (var url: String, baseActivity: Activity) : GetAlbumWithCallback(
        url, baseActivity
    ) {
        override fun doWithData(jsonElements: List<Image>) {
            super.doWithData(jsonElements)
            //May be a bug with downloading multiple comment albums off the same submission
            val adapter = AlbumView(
                baseActivity, jsonElements, 0, s!!.subredditName,
                s!!.comment.comment.submissionTitle
            )
            (list as RecyclerView?)!!.adapter = adapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        s = ShadowboxComments.comments!![bundle!!.getInt("page", 0)]
    }
}
